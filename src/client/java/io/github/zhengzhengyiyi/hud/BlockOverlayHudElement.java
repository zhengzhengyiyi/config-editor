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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BlockOverlayHudElement implements HudElement {
    // Layout constants
    private static final int PADDING_X     = 8;
    private static final int PADDING_Y     = 5;
    private static final int ICON_SIZE     = 16;
    private static final int ICON_TEXT_GAP = 5;
    private static final int TOP_OFFSET    = 8;
    private static final int LINE_GAP      = 2;

    // Colors
    private static final int BG_COLOR       = 0xD0141420; // deep dark semi-transparent
    private static final int BORDER_COLOR   = 0xFF89B4FA; // blue accent border
    private static final int TEXT_COLOR     = 0xFFCDD6F4; // soft white
    private static final int SUB_TEXT_COLOR = 0xFF6C7086; // muted grey for registry id

    @Override
    public void extractRenderState(@SuppressWarnings("null") GuiGraphicsExtractor context, @SuppressWarnings("null") DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();

        // Only render in-game with no screen open
        if (mc.screen != null) return;
        if (mc.level == null || mc.player == null) return;
        if (!ConfigEditorClient.configManager.getConfig().showBlockOverlay) return;

        var level = mc.level; // non-null after the check above

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        @SuppressWarnings("null")
        BlockState state = level.getBlockState(blockHit.getBlockPos());
        if (state.isAir()) return;

        Block block = state.getBlock();
        Component blockName = block.getName();

        // Get registry id (e.g. "minecraft:stone")
        Identifier blockId = level.registryAccess()
                .lookupOrThrow(Registries.BLOCK)
                .getKey(block);
        String registryId = blockId != null
                ? blockId.getNamespace() + ":" + blockId.getPath()
                : "";

        Font font = mc.font;
        int screenWidth = context.guiWidth();
        int lineHeight = font.lineHeight;

        int nameWidth = font.width(blockName);
        int idWidth   = registryId.isEmpty() ? 0 : font.width(registryId);
        int maxTextWidth = Math.max(nameWidth, idWidth);

        // Content area: icon on the left, two text lines on the right
        int textBlockHeight = registryId.isEmpty()
                ? lineHeight
                : lineHeight + LINE_GAP + lineHeight;
        int contentHeight = Math.max(ICON_SIZE, textBlockHeight);

        int boxWidth  = PADDING_X + ICON_SIZE + ICON_TEXT_GAP + maxTextWidth + PADDING_X;
        int boxHeight = PADDING_Y + contentHeight + PADDING_Y;

        int boxX = (screenWidth - boxWidth) / 2;
        int boxY = TOP_OFFSET;

        // --- Draw border (1px outline) ---
        context.fill(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, BORDER_COLOR);
        // --- Draw background ---
        context.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, BG_COLOR);

        // --- Draw block item icon ---
        ItemStack stack = new ItemStack(block.asItem());
        int iconX = boxX + PADDING_X;
        int iconY = boxY + PADDING_Y + (contentHeight - ICON_SIZE) / 2;
        if (!stack.isEmpty()) {
            context.item(stack, iconX, iconY);
        }

        // --- Draw text ---
        int textX = boxX + PADDING_X + ICON_SIZE + ICON_TEXT_GAP;

        if (registryId.isEmpty()) {
            // Single line — vertically center
            int textY = boxY + PADDING_Y + (contentHeight - lineHeight) / 2;
            context.text(font, blockName, textX, textY, TEXT_COLOR, true);
        } else {
            // Two lines: block name + registry id
            int nameY = boxY + PADDING_Y + (contentHeight - textBlockHeight) / 2;
            int idY   = nameY + lineHeight + LINE_GAP;
            context.text(font, blockName, textX, nameY, TEXT_COLOR, true);
            context.text(font, registryId, textX, idY, SUB_TEXT_COLOR, false);
        }
    }
}
