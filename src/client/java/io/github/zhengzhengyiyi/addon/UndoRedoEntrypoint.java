package io.github.zhengzhengyiyi.addon;

import io.github.zhengzhengyiyi.gui.EditorScreen;
import io.github.zhengzhengyiyi.api.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayDeque;
import java.util.Deque;

public class UndoRedoEntrypoint implements ApiEntrypoint {
    private boolean enabled = true;
    private EditorScreen editor;
    private Deque<String> undoStack = new ArrayDeque<>();
    private Deque<String> redoStack = new ArrayDeque<>();
    private String currentText = "";
    private boolean isUndoing = false;
    private boolean isRedoing = false;
    private static final int MAX_HISTORY = 100;

    @Override
    public void init() {
    }

    @Override
    public void onEditerOpen(EditorScreen editor) {
        this.editor = editor;
        resetStacks();
        if (editor.getTextWidget() != null) {
            currentText = editor.getTextWidget().getText();
            saveState();
        }
    }

    @Override
    public void onEditerClose(EditorScreen editor) {
        this.editor = null;
        resetStacks();
    }

    @Override
    public ActionResult onMouseDown(int x, int y) {
        return ActionResult.PASS;
    }

    @Override
    public void onMouseScroll() {
    }

    @Override
    public ActionResult onType(int keyCode, int scanCode, int modifiers) {
        if (!enabled || editor == null || editor.getTextWidget() == null) return ActionResult.PASS;

        if (keyCode == GLFW.GLFW_KEY_Z && Screen.hasControlDown()) {
            if (Screen.hasShiftDown()) {
                performRedo();
            } else {
                performUndo();
            }
            return ActionResult.FAIL;
        }
        
        if (keyCode == GLFW.GLFW_KEY_Y && Screen.hasControlDown()) {
            performRedo();
            return ActionResult.FAIL;
        }

        checkTextChange();
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onCharTyped(char chr, int modifiers) {
        if (!enabled || editor == null || editor.getTextWidget() == null) return ActionResult.PASS;
        
        checkTextChange();
        return ActionResult.PASS;
    }

    @Override
    public void renderButton(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void checkTextChange() {
        if (isUndoing || isRedoing) return;
        
        String newText = editor.getTextWidget().getText();
        if (!newText.equals(currentText)) {
            saveState();
            currentText = newText;
        }
    }

    private void saveState() {
        if (isUndoing || isRedoing) return;
        
        undoStack.push(currentText);
        if (undoStack.size() > MAX_HISTORY) {
            Deque<String> newStack = new ArrayDeque<>();
            int count = 0;
            for (String state : undoStack) {
                if (count++ < MAX_HISTORY) newStack.add(state);
            }
            undoStack = newStack;
        }
        redoStack.clear();
    }

    private void performUndo() {
        if (undoStack.size() <= 1 || isUndoing) return;
        
        isUndoing = true;
        redoStack.push(currentText);
        undoStack.pop();
        String previousText = undoStack.peek();
        editor.getTextWidget().setText(previousText);
        currentText = previousText;
        isUndoing = false;
    }

    private void performRedo() {
        if (redoStack.isEmpty() || isRedoing) return;
        
        isRedoing = true;
        String nextText = redoStack.pop();
        undoStack.push(nextText);
        editor.getTextWidget().setText(nextText);
        currentText = nextText;
        isRedoing = false;
    }

    private void resetStacks() {
        undoStack.clear();
        redoStack.clear();
        currentText = "";
    }

    public void clearHistory() {
        resetStacks();
    }

    public boolean canUndo() {
        return undoStack.size() > 1;
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) resetStacks();
    }
}
