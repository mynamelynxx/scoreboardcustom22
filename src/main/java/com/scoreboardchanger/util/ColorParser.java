package com.scoreboardchanger.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Достаёт цвета из строки в любом из форматов:
 * §x§f§f§c§5§0§0, &x&f&f&c&5&0&0, &#ffc500, #ffc500, <#ffc500>, <gradient:#A:#B>, ffc500.
 * Поле ввода Minecraft вырезает символ § при вставке, поэтому "xffc500" тоже понимается.
 * Текст между кодами игнорируется. Несколько цветов в строке = градиент.
 */
public final class ColorParser {
    private static final Pattern HASH = Pattern.compile("#([0-9a-fA-F]{6})");
    private static final Pattern LEGACY = Pattern.compile("[xX]([0-9a-fA-F]{6})");
    private static final Pattern BARE = Pattern.compile("[0-9a-fA-F]{6}");

    private ColorParser() {}

    public static List<Integer> parse(String raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null) return out;
        String s = raw.replace("\u00A7", "").replace("&", "").trim();

        collect(HASH.matcher(s), out);
        if (!out.isEmpty()) return out;

        collect(LEGACY.matcher(s), out);
        if (!out.isEmpty()) return out;

        if (BARE.matcher(s).matches()) out.add(Integer.parseInt(s, 16));
        return out;
    }

    private static void collect(Matcher m, List<Integer> out) {
        while (m.find()) out.add(Integer.parseInt(m.group(1), 16));
    }
}
