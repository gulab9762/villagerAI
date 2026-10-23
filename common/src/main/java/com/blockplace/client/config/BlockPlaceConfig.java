package com.blockplace.client.config;

public class BlockPlaceConfig {
    /**
     * Duration of the travel animation in ticks (20 ticks = 1 second).
     * 5 ticks = 250 milliseconds, snappy and responsive.
     */
    public static int durationTicks = 5;

    /**
     * Initial scale of the block when leaving the player's hand.
     * Matches typical held item scale in first-person view.
     */
    public static float startScale = 0.32f;

    /**
     * Height of the parabolic travel arc in blocks.
     */
    public static float arcHeight = 0.18f;

    /**
     * Initial rotation tilt from hand angle in degrees.
     */
    public static float initialTiltDegrees = 25.0f;

    /**
     * Subtle bounce/squash effect on block arrival.
     */
    public static boolean enableSettleBounce = true;

    /**
     * Subtle particle poof on block landing.
     */
    public static boolean enableArrivalParticles = true;

    /**
     * Whether to apply the animation only in first-person perspective.
     */
    public static boolean firstPersonOnly = true;
}
