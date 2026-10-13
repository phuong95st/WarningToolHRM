package com.gm.warning.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class DesktopToastNotifier {
    private static final Logger log = LoggerFactory.getLogger(DesktopToastNotifier.class);

    private TrayIcon trayIcon;

    public DesktopToastNotifier() {
    }

    public void setTrayIcon(TrayIcon trayIcon) {
        this.trayIcon = trayIcon;
    }

    public void showNotification(String title, String message, TrayIcon.MessageType type) {
        log.info("[DESKTOP TOAST] {} - {}", title, message);

        if (trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, type);
            } catch (Exception e) {
                log.warn("Failed to display tray message: {}", e.getMessage());
            }
        }

        // Play warning beep if it's an error or warning
        if (type == TrayIcon.MessageType.WARNING || type == TrayIcon.MessageType.ERROR) {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception ignored) {
            }
        }
    }

    public void showInfo(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.INFO);
    }

    public void showWarning(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.WARNING);
    }

    public void showError(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.ERROR);
    }
}
