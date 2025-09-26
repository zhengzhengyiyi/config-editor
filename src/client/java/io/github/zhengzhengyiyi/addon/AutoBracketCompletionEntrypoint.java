package io.github.zhengzhengyiyi.addon;

import io.github.zhengzhengyiyi.gui.EditorScreen;
import io.github.zhengzhengyiyi.api.*;
import io.github.zhengzhengyiyi.config.*;
import net.minecraft.util.ActionResult;
import java.util.HashMap;
import java.util.Map;

/**
 * This class will handle if use type bracket or other bracket like charactors.
 * {@code
 * bracketPairs.put('(', ')');
 *      bracketPairs.put('[', ']');
 *      bracketPairs.put('{', '}');
 *      bracketPairs.put('"', '"');
 *      bracketPairs.put('\'', '\'');
 *      bracketPairs.put('`', '`');
 * }
 */
public class AutoBracketCompletionEntrypoint implements ApiEntrypoint {
    private boolean enabled = true;
    private EditorScreen editor;
    private final Map<Character, Character> bracketPairs = new HashMap<>();

    public AutoBracketCompletionEntrypoint() {
        initializeBracketPairs();
    }

    /**
     * 
     * nothing needs to init, leave as empty
     */
    @Override
    public void init() {
    }

    @Override
    public void onEditerOpen(EditorScreen editor) {
        this.editor = editor;
    }

    @Override
    public void onEditerClose(EditorScreen editor) {
        this.editor = null;
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
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onCharTyped(char chr, int modifiers) {
        if (!enabled || editor == null || editor.getTextWidget() == null) {
            return ActionResult.PASS;
        }
        
        if (!ConfigManager.getConfig().hint) return ActionResult.SUCCESS;

        if (bracketPairs.containsKey(chr)) {
            handleBracketCompletion(chr);
            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }

    @Override
    public void renderButton(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void initializeBracketPairs() {
        bracketPairs.put('(', ')');
        bracketPairs.put('[', ']');
        bracketPairs.put('{', '}');
        bracketPairs.put('"', '"');
        bracketPairs.put('\'', '\'');
        bracketPairs.put('`', '`');
    }

    private void handleBracketCompletion(char openingChar) {
	    char closingChar = bracketPairs.get(openingChar);
	    String pair = String.valueOf(openingChar) + closingChar;
	        
	    editor.getTextWidget().insertTextAtCursor(pair);
	        
	    if (openingChar != closingChar) {
	        int currentPos = editor.getTextWidget().getCursorPosition();
	        editor.getTextWidget().setCursorPosition(currentPos - 1);
	   }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggleEnabled() {
        this.enabled = !this.enabled;
    }
}