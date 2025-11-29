package io.github.zhengzhengyiyi.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

public class NbtEditorScreen2 extends Screen {

    private Optional<NbtCompound> nbtData = Optional.empty();
    private TextFieldWidget nameField;
    private TextFieldWidget contentField;
    private TextFieldWidget renameField;
    private final MinecraftClient client = MinecraftClient.getInstance();

    public NbtEditorScreen2(NbtCompound nbtData) {
        super(Text.of("NBT Editor"));
        this.nbtData = Optional.ofNullable(nbtData);
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;

        nameField = new TextFieldWidget(textRenderer, centerX - 100, centerY - 80, 200, 20, Text.of("Original Name"));
        contentField = new TextFieldWidget(textRenderer, centerX - 100, centerY - 50, 200, 100, Text.of("NBT Content"));
        renameField = new TextFieldWidget(textRenderer, centerX - 100, centerY + 70, 200, 20, Text.of("Save File Name"));

        nameField.setText(nbtData.flatMap(nbt -> nbt.getString("Name")).orElse(""));
        contentField.setText(nbtToJsonString(nbtData.orElse(new NbtCompound())));
        renameField.setText(nameField.getText());

        addSelectableChild(nameField);
        addSelectableChild(contentField);
        addSelectableChild(renameField);

        ButtonWidget saveButton = ButtonWidget.builder(Text.of("Save"), button -> saveNbt())
                .dimensions(centerX - 110, centerY + 100, 100, 20)
                .build();

        ButtonWidget closeButton = ButtonWidget.builder(Text.of("Close"), button -> client.setScreen(null))
                .dimensions(centerX + 10, centerY + 100, 100, 20)
                .build();

        addDrawableChild(saveButton);
        addDrawableChild(closeButton);
    }

    private String nbtToJsonString(NbtCompound nbt) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject jsonObject = new JsonObject();
        Set<String> keys = nbt.getKeys();
        for (String key : keys) {
            NbtElement element = nbt.get(key);
            jsonObject.addProperty(key, element.toString());
        }
        return gson.toJson(jsonObject);
    }

    private void saveNbt() {
        String fileName = renameField.getText().trim();
        if (!fileName.isEmpty() && nbtData.isPresent()) {
            File saveDir = new File(MinecraftClient.getInstance().runDirectory, "saved_nbt");
            if (!saveDir.exists()) saveDir.mkdirs();

            File outFile = new File(saveDir, fileName + ".json");
            try (FileWriter writer = new FileWriter(outFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonElement jsonElement = JsonParser.parseString(nbtToJsonString(nbtData.get()));
                gson.toJson(jsonElement, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        client.setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        nameField.render(context, mouseX, mouseY, delta);
        contentField.render(context, mouseX, mouseY, delta);
        renameField.render(context, mouseX, mouseY, delta);
    }
}
