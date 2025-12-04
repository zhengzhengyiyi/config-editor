package io.github.zhengzhengyiyi.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.github.zhengzhengyiyi.gui.widget.JsonFieldWidget;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonVisualEditorScreen extends Screen {
    public final String originalJson;
    private JsonObject jsonObject;
    private final List<JsonFieldWidget> fieldWidgets = new ArrayList<>();
    private final Map<String, Object> fieldValues = new LinkedHashMap<>();
    private int scrollOffset = 0;
    private static final int FIELD_HEIGHT = 30;
    public static final int MARGIN = 10;
    private static final int MAX_VISIBLE_FIELDS = 12;
    
    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;
    private ButtonWidget addFieldButton;
    private ButtonWidget rawEditButton;
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    
    public JsonVisualEditorScreen(String jsonContent) {
        super(Text.translatable("configeditor.visual.title"));
        this.originalJson = jsonContent;
        
        try {
            this.jsonObject = JsonParser.parseString(jsonContent).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                fieldValues.put(entry.getKey(), jsonElementToValue(entry.getValue()));
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            this.jsonObject = new JsonObject();
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
            button -> addNewField()
        )
        .dimensions(centerX - buttonWidth / 2, height - 60, buttonWidth, buttonHeight)
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
        addDrawableChild(rawEditButton);
        addDrawableChild(scrollUpButton);
        addDrawableChild(scrollDownButton);
        
        rebuildFieldWidgets();
        updateScrollButtons();
    }
    
    private void rebuildFieldWidgets() {
        for (JsonFieldWidget widget : fieldWidgets) {
            remove(widget);
        }
        fieldWidgets.clear();
        
        int yPos = 50;
        int centerX = width / 2;
        int maxFieldsToShow = Math.min(MAX_VISIBLE_FIELDS, fieldValues.size() - scrollOffset);
        
        List<Map.Entry<String, Object>> entries = new ArrayList<>(fieldValues.entrySet());
        
        for (int i = scrollOffset; i < scrollOffset + maxFieldsToShow && i < entries.size(); i++) {
            Map.Entry<String, Object> entry = entries.get(i);
//            final int fieldIndex = i;
            
            JsonFieldWidget fieldWidget = new JsonFieldWidget(
                entry.getKey(),
                entry.getValue(),
                centerX - 150,
                yPos,
                300,
                FIELD_HEIGHT,
                Text.literal(""),
                widget -> {
                    String oldKey = entry.getKey();
                    String newKey = widget.getKey();
                    Object newValue = widget.getValue();
                    
                    if (!oldKey.equals(newKey)) {
                        fieldValues.remove(oldKey);
                    }
                    
                    fieldValues.put(newKey, newValue);
                    
                    rebuildFieldWidgets();
                }
            );
            
            fieldWidgets.add(fieldWidget);
            addDrawableChild(fieldWidget);
            yPos += FIELD_HEIGHT + 5;
        }
    }
    
    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            rebuildFieldWidgets();
            updateScrollButtons();
        }
    }
    
    private void scrollDown() {
        if (scrollOffset < fieldValues.size() - MAX_VISIBLE_FIELDS) {
            scrollOffset++;
            rebuildFieldWidgets();
            updateScrollButtons();
        }
    }
    
    private void updateScrollButtons() {
        scrollUpButton.active = scrollOffset > 0;
        scrollDownButton.active = scrollOffset < Math.max(0, fieldValues.size() - MAX_VISIBLE_FIELDS);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
//        renderBackground(context);
        
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
        
        super.render(context, mouseX, mouseY, delta);
        
        for (JsonFieldWidget widget : fieldWidgets) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                widget.renderTooltip(context, mouseX, mouseY);
            }
        }
        
        if (fieldWidgets.isEmpty()) {
            context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("configeditor.visual.empty"),
                centerX,
                height / 2,
                0x888888
            );
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
        JsonObject updatedJson = new JsonObject();
        boolean hasErrors = false;
        
        for (JsonFieldWidget widget : fieldWidgets) {
            String key = widget.getKey();
            Object value = widget.getValue();
            
            if (key != null && !key.trim().isEmpty()) {
                try {
                    if (value instanceof String strValue) {
                        updatedJson.addProperty(key, strValue);
                    } else if (value instanceof Boolean boolValue) {
                        updatedJson.addProperty(key, boolValue);
                    } else if (value instanceof Integer intValue) {
                        updatedJson.addProperty(key, intValue);
                    } else if (value instanceof Double doubleValue) {
                        updatedJson.addProperty(key, doubleValue);
                    } else if (value == null) {
                        updatedJson.add(key, null);
                    } else {
                        updatedJson.addProperty(key, value.toString());
                    }
                } catch (Exception e) {
                    hasErrors = true;
                    widget.setError(true);
                }
            }
        }
        
        if (!hasErrors) {
            String newJson = updatedJson.toString();
            
            if (client != null && client.currentScreen instanceof EditorScreen) {
                EditorScreen editorScreen = (EditorScreen) client.currentScreen;
                editorScreen.setEditorText(newJson);
                editorScreen.showMessagePopup(Text.translatable("configeditor.message.saved"));
                close();
            }
        } else {
            showErrorPopup(Text.translatable("configeditor.error.invalidjson"));
        }
    }
    
    private void addNewField() {
        String baseKey = Text.translatable("configeditor.visual.newfield").getString();
        String newKey = baseKey + "_" + (fieldValues.size() + 1);
        fieldValues.put(newKey, "");
        
        if (scrollOffset < fieldValues.size() - MAX_VISIBLE_FIELDS) {
            scrollOffset = Math.max(0, fieldValues.size() - MAX_VISIBLE_FIELDS);
        }
        
        rebuildFieldWidgets();
        updateScrollButtons();
    }
    
    private Object jsonElementToValue(JsonElement element) {
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
        } else if (element.isJsonArray() || element.isJsonObject()) {
            return element.toString();
        }
        return "";
    }
    
    private void returnToRawEditor() {
        if (client != null) {
//            close();
            client.setScreen(new EditorScreen());
        }
    }
    
    private void showErrorPopup(Text message) {
        if (client != null) {
            client.setScreen(new MessageScreen(message));
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
}
