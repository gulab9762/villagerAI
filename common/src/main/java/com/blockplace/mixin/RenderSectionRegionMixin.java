package com.blockplace.mixin;

import com.blockplace.client.animation.BlockPlacementManager;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin {

    @Inject(
            method = "getBlockState",
            at = @At("HEAD"),
            cancellable = true
    )
    private void blockplace$getHiddenBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        BlockState hiddenState = BlockPlacementManager.getHiddenBlockReplacedState(pos);
        if (hiddenState != null) {
            cir.setReturnValue(hiddenState);
        }
    }
}
