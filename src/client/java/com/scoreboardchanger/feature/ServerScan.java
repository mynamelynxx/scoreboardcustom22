package com.scoreboardchanger.feature;

import com.scoreboardchanger.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Читает НАСТОЯЩИЙ скорборд сервера: номер анархии и реальный ранг. */
public final class ServerScan {
    private static final Pattern ANARCHY = Pattern.compile("(?iu)(?:анархи[яиюе]|anarchy)\\s*[-–—:#№]?\\s*(\\d{1,4})");
    private static final Pattern RANK = Pattern.compile("(?iu)ранг\\s*:\\s*(.+)");

    private static String anarchy = null;
    private static String realRank = null;
    private static int tick = 0;

    private ServerScan() {}

    public static void tick(MinecraftClient c) {
        if (c.world == null) {
            anarchy = null;
            realRank = null;
            return;
        }
        if (++tick % 10 != 0) return;
        try {
            scan(c);
        } catch (Exception e) {
            // не ломаем игру из-за чтения скорборда
        }
    }

    private static void scan(MinecraftClient c) {
        Scoreboard sb = c.world.getScoreboard();
        ScoreboardObjective obj = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (obj == null) return;

        List<String> lines = new ArrayList<>();
        lines.add(strip(obj.getDisplayName().getString()));

        for (ScoreboardEntry e : sb.getScoreboardEntries(obj)) {
            String owner = e.owner();
            if (owner == null || owner.startsWith("#")) continue;
            String name = e.display() != null ? e.display().getString() : owner;
            Team team = sb.getScoreHolderTeam(owner);
            String prefix = team != null ? team.getPrefix().getString() : "";
            String suffix = team != null ? team.getSuffix().getString() : "";
            lines.add(strip(prefix + name + suffix));
        }

        for (String line : lines) {
            Matcher m = ANARCHY.matcher(line);
            if (m.find()) {
                anarchy = m.group(1);
                break;
            }
        }
        for (String line : lines) {
            Matcher m = RANK.matcher(line);
            if (m.find()) {
                String r = m.group(1).trim();
                if (!r.isEmpty()) realRank = r;
                break;
            }
        }
    }

    private static String strip(String s) {
        return s.replaceAll("§.", "").trim();
    }

    /** Номер анархии с сервера, либо запасное значение из поля, если скорборд не найден. */
    public static String anarchy(ModConfig cfg) {
        if (cfg.autoAnarchy && anarchy != null) return anarchy;
        return cfg.fakeSkulls;
    }

    public static String detectedAnarchy() {
        return anarchy;
    }

    public static String realRank() {
        return realRank;
    }
}
