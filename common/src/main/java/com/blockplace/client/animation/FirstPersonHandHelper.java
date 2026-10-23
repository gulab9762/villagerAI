package com.blockplace.client.animation;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public class FirstPersonHandHelper {

    /**
     * Calculates the 3D world coordinates of the player's held hand in first-person view.
     *
     * @param player the player placing the block
     * @param hand   the interaction hand used (main hand or off hand)
     * @return 3D position vector in world coordinates where the held item is located
     */
    public static Vec3 getFirstPersonHandPosition(Player player, InteractionHand hand) {
        Minecraft client = Minecraft.getInstance();
        Camera camera = client.gameRenderer.mainCamera();

        boolean isRightSide = (player.getMainArm() == HumanoidArm.RIGHT);
        if (hand == InteractionHand.OFF_HAND) {
            isRightSide = !isRightSide;
        }
        double sideMultiplier = isRightSide ? 1.0 : -1.0;

        if (camera.isInitialized() && client.options.getCameraType().isFirstPerson()) {
            Vec3 camPos = camera.position();
            Vector3fc forward = camera.forwardVector();
            Vector3fc up = camera.upVector();
            Vector3fc left = camera.leftVector(); // Points left in world space

            // In first person:
            // Forward: slightly ahead of camera
            // Side: to the right (-left * sideMultiplier)
            // Down: below camera view (downward along -up)
            double forwardDist = 0.22;
            double sideDist = 0.26 * sideMultiplier;
            double downDist = -0.24;

            return camPos
                    .add(forward.x() * forwardDist, forward.y() * forwardDist, forward.z() * forwardDist)
                    .add(-left.x() * sideDist, -left.y() * sideDist, -left.z() * sideDist)
                    .add(up.x() * downDist, up.y() * downDist, up.z() * downDist);
        }

        // Fallback (e.g. third-person): position around player torso/hand
        Vec3 look = player.getLookAngle();
        return player.position()
                .add(0, player.getEyeHeight() * 0.65, 0)
                .add(look.scale(0.3));
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
