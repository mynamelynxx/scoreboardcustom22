package com.scoreboardchanger.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("scoreboardchanger.json");

    private static ModConfig INSTANCE = new ModConfig();

    // Toggle: enable or disable the fake scoreboard
    public boolean enabled = false;

    // Player info
    public String fakeNickname = "yavklynxx";
    public String fakeRank = "Игрок";
    public String fakeRankColor = "§a"; // green by default

    // Currency
    public String fakeCoins = "0";
    public String fakeTokens = "0";
    public String fakeSkulls = "0";

    // Stats
    public String fakeKills = "0";
    public String fakeDeaths = "0";
    public String fakePlaytime = "0ч";

    // Свои цвета ранга: название + код (§x§f§f§c§5§0§0; несколько цветов подряд = градиент)
    public List<CustomColor> customColors = new ArrayList<>();

    public static class CustomColor {
        public String name = "";
        public String code = "";

        public CustomColor() {}

        public CustomColor(String name, String code) {
            this.name = name;
            this.code = code;
        }
    }

    // Position offset (adjust to align with real scoreboard)
    public int offsetX = 0;
    public int offsetY = 0;

    // Debug: show overlay even without scoreboard
    public boolean debugMode = false;

    public static ModConfig getInstance() {
        return INSTANCE;
    }

    public static void load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                    if (INSTANCE.customColors == null) INSTANCE.customColors = new ArrayList<>();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
