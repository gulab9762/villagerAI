package com.villagerai.fabric;

import com.villagerai.Constants;
import com.villagerai.VillagerAICommon;
import net.fabricmc.api.ModInitializer;

public class VillagerAIFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        VillagerAICommon.init();
        Constants.LOG.info("{} Fabric initialized successfully!", Constants.MOD_NAME);
    }
}
