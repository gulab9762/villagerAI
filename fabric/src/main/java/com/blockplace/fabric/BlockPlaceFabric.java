package com.blockplace.fabric;

import com.blockplace.BlockPlaceCommon;
import net.fabricmc.api.ModInitializer;

public class BlockPlaceFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BlockPlaceCommon.init();
    }
}
