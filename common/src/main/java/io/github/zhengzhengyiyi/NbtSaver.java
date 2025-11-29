package io.github.zhengzhengyiyi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;

import java.lang.reflect.Field;

import io.github.zhengzhengyiyi.gui.NbtEditorScreen;
import net.minecraft.block.BlockState;

public class NbtSaver {
    private final MinecraftClient client = MinecraftClient.getInstance();

    public void saveAndOpenEditor() {
        if (client == null || client.player == null || client.world == null) return;
        NbtCompound tag = readPointedNbt(client.player, client.world);
        openEditor(tag);
    }

    private NbtCompound readPointedNbt(ClientPlayerEntity player, ClientWorld world) {
        HitResult hit = client.crosshairTarget;
        if (hit == null) return new NbtCompound();

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            BlockEntity be = world.getBlockEntity(bhr.getBlockPos());
            NbtCompound tag = new NbtCompound();
            if (be != null) {
//                tag = be.createNbt(null);
            } else {
                BlockState state = world.getBlockState(bhr.getBlockPos());
                tag.putString("block", state.toString());
            }
            return tag;
        }

        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) hit;
            Entity e = ehr.getEntity();
            NbtCompound tag = new NbtCompound();
            try {
                Field nbtField = Entity.class.getDeclaredField("customData");
                nbtField.setAccessible(true);
                Object nbtObj = nbtField.get(e);
                if (nbtObj instanceof NbtCompound) {
                    tag = (NbtCompound) nbtObj;
                }
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
            return tag;
        }

        return new NbtCompound();
    }

    private void openEditor(NbtCompound tag) {
        client.execute(() -> client.setScreen(new NbtEditorScreen(tag)));
    }
}

