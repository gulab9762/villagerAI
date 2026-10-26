package com.villagerai.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import com.villagerai.ai.AcquireNearestJobSite;
import com.villagerai.config.VillagerAIConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerGoalPackages.class)
public abstract class VillagerGoalPackagesMixin {

    @Invoker("getFullLookBehavior")
    public static Pair<Integer, BehaviorControl<LivingEntity>> invokeGetFullLookBehavior() {
        throw new AssertionError();
    }

    @Invoker("getMinimalLookBehavior")
    public static Pair<Integer, BehaviorControl<LivingEntity>> invokeGetMinimalLookBehavior() {
        throw new AssertionError();
    }

    /**
     * Override core package to replace vanilla AcquirePoi for JOB_SITE with AcquireNearestJobSite,
     * ensuring the closest eligible unassigned villager always claims the workstation.
     */
    @Inject(method = "getCorePackage", at = @At("RETURN"), cancellable = true)
    private static void onGetCorePackage(Holder<VillagerProfession> profession, float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        if (!VillagerAIConfig.get().assignNearestVillager) {
            return;
        }
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original = cir.getReturnValue();
        ImmutableList.Builder<Pair<Integer, ? extends BehaviorControl<? super Villager>>> builder = ImmutableList.builder();

        for (Pair<Integer, ? extends BehaviorControl<? super Villager>> pair : original) {
            if (pair.getFirst() == 6) {
                builder.add(Pair.of(6, new AcquireNearestJobSite(speedModifier)));
            } else {
                builder.add(pair);
            }
        }
        cir.setReturnValue(builder.build());
    }

    /**
     * Override idle package to remove random wandering/roaming (VillageBoundRandomStroll,
     * SetWalkTargetFromLookTarget) so villagers remain in place while idle, while still
     * socializing, breeding, trading, and looking around.
     */
    @Inject(method = "getIdlePackage", at = @At("HEAD"), cancellable = true)
    private static void onGetIdlePackage(float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        if (!VillagerAIConfig.get().disableRoaming) {
            return;
        }
        cir.setReturnValue(
            ImmutableList.of(
                Pair.of(
                    2,
                    new RunOne<>(
                        ImmutableList.of(
                            Pair.of(InteractWith.of(EntityTypes.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 2),
                            Pair.of(
                                InteractWith.of(
                                    EntityTypes.VILLAGER, 8, AgeableMob::canBreed, AgeableMob::canBreed, MemoryModuleType.BREED_TARGET, speedModifier, 2
                                ),
                                1
                            ),
                            Pair.of(InteractWith.of(EntityTypes.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 1),
                            // Roaming behaviors removed: VillageBoundRandomStroll & SetWalkTargetFromLookTarget
                            Pair.of(new DoNothing(30, 60), 2)
                        )
                    )
                ),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(3, SetLookAndInteract.create(EntityTypes.PLAYER, 4)),
                Pair.of(3, new ShowTradesToPlayer(400, 1600)),
                Pair.of(
                    3,
                    new GateBehavior<>(
                        ImmutableMap.of(),
                        ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),
                        GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE,
                        ImmutableList.of(Pair.of(new TradeWithVillager(), 1))
                    )
                ),
                Pair.of(
                    3,
                    new GateBehavior<>(
                        ImmutableMap.of(),
                        ImmutableSet.of(MemoryModuleType.BREED_TARGET),
                        GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE,
                        ImmutableList.of(Pair.of(new VillagerMakeLove(), 1))
                    )
                ),
                invokeGetFullLookBehavior(),
                Pair.of(99, UpdateActivityFromSchedule.create())
            )
        );
    }

    /**
     * Override work package to remove StrollAroundPoi so villagers do not wander around their workstation,
     * while retaining direct workstation navigation (SetWalkTargetFromBlockMemory, StrollToPoi) and tasks.
     */
    @Inject(method = "getWorkPackage", at = @At("HEAD"), cancellable = true)
    private static void onGetWorkPackage(Holder<VillagerProfession> profession, float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        if (!VillagerAIConfig.get().disableRoaming || !VillagerAIConfig.get().disableWorkRoaming) {
            return;
        }
        WorkAtPoi workAtPoi;
        if (profession.is(VillagerProfession.FARMER)) {
            workAtPoi = new WorkAtComposter();
        } else {
            workAtPoi = new WorkAtPoi();
        }

        cir.setReturnValue(
            ImmutableList.of(
                invokeGetMinimalLookBehavior(),
                Pair.of(
                    5,
                    new RunOne<>(
                        ImmutableList.of(
                            Pair.of(workAtPoi, 7),
                            // StrollAroundPoi removed to prevent wandering around workstation
                            Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, 0.4F, 1, 10), 5),
                            Pair.of(StrollToPoiList.create(MemoryModuleType.SECONDARY_JOB_SITE, speedModifier, 1, 6, MemoryModuleType.JOB_SITE), 5),
                            Pair.of(new HarvestFarmland(), profession.is(VillagerProfession.FARMER) ? 2 : 5),
                            Pair.of(new UseBonemeal(), profession.is(VillagerProfession.FARMER) ? 4 : 7)
                        )
                    )
                ),
                Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                Pair.of(10, SetLookAndInteract.create(EntityTypes.PLAYER, 4)),
                Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.JOB_SITE, speedModifier, 9, 100, 1200)),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(99, UpdateActivityFromSchedule.create())
            )
        );
    }

    /**
     * Override meet package to remove 40-block wandering around the bell (StrollAroundPoi),
     * keeping gathering and socialization at the meeting point.
     */
    @Inject(method = "getMeetPackage", at = @At("HEAD"), cancellable = true)
    private static void onGetMeetPackage(float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        if (!VillagerAIConfig.get().disableRoaming || !VillagerAIConfig.get().disableBellRoaming) {
            return;
        }
        cir.setReturnValue(
            ImmutableList.of(
                Pair.of(2, SocializeAtBell.create()),
                Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                Pair.of(10, SetLookAndInteract.create(EntityTypes.PLAYER, 4)),
                Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.MEETING_POINT, speedModifier, 6, 100, 200)),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(3, ValidateNearbyPoi.create(p -> p.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT)),
                Pair.of(
                    3,
                    new GateBehavior<>(
                        ImmutableMap.of(),
                        ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),
                        GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE,
                        ImmutableList.of(Pair.of(new TradeWithVillager(), 1))
                    )
                ),
                invokeGetFullLookBehavior(),
                Pair.of(99, UpdateActivityFromSchedule.create())
            )
        );
    }

    /**
     * Override rest package to remove InsideBrownianWalk, ensuring villagers go to bed
     * and rest peacefully without restless pacing.
     */
    @Inject(method = "getRestPackage", at = @At("HEAD"), cancellable = true)
    private static void onGetRestPackage(float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        if (!VillagerAIConfig.get().disableRoaming || !VillagerAIConfig.get().disableRestRoaming) {
            return;
        }
        cir.setReturnValue(
            ImmutableList.of(
                Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.HOME, speedModifier, 1, 150, 1200)),
                Pair.of(3, ValidateNearbyPoi.create(p -> p.is(PoiTypes.HOME), MemoryModuleType.HOME)),
                Pair.of(3, new SleepInBed()),
                Pair.of(
                    5,
                    new RunOne<>(
                        ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT),
                        ImmutableList.of(
                            Pair.of(SetClosestHomeAsWalkTarget.create(speedModifier), 1),
                            // InsideBrownianWalk removed
                            Pair.of(GoToClosestVillage.create(speedModifier, 4), 2),
                            Pair.of(new DoNothing(20, 40), 2)
                        )
                    )
                ),
                invokeGetMinimalLookBehavior(),
                Pair.of(99, UpdateActivityFromSchedule.create())
            )
        );
    }

    /**
     * Override play package for baby villagers to prevent wandering away into danger.
     */
    @Inject(method = "getPlayPackage", at = @At("HEAD"), cancellable = true)
    private static void onGetPlayPackage(float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        if (!VillagerAIConfig.get().disableRoaming || !VillagerAIConfig.get().disableBabyRoaming) {
            return;
        }
        cir.setReturnValue(
            ImmutableList.of(
                Pair.of(0, new MoveToTargetSink(80, 120)),
                invokeGetFullLookBehavior(),
                Pair.of(5, PlayTagWithOtherKids.create()),
                Pair.of(
                    5,
                    new RunOne<>(
                        ImmutableMap.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryStatus.VALUE_ABSENT),
                        ImmutableList.of(
                            Pair.of(InteractWith.of(EntityTypes.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 2),
                            Pair.of(InteractWith.of(EntityTypes.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 1),
                            // VillageBoundRandomStroll and SetWalkTargetFromLookTarget removed
                            Pair.of(new JumpOnBed(speedModifier), 2),
                            Pair.of(new DoNothing(20, 40), 2)
                        )
                    )
                ),
                Pair.of(99, UpdateActivityFromSchedule.create())
            )
        );
    }
}
