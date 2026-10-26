package com.villagerai.client;

import com.villagerai.config.VillagerAIConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class VillagerStatsData {
    public boolean hasVillager;
    public String professionName;
    public int level;
    public int xp;
    public boolean isBaby;
    public boolean noRoam;
    public boolean assignNearest;
    public String clockTime;
    public long dayTime;
    public long dayCount;
    public String scheduledPhase;
    public String currentActivity;
    public String workstationPos;
    public String bedPos;
    public String bellPos;
    public String walkTargetInfo;

    public static VillagerStatsData fromVillager(Villager villager, Level level) {
        VillagerStatsData data = new VillagerStatsData();
        data.hasVillager = true;

        long clockTicks = level.getOverworldClockTime();
        data.dayTime = clockTicks % 24000L;
        data.dayCount = clockTicks / 24000L;
        data.clockTime = formatClockTime(data.dayTime);

        VillagerData villagerData = villager.getVillagerData();
        Holder<VillagerProfession> profHolder = villagerData.profession();
        data.professionName = capitalize(profHolder.getRegisteredName());
        data.level = villagerData.level();
        data.xp = villager.getVillagerXp();
        data.isBaby = villager.isBaby();

        data.noRoam = VillagerAIConfig.get().disableRoaming;
        data.assignNearest = VillagerAIConfig.get().assignNearestVillager;
        data.scheduledPhase = getScheduledPhase(data.dayTime, data.isBaby);

        Brain<Villager> brain = villager.getBrain();
        Optional<Activity> activity = brain.getActiveNonCoreActivity();
        data.currentActivity = activity.map(Activity::getName).orElse("idle").toUpperCase();

        data.workstationPos = formatPos(brain.getMemory(MemoryModuleType.JOB_SITE));
        data.bedPos = formatPos(brain.getMemory(MemoryModuleType.HOME));
        data.bellPos = formatPos(brain.getMemory(MemoryModuleType.MEETING_POINT));

        Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);
        if (walkTarget.isPresent()) {
            Vec3 target = walkTarget.get().getTarget().currentPosition();
            double dist = villager.position().distanceTo(target);
            data.walkTargetInfo = String.format("[%.1f, %.1f, %.1f] (%.1fm, spd %.2f)",
                target.x, target.y, target.z, dist, walkTarget.get().getSpeedModifier());
        } else {
            data.walkTargetInfo = data.noRoam ? "None (Stationary - No Roam)" : "None";
        }

        return data;
    }

    public static VillagerStatsData fromSchedule(Level level) {
        VillagerStatsData data = new VillagerStatsData();
        data.hasVillager = false;

        long clockTicks = level.getOverworldClockTime();
        data.dayTime = clockTicks % 24000L;
        data.dayCount = clockTicks / 24000L;
        data.clockTime = formatClockTime(data.dayTime);

        data.noRoam = VillagerAIConfig.get().disableRoaming;
        data.assignNearest = VillagerAIConfig.get().assignNearestVillager;
        data.scheduledPhase = getScheduledPhase(data.dayTime, false);
        return data;
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
        if (globalPos == null || globalPos.isEmpty()) {
            return "None";
        }
        BlockPos pos = globalPos.get().pos();
        return String.format("[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ());
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "None";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
