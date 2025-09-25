package io.github.zhengzhengyiyi.util;

import com.google.gson.JsonParser;
import java.util.*;
import java.util.regex.*;

public class JSONValidator {
    public static List<JSONError> validateJSON(String text) {
        List<JSONError> errors = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return errors;
        }
        
        try {
            JsonParser.parseString(text);
        } catch (Exception e) {
            parseException(e, text, errors);
        }
        
        checkBasicErrors(text, errors);
        return errors;
    }
    
    private static void parseException(Exception e, String text, List<JSONError> errors) {
        String message = e.getMessage();
        Pattern pattern = Pattern.compile("at line (\\d+) column (\\d+)");
        Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            int line = Integer.parseInt(matcher.group(1));
            int column = Integer.parseInt(matcher.group(2));
            int position = findPosition(text, line, column);
            
            int startPos = findErrorStart(text, position);
            int endPos = findErrorEnd(text, position);
            
            String errorMsg = simplifyMessage(message);
            errors.add(new JSONError(line, column, errorMsg, startPos, endPos));
        } else {
            checkManual(text, errors);
        }
    }
    
    private static int findPosition(String text, int line, int column) {
        int currentLine = 1;
        int currentColumn = 1;
        
        for (int i = 0; i < text.length(); i++) {
            if (currentLine == line && currentColumn == column) {
                return i;
            }
            
            if (text.charAt(i) == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
        }
        
        return Math.min(text.length() - 1, 0);
    }
    
    private static int findErrorStart(String text, int position) {
        int start = position;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (c == '\n' || c == '{' || c == '[' || c == ',' || c == ':') {
                break;
            }
            start--;
        }
        return Math.max(start, 0);
    }
    
    private static int findErrorEnd(String text, int position) {
        int end = position;
        while (end < text.length() - 1) {
            char c = text.charAt(end + 1);
            if (c == '\n' || c == '}' || c == ']' || c == ',' || c == ':') {
                break;
            }
            end++;
        }
        return Math.min(end, text.length() - 1);
    }
    
    private static String simplifyMessage(String message) {
        if (message.contains("Expected")) {
            return "Missing symbol or value";
        } else if (message.contains("Unterminated")) {
            return "String not closed";
        } else if (message.contains("Malformed")) {
            return "Format error";
        } else if (message.contains("Expected value")) {
            return "Missing value";
        } else {
            return "Syntax error";
        }
    }
    
    private static void checkManual(String text, List<JSONError> errors) {
        checkQuotes(text, errors);
        checkBrackets(text, errors);
        checkCommas(text, errors);
    }
    
    private static void checkQuotes(String text, List<JSONError> errors) {
        boolean inString = false;
        int quoteStart = -1;
        boolean escaped = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                if (!inString) {
                    inString = true;
                    quoteStart = i;
                } else {
                    inString = false;
                    quoteStart = -1;
                }
            }
            
            if (escaped) {
                escaped = false;
            }
        }
        
        if (inString && quoteStart != -1) {
            int line = countLines(text, 0, quoteStart) + 1;
            int column = getColumn(text, quoteStart);
            errors.add(new JSONError(line, column, "String not closed", quoteStart, text.length() - 1));
        }
    }
    
    private static void checkBrackets(String text, List<JSONError> errors) {
        Stack<Character> stack = new Stack<>();
        Stack<Integer> positions = new Stack<>();
        Map<Character, Character> pairs = new HashMap<>();
        pairs.put('}', '{');
        pairs.put(']', '[');
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '"') {
                i = skipString(text, i);
                continue;
            }
            
            if (c == '{' || c == '[') {
                stack.push(c);
                positions.push(i);
            } else if (c == '}' || c == ']') {
                if (stack.isEmpty()) {
                    addBracketError(text, i, "Extra bracket", errors);
                } else if (stack.peek() != pairs.get(c)) {
                    addBracketError(text, i, "Bracket mismatch", errors);
                } else {
                    stack.pop();
                    positions.pop();
                }
            }
        }
        
        while (!stack.isEmpty()) {
            int pos = positions.pop();
            char bracket = stack.pop();
            String name = (bracket == '{') ? "Curly brace" : "Square bracket";
            addBracketError(text, pos, name + " not closed", errors);
        }
    }
    
    private static int skipString(String text, int start) {
        boolean escaped = false;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (c == '\\' && !escaped) {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                return i;
            }
            
            if (escaped) {
                escaped = false;
            }
        }
        return text.length() - 1;
    }
    
    private static void addBracketError(String text, int position, String message, List<JSONError> errors) {
        int line = countLines(text, 0, position) + 1;
        int column = getColumn(text, position);
        errors.add(new JSONError(line, column, message, position, position));
    }
    
    private static void checkCommas(String text, List<JSONError> errors) {
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.endsWith(",") && i == lines.length - 1) {
                int lineStart = getLineStart(text, i);
                int commaPos = lineStart + lines[i].length() - 1;
                errors.add(new JSONError(i + 1, lines[i].length(), "Extra comma", commaPos, commaPos));
            }
        }
    }
    
    private static void checkBasicErrors(String text, List<JSONError> errors) {
        if (text.trim().startsWith(",")) {
            errors.add(new JSONError(1, 1, "Cannot start with comma", 0, 0));
        }
    }
    
    private static int countLines(String text, int start, int end) {
        int lines = 0;
        for (int i = start; i < end && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }
    
    private static int getColumn(String text, int position) {
        int column = 1;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                column = 1;
            } else {
                column++;
            }
        }
        return column;
    }
    
    private static int getLineStart(String text, int lineIndex) {
        int currentLine = 0;
        for (int i = 0; i < text.length(); i++) {
            if (currentLine == lineIndex) {
                return i;
            }
            if (text.charAt(i) == '\n') {
                currentLine++;
            }
        }
        return 0;
    }
}
