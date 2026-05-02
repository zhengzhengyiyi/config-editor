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
    }

    @Override
    public void renderButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!enabled || editor == null || editor.getTextWidget() == null) return;
        
        String text = "";
        AbstractWidget textWidget = editor.getTextWidget();
        
        if (textWidget instanceof MultilineEditor) {
    		text = ((MultilineEditor)textWidget).text;
        } else {
			LOGGER.error("can not find current text");
		}
        
        int charCount = text.length();
        int wordCount = text.trim().isEmpty() ? 0 : text.split("\\s+").length;
        int lineCount = text.isEmpty() ? 0 : text.split("\n").length;
        
        String stats = "letter: " + charCount + " | words: " + wordCount + " | Lines: " + lineCount;
        context.text(Minecraft.getInstance().font, stats, statsX, statsY, 0xFFFFFF00, false);
    }
}
