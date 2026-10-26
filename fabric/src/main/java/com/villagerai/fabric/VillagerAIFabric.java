package com.villagerai.fabric;

import com.villagerai.Constants;
import com.villagerai.VillagerAICommon;
import com.villagerai.command.VillagerStatsCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class VillagerAIFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        VillagerAICommon.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            VillagerStatsCommand.register(dispatcher);
        });

        Constants.LOG.info("{} Fabric initialized successfully!", Constants.MOD_NAME);
    }
}
