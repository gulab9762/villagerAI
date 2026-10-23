package com.blockplace.mixin;

import com.blockplace.client.animation.BlockPlacementManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {

    @ModifyExpressionValue(
            method = "compile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"
            )
    )
    private RenderShape blockplace$hideBlockInChunk(RenderShape original, @Local BlockPos pos) {
        if (original != RenderShape.INVISIBLE && BlockPlacementManager.isBlockHidden(pos)) {
            return RenderShape.INVISIBLE;
        }
        return original;
    }
}
