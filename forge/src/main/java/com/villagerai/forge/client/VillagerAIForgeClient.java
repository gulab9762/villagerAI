package com.villagerai.forge.client;

import com.villagerai.client.VillagerStatsClient;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

public class VillagerAIForgeClient {

    public static void init() {
        RegisterKeyMappingsEvent.BUS.addListener(VillagerAIForgeClient::onRegisterKeyMappings);
        TickEvent.ClientTickEvent.Post.BUS.addListener(VillagerAIForgeClient::onClientTick);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(VillagerStatsClient.KEY_MAPPING);
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        while (VillagerStatsClient.KEY_MAPPING.consumeClick()) {
            VillagerStatsClient.openStatsScreen();
        }
    }
}

