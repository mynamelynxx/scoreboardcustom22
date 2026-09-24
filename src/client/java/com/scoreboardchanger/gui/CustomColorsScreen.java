package com.scoreboardchanger.gui;

import com.scoreboardchanger.ScoreboardChangerClient;
import com.scoreboardchanger.config.ModConfig;
import com.scoreboardchanger.util.ColorParser;
import com.scoreboardchanger.util.RankColors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Свои цвета ранга: добавить по коду, назвать, выбрать, удалить. Хранится в config/scoreboardchanger.json. */
public class CustomColorsScreen extends Screen {
    private static final int ROW_H = 22;
    private static final int LIST_Y = 116;

    private TextFieldWidget nameField;
    private TextFieldWidget codeField;
    private String nameText = "";
    private String codeText = "";
    private Text status = Text.empty();
    private int page = 0;
    private int pages = 1;
    private boolean backgroundDrawn;
    private final List<Text> rowTexts = new ArrayList<>();

    public CustomColorsScreen() {
        super(Text.literal("Свои цвета ранга"));
    }

    @Override
    protected void init() {
        rowTexts.clear();
        int cx = this.width / 2;

        nameField = field(cx - 150, 28, 20, nameText, "Название (например: Сакура)", s -> nameText = s);
        codeField = field(cx - 150, 52, 4000, codeText, "Код цвета (xffc500, #ffc500, градиент...)", s -> codeText = s);

        addDrawableChild(ButtonWidget.builder(Text.literal("Добавить цвет"), b -> add())
                .dimensions(cx - 150, 76, 300, 20).build());

        List<ModConfig.CustomColor> all = ModConfig.getInstance().customColors;
        int rows = Math.max(1, Math.min(8, (this.height - LIST_Y - 40) / ROW_H));
        pages = Math.max(1, (all.size() + rows - 1) / rows);
        page = Math.max(0, Math.min(page, pages - 1));

        for (int r = 0; r < rows; r++) {
            int idx = page * rows + r;
            if (idx >= all.size()) break;
            ModConfig.CustomColor entry = all.get(idx);
            int y = LIST_Y + r * ROW_H;
            rowTexts.add(ScoreboardChangerClient.parseColoredText(RankColors.styled(entry.code, entry.name)));
            addDrawableChild(ButtonWidget.builder(Text.literal("Выбрать"), b -> select(entry))
                    .dimensions(cx + 4, y, 66, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Удалить"), b -> delete(entry))
                    .dimensions(cx + 74, y, 76, 20).build());
        }

        int by = this.height - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page--; clearAndInit(); })
                .dimensions(cx - 150, by, 40, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page++; clearAndInit(); })
                .dimensions(cx + 110, by, 40, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(cx - 50, by, 100, 20).build());
    }

    private TextFieldWidget field(int x, int y, int maxLength, String value, String hint, Consumer<String> onChange) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, 300, 20, Text.literal(hint));
        f.setMaxLength(maxLength);
        f.setText(value);
        f.setSuggestion(value.isEmpty() ? hint : null);
        f.setChangedListener(s -> {
            onChange.accept(s);
            f.setSuggestion(s.isEmpty() ? hint : null);
        });
        addDrawableChild(f);
        return f;
    }

    private void add() {
        String name = nameText.trim();
        List<Integer> colors = ColorParser.parse(codeText);
        if (name.isEmpty()) {
            status = Text.literal("Введи название").formatted(Formatting.RED);
            return;
        }
        if (colors.isEmpty()) {
            status = Text.literal("Код цвета не найден").formatted(Formatting.RED);
            return;
        }

        ModConfig cfg = ModConfig.getInstance();
        String code = RankColors.canonical(colors);
        boolean replaced = false;
        for (ModConfig.CustomColor existing : cfg.customColors) {
            if (existing.name.equalsIgnoreCase(name)) {
                existing.name = name;
                existing.code = code;
                replaced = true;
                break;
            }
        }
        if (!replaced) cfg.customColors.add(new ModConfig.CustomColor(name, code));
        ModConfig.save();

        nameText = "";
        codeText = "";
        status = Text.literal((replaced ? "Обновлено: " : "Добавлено: ") + name).formatted(Formatting.GREEN);
        page = Integer.MAX_VALUE;
        clearAndInit();
    }

    private void select(ModConfig.CustomColor entry) {
        ModConfig cfg = ModConfig.getInstance();
        cfg.fakeRankColor = entry.code;
        ModConfig.save();
        status = Text.literal("Цвет ранга: " + entry.name).formatted(Formatting.GREEN);
    }

    private void delete(ModConfig.CustomColor entry) {
        ModConfig.getInstance().customColors.remove(entry);
        ModConfig.save();
        status = Text.literal("Удалено: " + entry.name).formatted(Formatting.YELLOW);
        clearAndInit();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && (nameField.isFocused() || codeField.isFocused())) {
            add();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (backgroundDrawn) return;
        backgroundDrawn = true;
        super.renderBackground(context, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        backgroundDrawn = false;
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        context.drawCenteredTextWithShadow(textRenderer, this.title, cx, 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, status, cx, 100, 0xFFFFFF);

        if (rowTexts.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Пока нет своих цветов").formatted(Formatting.GRAY), cx, LIST_Y + 6, 0xFFFFFF);
        }
        for (int i = 0; i < rowTexts.size(); i++) {
            context.drawTextWithShadow(textRenderer, rowTexts.get(i), cx - 150, LIST_Y + i * ROW_H + 6, 0xFFFFFF);
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal((page + 1) + " / " + pages),
                cx, this.height - 38, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.client.setScreen(new ScoreboardChangerScreen());
    }
}
