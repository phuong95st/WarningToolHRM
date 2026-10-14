package com.gm.warning.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.warning.config.AppConfig;
import com.gm.warning.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HrmApiClient {
    private static final Logger log = LoggerFactory.getLogger(HrmApiClient.class);
    private static final Logger attendanceLogger = LoggerFactory.getLogger("ATTENDANCE_LOGGER");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final AppConfig config;
    private final TokenCache tokenCache;
    private final SsoLoginService ssoLoginService;
    private final HttpClient httpClient;

    private String lastStatusDescription = "Khởi tạo";
    private List<HolidayItem> cachedHolidays = new ArrayList<>();
    private LocalDate lastHolidaysFetchDate = null;

    public HrmApiClient(AppConfig config, TokenCache tokenCache, SsoLoginService ssoLoginService) {
        this.config = config;
        this.tokenCache = tokenCache;
        this.ssoLoginService = ssoLoginService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetch attendance records for the given year and month.
     * Automatically refreshes token via SSO if expired or rejected with 401.
     */
    public AttendanceResponse fetchAttendance(int year, int month) {
        ensureValidToken();

        String token = tokenCache.getToken();
        if (token == null || token.isBlank()) {
            String msg = "Không có Token để gọi API (Đăng nhập SSO chưa thành công)";
            log.error("[LỖI XÁC THỰC] {}", msg);
            lastStatusDescription = msg;
            return null;
        }

        AttendanceResponse response = sendRequest(year, month, token);

        // If 401 Unauthorized, re-login once and retry
        if (response == null && "401".equals(lastStatusDescription)) {
            log.warn("[401 UNAUTHORIZED] Token hết hạn hoặc không hợp lệ. Đang kích hoạt đăng nhập lại SSO qua Playwright...");
            String newToken = ssoLoginService.performSsoLogin();
            if (newToken != null) {
                log.info("Lấy lại token mới thành công, thử gọi lại API HRM...");
                return sendRequest(year, month, newToken);
            } else {
                log.error("[ĐĂNG NHẬP THẤT BẠI] Không thể tái đăng nhập SSO để lấy token mới.");
            }
        }

        return response;
    }

    public AttendanceResponse fetchCurrentMonthAttendance() {
        LocalDate now = LocalDate.now();
        return fetchAttendance(now.getYear(), now.getMonthValue());
    }

    private void ensureValidToken() {
        if (!tokenCache.isValid(config.getMicrosoftUsername())) {
            log.info("Token hiện tại đã hết hạn hoặc không khớp nhân viên [{}]. Đang kích hoạt đăng nhập SSO...",
                    config.getMicrosoftUsername());
            ssoLoginService.performSsoLogin();
        }
    }

    private AttendanceResponse sendRequest(int year, int month, String token) {
        String url = String.format("%s%s?year=%d&month=%d",
                config.getHrmUrl(),
                config.getAttendanceApiPath(),
                year,
                month);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = httpResponse.statusCode();

            if (statusCode == 200) {
                lastStatusDescription = "Kết nối API bình thường (HTTP 200)";
                return mapper.readValue(httpResponse.body(), AttendanceResponse.class);
            } else if (statusCode == 401) {
                lastStatusDescription = "401";
                log.warn("[LỖI XÁC THỰC HTTP 401] Token bị từ chối bởi máy chủ HRM: {}", url);
                return null;
            } else if (statusCode >= 500) {
                String errorMsg = String.format("Máy chủ HRM gặp sự cố hoặc đang bảo trì (HTTP %d %s)",
                        statusCode, httpResponse.body());
                lastStatusDescription = errorMsg;
                log.error("[LỖI MÁY CHỦ HRM] {}", errorMsg);
                attendanceLogger.warn("[CẢNH BÁO HỆ THỐNG] {}", errorMsg);
            } else {
                String errorMsg = String.format("Gọi API HRM thất bại (HTTP %d): %s", statusCode, httpResponse.body());
                lastStatusDescription = errorMsg;
                log.error("[LỖI API HRM] {}", errorMsg);
            }
        } catch (HttpTimeoutException e) {
            String msg = "Máy chủ HRM không phản hồi trong 15 giây (Timeout - mạng quá tải hoặc máy chủ treo)";
            lastStatusDescription = msg;
            log.error("[LỖI TIMEOUT] {} tại URL: {}", msg, url);
            attendanceLogger.warn("[MẤT KẾT NỐI] {}", msg);
        } catch (UnknownHostException e) {
            String msg = "Không thể phân giải tên miền hrm.gm-group.vn (Mất kết nối Internet hoặc lỗi DNS)";
            lastStatusDescription = msg;
            log.error("[LỖI DNS/MẠNG] {}", msg);
            attendanceLogger.warn("[MẤT KẾT NỐI] {}", msg);
        } catch (ConnectException e) {
            String msg = "Không thể kết nối đến máy chủ HRM (Connection Refused - Máy chủ HRM đang sập hoặc bị chặn firewall)";
            lastStatusDescription = msg;
            log.error("[LỖI KẾT NỐI] {}", msg);
            attendanceLogger.warn("[MẤT KẾT NỐI] {}", msg);
        } catch (Exception e) {
            String msg = "Lỗi khi gọi API HRM: " + e.getMessage();
            lastStatusDescription = msg;
            log.error("[LỖI KHÔNG XÁC ĐỊNH] {} - Chi tiết:", msg, e);
        }
        return null;
    }

    public String getLastStatusDescription() {
        return lastStatusDescription;
    }

    /**
     * Tải danh sách ngày lễ từ API HRM
     */
    public synchronized List<HolidayItem> fetchHolidays() {
        ensureValidToken();
        String token = tokenCache.getToken();
        if (token == null || token.isBlank()) return cachedHolidays;

        String url = config.getHrmUrl() + config.getHolidaysApiPath();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                HolidaysResponse res = mapper.readValue(httpResponse.body(), HolidaysResponse.class);
                if (res != null && res.isSuccess() && res.getData() != null) {
                    cachedHolidays = res.getData();
                    lastHolidaysFetchDate = LocalDate.now();
                    log.info("Lấy thành công danh sách {} ngày lễ từ HRM: {}",
                            cachedHolidays.size(),
                            cachedHolidays.stream().map(HolidayItem::getName).toList());
                }
            } else {
                log.warn("Gọi API ngày lễ thất bại với HTTP {}: {}", httpResponse.statusCode(), httpResponse.body());
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải danh sách ngày lễ: {}", e.getMessage());
        }
        return cachedHolidays;
    }

    /**
     * Kiểm tra xem ngày chỉ định có phải là ngày nghỉ lễ không.
     * @return HolidayItem nếu là ngày lễ, hoặc null nếu không phải
     */
    public synchronized HolidayItem getHolidayForDate(LocalDate date) {
        if (date == null) return null;
        if (lastHolidaysFetchDate == null || !lastHolidaysFetchDate.equals(date)) {
            fetchHolidays();
        }
        String targetDateStr = date.toString();
        for (HolidayItem h : cachedHolidays) {
            if (!h.isDeleted() && targetDateStr.equals(h.getHolidayDate())) {
                return h;
            }
        }
        return null;
    }

    /**
     * Tải danh sách thông báo từ API /api/notifications/mine
     */
    public synchronized List<com.gm.warning.model.HrmNotificationItem> fetchNotifications() {
        ensureValidToken();
        String token = tokenCache.getToken();
        if (token == null || token.isBlank()) return java.util.Collections.emptyList();

        String url = config.getHrmUrl() + config.getNotificationsApiPath();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                com.gm.warning.model.HrmNotificationsResponse res =
                        mapper.readValue(httpResponse.body(), com.gm.warning.model.HrmNotificationsResponse.class);
                if (res != null && res.isSuccess() && res.getData() != null) {
                    return res.getData();
                }
            } else {
                log.warn("Gọi API thông báo thất bại với HTTP {}: {}", httpResponse.statusCode(), httpResponse.body());
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải thông báo HRM: {}", e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Tải thông tin số dư ngày phép theo năm từ /api/leave/balance?year={year}
     */
    public synchronized LeaveBalanceData fetchLeaveBalance(int year) {
        ensureValidToken();
        String token = tokenCache.getToken();
        if (token == null || token.isBlank()) return null;

        String url = String.format("%s%s?year=%d", config.getHrmUrl(), config.getLeaveBalanceApiPath(), year);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                LeaveBalanceResponse res = mapper.readValue(httpResponse.body(), LeaveBalanceResponse.class);
                if (res != null && res.isSuccess() && res.getData() != null) {
                    return res.getData();
                }
            } else if (httpResponse.statusCode() == 401) {
                log.warn("[401] Token hết hạn khi gọi API phép. Thử login lại...");
                String newToken = ssoLoginService.performSsoLogin();
                if (newToken != null) {
                    return fetchLeaveBalance(year);
                }
            } else {
                log.warn("Gọi API số dư phép thất bại HTTP {}: {}", httpResponse.statusCode(), httpResponse.body());
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi API số dư phép: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Tải danh sách đơn làm thêm giờ (OT) của tôi từ /api/overtime/requests/mine
     */
    public synchronized List<OvertimeItem> fetchOvertimeRequests() {
        ensureValidToken();
        String token = tokenCache.getToken();
        if (token == null || token.isBlank()) return Collections.emptyList();

        String url = config.getHrmUrl() + config.getOvertimeApiPath();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() == 200) {
                OvertimeResponse res = mapper.readValue(httpResponse.body(), OvertimeResponse.class);
                if (res != null && res.isSuccess() && res.getData() != null) {
                    return res.getData();
                }
            } else if (httpResponse.statusCode() == 401) {
                log.warn("[401] Token hết hạn khi gọi API OT. Thử login lại...");
                String newToken = ssoLoginService.performSsoLogin();
                if (newToken != null) {
                    return fetchOvertimeRequests();
                }
            } else {
                log.warn("Gọi API OT thất bại HTTP {}: {}", httpResponse.statusCode(), httpResponse.body());
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi API OT: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
