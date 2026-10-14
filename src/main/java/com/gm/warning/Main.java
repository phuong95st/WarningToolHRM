package com.gm.warning;

import com.gm.warning.client.HrmApiClient;
import com.gm.warning.client.SsoLoginService;
import com.gm.warning.client.TokenCache;
import com.gm.warning.config.AppConfig;
import com.gm.warning.engine.AttendanceRuleEngine;
import com.gm.warning.model.AttendanceRecord;
import com.gm.warning.model.AttendanceResponse;
import com.gm.warning.model.HolidayItem;
import com.gm.warning.notification.DesktopToastNotifier;
import com.gm.warning.notification.NotificationManager;
import com.gm.warning.notification.TelegramNotifier;
import com.gm.warning.scheduler.AttendanceScheduler;
import com.gm.warning.scheduler.LogRetentionCleaner;
import com.gm.warning.ui.DesktopTrayApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("==================================================");
        log.info("   Starting HRM Attendance Warning Tool v1.0      ");
        log.info("==================================================");

        try {
            // Set Windows Native Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        // 1. Initialize Configuration
        AppConfig config = new AppConfig();

        // 2. Initialize Token Cache
        TokenCache tokenCache = new TokenCache();

        // 3. Initialize SSO Login Service (Playwright)
        SsoLoginService ssoLoginService = new SsoLoginService(config, tokenCache);

        // 4. Initialize HRM API Client
        HrmApiClient hrmApiClient = new HrmApiClient(config, tokenCache, ssoLoginService);

        // 5. Initialize Notifiers
        TelegramNotifier telegramNotifier = new TelegramNotifier(config);
        DesktopToastNotifier desktopToastNotifier = new DesktopToastNotifier();
        NotificationManager notificationManager = new NotificationManager(telegramNotifier, desktopToastNotifier);

        // 5.1 Initialize Telegram Bot Command Service (lắng nghe lệnh tra cứu)
        com.gm.warning.notification.TelegramBotService telegramBotService =
                new com.gm.warning.notification.TelegramBotService(config, hrmApiClient, telegramNotifier);
        telegramBotService.start();

        // 6. Initialize Notification Monitor (Đơn nghỉ phép, remote, OT)
        com.gm.warning.notification.NotificationMonitorService notificationMonitorService =
                new com.gm.warning.notification.NotificationMonitorService(hrmApiClient, telegramNotifier, desktopToastNotifier);

        // 7. Initialize Rule Engine
        AttendanceRuleEngine ruleEngine = new AttendanceRuleEngine(config);

        // 8. Initial Log Retention Clean (remove logs > 30 days)
        LogRetentionCleaner.cleanOldLogs("logs", config.getLogRetentionDays());

        // 9. Initialize Desktop Tray App
        DesktopTrayApp trayApp = new DesktopTrayApp(
                config,
                hrmApiClient,
                ssoLoginService,
                ruleEngine,
                notificationManager,
                telegramNotifier,
                desktopToastNotifier,
                notificationMonitorService,
                telegramBotService
        );

        SwingUtilities.invokeLater(trayApp::initTray);

        // 9. Register Shutdown Hook for leaving/shutdown detection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("System shutdown detected. Checking if user forgot to check-out...");
            try {
                LocalDate today = LocalDate.now();
                boolean isWeekend = (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY);
                if (isWeekend) return;

                HolidayItem holiday = hrmApiClient.getHolidayForDate(today);
                if (holiday != null) return;

                AttendanceResponse response = hrmApiClient.fetchCurrentMonthAttendance();
                if (response != null && response.isSuccess() && response.getData() != null) {
                    String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    AttendanceRecord todayRecord = response.getData().stream()
                            .filter(r -> todayStr.equals(r.getWorkDate()))
                            .findFirst()
                            .orElse(null);

                    if (todayRecord != null && todayRecord.isCheckedIn()) {
                        if (!todayRecord.isCheckedOut()) {
                            log.warn("SHUTDOWN ALERT: User is shutting down PC but check_out is null!");
                            telegramNotifier.sendMessage(
                                    com.gm.warning.notification.TelegramTemplate.shutdownAlert(todayRecord.getCheckIn())
                            );
                        } else if (!todayRecord.isFullWork()) {
                            LocalTime checkInTime = todayRecord.getCheckInTime();
                            LocalTime earliestOut = checkInTime != null
                                    ? checkInTime.plusHours(config.getRequiredSpanHours())
                                    : config.getEarliestStart().plusHours(config.getRequiredSpanHours());
                            String earliestOutStr = earliestOut.format(DateTimeFormatter.ofPattern("HH:mm"));
                            log.warn("SHUTDOWN ALERT: User is shutting down PC but check_out is not full work ({})!", todayRecord.getStatus());
                            telegramNotifier.sendMessage(
                                    com.gm.warning.notification.TelegramTemplate.shutdownIncompleteAlert(
                                            todayRecord.getCheckIn(),
                                            todayRecord.getCheckOut(),
                                            earliestOutStr,
                                            todayRecord.getStatus()
                                    )
                            );
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not execute shutdown check: {}", e.getMessage());
            }
        }, "hrm-shutdown-hook"));

        // 10. Start Scheduler
        AttendanceScheduler scheduler = new AttendanceScheduler(config, trayApp);
        scheduler.start();

        log.info("HRM Attendance Warning Tool is up and running in background!");
    }
}
