# Kết quả kiểm thử Kỷ lục mới

Ngày: 2026-11-18 (Asia/Bangkok)

## Phạm vi

- Khi kết thúc lượt chơi, so sánh điểm với điểm cao nhất của cùng tài khoản trước khi lưu lượt mới.
- Chỉ hiển thị **KỶ LỤC MỚI!** khi điểm lớn hơn nghiêm ngặt; điểm bằng không hiển thị thông báo.
- Bỏ qua callback muộn sau khi người dùng bắt đầu lượt mới hoặc rời Game Over.

## Xác minh

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --no-daemon clean testDebugUnitTest assembleDebug lintDebug assembleRelease
```

Kết quả: 151 unit tests, 0 failures, 0 errors; Android Lint có 0 error; tạo thành công APK debug và release đã ký.

Các test trực tiếp:

- `GameResultsCoordinatorTest.saveAndReportNewHighScoreReportsOnlyAStrictPersonalRecord`
- `NewRecordUiControllerTest.newHighScoreShowsOnlyForTheCurrentGameOver`

Kiểm tra thủ công Pixel 7 vẫn chờ vì ADB chưa nhận emulator trong phiên này.
