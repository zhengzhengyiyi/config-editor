package io.github.zhengzhengyiyi.gui.widget;

import io.github.zhengzhengyiyi.ConfigEditorClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class UndoRedoEntrypoint extends ClickableWidget {
    private final TextRenderer textRenderer;
    private String text = "";
//    private int yOffset = 0;
    private int scrollOffset = 0;
    private boolean editable = true;
    private Consumer<String> changedListener;
    private int cursorPosition = 0;
    private long lastCursorBlinkTime = 0;
    private boolean cursorVisible = true;
    
    private int lastCursorX = 0;

    public UndoRedoEntrypoint(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.setFocused(false);
        System.out.println(lastCursorX);
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

        for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
            entrypoint.renderButton(context, mouseX, mouseY, delta);
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
            
            int lineHeight = this.textRenderer.fontHeight + 2;
            int clickedY = (int)mouseY - (this.getY() + 4);
            int lineIndex = MathHelper.clamp(clickedY / lineHeight + this.scrollOffset, 0, this.text.split("\n", -1).length - 1);
            
            String[] lines = this.text.split("\n", -1);
            String line = lines[lineIndex];
            
            int clickedX = (int)mouseX - (this.getX() + 4);
            
            int charIndex = 0;
            for (int i = 1; i <= line.length(); i++) {
                if (this.textRenderer.getWidth(line.substring(0, i)) > clickedX) {
                    charIndex = i - 1;
                    break;
                }
                charIndex = i;
            }
            
            int newPosition = 0;
            for (int i = 0; i < lineIndex; i++) {
                newPosition += lines[i].length() + 1;
            }
            newPosition += charIndex;
            
            this.cursorPosition = MathHelper.clamp(newPosition, 0, this.text.length());
//            this.lastCursorX = this.textRenderer.getWidth(line.substring(0, charIndex));
            
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
            	ActionResult result = entrypoint.onMouseDown((int)Math.round(mouseX), (int)Math.round(mouseY));
                if (result == ActionResult.FAIL) {
                    return true;
                }
            }
            
            return true;
        } else {
            this.setFocused(false);
            return false;
        }
    }

//    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!this.isMouseOver(mouseX, mouseY)) return false;
        
        int lineHeight = this.textRenderer.fontHeight + 2;
        int maxLines = this.text.split("\n", -1).length;
        int maxVisibleLines = this.height / lineHeight;
        
        int newScrollOffset = this.scrollOffset - (int)Math.signum(amount);
        this.scrollOffset = MathHelper.clamp(newScrollOffset, 0, Math.max(0, maxLines - maxVisibleLines));
        
        for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
            entrypoint.onMouseScroll();
        }
        
        return true;
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.mouseScrolled(mouseX, mouseY, verticalAmount);
    }

//    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused() || !this.editable) {
            return false;
        }
        
        for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
        	ActionResult result = entrypoint.onType(keyCode, scanCode, modifiers);
            if (result == ActionResult.FAIL) {
                return true;
            }
        }

        boolean controlDown = Screen.hasControlDown();
//        boolean shiftDown = Screen.hasShiftDown();

        if (controlDown) {
            if (keyCode == GLFW.GLFW_KEY_V) {
                pasteFromClipboard();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_C) {
                copyToClipboard();
                return true;
            }
        }
        
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.cursorPosition = MathHelper.clamp(this.cursorPosition - 1, 0, this.text.length());
            updateCursorX();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.cursorPosition = MathHelper.clamp(this.cursorPosition + 1, 0, this.text.length());
            updateCursorX();
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_HOME) {
            this.cursorPosition = getLineStart(this.cursorPosition);
            updateCursorX();
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_END) {
            this.cursorPosition = getLineEnd(this.cursorPosition);
            updateCursorX();
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_UP) {
            int lineStart = getLineStart(this.cursorPosition);
//            int lineEnd = getLineEnd(this.cursorPosition);
//            int currentLineLength = lineEnd - lineStart;
            int currentX = this.cursorPosition - lineStart;
            
            int prevLineEnd = lineStart > 0 ? getLineEnd(lineStart - 1) : -1;
            if (prevLineEnd != -1) {
                int prevLineStart = getLineStart(prevLineEnd);
                int prevLineLength = prevLineEnd - prevLineStart;
                int newX = Math.min(currentX, prevLineLength);
                this.cursorPosition = prevLineStart + newX;
            }
            updateCursorX();
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(keyCode, scanCode, modifiers);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            int lineEnd = getLineEnd(this.cursorPosition);
//            int currentLineLength = lineEnd - getLineStart(this.cursorPosition);
            int currentX = this.cursorPosition - getLineStart(this.cursorPosition);
            
            int nextLineStart = lineEnd < this.text.length() ? lineEnd + 1 : -1;
            if (nextLineStart != -1) {
                int nextLineEnd = getLineEnd(nextLineStart);
                int nextLineLength = nextLineEnd - nextLineStart;
                int newX = Math.min(currentX, nextLineLength);
                this.cursorPosition = nextLineStart + newX;
            }
            updateCursorX();
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(keyCode, scanCode, modifiers);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (this.cursorPosition > 0) {
                this.text = this.text.substring(0, this.cursorPosition - 1) + this.text.substring(this.cursorPosition);
                this.cursorPosition--;
                this.onTextChanged();
                updateCursorX();
            }
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(keyCode, scanCode, modifiers);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (this.cursorPosition < this.text.length()) {
                this.text = this.text.substring(0, this.cursorPosition) + this.text.substring(this.cursorPosition + 1);
                this.onTextChanged();
                updateCursorX();
            }
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(keyCode, scanCode, modifiers);
            }
            return true;
        }
        
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            this.text = this.text.substring(0, this.cursorPosition) + "\n" + this.text.substring(this.cursorPosition);
            this.cursorPosition++;
            this.onTextChanged();
            updateCursorX();
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(keyCode, scanCode, modifiers);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!this.isFocused() || !this.editable) {
            return false;
        }
        
        for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
        	ActionResult result = entrypoint.onCharTyped(chr, modifiers);
            if (result == ActionResult.FAIL) {
                return true;
            }
        }
        
        if (chr >= 32 && chr != 127) {
            this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
            this.cursorPosition++;
            this.onTextChanged();
            updateCursorX();
            for (io.github.zhengzhengyiyi.api.ApiEntrypoint entrypoint : ConfigEditorClient.ENTRYPOINTS) {
                entrypoint.onType(chr, 0, modifiers);
            }
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
        updateCursorX();
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
    
    private void updateCursorX() {
        int lineStart = getLineStart(this.cursorPosition);
        String currentLine = this.text.substring(lineStart, this.cursorPosition);
        this.lastCursorX = this.textRenderer.getWidth(currentLine);
    }

    private int getLineStart(int pos) {
        int start = this.text.lastIndexOf('\n', pos - 1) + 1;
        return start;
    }

    private int getLineEnd(int pos) {
        int end = this.text.indexOf('\n', pos);
        if (end == -1) {
            end = this.text.length();
        }
        return end;
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
            updateCursorX();
        }
    }
    
    public void insertTextAtCursor(String text) {
        this.text = this.text.substring(0, this.cursorPosition) + text + this.text.substring(this.cursorPosition);
        this.cursorPosition += text.length();
        this.onTextChanged();
        updateCursorX();
    }
    
    public int getCursorPosition() {
        return this.cursorPosition;
    }

    public void setCursorPosition(int position) {
        this.cursorPosition = MathHelper.clamp(position, 0, this.text.length());
        updateCursorX();
    }
}