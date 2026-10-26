package com.villagerai.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class VillagerAssignmentHelper {

    public static final double SCAN_RADIUS = 48.0;

    /**
     * Determines whether a villager is eligible to take this workstation job.
     */
    public static boolean canTakeJob(Villager villager, Holder<PoiType> poiType) {
        if (!villager.isAlive() || villager.isBaby()) {
            return false;
        }
        // Nitwits can never take jobs
        if (villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            return false;
        }
        // If already has an active claimed job site memory, not eligible
        if (villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isPresent()) {
            return false;
        }         
        // If traded (experience > 0 or level > 1), profession is locked
        if (villager.getVillagerXp() > 0 || villager.getVillagerData().level() > 1) {
            // Can only re-claim if its locked profession matches this POI
            return villager.getVillagerData().profession().value().heldJobSite().test(poiType);
        }
        // Unemployed villagers (NONE) can take any job
        if (villager.getVillagerData().profession().is(VillagerProfession.NONE)) {
            return true;
        }
        // Level 1 unemployed without trades can switch to or take this job
        return villager.getVillagerData().profession().value().heldJobSite().test(poiType);
    }

    /**
     * Finds the closest eligible unassigned villager to the given POI position.
     */
    public static Villager findNearestEligibleVillager(ServerLevel level, BlockPos pos, Holder<PoiType> poiType, double maxRadius) {
        Vec3 center = Vec3.atCenterOf(pos);
        double maxDistSq = maxRadius * maxRadius;

        List<Villager> villagers = new java.util.ArrayList<>(level.getEntities(
            EntityTypes.VILLAGER,
            v -> v.isAlive() && !v.isBaby() && v.distanceToSqr(center) <= maxDistSq && canTakeJob(v, poiType)
        ));

        if (villagers.isEmpty()) {
            return null;
        }

        villagers.sort(Comparator.comparingDouble(v -> v.distanceToSqr(center)));
        return villagers.get(0);
    }

    /**
     * Called when a workstation block is placed in the world.
     */
    public static void onWorkstationPlaced(ServerLevel level, BlockPos pos, BlockState state) {
        Optional<Holder<PoiType>> optionalPoi = PoiTypes.forState(state);
        if (optionalPoi.isEmpty()) {
            return;
        }
        Holder<PoiType> poiHolder = optionalPoi.get();

        // Check if this POI is an acquirable villager job
        if (!VillagerProfession.ALL_ACQUIRABLE_JOBS.test(poiHolder)) {
            return;
        }

        // Find nearest eligible villager
        Villager nearest = findNearestEligibleVillager(level, pos, poiHolder, SCAN_RADIUS);
        if (nearest == null) {
            return;
        }

        assignWorkstation(level, nearest, pos, poiHolder);
    }

    /**
     * Assigns the workstation to the target villager, clearing competitors if necessary.
     */
    public static void assignWorkstation(ServerLevel level, Villager villager, BlockPos pos, Holder<PoiType> poiHolder) {
        PoiManager poiManager = level.getPoiManager();

        // Clear any competitor who previously had this pos as potential or uncompleted job site
        List<? extends Villager> competitors = level.getEntities(EntityTypes.VILLAGER, v -> v != villager && v.isAlive());
        for (Villager other : competitors) {
            Optional<GlobalPos> pot = other.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
            if (pot.isPresent() && pot.get().pos().equals(pos)) {
                other.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
                other.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            }
            Optional<GlobalPos> job = other.getBrain().getMemory(MemoryModuleType.JOB_SITE);
            if (job.isPresent() && job.get().pos().equals(pos) && other.getVillagerXp() == 0 && other.getVillagerData().level() <= 1) {
                other.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
                other.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                other.setVillagerData(other.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.getOrThrow(VillagerProfession.NONE)));
                other.refreshBrain(level);
            }
        }

        // Take POI ticket
        poiManager.take(h -> h.equals(poiHolder), (h, p) -> p.equals(pos), pos, 1);

        // Find matching profession
        Optional<Holder.Reference<VillagerProfession>> matchingProf = BuiltInRegistries.VILLAGER_PROFESSION.listElements()
            .filter(h -> h.value().heldJobSite().test(poiHolder))
            .findFirst();

        // If close enough (within 2.5 blocks), directly establish job site and profession
        if (pos.closerToCenterThan(villager.position(), 2.5d)) {
            villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
            villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), pos));

            matchingProf.ifPresent(prof -> {
                villager.setVillagerData(villager.getVillagerData().withProfession(prof));
                villager.refreshBrain(level);
                if (prof.value().workSound() != null) {
                    level.playSound(null, villager.getX(), villager.getY(), villager.getZ(), prof.value().workSound(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                }               
            });

            level.broadcastEntityEvent(villager, (byte) 14); // Happy green sparkles
        } else {
            // Set POTENTIAL_JOB_SITE and navigate villager toward the workstation
            villager.getBrain().setMemory(MemoryModuleType.POTENTIAL_JOB_SITE, GlobalPos.of(level.dimension(), pos));
            BehaviorUtils.setWalkAndLookTargetMemories(villager, pos, 0.5F, 1);
        }
    }
}
