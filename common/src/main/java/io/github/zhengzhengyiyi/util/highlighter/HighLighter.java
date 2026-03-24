package io.github.zhengzhengyiyi.util.highlighter;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * A syntax highlighter interface that provides methods for text rendering and measurement
 * with syntax highlighting capabilities.
 * 
 * Implementations of this interface should provide syntax highlighting for specific
 * programming languages or data formats by tokenizing text and applying appropriate
 * colors to different token types.
 */
public interface HighLighter {
    
    /**
     * Draws syntax-highlighted text at the specified position.
     * 
     * @param context the draw context used for rendering
     * @param textRenderer the text renderer for measuring and drawing text
     * @param text the text to be highlighted and drawn
     * @param x the x-coordinate of the starting position
     * @param y the y-coordinate of the starting position
     * @param editable whether the text is in an editable context, which may affect color scheme
     */
    void drawHighlightedText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, boolean editable);
    
    /**
     * Calculates the total width of the syntax-highlighted text.
     * 
     * @param textRenderer the text renderer for measuring text width
     * @param text the text to be measured
     * @return the total width in pixels of the highlighted text
     */
    int getTextWidth(TextRenderer textRenderer, String text);
    
    /**
     * Finds the character index in the text that corresponds to the given x-coordinate.
     * 
     * @param textRenderer the text renderer for measuring text width
     * @param line the text line to search in
     * @param targetX the target x-coordinate in pixels
     * @return the character index at or before the target x-coordinate
     */
    int getCharIndexFromTokens(TextRenderer textRenderer, String line, int targetX);
    
    /**
     * Calculates the text width from the beginning of the line up to the specified character index.
     * 
     * @param textRenderer the text renderer for measuring text width
     * @param line the text line to measure
     * @param charIndex the character index to measure up to (exclusive)
     * @return the width in pixels of the text from start to the specified character index
     */
    int getTextWidthUpToChar(TextRenderer textRenderer, String line, int charIndex);
}
