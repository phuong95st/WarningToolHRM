package com.gm.warning.notification;

public class TelegramTemplate {

    // Đường gạch ngang 16 ký tự '━', hiển thị gọn gàng, không bị tràn dòng
    public static final String SEPARATOR = "━━━━━━━━━━━━━━━━";

    /**
     * Thông báo xác nhận Check-in buổi sáng thành công
     */
    public static String checkInSuccess(String checkInTime, String earliestOutTime, String status) {
        return SEPARATOR + "\n" +
                "✅ <b>CHECK-IN THÀNH CÔNG</b>\n\n" +
                "<blockquote>⏰ Giờ vào: <b>" + checkInTime + "</b>\n" +
                "🎯 Giờ về tối thiểu: <b>" + earliestOutTime + "</b> <i>(đủ 8h)</i>\n" +
                "🏢 Trạng thái: <b>" + status + "</b></blockquote>\n\n" +
                "💡 <i>Chúc bạn một ngày làm việc hiệu quả!</i>";
    }

    /**
     * Nhắc nhở chấm công sáng (08:30, 08:45, 08:55)
     */
    public static String morningReminder(String timeSlot, String latestStart) {
        return SEPARATOR + "\n" +
                "⏰ <b>NHẮC CHẤM CÔNG SÁNG (" + timeSlot + ")</b>\n\n" +
                "<blockquote>⚠️ Bạn chưa bấm Check-in trên HRM!\n" +
                "🏢 Giờ làm việc hạn chót: <b>" + latestStart + "</b></blockquote>\n\n" +
                "👉 <i>Hãy vào HRM bấm</i> <b>Check-in ngay</b> <i>trước</i> <b>" + latestStart + "</b>!";
    }

    /**
     * Báo động đỏ khi quá 09:00 mà chưa check-in
     */
    public static String lateCheckInAlert(String currentTime, String latestStart, String hrmUrl) {
        return SEPARATOR + "\n" +
                "🚨 <b>QUÁ GIỜ CHẤM CÔNG SÁNG</b>\n\n" +
                "<blockquote>⚠️ Hiện tại là <b>" + currentTime + "</b> (đã quá <b>" + latestStart + "</b>)!\n" +
                "❌ Hệ thống HRM <b>chưa ghi nhận Check-in</b> của bạn.</blockquote>\n\n" +
                "👉 Vào ngay <a href=\"" + hrmUrl + "\"><b>HRM System</b></a> để <b>Check-in ngay</b>!";
    }

    /**
     * Thông báo khi đã làm đủ 8 tiếng (span 9 tiếng gồm nghỉ trưa)
     */
    public static String minimumHoursReached(String checkInTime, String earliestOutTime) {
        return SEPARATOR + "\n" +
                "🎉 <b>ĐÃ ĐỦ 8H LÀM VIỆC HÔM NAY</b>\n\n" +
                "<blockquote>⏰ Giờ vào: <b>" + checkInTime + "</b>\n" +
                "🎯 Đến <b>" + earliestOutTime + "</b> bạn đã hoàn thành đủ 8 tiếng.</blockquote>\n\n" +
                "🔔 <i>Khi nào kết thúc ca làm, nhớ bấm</i> <b>Check-out</b> <i>nhé!</i>";
    }

    /**
     * Nhắc nhở Check-out nghỉ trưa / kết thúc ca sáng (đề phòng buổi chiều nghỉ không đi làm)
     */
    public static String noonReminder(String currentTime, String checkInTime) {
        String inText = (checkInTime != null && !checkInTime.isBlank()) ? checkInTime : "--:--";
        return SEPARATOR + "\n" +
                "🔔 <b>NHẮC CHECK-OUT NGHỈ TRƯA</b>\n\n" +
                "<blockquote>⏰ Giờ vào sáng: <b>" + inText + "</b>\n" +
                "🕛 Nghỉ trưa: <b>12:00 - 13:00</b> (Hiện tại: <b>" + currentTime + "</b>)\n" +
                "💡 Nếu chiều nghỉ, nhớ bấm Check-out để chốt công!</blockquote>\n\n" +
                "👉 <i>Nếu buổi chiều nghỉ làm, hãy bấm</i> <b>Check-out ngay</b> <i>để được tính nửa ngày công ca sáng nhé!</i>";
    }

    /**
     * Nhắc nhở Check-out buổi chiều/tối
     */
    public static String eveningReminder(String timeSlot, String currentTime, String checkInTime) {
        String inText = (checkInTime != null && !checkInTime.isBlank()) ? checkInTime : "--:--";
        return SEPARATOR + "\n" +
                "🔔 <b>NHẮC CHECK-OUT (" + timeSlot + ")</b>\n\n" +
                "<blockquote>🚪 Hiện tại là <b>" + currentTime + "</b>, bạn <b>CHƯA BẤM CHECK-OUT</b>.\n" +
                "⏰ Giờ vào hôm nay: <b>" + inText + "</b></blockquote>\n\n" +
                "👉 <i>Nếu chuẩn bị ra về, hãy bấm</i> <b>Check-out ngay</b> <i>kẻo quên!</i>";
    }

    /**
     * Cảnh báo nghi vấn quên Check-out (sau 19:00)
     */
    public static String lateEveningAlert(String currentTime, String checkInTime) {
        String inText = (checkInTime != null && !checkInTime.isBlank()) ? checkInTime : "--:--";
        return SEPARATOR + "\n" +
                "⚠️ <b>QUÊN BẤM CHECK-OUT?</b>\n\n" +
                "<blockquote>🌙 Đã <b>" + currentTime + "</b> nhưng chưa có bản ghi Check-out trên HRM.\n" +
                "⏰ Giờ vào: <b>" + inText + "</b></blockquote>\n\n" +
                "👉 <i>Nếu đã về nhà, hãy vào HRM làm</i> <b>đơn giải trình bổ sung Check-out</b>!";
    }

    /**
     * Cảnh báo khẩn cấp khi Shutdown / Thoát máy mà chưa Check-out
     */
    public static String shutdownAlert(String checkInTime) {
        String inText = (checkInTime != null && !checkInTime.isBlank()) ? checkInTime : "--:--";
        return SEPARATOR + "\n" +
                "🚨 <b>TẮT MÁY CHƯA CHECK-OUT!</b>\n\n" +
                "<blockquote>⚠️ Phát hiện máy tính đang <b>TẮT / RỜI BÀN LÀM VIỆC</b>!\n" +
                "⏰ Giờ vào hôm nay: <b>" + inText + "</b>\n" +
                "🚪 Giờ ra: <b>Chưa bấm Check-out</b></blockquote>\n\n" +
                "👉 <i>Hãy mở HRM trên điện thoại để</i> <b>Check-out ngay</b> <i>kẻo mất công!</i>";
    }

    /**
     * Thông báo phê duyệt đơn mới từ HRM (nghỉ phép, remote, OT)
     */
    public static String hrmApprovalNotification(String title, String content, String createdAt) {
        return SEPARATOR + "\n" +
                "🔔 <b>KẾT QUẢ DUYỆT ĐƠN TỪ HRM</b>\n\n" +
                "<blockquote>📌 <b>" + escapeHtml(title) + "</b>\n" +
                "📝 " + escapeHtml(content) + "\n" +
                "⏰ Thời gian: <b>" + formatTime(createdAt) + "</b></blockquote>";
    }

    /**
     * Cảnh báo khi có Check-out nhưng sáng lại quên Check-in
     */
    public static String missingCheckInWithCheckOut(String checkOutTime, String status) {
        return SEPARATOR + "\n" +
                "🚨 <b>THIẾU CHECK-IN SÁNG!</b>\n\n" +
                "<blockquote>🚪 Check-out lúc: <b>" + checkOutTime + "</b>\n" +
                "❌ Buổi sáng <b>CHƯA BẤM CHECK-IN</b>!\n" +
                "📊 Trạng thái: <b>" + (status != null ? status : "N/A") + "</b></blockquote>\n\n" +
                "👉 <i>Hãy vào HRM làm</i> <b>đơn Giải trình quên check-in</b> <i>ngay!</i>";
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
                ? "⚠️ <b>CHECK-OUT CHƯA ĐỦ CÔNG</b>"
                : "⚠️ <b>CHECK-OUT LẠI LẦN " + attemptNumber + ": CHƯA ĐỦ CÔNG</b>";

        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");
        sb.append(headerTitle).append("\n\n");
        sb.append("<blockquote>⏰ Giờ vào: <b>").append(checkInTime != null ? checkInTime : "--:--").append("</b> | Giờ ra: <b>").append(checkOutTime).append("</b>\n");
        sb.append("⏳ Đã làm: <b>").append(hoursWorked).append("h ").append(minsWorked).append("p</b>\n");
        sb.append("🎯 Giờ ra yêu cầu: <b>").append(earliestOutTime).append("</b> <i>(đủ 8h)</i>\n");
        if (minsRemaining > 0) {
            sb.append("⚠️ Còn thiếu: <b>").append(minsRemaining).append(" phút</b>\n");
        }
        sb.append("📊 Trạng thái: <b>").append(status != null ? status : "Chưa đủ công").append("</b></blockquote>\n\n");

        if (attemptNumber <= 1) {
            sb.append("👉 <i>Nếu còn ở lại, nhớ bấm Check-out lại sau</i> <b>").append(earliestOutTime).append("</b> <i>(đủ 8h)!</i>");
        } else {
            sb.append("👉 <i>Hãy tiếp tục làm việc và bấm Check-out lại sau</i> <b>").append(earliestOutTime).append("</b> <i>nhé!</i>");
        }

        return sb.toString();
    }

    /**
     * Xác nhận Check-out thành công & Đã đủ công (hỗ trợ lần đầu hoặc sau khi check-out lại)
     */
    public static String checkOutSuccess(String checkInTime, String checkOutTime, long hoursWorked, long minsWorked, String status, int attemptNumber) {
        String header = (attemptNumber <= 1)
                ? "🎉 <b>XÁC NHẬN: CHECK-OUT THÀNH CÔNG</b>"
                : "🎉 <b>CHECK-OUT LẠI THÀNH CÔNG (ĐỦ CÔNG)</b>";

        StringBuilder sb = new StringBuilder();
        sb.append(SEPARATOR).append("\n");
        sb.append(header).append("\n\n");
        sb.append("<blockquote>⏰ Giờ vào: <b>").append(checkInTime != null ? checkInTime : "--:--").append("</b> | Giờ ra: <b>").append(checkOutTime).append("</b>\n");
        sb.append("⏳ Tổng làm việc: <b>").append(hoursWorked).append("h ").append(minsWorked).append("p</b>\n");
        sb.append("📊 Trạng thái: <b>").append(status != null ? status : "ĐỦ CÔNG").append("</b></blockquote>\n\n");
        if (attemptNumber > 1) {
            sb.append("✅ <i>Lần check-out thứ ").append(attemptNumber).append(" này đã hoàn thành <b>đủ 8 tiếng</b>. Chúc bạn buổi tối vui vẻ!</i>");
        } else {
            sb.append("✅ <i>Hoàn thành <b>đủ ngày công</b>. Chúc bạn một buổi tối vui vẻ!</i>");
        }
        return sb.toString();
    }

    /**
     * Nhắc nhở bấm Check-out lại khi đã đến giờ đủ công (sau khi trước đó từng check-out về sớm)
     */
    public static String remindReCheckOutAtTarget(String checkInTime, String currentTime, String previousCheckOutTime, String earliestOutTime) {
        return SEPARATOR + "\n" +
                "⏰ <b>ĐÃ ĐỦ 8H: HÃY CHECK-OUT LẠI!</b>\n\n" +
                "<blockquote>⏰ Giờ vào: <b>" + (checkInTime != null ? checkInTime : "--:--") + "</b> | Mốc đủ: <b>" + earliestOutTime + "</b>\n" +
                "🚪 Lúc <b>" + previousCheckOutTime + "</b> bạn từng check-out thiếu giờ.\n" +
                "🎯 Hiện tại (<b>" + currentTime + "</b>) bạn đã hoàn thành đủ 8 tiếng!</blockquote>\n\n" +
                "👉 <i>Hãy bấm</i> <b>Check-out lại ngay</b> <i>để hệ thống cập nhật</i> <b>ĐỦ CÔNG</b>!";
    }

    /**
     * Cảnh báo khi người dùng tắt máy nhưng trước đó check-out chưa đủ công
     */
    public static String shutdownIncompleteAlert(String checkInTime, String checkOutTime, String earliestOutTime, String status) {
        return SEPARATOR + "\n" +
                "🚨 <b>TẮT MÁY KHI CHƯA ĐỦ CÔNG!</b>\n\n" +
                "<blockquote>⏰ Giờ vào: <b>" + (checkInTime != null ? checkInTime : "--:--") + "</b> | Check-out: <b>" + checkOutTime + "</b>\n" +
                "🎯 Giờ ra yêu cầu: <b>" + earliestOutTime + "</b>\n" +
                "📊 Trạng thái: <b>" + (status != null ? status : "Chưa đủ công") + "</b></blockquote>\n\n" +
                "👉 <i>Đừng quên bấm Check-out lại sau</i> <b>" + earliestOutTime + "</b> <i>trên HRM nếu bạn vừa làm đủ giờ!</i>";
    }

    /**
     * Cảnh báo ngày chưa đủ công trong tháng
     */
    public static String incompleteAttendanceAlert(String workDate, String checkIn, String checkOut, String status) {
        return SEPARATOR + "\n" +
                "⚠️ <b>CẢNH BÁO: CHƯA ĐỦ CÔNG TRONG THÁNG</b>\n\n" +
                "<blockquote>📅 Ngày: <b>" + workDate + "</b>\n" +
                "⏰ In: <b>" + (checkIn != null ? checkIn : "--:--") + "</b> | Out: <b>" + (checkOut != null ? checkOut : "--:--") + "</b>\n" +
                "📊 Trạng thái: <b>" + status + "</b> <i>(Chưa đủ công)</i></blockquote>\n\n" +
                "👉 <i>Hãy kiểm tra và gửi</i> <b>đơn Giải trình chấm công</b> <i>nếu cần!</i>";
    }

    /**
     * Tin nhắn test kết nối
     */
    public static String testMessage() {
        return SEPARATOR + "\n" +
                "🤖 <b>HRM ATTENDANCE TOOL</b>\n\n" +
                "<blockquote>✅ Kết nối Telegram Bot thành công!\n" +
                "📱 Thông báo chấm công & duyệt đơn sẽ gửi tới đây.</blockquote>";
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
