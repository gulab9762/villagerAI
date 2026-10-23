package com.blockplace.neoforge;

import com.blockplace.BlockPlaceCommon;
import com.blockplace.Constants;
import com.blockplace.client.animation.BlockPlacementManager;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@Mod(Constants.MOD_ID)
public class BlockPlaceNeoForge {

    public BlockPlaceNeoForge() {
        BlockPlaceCommon.init();
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft client = Minecraft.getInstance();
            if (client.level != null && !client.isPaused()) {
                BlockPlacementManager.tick(client.level);
            }
        }
    }
}
