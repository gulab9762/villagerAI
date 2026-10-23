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

    // Orthogonal basis vectors for spiral path
    private final Vec3 spiralU;
    private final Vec3 spiralV;

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

        // Precompute orthogonal basis vectors perpendicular to flight line for spiral calculations
        Vec3 flightDir = this.targetCenter.subtract(startPos);
        if (flightDir.lengthSqr() < 1e-4) {
            flightDir = new Vec3(0, 1, 0);
        } else {
            flightDir = flightDir.normalize();
        }
        Vec3 arbitrary = Math.abs(flightDir.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        this.spiralU = flightDir.cross(arbitrary).normalize();
        this.spiralV = flightDir.cross(this.spiralU).normalize();

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
        BlockPlaceConfig.AnimationStyle style = BlockPlaceConfig.style;

        switch (style) {
            case POP:
            case EXPAND:
                // Stationary at target socket
                return targetCenter;

            case DROP: {
                // Starts 0.9 blocks directly above socket, falls with gravity acceleration
                if (progress < 0.82f) {
                    float t = progress / 0.82f;
                    float easeGravity = t * t; // Acceleration
                    double y = Mth.lerp(easeGravity, targetCenter.y + 0.9, targetCenter.y);
                    return new Vec3(targetCenter.x, y, targetCenter.z);
                } else {
                    // Slight rebound bounce on impact
                    float t = (progress - 0.82f) / 0.18f;
                    double bounce = 0.08 * Math.sin(t * Math.PI);
                    return new Vec3(targetCenter.x, targetCenter.y + bounce, targetCenter.z);
                }
            }

            case SPIRAL: {
                // Smooth travel along line + corkscrew helical offset
                float easeT = progress * progress * (3.0f - 2.0f * progress);
                double baseX = Mth.lerp(easeT, startPos.x, targetCenter.x);
                double baseY = Mth.lerp(easeT, startPos.y, targetCenter.y);
                double baseZ = Mth.lerp(easeT, startPos.z, targetCenter.z);

                double radius = 0.35 * (1.0 - progress) * Math.sin(progress * Math.PI);
                double angle = progress * 4.0 * Math.PI; // 2 complete vortex loops

                double offsetX = (spiralU.x * Math.cos(angle) + spiralV.x * Math.sin(angle)) * radius;
                double offsetY = (spiralU.y * Math.cos(angle) + spiralV.y * Math.sin(angle)) * radius;
                double offsetZ = (spiralU.z * Math.cos(angle) + spiralV.z * Math.sin(angle)) * radius;

                return new Vec3(baseX + offsetX, baseY + offsetY, baseZ + offsetZ);
            }

            case SPRING: {
                // Energetic trampoline trajectory with high arc and secondary landing hop
                float easeT = progress * progress * (3.0f - 2.0f * progress);
                double x = Mth.lerp(easeT, startPos.x, targetCenter.x);
                double y = Mth.lerp(easeT, startPos.y, targetCenter.y);
                double z = Mth.lerp(easeT, startPos.z, targetCenter.z);

                double highArc = 0.42 * Math.sin(progress * Math.PI);
                if (progress > 0.8f) {
                    double secondHop = 0.07 * Math.sin(((progress - 0.8f) / 0.2f) * Math.PI);
                    highArc += secondHop;
                }
                return new Vec3(x, y + highArc, z);
            }

            case SMOOTH:
            case ROTATING:
            default: {
                // Steady smoothstep trajectory across screen
                float easeT = progress * progress * (3.0f - 2.0f * progress);
                double x = Mth.lerp(easeT, startPos.x, targetCenter.x);
                double y = Mth.lerp(easeT, startPos.y, targetCenter.y);
                double z = Mth.lerp(easeT, startPos.z, targetCenter.z);

                if (BlockPlaceConfig.arcHeight > 0.0f) {
                    double arc = BlockPlaceConfig.arcHeight * Math.sin(progress * Math.PI);
                    y += arc;
                }
                return new Vec3(x, y, z);
            }
        }
    }

    public Vec3 getCurrentScale(float progress) {
        BlockPlaceConfig.AnimationStyle style = BlockPlaceConfig.style;

        switch (style) {
            case POP: {
                // Cartoon Squash & Stretch: flat -> tall stretch -> bouncy spring settling
                if (progress < 0.25f) {
                    float t = progress / 0.25f;
                    double sxz = 0.5 + 0.6 * t;
                    double sy = 0.15 + 0.4 * t;
                    return new Vec3(sxz, sy, sxz);
                } else if (progress < 0.65f) {
                    float t = (progress - 0.25f) / 0.40f;
                    double sxz = Mth.lerp(t, 1.10, 0.88);
                    double sy = Mth.lerp(t, 0.55, 1.25);
                    return new Vec3(sxz, sy, sxz);
                } else {
                    float t = (progress - 0.65f) / 0.35f;
                    double spring = Math.sin(t * Math.PI * 2.5) * Math.exp(-t * 3.0) * 0.22;
                    double sy = 1.0 + spring;
                    double sxz = 1.0 - spring * 0.5;
                    return new Vec3(sxz, sy, sxz);
                }
            }

            case DROP: {
                // Subtle stretch during gravity fall, squash on landing
                if (progress < 0.82f) {
                    float t = progress / 0.82f;
                    double sy = 1.0 + 0.10 * t;
                    double sxz = 1.0 - 0.05 * t;
                    return new Vec3(sxz, sy, sxz);
                } else {
                    float t = (progress - 0.82f) / 0.18f;
                    double squash = Math.sin(t * Math.PI) * 0.10;
                    return new Vec3(1.0 + squash, 1.0 - squash, 1.0 + squash);
                }
            }

            case EXPAND: {
                // Miniature voxel burst: starts at 0.06x, overshoots to 1.12x, settles squarely
                float t = progress * progress * (3.0f - 2.0f * progress);
                double scale = 0.06 + (1.0 - 0.06) * t;
                if (progress > 0.70f) {
                    float phase = (progress - 0.70f) / 0.30f;
                    double overshoot = 0.08 * Math.sin(phase * Math.PI);
                    scale += overshoot;
                }
                return new Vec3(scale, scale, scale);
            }

            case SPRING: {
                // Dynamic growth with bouncy rebound
                float easeT = progress * progress * (3.0f - 2.0f * progress);
                double scale = BlockPlaceConfig.startScale + (1.0 - BlockPlaceConfig.startScale) * easeT;
                if (progress > 0.80f) {
                    float phase = (progress - 0.80f) / 0.20f;
                    scale += 0.06 * Math.sin(phase * Math.PI);
                }
                return new Vec3(scale, scale, scale);
            }

            case SMOOTH:
            case ROTATING:
            case SPIRAL:
            default: {
                // Smooth growth from hand scale to 1.0 with subtle settle bounce
                float easeT = progress * progress * (3.0f - 2.0f * progress);
                double scale = BlockPlaceConfig.startScale + (1.0f - BlockPlaceConfig.startScale) * easeT;

                if (BlockPlaceConfig.enableSettleBounce && progress > 0.85f) {
                    float settlePhase = (progress - 0.85f) / 0.15f;
                    scale += 0.03f * Math.sin(settlePhase * Math.PI);
                }
                return new Vec3(scale, scale, scale);
            }
        }
    }

    public void render(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState, float partialTicks, ClientLevel level) {
        if (blockState.getRenderShape() != RenderShape.MODEL) {
            return;
        }

        float progress = getProgress(partialTicks);
        Vec3 currentPos = getCurrentPosition(progress);
        Vec3 scaleVec = getCurrentScale(progress);

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        double renderX = currentPos.x - cameraPos.x();
        double renderY = currentPos.y - cameraPos.y();
        double renderZ = currentPos.z - cameraPos.z();

        poseStack.pushPose();

        poseStack.translate(renderX, renderY, renderZ);

        float remainingRot = (1.0f - progress);
        BlockPlaceConfig.AnimationStyle style = BlockPlaceConfig.style;

        switch (style) {
            case ROTATING: {
                // Dynamic 3D tumbling spin on X, Y, Z decelerating cleanly to 0 on landing
                float spinProgress = remainingRot * remainingRot;
                float spinY = spinProgress * 360.0f * sideMultiplier;
                float spinX = spinProgress * 360.0f;
                float spinZ = spinProgress * 180.0f * sideMultiplier;

                poseStack.rotateDegrees(Axis.YP, spinY);
                poseStack.rotateDegrees(Axis.XP, spinX);
                poseStack.rotateDegrees(Axis.ZP, spinZ);
                break;
            }

            case SPIRAL: {
                // Rolls along the direction of corkscrew movement
                float rollAngle = (1.0f - progress) * 720.0f * sideMultiplier;
                poseStack.rotateDegrees(Axis.YP, rollAngle * 0.5f);
                poseStack.rotateDegrees(Axis.ZP, rollAngle * 0.5f);
                break;
            }

            case SPRING: {
                // Playful forward pitch roll
                float rollX = remainingRot * 360.0f;
                poseStack.rotateDegrees(Axis.XP, rollX);
                break;
            }

            case EXPAND: {
                // 45 degree twist that unwinds squarely
                float twistY = remainingRot * 45.0f * sideMultiplier;
                poseStack.rotateDegrees(Axis.YP, twistY);
                break;
            }

            case POP:
            case DROP: {
                // Square to grid, no rotation
                break;
            }

            case SMOOTH:
            default: {
                // Subtle hand tilt easing
                float tiltY = remainingRot * (BlockPlaceConfig.initialTiltDegrees * sideMultiplier);
                float tiltX = remainingRot * (BlockPlaceConfig.initialTiltDegrees * 0.6f);

                poseStack.rotateDegrees(Axis.YP, tiltY);
                poseStack.rotateDegrees(Axis.XP, tiltX);
                break;
            }
        }

        poseStack.scale((float) scaleVec.x, (float) scaleVec.y, (float) scaleVec.z);
        poseStack.translate(-0.5, -0.5, -0.5);

        BlockPos currentBlockPos = BlockPos.containing(currentPos);
        movingBlockState.blockPos = currentBlockPos;

        collector.submitMovingBlock(poseStack, movingBlockState, 0);

        poseStack.popPose();
    }
}
