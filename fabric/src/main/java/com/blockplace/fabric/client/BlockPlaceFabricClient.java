package com.blockplace.fabric.client;

import com.blockplace.Constants;
import com.blockplace.client.animation.BlockPlacementManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class BlockPlaceFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Constants.LOG.info("Initializing {} on Fabric Client", Constants.MOD_NAME);

        // Tick active block placement animations every client game tick
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && !client.isPaused()) {
                BlockPlacementManager.tick(client.level);
            }
        });

        // Reset flight animations on disconnect
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BlockPlacementManager.clear();
        });
    }
}
