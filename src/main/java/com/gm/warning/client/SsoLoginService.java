package com.gm.warning.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.warning.config.AppConfig;
import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

public class SsoLoginService {
    private static final Logger log = LoggerFactory.getLogger(SsoLoginService.class);
    private static final Logger attendanceLogger = LoggerFactory.getLogger("ATTENDANCE_LOGGER");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AppConfig config;
    private final TokenCache tokenCache;

    public SsoLoginService(AppConfig config, TokenCache tokenCache) {
        this.config = config;
        this.tokenCache = tokenCache;
    }

    /**
     * Attempts to automatically login via Playwright and capture a fresh JWT token.
     * @return Fresh JWT token or null if failed.
     */
    public synchronized String performSsoLogin() {
        log.info("Bắt đầu quy trình tự động đăng nhập SSO ngầm qua Playwright cho tài khoản [{}]...",
                config.getMicrosoftUsername());
        AtomicReference<String> capturedToken = new AtomicReference<>();

        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(config.isBrowserHeadless());

            if (config.getBrowserChannel() != null && !config.getBrowserChannel().isBlank()) {
                try {
                    options.setChannel(config.getBrowserChannel());
                } catch (Exception e) {
                    log.warn("Browser channel '{}' không khả dụng, chuyển về mặc định: {}",
                            config.getBrowserChannel(), e.getMessage());
                }
            }

            BrowserContext context = null;
            try {
                context = playwright.chromium().launchPersistentContext(
                        Paths.get(config.getBrowserUserDataDir()),
                        options
                );
            } catch (Exception e) {
                log.warn("Không thể mở browser persistent context với channel {}, chuyển sang Chromium tiêu chuẩn: {}",
                        config.getBrowserChannel(), e.getMessage());
                options.setChannel((String) null);
                context = playwright.chromium().launchPersistentContext(
                        Paths.get(config.getBrowserUserDataDir()),
                        options
                );
            }

            context.setDefaultTimeout(30000);

            // Intercept outgoing HTTP requests to capture Authorization Bearer header
            context.onRequest(request -> {
                String authHeader = request.headers().get("authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7).trim();
                    if (!token.isBlank() && token.contains(".")) {
                        capturedToken.set(token);
                        log.info("Chặn bắt thành công Bearer token từ request: {}", request.url());
                    }
                }
            });

            // Intercept responses for Keycloak token endpoint
            context.onResponse(response -> {
                if (response.url().contains("/protocol/openid-connect/token")) {
                    try {
                        String text = response.text();
                        JsonNode json = mapper.readTree(text);
                        if (json.has("access_token")) {
                            String token = json.get("access_token").asText();
                            capturedToken.set(token);
                            log.info("Chặn bắt thành công access_token từ Keycloak token endpoint");
                        }
                    } catch (Exception ignored) {
                    }
                }
            });

            Page page = context.newPage();
            log.info("Đang truy cập trang web HRM: {}", config.getHrmUrl());
            try {
                page.navigate(config.getHrmUrl());
            } catch (Exception e) {
                String msg = String.format("Không thể mở trang HRM (%s). Hệ thống chấm công có thể đang sập, mất mạng hoặc bảo trì. Chi tiết: %s",
                        config.getHrmUrl(), e.getMessage());
                log.error("[LỖI KẾT NỐI WEB HRM] {}", msg);
                attendanceLogger.warn("[MẤT KẾT NỐI] {}", msg);
                context.close();
                return null;
            }

            // Wait and inspect navigation flow
            for (int i = 0; i < 20; i++) {
                if (capturedToken.get() != null) {
                    break;
                }

                String currentUrl = page.url();
                log.debug("URL trang hiện tại [bước {}]: {}", i, currentUrl);

                // Check if on Keycloak login page with social login buttons
                if (currentUrl.contains("auth.gm-group.vn") || currentUrl.contains("/openid-connect/auth")) {
                    try {
                        Locator msButton = page.locator("a:has-text('Microsoft'), a:has-text('Azure'), button:has-text('Microsoft'), #social-microsoft, #zocial-microsoft");
                        if (msButton.count() > 0 && msButton.first().isVisible()) {
                            log.info("Phát hiện nút đăng nhập Microsoft trên Keycloak, đang nhấn...");
                            msButton.first().click();
                            page.waitForTimeout(2000);
                        }
                    } catch (Exception ignored) {
                    }
                }

                // Check if on Microsoft login page
                if (currentUrl.contains("login.microsoftonline.com") || currentUrl.contains("login.live.com")) {
                    handleMicrosoftLogin(page);
                }

                // If already on HRM dashboard, try to trigger a query or evaluate token
                if (currentUrl.contains("hrm.gm-group.vn")) {
                    try {
                        Object tokenObj = page.evaluate("() => window.Xu ? window.Xu.token : (localStorage.getItem('token') || sessionStorage.getItem('token'))");
                        if (tokenObj != null && tokenObj.toString().contains(".")) {
                            capturedToken.set(tokenObj.toString());
                            log.info("Đọc được token thành công từ ngữ cảnh trang web");
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }

                page.waitForTimeout(2000);
            }

            if (capturedToken.get() == null) {
                page.waitForTimeout(3000);
            }

            context.close();

        } catch (Exception e) {
            String msg = String.format("Lỗi trong quy trình Playwright SSO: %s", e.getMessage());
            log.error("[LỖI PLAYWRIGHT SSO] {}", msg, e);
            attendanceLogger.warn("[LỖI SSO] {}", msg);
        }

        String token = capturedToken.get();
        if (token != null) {
            log.info("Đăng nhập SSO thành công! Cập nhật token cho nhân viên [{}]...", config.getMicrosoftUsername());
            tokenCache.setToken(token, config.getMicrosoftUsername());
            return token;
        } else {
            String warnMsg = "Đăng nhập SSO hoàn tất nhưng không lấy được token. Vui lòng kiểm tra lại tài khoản hoặc trạng thái máy chủ!";
            log.warn("[CẢNH BÁO SSO] {}", warnMsg);
            attendanceLogger.warn("[CẢNH BÁO SSO] {}", warnMsg);
            return null;
        }
    }

    private void handleMicrosoftLogin(Page page) {
        log.info("Đang xử lý trang đăng nhập Microsoft Account...");
        try {
            // Check for previous errors on page
            checkMicrosoftErrors(page);

            // 1. Enter email if present
            Locator emailInput = page.locator("input[type='email'], input[name='loginfmt'], #i0116");
            if (emailInput.count() > 0 && emailInput.first().isVisible()) {
                log.info("Điền email Microsoft: {}", config.getMicrosoftUsername());
                emailInput.first().fill(config.getMicrosoftUsername());
                page.waitForTimeout(500);
                Locator nextBtn = page.locator("input[type='submit'], #idSIButton9, button:has-text('Next'), button:has-text('Tiếp theo')");
                if (nextBtn.count() > 0) {
                    nextBtn.first().click();
                }
                page.waitForTimeout(2000);
                checkMicrosoftErrors(page);
            }

            // 2. Enter password if present
            Locator passwordInput = page.locator("input[type='password'], input[name='passwd'], #i0118");
            if (passwordInput.count() > 0 && passwordInput.first().isVisible()) {
                log.info("Điền mật khẩu Microsoft...");
                passwordInput.first().fill(config.getMicrosoftPassword());
                page.waitForTimeout(500);
                Locator submitBtn = page.locator("input[type='submit'], #idSIButton9, button:has-text('Sign in'), button:has-text('Đăng nhập')");
                if (submitBtn.count() > 0) {
                    submitBtn.first().click();
                }
                page.waitForTimeout(2500);
                checkMicrosoftErrors(page);
            }

            // 3. "Stay signed in?" prompt
            Locator kmsiBtn = page.locator("#idSIButton9, input[value='Yes'], button:has-text('Yes'), button:has-text('Có')");
            if (kmsiBtn.count() > 0 && kmsiBtn.first().isVisible()) {
                log.info("Xác nhận 'Duy trì đăng nhập' (Stay signed in)...");
                kmsiBtn.first().click();
                page.waitForTimeout(2000);
            }
        } catch (Exception e) {
            log.warn("Ngoại lệ khi điền form Microsoft: {}", e.getMessage());
        }
    }

    private void checkMicrosoftErrors(Page page) {
        try {
            Locator errorEl = page.locator("#usernameError, #passwordError, .error-message, .alert-error");
            if (errorEl.count() > 0 && errorEl.first().isVisible()) {
                String errorText = errorEl.first().innerText();
                log.error("[LỖI XÁC THỰC MICROSOFT] Báo lỗi từ Microsoft: {}", errorText);
            }

            Locator mfaPrompt = page.locator("#idDiv_SAOTCS_Title, #idRichContext_DisplaySign, div:has-text('Approve a request'), div:has-text('Xác minh danh tính')");
            if (mfaPrompt.count() > 0 && mfaPrompt.first().isVisible()) {
                log.warn("[CẦN DUYỆT 2FA MFA] Microsoft yêu cầu phê duyệt qua ứng dụng Authenticator hoặc mã SMS!");
            }
        } catch (Exception ignored) {
        }
    }
}
