package com.gm.warning.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;

public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final String CONFIG_FILE = "config.properties";

    private final Properties properties = new Properties();

    private String hrmUrl;
    private String attendanceApiPath;
    private String holidaysApiPath;
    private String notificationsApiPath;
    private String leaveBalanceApiPath;
    private String leaveRequestsApiPath;
    private String overtimeApiPath;
    private String microsoftUsername;
    private String microsoftPassword;
    private String browserChannel;
    private boolean browserHeadless;
    private String browserUserDataDir;

    private boolean telegramEnabled;
    private String telegramBotToken;
    private String telegramChatId;

    private LocalTime earliestStart;
    private LocalTime latestStart;
    private int requiredSpanHours;

    private List<LocalTime> morningReminders = new ArrayList<>();
    private LocalTime noonReminder = LocalTime.of(12, 0);
    private List<LocalTime> eveningReminders = new ArrayList<>();
    private int checkIntervalMinutes;
    private int logRetentionDays;

    public AppConfig() {
        loadConfig();
    }

    public void loadConfig() {
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                log.info("Loaded config from file: {}", path.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to read {}: {}", CONFIG_FILE, e.getMessage());
            }
        } else {
            // Try loading from classpath
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (in != null) {
                    properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    log.info("Loaded default config from classpath resource");
                }
            } catch (IOException e) {
                log.error("Failed to load config from classpath: {}", e.getMessage());
            }
        }

        // Tự động nạp đè cấu hình cá nhân nếu có file config.properties.local (không lưu trên Git)
        Path localPath = Paths.get("config.properties.local");
        if (Files.exists(localPath)) {
            try (InputStream in = Files.newInputStream(localPath)) {
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                log.info("Loaded local private config overrides from: {}", localPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to read {}: {}", localPath, e.getMessage());
            }
        }

        hrmUrl = getProperty("hrm.url", "http://hrm.gm-group.vn");
        attendanceApiPath = getProperty("hrm.api.attendance.path", "/api/attendance/today");
        holidaysApiPath = getProperty("hrm.api.holidays.path", "/api/holidays");
        notificationsApiPath = getProperty("hrm.api.notifications.path", "/api/notifications/mine");
        leaveBalanceApiPath = getProperty("hrm.api.leave.balance.path", "/api/leave/balance");
        leaveRequestsApiPath = getProperty("hrm.api.leave.requests.path", "/api/leave/requests/mine");
        overtimeApiPath = getProperty("hrm.api.overtime.path", "/api/overtime/requests/mine");

        microsoftUsername = getProperty("microsoft.username", "phuongnh@gm-group.vn");
        microsoftPassword = getProperty("microsoft.password", "");

        browserChannel = getProperty("browser.channel", "msedge");
        browserHeadless = Boolean.parseBoolean(getProperty("browser.headless", "true"));
        browserUserDataDir = getProperty("browser.user.data.dir", "./playwright_profile");

        telegramEnabled = Boolean.parseBoolean(getProperty("telegram.enabled", "true"));
        telegramBotToken = getProperty("telegram.bot.token", "");
        telegramChatId = getProperty("telegram.chat.id", "");

        earliestStart = LocalTime.parse(getProperty("work.flexible.earliest.start", "08:30"));
        latestStart = LocalTime.parse(getProperty("work.flexible.latest.start", "09:00"));
        requiredSpanHours = Integer.parseInt(getProperty("work.required.span.hours", "9"));

        morningReminders = parseTimeList(getProperty("reminders.morning", "08:30,08:45,08:55"));
        String noonStr = getProperty("reminders.noon", "12:00");
        try {
            noonReminder = LocalTime.parse(noonStr.trim());
        } catch (Exception e) {
            log.warn("Invalid time format for reminders.noon: {}, fallback to 12:00", noonStr);
            noonReminder = LocalTime.of(12, 0);
        }
        eveningReminders = parseTimeList(getProperty("reminders.evening", "17:35,18:05,18:30,19:00,20:00"));

        checkIntervalMinutes = Integer.parseInt(getProperty("check.interval.minutes", "1"));
        logRetentionDays = Integer.parseInt(getProperty("log.retention.days", "30"));
    }

    private List<LocalTime> parseTimeList(String raw) {
        List<LocalTime> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) return list;
        for (String item : raw.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                try {
                    list.add(LocalTime.parse(trimmed));
                } catch (Exception e) {
                    log.warn("Invalid time format in config: {}", trimmed);
                }
            }
        }
        return list;
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public void saveConfig() {
        Path path = Paths.get(CONFIG_FILE);
        try (OutputStream out = Files.newOutputStream(path)) {
            properties.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "HRM Attendance Warning Tool Config");
            log.info("Saved updated config to: {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save config: {}", e.getMessage());
        }
    }

    // Getters
    public String getHrmUrl() { return hrmUrl; }
    public String getAttendanceApiPath() { return attendanceApiPath; }
    public String getHolidaysApiPath() { return holidaysApiPath; }
    public String getNotificationsApiPath() { return notificationsApiPath; }
    public String getLeaveBalanceApiPath() { return leaveBalanceApiPath; }
    public String getLeaveRequestsApiPath() { return leaveRequestsApiPath; }
    public String getOvertimeApiPath() { return overtimeApiPath; }
    public String getMicrosoftUsername() { return microsoftUsername; }
    public String getMicrosoftPassword() { return microsoftPassword; }
    public String getBrowserChannel() { return browserChannel; }
    public boolean isBrowserHeadless() { return browserHeadless; }
    public String getBrowserUserDataDir() { return browserUserDataDir; }
    public boolean isTelegramEnabled() { return telegramEnabled; }
    public String getTelegramBotToken() { return telegramBotToken; }
    public String getTelegramChatId() { return telegramChatId; }
    public LocalTime getEarliestStart() { return earliestStart; }
    public LocalTime getLatestStart() { return latestStart; }
    public int getRequiredSpanHours() { return requiredSpanHours; }
    public List<LocalTime> getMorningReminders() { return morningReminders; }
    public LocalTime getNoonReminder() { return noonReminder != null ? noonReminder : LocalTime.of(12, 0); }
    public void setNoonReminder(LocalTime noonReminder) { this.noonReminder = noonReminder; }
    public List<LocalTime> getEveningReminders() { return eveningReminders; }
    public int getCheckIntervalMinutes() { return checkIntervalMinutes; }
    public int getLogRetentionDays() { return logRetentionDays; }

    public void setTelegramBotToken(String token) {
        this.telegramBotToken = token;
        setProperty("telegram.bot.token", token);
    }

    public void setTelegramChatId(String chatId) {
        this.telegramChatId = chatId;
        setProperty("telegram.chat.id", chatId);
    }
}
