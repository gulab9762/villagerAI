package com.villagerai.fabric.client;

import com.villagerai.client.VillagerStatsClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public class VillagerAIFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(VillagerStatsClient.KEY_MAPPING);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (VillagerStatsClient.KEY_MAPPING.consumeClick()) {
                VillagerStatsClient.openStatsScreen();
            }
        });
    }
}
