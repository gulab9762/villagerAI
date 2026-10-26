package com.villagerai.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Comparator;
import java.util.List;

public class VillagerStatsClient {

    public static final KeyMapping KEY_MAPPING = new KeyMapping(
        "key.villagerai.stats",
        InputConstants.KEY_V,
        KeyMapping.Category.MISC
    );

    public static void openStatsScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Toggle behavior: pressing V when already open closes it
        if (mc.gui.screen() instanceof VillagerStatsScreen) {
            mc.gui.screen().onClose();
            return;
        }

        Villager target = findTargetVillager(mc.player);
        VillagerStatsData data;

        if (target != null) {
            Villager resolved = target;
            if (mc.getSingleplayerServer() != null) {
                ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(mc.level.dimension());
                if (serverLevel != null && serverLevel.getEntity(target.getId()) instanceof Villager serverVillager) {
                    resolved = serverVillager;
                }
            }
            data = VillagerStatsData.fromVillager(resolved, mc.level);
        } else {
            data = VillagerStatsData.fromSchedule(mc.level);
        }

        mc.gui.setScreen(new VillagerStatsScreen(data));
    }

    public static Villager findTargetVillager(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.crosshairPickEntity instanceof Villager v) {
            return v;
        }
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof Villager, 12.0);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Villager v) {
            return v;
        }
        return null;
    }
}
