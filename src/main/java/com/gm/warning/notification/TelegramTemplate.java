package com.gm.warning.notification;

public class TelegramTemplate {

    public static final String SEPARATOR = "━━━━━━━━━━━━━━━━━━━━━";

    /**
     * Thông báo xác nhận Check-in buổi sáng thành công
     */
    public static String checkInSuccess(String checkInTime, String earliestOutTime, String status) {
        return SEPARATOR + "\n" +
                "✅ <b>CHECK-IN THÀNH CÔNG</b>\n" +
                SEPARATOR + "\n\n" +
                "⏰ Giờ vào: <b>" + checkInTime + "</b>\n" +
                "🎯 Giờ về tối thiểu: <b>" + earliestOutTime + "</b> <i>(đủ 8h)</i>\n" +
                "🏢 Trạng thái: <b>" + status + "</b>\n\n" +
                "💡 <i>Chúc bạn một ngày làm việc hiệu quả!</i>";
    }

    /**
     * Nhắc nhở chấm công sáng (08:30, 08:45, 08:55)
     */
    public static String morningReminder(String timeSlot, String latestStart) {
        return SEPARATOR + "\n" +
                "⏰ <b>NHẮC CHẤM CÔNG SÁNG (" + timeSlot + ")</b>\n" +
                SEPARATOR + "\n\n" +
                "⚠️ <b>Bạn chưa bấm Check-in trên HRM!</b>\n" +
                "🏢 Giờ làm việc linh hoạt hạn chót: <b>" + latestStart + "</b>\n\n" +
                "👉 Hãy chấm công ngay khi đến công ty.";
    }

    /**
     * Báo động đỏ khi quá 09:00 mà chưa check-in
     */
    public static String lateCheckInAlert(String currentTime, String latestStart, String hrmUrl) {
        return SEPARATOR + "\n" +
                "🚨 <b>BÁO ĐỘNG: QUÁ GIỜ CHẤM CÔNG</b>\n" +
                SEPARATOR + "\n\n" +
                "⚠️ Hiện tại là <b>" + currentTime + "</b> (đã quá hạn <b>" + latestStart + "</b>)!\n" +
                "❌ Hệ thống HRM <b>chưa ghi nhận Check-in</b> của bạn.\n\n" +
                "👉 Vào ngay <a href=\"" + hrmUrl + "\">HRM System</a> để chấm công hoặc làm đơn giải trình!";
    }

    /**
     * Thông báo khi đã làm đủ 8 tiếng (span 9 tiếng gồm nghỉ trưa)
     */
    public static String minimumHoursReached(String checkInTime, String earliestOutTime) {
        return SEPARATOR + "\n" +
                "🎉 <b>ĐÃ ĐỦ GIỜ LÀM VIỆC HÔM NAY</b>\n" +
                SEPARATOR + "\n\n" +
                "⏰ Giờ vào: <b>" + checkInTime + "</b>\n" +
                "🎯 Đến <b>" + earliestOutTime + "</b> bạn đã hoàn thành đủ 8 tiếng.\n\n" +
                "🔔 <i>Khi nào kết thúc ca làm, nhớ bấm Check-out nhé!</i>";
    }

    /**
     * Nhắc nhở Check-out buổi chiều/tối
     */
    public static String eveningReminder(String timeSlot, String currentTime, String checkInTime) {
        String inText = (checkInTime != null && !checkInTime.isBlank()) ? checkInTime : "--:--";
        return SEPARATOR + "\n" +
                "🔔 <b>NHẮC CHECK-OUT (" + timeSlot + ")</b>\n" +
                SEPARATOR + "\n\n" +
                "🚪 Hiện tại là <b>" + currentTime + "</b>, bạn <b>CHƯA BẤM CHECK-OUT</b>.\n" +
                "⏰ Giờ vào hôm nay: <b>" + inText + "</b>\n\n" +
                "👉 Nếu chuẩn bị ra về, hãy bấm Check-out ngay kẻo quên!";
    }

    /**
     * Cảnh báo nghi vấn quên Check-out (sau 19:00)
     */
    public static String lateEveningAlert(String currentTime, String checkInTime) {
        String inText = (checkInTime != null && !checkInTime.isBlank()) ? checkInTime : "--:--";
        return SEPARATOR + "\n" +
                "⚠️ <b>CẢNH BÁO: BẠN CÓ QUÊN CHECK-OUT?</b>\n" +
                SEPARATOR + "\n\n" +
                "🌙 Đã <b>" + currentTime + "</b> nhưng chưa có bản ghi Check-out trên HRM.\n" +
                "⏰ Giờ vào: <b>" + inText + "</b>\n\n" +
                "👉 Nếu đã về nhà, hãy vào HRM giải trình bổ sung check-out!";
    }

    /**
     * Cảnh báo khẩn cấp khi Shutdown / Thoát máy mà chưa Check-out
     */
    public static String shutdownAlert(String checkInTime) {
        String inText = (checkInTime != null && !checkInTime.isBlank()) ? checkInTime : "--:--";
        return SEPARATOR + "\n" +
                "🚨 <b>CẢNH BÁO: TẮT MÁY NHƯNG CHƯA CHECK-OUT!</b>\n" +
                SEPARATOR + "\n\n" +
                "⚠️ Phát hiện máy tính đang <b>TẮT / RỜI BÀN LÀM VIỆC</b>!\n" +
                "⏰ Giờ vào hôm nay: <b>" + inText + "</b>\n" +
                "🚪 Giờ ra: <b>Chưa bấm Check-out</b>\n\n" +
                "👉 Hãy mở HRM trên điện thoại để Check-out ngay kẻo mất công!";
    }

    /**
     * Thông báo phê duyệt đơn mới từ HRM (nghỉ phép, remote, OT)
     */
    public static String hrmApprovalNotification(String title, String content, String createdAt) {
        return SEPARATOR + "\n" +
                "🔔 <b>KẾT QUẢ DUYỆT ĐƠN TỪ HRM</b>\n" +
                SEPARATOR + "\n\n" +
                "📌 <b>" + escapeHtml(title) + "</b>\n" +
                "📝 " + escapeHtml(content) + "\n\n" +
                "⏰ Thời gian: <b>" + formatTime(createdAt) + "</b>";
    }

    /**
     * Cảnh báo khi có Check-out nhưng sáng lại quên Check-in
     */
    public static String missingCheckInWithCheckOut(String checkOutTime, String status) {
        return SEPARATOR + "\n" +
                "🚨 <b>CẢNH BÁO: CÓ CHECK-OUT NHƯNG THIẾU CHECK-IN SÁNG!</b>\n" +
                SEPARATOR + "\n\n" +
                "🚪 Hệ thống ghi nhận bạn đã Check-out lúc: <b>" + checkOutTime + "</b>\n" +
                "❌ Nhưng <b>BUỔI SÁNG BẠN CHƯA BẤM CHECK-IN</b>!\n" +
                "📊 Trạng thái hiện tại: <b>" + (status != null ? status : "N/A") + "</b>\n\n" +
                "👉 Trường hợp này bạn sẽ bị thiếu công. Hãy vào HRM làm đơn Giải trình quên check-in ngay!";
    }

    /**
     * Cảnh báo khi Check-out về sớm / chưa đủ 8 tiếng công
     */
    public static String earlyCheckOutIncomplete(String checkInTime, String checkOutTime, String earliestOutTime,
                                                 long hoursWorked, long minsWorked, String status) {
        return earlyCheckOutIncomplete(checkInTime, checkOutTime, earliestOutTime, hoursWorked, minsWorked, status, 1, 0);
    }

    /**
     * Cảnh báo thông minh khi Check-out chưa đủ công (hỗ trợ nhiều lần check-out trong ngày)
     */
    public static String earlyCheckOutIncomplete(String checkInTime, String checkOutTime, String earliestOutTime,
                                                 long hoursWorked, long minsWorked, String status, int attemptNumber, long minsRemaining) {
        String headerTitle = (attemptNumber <= 1)
                ? "⚠️ <b>CẢNH BÁO: CHECK-OUT CHƯA ĐỦ CÔNG!</b>"
                : "⚠️ <b>CẢNH BÁO: CHECK-OUT LẠI (LẦN " + attemptNumber + ") VẪN CHƯA ĐỦ CÔNG!</b>";

        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");
        sb.append(headerTitle).append("\n");
        sb.append(SEPARATOR).append("\n\n");
        sb.append("⏰ Giờ vào: <b>").append(checkInTime != null ? checkInTime : "--:--").append("</b> | Giờ ra: <b>").append(checkOutTime).append("</b>\n");
        sb.append("⏳ Thời gian thực tế: <b>").append(hoursWorked).append(" giờ ").append(minsWorked).append(" phút</b>\n");
        sb.append("🎯 Giờ ra tối thiểu yêu cầu: <b>").append(earliestOutTime).append("</b> <i>(đủ 8h làm việc)</i>\n");
        if (minsRemaining > 0) {
            sb.append("⚠️ Bạn còn thiếu: <b>").append(minsRemaining).append(" phút</b> nữa mới đủ công!\n");
        }
        sb.append("📊 Trạng thái hệ thống: <b>").append(status != null ? status : "Chưa đủ công").append("</b>\n\n");

        if (attemptNumber <= 1) {
            sb.append("👉 Bạn về trước giờ quy định hoặc thiếu giờ. Nếu có lý do hãy làm đơn Giải trình/Xin phép. Nếu còn ở lại, nhớ bấm Check-out lại sau <b>").append(earliestOutTime).append("</b>!");
        } else {
            sb.append("👉 Vẫn chưa đạt mốc <b>").append(earliestOutTime).append("</b>. Hãy tiếp tục làm việc và bấm Check-out lại khi đủ giờ nhé!");
        }

        return sb.toString();
    }

    /**
     * Xác nhận Check-out thành công & Đã đủ công (hỗ trợ lần đầu hoặc sau khi check-out lại)
     */
    public static String checkOutSuccess(String checkInTime, String checkOutTime, long hoursWorked, long minsWorked, String status, int attemptNumber) {
        String header = (attemptNumber <= 1)
                ? "🎉 <b>XÁC NHẬN: CHECK-OUT THÀNH CÔNG!</b>"
                : "🎉 <b>XÁC NHẬN: CHECK-OUT LẠI THÀNH CÔNG (ĐÃ ĐỦ CÔNG)!</b>";

        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");
        sb.append(header).append("\n");
        sb.append(SEPARATOR).append("\n\n");
        sb.append("⏰ Giờ vào: <b>").append(checkInTime != null ? checkInTime : "--:--").append("</b> | Giờ ra: <b>").append(checkOutTime).append("</b>\n");
        sb.append("⏳ Tổng thời gian làm việc: <b>").append(hoursWorked).append(" giờ ").append(minsWorked).append(" phút</b>\n");
        sb.append("📊 Trạng thái hệ thống: <b>").append(status != null ? status : "ĐỦ CÔNG").append("</b>\n\n");
        if (attemptNumber > 1) {
            sb.append("✅ Tuyệt vời! Lần check-out thứ ").append(attemptNumber).append(" này đã hoàn thành đủ 8 tiếng công.\n");
        } else {
            sb.append("✅ Hệ thống đã ghi nhận bạn hoàn thành đủ ngày công làm việc.\n");
        }
        sb.append("Chúc bạn một buổi tối vui vẻ!");
        return sb.toString();
    }

    /**
     * Nhắc nhở bấm Check-out lại khi đã đến giờ đủ công (sau khi trước đó từng check-out về sớm)
     */
    public static String remindReCheckOutAtTarget(String checkInTime, String currentTime, String previousCheckOutTime, String earliestOutTime) {
        return SEPARATOR + "\n" +
                "⏰ <b>ĐÃ ĐẾN GIỜ ĐỦ CÔNG — HÃY CHECK-OUT LẠI!</b>\n" +
                SEPARATOR + "\n\n" +
                "⏰ Giờ vào: <b>" + (checkInTime != null ? checkInTime : "--:--") + "</b> | Mốc đủ công: <b>" + earliestOutTime + "</b>\n" +
                "🚪 Lúc <b>" + previousCheckOutTime + "</b> bạn từng bấm Check-out khi chưa đủ công.\n" +
                "🎯 Bây giờ (<b>" + currentTime + "</b>) bạn đã hoàn thành đủ 8 tiếng!\n\n" +
                "👉 Hãy vào HRM bấm <b>Check-out lại ngay bây giờ</b> để hệ thống cập nhật thành <b>ĐỦ CÔNG</b> nhé!";
    }

    /**
     * Cảnh báo khi người dùng tắt máy nhưng trước đó check-out chưa đủ công
     */
    public static String shutdownIncompleteAlert(String checkInTime, String checkOutTime, String earliestOutTime, String status) {
        return SEPARATOR + "\n" +
                "🚨 <b>BÁO ĐỘNG: ĐANG TẮT MÁY NHƯNG CHƯA CHECK-OUT ĐỦ CÔNG!</b>\n" +
                SEPARATOR + "\n\n" +
                "⏰ Giờ vào: <b>" + (checkInTime != null ? checkInTime : "--:--") + "</b> | Check-out trước đó: <b>" + checkOutTime + "</b>\n" +
                "🎯 Giờ ra tối thiểu yêu cầu: <b>" + earliestOutTime + "</b>\n" +
                "📊 Trạng thái hệ thống: <b>" + (status != null ? status : "Chưa đủ công") + "</b>\n\n" +
                "👉 Bạn đang tắt máy khi công chưa đủ. Đừng quên bấm <b>Check-out lại</b> trên HRM nếu bạn vừa làm đủ giờ!";
    }

    /**
     * Cảnh báo ngày chưa đủ công trong tháng
     */
    public static String incompleteAttendanceAlert(String workDate, String checkIn, String checkOut, String status) {
        return SEPARATOR + "\n" +
                "⚠️ <b>CẢNH BÁO TRẠNG THÁI CÔNG</b>\n" +
                SEPARATOR + "\n\n" +
                "📅 Ngày: <b>" + workDate + "</b>\n" +
                "⏰ In: <b>" + (checkIn != null ? checkIn : "--:--") + "</b> | Out: <b>" + (checkOut != null ? checkOut : "--:--") + "</b>\n" +
                "📊 Trạng thái: <b>" + status + "</b> <i>(Chưa đủ công)</i>\n\n" +
                "👉 Hãy kiểm tra và gửi đơn Giải trình chấm công nếu cần!";
    }

    /**
     * Tin nhắn test kết nối
     */
    public static String testMessage() {
        return SEPARATOR + "\n" +
                "🤖 <b>HRM ATTENDANCE TOOL</b>\n" +
                SEPARATOR + "\n\n" +
                "✅ <b>Kết nối Telegram Bot thành công!</b>\n" +
                "📱 Các thông báo nhắc nhở chấm công và duyệt đơn sẽ được gửi tự động tới đây.";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String formatTime(String raw) {
        if (raw == null) return "";
        if (raw.contains("T")) {
            return raw.replace("T", " ").substring(0, Math.min(19, raw.length()));
        }
        return raw;
    }
}
