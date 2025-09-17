package io.github.zhengzhengyiyi.addon;

import io.github.zhengzhengyiyi.api.ApiEntrypoint;
import io.github.zhengzhengyiyi.gui.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class TextStatsEntrypoint implements ApiEntrypoint {
    private boolean enabled = true;
    private EditorScreen editor;
    private int statsX = 80;
    private int statsY = 15;

    @Override
    public void init() {}

    @Override
    public void onEditerOpen(EditorScreen editor) {
        this.editor = editor;
        
        statsX = this.editor.width / 2 - 30;
        statsY = this.editor.height - 10;
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!enabled || editor == null || editor.getTextWidget() == null) return;
        
        String text = editor.getTextWidget().getText();
        int charCount = text.length();
        int wordCount = text.trim().isEmpty() ? 0 : text.split("\\s+").length;
        int lineCount = text.isEmpty() ? 0 : text.split("\n").length;
        
        String stats = "letter: " + charCount + " | words: " + wordCount + " | Lines: " + lineCount;
        context.drawText(MinecraftClient.getInstance().textRenderer, stats, statsX, statsY, 0xFFFFFF00, false);
    }
}
