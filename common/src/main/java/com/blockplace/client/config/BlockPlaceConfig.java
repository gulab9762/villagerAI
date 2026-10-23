package com.blockplace.client.config;

import com.blockplace.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;

public class BlockPlaceConfig {

    public enum AnimationStyle {
        SMOOTH,
        ROTATING,
        POP,
        DROP,
        SPIRAL,
        EXPAND,
        SPRING,
        NONE
    }

    private static final Path CONFIG_PATH = Path.of("config", "blockplace.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Currently active placement animation style.
     */
    public static AnimationStyle style = AnimationStyle.ROTATING;

    /**
     * Duration of the travel animation in ticks (20 ticks = 1 second).
     * 6 ticks = 300 milliseconds.
     */
    public static int durationTicks = 6;

    /**
     * Initial scale of the block when leaving the player's hand.
     */
    public static float startScale = 0.28f;

    /**
     * Height of the travel arc in blocks.
     */
    public static float arcHeight = 0.05f;

    /**
     * Initial rotation tilt from hand angle in degrees (used in SMOOTH style).
     */
    public static float initialTiltDegrees = 20.0f;

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

    /**
     * Loads saved configuration from config/blockplace.json.
     */
    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String content = Files.readString(CONFIG_PATH);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                if (json.has("style")) {
                    try {
                        String savedStyle = json.get("style").getAsString();
                        setStyle(savedStyle);
                    } catch (Exception ignored) {}
                }
                if (json.has("durationTicks")) durationTicks = json.get("durationTicks").getAsInt();
                if (json.has("startScale")) startScale = json.get("startScale").getAsFloat();
                if (json.has("arcHeight")) arcHeight = json.get("arcHeight").getAsFloat();
                if (json.has("enableSettleBounce")) enableSettleBounce = json.get("enableSettleBounce").getAsBoolean();
                if (json.has("enableArrivalParticles")) enableArrivalParticles = json.get("enableArrivalParticles").getAsBoolean();
                Constants.LOG.info("Loaded {} config from {}, style={}", Constants.MOD_NAME, CONFIG_PATH, style);
            } else {
                save();
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to load config from {}", CONFIG_PATH, e);
        }
    }

    /**
     * Saves configuration to config/blockplace.json.
     */
    public static void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            JsonObject json = new JsonObject();
            json.addProperty("style", style.name().toLowerCase());
            json.addProperty("durationTicks", durationTicks);
            json.addProperty("startScale", startScale);
            json.addProperty("arcHeight", arcHeight);
            json.addProperty("enableSettleBounce", enableSettleBounce);
            json.addProperty("enableArrivalParticles", enableArrivalParticles);
            Files.writeString(CONFIG_PATH, GSON.toJson(json));
            Constants.LOG.info("Saved {} config to {}, style={}", Constants.MOD_NAME, CONFIG_PATH, style);
        } catch (Exception e) {
            Constants.LOG.error("Failed to save config to {}", CONFIG_PATH, e);
        }
    }

    /**
     * Sets the active animation style and persists it immediately to file.
     * Supports "smooth", "rotating", "pop", "drop", "spiral", "expand", "spring", "none" (or "vanilla"/"off").
     *
     * @param styleName style name
     * @return true if valid style was set, false otherwise
     */
    public static boolean setStyle(String styleName) {
        if (styleName.equalsIgnoreCase("vanilla") || styleName.equalsIgnoreCase("off") || styleName.equalsIgnoreCase("disabled")) {
            style = AnimationStyle.NONE;
            save();
            return true;
        }
        for (AnimationStyle s : AnimationStyle.values()) {
            if (s.name().equalsIgnoreCase(styleName)) {
                style = s;
                save();
                return true;
            }
        }
        return false;
    }
}
