package com.heavenlynether.block;

import com.heavenlynether.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class HeavenlyPortalShape {
    public static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 21;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 21;

    private final BlockPos bottomLeftAir;
    private final int width;
    private final int height;
    private final Direction.Axis axis;
    private final Direction rightDir;

    public HeavenlyPortalShape(BlockPos bottomLeftAir, int width, int height, Direction.Axis axis, Direction rightDir) {
        this.bottomLeftAir = bottomLeftAir;
        this.width = width;
        this.height = height;
        this.axis = axis;
        this.rightDir = rightDir;
    }

    public static boolean isFrameBlock(BlockState state) {
        return state.is(Blocks.QUARTZ_BRICKS)
                || state.is(Blocks.SMOOTH_QUARTZ)
                || state.is(Blocks.QUARTZ_BLOCK)
                || state.is(Blocks.CHISELED_QUARTZ_BLOCK)
                || state.is(Blocks.GLOWSTONE);
    }

    private static boolean isAirOrPortal(BlockState state) {
        return state.isAir() || state.is(HeavenlyNetherBlocks.HEAVENLY_PORTAL);
    }

    public static boolean tryIgnite(Level level, BlockPos startPos) {
        Optional<HeavenlyPortalShape> shape = findShape(level, startPos, Direction.Axis.X);
        if (shape.isEmpty()) {
            shape = findShape(level, startPos, Direction.Axis.Z);
        }

        if (shape.isPresent()) {
            HeavenlyPortalShape portal = shape.get();
            portal.createPortalBlocks(level);
            return true;
        }
        return false;
    }

    public static Optional<HeavenlyPortalShape> findShape(Level level, BlockPos startAirPos, Direction.Axis axis) {
        Direction rightDir = (axis == Direction.Axis.X) ? Direction.EAST : Direction.SOUTH;
        Direction leftDir = rightDir.getOpposite();

        // 1. Trace down to find bottom air block above frame
        BlockPos current = startAirPos;
        int downSteps = 0;
        while (downSteps <= MAX_HEIGHT) {
            BlockState stateBelow = level.getBlockState(current.below());
            if (isFrameBlock(stateBelow)) {
                break;
            }
            if (!isAirOrPortal(stateBelow)) {
                return Optional.empty();
            }
            current = current.below();
            downSteps++;
        }
        if (downSteps > MAX_HEIGHT) {
            return Optional.empty();
        }
        BlockPos bottomAirInCol = current;

        // 2. Trace left to find left frame pillar
        BlockPos leftAir = bottomAirInCol;
        int leftSteps = 0;
        while (leftSteps <= MAX_WIDTH) {
            BlockPos nextLeft = leftAir.relative(leftDir);
            BlockState nextState = level.getBlockState(nextLeft);
            if (isFrameBlock(nextState)) {
                break;
            }
            if (!isAirOrPortal(nextState) || !isFrameBlock(level.getBlockState(nextLeft.below()))) {
                return Optional.empty();
            }
            leftAir = nextLeft;
            leftSteps++;
        }
        if (leftSteps > MAX_WIDTH) {
            return Optional.empty();
        }
        BlockPos bottomLeftAir = leftAir;

        // 3. Trace right to find total width and ensure bottom frame exists
        BlockPos rightAir = bottomLeftAir;
        int width = 0;
        while (width <= MAX_WIDTH) {
            if (!isAirOrPortal(level.getBlockState(rightAir)) || !isFrameBlock(level.getBlockState(rightAir.below()))) {
                return Optional.empty();
            }
            BlockPos nextRight = rightAir.relative(rightDir);
            BlockState nextState = level.getBlockState(nextRight);
            width++;
            if (isFrameBlock(nextState)) {
                break;
            }
            rightAir = nextRight;
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            return Optional.empty();
        }

        // 4. Measure height from leftmost column
        int height = 0;
        BlockPos colPos = bottomLeftAir;
        while (height <= MAX_HEIGHT) {
            if (!isAirOrPortal(level.getBlockState(colPos))) {
                return Optional.empty();
            }
            BlockPos nextUp = colPos.above();
            BlockState nextState = level.getBlockState(nextUp);
            height++;
            if (isFrameBlock(nextState)) {
                break;
            }
            colPos = nextUp;
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            return Optional.empty();
        }

        // 5. Verify rectangular interior and top frame
        for (int w = 0; w < width; w++) {
            BlockPos base = bottomLeftAir.relative(rightDir, w);
            for (int h = 0; h < height; h++) {
                if (!isAirOrPortal(level.getBlockState(base.above(h)))) {
                    return Optional.empty();
                }
            }
            if (!isFrameBlock(level.getBlockState(base.above(height)))) {
                return Optional.empty();
            }
        }

        // 6. Verify left and right frame pillars
        for (int h = 0; h < height; h++) {
            BlockPos leftPillar = bottomLeftAir.relative(leftDir).above(h);
            if (!isFrameBlock(level.getBlockState(leftPillar))) {
                return Optional.empty();
            }
            BlockPos rightPillar = bottomLeftAir.relative(rightDir, width).above(h);
            if (!isFrameBlock(level.getBlockState(rightPillar))) {
                return Optional.empty();
            }
        }

        return Optional.of(new HeavenlyPortalShape(bottomLeftAir, width, height, axis, rightDir));
    }

    public void createPortalBlocks(Level level) {
        BlockState portalState = HeavenlyNetherBlocks.HEAVENLY_PORTAL.defaultBlockState().setValue(HeavenlyPortalBlock.AXIS, this.axis);
        for (int w = 0; w < this.width; w++) {
            for (int h = 0; h < this.height; h++) {
                BlockPos pos = this.bottomLeftAir.relative(this.rightDir, w).above(h);
                level.setBlock(pos, portalState, 3);
            }
        }

        level.playSound(
                null,
                this.bottomLeftAir,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
        level.playSound(
                null,
                this.bottomLeftAir,
                SoundEvents.PORTAL_TRIGGER,
                SoundSource.BLOCKS,
                0.6F,
                1.4F
        );

        if (level instanceof ServerLevel serverLevel) {
            for (int w = 0; w < this.width; w++) {
                for (int h = 0; h < this.height; h++) {
                    BlockPos p = this.bottomLeftAir.relative(this.rightDir, w).above(h);
                    serverLevel.sendParticles(
                            ParticleTypes.END_ROD,
                            p.getX() + 0.5,
                            p.getY() + 0.5,
                            p.getZ() + 0.5,
                            3,
                            0.2, 0.2, 0.2,
                            0.02
                    );
                }
            }
        }
    }

    public static TeleportTransition createTeleportTransition(ServerLevel currentLevel, ServerLevel targetLevel, Entity entity, BlockPos portalPos) {
        double scale = DimensionType.getTeleportationScale(currentLevel.dimensionType(), targetLevel.dimensionType());
        WorldBorder border = targetLevel.getWorldBorder();
        double targetX = Mth.clamp(entity.getX() * scale, border.getMinX() + 16, border.getMaxX() - 16);
        double targetZ = Mth.clamp(entity.getZ() * scale, border.getMinZ() + 16, border.getMaxZ() - 16);
        int blockX = Mth.floor(targetX);
        int blockZ = Mth.floor(targetZ);

        BlockPos existingPortal = findExistingPortal(targetLevel, blockX, blockZ, 64);
        if (existingPortal != null) {
            Vec3 spawnPos = new Vec3(existingPortal.getX() + 0.5, existingPortal.getY(), existingPortal.getZ() + 0.5);
            return new TeleportTransition(targetLevel, spawnPos, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND);
        }

        BlockPos generatedSpawn = generateExitPortal(targetLevel, blockX, blockZ);
        Vec3 spawnPos = new Vec3(generatedSpawn.getX() + 0.5, generatedSpawn.getY(), generatedSpawn.getZ() + 0.5);
        return new TeleportTransition(targetLevel, spawnPos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND);
    }

    public static BlockPos findExistingPortal(ServerLevel level, int targetX, int targetZ, int radius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY() + 5, 10);
        int maxY = Math.min(level.getMaxY() - 5, 250);

        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                int cx = targetX + dx;
                int cz = targetZ + dz;
                if (!level.getWorldBorder().isWithinBounds(cx, cz)) {
                    continue;
                }
                for (int y = minY; y <= maxY; y++) {
                    mutable.set(cx, y, cz);
                    BlockState state = level.getBlockState(mutable);
                    if (state.is(HeavenlyNetherBlocks.HEAVENLY_PORTAL)) {
                        BlockPos bottom = mutable.immutable();
                        while (level.getBlockState(bottom.below()).is(HeavenlyNetherBlocks.HEAVENLY_PORTAL)) {
                            bottom = bottom.below();
                        }
                        double distSq = (cx - targetX) * (cx - targetX) + (cz - targetZ) * (cz - targetZ);
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            closest = bottom;
                        }
                        break;
                    }
                }
            }
        }
        return closest;
    }

    public static BlockPos generateExitPortal(ServerLevel level, int targetX, int targetZ) {
        boolean isHeavenly = level.dimension().identifier().equals(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "heavenly_nether"));
        int safeY = -1;
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();

        if (isHeavenly) {
            for (int y = 70; y >= 35; y--) {
                testPos.set(targetX, y, targetZ);
                if (!level.getBlockState(testPos).isAir()
                        && level.getBlockState(testPos.above()).isAir()
                        && level.getBlockState(testPos.above(2)).isAir()
                        && level.getBlockState(testPos.above(3)).isAir()) {
                    safeY = y + 1;
                    break;
                }
            }
            if (safeY == -1) {
                for (int y = 71; y <= 110; y++) {
                    testPos.set(targetX, y, targetZ);
                    if (!level.getBlockState(testPos).isAir()
                            && level.getBlockState(testPos.above()).isAir()
                            && level.getBlockState(testPos.above(2)).isAir()) {
                        safeY = y + 1;
                        break;
                    }
                }
            }
            if (safeY == -1) {
                safeY = 70;
            }
        } else {
            safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
            if (safeY < level.getMinY() + 5) {
                safeY = 64;
            }
        }

        BlockState frameState = Blocks.QUARTZ_BRICKS.defaultBlockState();
        BlockState portalState = HeavenlyNetherBlocks.HEAVENLY_PORTAL.defaultBlockState().setValue(HeavenlyPortalBlock.AXIS, Direction.Axis.X);

        // 1. Build a solid 6x5 Quartz platform underneath at safeY - 1
        for (int x = -1; x <= 4; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(new BlockPos(targetX + x, safeY - 1, targetZ + z), frameState, 3);
            }
        }

        // 2. Clear safe air pocket around portal
        for (int x = -1; x <= 4; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    level.setBlock(new BlockPos(targetX + x, safeY + y, targetZ + z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        // 3. Build 4x5 Quartz Bricks frame (X: 0..3, Y: 0..4, Z: 0)
        for (int x = 0; x <= 3; x++) {
            level.setBlock(new BlockPos(targetX + x, safeY, targetZ), frameState, 3);       // bottom
            level.setBlock(new BlockPos(targetX + x, safeY + 4, targetZ), frameState, 3);   // top
        }
        for (int y = 1; y <= 3; y++) {
            level.setBlock(new BlockPos(targetX, safeY + y, targetZ), frameState, 3);       // left pillar
            level.setBlock(new BlockPos(targetX + 3, safeY + y, targetZ), frameState, 3);   // right pillar
        }

        // 4. Fill inner 2x3 area with Heavenly Portal
        for (int x = 1; x <= 2; x++) {
            for (int y = 1; y <= 3; y++) {
                level.setBlock(new BlockPos(targetX + x, safeY + y, targetZ), portalState, 3);
            }
        }

        return new BlockPos(targetX + 1, safeY + 1, targetZ);
    }
}
