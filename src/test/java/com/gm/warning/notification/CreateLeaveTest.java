package com.gm.warning.notification;

import com.gm.warning.config.AppConfig;
import com.gm.warning.model.CreateLeaveResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class CreateLeaveTest {

    @Test
    public void testAppConfigCreateLeavePath() {
        AppConfig config = new AppConfig();
        assertNotNull(config.getCreateLeaveApiPath());
        assertEquals("/api/leave/request", config.getCreateLeaveApiPath());
    }

    @Test
    public void testCreateLeaveResult() {
        CreateLeaveResult resSuccess = new CreateLeaveResult(true, "Thành công", 200);
        assertTrue(resSuccess.isSuccess());
        assertEquals("Thành công", resSuccess.getMessage());
        assertEquals(200, resSuccess.getStatusCode());

        CreateLeaveResult resFail = new CreateLeaveResult(false, "Lỗi số dư", 400);
        assertFalse(resFail.isSuccess());
        assertEquals("Lỗi số dư", resFail.getMessage());
        assertEquals(400, resFail.getStatusCode());
    }

    @Test
    public void testTelegramTemplateCreateLeaveSuccess() {
        String msg = TelegramTemplate.createLeaveSuccess("2026-11-17", "2026-11-17", "FULL_DAY", "ANNUAL", "Việc cá nhân");
        assertTrue(msg.contains("TẠO ĐƠN NGHỈ PHÉP THÀNH CÔNG"));
        assertTrue(msg.contains("2026-11-17"));
        assertTrue(msg.contains("Cả ngày"));
        assertTrue(msg.contains("Nghỉ phép năm"));
        assertTrue(msg.contains("Việc cá nhân"));
        assertTrue(msg.contains("PENDING"));

        String msgRange = TelegramTemplate.createLeaveSuccess("2026-11-17", "2026-11-19", "MORNING", "UNPAID", "Bận việc nhà");
        assertTrue(msgRange.contains("2026-11-17 ➜ 2026-11-19"));
        assertTrue(msgRange.contains("Buổi sáng"));
        assertTrue(msgRange.contains("Nghỉ không lương"));
        assertTrue(msgRange.contains("Bận việc nhà"));
    }

    @Test
    public void testTelegramTemplateCreateLeaveError() {
        String msg = TelegramTemplate.createLeaveError("Số ngày phép không đủ");
        assertTrue(msg.contains("TẠO ĐƠN NGHỈ PHÉP THẤT BẠI"));
        assertTrue(msg.contains("Số ngày phép không đủ"));
    }

    @Test
    public void testTelegramTemplateCreateLeaveHelp() {
        String msg = TelegramTemplate.createLeaveHelpMessage();
        assertTrue(msg.contains("HƯỚNG DẪN TẠO ĐƠN NGHỈ PHÉP"));
        assertTrue(msg.contains("/taodon"));
    }

    @Test
    public void testBotHelpMessageContainsTaoDon() {
        String help = TelegramTemplate.botHelpMessage();
        assertTrue(help.contains("/taodon"));
    }
}
