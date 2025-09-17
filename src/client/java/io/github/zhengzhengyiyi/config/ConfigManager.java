package io.github.zhengzhengyiyi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration manager that handles loading, saving, and monitoring configuration files.
 * Provides static methods for accessing configuration data and automatically reloads
 * configuration when the file is modified externally.
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "editor_config.json");
    private static ConfigData config = new ConfigData();
    private static FileTime lastModifiedTime;
    private static ScheduledExecutorService fileWatcher;
    private static Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    
    /**
     * Static initialization block to load configuration when the class is loaded.
     */
    static {
        load();
    }
    
    /**
     * The init method
     */
    public static void init() {
    	
    }
    
    /**
     * Loads configuration from file. Creates default configuration if file doesn't exist.
     */
    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String jsonContent = new String(Files.readAllBytes(CONFIG_PATH));
                config = GSON.fromJson(jsonContent, ConfigData.class);
                lastModifiedTime = Files.getLastModifiedTime(CONFIG_PATH);
            } else {
                save();
            }
        } catch (Exception e) {
        	LOGGER.error("Failed to load config: " + e.getMessage());
            config = new ConfigData();
        }
        
        startFileWatcher();
    }
    
    /**
     * Saves current configuration to file.
     */
    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String jsonContent = GSON.toJson(config);
            Files.write(CONFIG_PATH, jsonContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            lastModifiedTime = Files.getLastModifiedTime(CONFIG_PATH);
        } catch (Exception e) {
        	LOGGER.error("Failed to save config: " + e.getMessage());
        }
    }
    
    /**
     * Starts a background thread that monitors the config file for changes.
     * Automatically reloads the configuration if the file is modified externally.
     */
    private static void startFileWatcher() {
        if (fileWatcher != null) {
            fileWatcher.shutdown();
        }
        
        fileWatcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Config File Watcher");
            thread.setDaemon(true);
            return thread;
        });
        
        fileWatcher.scheduleAtFixedRate(() -> {
            try {
                if (Files.exists(CONFIG_PATH)) {
                    FileTime currentModifiedTime = Files.getLastModifiedTime(CONFIG_PATH);
                    if (!currentModifiedTime.equals(lastModifiedTime)) {
                        MinecraftClient.getInstance().execute(() -> {
                        	new Thread(() -> {
                        		load();
                        		LOGGER.info("Configuration reloaded due to external changes");
                        	}).start();
                        });
                    }
                }
            } catch (Exception e) {
            	LOGGER.error("File watcher error: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Returns the current configuration instance.
     * @return ConfigData instance containing all configuration values
     */
    public static ConfigData getConfig() {
        return config;
    }
    
    /**
     * Shuts down the file watcher thread. Should be called when the application exits.
     */
    public static void shutdown() {
        if (fileWatcher != null) {
            fileWatcher.shutdown();
            try {
                if (!fileWatcher.awaitTermination(1, TimeUnit.SECONDS)) {
                    fileWatcher.shutdownNow();
                }
            } catch (InterruptedException e) {
                fileWatcher.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
