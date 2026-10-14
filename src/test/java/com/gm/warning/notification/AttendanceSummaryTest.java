package com.gm.warning.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gm.warning.client.TokenCache;
import com.gm.warning.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AttendanceSummaryTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String SAMPLE_SUMMARY_JSON = """
            {
              "employeeId": "2210025",
              "employeeName": "Nguyễn Hữu Phương",
              "department": "Kỹ thuật lâp trình",
              "totalScore": 19.8,
              "attendanceScore": 14.6,
              "holidayDays": 2.0,
              "workModeDays": 3.2,
              "annualLeaveDays": 2.0,
              "actualPaidDays": 21.8,
              "dailyScores": {
                "1": 1.0, "2": 1.0, "3": 1.0, "4": 1.0, "5": 0.0, "6": 0.0, "7": 0.97, "8": 1.0,
                "9": 1.0, "10": 1.0, "11": 1.0, "12": 0.0, "13": 0.0, "14": 1.0, "15": 1.0,
                "16": 0.99, "17": 1.0, "18": 1.0, "19": 0.0, "20": 0.0, "21": 1.0, "22": 1.0,
                "23": 1.0, "24": 1.0, "25": 0.84, "26": 0.0, "27": 0.0, "28": 1.0, "29": 0.0, "30": 0.0
              },
              "dailyWorkModes": {
                "11": {
                  "workMode": "REMOTE",
                  "period": "AFTERNOON",
                  "approvedScore": 0.6,
                  "label": "Remote",
                  "periodLabel": "Buổi chiều"
                },
                "17": {
                  "workMode": "REMOTE",
                  "period": "FULL_DAY",
                  "approvedScore": 1.0,
                  "label": "Remote",
                  "periodLabel": "Cả ngày"
                },
                "22": {
                  "workMode": "REMOTE",
                  "period": "FULL_DAY",
                  "approvedScore": 1.0,
                  "label": "Remote",
                  "periodLabel": "Cả ngày"
                },
                "25": {
                  "workMode": "REMOTE",
                  "period": "AFTERNOON",
                  "approvedScore": 0.6,
                  "label": "Remote",
                  "periodLabel": "Buổi chiều"
                }
              },
              "dailyLeaves": {
                "2026-09-30": {
                  "leaveCategory": "ANNUAL",
                  "leaveCategoryLabel": "Nghỉ phép năm",
                  "leaveType": "FULL_DAY",
                  "paid": "true",
                  "countsAsAnnual": "true",
                  "statutoryPaidLeave": "false",
                  "attendanceDisplayOnly": "false",
                  "leaveScore": "1.0"
                },
                "2026-09-29": {
                  "leaveCategory": "ANNUAL",
                  "leaveCategoryLabel": "Nghỉ phép năm",
                  "leaveType": "FULL_DAY",
                  "paid": "true",
                  "countsAsAnnual": "true",
                  "statutoryPaidLeave": "false",
                  "attendanceDisplayOnly": "false",
                  "leaveScore": "1.0"
                }
              }
            }
            """;

    @Test
    public void testParseAttendanceSummary() throws Exception {
        AttendanceSummaryData data = mapper.readValue(SAMPLE_SUMMARY_JSON, AttendanceSummaryData.class);

        assertNotNull(data);
        assertEquals("2210025", data.getEmployeeId());
        assertEquals("Nguyễn Hữu Phương", data.getEmployeeName());
        assertEquals("Kỹ thuật lâp trình", data.getDepartment());
        assertEquals(21.8, data.getActualPaidDays(), 0.001);
        assertEquals(14.6, data.getAttendanceScore(), 0.001);
        assertEquals(3.2, data.getWorkModeDays(), 0.001);
        assertEquals(2.0, data.getAnnualLeaveDays(), 0.001);
        assertEquals(2.0, data.getHolidayDays(), 0.001);

        // Daily Work modes
        DailyWorkModeItem mode11 = data.getWorkModeForDay(11);
        assertNotNull(mode11);
        assertEquals("REMOTE", mode11.getWorkMode());
        assertEquals(0.6, mode11.getApprovedScore(), 0.001);
        assertEquals("Buổi chiều", mode11.getPeriodLabel());

        DailyWorkModeItem mode17 = data.getWorkModeForDay(17);
        assertNotNull(mode17);
        assertEquals(1.0, mode17.getApprovedScore(), 0.001);
        assertEquals("Cả ngày", mode17.getPeriodLabel());

        // Daily Leaves
        DailyLeaveItem leave30 = data.getLeaveForDay(2026, 9, 30);
        assertNotNull(leave30);
        assertEquals("Nghỉ phép năm", leave30.getLeaveCategoryLabel());
        assertEquals(1.0, leave30.getLeaveScoreValue(), 0.001);

        // Daily Scores
        assertEquals(0.97, data.getScoreForDay(7), 0.001);
        assertEquals(0.84, data.getScoreForDay(25), 0.001);
    }

    @Test
    public void testAttendanceMonthSummaryFormatting() throws Exception {
        AttendanceSummaryData summary = mapper.readValue(SAMPLE_SUMMARY_JSON, AttendanceSummaryData.class);

        List<AttendanceRecord> records = new ArrayList<>();
        records.add(new AttendanceRecord("ĐỦ CÔNG", "2026-09-03", "08:41", "18:34"));
        records.add(new AttendanceRecord("VỀ SỚM", "2026-09-11", "08:39", "13:05"));
        records.add(new AttendanceRecord("REMOTE", "2026-09-17", null, null));
        records.add(new AttendanceRecord("CA SÁNG", "2026-09-25", "08:59", "11:08"));
        records.add(new AttendanceRecord("NGHỈ", "2026-09-29", null, null));

        List<HolidayItem> holidays = new ArrayList<>();
        HolidayItem h1 = new HolidayItem();
        h1.setHolidayDate("2026-09-01");
        h1.setName("Nghỉ lễ Quốc Khánh");
        HolidayItem h2 = new HolidayItem();
        h2.setHolidayDate("2026-09-02");
        h2.setName("Nghỉ lễ Quốc Khánh");
        holidays.add(h1);
        holidays.add(h2);

        String text = TelegramTemplate.attendanceMonthSummary(2026, 9, records, summary, holidays);

        // Verify summary block
        assertTrue(text.contains("BẢNG CHẤM CÔNG THÁNG 09/2026"));
        assertTrue(text.contains("Nguyễn Hữu Phương"));
        assertTrue(text.contains("2210025"));
        assertTrue(text.contains("<b>Tổng công tính lương: 21.8</b> công"));
        assertTrue(text.contains("Chấm công tại VP: <b>14.6</b> công"));
        assertTrue(text.contains("Làm việc từ xa (Remote): <b>3.2</b> công"));
        assertTrue(text.contains("Nghỉ phép năm: <b>2</b> công"));
        assertTrue(text.contains("Nghỉ lễ có lương: <b>2</b> công"));

        // Verify daily breakdown lines
        assertTrue(text.contains("01/09"));
        assertTrue(text.contains("Nghỉ lễ Quốc Khánh"));

        assertTrue(text.contains("03/09"));
        assertTrue(text.contains("08:41 - 18:34"));
        assertTrue(text.contains("ĐỦ CÔNG"));

        // Day 11 (Remote afternoon with morning office work)
        assertTrue(text.contains("11/09"));
        assertTrue(text.contains("08:39 - 13:05"));
        assertTrue(text.contains("Remote Buổi chiều (+0.6) | 1 công"));

        // Day 17 (Remote full day)
        assertTrue(text.contains("17/09"));
        assertTrue(text.contains("Remote Cả ngày (1 công)"));

        // Day 25 (Morning office + Afternoon remote)
        assertTrue(text.contains("25/09"));
        assertTrue(text.contains("08:59 - 11:08"));
        assertTrue(text.contains("Remote Buổi chiều (+0.6) | 0.84 công"));

        // Day 29 (Annual Leave)
        assertTrue(text.contains("29/09"));
        assertTrue(text.contains("Nghỉ phép năm (1 công)"));
    }

    @Test
    public void testTokenCacheExtractEmployeeId() {
        String token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIyMjIwZDA1NS05ZTVmLTRjNTgtOWE1MC03MTZhYjA0ZDVhMTEiLCJpYXQiOjE3OTE5NzY2MDIsImp0aSI6IjkzNGI3NGFhLTAyMjktNDkwMC1iZGZmLTBiYWIyMTUwOWY4NSIsImVtcGxveWVlaWQiOiIyMjEwMDI1IiwiaXNzIjoiaHJtLWdtLWdyb3VwLWFjY2VzcyIsInVzZXJpZCI6ImMwOTg0Y2M2LTg3OTMtNGJlNy04NzNiLTY5MjY1MjQ1MzRjZCIsInJvbGUiOiJFTVBMT1lFRSJ9.dummy_signature";
        String empId = TokenCache.extractEmployeeId(token);
        assertEquals("2210025", empId);
    }
}
