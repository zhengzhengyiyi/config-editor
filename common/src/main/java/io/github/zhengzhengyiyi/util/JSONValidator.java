package io.github.zhengzhengyiyi.util;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
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
        checkStructuralErrors(text, errors);
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
            
            String errorMsg = getCleanErrorMessage(e);
            int startPos = findPreciseErrorStart(text, position, errorMsg);
            int endPos = findPreciseErrorEnd(text, position, errorMsg);
            
            int actualLine = calculateLineNumber(text, startPos) + 1;
            int actualColumn = calculateColumnNumber(text, startPos);
            
            errors.add(new JSONError(actualLine, actualColumn, errorMsg, startPos, endPos));
        } else {
            checkManualErrorDetection(text, errors);
        }
    }
    
    private static String getCleanErrorMessage(Exception e) {
        if (e instanceof JsonSyntaxException) {
            String msg = e.getMessage();
            if (msg.contains("Expected")) {
                if (msg.contains("BEGIN_OBJECT")) return "Expected '{'";
                if (msg.contains("END_OBJECT")) return "Expected '}'";
                if (msg.contains("BEGIN_ARRAY")) return "Expected '['";
                if (msg.contains("END_ARRAY")) return "Expected ']'";
                if (msg.contains("COLON")) return "Expected ':'";
                if (msg.contains("COMMA")) return "Expected ','";
                return "Expected value";
            } else if (msg.contains("Unterminated")) {
                return "Unterminated string";
            } else if (msg.contains("Malformed")) {
                return "Malformed JSON";
            }
            return "Syntax error";
        } else if (e instanceof MalformedJsonException) {
            return "Malformed JSON";
        }
        return "JSON error";
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
        
        return Math.max(0, Math.min(text.length() - 1, 0));
    }
    
    private static int calculateLineNumber(String text, int position) {
        int line = 0;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
    
    private static int calculateColumnNumber(String text, int position) {
        int column = 1;
//        int lastNewLine = -1;
        for (int i = 0; i < position && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
//                lastNewLine = i;
                column = 1;
            } else {
                column++;
            }
        }
        return column;
    }
    
    private static int findPreciseErrorStart(String text, int position, String errorMsg) {
        if (errorMsg.contains("Expected '{'") || errorMsg.contains("Expected '}'")) {
            return findBraceBoundary(text, position, true);
        } else if (errorMsg.contains("Expected '['") || errorMsg.contains("Expected ']'")) {
            return findBracketBoundary(text, position, true);
        } else if (errorMsg.contains("Expected ':'")) {
            return findColonBoundary(text, position, true);
        } else if (errorMsg.contains("Expected ','")) {
            return findCommaBoundary(text, position, true);
        } else if (errorMsg.contains("Unterminated string")) {
            return findStringStart(text, position);
        }
        
        return findTokenBoundary(text, position, true);
    }
    
    private static int findPreciseErrorEnd(String text, int position, String errorMsg) {
        if (errorMsg.contains("Expected '{'") || errorMsg.contains("Expected '}'")) {
            return findBraceBoundary(text, position, false);
        } else if (errorMsg.contains("Expected '['") || errorMsg.contains("Expected ']'")) {
            return findBracketBoundary(text, position, false);
        } else if (errorMsg.contains("Expected ':'")) {
            return findColonBoundary(text, position, false);
        } else if (errorMsg.contains("Expected ','")) {
            return findCommaBoundary(text, position, false);
        } else if (errorMsg.contains("Unterminated string")) {
            return findStringEnd(text, position);
        }
        
        return findTokenBoundary(text, position, false);
    }
    
    private static int findBraceBoundary(String text, int position, boolean isStart) {
        if (isStart) {
            for (int i = position; i >= 0; i--) {
                if (i == 0 || text.charAt(i) == '{' || text.charAt(i) == '}' || 
                    text.charAt(i) == '[' || text.charAt(i) == ']' || 
                    text.charAt(i) == ',' || text.charAt(i) == '\n') {
                    return i;
                }
            }
        } else {
            for (int i = position; i < text.length(); i++) {
                if (i == text.length() - 1 || text.charAt(i) == '{' || text.charAt(i) == '}' || 
                    text.charAt(i) == '[' || text.charAt(i) == ']' || 
                    text.charAt(i) == ',' || text.charAt(i) == '\n') {
                    return i;
                }
            }
        }
        return position;
    }
    
    private static int findBracketBoundary(String text, int position, boolean isStart) {
        return findBraceBoundary(text, position, isStart);
    }
    
    private static int findColonBoundary(String text, int position, boolean isStart) {
        if (isStart) {
            for (int i = position; i >= 0; i--) {
                if (i == 0 || text.charAt(i) == ':' || text.charAt(i) == '"' || 
                    text.charAt(i) == ',' || text.charAt(i) == '\n') {
                    return i;
                }
            }
        } else {
            for (int i = position; i < text.length(); i++) {
                if (i == text.length() - 1 || text.charAt(i) == ':' || text.charAt(i) == '"' || 
                    text.charAt(i) == ',' || text.charAt(i) == '\n') {
                    return i;
                }
            }
        }
        return position;
    }
    
    private static int findCommaBoundary(String text, int position, boolean isStart) {
        return findColonBoundary(text, position, isStart);
    }
    
    private static int findStringStart(String text, int position) {
        for (int i = position; i >= 0; i--) {
            if (text.charAt(i) == '"') {
                return i;
            }
        }
        return position;
    }
    
    private static int findStringEnd(String text, int position) {
        for (int i = position; i < text.length(); i++) {
            if (text.charAt(i) == '"' && (i == 0 || text.charAt(i-1) != '\\')) {
                return i;
            }
        }
        return text.length() - 1;
    }
    
    private static int findTokenBoundary(String text, int position, boolean isStart) {
        if (isStart) {
            int start = position;
            while (start > 0) {
                char c = text.charAt(start - 1);
                if (c == '\n' || c == '{' || c == '[' || c == ',' || c == ':' || c == '}' || c == ']') {
                    break;
                }
                start--;
            }
            return Math.max(start, 0);
        } else {
            int end = position;
            while (end < text.length() - 1) {
                char c = text.charAt(end + 1);
                if (c == '\n' || c == '}' || c == ']' || c == ',' || c == ':' || c == '{' || c == '[') {
                    break;
                }
                end++;
            }
            return Math.min(end, text.length() - 1);
        }
    }
    
    private static void checkManualErrorDetection(String text, List<JSONError> errors) {
        checkQuotes(text, errors);
        checkBrackets(text, errors);
        checkCommas(text, errors);
    }
    
    private static void checkBasicErrors(String text, List<JSONError> errors) {
        String trimmed = text.trim();
        if (trimmed.startsWith(",")) {
            errors.add(new JSONError(1, 1, "Unexpected comma", 0, 0));
        }
        if (trimmed.endsWith(",")) {
            int line = calculateLineNumber(text, text.length() - 1) + 1;
            int column = calculateColumnNumber(text, text.length() - 1);
            errors.add(new JSONError(line, column, "Trailing comma", text.length() - 1, text.length() - 1));
        }
    }
    
    private static void checkStructuralErrors(String text, List<JSONError> errors) {
        Stack<Character> stack = new Stack<>();
        Stack<Integer> positions = new Stack<>();
        Map<Character, Character> pairs = new HashMap<>();
        pairs.put('}', '{');
        pairs.put(']', '[');
        
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (!inString) {
                if (c == '{' || c == '[') {
                    stack.push(c);
                    positions.push(i);
                } else if (c == '}' || c == ']') {
                    if (stack.isEmpty()) {
                        int line = calculateLineNumber(text, i) + 1;
                        int column = calculateColumnNumber(text, i);
                        errors.add(new JSONError(line, column, "Unexpected closing bracket", i, i));
                    } else if (stack.peek() != pairs.get(c)) {
                        int line = calculateLineNumber(text, i) + 1;
                        int column = calculateColumnNumber(text, i);
                        errors.add(new JSONError(line, column, "Bracket mismatch", i, i));
                    } else {
                        stack.pop();
                        positions.pop();
                    }
                }
            }
            
            if (c == '"' && !escaped) {
                inString = !inString;
            }
            escaped = (c == '\\' && !escaped);
        }
        
        while (!stack.isEmpty()) {
            int pos = positions.pop();
            char bracket = stack.pop();
            String msg = (bracket == '{') ? "Unclosed curly brace" : "Unclosed square bracket";
            int line = calculateLineNumber(text, pos) + 1;
            int column = calculateColumnNumber(text, pos);
            errors.add(new JSONError(line, column, msg, pos, pos));
        }
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
            int line = calculateLineNumber(text, quoteStart) + 1;
            int column = calculateColumnNumber(text, quoteStart);
            errors.add(new JSONError(line, column, "Unterminated string", quoteStart, text.length() - 1));
        }
    }
    
    private static void checkBrackets(String text, List<JSONError> errors) {
    }
    
    private static void checkCommas(String text, List<JSONError> errors) {
    }
    
//    private static int countLines(String text, int start, int end) {
//        int lines = 0;
//        for (int i = start; i < end && i < text.length(); i++) {
//            if (text.charAt(i) == '\n') {
//                lines++;
//            }
//        }
//        return lines;
//    }
}
