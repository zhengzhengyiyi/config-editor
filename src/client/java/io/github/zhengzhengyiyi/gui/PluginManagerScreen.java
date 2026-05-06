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

        int centerX = this.width / 2;
        int y = 50;
        
        for (ApiEntrypoint plugin : ConfigEditorClient.TOTAL_ENTRYPOINTS) {
            Identifier pluginId = plugin.getIdentifier();
            String pluginName = pluginId.getPath();
            pluginStates.put(plugin, ConfigEditorClient.ENTRYPOINTS.contains(plugin));

            // Plugin name label (non-interactive button used as a label)
            this.addRenderableWidget(Button.builder(Component.literal(pluginName), button -> {})
                .bounds(centerX - 130, y, 160, 20)
                .build());

            boolean enabled = ConfigEditorClient.ENTRYPOINTS.contains(plugin);
            Button toggleButton = Button.builder(
                enabled
                    ? Component.literal("§a✔ Enabled")
                    : Component.literal("§c✘ Disabled"),
                button -> togglePlugin(plugin, button)
            ).bounds(centerX + 40, y, 90, 20).build();

            this.addRenderableWidget(toggleButton);
            y += 26;
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(centerX - 100, this.height - 32, 200, 20)
            .build());
    }

    private void togglePlugin(ApiEntrypoint plugin, Button button) {
        String pluginId = plugin.getIdentifier().toString();
        
        if (ConfigEditorClient.ENTRYPOINTS.contains(plugin)) {
            // Disable plugin
            button.setMessage(Component.literal("§c✘ Disabled"));
            ConfigEditorClient.ENTRYPOINTS.remove(plugin);
            
            // Add to disabled list in config
            if (!ConfigEditorClient.configManager.getConfig().disabledPlugins.contains(pluginId)) {
                ConfigEditorClient.configManager.getConfig().disabledPlugins.add(pluginId);
            }
        } else {
            // Enable plugin
            button.setMessage(Component.literal("§a✔ Enabled"));
            ConfigEditorClient.ENTRYPOINTS.add(plugin);
            
            // Remove from disabled list in config
            ConfigEditorClient.configManager.getConfig().disabledPlugins.remove(pluginId);
        }
        
        // Save config to persist changes
        ConfigEditorClient.configManager.save();
    }

    @SuppressWarnings("null")
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Background
        context.fill(0, 0, this.width, this.height, 0xFF1E1E2E);

        // Header bar
        context.fill(0, 0, this.width, 36, 0xFF181825);
        context.fill(0, 35, this.width, 36, 0xFF89B4FA);

        // Title
        context.centeredText(this.font, Component.literal("✦ Plugin Manager"), this.width / 2, 12, 0xFFCDD6F4);

        // Column headers
        int centerX = this.width / 2;
        context.text(this.font, "§7Plugin", centerX - 130, 40, 0xFF6C7086, false);
        context.text(this.font, "§7Status", centerX + 40, 40, 0xFF6C7086, false);

        // Row backgrounds
        int y = 50;
        int rowIndex = 0;
        for (ApiEntrypoint plugin : ConfigEditorClient.TOTAL_ENTRYPOINTS) {
            int rowBg = (rowIndex % 2 == 0) ? 0x18313244 : 0x10181825;
            context.fill(centerX - 135, y - 1, centerX + 135, y + 21, rowBg);
            y += 26;
            rowIndex++;
        }

        // Bottom separator
        context.fill(0, this.height - 38, this.width, this.height - 37, 0xFF45475A);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
