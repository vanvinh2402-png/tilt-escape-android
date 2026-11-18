# Kết quả kiểm thử Settings / Tilt Sensitivity

Ngày: 2026-11-18 (Asia/Bangkok)

Lệnh xác minh:

```powershell
.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug assembleRelease
```

Kết quả: build tạo APK debug/release; 145 unit tests, 0 failures, 0 errors; Android Lint không có lỗi chặn build.

Đã kiểm tra tự động:

- Mặc định và dữ liệu preference không hợp lệ đều về Trung bình.
- Lựa chọn độ nhạy được lưu qua instance store mới.
- Mức Thấp/Trung bình/Cao lần lượt cho phản hồi thấp/trung bình/cao, đầu ra vẫn bị clamp trong [-1, 1].
- Settings là overlay cuộn được, chứa ba lựa chọn và nút Quay lại.
- Thiết bị không có accelerometer ẩn action Settings.

Kiểm tra Pixel 7 vẫn chờ vì ADB chưa nhận diện thiết bị/emulator trong lượt kiểm tra này.
