package com.gm.warning.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class LogRetentionCleaner {
    private static final Logger log = LoggerFactory.getLogger(LogRetentionCleaner.class);

    /**
     * Cleans up log files in the specified directory that are older than the specified retention days.
     */
    public static void cleanOldLogs(String logDirPath, int retentionDays) {
        try {
            File dir = new File(logDirPath);
            if (!dir.exists() || !dir.isDirectory()) {
                return;
            }

            Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
            long cutoffMillis = cutoff.toEpochMilli();

            File[] files = dir.listFiles((d, name) -> name.endsWith(".log") || name.endsWith(".gz"));
            if (files == null) return;

            int deletedCount = 0;
            for (File file : files) {
                if (file.lastModified() < cutoffMillis) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        deletedCount++;
                        log.info("Deleted expired log file: {} (older than {} days)", file.getName(), retentionDays);
                    } else {
                        log.warn("Failed to delete expired log file: {}", file.getName());
                    }
                }
            }
            if (deletedCount > 0) {
                log.info("Log cleanup completed: Deleted {} expired log files.", deletedCount);
            }
        } catch (Exception e) {
            log.error("Error during log cleanup: {}", e.getMessage());
        }
    }
}
