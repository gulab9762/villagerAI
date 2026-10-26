package com.villagerai.neoforge;

import com.villagerai.Constants;
import com.villagerai.VillagerAICommon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class VillagerAINeoForge {
    public VillagerAINeoForge(IEventBus modEventBus) {
        VillagerAICommon.init();
        Constants.LOG.info("{} NeoForge initialized successfully!", Constants.MOD_NAME);
    }
}
