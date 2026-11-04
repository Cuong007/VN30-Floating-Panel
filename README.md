# VN30 Floating Panel

Ứng dụng Android WebView dạng cửa sổ nổi, ép giao diện WebView sang Light Mode độc lập với Dark Mode của Android.

## Cách hoạt động

- Mở `http://127.0.0.1:8000/index_mini.html`
- Dùng lại các server Termux hiện có (đặc biệt API ở cổng 3002).
- Không tạo MQTT connection mới.
- Cho phép kéo thanh tiêu đề, đổi kích thước bằng dải đáy, tải lại và đóng cửa sổ.

## Điều kiện trên Lenovo Pad

Trước khi mở ứng dụng:
1. Khởi động các server VN30 hiện có trong Termux.
2. Đảm bảo static server phục vụ thư mục `VN30_PRO` tại cổng 8000.
3. Kiểm tra trên trình duyệt của máy: `http://127.0.0.1:8000/index_mini.html`.

Ứng dụng và Termux phải chạy trên cùng máy tính bảng.

## Build APK

Có thể build bằng Android Studio (JDK 17, Android SDK Platform 35) hoặc GitHub Actions.

### GitHub Actions
1. Tạo repository GitHub mới và upload toàn bộ nội dung thư mục dự án này.
2. Mở tab **Actions**.
3. Chạy workflow **Build VN30 Floating Panel APK** (hoặc push lên nhánh main).
4. Mở lần chạy thành công, tải artifact `VN30-Floating-Panel-APK`.
5. Giải nén để lấy `app-debug.apk`, chép sang Lenovo Pad và cài đặt.

## Cài đặt và sử dụng
1. Cài APK.
2. Mở VN30 Floating Panel.
3. Cấp quyền **Display over other apps / Hiển thị trên ứng dụng khác**.
4. Chọn **MỞ CỬA SỔ NỔI**.
5. Kéo thanh trên cùng để di chuyển; kéo dải dưới để đổi kích thước.

## Địa chỉ mặc định
`http://127.0.0.1:8000/index_mini.html`

Nếu cổng hoặc đường dẫn static server của bạn khác, sửa hằng URL trong `FloatingPanelService.java`, sau đó build lại.
