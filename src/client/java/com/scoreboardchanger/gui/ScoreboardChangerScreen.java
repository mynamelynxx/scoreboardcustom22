package com.scoreboardchanger.gui;
 
import com.scoreboardchanger.ScoreboardChangerClient;
import com.scoreboardchanger.config.ModConfig;
import com.scoreboardchanger.feature.ServerScan;
import com.scoreboardchanger.util.RankColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
 
public class ScoreboardChangerScreen extends Screen {
 
    // Строковые коды цветов (включая RGB через §x)
    private static final String[] RANK_COLORS = {
        // Стандартные цвета Minecraft
        "§a",                   // Зелёный
        "§b",                   // Голубой
        "§e",                   // Жёлтый
        "§6",                   // Золотой
        // RGB-цвета
        "§x§f§f§f§f§f§f",      // Белый
        "§x§d§2§d§2§f§f",      // Лавандовый
        "§x§a§4§e§7§c§4",      // Мятный
        "§x§3§e§d§9§c§9",      // Бирюзовый
        "§x§0§0§f§f§f§4",      // Циановый
        "§x§0§0§b§9§f§f",      // Голубой (RGB)
        "§x§6§7§9§9§f§2",      // Васильковый
        "§x§f§f§8§3§5§2",      // Оранжевый
        "§x§f§f§c§5§0§0",      // Золотой (RGB)
        "§x§e§4§f§f§0§0",      // Лаймовый
        "§x§5§c§f§f§0§0",      // Ядовито-зелёный
        "§x§0§0§f§f§7§5",      // Изумрудный
        "§x§0§0§3§9§f§f",      // Кобальтовый
        "§x§8§0§0§0§f§f",      // Фиолетовый (RGB)
        "§x§f§f§0§0§a§f",      // Малиновый
        "§x§F§F§0§0§2§E",      // Алый
        "§x§c§2§0§0§0§0"       // Тёмно-красный (RGB)
    };
    private static final String[] RANK_COLOR_NAMES = {
        // Стандартные
        "Зелёный", "Голубой", "Жёлтый", "Золотой",
        // RGB
        "Белый", "Лавандовый", "Мятный", "Бирюзовый",
        "Циановый", "Голубой (RGB)", "Васильковый", "Оранжевый",
        "Золотой (RGB)", "Лаймовый", "Ядовито-зелёный", "Изумрудный",
        "Кобальтовый", "Фиолетовый (RGB)", "Малиновый", "Алый",
        "Тёмно-красный (RGB)"
    };
 
    private TextFieldWidget nicknameField, rankField, coinsField, tokensField,
            skullsField, killsField, deathsField, playtimeField;
 
    private int selectedColorIndex = 0;
    private final List<String> colorCodes = new ArrayList<>();
    private final List<String> colorNames = new ArrayList<>();
    private boolean backgroundDrawn;
    private ButtonWidget colorBtn;
    private ButtonWidget enableBtn, debugBtn, autoAnarchyBtn;
    private OffsetSlider sliderX, sliderY;
 
    public ScoreboardChangerScreen() {
        super(Text.literal("Scoreboard Changer"));
    }
 
    @Override
    protected void init() {
        ModConfig cfg = ModConfig.getInstance();
 
        // Определяем индекс по сохранённой строке цвета
        colorCodes.clear();
        colorNames.clear();
        for (int i = 0; i < RANK_COLORS.length; i++) {
            colorCodes.add(RANK_COLORS[i]);
            colorNames.add(RANK_COLOR_NAMES[i]);
        }
        for (ModConfig.CustomColor custom : cfg.customColors) {
            colorCodes.add(custom.code);
            colorNames.add(custom.name);
        }

        String savedColor = cfg.fakeRankColor;
        int found = -1;
        for (int i = 0; i < colorCodes.size(); i++) {
            if (colorCodes.get(i).equalsIgnoreCase(savedColor)) {
                found = i;
                break;
            }
        }
        if (found < 0 && savedColor != null && !savedColor.isEmpty()) {
            colorCodes.add(savedColor);
            colorNames.add("Текущий");
            found = colorCodes.size() - 1;
        }
        if (found >= 0) selectedColorIndex = found;
        if (selectedColorIndex >= colorCodes.size()) selectedColorIndex = 0;
 
        int col1X = this.width / 2 - 220;
        int col2X = this.width / 2 + 10;
        int y0 = 25, gap = 26, fW = 150, fH = 18;
 
        nicknameField = addField(col1X + 90, y0, fW, fH, cfg.fakeNickname);
        rankField = addField(col1X + 90, y0 + gap, fW, fH, cfg.fakeRank);
 
        colorBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal("Цвет: " + colorNames.get(selectedColorIndex)),
                btn -> {
                    selectedColorIndex = (selectedColorIndex + 1) % colorCodes.size();
                    btn.setMessage(Text.literal("Цвет: " + colorNames.get(selectedColorIndex)));
                }).dimensions(col1X + 90, y0 + gap * 2, fW, fH).build());
 
        coinsField = addField(col1X + 90, y0 + gap * 3, fW, fH, cfg.fakeCoins);
        tokensField = addField(col1X + 90, y0 + gap * 4, fW, fH, cfg.fakeTokens);
        skullsField = addField(col1X + 90, y0 + gap * 5, fW, fH, cfg.fakeSkulls);
 
        killsField = addField(col2X + 90, y0, fW, fH, cfg.fakeKills);
        deathsField = addField(col2X + 90, y0 + gap, fW, fH, cfg.fakeDeaths);
        playtimeField = addField(col2X + 90, y0 + gap * 2, fW, fH, cfg.fakePlaytime);

        addDrawableChild(ButtonWidget.builder(Text.literal("Настроить..."), btn -> {
            save();
            this.client.setScreen(new CustomColorsScreen());
        }).dimensions(col2X + 90, y0 + gap * 3, fW, fH).build());
 
        autoAnarchyBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal(cfg.autoAnarchy ? "§aАвто-анархия: ВКЛ" : "§7Авто-анархия: ВЫКЛ"),
                btn -> {
                    cfg.autoAnarchy = !cfg.autoAnarchy;
                    btn.setMessage(Text.literal(cfg.autoAnarchy ? "§aАвто-анархия: ВКЛ" : "§7Авто-анархия: ВЫКЛ"));
                }).dimensions(col2X, y0 + gap * 6, 240, fH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Т-Шоп..."), btn -> {
            save();
            this.client.setScreen(new ShopEditorScreen());
        }).dimensions(col2X, y0 + gap * 7, 118, fH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Чат..."), btn -> {
            save();
            this.client.setScreen(new FakeChatScreen());
        }).dimensions(col2X + 122, y0 + gap * 7, 118, fH).build());

        sliderX = new OffsetSlider(col1X, y0 + gap * 7, 200, 18, "Смещение X", cfg.offsetX, -300, 300);
        sliderY = new OffsetSlider(col1X, y0 + gap * 8, 200, 18, "Смещение Y", cfg.offsetY, -300, 300);
        addDrawableChild(sliderX);
        addDrawableChild(sliderY);
 
        int btnY = this.height - 55;
        enableBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal(cfg.enabled ? "§aВключено" : "§cВыключено"),
                btn -> {
                    cfg.enabled = !cfg.enabled;
                    btn.setMessage(Text.literal(cfg.enabled ? "§aВключено" : "§cВыключено"));
                }).dimensions(this.width / 2 - 155, btnY, 100, 20).build());
 
        debugBtn = addDrawableChild(ButtonWidget.builder(
                Text.literal(cfg.debugMode ? "§aДебаг: ВКЛ" : "§7Дебаг: ВЫКЛ"),
                btn -> {
                    cfg.debugMode = !cfg.debugMode;
                    btn.setMessage(Text.literal(cfg.debugMode ? "§aДебаг: ВКЛ" : "§7Дебаг: ВЫКЛ"));
                }).dimensions(this.width / 2 - 50, btnY, 100, 20).build());
 
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), btn -> save())
                .dimensions(this.width / 2 + 55, btnY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), btn -> close())
                .dimensions(this.width / 2 + 55, btnY + 25, 100, 20).build());
    }
 
    private TextFieldWidget addField(int x, int y, int w, int h, String value) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, w, h, Text.empty());
        f.setText(value);
        f.setMaxLength(64);
        addDrawableChild(f);
        return f;
    }
 
    private void save() {
        ModConfig cfg = ModConfig.getInstance();
        cfg.fakeNickname = nicknameField.getText();
        cfg.fakeRank = rankField.getText();
        cfg.fakeRankColor = colorCodes.get(selectedColorIndex); // сохраняем строку
        cfg.fakeCoins = coinsField.getText();
        cfg.fakeTokens = tokensField.getText();
        cfg.fakeSkulls = skullsField.getText();
        cfg.fakeKills = killsField.getText();
        cfg.fakeDeaths = deathsField.getText();
        cfg.fakePlaytime = playtimeField.getText();
        cfg.offsetX = sliderX.getIntValue();
        cfg.offsetY = sliderY.getIntValue();
        ModConfig.save();
    }
 
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        backgroundDrawn = false;
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 8, 0xFFFFFF);
 
        int col1X = this.width / 2 - 220;
        int col2X = this.width / 2 + 10;
        int y0 = 25, gap = 26;
 
        context.drawTextWithShadow(textRenderer, "Никнейм:", col1X, y0 + 5, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Ранг:", col1X, y0 + gap + 5, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Цвет ранга:", col1X, y0 + gap * 2 + 5, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Монеты:", col1X, y0 + gap * 3 + 5, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Токены:", col1X, y0 + gap * 4 + 5, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Анархия (запас.):", col1X, y0 + gap * 5 + 5, 0xAAAAAA);
 
        context.drawTextWithShadow(textRenderer, "Убийства:", col2X, y0 + 5, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Смерти:", col2X, y0 + gap + 5, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Наиграно:", col2X, y0 + gap * 2 + 5, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Свои цвета:", col2X, y0 + gap * 3 + 5, 0xAAAAAA);
 
        String detected = ServerScan.detectedAnarchy();
        context.drawTextWithShadow(textRenderer,
                "§7Анархия с сервера: " + (detected != null ? "§a" + detected : "§cне найдена"),
                col1X, y0 + gap * 6 + 5, 0xAAAAAA);

        // ===== Превью ранга (сплошной цвет или градиент) =====
        Text coloredRank = ScoreboardChangerClient.parseColoredText(
                RankColors.styled(colorCodes.get(selectedColorIndex), rankField.getText()));

        context.drawTextWithShadow(textRenderer, "Превью:", col2X, y0 + gap * 4, 0x888888);
        context.drawTextWithShadow(textRenderer, coloredRank, col2X, y0 + gap * 5, 0xFFFFFF);
        // =====================================
 
        context.drawTextWithShadow(textRenderer,
                "§7Дебаг: показывает overlay без сервера. Ползунки — подстрой под скорборд.",
                col1X, this.height - 12, 0x888888);
    }
 
    // В 1.21.x Screen.render сам рисует фон; второй вызов blur в один кадр падает — рисуем ровно один раз.
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (backgroundDrawn) return;
        backgroundDrawn = true;
        super.renderBackground(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() { return false; }
 
    @Override
    public void close() { this.client.setScreen(null); }
 
    public static class OffsetSlider extends SliderWidget {
        private final String label;
        private final int min, max;
 
        public OffsetSlider(int x, int y, int width, int height, String label, int current, int min, int max) {
            super(x, y, width, height, Text.empty(), (double) (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            updateMessage();
        }
 
        public int getIntValue() {
            return min + (int) Math.round(value * (max - min));
        }
 
        @Override
        protected void updateMessage() {
            setMessage(Text.literal(label + ": " + getIntValue()));
        }
 
        @Override
        protected void applyValue() {}
    }
}
