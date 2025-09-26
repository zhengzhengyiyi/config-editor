package io.github.zhengzhengyiyi.util;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.*;
import java.util.regex.Pattern;

public class CodeSuggester {
    public static final Set<String> JSON_KEYWORDS = Set.of(
        "true", "false", "null"
    );
    
    private static final Set<String> COMMON_KEYS = Set.of(
        "type", "name", "id", "count", "amount", "value", "data", "config",
        "enabled", "disabled", "width", "height", "x", "y", "z", "position",
        "color", "size", "speed", "duration", "delay", "interval", "random",
        "min", "max", "average", "total", "sum", "length", "weight", "price",
        "quality", "level", "tier", "rarity", "category", "group", "class",
        "version", "author", "description", "title", "label", "text", "message",
        "content", "items", "list", "array", "object", "properties", "settings",
        "options", "parameters", "args", "arguments", "input", "output", "result",
        "effect", "action", "event", "trigger", "condition", "requirement",
        "target", "source", "destination", "from", "to", "start", "end"
    );
    
    private static final Map<String, Set<String>> KEY_VALUES = Map.of(
        "type", Set.of("object", "array", "string", "number", "boolean", "integer"),
        "format", Set.of("json", "yaml", "xml", "csv", "text"),
        "mode", Set.of("read", "write", "append", "create", "delete", "update"),
        "status", Set.of("success", "error", "warning", "info", "pending", "completed"),
        "color", Set.of("red", "green", "blue", "yellow", "black", "white", "gray"),
        "direction", Set.of("up", "down", "left", "right", "forward", "backward")
    );
    
    public static final Pattern STRING_PATTERN = Pattern.compile("\"[^\"]*\"");
    public static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    public static final Pattern KEY_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:");

    public static List<String> suggestForPosition(String jsonText, int cursorPosition) {
        List<String> suggestions = new ArrayList<>();
        
        if (jsonText == null || jsonText.isEmpty()) {
            suggestions.add("{");
            suggestions.add("[");
            return suggestions;
        }
        
        String textBeforeCursor = jsonText.substring(0, cursorPosition);
        String textAfterCursor = jsonText.substring(cursorPosition);
        
        if (isInKeyContext(textBeforeCursor, textAfterCursor)) {
            suggestions.addAll(suggestKeys(textBeforeCursor));
        } else if (isInValueContext(textBeforeCursor, textAfterCursor)) {
            String currentKey = extractCurrentKey(textBeforeCursor);
            suggestions.addAll(suggestValues(currentKey, textBeforeCursor));
        } else if (isInStringContext(textBeforeCursor, textAfterCursor)) {
            suggestions.addAll(suggestStringContent(textBeforeCursor));
        }
        
        if (suggestions.isEmpty()) {
            suggestions.addAll(suggestStructural(jsonText, cursorPosition));
        }
        
        return filterDuplicates(suggestions);
    }
    
    private static boolean isInKeyContext(String before, String after) {
        int lastOpenBrace = before.lastIndexOf('{');
        int lastOpenBracket = before.lastIndexOf('[');
        int lastComma = before.lastIndexOf(',');
        int lastColon = before.lastIndexOf(':');
        
        return (lastOpenBrace > lastColon && lastOpenBrace > lastComma) ||
               (lastOpenBracket > lastColon && lastComma > lastColon) ||
               (lastComma > lastColon && before.trim().endsWith(","));
    }
    
    private static boolean isInValueContext(String before, String after) {
        int lastColon = before.lastIndexOf(':');
        int lastOpenBrace = before.lastIndexOf('{');
        int lastOpenBracket = before.lastIndexOf('[');
        
        return lastColon > lastOpenBrace && lastColon > lastOpenBracket &&
               !after.trim().startsWith("}") && !after.trim().startsWith("]");
    }
    
    private static boolean isInStringContext(String before, String after) {
        int lastQuote = before.lastIndexOf('"');
        int secondLastQuote = before.lastIndexOf('"', lastQuote - 1);
        return lastQuote > secondLastQuote && lastQuote >= 0;
    }
    
    private static List<String> suggestKeys(String textBeforeCursor) {
        List<String> keys = new ArrayList<>();
        keys.addAll(COMMON_KEYS);
        keys.addAll(extractExistingKeys(textBeforeCursor));
        return keys.stream().map(key -> "\"" + key + "\": ").toList();
    }
    
    private static List<String> suggestValues(String currentKey, String textBeforeCursor) {
        List<String> values = new ArrayList<>();
        
        if (KEY_VALUES.containsKey(currentKey)) {
            values.addAll(KEY_VALUES.get(currentKey));
        }
        
        values.add("\"\"");
        values.add("0");
        values.add("1");
        values.add("true");
        values.add("false");
        values.add("null");
        values.add("[]");
        values.add("{}");
        
        if ("count".equals(currentKey) || "amount".equals(currentKey) || 
            "quantity".equals(currentKey)) {
            for (int i = 1; i <= 64; i *= 2) {
                values.add(String.valueOf(i));
            }
        }
        
        return values.stream().map(value -> {
            if (value.startsWith("\"") || value.equals("true") || 
                value.equals("false") || value.equals("null") ||
                value.equals("[]") || value.equals("{}")) {
                return value;
            }
            return "\"" + value + "\"";
        }).toList();
    }
    
    private static List<String> suggestStringContent(String textBeforeCursor) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("text");
        suggestions.add("value");
        suggestions.add("data");
        suggestions.add("content");
        return suggestions;
    }
    
    private static List<String> suggestStructural(String jsonText, int cursorPosition) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            JsonParser.parseString(jsonText);
            suggestions.add(",");
            suggestions.add("}");
            suggestions.add("]");
        } catch (JsonSyntaxException e) {
            String textBefore = jsonText.substring(0, cursorPosition);
//            String textAfter = jsonText.substring(cursorPosition);
            
            if (textBefore.trim().endsWith("{") || textBefore.trim().endsWith("[")) {
                suggestions.add("\"\": ");
            } else if (textBefore.trim().endsWith(":")) {
                suggestions.add("\"\"");
                suggestions.add("0");
                suggestions.add("true");
                suggestions.add("false");
            } else if (textBefore.trim().endsWith(",")) {
                suggestions.add("\"\": ");
            }
        }
        
        return suggestions;
    }
    
    private static String extractCurrentKey(String textBeforeCursor) {
        String[] lines = textBeforeCursor.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.contains(":")) {
                int colonIndex = line.indexOf(':');
                String keyPart = line.substring(0, colonIndex).trim();
                if (keyPart.startsWith("\"") && keyPart.endsWith("\"")) {
                    return keyPart.substring(1, keyPart.length() - 1);
                }
            }
        }
        return "";
    }
    
    private static Set<String> extractExistingKeys(String jsonText) {
        Set<String> keys = new HashSet<>();
        var matcher = KEY_PATTERN.matcher(jsonText);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }
    
    private static List<String> filterDuplicates(List<String> list) {
        return new ArrayList<>(new LinkedHashSet<>(list));
    }
    
    public static List<String> getCommonKeys() {
        return new ArrayList<>(COMMON_KEYS);
    }
    
    public static List<String> getValueSuggestions(String key) {
        return KEY_VALUES.getOrDefault(key, Collections.emptySet())
                .stream().toList();
    }
}
