package io.github.zhengzhengyiyi.api;

import io.github.zhengzhengyiyi.util.highlighter.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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
	    
	    @Override
	    public void drawHighlightedText(GuiGraphicsExtractor context, Font font, String text, int x, int y, boolean editable) {
	        if (text == null || text.isEmpty()) return;
	        
	        int color = editable ? 0xFFFFFFFF : 0xFFAAAAAA;
	        context.text(font, text, x, y, color, false);
	    }
	    
	    @Override
	    public int getTextWidth(Font font, String text) {
	        if (text == null || text.isEmpty()) return 0;
	        return font.width(text);
	    }
	    
	    @Override
	    public int getCharIndexFromTokens(Font font, String line, int targetX) {
	        if (line == null || line.isEmpty()) return 0;
	        
	        int currentWidth = 0;
	        for (int i = 0; i < line.length(); i++) {
	            char c = line.charAt(i);
	            int charWidth = font.width(String.valueOf(c));
	            if (currentWidth + charWidth > targetX) {
	                return i;
	            }
	            currentWidth += charWidth;
	        }
	        return line.length();
	    }
	    
	    @Override
	    public int getTextWidthUpToChar(Font font, String line, int charIndex) {
	        if (line == null || line.isEmpty() || charIndex <= 0) return 0;
	        
	        if (charIndex >= line.length()) {
	            return font.width(line);
	        }
	        
	        String substring = line.substring(0, charIndex);
	        return font.width(substring);
	    }
	}
}
