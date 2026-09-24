package com.scoreboardchanger.mixin;

import com.scoreboardchanger.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DrawContext.class)
public class ScoreboardObjectiveRendererMixin {

    @Inject(
        method = "drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)I",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onDrawTextWithShadow(TextRenderer textRenderer, Text text, int x, int y, int color, CallbackInfoReturnable<Integer> cir) {
        ModConfig cfg = ModConfig.getInstance();
        if (!cfg.enabled) return;

        String raw = text.getString();
        Text replacement = tryReplace(raw, cfg);
        if (replacement != null) {
            DrawContext self = (DrawContext)(Object)this;
            int result = self.drawTextWithShadow(textRenderer, replacement, x, y, color);
            cir.setReturnValue(result);
        }
    }

    private Text tryReplace(String raw, ModConfig cfg) {
        if (containsStripped(raw, "Ранг:"))      return Text.literal("§7Ранг: " + cfg.fakeRankColor + cfg.fakeRank);
        if (containsStripped(raw, "Монет:"))     return Text.literal("§7Монет: §6" + cfg.fakeCoins);
        if (containsStripped(raw, "Токенов:"))   return Text.literal("§7Токенов: §b" + cfg.fakeTokens);
        if (containsStripped(raw, "Черепков:"))  return Text.literal("§7Черепков: §d" + cfg.fakeSkulls);
        if (containsStripped(raw, "Убийств:"))   return Text.literal("§7Убийств: §a" + cfg.fakeKills);
        if (containsStripped(raw, "Смертей:"))   return Text.literal("§7Смертей: §c" + cfg.fakeDeaths);
        if (containsStripped(raw, "Наиграно:"))  return Text.literal("§7Наиграно: §e" + cfg.fakePlaytime);
        // Nickname: matches exact stored value
        String stripped = raw.replaceAll("§.", "").trim();
        if (stripped.equals(cfg.fakeNickname) && !cfg.fakeNickname.isEmpty()) {
            return Text.literal(cfg.fakeNickname);
        }
        return null;
    }

    private boolean containsStripped(String text, String keyword) {
        return text.replaceAll("§.", "").contains(keyword);
    }
}
