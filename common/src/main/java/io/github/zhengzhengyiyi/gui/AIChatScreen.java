package io.github.zhengzhengyiyi.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.github.zhengzhengyiyi.api.AiClient;

public class AIChatScreen extends Screen {
    private TextFieldWidget inputField;
    private TextFieldWidget pathField;
    private ButtonWidget sendButton;
    private ButtonWidget loadButton;
    private ButtonWidget clearButton;
    private ButtonWidget settingsButton;
    private List<String> chatHistory;
    private List<String> userInputs;
    private boolean waitingForSendResponse = false;
    private boolean loadingFile = false;
    private boolean serverAvailable = false;
    private boolean checkingServer = true;
    private int chatScrollOffset = 0;
    private ButtonWidget scrollUpButton;
    private ButtonWidget scrollDownButton;
    private static Logger LOGGER = LoggerFactory.getLogger(AIChatScreen.class);
    private AiClient aiClient = new AiClient();

    public AIChatScreen() {
        super(Text.literal("AI Chat"));
        this.chatHistory = new ArrayList<>();
        this.userInputs = new ArrayList<>();
        chatHistory.add("AI: Hello! How can I help you today?");
        chatHistory.add("AI: I'm ready to assist you with anything!");
    }

    @Override
    protected void init() {
        super.init();
        checkServerAvailability();
        int panelWidth = Math.min(400, this.width - 40);
        int panelHeight = Math.min(300, this.height - 80);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 20;
        int inputY = panelY + panelHeight + 10;
        int inputWidth = panelWidth - 100;
        this.inputField = new TextFieldWidget(this.textRenderer, panelX, inputY, inputWidth, 20, Text.literal("Type your message..."));
        this.inputField.setMaxLength(1024);
        this.inputField.setEditable(false);
        this.addDrawableChild(this.inputField);
        this.sendButton = ButtonWidget.builder(Text.literal("Send"), button -> sendMessage())
            .dimensions(panelX + inputWidth + 5, inputY, 45, 20)
            .build();
        this.sendButton.active = false;
        this.addDrawableChild(this.sendButton);
        int pathY = inputY + 26;
        this.pathField = new TextFieldWidget(this.textRenderer, panelX, pathY, inputWidth - 50, 20, Text.literal("Enter file or folder path relative to .minecraft or absolute"));
        this.pathField.setMaxLength(1024);
        this.addDrawableChild(this.pathField);
        this.loadButton = ButtonWidget.builder(Text.literal("Load"), button -> loadPathAndSend())
            .dimensions(panelX + inputWidth - 40, pathY, 45, 20)
            .build();
        this.addDrawableChild(this.loadButton);
        this.clearButton = ButtonWidget.builder(Text.literal("Clear"), button -> clearChat())
            .dimensions(panelX + panelWidth - 90, panelY - 25, 45, 20)
            .build();
        this.addDrawableChild(this.clearButton);
        this.settingsButton = ButtonWidget.builder(Text.literal("Settings"), button -> openSettings())
            .dimensions(panelX + panelWidth - 45, panelY - 25, 45, 20)
            .build();
        this.addDrawableChild(this.settingsButton);
        this.scrollUpButton = ButtonWidget.builder(Text.literal("↑"), button -> scrollUp())
            .dimensions(panelX + panelWidth - 20, panelY + 25, 16, 16)
            .build();
        this.addDrawableChild(this.scrollUpButton);
        this.scrollDownButton = ButtonWidget.builder(Text.literal("↓"), button -> scrollDown())
            .dimensions(panelX + panelWidth - 20, panelY + panelHeight - 20, 16, 16)
            .build();
        this.addDrawableChild(this.scrollDownButton);
        this.setInitialFocus(this.inputField);
        updateScrollButtons();
    }

    private void checkServerAvailability() {
        checkingServer = true;
        aiClient.checkServerStatus().thenAccept(available -> {
            serverAvailable = available;
            checkingServer = false;
            if (!serverAvailable) {
                chatHistory.add("System: Ollama server is not available. Please make sure Ollama is running on localhost:11434");
            }
            updateUIState();
        }).exceptionally(throwable -> {
            serverAvailable = false;
            checkingServer = false;
            chatHistory.add("System: Failed to check server status: " + throwable.getMessage());
            updateUIState();
            return null;
        });
    }

    private void updateUIState() {
        if (this.inputField != null) {
            this.inputField.setEditable(serverAvailable && !waitingForSendResponse);
        }
        if (this.sendButton != null) {
            this.sendButton.active = serverAvailable && !waitingForSendResponse && (this.inputField != null && !this.inputField.getText().trim().isEmpty());
        }
        if (this.loadButton != null) {
            this.loadButton.active = serverAvailable && !loadingFile;
        }
        updateScrollButtons();
    }

    private void updateScrollButtons() {
        if (scrollUpButton != null && scrollDownButton != null) {
            scrollUpButton.active = chatScrollOffset > 0;
            scrollDownButton.active = chatScrollOffset < getMaxScrollOffset();
        }
    }

    private int getMaxScrollOffset() {
        int panelWidth = Math.min(400, this.width - 40);
        int panelHeight = Math.min(300, this.height - 80);
        int maxVisibleLines = (panelHeight - 50) / 10;
        int totalLines = getTotalChatLines(panelWidth - 30);
        return Math.max(0, totalLines - maxVisibleLines);
    }

    private int getTotalChatLines(int maxWidth) {
        int totalLines = 0;
        for (String message : chatHistory) {
            List<String> wrappedLines = wrapText(message, maxWidth);
            totalLines += wrappedLines.size();
        }
        if (checkingServer || waitingForSendResponse || loadingFile) {
            totalLines += 1;
        }
        return totalLines;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (this.textRenderer.getWidth(testLine) <= maxWidth) {
                currentLine.append(currentLine.length() > 0 ? " " + word : word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                    currentLine = new StringBuilder();
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private void scrollUp() {
        if (chatScrollOffset > 0) {
            chatScrollOffset--;
            updateScrollButtons();
        }
    }

    private void scrollDown() {
        if (chatScrollOffset < getMaxScrollOffset()) {
            chatScrollOffset++;
            updateScrollButtons();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelWidth = Math.min(400, this.width - 40);
        int panelHeight = Math.min(300, this.height - 80);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 20;
        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
            if (verticalAmount < 0) {
                scrollDown();
            } else if (verticalAmount > 0) {
                scrollUp();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(400, this.width - 40);
        int panelHeight = Math.min(300, this.height - 80);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 20;
        context.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2, 0xFF333333);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1a1a1a);
        String title = "AI Chat Assistant";
        if (checkingServer) {
            title += " (Checking server...)";
        } else if (!serverAvailable) {
            title += " (Server Offline)";
        }
        context.drawText(this.textRenderer, Text.literal(title), this.width / 2 - 70, panelY - 15, 0xFFFFFF, false);
        context.drawHorizontalLine(panelX + 5, panelX + panelWidth - 5, panelY + 20, 0xFF666666);
        int chatY = panelY + 35;
        int maxVisibleLines = (panelHeight - 50) / 12;
        int textAreaWidth = panelWidth - 30;
        drawScrollBar(context, panelX, panelY, panelWidth, panelHeight);
        updateUIState();
        super.render(context, mouseX, mouseY, delta);
        context.enableScissor(panelX + 5, panelY + 25, panelX + panelWidth - 5, panelY + panelHeight - 5);
        int currentLine = 0;
        int visibleLineCount = 0;
        for (String message : chatHistory) {
            List<String> wrappedLines = wrapText(message, textAreaWidth);
            for (String line : wrappedLines) {
                if (currentLine >= chatScrollOffset && visibleLineCount < maxVisibleLines) {
                    int renderY = chatY + visibleLineCount * 12;
                    int color = getMessageColor(message);
                    context.drawText(this.textRenderer, line, panelX + 10, renderY, color, false);
                    visibleLineCount++;
                }
                currentLine++;
            }
        }
        if ((checkingServer || waitingForSendResponse || loadingFile) && visibleLineCount < maxVisibleLines) {
            int renderY = chatY + visibleLineCount * 12;
            String loadingText = checkingServer ? "Checking server status" : (waitingForSendResponse ? "AI is thinking" : "Loading files...");
            context.drawText(this.textRenderer, loadingText, panelX + 10, renderY, 0xFFFFFFFF, false);
        }
        context.disableScissor();
    }

    private int getMessageColor(String message) {
        return 0xFFFFFFFF;
    }

    private void drawScrollBar(DrawContext context, int panelX, int panelY, int panelWidth, int panelHeight) {
        int scrollBarX = panelX + panelWidth - 18;
        int scrollBarY = panelY + 25;
        int scrollBarHeight = panelHeight - 30;
        context.fill(scrollBarX, scrollBarY, scrollBarX + 12, scrollBarY + scrollBarHeight, 0xFF444444);
        int maxScrollOffset = getMaxScrollOffset();
        if (maxScrollOffset > 0) {
            int thumbHeight = Math.max(20, (int)(scrollBarHeight * (1.0 / (maxScrollOffset + 1))));
            int thumbY = scrollBarY + (int)((scrollBarHeight - thumbHeight) * (chatScrollOffset / (double)maxScrollOffset));
            context.fill(scrollBarX, thumbY, scrollBarX + 12, thumbY + thumbHeight, 0xFF888888);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.getKeycode();
        if (keyCode == 256) {
            this.close();
            return true;
        }
        if (keyCode == 257 && this.inputField.isFocused() && !waitingForSendResponse && serverAvailable) {
            this.sendMessage();
            return true;
        }
        return super.keyPressed(input);
    }

    private void sendMessage() {
        String message = this.inputField.getText().trim();
        if (!message.isEmpty() && !waitingForSendResponse && serverAvailable) {
            userInputs.add(message);
            chatHistory.add("You: " + message);
            inputField.setText("");
            waitingForSendResponse = true;
            chatScrollOffset = getMaxScrollOffset();
            updateUIState();
            input(message);
        }
    }

    private void clearChat() {
        chatHistory.clear();
        userInputs.clear();
        chatScrollOffset = 0;
        if (serverAvailable) {
            chatHistory.add("AI: Chat cleared. How can I help you?");
        } else {
            chatHistory.add("System: Chat cleared. Server is still unavailable.");
        }
        updateUIState();
    }

    private void openSettings() {
        MinecraftClient.getInstance().setScreen(new EditorScreen());
    }

    private void loadPathAndSend() {
        String rawPath = this.pathField.getText().trim();
        if (rawPath.isEmpty()) {
            chatHistory.add("System: Path is empty.");
            return;
        }
        File base = MinecraftClient.getInstance().runDirectory;
        File target = new File(rawPath);
        if (!target.isAbsolute()) {
            target = new File(base, rawPath);
        }
        if (!target.exists()) {
            chatHistory.add("System: Path does not exist: " + target.getPath());
            return;
        }
        loadingFile = true;
        chatHistory.add("You (file): " + target.getPath());
        chatScrollOffset = getMaxScrollOffset();
        updateUIState();
        if (target.isFile()) {
            try {
                String content = readFileContent(target);
                String prompt = "User loaded configure file:" + target.getName() + "\n" + content;
                aiClient.sendChatRequest("tinyllama:latest", prompt).thenAccept(aiResponse -> {
                    String safe = unescapeJsonEscapes(aiResponse);
                    if (!safe.isEmpty()) {
                        chatHistory.add("AI: " + safe);
                        chatScrollOffset = getMaxScrollOffset();
                        LOGGER.info(safe);
                    }
                    loadingFile = false;
                    updateUIState();
                }).exceptionally(throwable -> {
                    chatHistory.add("System: Error getting AI response: " + throwable.getMessage());
                    loadingFile = false;
                    updateUIState();
                    return null;
                });
            } catch (IOException e) {
                chatHistory.add("System: Failed to read file: " + e.getMessage());
                loadingFile = false;
                updateUIState();
            }
        } else if (target.isDirectory()) {
            try {
                StringBuilder sb = new StringBuilder();
                File[] files = target.listFiles();
                if (files == null || files.length == 0) {
                    chatHistory.add("System: Directory is empty.");
                    loadingFile = false;
                    updateUIState();
                    return;
                }
                for (File f : files) {
                    if (f.isFile() && f.length() < 20000) {
                        sb.append("---FILE: ").append(f.getName()).append("---\n");
                        sb.append(readFileContent(f)).append("\n\n");
                    } else {
                        sb.append("---SKIP: ").append(f.getName()).append(" (too large or not a file)---\n");
                    }
                    if (sb.length() > 30000) {
                        sb.append("\n...TRUNCATED...\n");
                        break;
                    }
                }
                String prompt = "Please analyze the following directory listing and small file contents:\n" + sb.toString();
                aiClient.sendChatRequest("tinyllama:latest", prompt).thenAccept(aiResponse -> {
                    String safe = unescapeJsonEscapes(aiResponse);
                    if (!safe.isEmpty()) {
                        chatHistory.add("AI: " + safe);
                        chatScrollOffset = getMaxScrollOffset();
                        LOGGER.info(safe);
                    }
                    loadingFile = false;
                    updateUIState();
                }).exceptionally(throwable -> {
                    chatHistory.add("System: Error getting AI response: " + throwable.getMessage());
                    loadingFile = false;
                    updateUIState();
                    return null;
                });
            } catch (IOException e) {
                chatHistory.add("System: Failed to read directory: " + e.getMessage());
                loadingFile = false;
                updateUIState();
            }
        }
    }

    private String readFileContent(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String s = new String(bytes, StandardCharsets.UTF_8);
        if (s.length() > 10000) return s.substring(0, 10000) + "\n...TRUNCATED...";
        return s;
    }

    private String unescapeJsonEscapes(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    case '/': sb.append('/'); break;
                    case 'u':
                        if (i + 4 < s.length()) {
                            String hex = s.substring(i + 1, i + 5);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                                i += 4;
                            } catch (Exception e) {
                                sb.append('\\');
                                sb.append('u');
                            }
                        } else {
                            sb.append('\\');
                            sb.append('u');
                        }
                        break;
                    default: sb.append(n); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void input(String message) {
        aiClient.sendChatRequest("tinyllama:latest", message).thenAccept(aiResponse -> {
            String safe = unescapeJsonEscapes(aiResponse);
            if (!safe.isEmpty()) {
                chatHistory.add("AI: " + safe);
                chatScrollOffset = getMaxScrollOffset();
                LOGGER.info(safe);
            }
            waitingForSendResponse = false;
            updateUIState();
        }).exceptionally(throwable -> {
            chatHistory.add("System: Error getting AI response: " + throwable.getMessage());
            chatScrollOffset = getMaxScrollOffset();
            waitingForSendResponse = false;
            updateUIState();
            return null;
        });
    }

    @Override
    public void close() {
        super.close();
    }
}
