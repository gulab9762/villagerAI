package com.villagerai.quilt;

import com.villagerai.Constants;
import com.villagerai.VillagerAICommon;
import com.villagerai.command.VillagerStatsCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class VillagerAIQuilt implements ModInitializer {
    private static boolean initialized = false;

    @Override
    public void onInitialize() {
        if (initialized) return;
        initialized = true;

        VillagerAICommon.init();
        Constants.LOG.info("{} Quilt initialized successfully!", Constants.MOD_NAME);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            VillagerStatsCommand.register(dispatcher);
        });
    }
}

