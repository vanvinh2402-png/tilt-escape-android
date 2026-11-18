# Kết quả kiểm thử màn Hướng dẫn chơi

Ngày: 2026-11-18 (Asia/Bangkok)

## Kiểm thử tự động

Lệnh đã chạy:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME='C:\Users\CongQuyManh\AppData\Local\Android\Sdk'
.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug assembleRelease
```

Kết quả: **BUILD SUCCESSFUL**.

- Unit test: 141
- Failures: 0
- Errors: 0
- Android Lint: 0 errors (warnings không chặn build)
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK đã ký: `app/build/outputs/apk/release/app-release.apk`
- Release APK size: 2,556,564 bytes
- SHA-256: `414F799801D570120455868C9A4850FD92C7527CFA829B84C8C2639C337B9BA0`
- `apksigner verify --verbose`: một signer, APK Signature Scheme v2 hợp lệ.

Các test mới kiểm tra:

- Panel Hướng dẫn chơi, nút mở từ menu, Bắt đầu chơi và Quay lại tồn tại; hai action nằm trong `ScrollView`.
- Hướng dẫn chỉ mở khi menu được phép ở trạng thái `READY`.
- Mở hướng dẫn ẩn menu; Quay lại hoặc phím Back trả về menu.
- Bắt đầu chơi gọi lại luồng Start có sẵn và đóng overlay.

## Kiểm tra Pixel 7

Đã thử cài release bằng ADB vào ngày 2026-11-18, nhưng không có emulator/thiết bị nào được ADB nhận diện (`no devices/emulators found`). Vì vậy chưa đánh dấu kiểm tra thủ công là đạt.

Khi Pixel 7 đang chạy, thực hiện nhanh:

1. Đăng nhập, tại menu chọn **Hướng dẫn chơi**.
2. Xác nhận ba hướng dẫn và hai nút đều đọc được, có thể cuộn trên màn hình thấp.
3. Chọn **Quay lại**, xác nhận về menu.
4. Mở lại, chọn **Bắt đầu chơi**, xác nhận game chạy ở điểm 0.
