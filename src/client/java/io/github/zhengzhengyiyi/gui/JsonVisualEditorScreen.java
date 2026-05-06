package io.github.zhengzhengyiyi.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonVisualEditorScreen extends Screen {
    public final String originalJson;
    private JsonObject jsonObject;
    private final List<FieldEntry> fieldEntries = new ArrayList<>();
    private final Map<String, Object> fieldValues = new LinkedHashMap<>();
    private int scrollOffset = 0;
    private static final int FIELD_HEIGHT = 30;
    public static final int MARGIN = 10;
    private static final int MAX_VISIBLE_FIELDS = 12;
    private static final int INDENT_WIDTH = 40;

    private Font textRenderer = Minecraft.getInstance().font;
    
    private Button saveButton;
    private Button cancelButton;
    private Button addFieldButton;
    private Button addObjectButton;
    private Button rawEditButton;
    private Button scrollUpButton;
    private Button scrollDownButton;
    
    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public JsonVisualEditorScreen(String jsonContent, String fileName) {
        super(Component.translatable("configeditor.visual.title"));
        this.originalJson = jsonContent;
        this.filePath = Path.of(FabricLoader.getInstance().getConfigDir().toString(), fileName);
        
        try {
            this.jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
            parseJsonObject(jsonObject, "", 0);
        } catch (JsonSyntaxException | IllegalStateException e) {
            this.jsonObject = new JsonObject();
        }
    }
    
    private void parseJsonObject(JsonObject obj, String prefix, int depth) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            
            if (entry.getValue().isJsonObject()) {
                fieldValues.put(fullKey, new NestedObject(entry.getValue().getAsJsonObject(), depth + 1));
                parseJsonObject(entry.getValue().getAsJsonObject(), fullKey, depth + 1);
            } else if (entry.getValue().isJsonArray()) {
                fieldValues.put(fullKey, entry.getValue().toString());
            } else if (entry.getValue().isJsonNull()) {
                fieldValues.put(fullKey, null);
            } else if (entry.getValue().getAsJsonPrimitive().isString()) {
                fieldValues.put(fullKey, entry.getValue().getAsString());
            } else if (entry.getValue().getAsJsonPrimitive().isBoolean()) {
                fieldValues.put(fullKey, entry.getValue().getAsBoolean());
            } else if (entry.getValue().getAsJsonPrimitive().isNumber()) {
                try {
                    fieldValues.put(fullKey, entry.getValue().getAsInt());
                } catch (Exception e) {
                    fieldValues.put(fullKey, entry.getValue().getAsDouble());
                }
            }
        }
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = width / 2;
        int buttonWidth = 100;
        int buttonHeight = 20;
        
        saveButton = Button.builder(
            Component.translatable("configeditor.button.save"),
            button -> saveChanges()
        )
        .bounds(centerX - 110, height - 30, buttonWidth, buttonHeight)
        .build();
        
        cancelButton = Button.builder(
            Component.translatable("configeditor.button.close"),
            button -> onClose()
        )
        .bounds(centerX + 10, height - 30, buttonWidth, buttonHeight)
        .build();
        
        addFieldButton = Button.builder(
            Component.translatable("configeditor.visual.addfield"),
            button -> addNewField("")
        )
        .bounds(centerX - buttonWidth - 5, height - 60, buttonWidth, buttonHeight)
        .build();
        
        addObjectButton = Button.builder(
            Component.translatable("configeditor.visual.addobject"),
            button -> addNewObject("")
        )
        .bounds(centerX + 5, height - 60, buttonWidth, buttonHeight)
        .build();
        
        rawEditButton = Button.builder(
            Component.translatable("configeditor.visual.rawedit"),
            button -> returnToRawEditor()
        )
        .bounds(width - 110, 10, 100, buttonHeight)
        .build();
        
        scrollUpButton = Button.builder(
            Component.literal("↑"),
            button -> scrollUp()
        )
        .bounds(width - 40, 50, 30, 20)
        .build();
        
        scrollDownButton = Button.builder(
            Component.literal("↓"),
            button -> scrollDown()
        )
        .bounds(width - 40, height - 120, 30, 20)
        .build();
        
        addRenderableWidget(saveButton);
        addRenderableWidget(cancelButton);
        addRenderableWidget(addFieldButton);
        addRenderableWidget(addObjectButton);
        addRenderableWidget(rawEditButton);
        addRenderableWidget(scrollUpButton);
        addRenderableWidget(scrollDownButton);
        
        rebuildFieldEntries();
        updateScrollButtons();
    }
    
    @SuppressWarnings("null")
    private void rebuildFieldEntries() {
        fieldEntries.clear();
        
        int yPos = 50;
        int centerX = width / 2;
        int maxFieldsToShow = Math.min(MAX_VISIBLE_FIELDS, fieldValues.size() - scrollOffset);
        
        List<Map.Entry<String, Object>> entries = new ArrayList<>(fieldValues.entrySet());
        
        for (int i = scrollOffset; i < scrollOffset + maxFieldsToShow && i < entries.size(); i++) {
            Map.Entry<String, Object> entry = entries.get(i);
            String fullKey = entry.getKey();
            Object value = entry.getValue();
            
            int depth = getDepth(fullKey);
            int indent = depth * INDENT_WIDTH;
            
            FieldEntry fieldEntry = new FieldEntry(this);
            fieldEntry.fullKey = fullKey;
            fieldEntry.displayKey = getLastPart(fullKey);
            fieldEntry.value = value;
            fieldEntry.depth = depth;
            fieldEntry.x = centerX - 170 + indent;
            fieldEntry.y = yPos;
            fieldEntry.width = 420 - indent;
            fieldEntry.height = FIELD_HEIGHT;
            
            Font textRenderer = Minecraft.getInstance().font;
            
            fieldEntry.keyField = new EditBox(
                textRenderer,
                fieldEntry.x + 5,
                fieldEntry.y + 5,
                fieldEntry.width / 2 - 40,
                20,
                Component.translatable("configeditor.visual.key")
            );
            fieldEntry.keyField.setValue(fieldEntry.displayKey);
            fieldEntry.keyField.setMaxLength(100);
            fieldEntry.keyField.setResponder(text -> {
                String oldFullKey = fieldEntry.fullKey;
                String newDisplayKey = text;
                String newFullKey = getParentPath(oldFullKey) + (getParentPath(oldFullKey).isEmpty() ? "" : ".") + newDisplayKey;
                
                if (!oldFullKey.equals(newFullKey)) {
                    Object val = fieldEntry.value;
                    fieldValues.remove(oldFullKey);
                    fieldValues.put(newFullKey, val);
                    fieldEntry.fullKey = newFullKey;
                    fieldEntry.displayKey = newDisplayKey;
                    rebuildFieldEntries();
                }
            });
            
            fieldEntry.valueField = new EditBox(
                textRenderer,
                fieldEntry.x + fieldEntry.width / 2 + 5,
                fieldEntry.y + 5,
                fieldEntry.width / 2 - 100,
                20,
                Component.translatable("configeditor.visual.value")
            );
            
            if (value instanceof NestedObject) {
                fieldEntry.valueField.setValue("{...}");
                fieldEntry.valueField.setEditable(false);
            } else {
                fieldEntry.valueField.setValue(value == null ? "null" : String.valueOf(value));
                fieldEntry.valueField.setEditable(true);
            }
            
            fieldEntry.valueField.setMaxLength(500);
            fieldEntry.valueField.setResponder(text -> {
                if (!(fieldEntry.value instanceof NestedObject)) {
                    fieldEntry.value = parseValue(text, fieldEntry.getCurrentType());
                    fieldValues.put(fieldEntry.fullKey, fieldEntry.value);
                }
            });
            
            fieldEntry.updateTypeButton();
            
            if (value instanceof NestedObject) {
                fieldEntry.expandButton = Button.builder(
                    Component.literal("▶"),
                    button -> toggleObjectExpansion(fieldEntry)
                )
                .bounds(fieldEntry.x - 20, fieldEntry.y + 5, 15, 20)
                .build();
                addRenderableWidget(fieldEntry.expandButton);
            }
            
            fieldEntry.deleteButton = Button.builder(
                Component.literal("×"),
                button -> {
                    fieldValues.remove(fieldEntry.fullKey);
                    removeChildEntries(fieldEntry.fullKey);
                    rebuildFieldEntries();
                }
            )
            .bounds(fieldEntry.x + fieldEntry.width - 65, fieldEntry.y + 5, 20, 20)
            .build();
            
            addRenderableWidget(fieldEntry.keyField);
            addRenderableWidget(fieldEntry.valueField);
            addRenderableWidget(fieldEntry.typeButton);
            addRenderableWidget(fieldEntry.deleteButton);
            
            fieldEntries.add(fieldEntry);
            yPos += FIELD_HEIGHT + 5;
        }
    }
    
    private int getDepth(String fullKey) {
        if (fullKey == null || fullKey.isEmpty()) return 0;
        int count = 0;
        for (char c : fullKey.toCharArray()) {
            if (c == '.') count++;
        }
        return count;
    }
    
    private String getLastPart(String fullKey) {
        if (fullKey == null || fullKey.isEmpty()) return "";
        int lastDot = fullKey.lastIndexOf('.');
        return lastDot == -1 ? fullKey : fullKey.substring(lastDot + 1);
    }
    
    private String getParentPath(String fullKey) {
        if (fullKey == null || fullKey.isEmpty()) return "";
        int lastDot = fullKey.lastIndexOf('.');
        return lastDot == -1 ? "" : fullKey.substring(0, lastDot);
    }
    
    private void removeChildEntries(String parentKey) {
        List<String> toRemove = new ArrayList<>();
        for (String key : fieldValues.keySet()) {
            if (key.startsWith(parentKey + ".")) {
                toRemove.add(key);
            }
        }
        for (String key : toRemove) {
            fieldValues.remove(key);
        }
    }
    
    private void toggleObjectExpansion(FieldEntry entry) {
        if (entry.value instanceof NestedObject nestedObj) {
            nestedObj.expanded = !nestedObj.expanded;
            entry.expandButton.setMessage(Component.literal(nestedObj.expanded ? "▼" : "▶"));
            rebuildFieldEntries();
        }
    }
    
    private Object parseValue(String text, String type) {
        try {
            return switch (type) {
                case "number" -> text.contains(".") ? Double.parseDouble(text) : Integer.parseInt(text);
                case "boolean" -> Boolean.parseBoolean(text);
                case "null" -> null;
                default -> text;
            };
        } catch (NumberFormatException e) {
            return text;
        }
    }
    
    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            rebuildFieldEntries();
            updateScrollButtons();
        }
    }
    
    private void scrollDown() {
        if (scrollOffset < fieldValues.size() - MAX_VISIBLE_FIELDS) {
            scrollOffset++;
            rebuildFieldEntries();
            updateScrollButtons();
        }
    }
    
    private void updateScrollButtons() {
        scrollUpButton.active = scrollOffset > 0;
        scrollDownButton.active = scrollOffset < Math.max(0, fieldValues.size() - MAX_VISIBLE_FIELDS);
    }
    
    @SuppressWarnings("null")
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Background
        context.fill(0, 0, this.width, this.height, 0xFF1E1E2E);
        
        // Header bar
        int centerX = width / 2;
        context.fill(0, 0, this.width, 35, 0xFF181825);
        context.fill(0, 34, this.width, 35, 0xFF89B4FA);
        
        context.centeredText(
            textRenderer,
            Component.literal("✦ JSON Visual Editor"),
            centerX,
            10,
            0xFFCDD6F4
        );
        
        String fieldCountText = Component.translatable(
            "configeditor.visual.fieldcount", 
            fieldValues.size()
        ).getString();
        
        context.text(
            textRenderer,
            "§7" + fieldCountText,
            centerX - 150,
            22,
            0xFF6C7086
        );
        
        // Scroll indicators
        if (scrollOffset > 0) {
            context.text(
                textRenderer,
                "§7... ↑ more above ...",
                centerX - 150,
                45,
                0xFF6C7086
            );
        }
        
        if (scrollOffset + MAX_VISIBLE_FIELDS < fieldValues.size()) {
            int bottomTextY = 50 + (MAX_VISIBLE_FIELDS * (FIELD_HEIGHT + 5));
            context.text(
                textRenderer,
                "§7... ↓ more below ...",
                centerX - 150,
                bottomTextY,
                0xFF6C7086
            );
        }
        
        // Field rows with alternating backgrounds
        int rowIndex = 0;
        for (FieldEntry entry : fieldEntries) {
            // Alternating row background
            int rowBg = (rowIndex % 2 == 0) ? 0x20313244 : 0x20181825;
            context.fill(entry.x - 5, entry.y, entry.x + entry.width + 5, entry.y + entry.height, rowBg);
            
            // Error highlight
            if (entry.hasError) {
                context.fill(entry.x - 5, entry.y, entry.x + entry.width + 5, entry.y + entry.height, 0x30F38BA8);
            }
            
            // Left border accent for depth
            if (entry.depth > 0) {
                int accentColor = 0xFF45475A;
                context.fill(entry.x - 5, entry.y, entry.x - 4, entry.y + entry.height, accentColor);
            }
            
            // Colon separator
            context.text(
                textRenderer,
                ":",
                entry.x + entry.width / 2 - 10,
                entry.y + 10,
                0xFF89B4FA
            );
            
            // Tree structure lines
            for (int i = 0; i < entry.depth; i++) {
                int lineX = entry.x - INDENT_WIDTH * (entry.depth - i) + 5;
                context.text(
                    textRenderer,
                    "│",
                    lineX,
                    entry.y + 10,
                    0xFF45475A
                );
            }
            
            if (entry.depth > 0) {
                context.text(
                    textRenderer,
                    "├─",
                    entry.x - 15,
                    entry.y + 10,
                    0xFF45475A
                );
            }
            
            // Type badge
            String typeLabel = entry.getTypeLabel();
            int typeColor = entry.getTypeColor();
            int badgeX = entry.x + entry.width - 40;
            int badgeY = entry.y + 8;
            // Badge background
            context.fill(badgeX - 2, badgeY - 1, badgeX + textRenderer.width(typeLabel) + 2, badgeY + 9, 0x40000000);
            context.text(
                textRenderer,
                typeLabel,
                badgeX,
                badgeY,
                typeColor
            );
            
            if (entry.keyField.isMouseOver(mouseX, mouseY) || entry.valueField.isMouseOver(mouseX, mouseY)) {
                renderFieldTooltip(context, mouseX, mouseY, entry);
            }
            
            rowIndex++;
        }
        
        super.extractRenderState(context, mouseX, mouseY, delta);
        
        if (fieldEntries.isEmpty()) {
            context.centeredText(
                textRenderer,
                Component.literal("§7No fields. Click 'Add Field' to start."),
                centerX,
                height / 2,
                0xFF6C7086
            );
        }
    }
    
    private void renderFieldTooltip(GuiGraphicsExtractor context, int mouseX, int mouseY, FieldEntry entry) {
        Font textRenderer = Minecraft.getInstance().font;
        if (entry.typeButton.isMouseOver(mouseX, mouseY)) {
            context.setTooltipForNextFrame(textRenderer, Component.translatable("configeditor.visual.tooltip.type", entry.getTypeLabel()), mouseX, mouseY);
        } else if (entry.deleteButton.isMouseOver(mouseX, mouseY)) {
            context.setTooltipForNextFrame(textRenderer, Component.translatable("configeditor.visual.tooltip.delete"), mouseX, mouseY);
        } else if (entry.hasError) {
            context.setTooltipForNextFrame(textRenderer, Component.translatable("configeditor.visual.tooltip.error"), mouseX, mouseY);
        } else if (entry.value instanceof NestedObject && entry.expandButton != null && entry.expandButton.isMouseOver(mouseX, mouseY)) {
            context.setTooltipForNextFrame(textRenderer, Component.translatable("configeditor.visual.tooltip.expand"), mouseX, mouseY);
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            scrollUp();
            return true;
        } else if (verticalAmount < 0) {
            scrollDown();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    
    private void saveChanges() {
        JsonObject updatedJson = buildJsonObject();
        
        String finalJson = gson.toJson(updatedJson);
        
        new Thread(() -> {
            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, finalJson, StandardCharsets.UTF_8);
                
                Minecraft.getInstance().execute(() -> {
                    if (minecraft != null) {
                        minecraft.setScreen(null);
                    }
                });
                
            } catch (Exception e) {
                Minecraft.getInstance().execute(() -> {
                    if (minecraft != null) {
                        minecraft.setScreen(null);
                    }
                });
            }
        }).start();
    }
    
    private JsonObject buildJsonObject() {
        JsonObject root = new JsonObject();
        
        for (FieldEntry entry : fieldEntries) {
            String[] parts = entry.fullKey.split("\\.");
            JsonObject current = root;
            
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (!current.has(part)) {
                    current.add(part, new JsonObject());
                }
                current = current.getAsJsonObject(part);
            }
            
            String lastPart = parts[parts.length - 1];
            Object value = entry.value;
            
            if (value instanceof String strValue) {
                current.addProperty(lastPart, strValue);
            } else if (value instanceof Boolean boolValue) {
                current.addProperty(lastPart, boolValue);
            } else if (value instanceof Integer intValue) {
                current.addProperty(lastPart, intValue);
            } else if (value instanceof Double doubleValue) {
                current.addProperty(lastPart, doubleValue);
            } else if (value == null) {
                current.add(lastPart, null);
            } else if (value instanceof NestedObject) {
                current.add(lastPart, new JsonObject());
            } else {
                current.addProperty(lastPart, value.toString());
            }
        }
        
        return root;
    }
    
    private void addNewField(String parentPath) {
        String baseKey = Component.translatable("configeditor.visual.newfield").getString();
        String newKey = parentPath.isEmpty() ? baseKey + "_" + (fieldValues.size() + 1) : 
            parentPath + "." + baseKey + "_" + (fieldValues.size() + 1);
        fieldValues.put(newKey, "");
        
        if (scrollOffset < fieldValues.size() - MAX_VISIBLE_FIELDS) {
            scrollOffset = Math.max(0, fieldValues.size() - MAX_VISIBLE_FIELDS);
        }
        
        rebuildFieldEntries();
        updateScrollButtons();
    }
    
    private void addNewObject(String parentPath) {
        String baseKey = Component.translatable("configeditor.visual.newobject").getString();
        String newKey = parentPath.isEmpty() ? baseKey + "_" + (fieldValues.size() + 1) : 
            parentPath + "." + baseKey + "_" + (fieldValues.size() + 1);
        fieldValues.put(newKey, new NestedObject(new JsonObject(), getDepth(newKey)));
        
        if (scrollOffset < fieldValues.size() - MAX_VISIBLE_FIELDS) {
            scrollOffset = Math.max(0, fieldValues.size() - MAX_VISIBLE_FIELDS);
        }
        
        rebuildFieldEntries();
        updateScrollButtons();
    }
    
    public Object jsonElementToValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isString()) {
                return element.getAsString();
            } else if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else if (element.getAsJsonPrimitive().isNumber()) {
                try {
                    return element.getAsInt();
                } catch (Exception e) {
                    return element.getAsDouble();
                }
            }
        } else if (element.isJsonNull()) {
            return null;
        }
        return "";
    }
    
    private void returnToRawEditor() {
        if (minecraft != null) {
            minecraft.setScreen(new EditorScreen());
        }
    }
    
    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(new EditorScreen());
        }
        super.onClose();
    }
    
    @Override
    public boolean keyPressed(@SuppressWarnings("null") KeyEvent input) {
        if (input.key() == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }
    
    private static class FieldEntry {
        String fullKey;
        String displayKey;
        Object value;
        int x;
        int y;
        int width;
        int height;
        int depth;
        EditBox keyField;
        EditBox valueField;
        Button typeButton;
        Button deleteButton;
        Button expandButton;
        boolean hasError = false;
        JsonVisualEditorScreen screen;
        
        String getCurrentType() {
            if (value == null) return "null";
            if (value instanceof Boolean) return "boolean";
            if (value instanceof Number) return "number";
            if (value instanceof NestedObject) return "object";
            return "string";
        }
        
        String getTypeSymbol() {
            return switch (getCurrentType()) {
                case "string" -> "\"\"";
                case "number" -> "123";
                case "boolean" -> "T/F";
                case "null" -> "∅";
                case "object" -> "{}";
                default -> "?";
            };
        }
        
        String getTypeLabel() {
            return switch (getCurrentType()) {
                case "string" -> Component.translatable("configeditor.visual.type.string").getString();
                case "number" -> Component.translatable("configeditor.visual.type.number").getString();
                case "boolean" -> Component.translatable("configeditor.visual.type.boolean").getString();
                case "null" -> Component.translatable("configeditor.visual.type.null").getString();
                case "object" -> Component.translatable("configeditor.visual.type.object").getString();
                default -> Component.translatable("configeditor.visual.type.unknown").getString();
            };
        }
        
        int getTypeColor() {
            return switch (getCurrentType()) {
                case "string" -> 0x55FF55;
                case "number" -> 0x5555FF;
                case "boolean" -> 0xFFAA00;
                case "null" -> 0xFF5555;
                case "object" -> 0xAA55FF;
                default -> 0xAAAAAA;
            };
        }
        
        @SuppressWarnings("null")
        void updateTypeButton() {
            typeButton = Button.builder(
                Component.literal(getTypeSymbol()),
                button -> toggleType()
            )
            .bounds(x + width - 100, y + 5, 30, 20)
            .build();
        }
        
        @SuppressWarnings("null")
        private void toggleType() {
            String oldType = getCurrentType();
            String newType = switch (oldType) {
                case "string" -> "number";
                case "number" -> "boolean";
                case "boolean" -> "null";
                case "null" -> "object";
                case "object" -> "string";
                default -> "string";
            };
            
            value = parseValueForType(newType);
            if (value instanceof NestedObject) {
                valueField.setValue("{...}");
                valueField.setEditable(false);
            } else {
                valueField.setValue(value == null ? "null" : String.valueOf(value));
                valueField.setEditable(true);
            }
            updateTypeButton();
            screen.rebuildFieldEntries();
        }
        
        public FieldEntry(JsonVisualEditorScreen screen) {
        	this.screen = screen;
		}
        
        private Object parseValueForType(String type) {
            return switch (type) {
                case "boolean" -> true;
                case "number" -> 0;
                case "null" -> null;
                case "object" -> new NestedObject(new JsonObject(), depth + 1);
                default -> "";
            };
        }
    }
    
    private static class NestedObject {
        @SuppressWarnings("unused")
		public JsonObject jsonObject;
        @SuppressWarnings("unused")
        public int depth;
        boolean expanded;
        
        NestedObject(JsonObject jsonObject, int depth) {
            this.jsonObject = jsonObject;
            this.depth = depth;
            this.expanded = true;
        }
        
        @Override
        public String toString() {
            return "{...}";
        }
    }
}
