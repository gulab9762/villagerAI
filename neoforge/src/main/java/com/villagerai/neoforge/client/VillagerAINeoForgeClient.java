package com.villagerai.neoforge.client;

import com.villagerai.Constants;
import com.villagerai.client.VillagerStatsClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class VillagerAINeoForgeClient {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(VillagerStatsClient.KEY_MAPPING);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (VillagerStatsClient.KEY_MAPPING.consumeClick()) {
            VillagerStatsClient.openStatsScreen();
        }
    }
}
