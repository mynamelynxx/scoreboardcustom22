package com.scoreboardchanger.feature;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.util.Optional;

/** F8 в любом меню сервера = записать его содержимое в config/scoreboardchanger_menus.txt */
public final class MenuDump {
    private static final int KEY = GLFW.GLFW_KEY_F8;

    private MenuDump() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof HandledScreen<?> hs) {
                ScreenKeyboardEvents.afterKeyPress(screen).register((s, key, scancode, modifiers) -> {
                    if (key == KEY) dump(client, hs);
                });
            }
        });
    }

    private static void dump(MinecraftClient client, HandledScreen<?> screen) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== МЕНЮ: ").append(codes(screen.getTitle())).append("  [").append(LocalTime.now().withNano(0)).append("] ===\n");
        int count = 0;
        for (Slot slot : screen.getScreenHandler().slots) {
            Inventory inv = slot.inventory;
            if (inv instanceof net.minecraft.entity.player.PlayerInventory) continue; // свой инвентарь не пишем
            ItemStack st = slot.getStack();
            if (st.isEmpty()) continue;
            count++;
            sb.append("[слот ").append(slot.id).append("] ")
              .append(Registries.ITEM.getId(st.getItem())).append(" x").append(st.getCount()).append('\n');
            sb.append("  название: ").append(codes(st.getName())).append('\n');
            LoreComponent lore = st.get(DataComponentTypes.LORE);
            if (lore != null) {
                for (Text line : lore.lines()) sb.append("  лор: ").append(codes(line)).append('\n');
            }
        }
        sb.append('\n');
        try {
            Path file = FabricLoader.getInstance().getConfigDir().resolve("scoreboardchanger_menus.txt");
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            FakeChat.info("Меню сохранено (" + count + " предм.): config/scoreboardchanger_menus.txt");
        } catch (Exception e) {
            FakeChat.info("Не удалось сохранить меню: " + e.getMessage());
        }
    }

    /** Text -> строка с кодами &#rrggbb, &l, &o ... (как в нашем парсере). */
    private static String codes(Text t) {
        StringBuilder out = new StringBuilder();
        t.<Object>visit((style, str) -> {
            if (str.isEmpty()) return Optional.empty();
            out.append("&r");
            if (style.getColor() != null) out.append(String.format("&#%06x", style.getColor().getRgb()));
            if (style.isBold()) out.append("&l");
            if (style.isItalic()) out.append("&o");
            if (style.isUnderlined()) out.append("&n");
            if (style.isStrikethrough()) out.append("&m");
            out.append(str);
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }
}
