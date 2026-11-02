# TILT ESCAPE

TILT ESCAPE là game Android màn hình dọc: người chơi nghiêng điện thoại sang trái hoặc phải để né các chướng ngại vật đang rơi.

## Trạng thái hiện tại

Gameplay v2 đã có vòng chơi cục bộ hoàn chỉnh:

- Chướng ngại vật xuất hiện ở nhiều làn và không lặp lại ngay cùng một làn.
- Mỗi chướng ngại vật được né an toàn và đi hết khỏi màn hình sẽ cộng 1 điểm.
- Độ khó vẫn tăng theo mỗi 15 giây; tốc độ rơi, tần suất và số vật cản đều có giới hạn an toàn.
- Có Bắt đầu, Tạm dừng, Tiếp tục, Chơi lại, Màn hình chính và Chơi lại sau Game Over.
- Khi ứng dụng xuống nền trong lúc chơi, game tự tạm dừng và không tự chạy lại khi quay về.
- Giao diện tránh vùng status/navigation bar trên Android API 24 đến API 35.
- Kết quả Game Over được lưu cục bộ bằng SQLite; màn hình **Thành tích** hiển thị điểm cao nhất và 10 lượt gần nhất kể cả sau khi đóng/mở lại ứng dụng.

## Yêu cầu và cách chạy

- Android Studio, Android SDK 35 và JDK 17 hoặc 21.
- Thiết bị API 24 trở lên có cảm biến gia tốc, hoặc Android Emulator hỗ trợ Virtual sensors.

Clone repository, mở thư mục gốc trong Android Studio, chờ Gradle Sync, chọn thiết bị rồi chạy cấu hình `app`. Nhấn **Bắt đầu** và nghiêng thiết bị trái/phải để di chuyển.

Các nút **Tạm dừng**, **Tiếp tục**, **Chơi lại** và **Màn hình chính** điều khiển vòng chơi. Chơi lại luôn đặt điểm về 0; Tiếp tục giữ nguyên điểm.

Tại màn hình chính, nhấn **Thành tích** để xem điểm cao nhất và 10 lượt hoàn thành gần nhất. Mỗi lượt chỉ được lưu khi Game Over; tạm dừng, chơi lại giữa lượt hoặc mở lại màn hình Thành tích không tạo kết quả trùng. Cấu trúc dữ liệu được mô tả tại [`docs/database-schema.md`](docs/database-schema.md).

## Nghiêng điện thoại trên Android Emulator

Trong máy ảo, chọn **Extended Controls (`...`) → Virtual sensors → Device Pose**. Kéo hoặc xoay mô hình điện thoại sang trái/phải. Tab **Additional sensors** chỉ dành cho cảm biến môi trường, không giả lập thao tác nghiêng của game.

## Kiểm tra bằng dòng lệnh

```powershell
.\gradlew.bat --no-daemon testDebugUnitTest
.\gradlew.bat --no-daemon assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Giới hạn hiện tại

Chưa có đăng nhập, tài khoản, bảng xếp hạng trực tuyến, âm thanh, đồ họa hoàn thiện hoặc dịch vụ trực tuyến. Thành tích chỉ lưu cục bộ trên thiết bị và sẽ mất khi xóa dữ liệu hoặc gỡ ứng dụng. Chiều và cảm giác nghiêng cuối cùng vẫn cần xác nhận trên thiết bị Android thật.

## Cấu hình dự án

- Ngôn ngữ: Java
- UI: XML Views và Canvas
- Minimum SDK: 24
- Target/Compile SDK: 35
- Application ID: `com.vanvinh2402.tiltescape`
