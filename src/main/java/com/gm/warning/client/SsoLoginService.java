package com.gm.warning.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.warning.config.AppConfig;
import com.microsoft.playwright.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
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

        Map<String, String> env = new java.util.HashMap<>();
        if (System.getenv() != null) {
            env.putAll(System.getenv());
        }
        env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
        Playwright.CreateOptions createOptions = new Playwright.CreateOptions().setEnv(env);

        Path userDataPath = Paths.get(config.getBrowserUserDataDir()).toAbsolutePath();
        try {
            Files.deleteIfExists(userDataPath.resolve("SingletonLock"));
            Files.deleteIfExists(userDataPath.resolve("SingletonSocket"));
            Files.deleteIfExists(userDataPath.resolve("SingletonCookie"));
        } catch (Exception ignored) {
        }

        try (Playwright playwright = Playwright.create(createOptions)) {
            BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
                    .setHeadless(config.isBrowserHeadless());

            if (config.getBrowserChannel() != null && !config.getBrowserChannel().isBlank()) {
                options.setChannel(config.getBrowserChannel());
            }

            BrowserContext context = null;
            try {
                context = playwright.chromium().launchPersistentContext(
                        userDataPath,
                        options
                );
            } catch (Exception e) {
                log.warn("Không thể mở browser persistent context với channel {}, thử tìm file thực thi trên hệ thống: {}",
                        config.getBrowserChannel(), e.getMessage());
                Path systemBrowser = findSystemBrowserPath();
                if (systemBrowser != null) {
                    log.info("Sử dụng trình duyệt hệ thống tại: {}", systemBrowser);
                    options.setChannel((String) null);
                    options.setExecutablePath(systemBrowser);
                    context = playwright.chromium().launchPersistentContext(
                            userDataPath,
                            options
                    );
                } else {
                    throw e;
                }
            }

            context.setDefaultTimeout(30000);

            // Intercept outgoing HTTP requests to capture Authorization Bearer header
            context.onRequest(request -> {
                String authHeader = request.headerValue("authorization");
                if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
                    String token = authHeader.substring(7).trim();
                    if (isLikelyJwtAccessToken(token)) {
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
                            if (isLikelyJwtAccessToken(token)) {
                                capturedToken.set(token);
                                log.info("Chặn bắt thành công access_token từ Keycloak token endpoint");
                            }
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
            for (int i = 0; i < 25; i++) {
                if (capturedToken.get() != null) {
                    break;
                }

                String currentUrl = page.url();
                String title = "";
                try {
                    title = page.title();
                } catch (Exception ignored) {}
                log.info("URL trang [bước {}]: {} | Tiêu đề: [{}]", i + 1, currentUrl, title);

                // Check cookies for token (ignore Keycloak internal flow cookies like KC_RESTART)
                try {
                    for (com.microsoft.playwright.options.Cookie c : context.cookies()) {
                        if (c.name.startsWith("KC_") || c.name.startsWith("KEYCLOAK_") || c.name.startsWith("_ga")) {
                            continue;
                        }
                        if (isLikelyJwtAccessToken(c.value)) {
                            capturedToken.set(c.value);
                            log.info("Chặn bắt thành công JWT token từ Cookie: {}", c.name);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
                if (capturedToken.get() != null) {
                    break;
                }

                // 1. Check if on Microsoft login page
                if (currentUrl.contains("login.microsoftonline.com") || currentUrl.contains("login.live.com")) {
                    handleMicrosoftLogin(page);
                }
                // 2. Check if on Keycloak login page (strictly matching auth domain)
                else if (currentUrl.contains("://auth.gm-group.vn")) {
                    handleKeycloakLogin(page);
                }
                // 3. If on HRM web page
                else if (currentUrl.contains("://hrm.gm-group.vn")) {
                    handleHrmPage(page, capturedToken);
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

    private void handleKeycloakLogin(Page page) {
        try {
            String bodyText = "";
            try {
                bodyText = page.locator("body").innerText().replace("\n", " ").replaceAll("\\s+", " ").trim();
                if (bodyText.length() > 300) bodyText = bodyText.substring(0, 300);
            } catch (Exception ignored) {}
            log.info("Nội dung hiển thị trên trang Keycloak: [{}]", bodyText);

            Locator feedback = page.locator("#kc-feedback-text, .alert-error, .alert-danger, .error-message");
            if (feedback.count() > 0 && feedback.first().isVisible()) {
                log.warn("[BÁO LỖI TỪ KEYCLOAK] {}", feedback.first().innerText());
            }

            // Click social identity provider (GM Account / Microsoft)
            Locator msButton = page.locator("a:has-text('GM Account'), button:has-text('GM Account'), #social-gm-account, #zocial-gm-account, a:has-text('Microsoft'), button:has-text('Microsoft'), #social-microsoft, #zocial-microsoft, .zocial.microsoft");
            if (msButton.count() > 0 && msButton.first().isVisible()) {
                log.info("Phát hiện nút đăng nhập GM Account / Microsoft trên Keycloak, đang nhấn: [{}]...", msButton.first().innerText());
                msButton.first().click();
                page.waitForTimeout(3000);
            } else {
                Locator kcUsername = page.locator("#username, input[name='username']");
                Locator kcPassword = page.locator("#password, input[name='password']");
                if (kcUsername.count() > 0 && kcUsername.first().isVisible() && kcPassword.count() > 0 && kcPassword.first().isVisible()) {
                    log.info("Phát hiện form đăng nhập trực tiếp Keycloak, đang điền tài khoản...");
                    kcUsername.first().fill(config.getMicrosoftUsername());
                    kcPassword.first().fill(config.getMicrosoftPassword());
                    Locator loginSubmit = page.locator("#kc-login, input[type='submit'], button[type='submit']");
                    if (loginSubmit.count() > 0) {
                        loginSubmit.first().click();
                        page.waitForTimeout(3000);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Ngoại lệ khi thao tác trang Keycloak: {}", e.getMessage());
        }
    }

    private void handleHrmPage(Page page, AtomicReference<String> capturedToken) {
        try {
            // Check if there is a login button needing click
            Locator loginBtn = page.locator("a:has-text('Đăng nhập'), button:has-text('Đăng nhập'), a:has-text('Login'), button:has-text('Login'), a:has-text('Sign in'), button:has-text('Sign in'), .btn-login, #login");
            if (loginBtn.count() > 0 && loginBtn.first().isVisible()) {
                log.info("Phát hiện nút Đăng nhập trên giao diện HRM, đang nhấn...");
                loginBtn.first().click();
                page.waitForTimeout(2000);
            }

            // Inspect localStorage and sessionStorage
            Object tokenObj = page.evaluate("() => {\n" +
                    "  if (window.Xu && window.Xu.token) return window.Xu.token;\n" +
                    "  for (let i = 0; i < localStorage.length; i++) {\n" +
                    "    let key = localStorage.key(i);\n" +
                    "    let val = localStorage.getItem(key);\n" +
                    "    if (typeof val === 'string' && val.startsWith('eyJ') && val.split('.').length === 3) return val;\n" +
                    "  }\n" +
                    "  for (let i = 0; i < sessionStorage.length; i++) {\n" +
                    "    let key = sessionStorage.key(i);\n" +
                    "    let val = sessionStorage.getItem(key);\n" +
                    "    if (typeof val === 'string' && val.startsWith('eyJ') && val.split('.').length === 3) return val;\n" +
                    "  }\n" +
                    "  return null;\n" +
                    "}");
            if (tokenObj != null && isLikelyJwtAccessToken(tokenObj.toString())) {
                capturedToken.set(tokenObj.toString());
                log.info("Đọc được JWT token thành công từ Web Storage của trang HRM");
            }
        } catch (Exception ignored) {
        }
    }

    private void handleMicrosoftLogin(Page page) {
        log.info("Đang xử lý trang đăng nhập Microsoft Account...");
        try {
            // Check for previous errors on page
            checkMicrosoftErrors(page);

            // 0. Account picker tile if user previously signed in or suggested
            Locator accountTile = page.locator(String.format("div:has-text('%s'), small:has-text('%s'), div[data-test-id='%s']",
                    config.getMicrosoftUsername(), config.getMicrosoftUsername(), config.getMicrosoftUsername()));
            if (accountTile.count() > 0 && accountTile.first().isVisible()) {
                log.info("Phát hiện tài khoản có sẵn trên Microsoft account picker, đang chọn: {}", config.getMicrosoftUsername());
                accountTile.first().click();
                page.waitForTimeout(2000);
                checkMicrosoftErrors(page);
            }

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

    private boolean isLikelyJwtAccessToken(String token) {
        if (token == null || !token.startsWith("eyJ") || token.split("\\.").length != 3) {
            return false;
        }
        try {
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]);
            JsonNode payload = mapper.readTree(decoded);
            // Must have exp and cannot be KC_RESTART / internal Keycloak action
            if (!payload.has("exp")) {
                return false;
            }
            if (payload.has("act") && "AUTHENTICATE".equalsIgnoreCase(payload.get("act").asText())) {
                return false;
            }
            return payload.has("sub") || payload.has("employeeid") || payload.has("role") || payload.has("permissions");
        } catch (Exception e) {
            return false;
        }
    }

    private Path findSystemBrowserPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String[] candidatePaths = {
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                localAppData != null ? localAppData + "\\Microsoft\\Edge\\Application\\msedge.exe" : null,
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                localAppData != null ? localAppData + "\\Google\\Chrome\\Application\\chrome.exe" : null
        };
        for (String p : candidatePaths) {
            if (p != null) {
                try {
                    Path path = Paths.get(p);
                    if (Files.isRegularFile(path)) {
                        return path;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
