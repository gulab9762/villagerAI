package com.heavenlynether.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public class HeavenlyNetherBlocks {
    public static Block HEAVENLY_PORTAL;

    public static Block getHeavenlyPortal() {
        if (HEAVENLY_PORTAL != null) {
            return HEAVENLY_PORTAL;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(HeavenlyPortalBlock.PORTAL_KEY);
        if (block != null) {
            HEAVENLY_PORTAL = block;
            return block;
        }
        return null;
    }
}
