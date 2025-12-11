package io.github.zhengzhengyiyi.addon;

import io.github.zhengzhengyiyi.gui.EditorScreen;
import io.github.zhengzhengyiyi.api.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeDisplayEntrypoint implements ApiEntrypoint {
    private EditorScreen editor;
    private int xPos;
    private int yPos;

    @Override
    public void init() {}

    @Override
    public void onEditerOpen(EditorScreen editor) {
        this.editor = editor;
        
        yPos = 1;
    }

    @Override
    public void onEditerClose(EditorScreen editor) {
        this.editor = null;
    }

    @Override
    public ActionResult onMouseDown(int x, int y) {
        return ActionResult.PASS;
    }

    @Override
    public void onMouseScroll() {}
    
    @Override
    public Identifier getIdentifier() {
        return Identifier.of("zhengzhengyiyi", "datetime_display");
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    	if (this.editor == null) return;
    	
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timeText = now.format(formatter);
        
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(timeText);
        xPos = editor.width / 2 + textWidth / 2;
        
//        System.out.println("Rendering date/time at: " + xPos + ", " + yPos);
        
        context.drawText(
            MinecraftClient.getInstance().textRenderer, 
            timeText,
            xPos,
            yPos,
            0xFFFFFF00,
            false
        );
    }
}
