package io.github.zhengzhengyiyi.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import io.github.zhengzhengyiyi.gui.widget.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class NbtEditorScreen extends Screen {
    private Optional<CompoundTag> nbtData = Optional.empty();
    private EditBox nameField;
    private MultilineEditor contentField;
    private EditBox renameField;
    @SuppressWarnings("unused")
    private final Minecraft client = Minecraft.getInstance();

    public NbtEditorScreen(CompoundTag nbtData) {
        super(Component.literal("NBT Editor"));
        this.nbtData = Optional.ofNullable(nbtData);
    }

    @SuppressWarnings("null")
    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        nameField = new EditBox(font, centerX - 100, centerY - 80, 200, 20, Component.literal("Original Name"));
        contentField = new MultilineEditor(centerX - 100, centerY - 50, 200, 100, Component.literal("NBT Content"));
        renameField = new EditBox(font, centerX - 100, centerY + 70, 200, 20, Component.literal("Save File Name"));

        nameField.setValue(nbtData.flatMap(nbt -> nbt.getString("Name")).orElse(""));
        contentField.setText(nbtToJsonString(nbtData.orElse(new CompoundTag())));
        renameField.setValue(nameField.getValue());

        addRenderableWidget(nameField);
        addRenderableWidget(contentField);
        addRenderableWidget(renameField);

        Button saveButton = Button.builder(Component.literal("Save"), button -> saveNbt())
                .bounds(centerX - 110, centerY + 100, 100, 20)
                .build();

        Button closeButton = Button.builder(Component.literal("Close"), button -> minecraft.setScreen(null))
                .bounds(centerX + 10, centerY + 100, 100, 20)
                .build();

        addRenderableWidget(saveButton);
        addRenderableWidget(closeButton);
    }

    @SuppressWarnings("null")
    private String nbtToJsonString(CompoundTag nbt) {
        Gson gson = new GsonBuilder()
                .disableHtmlEscaping()
                .create();
        JsonObject jsonObject = new JsonObject();
        Set<String> keys = nbt.keySet();
        for (String key : keys) {
            Tag element = nbt.get(key);
            jsonObject.addProperty(key, element.toString());
        }
        return gson.toJson(jsonObject);
    }

    private void saveNbt() {
        String fileName = renameField.getValue().trim();
        if (!fileName.isEmpty() && nbtData.isPresent()) {
            File saveDir = new File(Minecraft.getInstance().gameDirectory, "saved_nbt");
            if (!saveDir.exists()) saveDir.mkdirs();

            File outFile = new File(saveDir, fileName + ".json");
            try (FileWriter writer = new FileWriter(outFile)) {
                Gson gson = new GsonBuilder()
                        .disableHtmlEscaping()
                        .create();
                JsonElement jsonElement = JsonParser.parseString(nbtToJsonString(nbtData.get()));
                gson.toJson(jsonElement, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        minecraft.setScreen(null);
    }

    @SuppressWarnings("null")
    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
    }
}
