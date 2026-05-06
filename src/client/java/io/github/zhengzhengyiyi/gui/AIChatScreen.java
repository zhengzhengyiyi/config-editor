package io.github.zhengzhengyiyi.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
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
    private EditBox inputField;
    private EditBox pathField;
    private Button sendButton;
    private Button loadButton;
    private Button clearButton;
    private Button settingsButton;
    private List<String> chatHistory;
    private List<String> userInputs;
    private boolean waitingForSendResponse = false;
    private boolean loadingFile = false;
    private boolean serverAvailable = false;
    private boolean checkingServer = true;
    private int chatScrollOffset = 0;
    private Button scrollUpButton;
    private Button scrollDownButton;
    private static Logger LOGGER = LoggerFactory.getLogger(AIChatScreen.class);
    private AiClient aiClient = new AiClient();

    public AIChatScreen() {
        super(Component.literal("AI Chat"));
        this.chatHistory = new ArrayList<>();
        this.userInputs = new ArrayList<>();
        chatHistory.add("AI: Hello! How can I help you today?");
        chatHistory.add("AI: I'm ready to assist you with anything!");
    }

    @SuppressWarnings("null")
    @Override
    protected void init() {
        super.init();
        checkServerAvailability();
        int panelWidth = Math.min(500, this.width - 40);
        int panelHeight = Math.min(320, this.height - 100);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 20;
        int inputY = panelY + panelHeight + 8;
        int inputWidth = panelWidth - 55;
        this.inputField = new EditBox(this.font, panelX, inputY, inputWidth, 20, Component.literal("Type your message…"));
        this.inputField.setMaxLength(1024);
        this.inputField.setEditable(false);
        this.addRenderableWidget(this.inputField);
        this.sendButton = Button.builder(Component.literal("Send ▶"), button -> sendMessage())
            .bounds(panelX + inputWidth + 5, inputY, 50, 20)
            .build();
        this.sendButton.active = false;
        this.addRenderableWidget(this.sendButton);
        int pathY = inputY + 26;
        this.pathField = new EditBox(this.font, panelX, pathY, inputWidth - 50, 20, Component.literal("File/folder path…"));
        this.pathField.setMaxLength(1024);
        this.addRenderableWidget(this.pathField);
        this.loadButton = Button.builder(Component.literal("Load"), button -> loadPathAndSend())
            .bounds(panelX + inputWidth - 40, pathY, 45, 20)
            .build();
        this.addRenderableWidget(this.loadButton);
        this.clearButton = Button.builder(Component.literal("Clear"), button -> clearChat())
            .bounds(panelX + panelWidth - 95, panelY - 24, 45, 18)
            .build();
        this.addRenderableWidget(this.clearButton);
        this.settingsButton = Button.builder(Component.literal("⚙ Editor"), button -> openSettings())
            .bounds(panelX + panelWidth - 48, panelY - 24, 48, 18)
            .build();
        this.addRenderableWidget(this.settingsButton);
        this.scrollUpButton = Button.builder(Component.literal("↑"), button -> scrollUp())
            .bounds(panelX + panelWidth - 15, panelY + 23, 12, 14)
            .build();
        this.addRenderableWidget(this.scrollUpButton);
        this.scrollDownButton = Button.builder(Component.literal("↓"), button -> scrollDown())
            .bounds(panelX + panelWidth - 15, panelY + panelHeight - 18, 12, 14)
            .build();
        this.addRenderableWidget(this.scrollDownButton);
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
            this.sendButton.active = serverAvailable && !waitingForSendResponse && (this.inputField != null && !this.inputField.getValue().trim().isEmpty());
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
        int panelWidth = Math.min(500, this.width - 40);
        int panelHeight = Math.min(320, this.height - 100);
        int maxVisibleLines = (panelHeight - 55) / 12;
        int totalLines = getTotalChatLines(panelWidth - 32);
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

    @SuppressWarnings("null")
    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (this.font.width(testLine) <= maxWidth) {
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
        int panelWidth = Math.min(500, this.width - 40);
        int panelHeight = Math.min(320, this.height - 100);
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
    public void extractRenderState(@SuppressWarnings("null") GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int panelWidth = Math.min(500, this.width - 40);
        int panelHeight = Math.min(320, this.height - 100);
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2 - 20;

        // ── Outer glow / border ───────────────────────────────────────────────
        context.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + panelHeight + 2, 0xFF45475A);
        // ── Panel background ──────────────────────────────────────────────────
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF1E1E2E);
        // ── Title bar ─────────────────────────────────────────────────────────
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 22, 0xFF181825);
        context.fill(panelX, panelY + 21, panelX + panelWidth, panelY + 22, 0xFF89B4FA);

        // Title text
        String title = "✦ AI Chat Assistant";
        if (checkingServer) {
            title += "  §7(checking…)";
        } else if (!serverAvailable) {
            title += "  §c(offline)";
        } else {
            title += "  §a(online)";
        }
        context.text(this.font, Component.literal(title), panelX + 8, panelY + 7, 0xFFCDD6F4, false);

        // ── Separator below title ─────────────────────────────────────────────
        // (already drawn as the accent line above)

        // ── Scroll bar track ──────────────────────────────────────────────────
        drawScrollBar(context, panelX, panelY, panelWidth, panelHeight);

        updateUIState();
        super.extractRenderState(context, mouseX, mouseY, delta);

        // ── Chat messages ─────────────────────────────────────────────────────
        int chatY = panelY + 28;
        int maxVisibleLines = (panelHeight - 55) / 12;
        int textAreaWidth = panelWidth - 32;

        context.enableScissor(panelX + 5, panelY + 23, panelX + panelWidth - 18, panelY + panelHeight - 5);
        int currentLine = 0;
        int visibleLineCount = 0;
        for (String message : chatHistory) {
            List<String> wrappedLines = wrapText(message, textAreaWidth);
            for (String line : wrappedLines) {
                if (currentLine >= chatScrollOffset && visibleLineCount < maxVisibleLines) {
                    int renderY = chatY + visibleLineCount * 12;
                    int color = getMessageColor(message);
                    // Draw a subtle left-edge accent for AI messages
                    if (message.startsWith("AI:") && line.equals(wrappedLines.get(0))) {
                        context.fill(panelX + 5, renderY - 1, panelX + 7, renderY + 10, 0xFF89B4FA);
                    } else if (message.startsWith("You:") && line.equals(wrappedLines.get(0))) {
                        context.fill(panelX + 5, renderY - 1, panelX + 7, renderY + 10, 0xFFA6E3A1);
                    } else if (message.startsWith("System:") && line.equals(wrappedLines.get(0))) {
                        context.fill(panelX + 5, renderY - 1, panelX + 7, renderY + 10, 0xFFF9E2AF);
                    }
                    context.text(this.font, line, panelX + 10, renderY, color, false);
                    visibleLineCount++;
                }
                currentLine++;
            }
        }
        if ((checkingServer || waitingForSendResponse || loadingFile) && visibleLineCount < maxVisibleLines) {
            int renderY = chatY + visibleLineCount * 12;
            String loadingText = checkingServer ? "§7Checking server…"
                    : (waitingForSendResponse ? "§7AI is thinking…" : "§7Loading files…");
            context.text(this.font, Component.literal(loadingText), panelX + 10, renderY, 0xFFAAAAAA, false);
        }
        context.disableScissor();
    }

    private int getMessageColor(String message) {
        if (message.startsWith("You:"))    return 0xFFA6E3A1; // green  — user
        if (message.startsWith("AI:"))     return 0xFFCDD6F4; // light  — AI
        if (message.startsWith("System:")) return 0xFFF9E2AF; // yellow — system
        return 0xFFBAC2DE;                                     // muted  — other
    }

    private void drawScrollBar(GuiGraphicsExtractor context, int panelX, int panelY, int panelWidth, int panelHeight) {
        int scrollBarX = panelX + panelWidth - 16;
        int scrollBarY = panelY + 23;
        int scrollBarHeight = panelHeight - 28;
        // Track
        context.fill(scrollBarX, scrollBarY, scrollBarX + 10, scrollBarY + scrollBarHeight, 0xFF313244);
        int maxScrollOffset = getMaxScrollOffset();
        if (maxScrollOffset > 0) {
            int thumbHeight = Math.max(16, (int)(scrollBarHeight * (1.0 / (maxScrollOffset + 1))));
            int thumbY = scrollBarY + (int)((scrollBarHeight - thumbHeight) * (chatScrollOffset / (double)maxScrollOffset));
            // Thumb
            context.fill(scrollBarX + 1, thumbY, scrollBarX + 9, thumbY + thumbHeight, 0xFF89B4FA);
        }
    }

    @Override
    public boolean keyPressed(@SuppressWarnings("null") KeyEvent input) {
        int keyCode = input.key();

        // Always pass Ctrl+A/C/V to the focused widget so clipboard works in text fields
        if (input.hasControlDown()) {
            if (keyCode == GLFW.GLFW_KEY_A || keyCode == GLFW.GLFW_KEY_C || keyCode == GLFW.GLFW_KEY_V) {
                var focused = getFocused();
                if (focused != null && focused.keyPressed(input)) {
                    return true;
                }
            }
        }

        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        if (keyCode == 257 && this.inputField.isFocused() && !waitingForSendResponse && serverAvailable) {
            this.sendMessage();
            return true;
        }
        return super.keyPressed(input);
    }

    private void sendMessage() {
        String message = this.inputField.getValue().trim();
        if (!message.isEmpty() && !waitingForSendResponse && serverAvailable) {
            userInputs.add(message);
            chatHistory.add("You: " + message);
            inputField.setValue("");
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
        Minecraft.getInstance().setScreen(new EditorScreen());
    }

    private void loadPathAndSend() {
        String rawPath = this.pathField.getValue().trim();
        if (rawPath.isEmpty()) {
            chatHistory.add("System: Path is empty.");
            return;
        }
        File base = Minecraft.getInstance().gameDirectory;
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
    public void onClose() {
        super.onClose();
    }
}
