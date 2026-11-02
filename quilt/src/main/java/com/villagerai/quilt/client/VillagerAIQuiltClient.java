package com.villagerai.quilt.client;

import com.villagerai.client.VillagerStatsClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class VillagerAIQuiltClient implements ClientModInitializer {
    private static boolean initialized = false;

    @Override
    public void onInitializeClient() {
        if (initialized) return;
        initialized = true;

        KeyMappingHelper.registerKeyMapping(VillagerStatsClient.KEY_MAPPING);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (VillagerStatsClient.KEY_MAPPING.consumeClick()) {
                VillagerStatsClient.openStatsScreen();
            }
        });
    }
}
