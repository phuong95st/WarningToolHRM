package com.gm.warning.engine;

import com.gm.warning.config.AppConfig;
import com.gm.warning.model.AttendanceRecord;
import com.gm.warning.model.AttendanceResponse;
import com.gm.warning.model.HolidayItem;
import com.gm.warning.notification.TelegramTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AttendanceRuleEngine {
    private static final Logger log = LoggerFactory.getLogger(AttendanceRuleEngine.class);
    private static final Logger attendanceLogger = LoggerFactory.getLogger("ATTENDANCE_LOGGER");

    private final AppConfig config;

    // Daily state cache to prevent spamming
    private LocalDate lastStateDate = LocalDate.now();
    private boolean checkInConfirmedSent = false;
    private boolean minimumHoursSent = false;
    private String lastAlertedCheckOut = null; // Lưu giờ check-out cuối cùng đã cảnh báo
    private int checkOutAttemptCount = 0;       // Đếm số lần check-out trong ngày
    private boolean fullWorkConfirmedSent = false;
    private boolean reCheckOutRemindedAtTarget = false;
    private String lastMissingCheckInCheckOutAlerted = null;
    private boolean noonReminderSent = false;
    private final Set<String> sentMorningReminders = new HashSet<>();
    private final Set<String> sentEveningReminders = new HashSet<>();
    private LocalTime lastLateCheckInAlertTime = null;
    private LocalTime lastLateCheckOutAlertTime = null;

    public AttendanceRuleEngine(AppConfig config) {
        this.config = config;
    }

    public synchronized List<RuleAlert> evaluate(AttendanceResponse response) {
        return evaluate(response, null);
    }

    public synchronized List<RuleAlert> evaluate(AttendanceResponse response, HolidayItem todayHoliday) {
        return evaluate(response, todayHoliday, LocalDate.now(), LocalTime.now());
    }

    public synchronized List<RuleAlert> evaluate(AttendanceResponse response, HolidayItem todayHoliday, LocalDate today, LocalTime now) {
        List<RuleAlert> alerts = new ArrayList<>();

        // Reset daily state at new day
        if (!today.equals(lastStateDate)) {
            log.info("Resetting daily notification state for new day: {}", today);
            lastStateDate = today;
            checkInConfirmedSent = false;
            minimumHoursSent = false;
            lastAlertedCheckOut = null;
            checkOutAttemptCount = 0;
            fullWorkConfirmedSent = false;
            reCheckOutRemindedAtTarget = false;
            lastMissingCheckInCheckOutAlerted = null;
            noonReminderSent = false;
            sentMorningReminders.clear();
            sentEveningReminders.clear();
            lastLateCheckInAlertTime = null;
            lastLateCheckOutAlertTime = null;
        }

        // 1. Check weekend (Saturday / Sunday)
        boolean isWeekday = (today.getDayOfWeek() != DayOfWeek.SATURDAY && today.getDayOfWeek() != DayOfWeek.SUNDAY);
        if (!isWeekday) {
            log.debug("Hôm nay là cuối tuần ({}), bỏ qua cảnh báo chấm công.", today.getDayOfWeek());
            return alerts;
        }

        // 2. Check official company holiday
        String todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (todayHoliday != null) {
            log.info("Hôm nay [{}] là ngày nghỉ lễ công ty: '{}' (Có lương: {}). Bỏ qua cảnh báo chấm công.",
                    todayStr, todayHoliday.getName(), todayHoliday.isPaid());
            attendanceLogger.info("Hôm nay [{}]: NGHỈ LỄ [{}] — Không áp dụng cảnh báo chấm công.",
                    todayStr, todayHoliday.getName());
            return alerts;
        }

        if (response == null || !response.isSuccess() || response.getData() == null) {
            log.warn("Attendance response is null or unsuccessful. Skipping evaluation.");
            return alerts;
        }

        // Find today's record
        AttendanceRecord todayRecord = response.getData().stream()
                .filter(r -> todayStr.equals(r.getWorkDate()))
                .findFirst()
                .orElse(null);

        // Log daily summary
        if (todayRecord != null) {
            attendanceLogger.info("Hôm nay [{}]: Check-in={}, Check-out={}, Trạng thái={}",
                    todayStr,
                    todayRecord.getCheckIn() != null ? todayRecord.getCheckIn() : "Chưa",
                    todayRecord.getCheckOut() != null ? todayRecord.getCheckOut() : "Chưa",
                    todayRecord.getStatus());
        } else {
            attendanceLogger.info("Hôm nay [{}]: Chưa có bản ghi chấm công trên hệ thống", todayStr);
        }

        // TRƯỜNG HỢP 1: Chưa có Check-in và cũng chưa có Check-out (Hoặc chưa có bản ghi)
        if (todayRecord == null || (!todayRecord.isCheckedIn() && !todayRecord.isCheckedOut())) {
            evaluateMorningMissingCheckIn(now, alerts);
        }
        // TRƯỜNG HỢP 2: Sáng KHÔNG Check-in mà lại có Check-out!
        else if (!todayRecord.isCheckedIn() && todayRecord.isCheckedOut()) {
            String currentCheckOutStr = (todayRecord.getCheckOutTime() != null)
                    ? todayRecord.getCheckOutTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    : todayRecord.getCheckOut().trim();

            if (!currentCheckOutStr.equals(lastMissingCheckInCheckOutAlerted)) {
                lastMissingCheckInCheckOutAlerted = currentCheckOutStr;
                alerts.add(new RuleAlert(
                        AlertLevel.URGENT,
                        "Cảnh báo thiếu Check-in sáng",
                        TelegramTemplate.missingCheckInWithCheckOut(
                                todayRecord.getCheckOut(),
                                todayRecord.getStatus()
                        )
                ));
            }
        }
        // TRƯỜNG HỢP 3: Có Check-in nhưng CHƯA Check-out (Đang làm việc)
        else if (todayRecord.isCheckedIn() && !todayRecord.isCheckedOut()) {
            LocalTime checkInTime = todayRecord.getCheckInTime();

            // Gửi xác nhận check-in 1 lần
            if (!checkInConfirmedSent && checkInTime != null) {
                checkInConfirmedSent = true;
                LocalTime earliestOut = checkInTime.plusHours(config.getRequiredSpanHours());
                String earliestOutStr = earliestOut.format(DateTimeFormatter.ofPattern("HH:mm"));
                alerts.add(new RuleAlert(
                        AlertLevel.INFO,
                        "Check-in thành công",
                        TelegramTemplate.checkInSuccess(todayRecord.getCheckIn(), earliestOutStr, todayRecord.getStatus())
                ));
            }

            // Nhắc Check-out nghỉ trưa (nếu chiều có thể nghỉ không đi làm)
            evaluateNoonCheckOut(now, checkInTime, alerts);

            // Đánh giá các mốc nhắc nhở buổi chiều/tối
            evaluateEveningCheckOut(now, checkInTime, alerts);
        }
        // TRƯỜNG HỢP 4: Đã Check-in và ĐÃ Check-out (Hỗ trợ check-out nhiều lần trong ngày)!
        else if (todayRecord.isCheckedIn() && todayRecord.isCheckedOut()) {
            LocalTime checkInTime = todayRecord.getCheckInTime();
            LocalTime checkOutTime = todayRecord.getCheckOutTime();
            LocalTime earliestOut = checkInTime != null
                    ? checkInTime.plusHours(config.getRequiredSpanHours())
                    : config.getEarliestStart().plusHours(config.getRequiredSpanHours());

            boolean isEarly = (checkOutTime != null && checkOutTime.isBefore(earliestOut));
            boolean isFullWork = todayRecord.isFullWork() || (!isEarly && "ĐỦ CÔNG".equalsIgnoreCase(todayRecord.getStatus()));

            String currentCheckOutStr = (checkOutTime != null)
                    ? checkOutTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                    : todayRecord.getCheckOut().trim();

            boolean isNewCheckOutTime = !currentCheckOutStr.equals(lastAlertedCheckOut);

            if (isNewCheckOutTime) {
                lastAlertedCheckOut = currentCheckOutStr;
                checkOutAttemptCount++;

                long minutesWorked = (checkInTime != null && checkOutTime != null)
                        ? Math.max(0, java.time.Duration.between(checkInTime, checkOutTime).toMinutes())
                        : 0;
                long h = minutesWorked / 60;
                long m = minutesWorked % 60;
                String earliestOutStr = earliestOut.format(DateTimeFormatter.ofPattern("HH:mm"));

                if (!isFullWork) {
                    long minsRemaining = (checkOutTime != null && checkOutTime.isBefore(earliestOut))
                            ? Math.max(0, java.time.Duration.between(checkOutTime, earliestOut).toMinutes())
                            : 0;

                    String alertTitle = (checkOutAttemptCount == 1)
                            ? "Cảnh báo Check-out chưa đủ công"
                            : "Cảnh báo Check-out lại (lần " + checkOutAttemptCount + ") vẫn chưa đủ công";

                    alerts.add(new RuleAlert(
                            AlertLevel.WARNING,
                            alertTitle,
                            TelegramTemplate.earlyCheckOutIncomplete(
                                    todayRecord.getCheckIn(),
                                    currentCheckOutStr,
                                    earliestOutStr,
                                    h,
                                    m,
                                    todayRecord.getStatus(),
                                    checkOutAttemptCount,
                                    minsRemaining
                            )
                    ));
                } else {
                    fullWorkConfirmedSent = true;
                    String alertTitle = (checkOutAttemptCount > 1)
                            ? "Check-out lại thành công & Đã đủ công"
                            : "Check-out thành công (Đủ công)";

                    alerts.add(new RuleAlert(
                            AlertLevel.INFO,
                            alertTitle,
                            TelegramTemplate.checkOutSuccess(
                                    todayRecord.getCheckIn(),
                                    currentCheckOutStr,
                                    h,
                                    m,
                                    todayRecord.getStatus(),
                                    checkOutAttemptCount
                            )
                    ));
                }
            }

            // Nếu trạng thái VẪN CHƯA ĐỦ CÔNG (ví dụ người dùng bấm về sớm lúc 16:30 và vẫn ngồi ở công ty):
            if (!isFullWork) {
                String checkInStr = checkInTime != null ? checkInTime.format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
                String earliestOutStr = earliestOut.format(DateTimeFormatter.ofPattern("HH:mm"));

                // Khi đồng hồ điểm đủ 8 tiếng (ví dụ 17:50) mà chưa check-out lại đủ công
                if (!reCheckOutRemindedAtTarget && now.isAfter(earliestOut)) {
                    reCheckOutRemindedAtTarget = true;
                    minimumHoursSent = true; // Tránh bắn trùng lặp với thông báo chung Đã đủ giờ làm việc
                    alerts.add(new RuleAlert(
                            AlertLevel.WARNING,
                            "Nhắc Check-out lại (Đã đủ 8h)",
                            TelegramTemplate.remindReCheckOutAtTarget(
                                    checkInStr,
                                    now.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    lastAlertedCheckOut,
                                    earliestOutStr
                            )
                    ));
                }

                // Tiếp tục nhắc nhở các mốc chiều/tối (18:05, 18:30...) để không quên check-out lại
                evaluateEveningCheckOut(now, checkInTime, alerts);
            }
        }

        return alerts;
    }

    private void evaluateMorningMissingCheckIn(LocalTime now, List<RuleAlert> alerts) {
        String latestStartStr = config.getLatestStart().format(DateTimeFormatter.ofPattern("HH:mm"));
        String nowStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Between 08:30 and 09:00: Check morning reminders
        for (LocalTime reminderTime : config.getMorningReminders()) {
            String timeKey = reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (now.isAfter(reminderTime.minusMinutes(1)) && now.isBefore(reminderTime.plusMinutes(3))) {
                if (!sentMorningReminders.contains(timeKey)) {
                    sentMorningReminders.add(timeKey);
                    alerts.add(new RuleAlert(
                            AlertLevel.WARNING,
                            "Nhắc nhở Check-in (" + timeKey + ")",
                            TelegramTemplate.morningReminder(timeKey, latestStartStr)
                    ));
                }
            }
        }

        // After 09:00: RED ALERT
        if (now.isAfter(config.getLatestStart())) {
            // Alert every 20 minutes
            if (lastLateCheckInAlertTime == null || now.isAfter(lastLateCheckInAlertTime.plusMinutes(20))) {
                lastLateCheckInAlertTime = now;
                alerts.add(new RuleAlert(
                        AlertLevel.URGENT,
                        "CẢNH BÁO: CHƯA CHECK-IN!",
                        TelegramTemplate.lateCheckInAlert(nowStr, latestStartStr, config.getHrmUrl())
                ));
            }
        }
    }

    private void evaluateNoonCheckOut(LocalTime now, LocalTime checkInTime, List<RuleAlert> alerts) {
        if (noonReminderSent) {
            return;
        }

        LocalTime noonTime = config.getNoonReminder();
        if (noonTime == null) {
            noonTime = LocalTime.of(12, 0);
        }

        // Kích hoạt khi đồng hồ tới mốc nhắc trưa (12:00) trong khung giờ nghỉ trưa 12:00 - 13:00
        LocalTime lunchEnd = LocalTime.of(13, 0);
        if (!now.isBefore(noonTime) && now.isBefore(lunchEnd)) {
            noonReminderSent = true;
            String checkInStr = checkInTime != null ? checkInTime.format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
            String nowStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
            alerts.add(new RuleAlert(
                    AlertLevel.WARNING,
                    "Nhắc Check-out nghỉ trưa (" + nowStr + ")",
                    TelegramTemplate.noonReminder(nowStr, checkInStr)
            ));
        }
    }

    private void evaluateEveningCheckOut(LocalTime now, LocalTime checkInTime, List<RuleAlert> alerts) {
        LocalTime earliestOut = checkInTime != null
                ? checkInTime.plusHours(config.getRequiredSpanHours())
                : config.getEarliestStart().plusHours(config.getRequiredSpanHours());
        String checkInStr = checkInTime != null ? checkInTime.format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
        String earliestOutStr = earliestOut.format(DateTimeFormatter.ofPattern("HH:mm"));
        String nowStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        // Notify once when user reaches the required 8 working hours (span of 9 hours)
        if (!minimumHoursSent && now.isAfter(earliestOut)) {
            minimumHoursSent = true;
            alerts.add(new RuleAlert(
                    AlertLevel.INFO,
                    "Đã đủ giờ làm việc",
                    TelegramTemplate.minimumHoursReached(checkInStr, earliestOutStr)
            ));
        }

        // Check evening reminders
        for (LocalTime reminderTime : config.getEveningReminders()) {
            String timeKey = reminderTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            if (now.isAfter(reminderTime.minusMinutes(1)) && now.isBefore(reminderTime.plusMinutes(3))) {
                if (!sentEveningReminders.contains(timeKey)) {
                    sentEveningReminders.add(timeKey);
                    alerts.add(new RuleAlert(
                            AlertLevel.WARNING,
                            "Nhắc Check-out (" + timeKey + ")",
                            TelegramTemplate.eveningReminder(timeKey, nowStr, checkInStr)
                    ));
                }
            }
        }

        // Late evening after 19:00: Alert every 30 minutes
        if (now.isAfter(LocalTime.of(19, 0))) {
            if (lastLateCheckOutAlertTime == null || now.isAfter(lastLateCheckOutAlertTime.plusMinutes(30))) {
                lastLateCheckOutAlertTime = now;
                alerts.add(new RuleAlert(
                        AlertLevel.URGENT,
                        "CẢNH BÁO: QUÊN CHECK-OUT?",
                        TelegramTemplate.lateEveningAlert(nowStr, checkInStr)
                ));
            }
        }
    }

    /**
     * Checks history of the month for days with incomplete attendance.
     */
    public List<AttendanceRecord> findIncompleteHistoryDays(AttendanceResponse response) {
        List<AttendanceRecord> incomplete = new ArrayList<>();
        if (response == null || response.getData() == null) return incomplete;

        LocalDate today = LocalDate.now();
        for (AttendanceRecord record : response.getData()) {
            LocalDate date = record.getWorkLocalDate();
            if (date != null && date.isBefore(today)) {
                // Not full work
                if (!"ĐỦ CÔNG".equalsIgnoreCase(record.getStatus())) {
                    incomplete.add(record);
                }
            }
        }
        return incomplete;
    }

    public enum AlertLevel {
        INFO, WARNING, URGENT
    }

    public static class RuleAlert {
        private final AlertLevel level;
        private final String title;
        private final String message;

        public RuleAlert(AlertLevel level, String title, String message) {
            this.level = level;
            this.title = title;
            this.message = message;
        }

        public AlertLevel getLevel() { return level; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
    }
}
