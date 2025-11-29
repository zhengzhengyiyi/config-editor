package io.github.zhengzhengyiyi.api;

import io.github.zhengzhengyiyi.util.highlighter.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public enum FileType {
	JSON("json", new JsonSyntaxHighlighter()),
	YML("yml", new DefaultHighlighter()),
    YAML("yaml", new YamlSyntaxHighlighter()),
    TXT("txt", new TextSyntaxHighlighter()),
    PROPERTIES("properties", new PropertiesSyntaxHighlighter()),
    TOML("toml", new TomlSyntaxHighlighter()),
    CONF("conf", new DefaultHighlighter()),
    CFG("cfg", new CfgSyntaxHighlighter()),
    INI("ini", new DefaultHighlighter()),
    JSON5("json5", new JsonSyntaxHighlighter()),
	UNKNOW("UNKNOW", new DefaultHighlighter());
	
	private final String extension;
	private final HighLighter highLighter;
	
	FileType(String extension, HighLighter highLighter) {
		this.extension = extension;
		this.highLighter = highLighter;
	}
	
	public String getExtension() {
		return this.extension;
	}
	
	public HighLighter getHighLighter() {
		return this.highLighter;
	}

	/**
	 * 
	 * A simple syntax highlighter implementation that does not perform any syntax highlighting,
	 * serving only as a basic text renderer.
	 */
	public static class DefaultHighlighter implements HighLighter {
	    
	    /**
	     * {@inheritDoc}
	     * 
	     * Simply draws the text without any syntax highlighting.
	     */
	    @Override
	    public void drawHighlightedText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, boolean editable) {
	        if (text == null || text.isEmpty()) return;
	        
	        int color = editable ? 0xFFFFFFFF : 0xFFAAAAAA;
	        context.drawText(textRenderer, text, x, y, color, false);
	    }
	    
	    /**
	     * {@inheritDoc}
	     * 
	     * Returns the raw width of the text.
	     */
	    @Override
	    public int getTextWidth(TextRenderer textRenderer, String text) {
	        if (text == null || text.isEmpty()) return 0;
	        return textRenderer.getWidth(text);
	    }
	    
	    /**
	     * {@inheritDoc}
	     * Finds the character index by measuring width character by character.
	     */
	    @Override
	    public int getCharIndexFromTokens(TextRenderer textRenderer, String line, int targetX) {
	        if (line == null || line.isEmpty()) return 0;
	        
	        int currentWidth = 0;
	        for (int i = 0; i < line.length(); i++) {
	            char c = line.charAt(i);
	            int charWidth = textRenderer.getWidth(String.valueOf(c));
	            if (currentWidth + charWidth > targetX) {
	                return i;
	            }
	            currentWidth += charWidth;
	        }
	        return line.length();
	    }
	    
	    /**
	     * {@inheritDoc}
	     * 
	     * Calculates the text width from the beginning of the line to the specified character index.
	     */
	    @Override
	    public int getTextWidthUpToChar(TextRenderer textRenderer, String line, int charIndex) {
	        if (line == null || line.isEmpty() || charIndex <= 0) return 0;
	        
	        if (charIndex >= line.length()) {
	            return textRenderer.getWidth(line);
	        }
	        
	        String substring = line.substring(0, charIndex);
	        return textRenderer.getWidth(substring);
	    }
	}
}
