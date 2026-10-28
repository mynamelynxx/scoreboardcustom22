package com.scoreboardchanger.gui;

import com.scoreboardchanger.ScoreboardChangerClient;
import com.scoreboardchanger.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Список товаров фейкового Т-Шопа: добавить / удалить. Хранится в config/scoreboardchanger.json. */
public class ShopEditorScreen extends Screen {
    private static final int ROW_H = 22;
    private static final int LIST_Y = 118;

    private String nameText = "";
    private String priceText = "";
    private String itemText = "minecraft:totem_of_undying";
    private String loreText = "";
    private Text status = Text.empty();
    private int page = 0;
    private int pages = 1;
    private boolean backgroundDrawn;
    private final List<Text> rowTexts = new ArrayList<>();

    public ShopEditorScreen() {
        super(Text.literal("Товары Т-Шопа"));
    }

    @Override
    protected void init() {
        rowTexts.clear();
        int cx = this.width / 2;

        field(cx - 150, 26, 190, 128, nameText, "Название (&#b400ffТалисман)", s -> nameText = s);
        field(cx + 44, 26, 106, 10, priceText, "Цена", s -> priceText = s);
        field(cx - 150, 50, 150, 100, itemText, "Предмет (minecraft:...)", s -> itemText = s);
        field(cx + 4, 50, 146, 400, loreText, "Описание (строки через |)", s -> loreText = s);

        addDrawableChild(ButtonWidget.builder(Text.literal("Добавить товар"), b -> add())
                .dimensions(cx - 150, 74, 300, 20).build());

        List<ModConfig.ShopItem> all = ModConfig.getInstance().shopItems;
        int rows = Math.max(1, Math.min(8, (this.height - LIST_Y - 40) / ROW_H));
        pages = Math.max(1, (all.size() + rows - 1) / rows);
        page = Math.max(0, Math.min(page, pages - 1));

        for (int r = 0; r < rows; r++) {
            int idx = page * rows + r;
            if (idx >= all.size()) break;
            ModConfig.ShopItem entry = all.get(idx);
            int y = LIST_Y + r * ROW_H;
            rowTexts.add(ScoreboardChangerClient.parseColoredText(entry.name + " &7— &#00fc00" + entry.price));
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

    private void field(int x, int y, int w, int maxLength, String value, String hint, Consumer<String> onChange) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, w, 20, Text.literal(hint));
        f.setMaxLength(maxLength);
        f.setText(value);
        f.setSuggestion(value.isEmpty() ? hint : null);
        f.setChangedListener(s -> {
            onChange.accept(s);
            f.setSuggestion(s.isEmpty() ? hint : null);
        });
        addDrawableChild(f);
    }

    private void add() {
        String name = nameText.trim();
        if (name.isEmpty()) {
            status = Text.literal("Введи название").formatted(Formatting.RED);
            return;
        }
        int price;
        try {
            price = Integer.parseInt(priceText.trim());
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            status = Text.literal("Цена — целое число").formatted(Formatting.RED);
            return;
        }
        Identifier id = Identifier.tryParse(itemText.trim());
        if (id == null || !Registries.ITEM.containsId(id)) {
            status = Text.literal("Нет такого предмета: " + itemText.trim()).formatted(Formatting.RED);
            return;
        }

        ModConfig.getInstance().shopItems.add(new ModConfig.ShopItem(name, price, id.toString(), loreText.trim()));
        ModConfig.save();

        nameText = "";
        priceText = "";
        loreText = "";
        status = Text.literal("Добавлено").formatted(Formatting.GREEN);
        page = Integer.MAX_VALUE;
        clearAndInit();
    }

    private void delete(ModConfig.ShopItem entry) {
        ModConfig.getInstance().shopItems.remove(entry);
        ModConfig.save();
        status = Text.literal("Удалено").formatted(Formatting.YELLOW);
        clearAndInit();
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
                    Text.literal("Пока нет товаров").formatted(Formatting.GRAY), cx, LIST_Y + 6, 0xFFFFFF);
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
