package com.heavenlynether.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class HeavenlyPortalActivator {
    public static InteractionResult onUseItemOnBlock(Player player, Level level, InteractionHand hand, BlockPos pos, Direction face) {
        if (hand != InteractionHand.MAIN_HAND && hand != InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        boolean isWaterBucket = held.is(Items.WATER_BUCKET);
        boolean isFlintAndSteel = held.is(Items.FLINT_AND_STEEL);

        if (!isWaterBucket && !isFlintAndSteel) {
            return InteractionResult.PASS;
        }

        BlockPos targetPos = pos;
        if (!level.getBlockState(targetPos).isAir()) {
            targetPos = pos.relative(face);
        }

        if (!level.getBlockState(targetPos).isAir()) {
            return InteractionResult.PASS;
        }

        if (HeavenlyPortalShape.tryIgnite(level, targetPos)) {
            if (!player.getAbilities().instabuild) {
                if (isWaterBucket) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                } else {
                    held.hurtAndBreak(1, player, hand);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
