package com.villagerai.ai;

import com.google.common.collect.ImmutableMap;
import com.villagerai.config.VillagerAIConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.pathfinder.Path;

import java.util.function.Predicate;

public class AcquireNearestJobSite extends Behavior<Villager> {
    private final float speedModifier;
    private long lastCheckTime;

    public AcquireNearestJobSite(float speedModifier) {
        super(ImmutableMap.of(
            MemoryModuleType.JOB_SITE, MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.POTENTIAL_JOB_SITE, MemoryStatus.VALUE_ABSENT
        ));
        this.speedModifier = speedModifier;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (!VillagerAIConfig.get().assignNearestVillager) {
            return false;
        }
        if (villager.isBaby() || villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            return false;
        }
        long time = level.getGameTime();
        if (time - this.lastCheckTime < 20L) {
            return false;
        }
        this.lastCheckTime = time;
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        Predicate<Holder<PoiType>> jobSitePredicate = villager.getVillagerData().profession().value().acquirableJobSite();
        PoiManager poiManager = level.getPoiManager();

        poiManager.findAllClosestFirstWithType(
            jobSitePredicate,
            pos -> true,
            villager.blockPosition(),
            48,
            PoiManager.Occupancy.HAS_SPACE
        ).limit(5).forEach(pair -> {
            BlockPos targetPos = pair.getSecond();
            Holder<PoiType> poiHolder = pair.getFirst();

            // Find who is truly the closest eligible unassigned villager to this workstation
            Villager nearest = VillagerAssignmentHelper.findNearestEligibleVillager(level, targetPos, poiHolder, 48.0);
            if (nearest == villager) {
                Path path = villager.getNavigation().createPath(targetPos, 1);
                if (path != null && path.canReach()) {
                    VillagerAssignmentHelper.assignWorkstation(level, villager, targetPos, poiHolder);
                }
            } else if (nearest != null) {
                // If another villager is closer, assign it to that closer villager
                Path path = nearest.getNavigation().createPath(targetPos, 1);
                if (path != null && path.canReach()) {
                    VillagerAssignmentHelper.assignWorkstation(level, nearest, targetPos, poiHolder);
                }
            }
        });
    }
}
