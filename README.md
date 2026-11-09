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
* Sử dụng thư viện **Microsoft Playwright 1.49** để tự động đăng nhập ngầm không gián đoạn:
  * Mặc định sử dụng **Chromium chuẩn** nhẹ và ổn định, hoặc linh hoạt tùy chọn Microsoft Edge / Google Chrome.
  * Tích hợp sẵn cơ chế **chống ép HTTPS ngầm (Auto-HTTPS bypass)** giúp truy cập trơn tru vào máy chủ nội bộ HRM chạy HTTP không mã hóa (tránh lỗi `ERR_BLOCKED_BY_CLIENT`).
  * Cơ chế bắt Bearer JWT token **phi đồng bộ in-memory** an toàn, triệt tiêu hoàn toàn nguy cơ nghẽn đường ống IPC (Deadlock) trong quá trình xác thực đa bước của Microsoft.
* Tự động nhận diện nút SSO **"GM Account"** trên hệ thống xác thực Keycloak của công ty để chuyển hướng đăng nhập tài khoản Microsoft tổ chức.
* Tự động lấy và gia hạn Bearer Token JWT khi token hết hạn hoặc máy chủ trả về `401 Unauthorized`.
* Tự động lưu cache phiên vào thư mục `playwright_profile` để đăng nhập siêu tốc ở các lần tiếp theo.
* **Cơ chế chống tràn ổ C**: Toàn bộ dữ liệu tạm thời của trình duyệt và Java được cách ly về thư mục `temp/` trên ổ đĩa dự án (ổ D), tuyệt đối không gây lỗi đầy ổ đĩa hệ thống `C:\`.

### 2. ⏰ Quy Tắc Cảnh Báo Chấm Công Thông Minh (Giờ Linh Hoạt)

Hệ thống được thiết kế bám sát thực tế quy chế giờ làm việc linh hoạt của công ty, tự động giám sát và thích ứng theo từng trạng thái:

* **🏖️ Nhận biết miễn trừ cảnh báo thông minh**:
  * **Cuối tuần**: Tự động tắt toàn bộ cảnh báo vào Thứ Bảy và Chủ Nhật.
  * **Lịch nghỉ lễ công ty**: Tự động đồng bộ từ API `http://hrm.gm-group.vn/api/holidays` (Quốc khánh, Tết, Teambuilding...), tự động bỏ qua cảnh báo vào ngày lễ.
  * **Đơn nghỉ phép cá nhân (Đồng bộ từ `/api/leave/requests/mine`)**:
    * **Nghỉ phép cả ngày**: Tự động tắt toàn bộ cảnh báo chấm công trong ngày.
    * **Nghỉ phép ca sáng**: Tự động miễn trừ nhắc nhở chấm công sáng.
    * **Nghỉ phép ca chiều**: Tự động miễn trừ các nhắc nhở Check-out chiều và tối.
  * **Trạng thái chấm công trên HRM**: Tự động bỏ qua nếu bản ghi ngày đó đã ghi nhận trạng thái `NGHỈ PHÉP` / `LEAVE`.

* **🌅 Khung giờ sáng linh hoạt (08:30 – 09:00)**:
  * Nhắc nhở chấm công định kỳ vào các mốc cấu hình: **`08:30`**, **`08:45`**, **`08:55`** nếu hệ thống chưa ghi nhận Check-in (chống spam: mỗi mốc chỉ gửi 1 lần).
  * **🚨 Báo động đỏ khẩn cấp sau `09:00`**: Nếu sau `09:00` mà bạn vẫn chưa bấm Check-in (và không có đơn nghỉ sáng), tool gửi cảnh báo khẩn cấp (chỉ gửi duy nhất 1 lần trong buổi sáng trước `12:00` để tránh làm phiền).

* **✅ Xác nhận Check-in & Tự động tính mốc về tối thiểu (+9 tiếng)**:
  * Ngay khi bạn vừa bấm Check-in (ví dụ vào lúc `08:44`), tool gửi tin nhắn Telegram xác nhận check-in thành công 1 lần duy nhất.
  * Tự động cộng span **9 tiếng** (gồm **8 tiếng làm việc** tiêu chuẩn + **1 tiếng nghỉ trưa** cố định `12:00 – 13:00`), tính ra chính xác mốc giờ về tối thiểu (ví dụ vào `08:44` ➔ giờ về tối thiểu là **`17:44`**).

* **🍱 Nhắc nhở Check-out nghỉ trưa (12:00 – 13:00)**:
  * Nếu buổi sáng đã Check-in, đến đúng **`12:00`** tool gửi thông báo nhắc Check-out nghỉ trưa: đề phòng trường hợp bạn có việc bận đột xuất hoặc nghỉ phép ca chiều không đi làm, giúp hệ thống HRM chốt và ghi nhận đủ nửa ngày công ca sáng thay vì bị mất công.
  * Nếu bạn đã chủ động bấm Check-out từ trước đó (ví dụ 11:55 ra về), hệ thống tự động không gửi nhắc nhở nữa.
  * Cơ chế chống spam: Chỉ gửi duy nhất **1 lần trong ngày** trong suốt khung giờ nghỉ trưa (`12:00 – 13:00`).

* **🎉 Thông báo đạt mốc hoàn thành đủ 8 tiếng công**:
  * Khi đồng hồ vượt qua mốc giờ về tối thiểu (`checkIn + 9h`), tool tự động gửi thông báo chúc mừng: *"Đã hoàn thành đủ 8 tiếng làm việc hôm nay"*, nhắc nhở bạn có thể chuẩn bị Check-out khi kết thúc ca làm.

* **🌇 Nhắc nhở Check-out chiều & tối**:
  * Nếu đang làm việc mà chưa Check-out, tool sẽ gửi thông báo nhắc nhở vào các mốc cấu hình: **`17:35`**, **`18:05`**, **`18:30`**, **`19:00`**, **`20:00`** (mỗi mốc chỉ nhắc 1 lần duy nhất).

* **🔄 Xử lý Đa Check-out & Cảnh báo về sớm / Chưa đủ công**:
  * **Trường hợp 1 - Check-out sớm (Chưa đủ công)**:
    * Bấm check-out sớm (ví dụ vào `08:50`, bấm về lúc `16:30`): Tool lập tức cảnh báo chi tiết số giờ đã làm (7h40m), mốc về yêu cầu (`17:50`), và **số phút còn thiếu** (thiếu 80 phút).
    * Hỗ trợ đếm số lần bấm: Cảnh báo rõ ràng cho Lần 1, Lần 2, Lần 3...
    * **Tự nhắc Check-out lại khi đến giờ đủ công**: Nếu bạn ở lại văn phòng làm tiếp, đúng lúc chạm mốc 8 tiếng (`17:50`), tool sẽ tự động gửi thông báo: *"Đã đến mốc đủ công, hãy bấm Check-out lại trên HRM ngay!"*.
    * Tiếp tục nhắc nhở các mốc chiều/tối tiếp theo để đảm bảo bạn không quên bấm lại trước khi ra về.
  * **Trường hợp 2 - Check-out ĐỦ CÔNG**:
    * Ngay khi bạn bấm Check-out lại và đạt `ĐỦ CÔNG` (ví dụ `18:00`), tool gửi thông báo chúc mừng: *"🎉 Check-out thành công (Đủ công)"* kèm tổng thời gian làm việc thực tế trong ngày, đồng thời **tự động dừng mọi nhắc nhở chiều/tối sau đó**.
  * **Chống spam**: Nếu bạn không bấm lại (thời gian Check-out trên HRM giữ nguyên), tool tuyệt đối không gửi lại thông báo trùng lặp.

* **🚨 Cảnh báo bất thường: Sáng KHÔNG Check-in mà chiều lại có Check-out**:
  * Nếu sáng quên chấm công vào nhưng chiều lại có bản ghi ra, tool phát hiện trạng thái bất thường và bắn báo động đỏ khẩn cấp, hướng dẫn bạn vào HRM làm đơn Giải trình quên check-in ngay để không bị trừ công.

* **🛡️ Cơ chế Chống Spam Đa Tầng**:
  * Trạng thái gửi tin được lưu trữ và tự động reset hoàn toàn vào đầu ngày mới (`00:00`).
  * Mọi thông báo theo mốc giờ hoặc theo sự kiện đều có cờ đánh dấu, đảm bảo không bao giờ gửi tin nhắn rác hoặc trùng lặp lên điện thoại của bạn.

### 3. 🔔 Giám Sát Realtime & Bắn Thông Báo Duyệt Đơn (Nghỉ Phép, Remote, OT)
* Tự động gọi API `http://hrm.gm-group.vn/api/notifications/mine` mỗi 2 phút.
* Bắn thông báo tức thì lên Telegram khi có kết quả duyệt:
  * ✅ **Đơn nghỉ phép năm** đã được duyệt.
  * ✅ **Đăng ký Remote (làm việc từ xa)** đã được duyệt (kèm số công được duyệt).
  * ✅ **Đơn làm thêm giờ (OT)** đã được duyệt (kèm số giờ và tên dự án).
  * ✅ **Thông báo lịch nghỉ lễ mới**.
* Có cơ chế lưu mốc (`notification_state.json`) để **chống spam**, chỉ gửi các thông báo mới phát sinh.

### 4. 🤖 Tra Cứu Dữ Liệu Tương Tác Qua Telegram Bot (Bot Commands)
Bạn có thể nhắn tin trực tiếp cho Bot trên Telegram để tra cứu dữ liệu chấm công và nhân sự cá nhân bất cứ lúc nào:
* 📅 **`/chamcong` [tháng/năm]**: Xem chi tiết danh sách từng ngày chấm công trong tháng chỉ định (mặc định tháng hiện tại), thống kê số ngày đủ công, về sớm, giờ vào, giờ ra của từng ngày.
  * *Ví dụ*: `/chamcong` hoặc `/chamcong 09/2026`
* 🏖 **`/phep` [năm]**: Xem tổng số ngày phép năm, số ngày phép đã sử dụng và số ngày phép còn lại thông qua API `/api/leave/balance`.
  * *Ví dụ*: `/phep` hoặc `/phep 2026`
* 📋 **`/donnghi`**: Xem chi tiết danh sách các đơn xin nghỉ phép, nghỉ Remote và trạng thái duyệt gần đây.
* ⏰ **`/ot`**: Xem chi tiết danh sách các đơn đăng ký làm thêm giờ (OT), số giờ được duyệt, lý do, dự án và người phê duyệt thông qua API `/api/overtime/requests/mine`.
* ❓ **`/help`**: Xem menu hướng dẫn sử dụng bot.
* *Hỗ trợ menu gợi ý lệnh nhanh (Menu Button)*: Khi mở chat với bot, Telegram sẽ hiển thị sẵn danh sách lệnh để bạn bấm chọn trực tiếp không cần gõ.

### 5. 📱 Template Thông Báo Telegram Tối Ưu Cho Mobile
* Mỗi thông báo có đường viền phân cách `━━━━━━━━━━━━━━━━` (16 ký tự) trên đầu để phân biệt rõ ràng khi nhận nhiều tin nhắn liên tiếp.
* **Tiêu đề IN HOA ngắn gọn ($\le 24$ ký tự)**: Luôn nằm gọn trên 1 dòng duy nhất trên điện thoại, tuyệt đối không bị rớt lẻ loi một chữ xuống dòng tiếp theo.
* **Bố cục đối xứng**: Khoảng cách đều 1 dòng trống phía trên và phía dưới khối thông tin, mang lại trải nghiệm xem thoáng mắt.
* **In đậm nổi bật thông tin cốt lõi**: Mốc giờ ra yêu cầu (`17:50`), các hành động quan trọng (`Check-in ngay`, `Check-out ngay`, `ĐỦ CÔNG`) được làm nổi bật để dễ nắm bắt ngay khi nhìn lướt qua.

### 6. 📂 Quản Lý Log Tự Động & Retention 30 Ngày
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

# 3. Trình duyệt tự động hóa (Playwright)
# Để trống để dùng Chromium chuẩn (khuyên dùng nhất), hoặc điền 'msedge' / 'chrome'
browser.channel=
browser.headless=true
browser.user.data.dir=./playwright_profile

# 4. Cấu hình nhận cảnh báo qua Telegram Bot
telegram.enabled=true
telegram.bot.token=DÁN_BOT_TOKEN_VÀO_ĐÂY
telegram.chat.id=DÁN_CHAT_ID_VÀO_ĐÂY

# 5. Khung giờ làm việc linh hoạt
work.flexible.earliest.start=08:30
work.flexible.latest.start=09:00
work.required.span.hours=9

# 6. Các mốc giờ nhắc nhở sáng, trưa và chiều
reminders.morning=08:30,08:45,08:55
reminders.noon=12:00
reminders.evening=17:35,18:05,18:30,19:00,20:00

# 7. Chu kỳ kiểm tra định kỳ (phút) & Thời gian lưu log (ngày)
check.interval.minutes=1
log.retention.days=30
```

> [!TIP]
> **Bảo mật cấu hình cá nhân**: Bạn có thể tạo file `config.properties.local` tại thư mục gốc để điền tài khoản, mật khẩu và token riêng của mình. Tool sẽ tự động ưu tiên nạp các giá trị từ file này, và file `.local` đã được thiết lập tự động bỏ qua khỏi Git (`.gitignore`) để chống lộ thông tin.

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
| **Lỗi `net::ERR_BLOCKED_BY_CLIENT at http://hrm.gm-group.vn/`** | Trình duyệt Microsoft Edge kích hoạt cơ chế Automatic HTTPS chuyển đổi port 80 (HTTP) sang port 443 (HTTPS) và chặn lại | Để trống `browser.channel=` trong file cấu hình để chuyển sang dùng Chromium mặc định của Playwright, hoặc cập nhật bản build mới nhất đã tích hợp cờ tắt Auto-HTTPS. |
| **Gửi lệnh Telegram (`/chamcong`) nhưng bot không phản hồi** | Luồng xử lý đang bị chờ khóa (lock) do tiến trình SSO login trước đó gặp sự cố hoặc đang chờ xác thực | Chạy `stop.bat` để tắt toàn bộ tiến trình cũ, khởi động lại tool. Phiên bản mới đã xử lý triệt để nguy cơ nghẽn luồng IPC giữa Playwright và Bot. |
| **Lỗi xung đột file lock (`ProcessSingleton Lock / Lock file error 32`)** | Vô tình nhấp mở nhiều lần làm 2 tiến trình `javaw` cùng chạy nền | Chạy file `stop.bat` để dừng toàn bộ các phiên bản đang chạy ngầm, sau đó mở lại 1 lần duy nhất bằng `run-background.vbs`. |
| **System Tray báo: "Mất kết nối Internet hoặc lỗi DNS"** | Máy tính đang mất mạng hoặc DNS không phân giải được `hrm.gm-group.vn` | Kiểm tra lại dây mạng/Wifi công ty. Tool sẽ tự động kết nối lại khi có mạng. |
| **System Tray báo: "Máy chủ HRM bảo trì (HTTP 502/503)"** | Server nội bộ của công ty đang restart hoặc bảo trì | Không cần làm gì, tool ghi nhận log và sẽ thử lại mỗi 2 phút cho đến khi server phục hồi. |
| **Báo lỗi xác thực Microsoft SSO** | Đổi mật khẩu Microsoft nhưng chưa cập nhật vào tool | Mở file `config.properties` (hoặc `config.properties.local`), cập nhật lại mật khẩu mới tại dòng `microsoft.password`. |
| **Microsoft yêu cầu 2FA / Authenticator** | Tài khoản bật xác thực 2 bước trên thiết bị lạ | Chuột phải vào biểu tượng khay hệ thống -> Chọn "Đăng nhập lại SSO", hoặc mở file cấu hình đổi `browser.headless=false` để trình duyệt hiện lên cho bạn bấm duyệt trên điện thoại 1 lần. |
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

### Phiên Bản v1.0.1 (Maintenance Update — 09/11/2026)
* 🛡️ **Khắc phục lỗi `net::ERR_BLOCKED_BY_CLIENT`**: Bổ sung các cờ tắt tính năng Automatic HTTPS (`--disable-features=msEdgeAutomaticHttps,HttpsUpgrades,msSmartScreenProtection`) khi truy cập web HRM nội bộ chạy HTTP port 80.
* ⚡ **Khắc phục lỗi nghẽn luồng IPC Playwright (Deadlock)**: Chuyển đổi phương thức trích xuất Bearer token từ `request.headerValue()` sang `request.headers()` in-memory an toàn, ngăn chặn tình trạng treo luồng và đảm bảo Telegram Bot luôn phản hồi tức thì các lệnh tra cứu (`/chamcong`, `/phep`, `/donnghi`, `/ot`).
* 🌐 **Hỗ trợ Chromium chuẩn của Playwright**: Mặc định để trống `browser.channel=` giúp đăng nhập SSO ổn định tuyệt đối và độc lập với cấu hình trình duyệt cá nhân của Windows.
* 📑 **Bảo mật file cấu hình cục bộ (`config.properties.local`)**: Tự động ưu tiên nạp cấu hình tài khoản cá nhân độc lập và loại trừ hoàn toàn khỏi Git tracker.

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