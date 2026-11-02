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
                source.sendFailure(Component.literal("§cNo villager found in crosshairs or within 16 blocks."));
                return showCalendar(source);
            }
        } else {
            source.sendFailure(Component.literal("§cOnly in-game players can target a villager."));
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
            source.sendSuccess(() -> Component.literal(String.format("§a[Easy Vanilla Villagers] §fCustom AI: Villager roaming is now §cDISABLED §f(villagers stay put). Refreshed %d villager(s).", count)), true);
        } else {
            source.sendSuccess(() -> Component.literal(String.format("§e[Easy Vanilla Villagers] §fVanilla AI: Villager roaming is now §aENABLED §f(vanilla wandering active). Refreshed %d villager(s).", count)), true);
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
        String profName = VillagerStatsData.capitalize(professionHolder.getRegisteredName());
        int levelNum = data.level();
        int xp = villager.getVillagerXp();

        Brain<Villager> brain = villager.getBrain();
        Optional<Activity> activeActivity = brain.getActiveNonCoreActivity();
        String currentActivity = activeActivity.map(Activity::getName).orElse("none");
        String scheduledPhase = VillagerStatsData.getScheduledPhase(dayTime, villager.isBaby());

        Optional<GlobalPos> jobSite = brain.getMemory(MemoryModuleType.JOB_SITE);
        Optional<GlobalPos> home = brain.getMemory(MemoryModuleType.HOME);
        Optional<GlobalPos> meeting = brain.getMemory(MemoryModuleType.MEETING_POINT);
        Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);

        boolean noRoam = VillagerAIConfig.get().disableRoaming;

        source.sendSuccess(() -> Component.literal("§6══════════ §e§lVillager Status §6══════════"), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Profession: §f%s §7(Level %d, %d XP) §8[%s§8]",
            profName,
            levelNum,
            xp,
            villager.isBaby() ? "§dBaby" : "§aAdult"
        )), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• AI Mode: %s",
            noRoam ? "§aNo-Roam Active (Custom AI)" : "§eVanilla Roaming Active")), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Time: §e%s §7(Tick %d/24000, Day %d)", clockTime, dayTime, dayCount)), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Scheduled Phase: §a%s", scheduledPhase)), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Brain Activity: §b%s", currentActivity.toUpperCase())), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Workstation: %s", formatPos(jobSite))), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Bed: %s", formatPos(home))), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Meeting Bell: %s", formatPos(meeting))), false);

        if (walkTarget.isPresent()) {
            Vec3 targetPos = walkTarget.get().getTarget().currentPosition();
            double dist = villager.position().distanceTo(targetPos);
            source.sendSuccess(() -> Component.literal(String.format("§7• Walk Target: §f[%.1f, %.1f, %.1f] §7(%.1fm away, speed %.2f)",
                targetPos.x, targetPos.y, targetPos.z, dist, walkTarget.get().getSpeedModifier())), false);
        } else {
            source.sendSuccess(() -> Component.literal(noRoam
                ? "§7• Walk Target: §aNone (Stationary - No Roam)"
                : "§7• Walk Target: §7None"), false);
        }
        source.sendSuccess(() -> Component.literal("§6═════════════════════════════════════════════"), false);

        return 1;
    }

    private static int showCalendar(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        long dayTime = level.getOverworldClockTime() % 24000L;
        long dayCount = level.getOverworldClockTime() / 24000L;
        String clockTime = VillagerStatsData.formatClockTime(dayTime);
        String currentPhase = VillagerStatsData.getScheduledPhase(dayTime, false);
        boolean noRoam = VillagerAIConfig.get().disableRoaming;

        source.sendSuccess(() -> Component.literal("§6══════════ §e§lVillager Daily Schedule §6══════════"), false);
        source.sendSuccess(() -> Component.literal(String.format("§7Current Time: §e%s §7(Tick %d/24000, Day %d) §8[Active: §a%s§8]",
            clockTime, dayTime, dayCount, currentPhase)), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• AI Mode: %s §7(/evv toggle-roam to change)",
            noRoam ? "§aNo-Roam Active (Config ON)" : "§eVanilla Roaming Active (Config OFF)")), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Nearest Job Assignment: %s",
            VillagerAIConfig.get().assignNearestVillager ? "§aEnabled (Nearest villager claims on place & gen)" : "§eDisabled (Vanilla)")), false);
        source.sendSuccess(() -> Component.literal("§7• §e06:00 - 08:00 §7(10 - 2000t):   §fIDLE §8(Wake up & morning leisure)"), false);
        source.sendSuccess(() -> Component.literal("§7• §a08:00 - 15:00 §7(2000 - 9000t):  §fWORK §8(Working at workstation)"), false);
        source.sendSuccess(() -> Component.literal("§7• §d15:00 - 17:00 §7(9000 - 11000t): §fMEET §8(Gathering at village bell)"), false);
        source.sendSuccess(() -> Component.literal("§7• §e17:00 - 18:00 §7(11000 - 12000t): §fIDLE §8(Evening leisure)"), false);
        source.sendSuccess(() -> Component.literal("§7• §918:00 - 06:00 §7(12000 - 24000t): §fREST §8(Sleeping in bed)"), false);
        source.sendSuccess(() -> Component.literal("§6═══════════════════════════════════════════════════"), false);

        return 1;
    }

    private static String formatPos(Optional<GlobalPos> globalPos) {
        if (globalPos.isEmpty()) {
            return "§cNone";
        }
        BlockPos pos = globalPos.get().pos();
        return String.format("§f[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
    }
}
