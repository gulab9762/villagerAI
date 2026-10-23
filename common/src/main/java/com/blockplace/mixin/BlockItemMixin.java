package com.blockplace.mixin;

import com.blockplace.client.animation.BlockPlacementManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Unique
    private final ThreadLocal<BlockState> blockplace$capturedReplacedState = new ThreadLocal<>();

    @Inject(
            method = "placeBlock",
            at = @At("HEAD")
    )
    private void blockplace$beforePlaceBlock(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (context.getLevel().isClientSide()) {
            BlockPos pos = context.getClickedPos();
            BlockState replaced = context.getLevel().getBlockState(pos);
            blockplace$capturedReplacedState.set(replaced);
        }
    }

    @Inject(
            method = "placeBlock",
            at = @At("RETURN")
    )
    private void blockplace$afterPlaceBlock(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        BlockState replaced = blockplace$capturedReplacedState.get();
        blockplace$capturedReplacedState.remove();

        if (cir.getReturnValue() && context.getLevel().isClientSide()) {
            Player player = context.getPlayer();
            if (player != null && player.isLocalPlayer()) {
                BlockPos pos = context.getClickedPos();
                BlockPlacementManager.onBlockPlaced(
                        (ClientLevel) context.getLevel(),
                        player,
                        context.getHand(),
                        pos,
                        state,
                        replaced != null ? replaced : Blocks.AIR.defaultBlockState()
                );
            }
        }
    }
}
