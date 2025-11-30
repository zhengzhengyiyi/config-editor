package io.github.zhengzhengyiyi.gui;

import io.github.zhengzhengyiyi.CommonEntryPoint;
import io.github.zhengzhengyiyi.api.ApiEntrypoint;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

public class PluginManagerScreen extends Screen {
    private final Screen parent;
    private final Map<ApiEntrypoint, Boolean> pluginStates;

    public PluginManagerScreen(Screen parent) {
        super(Text.translatable("screen.zhengzhengyiyi.plugin_manager"));
        this.parent = parent;
        this.pluginStates = new HashMap<>();
    }

    @Override
    protected void init() {
        super.init();

        int y = 40;
        
        for (ApiEntrypoint plugin : CommonEntryPoint.ENTRYPOINTS) {
            Identifier pluginId = plugin.getIdentifier();
            String pluginName = pluginId.getPath();
            pluginStates.put(plugin, true);

            this.addDrawableChild(ButtonWidget.builder(Text.literal(pluginName), button -> {})
                .dimensions(this.width / 2 - 120, y, 150, 20)
                .build());

            ButtonWidget toggleButton = ButtonWidget.builder(
                Text.translatable("button.zhengzhengyiyi.disable"),
                button -> togglePlugin(plugin, button)
            ).dimensions(this.width / 2 + 40, y, 80, 20).build();

            this.addDrawableChild(toggleButton);
            y += 25;
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
            .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
            .build());
    }

    private void togglePlugin(ApiEntrypoint plugin, ButtonWidget button) {
        boolean currentState = pluginStates.get(plugin);
        pluginStates.put(plugin, !currentState);
        
        if (currentState) {
            button.setMessage(Text.translatable("button.zhengzhengyiyi.enable"));
            if (!CommonEntryPoint.ENTRYPOINTS.contains(plugin)) CommonEntryPoint.ENTRYPOINTS.add(plugin);
        } else {
            button.setMessage(Text.translatable("button.zhengzhengyiyi.disable"));
            if (CommonEntryPoint.ENTRYPOINTS.contains(plugin)) CommonEntryPoint.ENTRYPOINTS.remove(plugin);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
