package com.gm.warning.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class TokenCache {
    private static final Logger log = LoggerFactory.getLogger(TokenCache.class);
    private static final String TOKEN_FILE = "token.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    private String token;
    private long expiresAtEpochSec = 0;
    private String username;

    public TokenCache() {
        loadFromFile();
    }

    public synchronized void setToken(String token, String username) {
        this.token = token;
        this.expiresAtEpochSec = extractExpiry(token);
        this.username = username;
        saveToFile();
    }

    public synchronized String getToken() {
        return token;
    }

    public synchronized String extractEmployeeId() {
        if (token == null || !token.contains(".")) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                JsonNode json = mapper.readTree(decoded);
                if (json.has("employeeid")) {
                    return json.get("employeeid").asText();
                } else if (json.has("sub")) {
                    return json.get("sub").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract employeeId from token: {}", e.getMessage());
        }
        return null;
    }

    public synchronized boolean isValid(String currentConfigUsername) {
        if (token == null || token.isBlank()) {
            return false;
        }
        // If employee username changed in config.properties, invalidate cached token
        if (currentConfigUsername != null && username != null && !currentConfigUsername.equalsIgnoreCase(username)) {
            log.warn("Configured username '{}' differs from cached token owner '{}'. Invalidating old token...",
                    currentConfigUsername, username);
            invalidate();
            return false;
        }
        // Buffer 5 minutes (300 seconds)
        long currentSec = Instant.now().getEpochSecond();
        return (expiresAtEpochSec - 300) > currentSec;
    }

    public synchronized void invalidate() {
        this.token = null;
        this.expiresAtEpochSec = 0;
        File file = new File(TOKEN_FILE);
        if (file.exists()) {
            file.delete();
            log.info("Deleted expired/invalid {}", TOKEN_FILE);
        }
    }

    public synchronized long getExpiresAtEpochSec() {
        return expiresAtEpochSec;
    }

    public synchronized String getEmployeeId() {
        return extractEmployeeId(this.token);
    }

    public static String extractEmployeeId(String jwt) {
        try {
            if (jwt == null || !jwt.contains(".")) {
                return null;
            }
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = mapper.readTree(decoded);
            if (payload.has("employeeid")) {
                return payload.get("employeeid").asText();
            }
            if (payload.has("employeeId")) {
                return payload.get("employeeId").asText();
            }
        } catch (Exception e) {
            log.warn("Failed to extract employeeId from JWT payload: {}", e.getMessage());
        }
        return null;
    }

    private long extractExpiry(String jwt) {
        try {
            if (jwt == null || !jwt.contains(".")) {
                return 0;
            }
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return 0;
            }
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = mapper.readTree(decoded);
            if (payload.has("exp")) {
                long exp = payload.get("exp").asLong();
                log.info("Token expiry: {} (Epoch: {})", Instant.ofEpochSecond(exp), exp);
                return exp;
            }
        } catch (Exception e) {
            log.warn("Failed to parse JWT payload: {}", e.getMessage());
        }
        return 0;
    }

    private void saveToFile() {
        try {
            TokenData data = new TokenData(token, expiresAtEpochSec, username);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(TOKEN_FILE), data);
            log.info("Saved token for user [{}] to {}", username, TOKEN_FILE);
        } catch (Exception e) {
            log.error("Failed to save token to file: {}", e.getMessage());
        }
    }

    private void loadFromFile() {
        File file = new File(TOKEN_FILE);
        if (file.exists()) {
            try {
                TokenData data = mapper.readValue(file, TokenData.class);
                this.token = data.getToken();
                this.expiresAtEpochSec = data.getExpiresAtEpochSec();
                this.username = data.getUsername();
                log.info("Loaded cached token for user [{}] from {}", username, TOKEN_FILE);
            } catch (Exception e) {
                log.warn("Failed to load token from file: {}", e.getMessage());
            }
        }
    }

    public static class TokenData {
        private String token;
        private long expiresAtEpochSec;
        private String username;

        public TokenData() {}

        public TokenData(String token, long expiresAtEpochSec, String username) {
            this.token = token;
            this.expiresAtEpochSec = expiresAtEpochSec;
            this.username = username;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public long getExpiresAtEpochSec() { return expiresAtEpochSec; }
        public void setExpiresAtEpochSec(long expiresAtEpochSec) { this.expiresAtEpochSec = expiresAtEpochSec; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
