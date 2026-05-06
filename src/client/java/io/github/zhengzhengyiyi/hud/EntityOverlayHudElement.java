package io.github.zhengzhengyiyi.hud;

import io.github.zhengzhengyiyi.ConfigEditorClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Jade/WTHIT-style HUD overlay for entities.
 *
 * Shows when the crosshair is pointing at an entity:
 *   - Entity display name (custom name if set, otherwise type name)
 *   - Registry id  (e.g. "minecraft:zombie")
 *   - Health bar with numeric value  (only for LivingEntity)
 *
 * Rendered just below the block overlay position so both can coexist.
 * Controlled by the same "show_block_overlay" config flag.
 */
public class EntityOverlayHudElement implements HudElement {

    // Layout
    private static final int PADDING_X      = 8;
    private static final int PADDING_Y      = 5;
    private static final int ICON_SIZE      = 16;
    private static final int ICON_TEXT_GAP  = 5;
    private static final int LINE_GAP       = 2;
    private static final int HEALTH_BAR_H   = 4;
    private static final int HEALTH_BAR_GAP = 3;
    // Offset from top — placed below the block overlay (block overlay is ~36px tall at TOP_OFFSET=8)
    private static final int TOP_OFFSET     = 52;

    // Colors
    private static final int BG_COLOR         = 0xD0141420; // deep dark semi-transparent
    private static final int BORDER_COLOR     = 0xFF89B4FA; // blue accent border
    private static final int NAME_COLOR       = 0xFFCDD6F4; // soft white
    private static final int ID_COLOR         = 0xFF6C7086; // muted grey
    private static final int HEALTH_BG_COLOR  = 0xFF313244; // dark track
    private static final int HEALTH_FG_COLOR  = 0xFFA6E3A1; // green health
    private static final int HEALTH_LOW_COLOR = 0xFFF38BA8; // red low health

    @Override
    public void extractRenderState(@SuppressWarnings("null") GuiGraphicsExtractor context, @SuppressWarnings("null") DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.screen != null) return;
        if (mc.level == null || mc.player == null) return;
        if (!ConfigEditorClient.configManager.getConfig().showEntityOverlay) return;

        // crosshairPickEntity is the entity the crosshair is directly targeting
        Entity entity = mc.crosshairPickEntity;
        if (entity == null || !entity.isAlive()) return;
        // Don't show overlay for the player themselves
        if (entity == mc.player) return;

        Font font = mc.font;
        int screenWidth = context.guiWidth();
        int lineHeight = font.lineHeight;

        // --- Collect display data ---
        Component displayName = entity.getDisplayName();
        String registryId = getRegistryId(mc, entity);

        boolean isLiving = entity instanceof LivingEntity;
        float health    = isLiving ? ((LivingEntity) entity).getHealth()    : 0f;
        float maxHealth = isLiving ? ((LivingEntity) entity).getMaxHealth() : 0f;

        // --- Measure layout ---
        int nameWidth = font.width(displayName);
        int idWidth   = registryId.isEmpty() ? 0 : font.width(registryId);

        // Health text: "12.0 / 20.0"
        String healthText = isLiving
                ? String.format("%.1f / %.1f", health, maxHealth)
                : "";
        @SuppressWarnings("null")
        int healthTextWidth = isLiving ? font.width(healthText) : 0;

        int maxTextWidth = Math.max(nameWidth, Math.max(idWidth, healthTextWidth + 4 + 60));

        // Content height: name + id + (health bar row if living)
        int textBlockHeight = lineHeight
                + (registryId.isEmpty() ? 0 : LINE_GAP + lineHeight)
                + (isLiving ? HEALTH_BAR_GAP + HEALTH_BAR_H + LINE_GAP + lineHeight : 0);
        int contentHeight = Math.max(ICON_SIZE, textBlockHeight);

        int boxWidth  = PADDING_X + ICON_SIZE + ICON_TEXT_GAP + maxTextWidth + PADDING_X;
        int boxHeight = PADDING_Y + contentHeight + PADDING_Y;

        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = TOP_OFFSET;

        // --- Background + border ---
        context.fill(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, BORDER_COLOR);
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_COLOR);

        // --- Entity spawn egg icon (best approximation without entity rendering) ---
        ItemStack eggStack = getSpawnEgg(entity);
        int iconX = boxX + PADDING_X;
        int iconY = boxY + PADDING_Y + (contentHeight - ICON_SIZE) / 2;
        if (!eggStack.isEmpty()) {
            context.item(eggStack, iconX, iconY);
        }

        // --- Text column ---
        int textX = boxX + PADDING_X + ICON_SIZE + ICON_TEXT_GAP;
        int textY = boxY + PADDING_Y + (contentHeight - textBlockHeight) / 2;

        // Entity name
        context.text(font, displayName, textX, textY, NAME_COLOR, true);
        textY += lineHeight + LINE_GAP;

        // Registry id
        if (!registryId.isEmpty()) {
            context.text(font, registryId, textX, textY, ID_COLOR, false);
            textY += lineHeight + LINE_GAP;
        }

        // Health bar + numeric value
        if (isLiving && maxHealth > 0) {
            textY += HEALTH_BAR_GAP;
            int barWidth = maxTextWidth - healthTextWidth - 4;
            barWidth = Math.max(20, barWidth);

            // Background track
            context.fill(textX, textY, textX + barWidth, textY + HEALTH_BAR_H, HEALTH_BG_COLOR);

            // Filled portion
            float ratio = Math.min(1f, health / maxHealth);
            int filledWidth = (int)(barWidth * ratio);
            int barColor = ratio > 0.4f ? HEALTH_FG_COLOR : HEALTH_LOW_COLOR;
            if (filledWidth > 0) {
                context.fill(textX, textY, textX + filledWidth, textY + HEALTH_BAR_H, barColor);
            }

            // Numeric health text to the right of the bar
            context.text(font, healthText, textX + barWidth + 4, textY - 1, ID_COLOR, false);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String getRegistryId(Minecraft mc, Entity entity) {
        if (mc.level == null) return "";
        Identifier id = mc.level.registryAccess()
                .lookupOrThrow(Registries.ENTITY_TYPE)
                .getKey(entity.getType());
        return id != null ? id.getNamespace() + ":" + id.getPath() : "";
    }

    /**
     * Returns a spawn egg ItemStack for the entity type if one exists,
     * otherwise falls back to a barrier icon so the slot is never empty.
     */
    private static ItemStack getSpawnEgg(Entity entity) {
        // Try to get the spawn egg via the entity type's description id
        // e.g. "entity.minecraft.zombie" → look for zombie_spawn_egg
        String descId = entity.getType().getDescriptionId(); // "entity.minecraft.zombie"
        String[] parts = descId.split("\\.");
        if (parts.length >= 3) {
            String eggName = parts[2] + "_spawn_egg"; // "zombie_spawn_egg"
            // Try to find it in the item registry by name
            try {
                var itemRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
                @SuppressWarnings("null")
                var eggId = Identifier.fromNamespaceAndPath(parts[1], eggName);
                var item = itemRegistry.getValue(eggId);
                if (item != null && item != Items.AIR) {
                    return new ItemStack(item);
                }
            } catch (Exception ignored) { }
        }
        // Fallback: barrier as a generic "entity" icon
        return new ItemStack(Items.BARRIER);
    }
}
