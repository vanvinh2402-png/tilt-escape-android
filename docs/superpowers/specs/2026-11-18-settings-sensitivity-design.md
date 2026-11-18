# Thiết kế Settings và Tilt Sensitivity

Ngày: 2026-11-18

## Mục tiêu

Hoàn thành Frame 8 / R60–R61: người chơi có màn Cài đặt riêng để chọn độ nhạy điều khiển nghiêng và lựa chọn được lưu, áp dụng ngay cho ván chơi.

## Phạm vi

- Thêm nút `Cài đặt` vào Main Menu và overlay Settings có thể cuộn.
- Cung cấp ba lựa chọn: Thấp, Trung bình và Cao; Trung bình là mặc định.
- Lưu lựa chọn trong `SharedPreferences`, độc lập với phiên đăng nhập và SQLite.
- Áp dụng hệ số ngay vào `TiltSignalProcessor`, không cần khởi động lại ứng dụng hoặc tạo lại ván.
- Quay lại menu bằng nút hoặc phím Back.

## Không nằm trong phạm vi

- Không có thanh kéo tự do, calibration, profile theo tài khoản, cloud sync hoặc thay đổi schema SQLite.
- Không thay đổi điểm, va chạm, difficulty, vòng đời sensor hay gameplay khác.

## Các hướng đã cân nhắc

1. **Ba mức enum + SharedPreferences riêng — đã chọn:** dễ hiểu với người chơi, giá trị ổn định để kiểm thử, không cần migration DB.
2. Thanh kéo 0–100: linh hoạt hơn nhưng khó giải thích, khó kiểm thử và không được SRS yêu cầu.
3. Lưu độ nhạy trong bảng account SQLite: gắn theo tài khoản nhưng tăng migration/schema trong khi yêu cầu chỉ cần lưu local.

## Hành vi và giao diện

- `LOW = 0.75`, `MEDIUM = 1.00`, `HIGH = 1.25`; mặc định là `MEDIUM` nếu preference thiếu, sai kiểu hoặc giá trị lạ.
- Hệ số được nhân với tilt đã lọc trước dead-zone/scaling; lọc và clamp hiện có vẫn giữ nguyên.
- Mở Settings chỉ khi game là `READY` và Main Menu được phép hiện. Khi mở, menu ẩn nhưng engine giữ `READY`.
- Chạm một lựa chọn lưu và áp dụng ngay; lựa chọn được phản chiếu khi mở lại màn.
- Máy thiếu accelerometer ẩn action Settings, tương tự action Hướng dẫn chơi, để tránh màn cài đặt không có tác dụng.
- Overlay dùng style Neon Focus, `ScrollView`, radio button và nút Quay lại có vùng chạm tối thiểu hiện có.

## Kiểm chứng

- Unit test cho enum/store với default, persistence và dữ liệu hỏng.
- Unit test cho `TiltSignalProcessor` chứng minh hệ số làm đáp ứng thấp/cao tương ứng mà không vượt [-1, 1].
- Robolectric test cho mở/đóng Settings, restore radio, áp dụng callback và gate menu.
- Build sạch, lint, release APK; Pixel 7 manual smoke nếu thiết bị ADB sẵn sàng.
