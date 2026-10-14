package com.gm.warning.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.warning.client.HrmApiClient;
import com.gm.warning.config.AppConfig;
import com.gm.warning.model.AttendanceRecord;
import com.gm.warning.model.AttendanceResponse;
import com.gm.warning.model.HrmNotificationItem;
import com.gm.warning.model.LeaveBalanceData;
import com.gm.warning.model.OvertimeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

public class TelegramBotService {
    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AppConfig config;
    private final HrmApiClient hrmApiClient;
    private final TelegramNotifier telegramNotifier;
    private final HttpClient httpClient;

    private volatile boolean running = false;
    private Thread pollingThread;
    private long lastUpdateId = 0;

    public TelegramBotService(AppConfig config, HrmApiClient hrmApiClient, TelegramNotifier telegramNotifier) {
        this.config = config;
        this.hrmApiClient = hrmApiClient;
        this.telegramNotifier = telegramNotifier;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public synchronized void start() {
        if (running) return;
        if (!config.isTelegramEnabled()) {
            log.info("Telegram notification is disabled in config. Skipping bot listener.");
            return;
        }

        String botToken = config.getTelegramBotToken();
        if (botToken == null || botToken.isBlank() || botToken.contains("YOUR_BOT_TOKEN")) {
            log.warn("Telegram Bot Token is not configured. Telegram bot listener will not start.");
            return;
        }

        running = true;
        registerBotCommands();

        pollingThread = new Thread(this::pollUpdatesLoop, "telegram-bot-polling");
        pollingThread.setDaemon(true);
        pollingThread.start();
        log.info("Telegram Bot Command Listener started successfully!");
    }

    public synchronized void stop() {
        running = false;
        if (pollingThread != null) {
            pollingThread.interrupt();
        }
        log.info("Telegram Bot Command Listener stopped.");
    }

    /**
     * Đăng ký menu lệnh nhanh trên giao diện Telegram (nút '/')
     */
    private void registerBotCommands() {
        try {
            String botToken = config.getTelegramBotToken();
            String url = String.format("https://api.telegram.org/bot%s/setMyCommands", botToken.trim());

            Map<String, Object> body = new HashMap<>();
            List<Map<String, String>> commands = new ArrayList<>();
            commands.add(Map.of("command", "chamcong", "description", "Xem chi tiết chấm công trong tháng"));
            commands.add(Map.of("command", "phep", "description", "Xem số ngày phép năm, đã dùng, còn lại"));
            commands.add(Map.of("command", "donnghi", "description", "Xem danh sách đơn nghỉ phép, Remote"));
            commands.add(Map.of("command", "ot", "description", "Xem danh sách đơn làm thêm giờ (OT)"));
            commands.add(Map.of("command", "help", "description", "Hướng dẫn sử dụng"));

            body.put("commands", commands);
            String json = mapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("Đăng ký menu lệnh Telegram thành công!");
            } else {
                log.warn("Không thể đăng ký menu Telegram: HTTP {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Lỗi khi đăng ký menu lệnh Telegram: {}", e.getMessage());
        }
    }

    private void pollUpdatesLoop() {
        while (running) {
            try {
                String botToken = config.getTelegramBotToken();
                String url = String.format("https://api.telegram.org/bot%s/getUpdates?offset=%d&timeout=20",
                        botToken.trim(), lastUpdateId + 1);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = mapper.readTree(response.body());
                    if (root.path("ok").asBoolean(false)) {
                        JsonNode results = root.path("result");
                        if (results.isArray()) {
                            for (JsonNode update : results) {
                                long updateId = update.path("update_id").asLong();
                                if (updateId > lastUpdateId) {
                                    lastUpdateId = updateId;
                                }
                                processUpdate(update);
                            }
                        }
                    }
                } else if (response.statusCode() == 409) {
                    log.warn("Telegram conflict (409): Một tiến trình bot khác đang chạy polling. Chờ 5s...");
                    Thread.sleep(5000);
                } else {
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                log.debug("Lỗi khi lấy update từ Telegram: {}", e.getMessage());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    private void processUpdate(JsonNode update) {
        JsonNode message = update.path("message");
        if (message.isMissingNode() || message.isNull()) return;

        String chatId = message.path("chat").path("id").asText();
        String text = message.path("text").asText("").trim();
        if (text.isEmpty()) return;

        // Bảo mật: Chỉ phản hồi đúng chat_id được cấu hình
        String configuredChatId = config.getTelegramChatId();
        if (configuredChatId != null && !configuredChatId.isBlank()) {
            if (!chatId.equals(configuredChatId.trim())) {
                log.warn("Từ chối tin nhắn Telegram từ chat_id lạ: {}", chatId);
                return;
            }
        }

        log.info("Nhận tin nhắn Telegram từ {}: '{}'", chatId, text);
        handleCommand(text);
    }

    private void handleCommand(String rawText) {
        String command = rawText.toLowerCase();
        // Cắt bỏ phần @botname nếu có (ví dụ /chamcong@MyBot -> /chamcong)
        if (command.contains("@")) {
            command = command.replaceAll("@[a-zA-Z0-9_]+", "");
        }

        String[] parts = command.split("\\s+");
        String cmd = parts[0];

        try {
            switch (cmd) {
                case "/start":
                case "/help":
                case "help":
                case "hướng dẫn":
                case "huong dan":
                    telegramNotifier.sendMessage(TelegramTemplate.botHelpMessage());
                    break;

                case "/chamcong":
                case "/cc":
                case "/attendance":
                case "chấm công":
                case "cham cong":
                    handleAttendanceCommand(parts);
                    break;

                case "/phep":
                case "/nghiphep":
                case "/leave":
                case "phép":
                case "phep":
                case "ngày phép":
                    handleLeaveBalanceCommand(parts);
                    break;

                case "/donnghi":
                case "/don":
                case "/remote":
                case "đơn nghỉ":
                case "don nghi":
                    handleLeaveRequestsCommand();
                    break;

                case "/ot":
                case "/tangca":
                case "/overtime":
                case "tăng ca":
                case "làm thêm":
                    handleOvertimeCommand();
                    break;

                default:
                    if (rawText.startsWith("/")) {
                        telegramNotifier.sendMessage("⚠️ <b>Lệnh không hợp lệ!</b>\n\nGõ /help để xem danh sách các lệnh được hỗ trợ.");
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý lệnh Telegram '{}': {}", rawText, e.getMessage(), e);
            telegramNotifier.sendMessage("❌ <b>Đã xảy ra lỗi khi thực thi lệnh:</b> " + e.getMessage());
        }
    }

    private void handleAttendanceCommand(String[] parts) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        if (parts.length > 1) {
            String arg = parts[1];
            // Format mm/yyyy hoặc m/yyyy hoặc yyyy-mm hoặc chỉ tháng (1-12)
            try {
                if (arg.contains("/")) {
                    String[] mYear = arg.split("/");
                    month = Integer.parseInt(mYear[0].trim());
                    year = Integer.parseInt(mYear[1].trim());
                } else if (arg.contains("-")) {
                    String[] yMonth = arg.split("-");
                    year = Integer.parseInt(yMonth[0].trim());
                    month = Integer.parseInt(yMonth[1].trim());
                } else {
                    int m = Integer.parseInt(arg.trim());
                    if (m >= 1 && m <= 12) {
                        month = m;
                    }
                }
            } catch (Exception e) {
                telegramNotifier.sendMessage("⚠️ Định dạng tháng không đúng! Vui lòng nhập dạng: <code>/chamcong 10/2026</code> hoặc <code>/chamcong 10</code>");
                return;
            }
        }

        AttendanceResponse response = hrmApiClient.fetchAttendance(year, month);
        if (response != null && response.isSuccess() && response.getData() != null) {
            telegramNotifier.sendMessage(TelegramTemplate.attendanceMonthSummary(year, month, response.getData()));
        } else {
            telegramNotifier.sendMessage(String.format("❌ Không lấy được dữ liệu chấm công tháng %02d/%d từ HRM.", month, year));
        }
    }

    private void handleLeaveBalanceCommand(String[] parts) {
        int year = LocalDate.now().getYear();
        if (parts.length > 1) {
            try {
                year = Integer.parseInt(parts[1].trim());
            } catch (Exception ignored) {}
        }

        LeaveBalanceData data = hrmApiClient.fetchLeaveBalance(year);
        if (data != null) {
            telegramNotifier.sendMessage(TelegramTemplate.leaveBalanceSummary(data));
        } else {
            telegramNotifier.sendMessage(String.format("❌ Không lấy được số dư ngày phép năm %d từ hệ thống HRM.", year));
        }
    }

    private void handleLeaveRequestsCommand() {
        List<HrmNotificationItem> notifications = hrmApiClient.fetchNotifications();
        telegramNotifier.sendMessage(TelegramTemplate.leaveRequestsSummary(notifications));
    }

    private void handleOvertimeCommand() {
        List<OvertimeItem> otList = hrmApiClient.fetchOvertimeRequests();
        telegramNotifier.sendMessage(TelegramTemplate.overtimeRequestsSummary(otList));
    }
}
