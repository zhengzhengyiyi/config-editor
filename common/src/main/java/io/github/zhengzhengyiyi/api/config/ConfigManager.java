package io.github.zhengzhengyiyi.api.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.MinecraftClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration manager that handles loading, saving, and monitoring configuration files.
 * Provides methods for accessing configuration data and automatically reloads
 * configuration when the file is modified externally.
 *
 * @param <T> the type of configuration data, must extend ConfigData
 */
public class ConfigManager<T extends ConfigData> {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    private final Class<T> configClass;
    private T config;
    private FileTime lastModifiedTime;
    private ScheduledExecutorService fileWatcher;
    private volatile boolean isReloading = false;
    private volatile boolean isWatcherRunning = false;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    
    /**
     * Constructs a new ConfigManager with the specified parameters.
     *
     * @param configPath the path to the configuration file
     * @param defaultConfig the default configuration instance
     * @param configClass the class object of the configuration type
     */
    public ConfigManager(Path configPath, T defaultConfig, Class<T> configClass) {
        this.configPath = configPath;
        this.config = defaultConfig;
        this.configClass = configClass;
        init();
    }
    
    /**
     * Initializes the configuration manager by loading existing configuration
     * or creating a new one with default values.
     */
    private void init() {
        load();
//        startFileWatcher();
    }
    
    /**
     * Loads configuration from file. Creates default configuration if file doesn't exist.
     */
    public void load() {
        if (isReloading) {
            return;
        }
        
        try {
            if (Files.exists(configPath)) {
                String jsonContent = Files.readString(configPath);
                T loadedConfig = gson.fromJson(jsonContent, configClass);
                if (loadedConfig != null) {
                    this.config = loadedConfig;
                }
                lastModifiedTime = Files.getLastModifiedTime(configPath);
//                LOGGER.info("Configuration loaded successfully from: {}", configPath);
            } else {
                save();
                LOGGER.info("Created new configuration file with default values: {}", configPath);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config from {}: {}", configPath, e.getMessage());
        }
    }
    
    /**
     * Saves current configuration to file.
     */
    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            String jsonContent = gson.toJson(config);
            Files.writeString(configPath, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            lastModifiedTime = Files.getLastModifiedTime(configPath);
            LOGGER.info("Configuration saved successfully to: {}", configPath);
        } catch (Exception e) {
            LOGGER.error("Failed to save config to {}: {}", configPath, e.getMessage());
        }
    }
    
    /**
     * Starts a background thread that monitors the config file for changes.
     * Automatically reloads the configuration if the file is modified externally.
     */
    private void startFileWatcher() {
        if (fileWatcher != null && !fileWatcher.isShutdown()) {
            fileWatcher.shutdown();
            try {
                if (!fileWatcher.awaitTermination(2, TimeUnit.SECONDS)) {
                    fileWatcher.shutdownNow();
                }
            } catch (InterruptedException e) {
                fileWatcher.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        fileWatcher = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Config-File-Watcher-" + configPath.getFileName());
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
        
        fileWatcher.scheduleAtFixedRate(() -> {
            try {
                if (!Files.exists(configPath)) {
                    LOGGER.debug("Config file does not exist: {}", configPath);
                    return;
                }
                
                FileTime currentModifiedTime = Files.getLastModifiedTime(configPath);
                boolean isModified = lastModifiedTime == null || 
                                   currentModifiedTime.toMillis() > lastModifiedTime.toMillis() + 1000;
                
                if (isModified && !isReloading) {
                    isReloading = true;
                    LOGGER.info("Detected config file modification, reloading...");
                    
                    if (MinecraftClient.getInstance() != null) {
                        MinecraftClient.getInstance().execute(() -> {
                            load();
                            isReloading = false;
                        });
                    } else {
                        load();
                        isReloading = false;
                    }
                }
                
            } catch (Exception e) {
                LOGGER.error("File watcher error for {}: {}", configPath, e.getMessage());
                isReloading = false;
            }
        }, 2, 2, TimeUnit.SECONDS);
        
        isWatcherRunning = true;
        LOGGER.info("File watcher started for: {}", configPath);
    }
    
    /**
     * Returns the current configuration instance.
     *
     * @return the current configuration instance
     */
    public T getConfig() {
    	load();
        return config;
    }
    
    /**
     * Updates the configuration with new values and saves to file.
     *
     * @param newConfig the new configuration instance
     */
    public void updateConfig(T newConfig) {
        this.config = newConfig;
        save();
    }
    
    /**
     * Checks if the file watcher is running.
     *
     * @return true if the file watcher is running
     */
    public boolean isWatcherRunning() {
        return isWatcherRunning && fileWatcher != null && !fileWatcher.isShutdown();
    }
    
    /**
     * Restarts the file watcher.
     */
    public void restartFileWatcher() {
        LOGGER.info("Restarting file watcher for: {}", configPath);
        startFileWatcher();
    }
    
    /**
     * Shuts down the file watcher thread. Should be called when the application exits.
     */
    public void shutdown() {
        isWatcherRunning = false;
        if (fileWatcher != null) {
            LOGGER.info("Shutting down file watcher for: {}", configPath);
            fileWatcher.shutdown();
            try {
                if (!fileWatcher.awaitTermination(3, TimeUnit.SECONDS)) {
                    fileWatcher.shutdownNow();
                    LOGGER.warn("File watcher had to be forcefully shutdown for: {}", configPath);
                } else {
                    LOGGER.info("File watcher shutdown successfully for: {}", configPath);
                }
            } catch (InterruptedException e) {
                fileWatcher.shutdownNow();
                Thread.currentThread().interrupt();
                LOGGER.warn("File watcher shutdown interrupted for: {}", configPath);
            }
        }
    }
}
