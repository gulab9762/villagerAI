package com.blockplace.client.animation;

import com.blockplace.client.config.BlockPlaceConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.level.block.Blocks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockPlacementManager {

    private static final Map<BlockPos, BlockFlyAnimation> ACTIVE_ANIMATIONS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, BlockState> HIDDEN_BLOCKS = new ConcurrentHashMap<>();

    /**
     * Called when a block is successfully placed by the player.
     */
    public static void onBlockPlaced(ClientLevel level, Player player, InteractionHand hand, BlockPos pos, BlockState state, BlockState replacedState) {
        Minecraft client = Minecraft.getInstance();

        // If animations are disabled, keep pure vanilla placement
        if (BlockPlaceConfig.style == BlockPlaceConfig.AnimationStyle.NONE) {
            return;
        }

        // Check if first-person perspective requirement is met
        if (BlockPlaceConfig.firstPersonOnly && !client.options.getCameraType().isFirstPerson()) {
            return;
        }

        // Only animate model blocks
        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }

        // Calculate 3D starting position at the player's held hand
        Vec3 startHandPos = FirstPersonHandHelper.getFirstPersonHandPosition(player, hand);
        float sideMultiplier = FirstPersonHandHelper.getHandSideMultiplier(player, hand);

        BlockFlyAnimation animation = new BlockFlyAnimation(level, pos, state, startHandPos, sideMultiplier);

        // Hide the block at target destination in chunk rendering while it is flying,
        // by making RenderSectionRegion return the replaced state (air) to avoid any x-ray hole.
        HIDDEN_BLOCKS.put(pos, replacedState != null ? replacedState : Blocks.AIR.defaultBlockState());
        BlockFlyAnimation old = ACTIVE_ANIMATIONS.put(pos, animation);
        if (old != null) {
            // Replaced previous animation
        }
    }

    /**
     * Checks if a block at given position is currently hidden due to an active placement flight.
     */
    public static boolean isBlockHidden(BlockPos pos) {
        return HIDDEN_BLOCKS.containsKey(pos);
    }

    /**
     * Returns the replaced block state (usually Air) while the position is hidden.
     */
    public static BlockState getHiddenBlockReplacedState(BlockPos pos) {
        return HIDDEN_BLOCKS.get(pos);
    }

    /**
     * Called every client tick to update all active flight animations.
     */
    public static void tick(ClientLevel level) {
        if (ACTIVE_ANIMATIONS.isEmpty()) {
            return;
        }

        ACTIVE_ANIMATIONS.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            BlockFlyAnimation anim = entry.getValue();

            anim.tick();

            // If level block changed unexpectedly, abort animation
            if (level.getBlockState(pos) != anim.getBlockState()) {
                HIDDEN_BLOCKS.remove(pos);
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), Block.UPDATE_ALL_IMMEDIATE);
                return true;
            }

            if (anim.isFinished()) {
                // Animation landed at target block location!
                finishAnimation(level, pos, anim.getBlockState());
                return true;
            }

            return false;
        });
    }

    /**
     * Completes the flight animation: unhides the block, prompts chunk mesh rebuild,
     * and triggers arrival effects.
     */
    private static void finishAnimation(ClientLevel level, BlockPos pos, BlockState state) {
        HIDDEN_BLOCKS.remove(pos);

        // Notify client world that the block state is ready to render in baked chunk
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL_IMMEDIATE);

        // Optional subtle arrival particles
        if (BlockPlaceConfig.enableArrivalParticles) {
            Vec3 center = Vec3.atCenterOf(pos);
            for (int i = 0; i < 4; i++) {
                double offsetX = (level.getRandom().nextDouble() - 0.5) * 0.4;
                double offsetY = (level.getRandom().nextDouble() - 0.5) * 0.2;
                double offsetZ = (level.getRandom().nextDouble() - 0.5) * 0.4;
                level.addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        center.x + offsetX,
                        pos.getY() + 0.1,
                        center.z + offsetZ,
                        offsetX * 0.5,
                        0.05,
                        offsetZ * 0.5
                );
            }
        }
    }

    /**
     * Renders all active flying blocks in the world.
     */
    public static void renderAll(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState) {
        if (ACTIVE_ANIMATIONS.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null) return;

        float partialTicks = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        for (BlockFlyAnimation anim : ACTIVE_ANIMATIONS.values()) {
            anim.render(poseStack, collector, levelRenderState, partialTicks, level);
        }
    }

    /**
     * Clears all animations (e.g. on disconnect or world unload).
     */
    public static void clear() {
        ACTIVE_ANIMATIONS.clear();
        HIDDEN_BLOCKS.clear();
    }
}
