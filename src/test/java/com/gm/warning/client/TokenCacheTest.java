package com.gm.warning.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class TokenCacheTest {

    @Test
    @DisplayName("TokenCache tự động hủy token khi đổi username nhân viên khác")
    void testUserChangeInvalidation() {
        TokenCache cache = new TokenCache();

        long futureExp = Instant.now().getEpochSecond() + 3600; // 1 hour in future
        String payload = "{\"sub\":\"user123\",\"exp\":" + futureExp + "}";
        String mockJwt = "eyJhbGciOiJIUzI1NiJ9." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes()) + ".signature";

        cache.setToken(mockJwt, "user_a@gm-group.vn");
        assertTrue(cache.isValid("user_a@gm-group.vn"), "Token của user_a phải hợp lệ");

        // Now employee is changed to user_b in config
        boolean validForUserB = cache.isValid("user_b@gm-group.vn");
        assertFalse(validForUserB, "Đổi sang user_b thì token cũ phải bị vô hiệu hóa ngay");
        assertNull(cache.getToken(), "Token cũ phải bị xóa khỏi cache");
    }

    @Test
    @DisplayName("Token hết hạn phải trả về isValid = false")
    void testExpiredToken() {
        TokenCache cache = new TokenCache();

        long pastExp = Instant.now().getEpochSecond() - 60; // 1 minute in the past
        String payload = "{\"sub\":\"user123\",\"exp\":" + pastExp + "}";
        String mockJwt = "eyJhbGciOiJIUzI1NiJ9." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes()) + ".signature";

        cache.setToken(mockJwt, "user_test@gm-group.vn");
        assertFalse(cache.isValid("user_test@gm-group.vn"), "Token hết hạn phải không hợp lệ");
    }

    @Test
    @DisplayName("Playwright khởi tạo driver thành công với PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD mà không tải trình duyệt thừa")
    void testPlaywrightDriverInitWithoutBrowserDownload() {
        java.util.Map<String, String> env = new java.util.HashMap<>();
        if (System.getenv() != null) {
            env.putAll(System.getenv());
        }
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        com.microsoft.playwright.Playwright.CreateOptions createOptions =
                new com.microsoft.playwright.Playwright.CreateOptions().setEnv(env);

        try (com.microsoft.playwright.Playwright playwright = com.microsoft.playwright.Playwright.create(createOptions)) {
            assertNotNull(playwright, "Playwright instance phải khởi tạo thành công");
        }
    }
}
