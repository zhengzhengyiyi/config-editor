package io.github.zhengzhengyiyi.util;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileLockUtil {
    
    public static FileLockResult tryLockFile(Path filePath) {
        return tryLockFile(filePath, 3, 100);
    }
    
    public static FileLockResult tryLockFile(Path filePath, int maxRetries, long retryInterval) {
        int attempt = 0;
        
        while (attempt <= maxRetries) {
            try {
                FileChannel channel = FileChannel.open(filePath, 
                    StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                
                FileLock lock = channel.tryLock();
                if (lock != null) {
                    return new FileLockResult(true, lock, channel, null);
                } else {
                    channel.close();
                }
                
            } catch (Exception e) {
                if (attempt >= maxRetries) {
                    return new FileLockResult(false, null, null, e);
                }
            }
            
            attempt++;
            if (attempt <= maxRetries) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return new FileLockResult(false, null, null, ie);
                }
            }
        }
        
        return new FileLockResult(false, null, null, new IOException("Max retries exceeded"));
    }
    
    public static void releaseLock(FileLockResult result) {
        if (result != null && result.lock != null) {
            try {
                result.lock.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (result != null && result.channel != null) {
            try {
                result.channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static class FileLockResult {
        public final boolean success;
        public final FileLock lock;
        public final FileChannel channel;
        public final Exception error;
        
        public FileLockResult(boolean success, FileLock lock, FileChannel channel, Exception error) {
            this.success = success;
            this.lock = lock;
            this.channel = channel;
            this.error = error;
        }
    }
}
