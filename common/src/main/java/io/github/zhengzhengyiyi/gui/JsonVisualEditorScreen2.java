package io.github.zhengzhengyiyi.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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

public class JsonVisualEditorScreen2 extends Screen {
    public final String originalJson;
    private JsonObject jsonObject;
    private final List<FieldEntry> fieldEntries = new ArrayList<>();
    private final Map<String, Object> fieldValues = new LinkedHashMap<>();
    private int scrollOffset = 0;
    private static final int FIELD_HEIGHT = 30;
    public static final int MARGIN = 10;
    private static final int MAX_VISIBLE_FIELDS = 12;
    private static final int INDENT_WIDTH = 40;
    
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private ButtonWidget addFieldButton;
    private ButtonWidget addObjectButton;
    private ButtonWidget rawEditButton;
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    
    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public JsonVisualEditorScreen2(String jsonContent, String fileName) {
        super(Text.translatable("configeditor.visual.title"));
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
        
        saveButton = ButtonWidget.builder(
            Text.translatable("configeditor.button.save"),
            button -> saveChanges()
        )
        .dimensions(centerX - 110, height - 30, buttonWidth, buttonHeight)
        .build();
        
        cancelButton = ButtonWidget.builder(
            Text.translatable("configeditor.button.close"),
            button -> close()
        )
        .dimensions(centerX + 10, height - 30, buttonWidth, buttonHeight)
        .build();
        
        addFieldButton = ButtonWidget.builder(
            Text.translatable("configeditor.visual.addfield"),
            button -> addNewField("")
        )
        .dimensions(centerX - buttonWidth - 5, height - 60, buttonWidth, buttonHeight)
        .build();
        
        addObjectButton = ButtonWidget.builder(
            Text.translatable("configeditor.visual.addobject"),
            button -> addNewObject("")
        )
        .dimensions(centerX + 5, height - 60, buttonWidth, buttonHeight)
        .build();
        
        rawEditButton = ButtonWidget.builder(
            Text.translatable("configeditor.visual.rawedit"),
            button -> returnToRawEditor()
        )
        .dimensions(width - 110, 10, 100, buttonHeight)
        .build();
        
        scrollUpButton = ButtonWidget.builder(
            Text.literal("↑"),
            button -> scrollUp()
        )
        .dimensions(width - 40, 50, 30, 20)
        .build();
        
        scrollDownButton = ButtonWidget.builder(
            Text.literal("↓"),
            button -> scrollDown()
        )
        .dimensions(width - 40, height - 120, 30, 20)
        .build();
        
        addDrawableChild(saveButton);
        addDrawableChild(cancelButton);
        addDrawableChild(addFieldButton);
        addDrawableChild(addObjectButton);
        addDrawableChild(rawEditButton);
        addDrawableChild(scrollUpButton);
        addDrawableChild(scrollDownButton);
        
        rebuildFieldEntries();
        updateScrollButtons();
    }
    
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
            
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            
            fieldEntry.keyField = new TextFieldWidget(
                textRenderer,
                fieldEntry.x + 5,
                fieldEntry.y + 5,
                fieldEntry.width / 2 - 40,
                20,
                Text.translatable("configeditor.visual.key")
            );
            fieldEntry.keyField.setText(fieldEntry.displayKey);
            fieldEntry.keyField.setMaxLength(100);
            fieldEntry.keyField.setChangedListener(text -> {
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
            
            fieldEntry.valueField = new TextFieldWidget(
                textRenderer,
                fieldEntry.x + fieldEntry.width / 2 + 5,
                fieldEntry.y + 5,
                fieldEntry.width / 2 - 100,
                20,
                Text.translatable("configeditor.visual.value")
            );
            
            if (value instanceof NestedObject) {
                fieldEntry.valueField.setText("{...}");
                fieldEntry.valueField.setEditable(false);
            } else {
                fieldEntry.valueField.setText(value == null ? "null" : String.valueOf(value));
                fieldEntry.valueField.setEditable(true);
            }
            
            fieldEntry.valueField.setMaxLength(500);
            fieldEntry.valueField.setChangedListener(text -> {
                if (!(fieldEntry.value instanceof NestedObject)) {
                    fieldEntry.value = parseValue(text, fieldEntry.getCurrentType());
                    fieldValues.put(fieldEntry.fullKey, fieldEntry.value);
                }
            });
            
            fieldEntry.updateTypeButton();
            
            if (value instanceof NestedObject) {
                fieldEntry.expandButton = ButtonWidget.builder(
                    Text.literal("▶"),
                    button -> toggleObjectExpansion(fieldEntry)
                )
                .dimensions(fieldEntry.x - 20, fieldEntry.y + 5, 15, 20)
                .build();
                addDrawableChild(fieldEntry.expandButton);
            }
            
            fieldEntry.deleteButton = ButtonWidget.builder(
                Text.literal("×"),
                button -> {
                    fieldValues.remove(fieldEntry.fullKey);
                    removeChildEntries(fieldEntry.fullKey);
                    rebuildFieldEntries();
                }
            )
            .dimensions(fieldEntry.x + fieldEntry.width - 65, fieldEntry.y + 5, 20, 20)
            .build();
            
            addDrawableChild(fieldEntry.keyField);
            addDrawableChild(fieldEntry.valueField);
            addDrawableChild(fieldEntry.typeButton);
            addDrawableChild(fieldEntry.deleteButton);
            
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
            entry.expandButton.setMessage(Text.literal(nestedObj.expanded ? "▼" : "▶"));
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
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.translatable("configeditor.visual.title"),
            centerX,
            20,
            0xFFFFFF
        );
        
        String fieldCountText = Text.translatable(
            "configeditor.visual.fieldcount", 
            fieldValues.size()
        ).getString();
        
        context.drawTextWithShadow(
            textRenderer,
            fieldCountText,
            centerX - 150,
            35,
            0xAAAAAA
        );
        
        if (scrollOffset > 0) {
            context.drawTextWithShadow(
                textRenderer,
                "... ↑ ...",
                centerX - 150,
                45,
                0x888888
            );
        }
        
        if (scrollOffset + MAX_VISIBLE_FIELDS < fieldValues.size()) {
            int bottomTextY = 50 + (MAX_VISIBLE_FIELDS * (FIELD_HEIGHT + 5));
            context.drawTextWithShadow(
                textRenderer,
                "... ↓ ...",
                centerX - 150,
                bottomTextY,
                0x888888
            );
        }
        
        for (FieldEntry entry : fieldEntries) {
            int bgColor = entry.hasError ? 0x30FF5555 : 0x20FFFFFF;
            context.fill(entry.x, entry.y, entry.x + entry.width, entry.y + entry.height, bgColor);
            
            context.drawTextWithShadow(
                textRenderer,
                ":",
                entry.x + entry.width / 2 - 10,
                entry.y + 10,
                0xFFFFFF
            );
            
            for (int i = 0; i < entry.depth; i++) {
                int lineX = entry.x - INDENT_WIDTH * (entry.depth - i) + 5;
                context.drawTextWithShadow(
                    textRenderer,
                    "│",
                    lineX,
                    entry.y + 10,
                    0x888888
                );
            }
            
            if (entry.depth > 0) {
                context.drawTextWithShadow(
                    textRenderer,
                    "├─",
                    entry.x - 15,
                    entry.y + 10,
                    0x888888
                );
            }
            
            String typeLabel = entry.getTypeLabel();
            int typeColor = entry.getTypeColor();
            context.drawTextWithShadow(
                textRenderer,
                typeLabel,
                entry.x + entry.width - 40,
                entry.y + 10,
                typeColor
            );
            
            if (entry.keyField.isMouseOver(mouseX, mouseY) || entry.valueField.isMouseOver(mouseX, mouseY)) {
                renderFieldTooltip(context, mouseX, mouseY, entry);
            }
        }
        
        super.render(context, mouseX, mouseY, delta);
        
        if (fieldEntries.isEmpty()) {
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("configeditor.visual.empty"),
                centerX,
                height / 2,
                0x888888
            );
        }
    }
    
    private void renderFieldTooltip(DrawContext context, int mouseX, int mouseY, FieldEntry entry) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (entry.typeButton.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.translatable("configeditor.visual.tooltip.type", entry.getTypeLabel()), mouseX, mouseY);
        } else if (entry.deleteButton.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.translatable("configeditor.visual.tooltip.delete"), mouseX, mouseY);
        } else if (entry.hasError) {
            context.drawTooltip(textRenderer, Text.translatable("configeditor.visual.tooltip.error"), mouseX, mouseY);
        } else if (entry.value instanceof NestedObject && entry.expandButton != null && entry.expandButton.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.translatable("configeditor.visual.tooltip.expand"), mouseX, mouseY);
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
                
                MinecraftClient.getInstance().execute(() -> {
                    if (client != null) {
                        client.setScreen(null);
                    }
                });
                
            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() -> {
                    if (client != null) {
                        client.setScreen(null);
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
        String baseKey = Text.translatable("configeditor.visual.newfield").getString();
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
        String baseKey = Text.translatable("configeditor.visual.newobject").getString();
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
        if (client != null) {
            client.setScreen(new EditorScreen());
        }
    }
    
    @Override
    public void close() {
        if (client != null) {
            client.setScreen(new EditorScreen());
        }
        super.close();
    }
    
    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.getKeycode() == 256) {
            close();
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
        TextFieldWidget keyField;
        TextFieldWidget valueField;
        ButtonWidget typeButton;
        ButtonWidget deleteButton;
        ButtonWidget expandButton;
        boolean hasError = false;
        JsonVisualEditorScreen2 screen;
        
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
                case "string" -> Text.translatable("configeditor.visual.type.string").getString();
                case "number" -> Text.translatable("configeditor.visual.type.number").getString();
                case "boolean" -> Text.translatable("configeditor.visual.type.boolean").getString();
                case "null" -> Text.translatable("configeditor.visual.type.null").getString();
                case "object" -> Text.translatable("configeditor.visual.type.object").getString();
                default -> Text.translatable("configeditor.visual.type.unknown").getString();
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
        
        void updateTypeButton() {
            typeButton = ButtonWidget.builder(
                Text.literal(getTypeSymbol()),
                button -> toggleType()
            )
            .dimensions(x + width - 100, y + 5, 30, 20)
            .build();
        }
        
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
                valueField.setText("{...}");
                valueField.setEditable(false);
            } else {
                valueField.setText(value == null ? "null" : String.valueOf(value));
                valueField.setEditable(true);
            }
            updateTypeButton();
            screen.rebuildFieldEntries();
        }
        
        public FieldEntry(JsonVisualEditorScreen2 screen) {
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
