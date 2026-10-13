package com.gm.warning.ui;

import com.gm.warning.client.HrmApiClient;
import com.gm.warning.client.SsoLoginService;
import com.gm.warning.config.AppConfig;
import com.gm.warning.engine.AttendanceRuleEngine;
import com.gm.warning.model.AttendanceRecord;
import com.gm.warning.model.AttendanceResponse;
import com.gm.warning.model.HolidayItem;
import com.gm.warning.notification.DesktopToastNotifier;
import com.gm.warning.notification.NotificationManager;
import com.gm.warning.notification.TelegramNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DesktopTrayApp {
    private static final Logger log = LoggerFactory.getLogger(DesktopTrayApp.class);

    private final AppConfig config;
    private final HrmApiClient hrmApiClient;
    private final SsoLoginService ssoLoginService;
    private final AttendanceRuleEngine ruleEngine;
    private final NotificationManager notificationManager;
    private final TelegramNotifier telegramNotifier;
    private final DesktopToastNotifier desktopNotifier;
    private final com.gm.warning.notification.NotificationMonitorService notificationMonitorService;

    private TrayIcon trayIcon;
    private MenuItem statusMenuItem;

    public DesktopTrayApp(AppConfig config,
                          HrmApiClient hrmApiClient,
                          SsoLoginService ssoLoginService,
                          AttendanceRuleEngine ruleEngine,
                          NotificationManager notificationManager,
                          TelegramNotifier telegramNotifier,
                          DesktopToastNotifier desktopNotifier,
                          com.gm.warning.notification.NotificationMonitorService notificationMonitorService) {
        this.config = config;
        this.hrmApiClient = hrmApiClient;
        this.ssoLoginService = ssoLoginService;
        this.ruleEngine = ruleEngine;
        this.notificationManager = notificationManager;
        this.telegramNotifier = telegramNotifier;
        this.desktopNotifier = desktopNotifier;
        this.notificationMonitorService = notificationMonitorService;
    }

    public void initTray() {
        if (!SystemTray.isSupported()) {
            log.warn("SystemTray is not supported on this environment. Running in headless mode.");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = createTrayIconImage();

            PopupMenu popup = new PopupMenu();

            MenuItem titleItem = new MenuItem("HRM Warning Tool v1.0");
            titleItem.setEnabled(false);
            popup.add(titleItem);
            popup.addSeparator();

            statusMenuItem = new MenuItem("Trạng thái: Đang kiểm tra...");
            statusMenuItem.setEnabled(false);
            popup.add(statusMenuItem);
            popup.addSeparator();

            MenuItem checkNowItem = new MenuItem("Kiểm tra ngay");
            checkNowItem.addActionListener(e -> checkAttendanceNow(true));
            popup.add(checkNowItem);

            MenuItem reloginItem = new MenuItem("Đăng nhập lại SSO");
            reloginItem.addActionListener(e -> {
                desktopNotifier.showInfo("SSO Login", "Đang tiến hành đăng nhập ngầm qua Playwright...");
                new Thread(() -> {
                    String token = ssoLoginService.performSsoLogin();
                    if (token != null) {
                        desktopNotifier.showInfo("SSO Login", "Đăng nhập thành công! Đã lấy token mới.");
                    } else {
                        desktopNotifier.showError("SSO Login", "Đăng nhập thất bại. Kiểm tra logs để biết chi tiết.");
                    }
                }).start();
            });
            popup.add(reloginItem);

            MenuItem testTelegramItem = new MenuItem("Thử gửi tin nhắn Telegram");
            testTelegramItem.addActionListener(e -> {
                boolean ok = telegramNotifier.testConnection();
                if (ok) {
                    desktopNotifier.showInfo("Telegram", "Gửi tin nhắn test thành công!");
                } else {
                    desktopNotifier.showError("Telegram", "Gửi thất bại! Hãy kiểm tra bot token và chat ID trong config.properties");
                }
            });
            popup.add(testTelegramItem);

            MenuItem openHrmItem = new MenuItem("Mở trang web HRM");
            openHrmItem.addActionListener(e -> {
                try {
                    Desktop.getDesktop().browse(URI.create(config.getHrmUrl()));
                } catch (Exception ex) {
                    log.error("Failed to open browser: {}", ex.getMessage());
                }
            });
            popup.add(openHrmItem);

            MenuItem openLogsItem = new MenuItem("Mở thư mục Logs");
            openLogsItem.addActionListener(e -> {
                try {
                    File logsDir = new File("logs");
                    if (!logsDir.exists()) logsDir.mkdirs();
                    Desktop.getDesktop().open(logsDir);
                } catch (Exception ex) {
                    log.error("Failed to open logs dir: {}", ex.getMessage());
                }
            });
            popup.add(openLogsItem);

            popup.addSeparator();
            MenuItem exitItem = new MenuItem("Thoát");
            exitItem.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                        null,
                        "Bạn có chắc chắn muốn tắt HRM Attendance Tool không?\nCảnh báo chấm công sẽ ngừng hoạt động.",
                        "Xác nhận thoát",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) {
                    tray.remove(trayIcon);
                    System.exit(0);
                }
            });
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "HRM Attendance Warning Tool", popup);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

            desktopNotifier.setTrayIcon(trayIcon);
            log.info("System Tray initialized successfully.");

        } catch (Exception e) {
            log.error("Error creating System Tray: {}", e.getMessage(), e);
        }
    }

    public void updateStatusText(String text) {
        if (statusMenuItem != null) {
            statusMenuItem.setLabel("Trạng thái: " + text);
        }
        if (trayIcon != null) {
            trayIcon.setToolTip("HRM: " + text);
        }
    }

    public void checkAttendanceNow(boolean manualTrigger) {
        new Thread(() -> {
            try {
                log.info("Checking attendance and notifications (manualTrigger={})...", manualTrigger);

                // 1. Luôn kiểm tra thông báo phê duyệt đơn mới từ HRM (nghỉ phép, remote, OT)
                notificationMonitorService.checkNewNotifications();

                LocalDate today = LocalDate.now();

                // 2. Kiểm tra cuối tuần (Thứ 7 / Chủ Nhật)
                boolean isWeekend = (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY);
                if (isWeekend) {
                    String msg = "Cuối tuần (" + today.getDayOfWeek() + ") - Nghỉ làm";
                    updateStatusText(msg);
                    if (manualTrigger) {
                        desktopNotifier.showInfo("Lịch làm việc", "Hôm nay là " + msg + ". Không áp dụng cảnh báo chấm công.");
                    }
                    return;
                }

                // 2. Kiểm tra ngày nghỉ lễ từ API /api/holidays
                HolidayItem holiday = hrmApiClient.getHolidayForDate(today);
                if (holiday != null) {
                    String msg = "Nghỉ lễ: " + holiday.getName();
                    updateStatusText(msg);
                    if (manualTrigger) {
                        desktopNotifier.showInfo("Lịch nghỉ lễ", "Hôm nay là ngày nghỉ lễ: " + holiday.getName());
                    }
                    return;
                }

                // 3. Lấy dữ liệu chấm công từ API
                AttendanceResponse response = hrmApiClient.fetchCurrentMonthAttendance();

                if (response != null && response.isSuccess() && response.getData() != null) {
                    String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    AttendanceRecord todayRecord = response.getData().stream()
                            .filter(r -> todayStr.equals(r.getWorkDate()))
                            .findFirst()
                            .orElse(null);

                    if (todayRecord != null) {
                        String summary = String.format("In: %s | Out: %s | %s",
                                todayRecord.getCheckIn() != null ? todayRecord.getCheckIn() : "Chưa",
                                todayRecord.getCheckOut() != null ? todayRecord.getCheckOut() : "Chưa",
                                todayRecord.getStatus() != null ? todayRecord.getStatus() : "");
                        updateStatusText(summary);

                        if (manualTrigger) {
                            desktopNotifier.showInfo("HRM Hôm nay", summary);
                        }
                    } else {
                        updateStatusText("Chưa có chấm công hôm nay");
                        if (manualTrigger) {
                            desktopNotifier.showWarning("HRM Hôm nay", "Hôm nay chưa có dữ liệu chấm công trên hệ thống!");
                        }
                    }

                    // Run evaluation rules with holiday
                    List<AttendanceRuleEngine.RuleAlert> alerts = ruleEngine.evaluate(response, holiday);
                    for (AttendanceRuleEngine.RuleAlert alert : alerts) {
                        notificationManager.dispatchAlert(alert);
                    }
                } else {
                    String errDesc = hrmApiClient.getLastStatusDescription();
                    updateStatusText(errDesc);
                    if (manualTrigger) {
                        desktopNotifier.showError("Lỗi kết nối HRM", errDesc);
                    }
                }
            } catch (Exception e) {
                log.error("Error checking attendance: {}", e.getMessage(), e);
            }
        }).start();
    }

    private Image createTrayIconImage() {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Circular background (Primary Blue / Teal)
        g2.setColor(new Color(14, 116, 222));
        g2.fillOval(0, 0, size - 1, size - 1);

        // Clock hands or letter
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        FontMetrics fm = g2.getFontMetrics();
        int x = (size - fm.stringWidth("H")) / 2;
        int y = ((size - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString("H", x, y);

        g2.dispose();
        return img;
    }
}
