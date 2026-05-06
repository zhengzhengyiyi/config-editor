package io.github.zhengzhengyiyi.gui;

import io.github.zhengzhengyiyi.ConfigEditorClient;
import io.github.zhengzhengyiyi.config.ModConfigData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A hub screen for configuring the entity overlay HUD element.
 *
 * <p>Accessible from the main {@link EditorScreen} via the "Entity Overlay" button.
 * Allows the player to toggle the entity overlay on/off and toggle the block
 * overlay independently, without leaving the game to edit a config file.</p>
 *
 * <p>All changes are persisted immediately through {@link ConfigEditorClient#configManager}.</p>
 */
public class EntityOverlayHubScreen extends Screen {

    /** The screen to return to when this hub is closed. */
    private final Screen parent;

    /** Toggle button for the entity overlay. */
    private Button entityOverlayToggle;

    /** Toggle button for the block overlay. */
    private Button blockOverlayToggle;

    /**
     * Creates a new {@code EntityOverlayHubScreen}.
     *
     * @param parent the screen to return to on close; may be {@code null}
     */
    public EntityOverlayHubScreen(Screen parent) {
        super(Component.translatable("configeditor.entityhub.title"));
        this.parent = parent;
    }

    @SuppressWarnings("null")
    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY   = this.height / 2 - 60;
        int btnW     = 220;
        int btnH     = 20;
        int gap      = 28;

        // --- Entity overlay toggle ---
        entityOverlayToggle = Button.builder(
                buildEntityLabel(),
                button -> toggleEntityOverlay())
                .bounds(centerX - btnW / 2, startY, btnW, btnH)
                .build();
        this.addRenderableWidget(entityOverlayToggle);

        // --- Block overlay toggle ---
        blockOverlayToggle = Button.builder(
                buildBlockLabel(),
                button -> toggleBlockOverlay())
                .bounds(centerX - btnW / 2, startY + gap, btnW, btnH)
                .build();
        this.addRenderableWidget(blockOverlayToggle);

        // --- Done button ---
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose())
                .bounds(centerX - 100, this.height - 38, 200, btnH)
                .build());
    }

    // -------------------------------------------------------------------------
    // Toggle helpers
    // -------------------------------------------------------------------------

    /**
     * Flips the {@code showEntityOverlay} config flag and saves the config.
     * Updates the button label to reflect the new state.
     */
    @SuppressWarnings("null")
    private void toggleEntityOverlay() {
        ModConfigData cfg = ConfigEditorClient.configManager.getConfig();
        cfg.showEntityOverlay = !cfg.showEntityOverlay;
        ConfigEditorClient.configManager.save();
        entityOverlayToggle.setMessage(buildEntityLabel());
    }

    /**
     * Flips the {@code showBlockOverlay} config flag and saves the config.
     * Updates the button label to reflect the new state.
     */
    @SuppressWarnings("null")
    private void toggleBlockOverlay() {
        ModConfigData cfg = ConfigEditorClient.configManager.getConfig();
        cfg.showBlockOverlay = !cfg.showBlockOverlay;
        ConfigEditorClient.configManager.save();
        blockOverlayToggle.setMessage(buildBlockLabel());
    }

    // -------------------------------------------------------------------------
    // Label builders
    // -------------------------------------------------------------------------

    /**
     * Builds the display label for the entity overlay toggle button,
     * reflecting the current enabled/disabled state.
     *
     * @return a {@link Component} with the current state label
     */
    private Component buildEntityLabel() {
        boolean on = ConfigEditorClient.configManager.getConfig().showEntityOverlay;
        return Component.translatable(
                on ? "configeditor.entityhub.entity_on"
                   : "configeditor.entityhub.entity_off");
    }

    /**
     * Builds the display label for the block overlay toggle button,
     * reflecting the current enabled/disabled state.
     *
     * @return a {@link Component} with the current state label
     */
    private Component buildBlockLabel() {
        boolean on = ConfigEditorClient.configManager.getConfig().showBlockOverlay;
        return Component.translatable(
                on ? "configeditor.entityhub.block_on"
                   : "configeditor.entityhub.block_off");
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void extractRenderState(@SuppressWarnings("null") GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        // Dim background
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        // Title
        context.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 90, 0xFFFFFF);

        // Status summary below the title
        ModConfigData cfg = ConfigEditorClient.configManager.getConfig();
        String summary = "Entity: " + (cfg.showEntityOverlay ? "§aON§r" : "§cOFF§r")
                + "   Block: " + (cfg.showBlockOverlay ? "§aON§r" : "§cOFF§r");
        context.centeredText(this.font, Component.literal(summary), this.width / 2, this.height / 2 - 75, 0xAAAAAA);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    /**
     * Handles keyboard input.
     *
     * <ul>
     *   <li>Ctrl+A / Ctrl+C / Ctrl+V — forwarded to the focused widget so
     *       clipboard shortcuts always work on this screen.</li>
     *   <li>Escape — closes the screen and returns to the parent.</li>
     * </ul>
     *
     * @param input the key event
     * @return {@code true} if the event was consumed
     */
    @Override
    public boolean keyPressed(@SuppressWarnings("null") KeyEvent input) {
        // Always pass Ctrl+A/C/V to the focused widget
        if (input.hasControlDown()) {
            int k = input.key();
            if (k == GLFW.GLFW_KEY_A || k == GLFW.GLFW_KEY_C || k == GLFW.GLFW_KEY_V) {
                var focused = getFocused();
                if (focused != null && focused.keyPressed(input)) {
                    return true;
                }
            }
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        return super.keyPressed(input);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Closes this screen and returns to the {@link #parent} screen.
     */
    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
