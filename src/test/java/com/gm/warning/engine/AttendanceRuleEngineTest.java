package com.gm.warning.engine;

import com.gm.warning.config.AppConfig;
import com.gm.warning.model.AttendanceRecord;
import com.gm.warning.model.AttendanceResponse;
import com.gm.warning.model.HolidayItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AttendanceRuleEngineTest {

    private AppConfig config;
    private AttendanceRuleEngine engine;

    @BeforeEach
    void setUp() {
        config = new AppConfig();
        engine = new AttendanceRuleEngine(config);
    }

    @Test
    @DisplayName("Cuối tuần (Thứ 7 / Chủ Nhật) không được phát cảnh báo nào")
    void testWeekendNoAlerts() {
        LocalDate saturday = LocalDate.of(2026, 10, 17); // Saturday
        LocalTime morningTime = LocalTime.of(8, 45);

        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(new ArrayList<>());

        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, null, saturday, morningTime);
        assertTrue(alerts.isEmpty(), "Thứ 7 không được bắn cảnh báo chấm công");
    }

    @Test
    @DisplayName("Ngày nghỉ lễ công ty không được phát cảnh báo nào")
    void testHolidayNoAlerts() {
        LocalDate weekday = LocalDate.of(2026, 9, 2); // Wednesday - Quốc Khánh
        LocalTime morningTime = LocalTime.of(8, 45);

        HolidayItem holiday = new HolidayItem();
        holiday.setName("Nghỉ lễ Quốc Khánh");
        holiday.setHolidayDate("2026-09-02");
        holiday.setPaid(true);

        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(new ArrayList<>());

        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, holiday, weekday, morningTime);
        assertTrue(alerts.isEmpty(), "Ngày nghỉ lễ không được bắn cảnh báo");
    }

    @Test
    @DisplayName("Chưa check-in vào mốc 08:45 phải phát cảnh báo nhắc nhở sáng")
    void testMorningReminderAt0845() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13); // Tuesday
        LocalTime time0845 = LocalTime.of(8, 45);

        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(new ArrayList<>()); // No check-in today

        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, null, tuesday, time0845);
        assertFalse(alerts.isEmpty(), "08:45 chưa check-in phải có cảnh báo nhắc nhở");
        assertEquals("Nhắc nhở Check-in (08:45)", alerts.get(0).getTitle());
    }

    @Test
    @DisplayName("Chống spam: Cảnh báo nhắc nhở cùng mốc không gửi 2 lần trong ngày")
    void testAntiSpamMorningReminder() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);
        LocalTime time0845 = LocalTime.of(8, 45);

        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(new ArrayList<>());

        List<AttendanceRuleEngine.RuleAlert> firstCall = engine.evaluate(response, null, tuesday, time0845);
        assertEquals(1, firstCall.size());

        // Call again 1 minute later at 08:46
        List<AttendanceRuleEngine.RuleAlert> secondCall = engine.evaluate(response, null, tuesday, LocalTime.of(8, 46));
        assertTrue(secondCall.isEmpty(), "Không được gửi lặp lại cảnh báo mốc 08:45");
    }

    @Test
    @DisplayName("Sau 09:00 chưa check-in phải phát báo động đỏ khẩn cấp")
    void testLateCheckInRedAlert() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);
        LocalTime time0905 = LocalTime.of(9, 5);

        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(new ArrayList<>());

        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, null, tuesday, time0905);
        assertFalse(alerts.isEmpty());
        assertEquals(AttendanceRuleEngine.AlertLevel.URGENT, alerts.get(0).getLevel());
        assertTrue(alerts.get(0).getMessage().toLowerCase().contains("quá giờ chấm công"));
    }

    @Test
    @DisplayName("Khi vừa check-in lúc 08:44 thì tính giờ về tối thiểu là 17:44 và gửi thông báo xác nhận")
    void testCheckInConfirmed() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);
        LocalTime time0850 = LocalTime.of(8, 50);

        AttendanceRecord record = new AttendanceRecord("ĐANG LÀM VIỆC", "2026-10-13", "08:44", null);
        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(List.of(record));

        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, null, tuesday, time0850);
        assertEquals(1, alerts.size());
        assertEquals(AttendanceRuleEngine.AlertLevel.INFO, alerts.get(0).getLevel());
        assertTrue(alerts.get(0).getMessage().contains("17:44"));
        assertTrue(alerts.get(0).getMessage().toLowerCase().contains("check-in thành công"));
    }

    @Test
    @DisplayName("Đến 17:44 (vào 08:44) phát thông báo hoàn thành đủ 8 tiếng")
    void testReached8Hours() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);
        AttendanceRecord record = new AttendanceRecord("ĐANG LÀM VIỆC", "2026-10-13", "08:44", null);
        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(List.of(record));

        // Evaluate earlier at 09:00 so check-in confirmation is already consumed
        engine.evaluate(response, null, tuesday, LocalTime.of(9, 0));

        // Now evaluate at 17:45
        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, null, tuesday, LocalTime.of(17, 45));
        assertEquals(1, alerts.size());
        assertEquals("Đã đủ giờ làm việc", alerts.get(0).getTitle());
        assertTrue(alerts.get(0).getMessage().toLowerCase().contains("đã đủ 8h làm việc"));
    }

    @Test
    @DisplayName("Buổi chiều mốc 18:05 chưa check-out phải phát thông báo nhắc nhở")
    void testEveningReminderAt1805() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);
        AttendanceRecord record = new AttendanceRecord("ĐANG LÀM VIỆC", "2026-10-13", "08:44", null);
        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(List.of(record));

        // Consume earlier notifications
        engine.evaluate(response, null, tuesday, LocalTime.of(9, 0));
        engine.evaluate(response, null, tuesday, LocalTime.of(17, 45));

        // Evaluate at 18:05
        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, null, tuesday, LocalTime.of(18, 5));
        assertEquals(1, alerts.size());
        assertEquals("Nhắc Check-out (18:05)", alerts.get(0).getTitle());
    }

    @Test
    @DisplayName("Đã check-out đầy đủ và đủ công thì không cảnh báo chiều")
    void testCompletedWorkNoAlert() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);
        AttendanceRecord record = new AttendanceRecord("ĐỦ CÔNG", "2026-10-13", "08:44", "18:10");
        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(List.of(record));

        // Consume check-in confirmation
        engine.evaluate(response, null, tuesday, LocalTime.of(9, 0));

        // Evaluate at 18:30
        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, null, tuesday, LocalTime.of(18, 30));
        assertTrue(alerts.isEmpty(), "Đã đủ công không được cảnh báo nhắc check-out");
    }

    @Test
    @DisplayName("Sáng quên check-in nhưng chiều có check-out -> Phát cảnh báo khẩn cấp nhắc làm đơn giải trình")
    void testMissingCheckInWithCheckOut() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);
        LocalTime time1815 = LocalTime.of(18, 15);

        // Record has checkout 18:10 but NO check-in
        AttendanceRecord record = new AttendanceRecord("CA CHIỀU", "2026-10-13", null, "18:10");
        AttendanceResponse response = new AttendanceResponse();
        response.setSuccess(true);
        response.setData(List.of(record));

        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(response, null, tuesday, time1815);
        assertEquals(1, alerts.size());
        assertEquals(AttendanceRuleEngine.AlertLevel.URGENT, alerts.get(0).getLevel());
        assertTrue(alerts.get(0).getTitle().contains("thiếu Check-in"));
        assertTrue(alerts.get(0).getMessage().toLowerCase().contains("thiếu check-in sáng"));
        assertTrue(alerts.get(0).getMessage().contains("18:10"));

        // Anti-spam verification: calling again should not fire
        List<AttendanceRuleEngine.RuleAlert> secondCall = engine.evaluate(response, null, tuesday, LocalTime.of(18, 20));
        assertTrue(secondCall.isEmpty(), "Không được gửi lặp lại cảnh báo thiếu check-in có check-out");
    }

    @Test
    @DisplayName("Có check-in và check-out nhưng về sớm / không đủ công -> Cảnh báo nhắc nhở")
    void testEarlyCheckOutIncomplete() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);

        // In the morning (09:00): checked in at 08:50, not checked out yet
        AttendanceRecord morningRecord = new AttendanceRecord("ĐANG LÀM VIỆC", "2026-10-13", "08:50", null);
        AttendanceResponse morningResponse = new AttendanceResponse();
        morningResponse.setSuccess(true);
        morningResponse.setData(List.of(morningRecord));
        engine.evaluate(morningResponse, null, tuesday, LocalTime.of(9, 0));

        // In the afternoon (16:35): checked out early at 16:30 (requires 17:50) -> status is CA SÁNG or not ĐỦ CÔNG
        LocalTime time1635 = LocalTime.of(16, 35);
        AttendanceRecord afternoonRecord = new AttendanceRecord("CA SÁNG", "2026-10-13", "08:50", "16:30");
        AttendanceResponse afternoonResponse = new AttendanceResponse();
        afternoonResponse.setSuccess(true);
        afternoonResponse.setData(List.of(afternoonRecord));

        List<AttendanceRuleEngine.RuleAlert> alerts = engine.evaluate(afternoonResponse, null, tuesday, time1635);
        assertEquals(1, alerts.size());
        assertEquals(AttendanceRuleEngine.AlertLevel.WARNING, alerts.get(0).getLevel());
        assertTrue(alerts.get(0).getTitle().contains("chưa đủ công"));
        assertTrue(alerts.get(0).getMessage().toLowerCase().contains("check-out chưa đủ công"));
        assertTrue(alerts.get(0).getMessage().contains("08:50"));
        assertTrue(alerts.get(0).getMessage().contains("16:30"));
        assertTrue(alerts.get(0).getMessage().contains("17:50")); // required minimum checkout

        // Anti-spam verification: calling again should not fire
        List<AttendanceRuleEngine.RuleAlert> secondCall = engine.evaluate(afternoonResponse, null, tuesday, LocalTime.of(16, 40));
        assertTrue(secondCall.isEmpty(), "Không được gửi lặp lại cảnh báo chưa đủ công");
    }

    @Test
    @DisplayName("Kịch bản đa check-out: Lần 1, 2 chưa đủ công -> Lần 3 đủ công -> Xác nhận hoàn thành và chống spam")
    void testMultipleCheckOuts_IncompleteThenFullWork() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);

        // Morning (09:00): Check-in at 08:50 (requires 17:50 to be 8h full work)
        AttendanceRecord recMorning = new AttendanceRecord("ĐANG LÀM VIỆC", "2026-10-13", "08:50", null);
        AttendanceResponse respMorning = new AttendanceResponse();
        respMorning.setSuccess(true);
        respMorning.setData(List.of(recMorning));
        engine.evaluate(respMorning, null, tuesday, LocalTime.of(9, 0));

        // LẦN 1: Check-out sớm lúc 16:30 (Chưa đủ công)
        AttendanceRecord rec1 = new AttendanceRecord("CA SÁNG", "2026-10-13", "08:50", "16:30");
        AttendanceResponse resp1 = new AttendanceResponse();
        resp1.setSuccess(true);
        resp1.setData(List.of(rec1));

        List<AttendanceRuleEngine.RuleAlert> alerts1 = engine.evaluate(resp1, null, tuesday, LocalTime.of(16, 31));
        assertEquals(1, alerts1.size());
        assertEquals("Cảnh báo Check-out chưa đủ công", alerts1.get(0).getTitle());
        assertTrue(alerts1.get(0).getMessage().contains("16:30"));
        assertTrue(alerts1.get(0).getMessage().contains("thiếu: <b>80 phút</b>"));

        // LẦN 2: Bấm Check-out lại lúc 17:15 (Vẫn chưa đủ công)
        AttendanceRecord rec2 = new AttendanceRecord("CA SÁNG", "2026-10-13", "08:50", "17:15");
        AttendanceResponse resp2 = new AttendanceResponse();
        resp2.setSuccess(true);
        resp2.setData(List.of(rec2));

        List<AttendanceRuleEngine.RuleAlert> alerts2 = engine.evaluate(resp2, null, tuesday, LocalTime.of(17, 16));
        assertEquals(1, alerts2.size());
        assertEquals("Cảnh báo Check-out lại (lần 2) vẫn chưa đủ công", alerts2.get(0).getTitle());
        assertTrue(alerts2.get(0).getMessage().contains("17:15"));
        assertTrue(alerts2.get(0).getMessage().contains("thiếu: <b>35 phút</b>"));

        // LẦN 3: Bấm Check-out lại lúc 18:00 (ĐÃ ĐỦ CÔNG!)
        AttendanceRecord rec3 = new AttendanceRecord("ĐỦ CÔNG", "2026-10-13", "08:50", "18:00");
        AttendanceResponse resp3 = new AttendanceResponse();
        resp3.setSuccess(true);
        resp3.setData(List.of(rec3));

        List<AttendanceRuleEngine.RuleAlert> alerts3 = engine.evaluate(resp3, null, tuesday, LocalTime.of(18, 1));
        assertEquals(1, alerts3.size());
        assertEquals(AttendanceRuleEngine.AlertLevel.INFO, alerts3.get(0).getLevel());
        assertEquals("Check-out lại thành công & Đã đủ công", alerts3.get(0).getTitle());
        assertTrue(alerts3.get(0).getMessage().contains("18:00"));
        assertTrue(alerts3.get(0).getMessage().contains("Lần check-out thứ 3"));

        // Chu kỳ tiếp theo lúc 18:05 (giờ check-out vẫn là 18:00) -> 0 alert (Chống spam tuyệt đối)
        List<AttendanceRuleEngine.RuleAlert> alerts4 = engine.evaluate(resp3, null, tuesday, LocalTime.of(18, 5));
        assertTrue(alerts4.isEmpty(), "Không được gửi lặp lại khi giờ check-out không thay đổi");
    }

    @Test
    @DisplayName("Sau khi check-out về sớm, khi đồng hồ điểm mốc đủ 8 tiếng (17:50) sẽ nhắc bấm Check-out lại")
    void testRemindReCheckOutAtTargetTimeAfterEarlyCheckOut() {
        LocalDate tuesday = LocalDate.of(2026, 10, 13);

        // Morning (09:00): Check-in 08:50 -> Target earliest checkout is 17:50
        AttendanceRecord recMorning = new AttendanceRecord("ĐANG LÀM VIỆC", "2026-10-13", "08:50", null);
        AttendanceResponse respMorning = new AttendanceResponse();
        respMorning.setSuccess(true);
        respMorning.setData(List.of(recMorning));
        engine.evaluate(respMorning, null, tuesday, LocalTime.of(9, 0));

        // Check-out early at 16:30
        AttendanceRecord recEarly = new AttendanceRecord("CA SÁNG", "2026-10-13", "08:50", "16:30");
        AttendanceResponse respEarly = new AttendanceResponse();
        respEarly.setSuccess(true);
        respEarly.setData(List.of(recEarly));
        engine.evaluate(respEarly, null, tuesday, LocalTime.of(16, 31));

        // At 17:51 (clock passes 17:50 milestone, user is still in office)
        List<AttendanceRuleEngine.RuleAlert> alertsTarget = engine.evaluate(respEarly, null, tuesday, LocalTime.of(17, 51));
        assertEquals(1, alertsTarget.size());
        assertEquals("Nhắc Check-out lại (Đã đủ 8h)", alertsTarget.get(0).getTitle());
        assertTrue(alertsTarget.get(0).getMessage().toLowerCase().contains("hãy check-out lại"));
        assertTrue(alertsTarget.get(0).getMessage().contains("16:30"));
        assertTrue(alertsTarget.get(0).getMessage().contains("17:50"));
    }
}
