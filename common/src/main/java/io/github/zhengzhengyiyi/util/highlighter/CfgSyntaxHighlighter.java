package io.github.zhengzhengyiyi.util.highlighter;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

public class CfgSyntaxHighlighter implements HighLighter{
    
    public enum TokenType {
        COMMENT,
        KEY,
        EQUALS,
        VALUE,
        STRING_VALUE,
        NUMBER_VALUE,
        BOOLEAN_VALUE,
        SECTION_HEADER,
        TEXT
    }
    
    private static class Token {
        public final TokenType type;
        public final String content;
        
        public Token(TokenType type, String content) {
            this.type = type;
            this.content = content;
        }
    }
    
    public int getCharIndexFromTokens(TextRenderer textRenderer, String line, int targetX) {
        if (line.isEmpty()) return 0;
        
        for (int i = 0; i <= line.length(); i++) {
            int width = getTextWidthUpToChar(textRenderer, line, i);
            if (width >= targetX) {
                return i;
            }
        }
        return line.length();
    }
    
    public int getTextWidthUpToChar(TextRenderer textRenderer, String line, int charIndex) {
        if (line.isEmpty() || charIndex <= 0) return 0;
        
        List<Token> tokens = tokenizeLine(line);
        int currentCharIndex = 0;
        int totalWidth = 0;
        
        for (Token token : tokens) {
            int tokenLength = token.content.length();
            if (currentCharIndex + tokenLength >= charIndex) {
                int charsInThisToken = charIndex - currentCharIndex;
                if (charsInThisToken > 0) {
                    totalWidth += textRenderer.getWidth(token.content.substring(0, charsInThisToken));
                }
                break;
            } else {
                totalWidth += textRenderer.getWidth(token.content);
                currentCharIndex += tokenLength;
            }
        }
        
        return totalWidth;
    }
    
    public void drawHighlightedText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, boolean editable) {
        if (text.isEmpty()) return;
        
        List<Token> tokens = tokenizeLine(text);
        int currentX = x;
        
        for (Token token : tokens) {
            int color = getTokenColor(token.type, editable);
            context.drawText(textRenderer, token.content, currentX, y, color, false);
            currentX += textRenderer.getWidth(token.content);
        }
    }
    
    private List<Token> tokenizeLine(String line) {
        List<Token> tokens = new ArrayList<>();
        if (line.isEmpty()) return tokens;
        
        String trimmedLine = line.trim();
        
        if (trimmedLine.startsWith("#") || trimmedLine.startsWith(";")) {
            tokens.add(new Token(TokenType.COMMENT, line));
            return tokens;
        }
        
        if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
            tokens.add(new Token(TokenType.SECTION_HEADER, line));
            return tokens;
        }
        
        int equalsIndex = line.indexOf('=');
        if (equalsIndex == -1) {
            tokens.add(new Token(TokenType.TEXT, line));
            return tokens;
        }
        
        String beforeEquals = line.substring(0, equalsIndex);
        String afterEquals = line.substring(equalsIndex + 1);
        
        String keyPart = beforeEquals.trim();
        int keyStart = beforeEquals.indexOf(keyPart);
        
        if (keyStart > 0) {
            tokens.add(new Token(TokenType.TEXT, beforeEquals.substring(0, keyStart)));
        }
        
        if (!keyPart.isEmpty()) {
            tokens.add(new Token(TokenType.KEY, keyPart));
        }
        
        tokens.add(new Token(TokenType.EQUALS, "="));
        
        if (!afterEquals.isEmpty()) {
            String value = afterEquals.trim();
            TokenType valueType = determineValueType(value);
            tokens.add(new Token(valueType, afterEquals));
        }
        
        return tokens;
    }
    
    private TokenType determineValueType(String value) {
        if (value.startsWith("\"") || value.startsWith("'")) {
            return TokenType.STRING_VALUE;
        }
        
        if (value.equals("true") || value.equals("false") || value.equals("yes") || value.equals("no") || 
            value.equals("on") || value.equals("off") || value.equals("enable") || value.equals("disable")) {
            return TokenType.BOOLEAN_VALUE;
        }
        
        if (value.matches("-?\\d+(\\.\\d+)?")) {
            return TokenType.NUMBER_VALUE;
        }
        
        return TokenType.VALUE;
    }
    
    public int getTextWidth(TextRenderer textRenderer, String text) {
        if (text == null || text.isEmpty()) return 0;
        
        List<Token> tokens = tokenizeLine(text);
        int totalWidth = 0;
        
        for (Token token : tokens) {
            totalWidth += textRenderer.getWidth(token.content);
        }
        
        return totalWidth;
    }
    
    private int getTokenColor(TokenType type, boolean editable) {
        int defaultColor = editable ? 0xFFFFFFFF : 0xFFAAAAAA;
        
        switch (type) {
            case COMMENT:
                return 0xFF6A9955;
            case SECTION_HEADER:
                return 0xFFFFD700;
            case KEY:
                return 0xFF5CD0F3;
            case EQUALS:
                return 0xFFAAAAAA;
            case STRING_VALUE:
                return 0xFFE6DB74;
            case NUMBER_VALUE:
                return 0xFFAE81FF;
            case BOOLEAN_VALUE:
                return 0xFF569CD6;
            case VALUE:
                return 0xFFE6DB74;
            case TEXT:
            default:
                return defaultColor;
        }
    }
}
