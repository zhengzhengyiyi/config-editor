package io.github.zhengzhengyiyi.addon;

import io.github.zhengzhengyiyi.gui.EditorScreen;
import io.github.zhengzhengyiyi.api.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.InteractionResult;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
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
    public InteractionResult onMouseDown(int x, int y) {
        return InteractionResult.PASS;
    }

    @Override
    public void onMouseScroll() {}
    
    @Override
    public Identifier getIdentifier() {
        return Identifier.fromNamespaceAndPath("zhengzhengyiyi", "datetime_display");
    }

    @Override
    public void renderButton(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    	if (this.editor == null) return;
    	
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timeText = now.format(formatter);
        
        int textWidth = Minecraft.getInstance().font.width(timeText);
        xPos = editor.width / 2 + textWidth / 2;
        
        context.text(
            Minecraft.getInstance().font, 
            timeText,
            xPos,
            yPos,
            0xFFFFFF00,
            false
        );
    }
}
