package io.github.zhengzhengyiyi.util;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for creating backups of configuration files.
 * Provides functionality to create compressed backups of config directories.
 */
public class BackupHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupHelper.class);
    private static final String BACKUP_DIR_NAME = "backups";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Creates a backup of the specified configuration file.
     * The backup is stored in a backups subdirectory with a timestamp.
     *
     * @param configFile the configuration file to backup
     * @return the path to the created backup file, or null if backup failed
     */
    public static Path backupConfigFile(Path configFile) {
        try {
            Path backupDir = getBackupDirectory(configFile.getParent());
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String backupFileName = configFile.getFileName() + "." + timestamp + ".bak";
            Path backupFile = backupDir.resolve(backupFileName);
            
            Files.copy(configFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Backup created: {}", backupFile);
            return backupFile;
        } catch (IOException e) {
            LOGGER.error("Failed to create backup for file: {}", configFile, e);
            return null;
        }
    }

    /**
     * Creates a compressed backup of the entire config directory.
     * The backup is stored as a ZIP file in the backups subdirectory.
     *
     * @return the path to the created ZIP backup file, or null if backup failed
     */
    public static Path backupEntireConfigDirectory() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return backupDirectoryToZip(configDir, "config_backup");
    }

    /**
     * Creates a compressed backup of a specific directory.
     *
     * @param directory the directory to backup
     * @param backupName the base name for the backup file
     * @return the path to the created ZIP file, or null if backup failed
     */
    public static Path backupDirectoryToZip(Path directory, String backupName) {
        try {
            Path backupDir = getBackupDirectory(directory);
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String zipFileName = backupName + "_" + timestamp + ".zip";
            Path zipFile = backupDir.resolve(zipFileName);
            
            createZipFile(directory, zipFile);
            LOGGER.info("Directory backup created: {}", zipFile);
            return zipFile;
        } catch (IOException e) {
            LOGGER.error("Failed to create directory backup: {}", directory, e);
            return null;
        }
    }

    /**
     * Ensures the backup directory exists and returns its path.
     *
     * @param parentDir the parent directory where backups should be stored
     * @return the path to the backup directory
     * @throws IOException if the directory cannot be created
     */
    private static Path getBackupDirectory(Path parentDir) throws IOException {
        Path backupDir = parentDir.resolve(BACKUP_DIR_NAME);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }
        return backupDir;
    }

    /**
     * Creates a ZIP file containing all files from the source directory.
     *
     * @param sourceDir the directory to compress
     * @param zipFile the target ZIP file path
     * @throws IOException if an I/O error occurs during ZIP creation
     */
    private static void createZipFile(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isSameFile(file, zipFile)) {
                        addToZipFile(file, sourceDir.relativize(file), zos);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!Files.isSameFile(dir, sourceDir)) {
                        ZipEntry entry = new ZipEntry(sourceDir.relativize(dir) + "/");
                        zos.putNextEntry(entry);
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * Adds a single file to the ZIP output stream.
     *
     * @param file the file to add to the ZIP
     * @param relativePath the relative path of the file within the ZIP
     * @param zos the ZIP output stream
     * @throws IOException if an I/O error occurs
     */
    private static void addToZipFile(Path file, Path relativePath, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(relativePath.toString());
        zos.putNextEntry(zipEntry);
        Files.copy(file, zos);
        zos.closeEntry();
    }

    /**
     * Gets the path to the backups directory for the main config directory.
     *
     * @return the path to the config backups directory
     */
    public static Path getConfigBackupDirectory() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve(BACKUP_DIR_NAME);
    }

    /**
     * Lists all available backups in the backup directory.
     *
     * @return an array of backup files, sorted by modification time (newest first)
     * @throws IOException if the backup directory cannot be read
     */
    public static Path[] listBackups() throws IOException {
        Path backupDir = getConfigBackupDirectory();
        if (!Files.exists(backupDir)) {
            return new Path[0];
        }
        
        return Files.list(backupDir)
            .sorted((p1, p2) -> {
                try {
                    return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                } catch (IOException e) {
                    return 0;
                }
            })
            .toArray(Path[]::new);
    }

    /**
     * Deletes old backups beyond the specified maximum count.
     *
     * @param maxBackups the maximum number of backups to keep
     * @return the number of backups deleted
     * @throws IOException if an error occurs during deletion
     */
    public static int cleanupOldBackups(int maxBackups) throws IOException {
        Path[] backups = listBackups();
        int deletedCount = 0;
        
        for (int i = maxBackups; i < backups.length; i++) {
            Files.delete(backups[i]);
            deletedCount++;
            LOGGER.info("Deleted old backup: {}", backups[i].getFileName());
        }
        
        return deletedCount;
    }
}
