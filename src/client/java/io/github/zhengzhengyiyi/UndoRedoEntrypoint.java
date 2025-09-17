package io.github.zhengzhengyiyi;

import io.github.zhengzhengyiyi.gui.EditorScreen;
import io.github.zhengzhengyiyi.api.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import java.util.Stack;

public class UndoRedoEntrypoint implements ApiEntrypoint {
    private boolean enabled = true;
    private EditorScreen editor;
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private String lastText = "";

    @Override
    public void init() {
    }

    @Override
    public void onEditerOpen(EditorScreen editor) {
        this.editor = editor;
        if (editor.getTextWidget() != null) {
            lastText = editor.getTextWidget().getText();
            undoStack.push(lastText);
        }
    }

    @Override
    public void onEditerClose(EditorScreen editor) {
        this.editor = null;
        undoStack.clear();
        redoStack.clear();
    }

    @Override
    public ActionResult onMouseDown(int x, int y) {
    	return ActionResult.SUCCESS;
    }

    @Override
    public void onMouseScroll() {
    }

    @Override
    public ActionResult onType(int keyCode, int scanCode, int modifiers) {
        if (!enabled || editor == null || editor.getTextWidget() == null) return ActionResult.PASS;

        String currentText = editor.getTextWidget().getText();
        
        if (!currentText.equals(lastText)) {
            undoStack.push(lastText);
            redoStack.clear();
            lastText = currentText;
        }

        if (keyCode == GLFW.GLFW_KEY_Z && Screen.hasControlDown()) {
            if (Screen.hasShiftDown()) {
                performRedo();
            } else {
                performUndo();
            }
            return ActionResult.FAIL;
        }
        
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onCharTyped(char chr, int modifiers) {
        if (!enabled || editor == null || editor.getTextWidget() == null) return ActionResult.PASS;

        String currentText = editor.getTextWidget().getText();
        
        if (!currentText.equals(lastText)) {
            undoStack.push(lastText);
            redoStack.clear();
            lastText = currentText;
        }
        
        return ActionResult.PASS;
    }

    @Override
    public void renderButton(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void performUndo() {
        if (undoStack.size() > 1) {
            redoStack.push(undoStack.pop());
            String previousText = undoStack.peek();
            editor.getTextWidget().setText(previousText);
            lastText = previousText;
        }
    }

    private void performRedo() {
        if (!redoStack.isEmpty()) {
            String nextText = redoStack.pop();
            undoStack.push(nextText);
            editor.getTextWidget().setText(nextText);
            lastText = nextText;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}