# 🚀 HRM Attendance Warning Tool — Phiên Bản Release v1.0

Ứng dụng Java chuyên nghiệp chạy nền giúp nhân viên công ty **GM Group** tự động theo dõi chấm công realtime từ hệ thống **HRM** (`http://hrm.gm-group.vn`), tự động cảnh báo lên **Điện thoại (Telegram Bot)** và **Máy tính (Windows System Tray & Notification)** để **không bao giờ quên check-in / check-out hoặc thiếu giờ làm**.

---

## 📋 MỤC LỤC
1. [Tính Năng Cốt Lõi](#-tính-năng-cốt-lõi)
2. [Yêu Cầu Hệ Thống](#-yêu-cầu-hệ-thống)
3. [Hướng Dẫn Cấu Hình (Chỉ 2 Phút)](#-hướng-dẫn-cấu-hình-chỉ-2-phút)
4. [Hướng Dẫn Khởi Chạy & Sử Dụng](#-hướng-dẫn-khởi-chạy--sử-dụng)
5. [Cấu Hình Tự Khởi Động Cùng Windows](#-cấu-hình-tự-khởi-động-cùng-windows)
6. [Cấu Hình Cho Nhân Viên Khác](#-cấu-hình-cho-nhân-viên-khác)
7. [Chẩn Đoán & Xử Lý Sự Cố (Troubleshooting)](#-chẩn-đoán--xử-lý-sự-cố-troubleshooting)
8. [Báo Cáo Kiểm Thử Chất Lượng (QA Test Report)](#-báo-cáo-kiểm-thử-chất-lượng-qa-test-report)
9. [Cấu Trúc Thư Mục Phát Hành (Release Package)](#-cấu-trúc-thư-mục-phát-hành-release-package)
10. [Nhật Ký Phiên Bản (Changelog)](#-nhật-ký-phiên-bản-changelog)

---

## 🌟 TÍNH NĂNG CỐT LÕI

### 1. 🔑 Tự Động Đăng Nhập SSO Azure AD Ngầm (Playwright)
* Sử dụng thư viện **Microsoft Playwright 1.49** kết hợp trình duyệt Microsoft Edge có sẵn trên máy tính để đăng nhập ngầm không gián đoạn (tương thích hoàn hảo với phiên bản Edge 130+ mới nhất).
* Tự động nhận diện nút SSO **"GM Account"** trên hệ thống xác thực Keycloak của công ty để chuyển hướng đăng nhập tài khoản Microsoft tổ chức.
* Tự động lấy và gia hạn Bearer Token JWT khi token hết hạn hoặc máy chủ trả về `401 Unauthorized`.
* Tự động lưu cache phiên vào thư mục `playwright_profile` để đăng nhập siêu tốc ở các lần tiếp theo.
* **Cơ chế chống tràn ổ C**: Toàn bộ dữ liệu tạm thời của trình duyệt và Java được cách ly về thư mục `temp/` trên ổ đĩa dự án (ổ D), tuyệt đối không gây lỗi đầy ổ đĩa hệ thống `C:\`.

### 2. ⏰ Quy Tắc Cảnh Báo Chấm Công Thông Minh (Giờ Linh Hoạt)
* **Khung giờ sáng linh hoạt (08:30 – 09:00)**:
  * Nhắc nhở chấm công định kỳ vào các mốc `08:30`, `08:45`, `08:55` nếu hệ thống chưa ghi nhận Check-in.
  * **Báo động đỏ khẩn cấp** sau `09:00` nếu vẫn chưa bấm chấm công.
* **Tự động tính giờ về tối thiểu**:
  * Ngay khi check-in (ví dụ vào lúc `08:44`), tool tự động cộng đủ 8 tiếng làm việc + 1 tiếng nghỉ trưa (`12:00 – 13:00`) = **9 tiếng** (về tối thiểu `17:44`) và gửi thông báo xác nhận.
* **🍱 Nhắc nhở Check-out nghỉ trưa (12:00 – 13:00)**:
  * Khung giờ nghỉ trưa công ty: **12:00 – 13:00**.
  * Nếu sáng đã Check-in, đến đúng **12:00** tool sẽ gửi thông báo nhắc Check-out 1 lần: đề phòng trường hợp buổi chiều bạn có việc bận hoặc nghỉ phép không đi làm, giúp hệ thống HRM chốt và ghi nhận đủ nửa ngày công ca sáng thay vì bị mất công.
  * Nếu bạn đã bấm Check-out từ trước đó (ví dụ 11:55 ra về), hệ thống tự động không nhắc nữa.
  * Cơ chế chống spam: Chỉ gửi duy nhất **1 lần trong ngày** trong suốt khung giờ nghỉ trưa (12:00 – 13:00).
* **Khung giờ chiều & tối**:
  * Thông báo ngay khi hoàn thành đủ 8 tiếng công trong ngày.
  * Nhắc nhở bấm Check-out vào các mốc `17:35`, `18:05`, `18:30`, `19:00`, `20:00`.
* **⚠️ Cảnh báo Check-out THÔNG MINH khi CHƯA ĐỦ CÔNG / Về sớm (Hỗ trợ Check-out nhiều lần)**:
  * **Lần 1 chưa đủ công**: Bấm check-out sớm (ví dụ vào 08:50, về lúc 16:30) -> Tool cảnh báo rõ số giờ đã làm (7h40m), giờ tối thiểu yêu cầu (17:50), và số phút còn thiếu (thiếu 80 phút).
  * **Tự nhắc Check-out lại khi đến mốc đủ giờ**: Nếu bạn ở lại văn phòng làm tiếp, đúng lúc chạm mốc 8 tiếng (17:50), tool sẽ gửi thông báo: *"Đã đến mốc đủ công, hãy bấm Check-out lại trên HRM ngay!"*.
  * **Lần 2, 3 nếu vẫn chưa đủ công**: Nếu bạn bấm lại (ví dụ 17:15) mà vẫn chưa đủ giờ, tool tiếp tục cảnh báo chính xác số phút còn thiếu cho lần bấm mới.
  * **Lần chốt ĐỦ CÔNG (Lần 2, 3 hoặc 4)**: Ngay khi bạn bấm Check-out lại và đạt `ĐỦ CÔNG` (ví dụ 18:00), tool lập tức gửi thông báo chúc mừng: *"🎉 Xác nhận Check-out lại thành công & Đã đủ công!"*, đồng thời tự động dừng mọi nhắc nhở chiều/tối!
  * **Cơ chế chống spam tuyệt đối**: Nếu bạn không bấm lại (giờ check-out giữ nguyên), tool tuyệt đối không gửi lại thông báo trùng lặp.
* **🚨 Cảnh báo Sáng KHÔNG Check-in mà chiều lại có Check-out**:
  * Nếu sáng quên chấm công vào nhưng chiều lại bấm ra, tool phát hiện ngay trạng thái bất thường và bắn thông báo khẩn cấp hướng dẫn vào HRM làm đơn Giải trình quên check-in để không bị trừ công.
* **🚨 Cảnh báo đi về mà quên Check-out (Shutdown Hook)**:
  * Khi bạn **tắt máy tính (Shutdown)**, khởi động lại (Restart), hoặc thoát ứng dụng vào ngày làm việc mà chưa bấm Check-out (hoặc trước đó check-out chưa đủ công), tool lập tức bắn thông báo khẩn cấp lên Telegram!

### 3. 🏖️ Tự Động Nhận Biết Cuối Tuần & Lịch Nghỉ Lễ Công Ty
* Tự động nhận biết Thứ Bảy và Chủ Nhật để tắt toàn bộ cảnh báo chấm công.
* Tự động đồng bộ lịch nghỉ lễ (Du lịch Teambuilding, Quốc Khánh, Tết...) từ API `http://hrm.gm-group.vn/api/holidays`.
* Tuyệt đối không gửi cảnh báo sai vào các ngày nghỉ lễ.

### 4. 🔔 Giám Sát Realtime & Bắn Thông Báo Duyệt Đơn (Nghỉ Phép, Remote, OT)
* Tự động gọi API `http://hrm.gm-group.vn/api/notifications/mine` mỗi 2 phút.
* Bắn thông báo tức thì lên Telegram khi có kết quả duyệt:
  * ✅ **Đơn nghỉ phép năm** đã được duyệt.
  * ✅ **Đăng ký Remote (làm việc từ xa)** đã được duyệt (kèm số công được duyệt).
  * ✅ **Đơn làm thêm giờ (OT)** đã được duyệt (kèm số giờ và tên dự án).
  * ✅ **Thông báo lịch nghỉ lễ mới**.
* Có cơ chế lưu mốc (`notification_state.json`) để **chống spam**, chỉ gửi các thông báo mới phát sinh.

### 5. 🤖 Tra Cứu Dữ Liệu Tương Tác Qua Telegram Bot (Bot Commands)
Bạn có thể nhắn tin trực tiếp cho Bot trên Telegram để tra cứu dữ liệu chấm công và nhân sự cá nhân bất cứ lúc nào:
* 📅 **`/chamcong` [tháng/năm]**: Xem chi tiết danh sách từng ngày chấm công trong tháng chỉ định (mặc định tháng hiện tại), thống kê số ngày đủ công, về sớm, giờ vào, giờ ra của từng ngày.
  * *Ví dụ*: `/chamcong` hoặc `/chamcong 09/2026`
* 🏖 **`/phep` [năm]**: Xem tổng số ngày phép năm, số ngày phép đã sử dụng và số ngày phép còn lại thông qua API `/api/leave/balance`.
  * *Ví dụ*: `/phep` hoặc `/phep 2026`
* 📋 **`/donnghi`**: Xem chi tiết danh sách các đơn xin nghỉ phép, nghỉ Remote và trạng thái duyệt gần đây.
* ⏰ **`/ot`**: Xem chi tiết danh sách các đơn đăng ký làm thêm giờ (OT), số giờ được duyệt, lý do, dự án và người phê duyệt thông qua API `/api/overtime/requests/mine`.
* ❓ **`/help`**: Xem menu hướng dẫn sử dụng bot.
* *Hỗ trợ menu gợi ý lệnh nhanh (Menu Button)*: Khi mở chat với bot, Telegram sẽ hiển thị sẵn danh sách lệnh để bạn bấm chọn trực tiếp không cần gõ.

### 6. 📱 Template Thông Báo Telegram Tối Ưu Cho Mobile
* Mỗi thông báo có đường viền phân cách `━━━━━━━━━━━━━━━━` (16 ký tự) trên đầu để phân biệt rõ ràng khi nhận nhiều tin nhắn liên tiếp.
* **Tiêu đề IN HOA ngắn gọn ($\le 24$ ký tự)**: Luôn nằm gọn trên 1 dòng duy nhất trên điện thoại, tuyệt đối không bị rớt lẻ loi một chữ xuống dòng tiếp theo.
* **Bố cục đối xứng**: Khoảng cách đều 1 dòng trống phía trên và phía dưới khối thông tin, mang lại trải nghiệm xem thoáng mắt.
* **In đậm nổi bật thông tin cốt lõi**: Mốc giờ ra yêu cầu (`17:50`), các hành động quan trọng (`Check-in ngay`, `Check-out ngay`, `ĐỦ CÔNG`) được làm nổi bật để dễ nắm bắt ngay khi nhìn lướt qua.

### 7. 📂 Quản Lý Log Tự Động & Retention 30 Ngày
* Tự động ghi log chi tiết vào `logs/hrm-tool-YYYY-MM-DD.log`.
* Tự động ghi tóm tắt công ngày vào `logs/attendance-summary-YYYY-MM-DD.log`.
* Tự động quét và xoá các file log cũ hơn **30 ngày (1 tháng)** để giải phóng ổ cứng.

---

## 💻 YÊU CẦU HỆ THỐNG
* **Hệ điều hành**: Windows 10 / Windows 11 (64-bit).
* **Môi trường Java**: Java Runtime Environment (JRE) hoặc JDK 21 trở lên (đã cài đặt).
* **Trình duyệt**: Microsoft Edge (có sẵn mặc định trên Windows) hoặc Google Chrome.

---

## ⚙️ HƯỚNG DẪN CẤU HÌNH (CHỈ 2 PHÚT)

Mở file [`config.properties`](file:///D:/03.Work/WarningToolHRM/config.properties) tại thư mục gốc của tool bằng Notepad:

```properties
# ===================================================================
# HRM Attendance Warning Tool Configuration
# ===================================================================

# 1. Địa chỉ máy chủ HRM
hrm.url=http://hrm.gm-group.vn
hrm.api.attendance.path=/api/attendance/today
hrm.api.holidays.path=/api/holidays
hrm.api.notifications.path=/api/notifications/mine

# 2. Tài khoản Microsoft SSO
microsoft.username=your_email@gm-group.vn
microsoft.password=your_password_here

# 3. Cấu hình nhận cảnh báo qua Telegram Bot
telegram.enabled=true
telegram.bot.token=DÁN_BOT_TOKEN_VÀO_ĐÂY
telegram.chat.id=DÁN_CHAT_ID_VÀO_ĐÂY

# 4. Khung giờ làm việc linh hoạt
work.flexible.earliest.start=08:30
work.flexible.latest.start=09:00
work.required.span.hours=9

# 5. Các mốc giờ nhắc nhở sáng, trưa và chiều
reminders.morning=08:30,08:45,08:55
reminders.noon=12:00
reminders.evening=17:35,18:05,18:30,19:00,20:00

# 6. Chu kỳ kiểm tra định kỳ (phút) & Thời gian lưu log (ngày)
check.interval.minutes=1
log.retention.days=30
```

### 📱 Các bước lấy Bot Token & Chat ID Telegram:
1. Mở Telegram, tìm kiếm bot **`@BotFather`** -> Gõ `/newbot` -> Đặt tên bot -> Nhận **Bot Token** (dạng `123456789:ABCdefGhIJKlmNo...`).
2. Mở Telegram tìm bot **`@userinfobot`** -> Bấm Start -> Lấy **Id** của bạn (dạng số, ví dụ `123456789`).
3. Điền Token và Chat ID vào file `config.properties`.
4. Tìm đến bot vừa tạo của bạn trên Telegram và bấm nút **Start** một lần để cho phép bot gửi tin nhắn.

---

## 🏃 HƯỚNG DẪN KHỞI CHẠY & SỬ DỤNG

### Cách 1: Chạy ngầm trong Background (Khuyên dùng hàng ngày)
* **Nhấp đúp chuột vào file**: [`run-background.vbs`](file:///D:/03.Work/WarningToolHRM/run-background.vbs)
* **Hiện tượng**: Không có cửa sổ màn hình đen nào xuất hiện. Ở khay hệ thống (góc dưới bên phải màn hình cạnh đồng hồ) sẽ xuất hiện biểu tượng chữ **H** màu xanh dương.
* **Menu tiện ích trên khay hệ thống**: Chuột phải vào biểu tượng chữ **H** để:
  * Xem trạng thái chấm công hôm nay.
  * Bấm **"Kiểm tra ngay"** để đồng bộ chấm công và duyệt đơn tức thì.
  * Bấm **"Đăng nhập lại SSO"** nếu muốn chủ động lấy token mới.
  * Bấm **"Thử gửi tin nhắn Telegram"** để test bot.
  * Bấm **"Mở trang web HRM"** để vào nhanh hệ thống chấm công.
  * Bấm **"Mở thư mục Logs"** để kiểm tra lịch sử.
  * Bấm **"Thoát"** khi muốn dừng tool.

### Cách 2: Chạy hiển thị Console (Dùng khi kiểm tra / debug)
* **Nhấp đúp chuột vào file**: [`run.bat`](file:///D:/03.Work/WarningToolHRM/run.bat)
* Cửa sổ dòng lệnh sẽ hiển thị chi tiết các bản ghi log và phản hồi của hệ thống.

### Cách 3: Dừng ứng dụng
* Chuột phải vào icon chữ **H** ở khay hệ thống chọn **Thoát**, HOẶC:
* **Nhấp đúp chuột vào file**: [`stop.bat`](file:///D:/03.Work/WarningToolHRM/stop.bat).

---

## 🔄 CẤU HÌNH TỰ KHỞI ĐỘNG CÙNG WINDOWS

Để mỗi ngày khi bạn bật máy tính lên, tool sẽ tự động chạy ngầm bảo vệ công của bạn:
1. Nhấn tổ hợp phím **`Windows + R`** trên bàn phím.
2. Nhập lệnh:
   ```text
   shell:startup
   ```
   rồi nhấn **Enter**. Thư mục Khởi động của Windows sẽ mở ra.
3. Nhấp chuột phải vào file [`run-background.vbs`](file:///D:/03.Work/WarningToolHRM/run-background.vbs) -> Chọn **Show more options** -> Chọn **Create shortcut** (Tạo lối tắt).
4. Kéo thả file shortcut vừa tạo vào thư mục Startup đã mở ở Bước 2.
5. Xong! Từ nay mỗi khi bật máy tính, tool sẽ tự động chạy ngầm.

---

## 👥 CẤU HÌNH CHO NHÂN VIÊN KHÁC

Tool được thiết kế hoàn toàn linh hoạt để chuyển giao cho nhân viên khác dùng:
1. Mở file [`config.properties`](file:///D:/03.Work/WarningToolHRM/config.properties):
   * Thay đổi `microsoft.username` và `microsoft.password` sang tài khoản của nhân viên mới.
   * Cập nhật `telegram.chat.id` sang tài khoản Telegram của nhân viên mới.
2. **Cơ chế tự bảo vệ dữ liệu (Auto-purge)**:
   * Tool tự động nhận biết khi đổi `username`, sẽ **tự động xóa sạch token cũ** và profile trình duyệt cũ để tránh nhầm lẫn dữ liệu công của người trước.
   * Khởi động tool bình thường qua `run-background.vbs`.

---

## 🔍 CHẨN ĐOÁN & XỬ LÝ SỰ CỐ (TROUBLESHOOTING)

| Triệu chứng | Nguyên nhân | Cách khắc phục |
| :--- | :--- | :--- |
| **System Tray báo: "Mất kết nối Internet hoặc lỗi DNS"** | Máy tính đang mất mạng hoặc DNS không phân giải được `hrm.gm-group.vn` | Kiểm tra lại dây mạng/Wifi công ty. Tool sẽ tự động kết nối lại khi có mạng. |
| **System Tray báo: "Máy chủ HRM bảo trì (HTTP 502/503)"** | Server nội bộ của công ty đang restart hoặc bảo trì | Không cần làm gì, tool ghi nhận log và sẽ thử lại mỗi 2 phút cho đến khi server phục hồi. |
| **Báo lỗi xác thực Microsoft SSO** | Đổi mật khẩu Microsoft nhưng chưa cập nhật vào tool | Mở file `config.properties`, cập nhật lại mật khẩu mới tại dòng `microsoft.password`. |
| **Microsoft yêu cầu 2FA / Authenticator** | Tài khoản bật xác thực 2 bước trên thiết bị lạ | Chuột phải vào biểu tượng khay hệ thống -> Chọn "Đăng nhập lại SSO", hoặc mở file `config.properties` đổi `browser.headless=false` để trình duyệt hiện lên cho bạn bấm duyệt trên điện thoại 1 lần. |
| **Lỗi Playwright / Tràn ổ C (Failed to create driver)** | Ổ C bị đầy hoặc thiếu dung lượng khi giải nén driver | Tool đã tự động cách ly toàn bộ thư mục tạm về thư mục `temp/` trên ổ D và bật cờ `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1`. Đảm bảo ổ đĩa cài đặt còn trống tối thiểu 500MB. |
| **Không nhận được tin nhắn Telegram** | Chưa bấm Start bot hoặc điền sai Chat ID | Kiểm tra lại `telegram.bot.token` và `telegram.chat.id`. Mở bot trên Telegram bấm `/start` một lần. |

---

## 🧪 BÁO CÁO KIỂM THỬ CHẤT LƯỢNG (QA TEST REPORT)

* **Bộ kiểm thử tự động (JUnit 5 - 19/19 Tests Passed 100%)**:
  * ✅ `testWeekendNoAlerts`: Cuối tuần (Thứ 7, CN) tuyệt đối không phát cảnh báo.
  * ✅ `testHolidayNoAlerts`: Ngày nghỉ lễ (Teambuilding, Quốc Khánh...) tự động bỏ qua cảnh báo.
  * ✅ `testMorningReminderAt0845`: Nhắc nhở chấm công đúng các mốc quy định (08:30, 08:45, 08:55).
  * ✅ `testAntiSpamMorningReminder`: Chống spam, không gửi lặp cảnh báo trong cùng mốc giờ.
  * ✅ `testLateCheckInRedAlert`: Phát báo động đỏ khẩn cấp sau 09:00.
  * ✅ `testCheckInConfirmed`: Tính đúng giờ về tối thiểu (+9h gồm làm việc và nghỉ trưa).
  * ✅ `testNoonCheckOutReminderWhenCheckedIn`: Sáng đã check-in ➔ Đến trưa (12:00) nhắc check-out 1 lần phòng trường hợp chiều nghỉ làm; chống spam không gửi lặp.
  * ✅ `testNoonCheckOutReminderNotFiredIfAlreadyCheckedOut`: Buổi trưa đã check-out từ trước (ví dụ 11:55) ➔ Tự động không nhắc nữa.
  * ✅ `testReached8Hours`: Thông báo chính xác khi hoàn thành đủ 8 tiếng.
  * ✅ `testEveningReminderAt1805`: Nhắc nhở check-out đúng các mốc giờ chiều/tối.
  * ✅ `testCompletedWorkNoAlert`: Đã check-out đủ công thì không cảnh báo sai.
  * ✅ `testMissingCheckInWithCheckOut`: Sáng quên check-in nhưng chiều có check-out ➔ Báo động đỏ khẩn cấp nhắc làm đơn giải trình.
  * ✅ `testEarlyCheckOutIncomplete`: Bấm check-out về sớm/thiếu giờ ➔ Cảnh báo chính xác số giờ đã làm và số phút còn thiếu.
  * ✅ `testMultipleCheckOuts_IncompleteThenFullWork`: Luồng đa Check-out (Lần 1, 2 chưa đủ ➔ Lần 3 đủ công ➔ Bắn chúc mừng và tắt nhắc nhở).
  * ✅ `testRemindReCheckOutAtTargetTimeAfterEarlyCheckOut`: Tự động nhắc bấm Check-out lại khi đồng hồ chạm mốc 8 tiếng (sau khi từng bấm về sớm).
  * ✅ `testUserChangeInvalidation`: Tự động hủy token cũ khi thay đổi tài khoản nhân viên.
  * ✅ `testExpiredToken`: Kiểm tra hạn và tự động làm mới Bearer token khi hết hạn.
  * ✅ `testPlaywrightDriverInitWithoutBrowserDownload`: Khởi tạo Playwright driver nhẹ, không tải trình duyệt dư thừa.
  * ✅ `testSsoLoginLaunch`: Kiểm thử luồng tự động đăng nhập ngầm qua Microsoft Edge và hệ thống Keycloak SSO.

* **Kiểm thử trực tiếp với Server thật (`http://hrm.gm-group.vn` & `login.microsoftonline.com`)**:
  * ✅ Đăng nhập tự động qua Keycloak (nút GM Account) ➔ Microsoft Account ➔ HRM System hoàn tất trong **34 giây**.
  * ✅ Chặn bắt và lưu thành công Bearer Token JWT hợp lệ (`role: EMPLOYEE`, `employeeid: ******`).
  * ✅ Lấy thành công dữ liệu chấm công tháng 10/2026.
  * ✅ Lấy thành công danh sách các ngày nghỉ lễ từ `/api/holidays`.
  * ✅ Lấy và khởi tạo mốc theo dõi duyệt đơn từ `/api/notifications/mine`.
  * ✅ Đóng gói production FAT JAR ([`WarningToolHRM-1.0.0.jar`](file:///D:/03.Work/WarningToolHRM/target/WarningToolHRM-1.0.0.jar)) thành công 100% và sẵn sàng release!

---

## 📦 CẤU TRÚC THƯ MỤC PHÁT HÀNH (RELEASE PACKAGE)

```text
WarningToolHRM/
├── config.properties          # File cấu hình duy nhất (Tài khoản, Telegram, Khung giờ)
├── run-background.vbs         # Khởi chạy ngầm (Không hiện màn hình đen, khuyến nghị)
├── run.bat                    # Khởi chạy hiển thị cửa sổ console (Dùng khi debug)
├── stop.bat                   # Dừng toàn bộ tiến trình tool đang chạy ngầm
├── README.md                  # Tài liệu hướng dẫn sử dụng chi tiết
├── target/
│   └── WarningToolHRM-1.0.0.jar  # File thực thi Java duy nhất đã đóng gói thư viện
├── logs/                      # Thư mục lưu nhật ký tự động (tự xoá sau 30 ngày)
│   ├── hrm-tool-YYYY-MM-DD.log
│   └── attendance-summary-YYYY-MM-DD.log
├── temp/                      # Thư mục chứa file tạm của Playwright/Java (tránh đầy ổ C)
├── playwright_profile/        # Thư mục lưu cache phiên đăng nhập của trình duyệt
├── token.json                 # Cache Bearer JWT an toàn (tự động tạo)
└── notification_state.json    # Mốc id thông báo duyệt đơn đã gửi (chống spam)
```

---

## 📝 NHẬT KÝ PHIÊN BẢN (CHANGELOG)

### Phiên Bản v1.0.0 (Official Release — 13/10/2026)
* 🚀 **Core**: Tự động đăng nhập ngầm qua Playwright (Microsoft Edge), tự động lấy và gia hạn Bearer Token JWT.
* ⏰ **Giờ làm việc linh hoạt**: Hỗ trợ khung giờ vào 08:30 – 09:00, tự động cộng 9 tiếng (8h làm việc + 1h trưa 12:00 – 13:00) để tính mốc về tối thiểu.
* 🍱 **Nhắc nhở nghỉ trưa (12:00 – 13:00)**: Tự động nhắc Check-out lúc 12:00 phòng trường hợp buổi chiều nghỉ làm để tránh mất nửa ngày công ca sáng; cơ chế chống spam chỉ gửi 1 lần.
* 🔄 **Multi-Checkout Intelligence**: Hỗ trợ nhân viên check-out nhiều lần trong ngày; cảnh báo số phút còn thiếu cho mỗi lần bấm sớm; tự động nhắc check-out lại khi đến giờ đủ công; chúc mừng khi chốt `ĐỦ CÔNG`.
* 🚨 **Edge Cases**: Cảnh báo khẩn cấp khi có Check-out nhưng thiếu Check-in sáng; Cảnh báo Shutdown Hook khi tắt máy tính mà quên check-out hoặc check-out chưa đủ công.
* 🏖️ **Lịch làm việc**: Tự động bỏ qua Thứ 7, Chủ Nhật và lịch nghỉ lễ lấy từ API công ty.
* 🔔 **Duyệt đơn realtime**: Bắn thông báo kết quả duyệt đơn Nghỉ phép, Remote, OT tức thì lên Telegram.
* 📱 **Giao diện Telegram chuẩn Mobile**: Tiêu đề IN HOA súc tích (<25 ký tự không rớt chữ), đường kẻ 16 ký tự, khoảng cách bố cục đối xứng, in đậm nổi bật mốc giờ và hành động.
* 💻 **Giao diện**: Windows System Tray (Khay hệ thống) với biểu tượng chữ H, menu chuột phải nhanh và Desktop Toasts.
* 📂 **Quản lý Log**: Tự động xoá file log sau 30 ngày (1 tháng).