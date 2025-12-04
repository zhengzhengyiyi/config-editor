package io.github.zhengzhengyiyi.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import java.util.function.Consumer;

public class JsonFieldWidget extends ClickableWidget {
    private TextFieldWidget keyField;
    private TextFieldWidget valueField;
    private ButtonWidget deleteButton;
    private ButtonWidget typeButton;
    private String currentType;
    private boolean hasError = false;
    private final Consumer<JsonFieldWidget> onChange;
    private String originalKey;
    
    public JsonFieldWidget(String key, Object value, int x, int y, int width, int height, 
                          Text message, Consumer<JsonFieldWidget> onChange) {
        super(x, y, width, height, message);
        this.onChange = onChange;
        this.originalKey = key;
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        
        int fieldHeight = 20;
        
        keyField = new TextFieldWidget(
            textRenderer,
            x + 5,
            y + 5,
            width / 2 - 40,
            fieldHeight,
            Text.translatable("configeditor.visual.key")
        );
        keyField.setText(key == null ? "" : key);
        keyField.setMaxLength(100);
        keyField.setChangedListener(text -> notifyChange());
        
        determineType(value);
        
        valueField = new TextFieldWidget(
            textRenderer,
            x + width / 2 + 5,
            y + 5,
            width / 2 - 70,
            fieldHeight,
            Text.translatable("configeditor.visual.value")
        );
        valueField.setText(valueToString(value));
        valueField.setMaxLength(500);
        valueField.setChangedListener(text -> {
            validateValue();
            notifyChange();
        });
        
        typeButton = ButtonWidget.builder(
            Text.literal(getTypeSymbol()),
            button -> toggleType()
        )
        .dimensions(x + width - 100, y + 5, 30, fieldHeight)
        .build();
        
        deleteButton = ButtonWidget.builder(
            Text.literal("×"),
            button -> {
                if (this.onChange != null) {
                    this.onChange.accept(this);
                }
            }
        )
        .dimensions(x + width - 65, y + 5, 20, fieldHeight)
        .build();
    }
    
    private String valueToString(Object value) {
        if (value == null) return "null";
        return String.valueOf(value);
    }
    
    private void determineType(Object value) {
        if (value == null) {
            currentType = "null";
        } else if (value instanceof Boolean) {
            currentType = "boolean";
        } else if (value instanceof Number) {
            currentType = "number";
        } else {
            currentType = "string";
        }
    }
    
    private String getTypeSymbol() {
        return switch (currentType) {
            case "string" -> "\"\"";
            case "number" -> "123";
            case "boolean" -> "T/F";
            case "null" -> "∅";
            default -> "?";
        };
    }
    
    private void toggleType() {
        currentType = switch (currentType) {
            case "string" -> "number";
            case "number" -> "boolean";
            case "boolean" -> "null";
            case "null" -> "string";
            default -> "string";
        };
        typeButton.setMessage(Text.literal(getTypeSymbol()));
        
        if (currentType.equals("boolean")) {
            valueField.setText("true");
        } else if (currentType.equals("null")) {
            valueField.setText("null");
        } else if (currentType.equals("number")) {
            valueField.setText("0");
        }
        
        validateValue();
        notifyChange();
    }
    
    private void validateValue() {
        String text = valueField.getText();
        hasError = false;
        
        try {
            switch (currentType) {
                case "number":
                    Double.parseDouble(text);
                    break;
                case "boolean":
                    if (!text.equals("true") && !text.equals("false")) {
                        hasError = true;
                    }
                    break;
                case "null":
                    if (!text.equals("null")) {
                        hasError = true;
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            hasError = true;
        }
    }
    
    private void notifyChange() {
        if (onChange != null) {
            onChange.accept(this);
        }
    }
    
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int bgColor = hasError ? 0x30FF5555 : 0x20FFFFFF;
        
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        
        context.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
        
        keyField.render(context, mouseX, mouseY, delta);
        
        context.drawTextWithShadow(
            textRenderer,
            ":",
            getX() + width / 2 - 10,
            getY() + 10,
            0xFFFFFF
        );
        
        valueField.render(context, mouseX, mouseY, delta);
        typeButton.render(context, mouseX, mouseY, delta);
        deleteButton.render(context, mouseX, mouseY, delta);
        
        String typeLabel = getTypeLabel();
        context.drawTextWithShadow(
            textRenderer,
            typeLabel,
            getX() + width - 40,
            getY() + 10,
            getTypeColor()
        );
    }
    
    private String getTypeLabel() {
        return switch (currentType) {
            case "string" -> Text.translatable("configeditor.visual.type.string").getString();
            case "number" -> Text.translatable("configeditor.visual.type.number").getString();
            case "boolean" -> Text.translatable("configeditor.visual.type.boolean").getString();
            case "null" -> Text.translatable("configeditor.visual.type.null").getString();
            default -> Text.translatable("configeditor.visual.type.unknown").getString();
        };
    }
    
    private int getTypeColor() {
        return switch (currentType) {
            case "string" -> 0x55FF55;
            case "number" -> 0x5555FF;
            case "boolean" -> 0xFFAA00;
            case "null" -> 0xFF5555;
            default -> 0xAAAAAA;
        };
    }
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (keyField.mouseClicked(click, doubled)) return true;
        if (valueField.mouseClicked(click, doubled)) return true;
        if (typeButton.mouseClicked(click, doubled)) return true;
        if (deleteButton.mouseClicked(click, doubled)) return true;
        return super.mouseClicked(click, doubled);
    }
    
    @Override
    public boolean keyPressed(KeyInput input) {
        if (keyField.isFocused()) return keyField.keyPressed(input);
        if (valueField.isFocused()) return valueField.keyPressed(input);
        return false;
    }
    
    @Override
    public boolean charTyped(CharInput input) {
        if (keyField.isFocused()) return keyField.charTyped(input);
        if (valueField.isFocused()) return valueField.charTyped(input);
        return false;
    }
    
    public String getKey() {
        return keyField.getText();
    }
    
    public String getOriginalKey() {
        return originalKey;
    }
    
    public Object getValue() {
        String text = valueField.getText();
        
        try {
            return switch (currentType) {
                case "number" -> text.contains(".") ? Double.parseDouble(text) : Integer.parseInt(text);
                case "boolean" -> Boolean.parseBoolean(text);
                case "null" -> null;
                default -> text;
            };
        } catch (NumberFormatException e) {
            return text;
        }
    }
    
    public void setError(boolean error) {
        this.hasError = error;
    }
    
    public void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (typeButton.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.translatable("configeditor.visual.tooltip.type", getTypeLabel()), mouseX, mouseY);
        } else if (deleteButton.isMouseOver(mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.translatable("configeditor.visual.tooltip.delete"), mouseX, mouseY);
        } else if (hasError) {
            context.drawTooltip(textRenderer, Text.translatable("configeditor.visual.tooltip.error"), mouseX, mouseY);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(NarrationPart.TITLE, Text.translatable("configeditor.title"));
    }
}
