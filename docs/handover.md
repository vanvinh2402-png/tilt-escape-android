# Hồ sơ bàn giao Tilt Escape 1.0

Tilt Escape hiện là ứng dụng Android Java có thể build, cài đặt và chơi được bằng cảm biến gia tốc. Gameplay cốt lõi, tài khoản local, SQLite, điểm cá nhân, Leaderboard local, giao diện Neon Focus, Hướng dẫn chơi và Cài đặt độ nhạy nghiêng đã hoàn thành.

## Thành phần bàn giao

- Mã nguồn: nhánh `main` tại https://github.com/vanvinh2402-png/tilt-escape-android
- APK release đã ký: `app/build/outputs/apk/release/app-release.apk`
- Cấu trúc SQLite: [database-schema.md](database-schema.md)
- Kết quả kiểm thử giao diện: [test-results-neon-focus-ui.md](test-results-neon-focus-ui.md)
- Kết quả kiểm thử Hướng dẫn chơi: [test-results-how-to-play.md](test-results-how-to-play.md)
- Kết quả kiểm thử Bảng xếp hạng: [test-results-leaderboard.md](test-results-leaderboard.md)
- Hướng dẫn ký release: [release-signing.md](release-signing.md)
- Hướng dẫn build, cài đặt và sử dụng: tài liệu này và `README.md`

## Cấu hình kỹ thuật

| Thành phần | Giá trị |
|---|---|
| Nền tảng | Android |
| Ngôn ngữ | Java 17 |
| UI | XML Views và Canvas |
| Cơ sở dữ liệu | SQLite, schema version 2 |
| Cảm biến | Accelerometer |
| Hướng màn hình | Dọc |
| Minimum SDK | API 24 |
| Target và Compile SDK | API 35 |
| Application ID | `com.vanvinh2402.tiltescape` |
| Phiên bản | `versionCode 1`, `versionName 1.0` |

## Chức năng đã hoàn thành

| Nhóm | Frame | Mã yêu cầu | Kết quả |
|---|---:|---|---|
| Đăng ký | 2 | R01–R05 | Hoàn thành kiểm tra dữ liệu, chống trùng tên, PBKDF2 và lưu SQLite |
| Đăng nhập | 1 | R06–R09 | Hoàn thành xác thực, thông báo lỗi và phiên đăng nhập local |
| Main Menu | 3 | R10 | Hoàn thành thông tin người chơi, Start, Hướng dẫn chơi, Cài đặt, Thành tích và Logout |
| Hướng dẫn chơi | 4 | R59 | Hoàn thành màn riêng hướng dẫn nghiêng máy, né vật cản để cộng điểm, va chạm kết thúc; có Bắt đầu chơi, Quay lại và phím Back |
| Cài đặt | 8 | R60–R61 | Hoàn thành ba mức độ nhạy nghiêng, lưu local và áp dụng ngay; có Quay lại và phím Back |
| Leaderboard | 9 | R62–R65 | Hoàn thành bảng xếp hạng local: một dòng/tài khoản theo điểm cao nhất, sắp xếp giảm dần, tối đa 10 dòng, có trạng thái rỗng và Quay lại/phím Back |
| Khởi động game | 3, 5 | R11–R13 | Hoàn thành reset ván, điểm 0, khu vực chơi và HUD |
| Cảm biến | 5 | R14–R16 | Hoàn thành phát hiện accelerometer, đọc mẫu và điều khiển trái/phải |
| Độ nhạy | 5 | R17 | Có lọc, dead zone, giới hạn di chuyển và ba mức tùy chỉnh từ Settings |
| Chướng ngại vật | 5 | R18–R20 | Hoàn thành sinh ở nhiều lane, chuyển động, loại bỏ và sinh lượt mới |
| Điểm số | 5 | R21–R24 | Điểm bắt đầu từ 0, cộng 1 cho mỗi vật né được, tăng độ khó theo thời gian và lưu điểm cao cá nhân |
| Va chạm | 5, 7 | R25–R28 | Hoàn thành va chạm, dừng game, điểm cuối và Game Over |
| Điều khiển ván | 6, 7 | R29–R32 | Hoàn thành Play Again, Restart, Pause, Resume và Main Menu |
| Đăng xuất | 3 | R33–R35 | Hoàn thành xóa phiên và quay lại Login |
| Vòng đời | 5, 6 | R66 | Tự Pause khi ứng dụng xuống nền, không tạo vòng lặp hoặc đăng ký cảm biến trùng |
| Thiếu cảm biến | 3 | R67 | Hiển thị cảnh báo và vô hiệu hóa Start; tài khoản và Thành tích vẫn dùng được |
| Phi chức năng | Toàn ứng dụng | R36–R42, R44, R46–R58 | Có delta time, giới hạn độ khó, phân lớp, kiểm tra đầu vào, password hash, test tự động và giao diện nhất quán |

R22 trong SRS mô tả tăng điểm theo thời gian sống sót. Theo quyết định đã thống nhất trong quá trình phát triển, phiên bản này thay bằng cách cộng 1 điểm cho mỗi chướng ngại vật né thành công để điểm dễ hiểu và không tăng quá nhanh.

## Chức năng chưa hoàn thành hoặc mới hoàn thành một phần

| Nội dung | Frame hoặc mã yêu cầu | Trạng thái thực tế |
|---|---|---|
| Thông báo kỷ lục mới tại Game Over | Frame 7 | Chưa hiển thị; điểm cao nhất vẫn được lưu và xem tại Thành tích |
| Tương thích nhiều kích thước màn hình | R43 | Đã kiểm tra Pixel 7 và test insets API 24/35; chưa nghiệm thu thủ công trên nhiều thiết bị thật |
| Ổn định khi xem Leaderboard | R45 | Đã có xử lý callback cũ sau khi đóng màn hình; kiểm tra thủ công Pixel 7 vẫn chờ ADB |
| Thiết bị Android thật | Tiêu chí nghiệm thu | Chưa kiểm thử; hiện đã kiểm thử trên Android Emulator Pixel 7 |

Âm thanh, rung, chơi trực tuyến, cửa hàng vật phẩm, nhiều nhân vật và nhiều bản đồ không được triển khai vì nằm ngoài phạm vi cốt lõi hoặc là tùy chọn.

## Build dự án

Yêu cầu Android Studio có Android SDK 35 và JDK 17 hoặc 21.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\CongQuyManh\AppData\Local\Android\Sdk'
.\gradlew.bat clean testDebugUnitTest assembleDebug lintDebug
```

Build APK release đã ký trên máy phát triển:

```powershell
.\gradlew.bat clean testDebugUnitTest assembleRelease lintDebug
```

Máy khác cần keystore và `keystore.properties` riêng theo [release-signing.md](release-signing.md). Không đưa hai file này lên Git.

## Cài đặt

Cài bản release mới:

```powershell
adb install app\build\outputs\apk\release\app-release.apk
```

Cập nhật một bản release đã ký bằng cùng keystore:

```powershell
adb install -r app\build\outputs\apk\release\app-release.apk
```

Bản debug và release dùng khóa khác nhau nên không thể cài đè lẫn nhau. Gỡ ứng dụng sẽ xóa tài khoản, phiên và kết quả SQLite local.

## Cách sử dụng

1. Mở ứng dụng và chọn **Tạo tài khoản**.
2. Nhập tên người dùng 3–20 ký tự gồm chữ, số hoặc dấu gạch dưới; nhập mật khẩu ít nhất 6 ký tự và xác nhận.
3. Đăng nhập bằng tài khoản vừa tạo.
4. Chọn **Hướng dẫn chơi** để xem cách điều khiển, cách tính điểm và điều kiện kết thúc; chọn **Bắt đầu chơi** hoặc **Quay lại**.
5. Chọn **Bắt đầu** và nghiêng điện thoại sang trái hoặc phải để di chuyển.
6. Mỗi chướng ngại vật đi hết màn hình mà không va chạm sẽ cộng 1 điểm.
7. Dùng **Tạm dừng**, **Tiếp tục**, **Chơi lại** hoặc **Màn hình chính** để điều khiển ván.
8. Chọn **Thành tích** tại Main Menu để xem điểm cao nhất và 10 lượt gần nhất của tài khoản.
9. Chọn **Bảng xếp hạng** để xem tối đa 10 tài khoản, mỗi tài khoản hiển thị điểm cao nhất local.
10. Chọn **Đăng xuất** để kết thúc phiên local.

Trên Android Emulator, mở Extended Controls, chọn Virtual sensors rồi Device Pose để mô phỏng nghiêng thiết bị.

## Kịch bản và kết quả kiểm thử

| ID | Kịch bản | Kết quả mong đợi | Kết quả |
|---|---|---|---|
| TC01 | Cài sạch APK release đã ký | Cài thành công và mở Login | Đạt |
| TC02 | Đăng ký tài khoản hợp lệ | Lưu SQLite và báo thành công | Đạt |
| TC03 | Đăng nhập đúng/sai | Đúng vào Main Menu; sai có thông báo | Đạt |
| TC04 | Đóng và mở lại ứng dụng | Khôi phục phiên đăng nhập | Đạt |
| TC05 | Logout | Xóa phiên và quay về Login | Đạt |
| TC06 | Mở bàn phím tại Register | Form co/cuộn, nút cuối vẫn truy cập được | Đạt |
| TC07 | Bắt đầu game | Điểm 0, nhân vật và vật cản xuất hiện | Đạt |
| TC08 | Mô phỏng accelerometer | Nhân vật di chuyển ngang và không ra ngoài biên | Đạt |
| TC09 | Né một vật cản | Điểm tăng đúng 1 | Đạt |
| TC10 | Va chạm | Dừng game và hiện điểm cuối | Đạt |
| TC11 | Pause/Resume | Dừng và tiếp tục đúng trạng thái | Đạt |
| TC12 | Restart/Play Again | Tạo ván mới và đưa điểm về 0 | Đạt |
| TC13 | Mở Thành tích sau Game Over | Lưu điểm cao và lịch sử theo tài khoản | Đạt |
| TC14 | Force-stop và mở lại | SQLite và phiên còn tồn tại | Đạt |
| TC15 | Chạy unit test và Android Lint | Không có test thất bại hoặc lint error | Đạt: 145 test, 0 lỗi; lint 0 error |
| TC18 | Đổi độ nhạy nghiêng | Thấp/Trung bình/Cao được lưu và áp dụng | Đạt tự động; kiểm tra Pixel 7 chờ ADB |
| TC16 | Xác minh chữ ký release | APK xác minh bằng apksigner | Đạt: v2, một signer |
| TC17 | Mở Hướng dẫn chơi trên Pixel 7 | Mở, Quay lại và Bắt đầu chơi đúng luồng | Chờ kiểm tra: giả lập chưa kết nối ADB vào ngày 2026-11-18 |

Lần cài sạch release cuối ngày 2026-11-06 trên `emulator-5554` thành công; ứng dụng mở màn Login, không ghi nhận fatal exception, ANR hoặc SQLite exception.

## Thông tin APK release

- Tệp: `app/build/outputs/apk/release/app-release.apk`
- Kích thước: 2,562,412 byte
- SHA-256: `A54649F4AF6067AD0503A73EFB84346DF5E0AA8DDC9FF1F4756BB7D42613DF6A`
- Chữ ký: APK Signature Scheme v2, RSA 4096
- Certificate SHA-256: `84c6febb235863b8a9cf271ed88322c85616c013158fd7c22963b04feaf13bf0`

## Checklist bàn giao

- [x] Mã nguồn Android Java hoàn chỉnh cho phạm vi đã triển khai
- [x] Dự án mở và build được bằng Android Studio/Gradle
- [x] SQLite và mô tả cấu trúc dữ liệu
- [x] APK release đã ký để cài đặt
- [x] Danh sách chức năng hoàn thành
- [x] Kịch bản và kết quả kiểm thử
- [x] Hướng dẫn build, cài đặt và sử dụng
- [x] Danh sách lỗi, giới hạn và chức năng chưa hoàn thành
