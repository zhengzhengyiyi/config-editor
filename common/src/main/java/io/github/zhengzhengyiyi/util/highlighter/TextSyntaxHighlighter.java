package io.github.zhengzhengyiyi.util.highlighter;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

public class TextSyntaxHighlighter implements HighLighter{
    
    public enum TokenType {
        COMMENT,
        HEADER,
        NUMBER,
        URL,
        EMAIL,
        QUOTED_TEXT,
        BRACKET,
        SYMBOL,
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
        
        if (trimmedLine.startsWith("#") || trimmedLine.startsWith("//")) {
            tokens.add(new Token(TokenType.COMMENT, line));
            return tokens;
        }
        
        StringBuilder currentToken = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '"';
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"' || c == '\'') {
                if (!inQuotes) {
                    if (currentToken.length() > 0) {
                        tokens.add(createToken(currentToken.toString()));
                        currentToken.setLength(0);
                    }
                    inQuotes = true;
                    quoteChar = c;
                } else if (c == quoteChar) {
                    inQuotes = false;
                    if (currentToken.length() > 0) {
                        tokens.add(new Token(TokenType.QUOTED_TEXT, currentToken.toString()));
                        currentToken.setLength(0);
                    }
                    continue;
                }
            }
            
            if (inQuotes) {
                currentToken.append(c);
            } else {
                if (Character.isWhitespace(c)) {
                    if (currentToken.length() > 0) {
                        tokens.add(createToken(currentToken.toString()));
                        currentToken.setLength(0);
                    }
                    tokens.add(new Token(TokenType.TEXT, String.valueOf(c)));
                } else if (isBracket(c)) {
                    if (currentToken.length() > 0) {
                        tokens.add(createToken(currentToken.toString()));
                        currentToken.setLength(0);
                    }
                    tokens.add(new Token(TokenType.BRACKET, String.valueOf(c)));
                } else if (isSymbol(c)) {
                    if (currentToken.length() > 0) {
                        tokens.add(createToken(currentToken.toString()));
                        currentToken.setLength(0);
                    }
                    tokens.add(new Token(TokenType.SYMBOL, String.valueOf(c)));
                } else {
                    currentToken.append(c);
                }
            }
        }
        
        if (currentToken.length() > 0) {
            tokens.add(createToken(currentToken.toString()));
        }
        
        return tokens;
    }
    
    private Token createToken(String text) {
        if (text.startsWith("#") || text.startsWith("//")) {
            return new Token(TokenType.COMMENT, text);
        }
        
        if (isHeader(text)) {
            return new Token(TokenType.HEADER, text);
        }
        
        if (isNumber(text)) {
            return new Token(TokenType.NUMBER, text);
        }
        
        if (isURL(text)) {
            return new Token(TokenType.URL, text);
        }
        
        if (isEmail(text)) {
            return new Token(TokenType.EMAIL, text);
        }
        
        return new Token(TokenType.TEXT, text);
    }
    
    private boolean isHeader(String text) {
        return text.length() > 0 && Character.isUpperCase(text.charAt(0)) && 
               text.length() < 50 && text.chars().filter(Character::isUpperCase).count() > text.length() * 0.3;
    }
    
    private boolean isNumber(String text) {
        return text.matches("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?%?");
    }
    
    private boolean isURL(String text) {
        return text.matches("(https?|ftp)://[^\\s]+") || 
               text.matches("www\\.[^\\s]+\\.[^\\s]+") ||
               text.matches("[^\\s]+\\.[a-z]{2,}(/\\S*)?");
    }
    
    private boolean isEmail(String text) {
        return text.matches("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    }
    
    private boolean isBracket(char c) {
        return c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}';
    }
    
    private boolean isSymbol(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '=' || c == '<' || c == '>' || 
               c == '!' || c == '?' || c == ':' || c == ';' || c == '.' || c == ',' || c == '&' || 
               c == '|' || c == '^' || c == '~' || c == '@' || c == '#' || c == '$' || c == '%';
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
            case HEADER:
                return 0xFFFFD700;
            case NUMBER:
                return 0xFFAE81FF;
            case URL:
                return 0xFF66D9EF;
            case EMAIL:
                return 0xFFA6E22E;
            case QUOTED_TEXT:
                return 0xFFE6DB74;
            case BRACKET:
                return 0xFFF92672;
            case SYMBOL:
                return 0xFFFD971F;
            case TEXT:
            default:
                return defaultColor;
        }
    }
}
