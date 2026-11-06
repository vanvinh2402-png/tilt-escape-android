# TILT ESCAPE

Release signing and signed APK instructions: [docs/release-signing.md](docs/release-signing.md).

Project handover package, completed requirements, test cases and known gaps: [docs/handover.md](docs/handover.md).

## Giao diện Neon Focus

Ứng dụng dùng giao diện Neon Focus thống nhất cho Đăng nhập, Đăng ký, menu chính, Tạm dừng, Kết thúc và Thành tích. Form được đặt trong card tối có độ tương phản cao; ô nhập có màu chữ/hint rõ ràng, viền focus tím; nút chính dùng gradient tím–xanh và điểm nhấn xanh ngọc.

Giao diện giữ nguyên toàn bộ luồng tài khoản, SQLite và gameplay, hỗ trợ API 24–35, vùng chạm tối thiểu 48dp và ScrollView cho form xác thực trên màn hình dọc.

## Tài khoản local

Ứng dụng hiện yêu cầu tài khoản cục bộ trước khi chơi:

1. Chọn **Tạo tài khoản**, nhập tên người dùng 3–20 ký tự không dấu (chữ, số hoặc dấu gạch dưới) và mật khẩu tối thiểu 6 ký tự.
2. Sau khi đăng ký thành công, đăng nhập bằng tài khoản vừa tạo.
3. Phiên đăng nhập được giữ khi đóng/mở lại ứng dụng. Chọn **Đăng xuất** tại Màn hình chính để kết thúc phiên.
4. Điểm cao nhất và 10 lượt gần nhất được tách riêng theo từng tài khoản.

Mật khẩu không được lưu dạng rõ; ứng dụng lưu PBKDF2 hash với salt riêng trong SQLite. Tài khoản, phiên và thành tích chỉ tồn tại trên thiết bị hiện tại, không đồng bộ qua mạng.

Nếu thiết bị không có cảm biến gia tốc, người dùng vẫn có thể đăng ký, đăng nhập, đăng xuất và xem Thành tích; nút Bắt đầu sẽ bị vô hiệu hóa.

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

Chưa có khôi phục/đổi mật khẩu, màn How to Play riêng, Settings, bảng xếp hạng nhiều người chơi, âm thanh hoặc dịch vụ trực tuyến. Thành tích chỉ lưu cục bộ theo từng tài khoản trên thiết bị và sẽ mất khi xóa dữ liệu hoặc gỡ ứng dụng. Chiều và cảm giác nghiêng cuối cùng vẫn cần xác nhận trên thiết bị Android thật.

## Cấu hình dự án

- Ngôn ngữ: Java
- UI: XML Views và Canvas
- Minimum SDK: 24
- Target/Compile SDK: 35
- Application ID: `com.vanvinh2402.tiltescape`

## Chức năng đã hoàn thành và giới hạn

Đã hoàn thành tài khoản local, đăng ký, đăng nhập, khôi phục phiên, đăng xuất, SQLite v2, migration dữ liệu cũ và thành tích tách theo tài khoản.

Chưa có khôi phục mật khẩu, đổi mật khẩu, xóa tài khoản, Leaderboard/bảng xếp hạng trực tuyến hoặc đồng bộ nhiều thiết bị. Xóa dữ liệu ứng dụng hay gỡ cài đặt sẽ xóa tài khoản và thành tích local.
