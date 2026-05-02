package io.github.zhengzhengyiyi.addon;

import io.github.zhengzhengyiyi.api.ApiEntrypoint;
import io.github.zhengzhengyiyi.gui.*;
import io.github.zhengzhengyiyi.gui.widget.MultilineEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.resources.Identifier;

public class TextStatsEntrypoint implements ApiEntrypoint {
    private boolean enabled = true;
    private EditorScreen editor;
    private int statsX = 80;
    private int statsY = 15;

    // Cached stats — only recomputed when the text reference changes
    private String lastText = null;
    private String cachedStats = "";

    @Override
    public void init() {}

    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath("zhengzhengyiyi", "text_stats_display");
    }

    @Override
    public void onEditerOpen(EditorScreen editor) {
        this.editor = editor;
        statsX = this.editor.width / 2 + 35;
        statsY = this.editor.height - 10;
        // Invalidate cache when editor opens
        lastText = null;
        cachedStats = "";
    }

    @Override
    public void renderButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!enabled || editor == null || editor.getTextWidget() == null) return;

        AbstractWidget textWidget = editor.getTextWidget();
        String text = "";
        if (textWidget instanceof MultilineEditor me) {
            text = me.text;
        } else {
            LOGGER.error("can not find current text");
        }

        // Only recompute stats when the text has actually changed
        if (!text.equals(lastText)) {
            lastText = text;
            int charCount = text.length();
            int wordCount = text.isBlank() ? 0 : text.split("\\s+").length;
            int lineCount = text.isEmpty() ? 0 : text.split("\n", -1).length;
            cachedStats = "chars: " + charCount + " | words: " + wordCount + " | lines: " + lineCount;
        }

        if (!cachedStats.isEmpty()) {
            context.text(Minecraft.getInstance().font, cachedStats, statsX, statsY, 0xFFFFFF00, false);
        }
    }
}
