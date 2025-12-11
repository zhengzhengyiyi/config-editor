package io.github.zhengzhengyiyi.gui;

import io.github.zhengzhengyiyi.ConfigEditorClient;
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
        
        for (ApiEntrypoint plugin : ConfigEditorClient.TOTAL_ENTRYPOINTS) {
            Identifier pluginId = plugin.getIdentifier();
            String pluginName = pluginId.getPath();
            pluginStates.put(plugin, ConfigEditorClient.ENTRYPOINTS.contains(plugin));
            
//            System.out.println("Loading plugin: " + pluginName + "Enabled: " + pluginStates.get(plugin));

            this.addDrawableChild(ButtonWidget.builder(Text.literal(pluginName), button -> {})
                .dimensions(this.width / 2 - 120, y, 150, 20)
                .build());

            ButtonWidget toggleButton = ButtonWidget.builder(
            		ConfigEditorClient.ENTRYPOINTS.contains(plugin) ? Text.translatable("button.zhengzhengyiyi.enable") : Text.translatable("button.zhengzhengyiyi.disable"),
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
            if (ConfigEditorClient.ENTRYPOINTS.contains(plugin)) {
            	button.setMessage(Text.translatable("button.zhengzhengyiyi.disable"));
//            	CommonEntryPoint.DISABLED_ENTRYPOINTS.add(plugin);
            	ConfigEditorClient.ENTRYPOINTS.remove(plugin);
            } else {
            	button.setMessage(Text.translatable("button.zhengzhengyiyi.enable"));
//            	CommonEntryPoint.DISABLED_ENTRYPOINTS.remove(plugin);
            	ConfigEditorClient.ENTRYPOINTS.add(plugin);
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
