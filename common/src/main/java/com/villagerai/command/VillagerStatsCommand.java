package com.villagerai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.villagerai.client.VillagerStatsData;
import com.villagerai.config.VillagerAIConfig;

public class VillagerStatsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerNodes(dispatcher, "villagerstats");
        registerNodes(dispatcher, "villagerStats");
        registerNodes(dispatcher, "evv");
        registerNodes(dispatcher, "easyvanillavillagers");
    }

    private static void registerNodes(CommandDispatcher<CommandSourceStack> dispatcher, String literal) {
        dispatcher.register(
            Commands.literal(literal)
                .executes(VillagerStatsCommand::executeSmart)
                .then(
                    Commands.literal("status")
                        .executes(VillagerStatsCommand::executeStatus)
                )
                .then(
                    Commands.literal("calendar")
                        .executes(VillagerStatsCommand::executeCalendar)
                )
                .then(
                    Commands.literal("toggle-roam")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(VillagerStatsCommand::executeToggleRoam)
                )
        );
    }

    private static int executeSmart(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            Villager target = findTargetVillager(player);
            if (target != null) {
                return showVillagerStatus(source, target);
            }
        }
        return showCalendar(source);
    }

    private static int executeStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            Villager target = findTargetVillager(player);
            if (target != null) {
                return showVillagerStatus(source, target);
            } else {
                source.sendFailure(Component.translatable("evv.command.no_target"));
                return showCalendar(source);
            }
        } else {
            source.sendFailure(Component.translatable("evv.command.player_only"));
            return showCalendar(source);
        }
    }

    private static int executeCalendar(CommandContext<CommandSourceStack> context) {
        return showCalendar(context.getSource());
    }

    private static int executeToggleRoam(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        VillagerAIConfig config = VillagerAIConfig.get();
        config.disableRoaming = !config.disableRoaming;
        VillagerAIConfig.save();

        int count = refreshAllLoadedVillagers(source.getServer());

        if (config.disableRoaming) {
            source.sendSuccess(() -> Component.translatable("evv.command.toggle.disabled", count), true);
        } else {
            source.sendSuccess(() -> Component.translatable("evv.command.toggle.enabled", count), true);
        }
        return 1;
    }

    public static int refreshAllLoadedVillagers(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            List<? extends Villager> villagers = level.getEntities(EntityTypes.VILLAGER, LivingEntity::isAlive);
            for (Villager villager : villagers) {
                villager.getNavigation().stop();
                villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
                villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
                villager.refreshBrain(level);
                count++;
            }
        }
        return count;
    }

    private static Villager findTargetVillager(ServerPlayer player) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof Villager, 12.0);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Villager villager) {
            return villager;
        }

        List<Villager> nearby = player.level().getEntitiesOfClass(
            Villager.class,
            player.getBoundingBox().inflate(16.0)
        );
        if (!nearby.isEmpty()) {
            nearby.sort(Comparator.comparingDouble(player::distanceToSqr));
            return nearby.get(0);
        }

        return null;
    }

    private static int showVillagerStatus(CommandSourceStack source, Villager villager) {
        ServerLevel level = villager.level() instanceof ServerLevel sl ? sl : source.getLevel();
        long dayTime = level.getOverworldClockTime() % 24000L;
        long dayCount = level.getOverworldClockTime() / 24000L;
        String clockTime = VillagerStatsData.formatClockTime(dayTime);

        VillagerData data = villager.getVillagerData();
        Holder<VillagerProfession> professionHolder = data.profession();
        String regName = professionHolder.getRegisteredName();
        String profId = regName != null && regName.contains(":") ? regName.substring(regName.indexOf(':') + 1) : (regName != null ? regName : "none");
        String profName = VillagerStatsData.capitalize(profId);
        Component profComponent = Component.translatableWithFallback("entity.minecraft.villager." + profId, profName);

        int levelNum = data.level();
        int xp = villager.getVillagerXp();

        Brain<Villager> brain = villager.getBrain();
        Optional<Activity> activeActivity = brain.getActiveNonCoreActivity();
        String actName = activeActivity.map(Activity::getName).orElse("idle").toLowerCase();
        Component activityComponent = Component.translatableWithFallback("evv.activity." + actName, actName.toUpperCase());
        Component scheduledPhaseComponent = VillagerStatsData.getScheduledPhaseComponent(dayTime, villager.isBaby());

        Optional<GlobalPos> jobSite = brain.getMemory(MemoryModuleType.JOB_SITE);
        Optional<GlobalPos> home = brain.getMemory(MemoryModuleType.HOME);
        Optional<GlobalPos> meeting = brain.getMemory(MemoryModuleType.MEETING_POINT);
        Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);

        boolean noRoam = VillagerAIConfig.get().disableRoaming;

        source.sendSuccess(() -> Component.translatable("evv.command.status.header"), false);
        source.sendSuccess(() -> Component.translatable("evv.command.status.profession",
            profComponent,
            levelNum,
            xp,
            villager.isBaby() ? Component.translatable("evv.gui.baby") : Component.translatable("evv.gui.adult")
        ), false);
        source.sendSuccess(() -> Component.translatable("evv.command.status.ai_mode",
            noRoam ? Component.translatable("evv.gui.mode.no_roam") : Component.translatable("evv.gui.mode.vanilla")), false);
        source.sendSuccess(() -> Component.translatable("evv.command.status.time", clockTime, dayTime, dayCount), false);
        source.sendSuccess(() -> Component.translatable("evv.command.status.scheduled_phase", scheduledPhaseComponent), false);
        source.sendSuccess(() -> Component.translatable("evv.command.status.brain_activity", activityComponent), false);
        source.sendSuccess(() -> Component.translatable("evv.command.status.workstation", formatPos(jobSite)), false);
        source.sendSuccess(() -> Component.translatable("evv.command.status.bed", formatPos(home)), false);
        source.sendSuccess(() -> Component.translatable("evv.command.status.bell", formatPos(meeting)), false);

        if (walkTarget.isPresent()) {
            Vec3 targetPos = walkTarget.get().getTarget().currentPosition();
            double dist = villager.position().distanceTo(targetPos);
            source.sendSuccess(() -> Component.translatable("evv.command.walk_target.coords",
                targetPos.x, targetPos.y, targetPos.z, dist, walkTarget.get().getSpeedModifier()), false);
        } else {
            source.sendSuccess(() -> Component.translatable(noRoam
                ? "evv.command.walk_target.stationary"
                : "evv.command.walk_target.none"), false);
        }
        source.sendSuccess(() -> Component.translatable("evv.command.status.footer"), false);

        return 1;
    }

    private static int showCalendar(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        long dayTime = level.getOverworldClockTime() % 24000L;
        long dayCount = level.getOverworldClockTime() / 24000L;
        String clockTime = VillagerStatsData.formatClockTime(dayTime);
        Component currentPhaseComponent = VillagerStatsData.getScheduledPhaseComponent(dayTime, false);
        boolean noRoam = VillagerAIConfig.get().disableRoaming;

        source.sendSuccess(() -> Component.translatable("evv.command.calendar.header"), false);
        source.sendSuccess(() -> Component.translatable("evv.command.calendar.time",
            clockTime, dayTime, dayCount, currentPhaseComponent), false);
        source.sendSuccess(() -> Component.translatable("evv.command.calendar.ai_mode",
            noRoam ? Component.translatable("evv.gui.mode.no_roam") : Component.translatable("evv.gui.mode.vanilla"),
            Component.translatable("evv.command.calendar.toggle_hint")), false);
        source.sendSuccess(() -> Component.translatable("evv.command.calendar.nearest_job",
            VillagerAIConfig.get().assignNearestVillager
                ? Component.translatable("evv.command.nearest_job.enabled")
                : Component.translatable("evv.command.nearest_job.disabled")), false);
        source.sendSuccess(() -> Component.literal("§7• ").append(Component.translatable("evv.gui.schedule.idle_morning")), false);
        source.sendSuccess(() -> Component.literal("§7• ").append(Component.translatable("evv.gui.schedule.work")), false);
        source.sendSuccess(() -> Component.literal("§7• ").append(Component.translatable("evv.gui.schedule.meet")), false);
        source.sendSuccess(() -> Component.literal("§7• ").append(Component.translatable("evv.gui.schedule.idle_evening")), false);
        source.sendSuccess(() -> Component.literal("§7• ").append(Component.translatable("evv.gui.schedule.rest")), false);
        source.sendSuccess(() -> Component.translatable("evv.command.calendar.footer"), false);

        return 1;
    }

    private static Component formatPos(Optional<GlobalPos> globalPos) {
        if (globalPos == null || globalPos.isEmpty()) {
            return Component.literal("§c").append(Component.translatable("evv.common.none"));
        }
        BlockPos pos = globalPos.get().pos();
        return Component.literal(String.format("§f[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ()));
    }
}
