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
        String clean = rawText.trim();
        if (clean.contains("@")) {
            clean = clean.replaceAll("@[a-zA-Z0-9_]+", "");
        }
        String lower = clean.toLowerCase();

        try {
            if (lower.startsWith("/chamcong") || lower.startsWith("/cc") || lower.startsWith("/attendance")
                    || lower.startsWith("chấm công") || lower.startsWith("cham cong")) {
                handleAttendanceCommand(clean);
            } else if (lower.startsWith("/phep") || lower.startsWith("/nghiphep") || lower.startsWith("/leave")
                    || lower.startsWith("phép") || lower.startsWith("phep") || lower.startsWith("ngày phép") || lower.startsWith("ngay phep")) {
                handleLeaveBalanceCommand(clean);
            } else if (lower.startsWith("/donnghi") || lower.startsWith("/don") || lower.startsWith("/remote")
                    || lower.startsWith("đơn nghỉ") || lower.startsWith("don nghi") || lower.startsWith("đơn") || lower.startsWith("don")) {
                handleLeaveRequestsCommand();
            } else if (lower.startsWith("/ot") || lower.startsWith("/tangca") || lower.startsWith("/overtime")
                    || lower.startsWith("tăng ca") || lower.startsWith("tang ca") || lower.startsWith("làm thêm") || lower.startsWith("lam them")) {
                handleOvertimeCommand();
            } else if (lower.equals("/start") || lower.equals("/help") || lower.equals("help")
                    || lower.equals("hướng dẫn") || lower.equals("huong dan")) {
                telegramNotifier.sendMessage(TelegramTemplate.botHelpMessage());
            } else if (clean.startsWith("/")) {
                telegramNotifier.sendMessage("⚠️ <b>Lệnh không hợp lệ!</b>\n\nGõ /help để xem danh sách các lệnh được hỗ trợ.");
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý lệnh Telegram '{}': {}", rawText, e.getMessage(), e);
            telegramNotifier.sendMessage("❌ <b>Đã xảy ra lỗi khi thực thi lệnh:</b> " + e.getMessage());
        }
    }

    private void handleAttendanceCommand(String rawText) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        // Tách lấy phần tham số phía sau lệnh
        String text = rawText.toLowerCase()
                .replaceAll("^(/chamcong|/cc|/attendance|chấm công|cham cong)", "")
                .replace("tháng", "")
                .replace("thang", "")
                .replace("năm", "")
                .replace("nam", "")
                .trim();

        if (!text.isEmpty()) {
            try {
                if (text.contains("/")) {
                    String[] mYear = text.split("/");
                    month = Integer.parseInt(mYear[0].trim());
                    year = Integer.parseInt(mYear[1].trim());
                } else if (text.contains("-")) {
                    String[] yMonth = text.split("-");
                    if (yMonth[0].trim().length() == 4) {
                        year = Integer.parseInt(yMonth[0].trim());
                        month = Integer.parseInt(yMonth[1].trim());
                    } else {
                        month = Integer.parseInt(yMonth[0].trim());
                        year = Integer.parseInt(yMonth[1].trim());
                    }
                } else {
                    String[] tokens = text.split("\\s+");
                    if (tokens.length >= 2) {
                        month = Integer.parseInt(tokens[0].trim());
                        year = Integer.parseInt(tokens[1].trim());
                    } else if (tokens.length == 1) {
                        int m = Integer.parseInt(tokens[0].trim());
                        if (m >= 1 && m <= 12) {
                            month = m;
                        }
                    }
                }
            } catch (Exception e) {
                telegramNotifier.sendMessage("⚠️ Định dạng tháng không đúng!\nVí dụ hợp lệ: <code>/chamcong 9</code>, <code>/chamcong 09/2026</code> hoặc <code>/chamcong 9 2026</code>");
                return;
            }
        }

        if (year < 100) year += 2000;
        if (month < 1 || month > 12) {
            telegramNotifier.sendMessage("⚠️ Tháng không hợp lệ (phải từ 1 đến 12).");
            return;
        }

        AttendanceResponse response = hrmApiClient.fetchAttendance(year, month);
        if (response != null && response.isSuccess() && response.getData() != null) {
            telegramNotifier.sendMessage(TelegramTemplate.attendanceMonthSummary(year, month, response.getData()));
        } else {
            telegramNotifier.sendMessage(String.format("❌ Không lấy được dữ liệu chấm công tháng %02d/%d từ HRM.", month, year));
        }
    }

    private void handleLeaveBalanceCommand(String rawText) {
        int year = LocalDate.now().getYear();
        String text = rawText.toLowerCase()
                .replaceAll("^(/phep|/nghiphep|/leave|ngày phép|ngay phep|phép|phep)", "")
                .replace("năm", "")
                .replace("nam", "")
                .trim();
        if (!text.isEmpty()) {
            try {
                int y = Integer.parseInt(text);
                if (y < 100) y += 2000;
                year = y;
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
        List<com.gm.warning.model.LeaveRequestItem> leaveList = hrmApiClient.fetchLeaveRequests();
        telegramNotifier.sendMessage(TelegramTemplate.leaveRequestsSummary(leaveList));
    }

    private void handleOvertimeCommand() {
        List<OvertimeItem> otList = hrmApiClient.fetchOvertimeRequests();
        telegramNotifier.sendMessage(TelegramTemplate.overtimeRequestsSummary(otList));
    }
}
