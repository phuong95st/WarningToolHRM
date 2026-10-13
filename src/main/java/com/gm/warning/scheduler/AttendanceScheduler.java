package com.gm.warning.scheduler;

import com.gm.warning.config.AppConfig;
import com.gm.warning.ui.DesktopTrayApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AttendanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(AttendanceScheduler.class);

    private final AppConfig config;
    private final DesktopTrayApp trayApp;
    private final ScheduledExecutorService scheduler;

    public AttendanceScheduler(AppConfig config, DesktopTrayApp trayApp) {
        this.config = config;
        this.trayApp = trayApp;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "hrm-scheduler-thread");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        int intervalMinutes = Math.max(1, config.getCheckIntervalMinutes());
        log.info("Starting AttendanceScheduler with check interval of {} minutes...", intervalMinutes);

        // Run immediately after 5 seconds, then periodically
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        trayApp.checkAttendanceNow(false);
                    } catch (Exception e) {
                        log.error("Unhandled exception in scheduled attendance check: {}", e.getMessage(), e);
                    }
                },
                5,
                intervalMinutes * 60L,
                TimeUnit.SECONDS
        );

        // Run log cleanup once every 6 hours
        scheduler.scheduleAtFixedRate(
                () -> {
                    try {
                        LogRetentionCleaner.cleanOldLogs("logs", config.getLogRetentionDays());
                    } catch (Exception e) {
                        log.error("Unhandled exception in log retention cleaner: {}", e.getMessage(), e);
                    }
                },
                10,
                6 * 3600L,
                TimeUnit.SECONDS
        );
    }

    public void stop() {
        log.info("Stopping AttendanceScheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
