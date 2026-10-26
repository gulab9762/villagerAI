package com.villagerai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

import com.villagerai.config.VillagerAIConfig;

public class VillagerScheduleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register /villagerschedule and alias /villagerSchedule
        registerNodes(dispatcher, "villagerschedule");
        registerNodes(dispatcher, "villagerSchedule");
    }

    private static void registerNodes(CommandDispatcher<CommandSourceStack> dispatcher, String literal) {
        dispatcher.register(
            Commands.literal(literal)
                .executes(VillagerScheduleCommand::executeSmart)
                .then(
                    Commands.literal("status")
                        .executes(VillagerScheduleCommand::executeStatus)
                )
                .then(
                    Commands.literal("calendar")
                        .executes(VillagerScheduleCommand::executeCalendar)
                )
                .then(
                    Commands.literal("toggle-roam")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(VillagerScheduleCommand::executeToggleRoam)
                )
                .then(
                    Commands.literal("reload")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(VillagerScheduleCommand::executeReload)
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

        if (config.disableRoaming) {
            source.sendSuccess(() -> Component.literal("§a[Villager AI] §fCustom AI: Villager roaming is now §cDISABLED §f(villagers stay put)."), true);
        } else {
            source.sendSuccess(() -> Component.literal("§e[Villager AI] §fVanilla AI: Villager roaming is now §aENABLED §f(vanilla wandering active)."), true);
        }
        return 1;
    }

    private static int executeReload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        VillagerAIConfig.load();
        boolean noRoam = VillagerAIConfig.get().disableRoaming;
        source.sendSuccess(() -> Component.literal("§a[Villager AI] §fConfig reloaded. Roaming override: " +
            (noRoam ? "§aActive (No Roam)" : "§eInactive (Vanilla Roam)")), true);
        return 1;
    }

    private static Villager findTargetVillager(ServerPlayer player) {
        // 1. Raycast line of sight up to 12 blocks
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof Villager, 12.0);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Villager villager) {
            return villager;
        }

        // 2. Nearest villager within 16 blocks radius
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
        String clockTime = formatClockTime(dayTime);
        String scheduledPhase = getScheduledPhase(dayTime, villager.isBaby());

        VillagerData data = villager.getVillagerData();
        Holder<VillagerProfession> profHolder = data.profession();
        String professionName = profHolder.unwrapKey()
            .map(k -> k.identifier().getPath())
            .orElse("none");

        Brain<Villager> brain = villager.getBrain();
        Optional<Activity> activeActivity = brain.getActiveNonCoreActivity();
        String currentActivity = activeActivity.map(Activity::getName).orElse("none");

        Optional<GlobalPos> jobSite = brain.getMemory(MemoryModuleType.JOB_SITE);
        Optional<GlobalPos> home = brain.getMemory(MemoryModuleType.HOME);
        Optional<GlobalPos> meeting = brain.getMemory(MemoryModuleType.MEETING_POINT);
        Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);

        boolean noRoam = VillagerAIConfig.get().disableRoaming;

        source.sendSuccess(() -> Component.literal("§6══════════ §e§lVillager AI: Status §6══════════"), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• Villager: §f%s §7(Level %d, %s)",
            capitalize(professionName),
            data.level(),
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
        String clockTime = formatClockTime(dayTime);
        String currentPhase = getScheduledPhase(dayTime, false);
        boolean noRoam = VillagerAIConfig.get().disableRoaming;

        source.sendSuccess(() -> Component.literal("§6══════════ §e§lVillager Daily Schedule §6══════════"), false);
        source.sendSuccess(() -> Component.literal(String.format("§7Current Time: §e%s §7(Tick %d/24000, Day %d) §8[Active: §a%s§8]",
            clockTime, dayTime, dayCount, currentPhase)), false);
        source.sendSuccess(() -> Component.literal(String.format("§7• AI Mode: %s §7(/villagerschedule toggle-roam to change)",
            noRoam ? "§aNo-Roam Active (Config ON)" : "§eVanilla Roaming Active (Config OFF)")), false);
        source.sendSuccess(() -> Component.literal("§7• §e06:00 - 08:00 §7(10 - 2000t):   §fIDLE §8(Wake up & morning leisure)"), false);
        source.sendSuccess(() -> Component.literal("§7• §a08:00 - 15:00 §7(2000 - 9000t):  §fWORK §8(Working at workstation)"), false);
        source.sendSuccess(() -> Component.literal("§7• §d15:00 - 17:00 §7(9000 - 11000t): §fMEET §8(Gathering at village bell)"), false);
        source.sendSuccess(() -> Component.literal("§7• §e17:00 - 18:00 §7(11000 - 12000t): §fIDLE §8(Evening leisure)"), false);
        source.sendSuccess(() -> Component.literal("§7• §918:00 - 06:00 §7(12000 - 24000t): §fREST §8(Sleeping in bed)"), false);
        source.sendSuccess(() -> Component.literal("§6═══════════════════════════════════════════════════"), false);

        return 1;
    }

    private static String formatClockTime(long dayTime) {
        long totalMinutes = ((dayTime + 6000L) % 24000L) * 60L / 1000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        return String.format("%02d:%02d", hours, minutes);
    }

    private static String getScheduledPhase(long dayTime, boolean isBaby) {
        if (isBaby) {
            if (dayTime < 3000L) return "IDLE (06:00 - 09:00)";
            if (dayTime < 6000L) return "PLAY (09:00 - 12:00)";
            if (dayTime < 10000L) return "IDLE (12:00 - 16:00)";
            if (dayTime < 12000L) return "PLAY (16:00 - 18:00)";
            return "REST (18:00 - 06:00)";
        } else {
            if (dayTime < 2000L) return "IDLE (06:00 - 08:00)";
            if (dayTime < 9000L) return "WORK (08:00 - 15:00)";
            if (dayTime < 11000L) return "MEET (15:00 - 17:00)";
            if (dayTime < 12000L) return "IDLE (17:00 - 18:00)";
            return "REST (18:00 - 06:00)";
        }
    }

    private static String formatPos(Optional<GlobalPos> globalPos) {
        if (globalPos.isEmpty()) {
            return "§cNone";
        }
        BlockPos pos = globalPos.get().pos();
        return String.format("§f[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "None";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
