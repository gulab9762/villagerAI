package com.villagerai;

import com.villagerai.config.VillagerAIConfig;

public class VillagerAICommon {
    public static void init() {
        Constants.LOG.info("Initializing {}", Constants.MOD_NAME);
        VillagerAIConfig.load();
    }
}
