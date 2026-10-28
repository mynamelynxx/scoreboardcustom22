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

    // Анархия: брать номер из настоящего скорборда сервера (fakeSkulls — запасное значение)
    public boolean autoAnarchy = true;

    // ===== Фейковый Т-Шоп =====
    public List<ShopItem> shopItems = defaultShopItems();
    // Команда, которая открывает фейковый магазин (/tshop или .tshop, без слэша)
    public String shopCommand = "tshop";
    // Плейсхолдеры: {item} {price} {tokens} {anarchy}
    public String shopBuyMessage = "&#fc9700&l[Т-Шоп] &fВы купили {item}&f за &#00fc00{price} &fтокенов &7(осталось: &#00fc00{tokens}&7)";
    public String shopNoMoneyMessage = "&#fc1a1a&l[Т-Шоп] &fНедостаточно токенов! Нужно: &#00fc00{price}";

    public static class ShopItem {
        public String name = "";                         // название с цветом (&#rrggbb, §, &)
        public int price = 0;                            // цена в токенах
        public String item = "minecraft:totem_of_undying"; // предмет
        public String lore = "";                         // строки описания через |

        public ShopItem() {}

        public ShopItem(String name, int price, String item, String lore) {
            this.name = name;
            this.price = price;
            this.item = item;
            this.lore = lore;
        }
    }

    private static List<ShopItem> defaultShopItems() {
        List<ShopItem> list = new ArrayList<>();
        list.add(new ShopItem("&#b400ff&lТалисман Фантома", 50, "minecraft:totem_of_undying", "&7Талисман|&7Покупается за токены"));
        return list;
    }

    // ===== Подмена чата =====
    public boolean fakeChatMode = false;
    // Запасной формат, если реальный не запомнен. Плейсхолдеры: {rank} {name} {msg}
    public String chatFormat = "&#fc1a1a[{rank}&#fc1a1a] &#fc9700{name} &#fc1a1a» &f{msg}";
    // Твой настоящий ник в чате сервера (пусто = ник аккаунта)
    public String chatRealName = "";
    // Запомненный формат реального сообщения (скобки, теги, цвета)
    public ChatTemplate chatTemplate = null;

    public static class ChatTemplate {
        public List<Run> runs = new ArrayList<>();
        public int rankStart = -1, rankEnd = -1;
        public int nameStart = -1, nameEnd = -1;
        public int msgStart = -1, msgEnd = -1;
    }

    public static class Run {
        public String text = "";
        public int color = -1;
        public boolean bold, italic, underline, strike, obfuscated;
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
                    if (INSTANCE.shopItems == null) INSTANCE.shopItems = new ArrayList<>();
                    if (INSTANCE.shopCommand == null) INSTANCE.shopCommand = "tshop";
                    if (INSTANCE.shopBuyMessage == null) INSTANCE.shopBuyMessage = new ModConfig().shopBuyMessage;
                    if (INSTANCE.shopNoMoneyMessage == null) INSTANCE.shopNoMoneyMessage = new ModConfig().shopNoMoneyMessage;
                    if (INSTANCE.chatFormat == null) INSTANCE.chatFormat = new ModConfig().chatFormat;
                    if (INSTANCE.chatRealName == null) INSTANCE.chatRealName = "";
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
