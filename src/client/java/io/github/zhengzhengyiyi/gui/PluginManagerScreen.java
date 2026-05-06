package io.github.zhengzhengyiyi.gui;

import io.github.zhengzhengyiyi.ConfigEditorClient;
import io.github.zhengzhengyiyi.api.ApiEntrypoint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;

public class PluginManagerScreen extends Screen {
    private final Screen parent;
    private final Map<ApiEntrypoint, Boolean> pluginStates;

    public PluginManagerScreen(Screen parent) {
        super(Component.translatable("screen.zhengzhengyiyi.plugin_manager"));
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

            this.addRenderableWidget(Button.builder(Component.literal(pluginName), button -> {})
                .bounds(this.width / 2 - 120, y, 150, 20)
                .build());

            Button toggleButton = Button.builder(
                ConfigEditorClient.ENTRYPOINTS.contains(plugin)
                    ? Component.translatable("button.zhengzhengyiyi.enable")
                    : Component.translatable("button.zhengzhengyiyi.disable"),
                button -> togglePlugin(plugin, button)
            ).bounds(this.width / 2 + 40, y, 80, 20).build();

            this.addRenderableWidget(toggleButton);
            y += 25;
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
            .build());
    }

    private void togglePlugin(ApiEntrypoint plugin, Button button) {
        if (ConfigEditorClient.ENTRYPOINTS.contains(plugin)) {
            button.setMessage(Component.translatable("button.zhengzhengyiyi.disable"));
            ConfigEditorClient.ENTRYPOINTS.remove(plugin);
        } else {
            button.setMessage(Component.translatable("button.zhengzhengyiyi.enable"));
            ConfigEditorClient.ENTRYPOINTS.add(plugin);
        }
    }

    @SuppressWarnings("null")
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
