package io.github.zhengzhengyiyi.gui;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
    private static final Identifier WINDOW_TEXTURE = Identifier.of("minecraft", "gui/advancements/window.png");
//    private static final Identifier WIDGETS_TEXTURE = Identifier.of("minecraft", "textures/gui/advancements/widgets.png");
    
    public final String originalJson;
    private JsonObject jsonObject;
    private final List<FieldEntry> fieldEntries = new ArrayList<>();
    private final Map<String, Object> fieldValues = new LinkedHashMap<>();
    private int scrollOffset = 0;
    private static final int FIELD_HEIGHT = 35;
    private static final int MAX_VISIBLE_FIELDS = 8;
    private static final int INDENT_WIDTH = 25;
    
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private ButtonWidget addFieldButton;
    private ButtonWidget addObjectButton;
    private ButtonWidget rawEditButton;
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    
    private final Path filePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public JsonVisualEditorScreen(String jsonContent, String fileName) {
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
        @SuppressWarnings("unused")
        int buttonWidth = 100;
        
        saveButton = ButtonWidget.builder(Text.translatable("configeditor.button.save"), b -> saveChanges())
            .dimensions(centerX - 105, height - 35, 100, 20).build();
        cancelButton = ButtonWidget.builder(Text.translatable("configeditor.button.close"), b -> close())
            .dimensions(centerX + 5, height - 35, 100, 20).build();
        addFieldButton = ButtonWidget.builder(Text.translatable("configeditor.visual.addfield"), b -> addNewField(""))
            .dimensions(10, height - 35, 80, 20).build();
        addObjectButton = ButtonWidget.builder(Text.translatable("configeditor.visual.addobject"), b -> addNewObject(""))
            .dimensions(95, height - 35, 80, 20).build();
        rawEditButton = ButtonWidget.builder(Text.translatable("configeditor.visual.rawedit"), b -> returnToRawEditor())
            .dimensions(width - 110, 10, 100, 20).build();
        scrollUpButton = ButtonWidget.builder(Text.literal("▲"), b -> scrollUp())
            .dimensions(width - 30, 60, 20, 20).build();
        scrollDownButton = ButtonWidget.builder(Text.literal("▼"), b -> scrollDown())
            .dimensions(width - 30, height - 70, 20, 20).build();
            
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
//        this.clearAndInit();
        int yPos = 55;
        int centerX = width / 2;
        List<Map.Entry<String, Object>> entries = new ArrayList<>(fieldValues.entrySet());
        int maxFields = Math.min(MAX_VISIBLE_FIELDS, entries.size() - scrollOffset);

        for (int i = scrollOffset; i < scrollOffset + maxFields && i < entries.size(); i++) {
            Map.Entry<String, Object> entry = entries.get(i);
            FieldEntry fe = new FieldEntry(this);
            fe.fullKey = entry.getKey();
            fe.displayKey = getLastPart(fe.fullKey);
            fe.value = entry.getValue();
            fe.depth = getDepth(fe.fullKey);
            int indent = fe.depth * INDENT_WIDTH;
            fe.x = centerX - 180 + indent;
            fe.y = yPos;
            fe.width = 360 - indent;
            fe.height = 30;

            fe.keyField = new TextFieldWidget(textRenderer, fe.x + 10, fe.y + 7, 100, 16, Text.empty());
            fe.keyField.setText(fe.displayKey);
            fe.keyField.setDrawsBackground(false);
            
            fe.valueField = new TextFieldWidget(textRenderer, fe.x + 120, fe.y + 7, 130, 16, Text.empty());
            if (fe.value instanceof NestedObject) {
                fe.valueField.setText("...");
                fe.valueField.setEditable(false);
            } else {
                fe.valueField.setText(fe.value == null ? "null" : String.valueOf(fe.value));
            }
            fe.valueField.setDrawsBackground(false);

            fe.updateTypeButton();
            fe.deleteButton = ButtonWidget.builder(Text.literal("×"), b -> {
                fieldValues.remove(fe.fullKey);
                rebuildFieldEntries();
            }).dimensions(fe.x + fe.width - 25, fe.y + 5, 20, 20).build();

            addDrawableChild(fe.keyField);
            addDrawableChild(fe.valueField);
            addDrawableChild(fe.typeButton);
            addDrawableChild(fe.deleteButton);
            fieldEntries.add(fe);
            yPos += FIELD_HEIGHT;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
//        renderBackground(context, mouseX, mouseY, delta);
        int centerX = width / 2;
        
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, centerX - 200, 40, 400, height - 90);
        context.drawCenteredTextWithShadow(textRenderer, title, centerX, 15, 0xFFFFFF);

        for (FieldEntry entry : fieldEntries) {
            context.fill(entry.x, entry.y, entry.x + entry.width - 30, entry.y + entry.height - 2, 0x44000000);
            context.drawHorizontalLine(entry.x, entry.x + entry.width - 30, entry.y + entry.height - 2, 0xFFFFFFFF);
            
            if (entry.depth > 0) {
                int lineX = entry.x - 10;
                context.fill(lineX, entry.y - 5, lineX + 1, entry.y + 15, 0xFFAAAAAA);
                context.fill(lineX, entry.y + 15, entry.x + 5, entry.y + 16, 0xFFAAAAAA);
            }
            
            context.drawTextWithShadow(textRenderer, ":", entry.x + 112, entry.y + 10, 0xAAAAAA);
        }
        
        super.render(context, mouseX, mouseY, delta);
    }

    private void saveChanges() {
        JsonObject updatedJson = buildJsonObject();
        String finalJson = gson.toJson(updatedJson);
        new Thread(() -> {
            try {
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, finalJson, StandardCharsets.UTF_8);
                MinecraftClient.getInstance().execute(() -> client.setScreen(null));
            } catch (Exception ignored) {}
        }).start();
    }

    private JsonObject buildJsonObject() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            JsonObject current = root;
            for (int i = 0; i < parts.length - 1; i++) {
                if (!current.has(parts[i])) current.add(parts[i], new JsonObject());
                current = current.getAsJsonObject(parts[i]);
            }
            String last = parts[parts.length - 1];
            Object val = entry.getValue();
            if (val instanceof String s) current.addProperty(last, s);
            else if (val instanceof Boolean b) current.addProperty(last, b);
            else if (val instanceof Number n) current.addProperty(last, n);
            else if (val == null) current.add(last, null);
            else if (val instanceof NestedObject) current.add(last, new JsonObject());
        }
        return root;
    }

    private int getDepth(String key) { return (int) key.chars().filter(ch -> ch == '.').count(); }
    private String getLastPart(String key) { return key.contains(".") ? key.substring(key.lastIndexOf('.') + 1) : key; }
    private void scrollUp() { if (scrollOffset > 0) { scrollOffset--; rebuildFieldEntries(); updateScrollButtons(); } }
    private void scrollDown() { if (scrollOffset < fieldValues.size() - MAX_VISIBLE_FIELDS) { scrollOffset++; rebuildFieldEntries(); updateScrollButtons(); } }
    private void updateScrollButtons() { scrollUpButton.active = scrollOffset > 0; scrollDownButton.active = scrollOffset < fieldValues.size() - MAX_VISIBLE_FIELDS; }
    private void addNewField(String p) { fieldValues.put("new_field_" + fieldValues.size(), ""); rebuildFieldEntries(); }
    private void addNewObject(String p) { fieldValues.put("new_obj_" + fieldValues.size(), new NestedObject(new JsonObject(), 0)); rebuildFieldEntries(); }
    private void returnToRawEditor() { if (client != null) client.setScreen(new EditorScreen()); }
    @Override public void close() { if (client != null) client.setScreen(new EditorScreen()); super.close(); }

    private static class FieldEntry {
        String fullKey, displayKey;
        Object value;
        int x, y, width, height, depth;
        TextFieldWidget keyField, valueField;
        ButtonWidget typeButton, deleteButton;
        JsonVisualEditorScreen screen;
        public FieldEntry(JsonVisualEditorScreen s) { this.screen = s; }

        void updateTypeButton() {
            typeButton = ButtonWidget.builder(Text.literal(getTypeSymbol()), b -> toggleType())
                .dimensions(x + width - 60, y + 5, 30, 20).build();
        }

        private void toggleType() {
            if (value instanceof String) value = 0;
            else if (value instanceof Number) value = true;
            else if (value instanceof Boolean) value = null;
            else value = "";
            screen.fieldValues.put(fullKey, value);
            screen.rebuildFieldEntries();
        }

        String getTypeSymbol() {
            if (value instanceof String) return "Abc";
            if (value instanceof Number) return "#";
            if (value instanceof Boolean) return "√";
            if (value instanceof NestedObject) return "{}";
            return "∅";
        }
    }

    private static class NestedObject {
        @SuppressWarnings("unused")
		public JsonObject jsonObject;
        @SuppressWarnings("unused")
        public int depth;
        NestedObject(JsonObject j, int d) { this.jsonObject = j; this.depth = d; }
    }
}
