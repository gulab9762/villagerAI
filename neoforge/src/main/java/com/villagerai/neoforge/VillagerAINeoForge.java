package com.villagerai.neoforge;

import com.villagerai.Constants;
import com.villagerai.VillagerAICommon;
import com.villagerai.command.VillagerScheduleCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(Constants.MOD_ID)
public class VillagerAINeoForge {
    public VillagerAINeoForge(IEventBus modEventBus) {
        VillagerAICommon.init();
        Constants.LOG.info("{} NeoForge initialized successfully!", Constants.MOD_NAME);
    }

    @EventBusSubscriber(modid = Constants.MOD_ID)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            VillagerScheduleCommand.register(event.getDispatcher());
        }
    }
}
