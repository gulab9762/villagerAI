package com.blockplace.client.animation;

import com.blockplace.client.config.BlockPlaceConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class BlockFlyAnimation {

    private final BlockPos targetPos;
    private final BlockState blockState;
    private final Vec3 startPos;
    private final Vec3 targetCenter;
    private final float sideMultiplier;

    private int ageTicks = 0;
    private final int maxAgeTicks;
    private boolean finished = false;

    private final MovingBlockRenderState movingBlockState = new MovingBlockRenderState();

    public BlockFlyAnimation(ClientLevel level, BlockPos targetPos, BlockState blockState, Vec3 startPos, float sideMultiplier) {
        this.targetPos = targetPos;
        this.blockState = blockState;
        this.startPos = startPos;
        this.targetCenter = Vec3.atCenterOf(targetPos);
        this.sideMultiplier = sideMultiplier;
        this.maxAgeTicks = Math.max(1, BlockPlaceConfig.durationTicks);

        this.movingBlockState.randomSeedPos = targetPos;
        this.movingBlockState.blockPos = targetPos;
        this.movingBlockState.blockState = blockState;
        this.movingBlockState.biome = level.getBiome(targetPos);
        this.movingBlockState.cardinalLighting = level.cardinalLighting();
        this.movingBlockState.lightEngine = level.getLightEngine();
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public boolean isFinished() {
        return finished;
    }

    public void tick() {
        if (finished) return;

        ageTicks++;
        if (ageTicks >= maxAgeTicks) {
            finished = true;
        }
    }

    public float getProgress(float partialTicks) {
        return Math.min(1.0f, (ageTicks + partialTicks) / (float) maxAgeTicks);
    }

    public Vec3 getCurrentPosition(float progress) {
        float easeT = 1.0f - (float) Math.pow(1.0f - progress, 3);

        double x = Mth.lerp(easeT, startPos.x, targetCenter.x);
        double y = Mth.lerp(easeT, startPos.y, targetCenter.y);
        double z = Mth.lerp(easeT, startPos.z, targetCenter.z);

        double arc = BlockPlaceConfig.arcHeight * Math.sin(progress * Math.PI);
        y += arc;

        return new Vec3(x, y, z);
    }

    public float getCurrentScale(float progress) {
        float easeT = 1.0f - (float) Math.pow(1.0f - progress, 2);
        float scale = BlockPlaceConfig.startScale + (1.0f - BlockPlaceConfig.startScale) * easeT;

        if (BlockPlaceConfig.enableSettleBounce && progress > 0.75f) {
            float settlePhase = (progress - 0.75f) / 0.25f;
            float bounce = 0.07f * (float) Math.sin(settlePhase * Math.PI);
            scale += bounce;
        }

        return scale;
    }

    public void render(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState, float partialTicks, ClientLevel level) {
        if (blockState.getRenderShape() != RenderShape.MODEL) {
            return;
        }

        float progress = getProgress(partialTicks);
        Vec3 currentPos = getCurrentPosition(progress);
        float scale = getCurrentScale(progress);

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double renderX = currentPos.x - cameraPos.x();
        double renderY = currentPos.y - cameraPos.y();
        double renderZ = currentPos.z - cameraPos.z();

        poseStack.pushPose();

        poseStack.translate(renderX, renderY, renderZ);

        float remainingRot = (1.0f - progress);
        float tiltY = remainingRot * (BlockPlaceConfig.initialTiltDegrees * sideMultiplier);
        float tiltX = remainingRot * (BlockPlaceConfig.initialTiltDegrees * 0.6f);

        poseStack.rotateDegrees(Axis.YP, tiltY);
        poseStack.rotateDegrees(Axis.XP, tiltX);

        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5, -0.5, -0.5);

        BlockPos currentBlockPos = BlockPos.containing(currentPos);
        movingBlockState.blockPos = currentBlockPos;

        collector.submitMovingBlock(poseStack, movingBlockState, 0);

        poseStack.popPose();
    }
}
