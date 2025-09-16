package io.github.zhengzhengyiyi.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class MultilineEditorWidget extends ClickableWidget {
    private final TextRenderer textRenderer;
    private String text = "";
//    private int yOffset = 0;
    private int scrollOffset = 0;
    private boolean editable = true;
    private Consumer<String> changedListener;
    private int cursorPosition = 0;
    private long lastCursorBlinkTime = 0;
    private boolean cursorVisible = true;

    public MultilineEditorWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.setFocused(false);
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        context.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF000000);
        context.drawBorder(this.getX(), this.getY(), this.width, this.height, 0xFFFFFFFF);

        String[] lines = this.text.split("\n", -1);
        int lineHeight = this.textRenderer.fontHeight + 2;
        int maxVisibleLines = this.height / lineHeight;

        for (int i = 0; i < lines.length; i++) {
            if (i >= this.scrollOffset && i < this.scrollOffset + maxVisibleLines) {
                int yPos = this.getY() + 4 + (i - this.scrollOffset) * lineHeight;
                context.drawText(this.textRenderer, lines[i], this.getX() + 4, yPos, this.editable ? 0xFFFFFFFF : 0xFF777777, false);
            }
        }

        if (this.isFocused() && this.editable) {
            long currentTime = Util.getMeasuringTimeNano();
            if (currentTime - lastCursorBlinkTime > 500000000) {
                cursorVisible = !cursorVisible;
                lastCursorBlinkTime = currentTime;
            }
            if (cursorVisible) {
                int lineIndex = 0;
                int xPos = this.getX() + 4;
                int remaining = this.cursorPosition;
                for (int i = 0; i < lines.length; i++) {
                    if (remaining <= lines[i].length()) {
                        xPos += this.textRenderer.getWidth(lines[i].substring(0, remaining));
                        lineIndex = i;
                        break;
                    }
                    remaining -= (lines[i].length() + 1);
                }
                
                if (lineIndex >= this.scrollOffset && lineIndex < this.scrollOffset + maxVisibleLines) {
                    int yPos = this.getY() + 4 + (lineIndex - this.scrollOffset) * lineHeight;
                    context.drawVerticalLine(xPos, yPos - 1, yPos + this.textRenderer.fontHeight + 1, 0xFFFFFFFF);
                }
            }
        }

        if (lines.length > maxVisibleLines) {
            int scrollbarHeight = Math.max(20, (int)((float)this.height * (float)maxVisibleLines / (float)lines.length));
            int scrollbarY = this.getY() + (int)((float)(this.height - scrollbarHeight) * (float)this.scrollOffset / (float)(lines.length - maxVisibleLines));
            context.fill(this.getX() + this.width - 5, this.getY(), this.getX() + this.width, this.getY() + this.height, 0xFF555555);
            context.fill(this.getX() + this.width - 4, scrollbarY, this.getX() + this.width - 1, scrollbarY + scrollbarHeight, 0xFFBBBBBB);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY) && this.editable) {
            this.setFocused(true);
            return true;
        } else {
            this.setFocused(false);
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int lineHeight = this.textRenderer.fontHeight + 2;
        int maxLines = this.text.split("\n", -1).length;
        int maxVisibleLines = this.height / lineHeight;
        
        int newScrollOffset = this.scrollOffset - (int)Math.signum(verticalAmount);
        this.scrollOffset = MathHelper.clamp(newScrollOffset, 0, Math.max(0, maxLines - maxVisibleLines));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused() || !this.editable) {
            return false;
        }

        if (Screen.hasControlDown()) {
            if (keyCode == GLFW.GLFW_KEY_V) {
                pasteFromClipboard();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_C) {
                copyToClipboard();
                return true;
            }
        }
        
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.cursorPosition > 0) {
                this.text = this.text.substring(0, this.cursorPosition - 1) + this.text.substring(this.cursorPosition);
                this.cursorPosition--;
                this.onTextChanged();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (this.cursorPosition < this.text.length()) {
                this.text = this.text.substring(0, this.cursorPosition) + this.text.substring(this.cursorPosition + 1);
                this.onTextChanged();
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.text = this.text.substring(0, this.cursorPosition) + "\n" + this.text.substring(this.cursorPosition);
            this.cursorPosition++;
            this.onTextChanged();
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!this.isFocused() || !this.editable) {
            return false;
        }
        
        if (chr >= 32 && chr != 127) {
            this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
            this.cursorPosition++;
            this.onTextChanged();
            return true;
        }
        return false;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        this.appendDefaultNarrations(builder);
    }

    public String getText() {
        return this.text;
    }
    
    public void setText(String text) {
        this.text = text;
        this.cursorPosition = MathHelper.clamp(this.cursorPosition, 0, this.text.length());
        this.onTextChanged();
    }
    
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void setChangedListener(Consumer<String> changedListener) {
        this.changedListener = changedListener;
    }

    private void onTextChanged() {
        if (this.changedListener != null) {
            this.changedListener.accept(this.text);
        }
    }

    private void copyToClipboard() {
        MinecraftClient.getInstance().keyboard.setClipboard(this.text);
    }

    private void pasteFromClipboard() {
        String clipboardText = MinecraftClient.getInstance().keyboard.getClipboard();
        if (clipboardText != null) {
            this.text = this.text.substring(0, this.cursorPosition) + clipboardText + this.text.substring(this.cursorPosition);
            this.cursorPosition += clipboardText.length();
            this.onTextChanged();
        }
    }
}
