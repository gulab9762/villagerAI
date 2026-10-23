package com.blockplace.client.animation;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class FirstPersonHandHelper {

    /**
     * Calculates the 3D world coordinates of the player's held hand in first-person view.
     *
     * @param player the player placing the block
     * @param hand   the interaction hand used (main hand or off hand)
     * @return 3D position vector in world coordinates where the held item is located
     */
    public static Vec3 getFirstPersonHandPosition(Player player, InteractionHand hand) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 forward = player.getLookAngle();

        float yaw = player.getYRot();
        double yawRad = Math.toRadians(yaw);

        // Perpendicular horizontal right vector based on camera yaw
        Vec3 right = new Vec3(-Math.sin(yawRad - Math.PI / 2.0), 0, Math.cos(yawRad - Math.PI / 2.0)).normalize();

        // Local up vector orthogonal to both forward and right vectors
        Vec3 up = right.cross(forward).normalize();

        // Determine which side (right = +1, left = -1)
        boolean isRightSide = (player.getMainArm() == HumanoidArm.RIGHT);
        if (hand == InteractionHand.OFF_HAND) {
            isRightSide = !isRightSide;
        }
        double sideMultiplier = isRightSide ? 1.0 : -1.0;

        // Visual offsets tuned for first-person perspective:
        // - Right/Left: offset toward the hand
        // - Down: positioned below eye level
        // - Forward: positioned in front of player
        double sideOffset = 0.36 * sideMultiplier;
        double downOffset = -0.28;
        double forwardOffset = 0.40;

        return eyePos
                .add(right.scale(sideOffset))
                .add(up.scale(downOffset))
                .add(forward.scale(forwardOffset));
    }

    /**
     * Returns the side multiplier (+1.0 for right hand, -1.0 for left hand).
     */
    public static float getHandSideMultiplier(Player player, InteractionHand hand) {
        boolean isRightSide = (player.getMainArm() == HumanoidArm.RIGHT);
        if (hand == InteractionHand.OFF_HAND) {
            isRightSide = !isRightSide;
        }
        return isRightSide ? 1.0f : -1.0f;
    }
}
