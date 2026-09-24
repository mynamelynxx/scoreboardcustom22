package com.scoreboardchanger;

import com.scoreboardchanger.config.ModConfig;
import com.scoreboardchanger.gui.ScoreboardChangerScreen;
import com.scoreboardchanger.util.RankColors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.text.Style;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ScoreboardChangerClient implements ClientModInitializer {

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.scoreboardchanger.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_ALT,
                "category.scoreboardchanger"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openGuiKey.wasPressed() && client.currentScreen == null) {
                client.setScreen(new ScoreboardChangerScreen());
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            ModConfig cfg = ModConfig.getInstance();
            if (!cfg.enabled && !cfg.debugMode) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null) return;

            renderOverlay(drawContext, client, cfg);
        });
    }

    private void renderOverlay(DrawContext context, MinecraftClient client, ModConfig cfg) {
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int lineHeight = textRenderer.fontHeight + 1;

        List<Text> lines = buildFakeLines(cfg);
        int maxWidth = 100;
        for (Text line : lines) {
            maxWidth = Math.max(maxWidth, textRenderer.getWidth(line));
        }
        maxWidth += 6;

        int panelRight = screenWidth - 2 + cfg.offsetX;
        int panelLeft = panelRight - maxWidth;
        int startY = 10 + cfg.offsetY;

        for (int i = 0; i < lines.size(); i++) {
            int lineY = startY + lineHeight + i * lineHeight;
            context.drawText(textRenderer, lines.get(i), panelLeft + 3, lineY, 0xFFFFFF, false);
        }
    }

    private List<Text> buildFakeLines(ModConfig cfg) {
        List<Text> lines = new ArrayList<>();
        lines.add(parseColoredText("&#fc8a1a&l  ⚡ &#fc1a1a&lАнархия-" + cfg.fakeSkulls));
        lines.add(parseColoredText("&#FC1A1A╔&#F21717═&#E81515═&#DE1212═&#D41010═&#CA0D0D═&#BF0A0A═&#B50808═&#AB0505═&#A10303═&#970000═"));
        lines.add(parseColoredText("&#fc1a1a╠╣ &#fc9700" + cfg.fakeNickname));
        lines.add(parseColoredText("&#fc1a1a╠ &#00fcfc⭐ §fРанг: " + RankColors.styled(cfg.fakeRankColor, cfg.fakeRank)));
        lines.add(parseColoredText("&#fc1a1a╠ &#fcfc1a$ §fМонет: &#fcfc1a" + cfg.fakeCoins));
        lines.add(parseColoredText("&#fc1a1a╠ &#00fc00⛁ §fТокенов: &#00fc00" + cfg.fakeTokens));
        lines.add(parseColoredText("&#fc1a1a╠"));
        lines.add(parseColoredText("&#fc1a1a╠╣ &#fc9700Статистика"));
        lines.add(parseColoredText("&#fc1a1a╠ §fУбийств: &#fcfc32" + cfg.fakeKills));
        lines.add(parseColoredText("&#fc1a1a╠ &#fcfce3Смертей: &#fc3200" + cfg.fakeDeaths));
        lines.add(parseColoredText("&#fc1a1a╠ &#fcfce3Наиграно: &#3297fc" + cfg.fakePlaytime));
        lines.add(parseColoredText("&#fc1a1a╠"));
        lines.add(parseColoredText("&#fc1a1a╠╣ &#fcfce3Донат &#65fc32/don"));
        lines.add(parseColoredText("&#FC1A1A╚&#F21717═&#E81515═&#DE1212═&#D41010═&#CA0D0D═&#BF0A0A═&#B50808═&#AB0505═&#A10303═&#970000═"));
        return lines;
    }

    public static Text parseColoredText(String raw) {
        if (raw == null || raw.isEmpty()) return Text.empty();

        // Преобразуем §x§F§F§0§0§2§E в &#FF002E
        String converted = raw;
        Pattern rgbPattern = Pattern.compile("§x(§[0-9A-Fa-f]){6}");
        var rgbMatcher = rgbPattern.matcher(converted);
        while (rgbMatcher.find()) {
            String fullMatch = rgbMatcher.group();
            StringBuilder hex = new StringBuilder("&#");
            for (int i = 2; i < fullMatch.length(); i += 2) {
                char digit = fullMatch.charAt(i + 1);
                hex.append(digit);
            }
            converted = converted.replace(fullMatch, hex.toString());
        }

        Pattern pattern = Pattern.compile("(&?#[0-9a-fA-F]{6})|([&§][0-9a-z])");
        var matcher = pattern.matcher(converted);
        MutableText result = Text.empty();
        int lastEnd = 0;
        Style currentStyle = Style.EMPTY;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String textPart = converted.substring(lastEnd, matcher.start());
                result.append(Text.literal(textPart).setStyle(currentStyle));
            }
            String code = matcher.group();
            if (code.startsWith("&#") || code.startsWith("#")) {
                String hex = code.startsWith("&#") ? code.substring(2) : code.substring(1);
                var dataResult = TextColor.parse("#" + hex);
                if (dataResult.result().isPresent()) {
                    currentStyle = currentStyle.withColor(dataResult.result().get());
                }
            } else {
                char c = code.charAt(1);
                currentStyle = applyFormatting(currentStyle, c);
            }
            lastEnd = matcher.end();
        }
        if (lastEnd < converted.length()) {
            result.append(Text.literal(converted.substring(lastEnd)).setStyle(currentStyle));
        }
        return result;
    }

    private static Style applyFormatting(Style style, char code) {
        switch (code) {
            case '0': return style.withColor(TextColor.fromRgb(0x000000));
            case '1': return style.withColor(TextColor.fromRgb(0x0000AA));
            case '2': return style.withColor(TextColor.fromRgb(0x00AA00));
            case '3': return style.withColor(TextColor.fromRgb(0x00AAAA));
            case '4': return style.withColor(TextColor.fromRgb(0xAA0000));
            case '5': return style.withColor(TextColor.fromRgb(0xAA00AA));
            case '6': return style.withColor(TextColor.fromRgb(0xFFAA00));
            case '7': return style.withColor(TextColor.fromRgb(0xAAAAAA));
            case '8': return style.withColor(TextColor.fromRgb(0x555555));
            case '9': return style.withColor(TextColor.fromRgb(0x5555FF));
            case 'a': return style.withColor(TextColor.fromRgb(0x55FF55));
            case 'b': return style.withColor(TextColor.fromRgb(0x55FFFF));
            case 'c': return style.withColor(TextColor.fromRgb(0xFF5555));
            case 'd': return style.withColor(TextColor.fromRgb(0xFF55FF));
            case 'e': return style.withColor(TextColor.fromRgb(0xFFFF55));
            case 'f': return style.withColor(TextColor.fromRgb(0xFFFFFF));
            case 'l': return style.withBold(true);
            case 'o': return style.withItalic(true);
            case 'n': return style.withUnderline(true);
            case 'm': return style.withStrikethrough(true);
            case 'r': return Style.EMPTY;
            default: return style;
        }
    }
}
