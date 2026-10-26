package com.villagerai.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.villagerai.Constants;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class VillagerAIConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath = Path.of("config", "villagerai.json");
    private static VillagerAIConfig instance = new VillagerAIConfig();

    /**
     * Master toggle: whether roaming/aimless wandering is disabled.
     * When true, villagers stay put while idle and do not wander off.
     */
    public boolean disableRoaming = true;

    /**
     * When true, disables 4-block wandering around workstation while working.
     */
    public boolean disableWorkRoaming = true;

    /**
     * When true, disables 40-block wandering around village bell during meeting time.
     */
    public boolean disableBellRoaming = true;

    /**
     * When true, disables restless wandering inside homes when resting.
     */
    public boolean disableRestRoaming = true;

    /**
     * When true, prevents baby villagers from wandering off during play.
     */
    public boolean disableBabyRoaming = true;

    /**
     * When true, prioritizes assigning workstations to the nearest eligible unassigned villager
     * both when a workstation is placed and during village generation / chunk load.
     */
    public boolean assignNearestVillager = true;

    public static VillagerAIConfig get() {
        return instance;
    }

    public static void init(Path path) {
        configPath = path;
        load();
    }

    public static void load() {
        try {
            if (Files.exists(configPath)) {
                try (Reader reader = Files.newBufferedReader(configPath)) {
                    VillagerAIConfig loaded = GSON.fromJson(reader, VillagerAIConfig.class);
                    if (loaded != null) {
                        instance = loaded;
                        Constants.LOG.info("Loaded Villager AI config from {}", configPath);
                        return;
                    }
                }
            }
            save(); // Create default config if missing
        } catch (Exception e) {
            Constants.LOG.error("Failed to load Villager AI config from {}, using defaults", configPath, e);
        }
    }

    public static void save() {
        try {
            if (configPath.getParent() != null) {
                Files.createDirectories(configPath.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(instance, writer);
            }
            Constants.LOG.info("Saved Villager AI config to {}", configPath);
        } catch (Exception e) {
            Constants.LOG.error("Failed to save Villager AI config to {}", configPath, e);
        }
    }
}
