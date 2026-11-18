# Kết quả kiểm thử Bảng xếp hạng

Ngày: 2026-11-18 (Asia/Bangkok)

## Phạm vi

- Bảng xếp hạng local SQLite lấy điểm cao nhất của mỗi tài khoản.
- Sắp xếp giảm dần theo điểm, sau đó tăng dần theo tên người chơi khi bằng điểm.
- Hiển thị tối đa 10 người chơi, hỗ trợ danh sách rỗng và Quay lại/phím Back.
- Bỏ qua phản hồi tải dữ liệu đã cũ sau khi người dùng rời màn hình.

## Kiểm thử tự động

Lệnh xác minh:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug assembleRelease
```

Kết quả:

- 149 unit tests, 0 failures, 0 errors.
- `LeaderboardUiControllerTest`: 3/3 đạt (mở/trạng thái rỗng, dữ liệu có tên + điểm, callback cũ sau khi đóng).
- `SQLiteGameResultRepositoryTest`: xác minh một người chơi chỉ có một dòng theo điểm cao nhất và thứ tự xếp hạng.
- Debug APK và signed release APK được tạo thành công.
- Android Lint hoàn tất không có lỗi chặn build; báo cáo có 22 cảnh báo sẵn có.

Kiểm tra thủ công trên Pixel 7 vẫn chờ vì ADB chưa nhận emulator trong phiên này.
