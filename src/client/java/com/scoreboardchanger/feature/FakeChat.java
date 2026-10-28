package com.scoreboardchanger.feature;

import com.scoreboardchanger.ScoreboardChangerClient;
import com.scoreboardchanger.config.ModConfig;
import com.scoreboardchanger.util.RankColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Подмена чата. Запоминает формат настоящего сообщения (скобки, тег, цвета),
 * потом собирает такие же сообщения с твоим фейковым ником и рангом. Только у тебя на экране.
 */
public final class FakeChat {
    public static boolean learnPending = false;
    public static String learnText = null;

    private FakeChat() {}

    private static final class Flat {
        final StringBuilder text = new StringBuilder();
        final List<Style> styles = new ArrayList<>();
    }

    // ---------- обучение ----------

    public static void startLearn() {
        learnPending = true;
        learnText = null;
    }

    public static void reset() {
        ModConfig.getInstance().chatTemplate = null;
        ModConfig.save();
    }

    public static void tryLearn(Text message) {
        if (!learnPending || learnText == null || learnText.isEmpty()) return;

        Flat f = flatten(message);
        String low = f.text.toString().toLowerCase(Locale.ROOT);
        int mi = low.lastIndexOf(learnText.toLowerCase(Locale.ROOT));
        if (mi < 0) return;
        int me = mi + learnText.length();

        ModConfig cfg = ModConfig.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();

        String realName = cfg.chatRealName != null && !cfg.chatRealName.isBlank()
                ? cfg.chatRealName.trim()
                : client.getSession().getUsername();

        String before = low.substring(0, mi);
        int ni = realName.isEmpty() ? -1 : before.lastIndexOf(realName.toLowerCase(Locale.ROOT));
        int ne = ni >= 0 ? ni + realName.length() : -1;

        int ri = -1;
        int re = -1;
        String rr = ServerScan.realRank();
        if (rr != null && !rr.isEmpty()) {
            int idx = before.indexOf(rr.toLowerCase(Locale.ROOT));
            if (idx >= 0) {
                int end = idx + rr.length();
                boolean overlapsName = ni >= 0 && idx < ne && end > ni;
                if (!overlapsName) {
                    ri = idx;
                    re = end;
                }
            }
        }

        ModConfig.ChatTemplate tpl = new ModConfig.ChatTemplate();
        tpl.runs = toRuns(f);
        tpl.msgStart = mi;
        tpl.msgEnd = me;
        tpl.nameStart = ni;
        tpl.nameEnd = ne;
        tpl.rankStart = ri;
        tpl.rankEnd = re;
        cfg.chatTemplate = tpl;
        ModConfig.save();

        learnPending = false;
        learnText = null;

        info("Формат чата запомнен. Ник: " + (ni >= 0 ? "найден" : "НЕ найден")
                + ", ранг: " + (ri >= 0 ? "найден" : "не найден (останется как есть)")
                + ". Режим подмены: .fc или меню мода.");
    }

    // ---------- отправка ----------

    public static void send(String msg) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.inGameHud == null) return;
        c.inGameHud.getChatHud().addMessage(build(msg));
    }

    public static void info(String s) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.inGameHud == null) return;
        c.inGameHud.getChatHud().addMessage(
                Text.literal("[Scoreboard Changer] ").formatted(net.minecraft.util.Formatting.GOLD)
                        .append(Text.literal(s).formatted(net.minecraft.util.Formatting.GRAY)));
    }

    public static Text build(String msg) {
        ModConfig cfg = ModConfig.getInstance();
        ModConfig.ChatTemplate tpl = cfg.chatTemplate;
        if (tpl != null && tpl.runs != null && !tpl.runs.isEmpty()) {
            try {
                return fromTemplate(tpl, msg, cfg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return fromFormat(msg, cfg);
    }

    private static Text fromFormat(String msg, ModConfig cfg) {
        String rank = RankColors.styled(cfg.fakeRankColor, cfg.fakeRank);
        String s = cfg.chatFormat
                .replace("{rank}", rank)
                .replace("{name}", cfg.fakeNickname)
                .replace("{msg}", msg);
        return ScoreboardChangerClient.parseColoredText(s);
    }

    private static Text fromTemplate(ModConfig.ChatTemplate tpl, String msg, ModConfig cfg) {
        Flat f = fromRuns(tpl.runs);
        int len = f.text.length();

        List<int[]> ranges = new ArrayList<>();
        addRange(ranges, tpl.rankStart, tpl.rankEnd, 0, len);
        addRange(ranges, tpl.nameStart, tpl.nameEnd, 1, len);
        addRange(ranges, tpl.msgStart, tpl.msgEnd, 2, len);
        ranges.sort(Comparator.comparingInt((int[] r) -> r[0]));

        MutableText out = Text.empty();
        int cur = 0;
        for (int[] r : ranges) {
            if (r[0] < cur) continue;
            appendSlice(out, f, cur, r[0]);
            if (r[2] == 0) {
                out.append(rankText(f, r[0], cfg));
            } else if (r[2] == 1) {
                out.append(nameText(f, r[0], r[1], cfg.fakeNickname));
            } else {
                out.append(Text.literal(msg).setStyle(f.styles.get(r[0])));
            }
            cur = r[1];
        }
        appendSlice(out, f, cur, len);
        return out;
    }

    private static void addRange(List<int[]> ranges, int s, int e, int type, int len) {
        if (s >= 0 && e > s && e <= len) ranges.add(new int[]{s, e, type});
    }

    private static void appendSlice(MutableText out, Flat f, int from, int to) {
        int i = from;
        while (i < to) {
            Style st = f.styles.get(i);
            int j = i + 1;
            while (j < to && f.styles.get(j).equals(st)) j++;
            out.append(Text.literal(f.text.substring(i, j)).setStyle(st));
            i = j;
        }
    }

    private static Text nameText(Flat f, int start, int end, String fake) {
        MutableText t = Text.empty();
        int oldLen = end - start;
        int n = fake.length();
        for (int i = 0; i < n; i++) {
            int src = start + Math.min(oldLen - 1, (int) ((long) i * oldLen / n));
            t.append(Text.literal(String.valueOf(fake.charAt(i))).setStyle(f.styles.get(src)));
        }
        return t;
    }

    private static Text rankText(Flat f, int start, ModConfig cfg) {
        Style base = f.styles.get(start).withColor((TextColor) null);
        MutableText r = Text.empty().setStyle(base);
        r.append(ScoreboardChangerClient.parseColoredText(RankColors.styled(cfg.fakeRankColor, cfg.fakeRank)));
        return r;
    }

    // ---------- Text <-> runs ----------

    private static Flat flatten(Text t) {
        Flat f = new Flat();
        t.<Object>visit((style, str) -> {
            for (int i = 0; i < str.length(); i++) {
                f.text.append(str.charAt(i));
                f.styles.add(style);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return f;
    }

    private static List<ModConfig.Run> toRuns(Flat f) {
        List<ModConfig.Run> runs = new ArrayList<>();
        int i = 0;
        int n = f.text.length();
        while (i < n) {
            Style st = f.styles.get(i);
            int j = i + 1;
            while (j < n && f.styles.get(j).equals(st)) j++;
            ModConfig.Run r = new ModConfig.Run();
            r.text = f.text.substring(i, j);
            r.color = st.getColor() != null ? st.getColor().getRgb() : -1;
            r.bold = st.isBold();
            r.italic = st.isItalic();
            r.underline = st.isUnderlined();
            r.strike = st.isStrikethrough();
            r.obfuscated = st.isObfuscated();
            runs.add(r);
            i = j;
        }
        return runs;
    }

    private static Flat fromRuns(List<ModConfig.Run> runs) {
        Flat f = new Flat();
        for (ModConfig.Run r : runs) {
            Style s = Style.EMPTY;
            if (r.color >= 0) s = s.withColor(TextColor.fromRgb(r.color));
            if (r.bold) s = s.withBold(true);
            if (r.italic) s = s.withItalic(true);
            if (r.underline) s = s.withUnderline(true);
            if (r.strike) s = s.withStrikethrough(true);
            if (r.obfuscated) s = s.withObfuscated(true);
            for (int i = 0; i < r.text.length(); i++) {
                f.text.append(r.text.charAt(i));
                f.styles.add(s);
            }
        }
        return f;
    }
}
