package com.scoreboardchanger.feature;

import com.scoreboardchanger.ScoreboardChangerClient;
import com.scoreboardchanger.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Фейковый Т-Шоп: токены списываются визуально (в оверлее), предмет выдаётся только на клиенте. */
public final class FakeShop {
    private FakeShop() {}

    public static ItemStack makeStack(ModConfig.ShopItem it) {
        Identifier id = Identifier.tryParse(it.item == null ? "" : it.item.trim());
        Item item = (id != null && Registries.ITEM.containsId(id)) ? Registries.ITEM.get(id) : Items.TOTEM_OF_UNDYING;
        ItemStack st = new ItemStack(item);

        MutableText name = Text.empty().setStyle(Style.EMPTY.withItalic(false));
        name.append(ScoreboardChangerClient.parseColoredText(it.name));
        st.set(DataComponentTypes.CUSTOM_NAME, name);

        List<Text> lore = loreLines(it);
        if (!lore.isEmpty()) st.set(DataComponentTypes.LORE, new LoreComponent(lore));
        return st;
    }

    public static List<Text> loreLines(ModConfig.ShopItem it) {
        List<Text> lore = new ArrayList<>();
        if (it.lore == null || it.lore.isBlank()) return lore;
        for (String line : it.lore.split("\\|")) {
            MutableText t = Text.empty().setStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(0xAAAAAA)));
            t.append(ScoreboardChangerClient.parseColoredText(line));
            lore.add(t);
        }
        return lore;
    }

    public static void buy(MinecraftClient c, ModConfig.ShopItem it) {
        if (c.player == null) return;
        ModConfig cfg = ModConfig.getInstance();
        long balance = parseMoney(cfg.fakeTokens);

        if (balance < 0 || balance < it.price) {
            message(cfg.shopNoMoneyMessage, it, cfg.fakeTokens);
            c.player.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        cfg.fakeTokens = formatMoney(balance - it.price, cfg.fakeTokens);
        ModConfig.save();

        c.player.getInventory().insertStack(makeStack(it));
        message(cfg.shopBuyMessage, it, cfg.fakeTokens);
        c.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private static void message(String template, ModConfig.ShopItem it, String tokens) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.inGameHud == null || template == null) return;
        ModConfig cfg = ModConfig.getInstance();
        String s = template
                .replace("{item}", it.name)
                .replace("{price}", String.valueOf(it.price))
                .replace("{tokens}", tokens)
                .replace("{anarchy}", ServerScan.anarchy(cfg));
        c.inGameHud.getChatHud().addMessage(ScoreboardChangerClient.parseColoredText(s));
    }

    /** "1 500" / "1,500" / "1500" -> число; если в строке есть буквы (1.5K) — -1. */
    public static long parseMoney(String s) {
        if (s == null) return -1;
        String t = s.trim();
        if (t.isEmpty() || !t.matches("[0-9][0-9 ,._\\u00a0]*")) return -1;
        String digits = t.replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 17) return -1;
        return Long.parseLong(digits);
    }

    /** Сохраняет разделитель разрядов, который был у исходного значения. */
    public static String formatMoney(long value, String original) {
        String digits = String.valueOf(Math.max(0, value));
        String sep = "";
        if (original != null) {
            if (original.contains(" ") || original.contains("\u00a0")) sep = " ";
            else if (original.contains(",")) sep = ",";
            else if (original.contains(".")) sep = ".";
        }
        if (sep.isEmpty() || digits.length() <= 3) return digits;
        StringBuilder sb = new StringBuilder();
        int first = digits.length() % 3;
        if (first > 0) sb.append(digits, 0, first);
        for (int i = first; i < digits.length(); i += 3) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(digits, i, i + 3);
        }
        return sb.toString();
    }
}
