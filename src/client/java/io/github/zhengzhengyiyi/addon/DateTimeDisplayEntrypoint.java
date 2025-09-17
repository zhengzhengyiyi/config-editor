package io.github.zhengzhengyiyi.addon;

import io.github.zhengzhengyiyi.gui.EditorScreen;
import io.github.zhengzhengyiyi.api.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.ActionResult;
import net.minecraft.client.MinecraftClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeDisplayEntrypoint implements ApiEntrypoint {
    private boolean enabled = true;
    private EditorScreen editor;

    @Override
    public void init() {}

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
    public void onMouseScroll() {}

    @Override
    public ActionResult onType(int keyCode, int scanCode, int modifiers) {
        return ActionResult.PASS;
    }

    @Override
    public ActionResult onCharTyped(char chr, int modifiers) {
        return ActionResult.PASS;
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!enabled || editor == null) return;

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd_HH:mm:ss");
        String timeText = now.format(formatter);

        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(timeText);
        int xPos = editor.width - textWidth - 10;
        int yPos = 10;

        context.drawText(MinecraftClient.getInstance().textRenderer, 
                       timeText, 
                       xPos,
                       yPos, 
                       0xFFFFFF00,
                       false);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
