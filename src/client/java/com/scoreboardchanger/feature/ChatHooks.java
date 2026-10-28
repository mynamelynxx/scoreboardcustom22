package com.scoreboardchanger.feature;

import com.scoreboardchanger.config.ModConfig;
import com.scoreboardchanger.gui.ShopScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Перехват того, что ты пишешь в чат. Локальные команды (на сервер не уходят):
 *   .tshop            открыть фейковый магазин (также /tshop)
 *   .fc текст         отправить фейковое сообщение
 *   .fc on / off      включить / выключить подмену всех сообщений
 *   .fc learn         запомнить формат чата
 *   .fc reset         забыть запомненный формат
 */
public final class ChatHooks {
    private static Supplier<Screen> pending = null;

    private ChatHooks() {}

    public static void open(Supplier<Screen> screen) {
        pending = screen;
    }

    /** Экран открываем в тике: чат после отправки сам закрывается и затёр бы наш экран. */
    public static void tick(MinecraftClient c) {
        if (pending != null && c.currentScreen == null) {
            Supplier<Screen> p = pending;
            pending = null;
            c.setScreen(p.get());
        }
    }

    public static boolean onChat(String message) {
        ModConfig cfg = ModConfig.getInstance();

        // режим обучения: настоящее сообщение уходит на сервер, мы ловим его эхо
        if (FakeChat.learnPending && FakeChat.learnText == null && !message.startsWith("/")) {
            FakeChat.learnText = message.trim();
            return true;
        }

        if (!cfg.enabled) return true;
        String m = message.trim();
        if (m.isEmpty()) return true;

        if (m.startsWith(".")) {
            String[] parts = m.substring(1).split("\\s+", 2);
            String cmd = parts[0].toLowerCase(Locale.ROOT);
            String arg = parts.length > 1 ? parts[1].trim() : "";

            if (!cfg.shopCommand.isBlank() && cmd.equals(cfg.shopCommand.toLowerCase(Locale.ROOT))) {
                open(ShopScreen::new);
                return false;
            }
            if (cmd.equals("fc")) {
                handleFc(cfg, arg);
                return false;
            }
        }

        if (cfg.fakeChatMode) {
            FakeChat.send(message);
            return false;
        }
        return true;
    }

    public static boolean onCommand(String command) {
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.enabled || cfg.shopCommand == null || cfg.shopCommand.isBlank()) return true;
        String first = command.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        if (first.equals(cfg.shopCommand.toLowerCase(Locale.ROOT))) {
            open(ShopScreen::new);
            return false;
        }
        return true;
    }

    private static void handleFc(ModConfig cfg, String arg) {
        String a = arg.toLowerCase(Locale.ROOT);
        if (arg.isEmpty()) {
            cfg.fakeChatMode = !cfg.fakeChatMode;
            ModConfig.save();
            FakeChat.info("Подмена чата: " + (cfg.fakeChatMode ? "ВКЛ" : "ВЫКЛ"));
        } else if (a.equals("on")) {
            cfg.fakeChatMode = true;
            ModConfig.save();
            FakeChat.info("Подмена чата: ВКЛ. Всё, что пишешь, остаётся только у тебя.");
        } else if (a.equals("off")) {
            cfg.fakeChatMode = false;
            ModConfig.save();
            FakeChat.info("Подмена чата: ВЫКЛ");
        } else if (a.equals("learn")) {
            FakeChat.startLearn();
            FakeChat.info("Напиши в чат любое короткое слово (оно уйдёт на сервер), формат запомнится.");
        } else if (a.equals("reset")) {
            FakeChat.reset();
            FakeChat.info("Запомненный формат сброшен, используется шаблон.");
        } else {
            FakeChat.send(arg);
        }
    }
}
