package io.github.zhengzhengyiyi.util.highlighter;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

public class YamlSyntaxHighlighter implements HighLighter{
    
    public enum TokenType {
        COMMENT,
        KEY,
        COLON,
        VALUE,
        STRING_VALUE,
        NUMBER_VALUE,
        BOOLEAN_VALUE,
        NULL_VALUE,
        LIST_ITEM,
        INDENTATION,
        DOCUMENT_MARKER,
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
        
        if (trimmedLine.startsWith("#")) {
            tokens.add(new Token(TokenType.COMMENT, line));
            return tokens;
        }
        
        if (trimmedLine.equals("---") || trimmedLine.equals("...")) {
            tokens.add(new Token(TokenType.DOCUMENT_MARKER, line));
            return tokens;
        }
        
        if (trimmedLine.startsWith("- ")) {
            tokens.add(new Token(TokenType.LIST_ITEM, line));
            return tokens;
        }
        
        int colonIndex = line.indexOf(':');
        if (colonIndex == -1) {
            tokens.add(new Token(TokenType.TEXT, line));
            return tokens;
        }
        
        String beforeColon = line.substring(0, colonIndex);
        String afterColon = line.substring(colonIndex + 1);
        
        String indentation = extractIndentation(beforeColon);
        if (!indentation.isEmpty()) {
            tokens.add(new Token(TokenType.INDENTATION, indentation));
        }
        
        String keyPart = beforeColon.trim();
        if (!keyPart.isEmpty()) {
            tokens.add(new Token(TokenType.KEY, keyPart));
        }
        
        tokens.add(new Token(TokenType.COLON, ":"));
        
        if (!afterColon.isEmpty()) {
            String value = afterColon.trim();
            TokenType valueType = determineValueType(value);
            tokens.add(new Token(valueType, afterColon));
        }
        
        return tokens;
    }
    
    private String extractIndentation(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != ' ' && text.charAt(i) != '\t') {
                return text.substring(0, i);
            }
        }
        return text;
    }
    
    private TokenType determineValueType(String value) {
        if (value.startsWith("\"") || value.startsWith("'")) {
            return TokenType.STRING_VALUE;
        }
        
        if (value.startsWith("- ")) {
            return TokenType.LIST_ITEM;
        }
        
        if (value.equals("true") || value.equals("false") || value.equals("yes") || value.equals("no")) {
            return TokenType.BOOLEAN_VALUE;
        }
        
        if (value.equals("null") || value.equals("~")) {
            return TokenType.NULL_VALUE;
        }
        
        if (value.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) {
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
            case KEY:
                return 0xFF5CD0F3;
            case COLON:
                return 0xFFAAAAAA;
            case STRING_VALUE:
                return 0xFFE6DB74;
            case NUMBER_VALUE:
                return 0xFFAE81FF;
            case BOOLEAN_VALUE:
                return 0xFF569CD6;
            case NULL_VALUE:
                return 0xFFF92672;
            case LIST_ITEM:
                return 0xFFA6E22E;
            case INDENTATION:
                return 0xFF555555;
            case DOCUMENT_MARKER:
                return 0xFFFFD700;
            case VALUE:
                return 0xFFE6DB74;
            case TEXT:
            default:
                return defaultColor;
        }
    }
}
