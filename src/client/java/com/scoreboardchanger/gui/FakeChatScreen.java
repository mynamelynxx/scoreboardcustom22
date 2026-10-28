package com.scoreboardchanger.gui;

import com.scoreboardchanger.config.ModConfig;
import com.scoreboardchanger.feature.FakeChat;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

/** Настройки подмены чата. */
public class FakeChatScreen extends Screen {
    private String formatText;
    private String realNameText;
    private String testText = "";
    private boolean backgroundDrawn;
    private ButtonWidget modeBtn;

    public FakeChatScreen() {
        super(Text.literal("Подмена чата"));
        ModConfig cfg = ModConfig.getInstance();
        formatText = cfg.chatFormat;
        realNameText = cfg.chatRealName;
    }

    @Override
    protected void init() {
        ModConfig cfg = ModConfig.getInstance();
        int cx = this.width / 2;

        modeBtn = addDrawableChild(ButtonWidget.builder(modeLabel(cfg), b -> {
            cfg.fakeChatMode = !cfg.fakeChatMode;
            ModConfig.save();
            b.setMessage(modeLabel(cfg));
        }).dimensions(cx - 150, 24, 300, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Запомнить формат из чата"), b -> {
            applyFields();
            FakeChat.startLearn();
            FakeChat.info("Напиши в чат любое короткое слово (оно уйдёт на сервер). Формат запомнится.");
            this.client.setScreen(null);
        }).dimensions(cx - 150, 48, 148, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить запомненный"), b -> {
            FakeChat.reset();
        }).dimensions(cx + 2, 48, 148, 20).build());

        field(cx - 150, 92, 300, 400, formatText, "Запасной формат: {rank} {name} {msg}", s -> formatText = s);
        field(cx - 150, 128, 300, 32, realNameText, "Твой настоящий ник в чате (пусто = ник аккаунта)", s -> realNameText = s);
        field(cx - 150, 164, 200, 256, testText, "Тестовое сообщение", s -> testText = s);

        addDrawableChild(ButtonWidget.builder(Text.literal("Отправить"), b -> {
            applyFields();
            if (!testText.isBlank()) {
                String msg = testText;
                this.client.setScreen(null);
                FakeChat.send(msg);
            }
        }).dimensions(cx + 54, 164, 96, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(cx - 50, this.height - 26, 100, 20).build());
    }

    private Text modeLabel(ModConfig cfg) {
        return Text.literal(cfg.fakeChatMode
                ? "§aПодмена чата: ВКЛ §7(всё, что пишешь, видишь только ты)"
                : "§cПодмена чата: ВЫКЛ");
    }

    private void field(int x, int y, int w, int maxLength, String value, String hint, Consumer<String> onChange) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, x, y, w, 20, Text.literal(hint));
        f.setMaxLength(maxLength);
        f.setText(value);
        f.setSuggestion(value.isEmpty() ? hint : null);
        f.setChangedListener(s -> {
            onChange.accept(s);
            f.setSuggestion(s.isEmpty() ? hint : null);
        });
        addDrawableChild(f);
    }

    private void applyFields() {
        ModConfig cfg = ModConfig.getInstance();
        if (!formatText.isBlank()) cfg.chatFormat = formatText;
        cfg.chatRealName = realNameText.trim();
        ModConfig.save();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        if (backgroundDrawn) return;
        backgroundDrawn = true;
        super.renderBackground(context, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        backgroundDrawn = false;
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        int cx = this.width / 2;
        ModConfig cfg = ModConfig.getInstance();
        context.drawCenteredTextWithShadow(textRenderer, this.title, cx, 8, 0xFFFFFF);

        boolean learned = cfg.chatTemplate != null && cfg.chatTemplate.runs != null && !cfg.chatTemplate.runs.isEmpty();
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(learned ? "§aФормат запомнен из реального чата" : "§7Формат не запомнен — работает запасной"),
                cx, 72, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, "Запасной формат:", cx - 150, 83, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Настоящий ник:", cx - 150, 118, 0xAAAAAA);
        context.drawTextWithShadow(textRenderer, "Тест:", cx - 150, 154, 0xAAAAAA);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("§7Команды: .fc текст · .fc on/off · .fc learn · .fc reset"),
                cx, this.height - 40, 0x888888);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        applyFields();
        this.client.setScreen(new ScoreboardChangerScreen());
    }
}
