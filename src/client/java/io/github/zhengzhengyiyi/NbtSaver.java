package io.github.zhengzhengyiyi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;

import io.github.zhengzhengyiyi.gui.NbtEditorScreen;
import net.minecraft.world.level.block.state.BlockState;

public class NbtSaver {
    private final Minecraft client = Minecraft.getInstance();

    public void saveAndOpenEditor() {
        if (client == null || client.player == null || client.level == null) return;
        CompoundTag tag = readPointedNbt(client.player, client.level);
        openEditor(tag);
    }

    private CompoundTag readPointedNbt(LocalPlayer player, ClientLevel world) {
        HitResult hit = client.hitResult;
        if (hit == null) return new CompoundTag();

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            BlockEntity be = world.getBlockEntity(bhr.getBlockPos());
            CompoundTag tag = new CompoundTag();
            if (be != null) {
//                tag = be.saveWithoutMetadata(null);
            } else {
                BlockState state = world.getBlockState(bhr.getBlockPos());
                tag.putString("block", state.toString());
            }
            return tag;
        }

        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) hit;
            Entity e = ehr.getEntity();
            CompoundTag tag = new CompoundTag();
            try {
                Field nbtField = Entity.class.getDeclaredField("customData");
                nbtField.setAccessible(true);
                Object nbtObj = nbtField.get(e);
                if (nbtObj instanceof CompoundTag) {
                    tag = (CompoundTag) nbtObj;
                }
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
            return tag;
        }

        return new CompoundTag();
    }

    private void openEditor(CompoundTag tag) {
        client.execute(() -> client.setScreen(new NbtEditorScreen(tag)));
    }
}
