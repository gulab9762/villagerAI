package com.villagerai.forge;

import com.villagerai.Constants;
import com.villagerai.VillagerAICommon;
import com.villagerai.command.VillagerStatsCommand;
import com.villagerai.forge.client.VillagerAIForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class VillagerAIForge {

    public VillagerAIForge() {
        VillagerAICommon.init();
        Constants.LOG.info("{} Forge initialized successfully!", Constants.MOD_NAME);

        RegisterCommandsEvent.BUS.addListener(event -> {
            VillagerStatsCommand.register(event.getDispatcher());
        });

        if (FMLEnvironment.dist == Dist.CLIENT) {
            VillagerAIForgeClient.init();
        }
    }
}

