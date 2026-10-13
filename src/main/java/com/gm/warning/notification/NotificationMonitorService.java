package com.gm.warning.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.warning.client.HrmApiClient;
import com.gm.warning.model.HrmNotificationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class NotificationMonitorService {
    private static final Logger log = LoggerFactory.getLogger(NotificationMonitorService.class);
    private static final String STATE_FILE = "notification_state.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final HrmApiClient hrmApiClient;
    private final TelegramNotifier telegramNotifier;
    private final DesktopToastNotifier desktopNotifier;

    private int lastSeenNotificationId = -1;

    public NotificationMonitorService(HrmApiClient hrmApiClient,
                                      TelegramNotifier telegramNotifier,
                                      DesktopToastNotifier desktopNotifier) {
        this.hrmApiClient = hrmApiClient;
        this.telegramNotifier = telegramNotifier;
        this.desktopNotifier = desktopNotifier;
        loadState();
    }

    /**
     * Checks for new notifications and dispatches alerts if any are found.
     */
    public synchronized void checkNewNotifications() {
        try {
            List<HrmNotificationItem> list = hrmApiClient.fetchNotifications();
            if (list == null || list.isEmpty()) {
                return;
            }

            // On very first run without state file, initialize to the latest notification ID to avoid spam
            if (lastSeenNotificationId == -1) {
                int maxId = list.stream().mapToInt(HrmNotificationItem::getId).max().orElse(0);
                lastSeenNotificationId = maxId;
                saveState();
                log.info("Khởi tạo giám sát thông báo HRM. Ghi nhận mốc ID thông báo hiện tại: #{}", maxId);
                return;
            }

            // Find new notifications where id > lastSeenNotificationId
            List<HrmNotificationItem> newItems = list.stream()
                    .filter(item -> item.getId() > lastSeenNotificationId)
                    .sorted(Comparator.comparingInt(HrmNotificationItem::getId))
                    .toList();

            if (!newItems.isEmpty()) {
                log.info("Phát hiện {} thông báo mới từ HRM!", newItems.size());

                for (HrmNotificationItem item : newItems) {
                    // Send Desktop notification
                    desktopNotifier.showInfo(item.getTitle(), item.getContent());

                    // Send Telegram notification
                    String telegramMsg = TelegramTemplate.hrmApprovalNotification(
                            item.getTitle(),
                            item.getContent(),
                            item.getCreatedAt()
                    );
                    telegramNotifier.sendMessage(telegramMsg);

                    // Advance last seen ID
                    if (item.getId() > lastSeenNotificationId) {
                        lastSeenNotificationId = item.getId();
                    }
                }

                saveState();
            }
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra thông báo mới từ HRM: {}", e.getMessage(), e);
        }
    }

    private void loadState() {
        File file = new File(STATE_FILE);
        if (file.exists()) {
            try {
                StateData data = mapper.readValue(file, StateData.class);
                this.lastSeenNotificationId = data.getLastSeenNotificationId();
                log.info("Đã tải mốc thông báo đã xem từ {}: #{}", STATE_FILE, lastSeenNotificationId);
            } catch (Exception e) {
                log.warn("Không thể đọc {}: {}", STATE_FILE, e.getMessage());
            }
        }
    }

    private void saveState() {
        try {
            StateData data = new StateData(lastSeenNotificationId);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(STATE_FILE), data);
            log.debug("Đã lưu mốc thông báo #{} vào {}", lastSeenNotificationId, STATE_FILE);
        } catch (Exception e) {
            log.error("Không thể lưu {}: {}", STATE_FILE, e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String formatTime(String raw) {
        if (raw == null) return "";
        if (raw.contains("T")) {
            return raw.replace("T", " ").substring(0, Math.min(19, raw.length()));
        }
        return raw;
    }

    public static class StateData {
        private int lastSeenNotificationId;

        public StateData() {}

        public StateData(int lastSeenNotificationId) {
            this.lastSeenNotificationId = lastSeenNotificationId;
        }

        public int getLastSeenNotificationId() { return lastSeenNotificationId; }
        public void setLastSeenNotificationId(int lastSeenNotificationId) { this.lastSeenNotificationId = lastSeenNotificationId; }
    }
}
