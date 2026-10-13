package com.gm.warning.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.warning.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TelegramNotifier {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotifier.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AppConfig config;
    private final HttpClient httpClient;

    public TelegramNotifier(AppConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends a notification message to the configured Telegram chat.
     */
    public boolean sendMessage(String htmlMessage) {
        if (!config.isTelegramEnabled()) {
            log.debug("Telegram notification is disabled in config.");
            return false;
        }

        String botToken = config.getTelegramBotToken();
        String chatId = config.getTelegramChatId();

        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.warn("Telegram bot token or chat ID is not configured! Please configure them in config.properties");
            return false;
        }

        String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken.trim());

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("chat_id", chatId.trim());
            payload.put("text", htmlMessage);
            payload.put("parse_mode", "HTML");
            payload.put("disable_web_page_preview", true);

            String jsonBody = mapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Telegram notification sent successfully to chat {}", chatId);
                return true;
            } else {
                log.error("Telegram API returned error {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send Telegram message: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Test sending a sample ping message.
     */
    public boolean testConnection() {
        return sendMessage(TelegramTemplate.testMessage());
    }
}
