package com.scoreboardchanger.gui;

import com.scoreboardchanger.ScoreboardChangerClient;
import com.scoreboardchanger.config.ModConfig;
import com.scoreboardchanger.feature.FakeShop;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Фейковый Т-Шоп: клик по предмету = покупка (токены списываются визуально, предмет выдаётся на клиенте). */
public class ShopScreen extends Screen {
    private static final int COLS = 9;
    private static final int SLOT = 18;
    private static final int PANEL_W = COLS * SLOT + 14;

    private int left, top, rows, panelH;
    private final List<ItemStack> stacks = new ArrayList<>();

    public ShopScreen() {
        super(Text.literal("Т-Шоп"));
    }

    @Override
    protected void init() {
        stacks.clear();
        List<ModConfig.ShopItem> items = ModConfig.getInstance().shopItems;
        int count = Math.min(items.size(), COLS * 6);
        for (int i = 0; i < count; i++) stacks.add(FakeShop.makeStack(items.get(i)));

        rows = Math.max(3, Math.min(6, (count + COLS - 1) / COLS));
        panelH = 22 + rows * SLOT + 8;
        left = (this.width - PANEL_W) / 2;
        top = Math.max(4, (this.height - panelH - 28) / 2);

        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(this.width / 2 - 50, top + panelH + 6, 100, 20).build());
    }

    private int slotAt(double mx, double my) {
        int gx = left + 7;
        int gy = top + 22;
        int cx = (int) Math.floor((mx - gx) / SLOT);
        int cy = (int) Math.floor((my - gy) / SLOT);
        if (cx < 0 || cx >= COLS || cy < 0 || cy >= rows) return -1;
        int idx = cy * COLS + cx;
        return idx < stacks.size() ? idx : -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0) {
            int idx = slotAt(mouseX, mouseY);
            if (idx >= 0) {
                FakeShop.buy(this.client, ModConfig.getInstance().shopItems.get(idx));
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // панель
        context.fill(left, top, left + PANEL_W, top + panelH, 0xFF000000);
        context.fill(left + 1, top + 1, left + PANEL_W - 1, top + panelH - 1, 0xFFC6C6C6);

        ModConfig cfg = ModConfig.getInstance();
        context.drawText(textRenderer, "Т-Шоп", left + 8, top + 7, 0x404040, false);
        Text balance = ScoreboardChangerClient.parseColoredText("&#404040Токенов: &#00a800" + cfg.fakeTokens);
        context.drawText(textRenderer, balance, left + PANEL_W - 8 - textRenderer.getWidth(balance), top + 7, 0xFFFFFF, false);

        // слоты
        int gx = left + 7;
        int gy = top + 22;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < COLS; c++) {
                int x = gx + c * SLOT;
                int y = gy + r * SLOT;
                context.fill(x, y, x + SLOT, y + SLOT, 0xFF373737);
                context.fill(x + 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
                context.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF8B8B8B);
            }
        }
        for (int i = 0; i < stacks.size(); i++) {
            int x = gx + (i % COLS) * SLOT + 1;
            int y = gy + (i / COLS) * SLOT + 1;
            context.drawItem(stacks.get(i), x, y);
        }

        // подсветка и подсказка
        int hover = slotAt(mouseX, mouseY);
        if (hover >= 0) {
            int x = gx + (hover % COLS) * SLOT + 1;
            int y = gy + (hover / COLS) * SLOT + 1;
            context.fill(x, y, x + 16, y + 16, 0x80FFFFFF);

            ModConfig.ShopItem it = cfg.shopItems.get(hover);
            List<Text> tip = new ArrayList<>();
            tip.add(ScoreboardChangerClient.parseColoredText(it.name));
            tip.addAll(FakeShop.loreLines(it));
            tip.add(Text.empty());
            tip.add(ScoreboardChangerClient.parseColoredText("&7Цена: &#00fc00" + it.price + " &7токенов"));
            tip.add(ScoreboardChangerClient.parseColoredText("&eНажми, чтобы купить"));
            context.drawTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
