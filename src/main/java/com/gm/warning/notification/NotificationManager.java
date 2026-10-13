package com.gm.warning.notification;

import com.gm.warning.engine.AttendanceRuleEngine.AlertLevel;
import com.gm.warning.engine.AttendanceRuleEngine.RuleAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.TrayIcon;

public class NotificationManager {
    private static final Logger log = LoggerFactory.getLogger(NotificationManager.class);

    private final TelegramNotifier telegramNotifier;
    private final DesktopToastNotifier desktopNotifier;

    public NotificationManager(TelegramNotifier telegramNotifier, DesktopToastNotifier desktopNotifier) {
        this.telegramNotifier = telegramNotifier;
        this.desktopNotifier = desktopNotifier;
    }

    public void dispatchAlert(RuleAlert alert) {
        if (alert == null) return;

        // 1. Send to Desktop Toast
        String plainText = stripHtml(alert.getMessage());
        TrayIcon.MessageType messageType = switch (alert.getLevel()) {
            case INFO -> TrayIcon.MessageType.INFO;
            case WARNING -> TrayIcon.MessageType.WARNING;
            case URGENT -> TrayIcon.MessageType.ERROR;
        };
        desktopNotifier.showNotification(alert.getTitle(), plainText, messageType);

        // 2. Send to Telegram
        telegramNotifier.sendMessage(alert.getMessage());
    }

    public void dispatchCustomNotification(String title, String htmlMessage, AlertLevel level) {
        dispatchAlert(new RuleAlert(level, title, htmlMessage));
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").trim();
    }
}
