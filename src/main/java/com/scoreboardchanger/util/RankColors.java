package com.scoreboardchanger.util;

import java.util.List;

/** Превращает сохранённый код цвета (сплошной или градиент) и текст в строку с §x-кодами. */
public final class RankColors {
    private RankColors() {}

    /** 0xFFC500 -> §x§f§f§c§5§0§0 */
    public static String canonical(int rgb) {
        StringBuilder sb = new StringBuilder("\u00A7x");
        for (char c : String.format("%06x", rgb & 0xFFFFFF).toCharArray()) {
            sb.append('\u00A7').append(c);
        }
        return sb.toString();
    }

    /** Список цветов одной строкой кода: один цвет = сплошной, несколько = градиент. */
    public static String canonical(List<Integer> colors) {
        StringBuilder sb = new StringBuilder();
        for (int c : colors) sb.append(canonical(c));
        return sb.toString();
    }

    /**
     * Текст, покрашенный кодом цвета. Для градиента цвет считается на каждый символ,
     * результат — строка вида §x§r§r§g§g§b§bА§x§r§r§g§g§b§bБ...
     */
    public static String styled(String colorCode, String text) {
        if (text == null || text.isEmpty()) return "";
        if (colorCode == null) colorCode = "";

        List<Integer> colors = ColorParser.parse(colorCode);
        if (colors.isEmpty()) return colorCode + text;          // обычные §a, §b, ...
        if (colors.size() == 1) return canonical(colors.get(0)) + text;

        int[] cps = text.codePoints().toArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cps.length; i++) {
            sb.append(canonical(colorAt(colors, i, cps.length))).appendCodePoint(cps[i]);
        }
        return sb.toString();
    }

    private static int colorAt(List<Integer> colors, int i, int n) {
        int k = colors.size();
        if (n == 1) return colors.get(0);
        if (k == n) return colors.get(i);
        double pos = i * (k - 1) / (double) (n - 1);
        int lo = (int) Math.floor(pos);
        int hi = Math.min(lo + 1, k - 1);
        return lerp(colors.get(lo), colors.get(hi), pos - lo);
    }

    private static int lerp(int a, int b, double t) {
        int rgb = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int ca = (a >> shift) & 0xFF;
            int cb = (b >> shift) & 0xFF;
            rgb |= ((int) Math.round(ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return rgb;
    }
}
