package io.github.zhengzhengyiyi.util.highlighter;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.*;

public class JsonSyntaxHighlighter implements HighLighter{
    public enum TokenType {
        BRACE_LEFT, BRACE_RIGHT,
        BRACKET_LEFT, BRACKET_RIGHT,
        COLON, COMMA,
        KEY,
        STRING_VALUE,
        NUMBER_VALUE,
        BOOLEAN_VALUE,
        NULL_VALUE,
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
        
        StringBuilder currentToken = new StringBuilder();
        boolean inString = false;
        boolean isKey = false;
        boolean expectValue = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (!inString) {
                    inString = true;
                    tokens.add(new Token(TokenType.TEXT, "\""));
                } else {
                    inString = false;
                    if (currentToken.length() > 0) {
                        TokenType type = isKey ? TokenType.KEY : (expectValue ? TokenType.STRING_VALUE : TokenType.TEXT);
                        tokens.add(new Token(type, currentToken.toString()));
                        currentToken.setLength(0);
                        if (type == TokenType.KEY) expectValue = true;
                    }
                    tokens.add(new Token(TokenType.TEXT, "\""));
                }
            } else if (inString) {
                currentToken.append(c);
            } else {
                switch (c) {
                    case '{':
                        tokens.add(new Token(TokenType.BRACE_LEFT, "{"));
                        isKey = true;
                        expectValue = false;
                        break;
                    case '}':
                        tokens.add(new Token(TokenType.BRACE_RIGHT, "}"));
                        isKey = false;
                        expectValue = false;
                        break;
                    case '[':
                        tokens.add(new Token(TokenType.BRACKET_LEFT, "["));
                        isKey = false;
                        expectValue = false;
                        break;
                    case ']':
                        tokens.add(new Token(TokenType.BRACKET_RIGHT, "]"));
                        isKey = false;
                        expectValue = false;
                        break;
                    case ':':
                        tokens.add(new Token(TokenType.COLON, ":"));
                        isKey = false;
                        expectValue = true;
                        break;
                    case ',':
                        tokens.add(new Token(TokenType.COMMA, ","));
                        isKey = true;
                        expectValue = false;
                        break;
                    case '=': case ';': case '+': case '-': case '*': case '/': case '%': case '&': case '|': case '!': case '?': case '<': case '>': case '~': case '^': case '@': case '#': case '$': case '(': case ')':
                        tokens.add(new Token(TokenType.SYMBOL, String.valueOf(c)));
                        break;
                    case ' ': case '\t':
                        tokens.add(new Token(TokenType.TEXT, String.valueOf(c)));
                        break;
                    default:
                        if (Character.isDigit(c) || c == '-' || c == '.') {
                            currentToken.setLength(0);
                            currentToken.append(c);
                            while (i + 1 < line.length()) {
                                char nextChar = line.charAt(i + 1);
                                if (Character.isDigit(nextChar) || nextChar == '.' || nextChar == 'e' || nextChar == 'E' || nextChar == '+' || nextChar == '-') {
                                    currentToken.append(nextChar);
                                    i++;
                                } else {
                                    break;
                                }
                            }
                            tokens.add(new Token(TokenType.NUMBER_VALUE, currentToken.toString()));
                            currentToken.setLength(0);
                        } else if (Character.isLetter(c) || c == '_') {
                            currentToken.setLength(0);
                            currentToken.append(c);
                            while (i + 1 < line.length() && (Character.isLetterOrDigit(line.charAt(i + 1)) || line.charAt(i + 1) == '_')) {
                                currentToken.append(line.charAt(++i));
                            }
                            String word = currentToken.toString();
                            TokenType type = TokenType.TEXT;
                            if ("true".equals(word) || "false".equals(word)) {
                                type = TokenType.BOOLEAN_VALUE;
                            } else if ("null".equals(word)) {
                                type = TokenType.NULL_VALUE;
                            }
                            tokens.add(new Token(type, word));
                            currentToken.setLength(0);
                        } else {
                            tokens.add(new Token(TokenType.TEXT, String.valueOf(c)));
                        }
                        break;
                }
            }
        }
        
        if (inString && currentToken.length() > 0) {
            tokens.add(new Token(TokenType.STRING_VALUE, currentToken.toString()));
        }
        
        return tokens;
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
            case BRACE_LEFT: case BRACE_RIGHT:
            case BRACKET_LEFT: case BRACKET_RIGHT:
                return 0xFFFFFF00;
            case COLON: case COMMA:
                return 0xFFAAAAAA;
            case SYMBOL:
                return 0xFFFF00FF;
            case KEY:
                return 0xFF5CD0F3;
            case STRING_VALUE:
                return 0xFFE6DB74;
            case NUMBER_VALUE:
                return 0xFFAE81FF;
            case BOOLEAN_VALUE:
                return 0xFF569CD6;
            case NULL_VALUE:
                return 0xFFF92672;
            case TEXT:
            default:
                return defaultColor;
        }
    }
}
