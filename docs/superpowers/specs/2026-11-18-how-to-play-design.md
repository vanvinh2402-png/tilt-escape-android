# Thiết kế màn Hướng dẫn chơi

Ngày: 2026-11-18

## Mục tiêu

Hoàn thành Frame 4 / R59 trong tài liệu yêu cầu: người chơi có một màn hướng dẫn riêng trước khi vào ván, giải thích cách điều khiển nghiêng, cách ghi điểm và điều kiện kết thúc.

## Phạm vi

- Thêm nút `Hướng dẫn chơi` trong menu chính khi trò chơi ở trạng thái sẵn sàng.
- Thêm một overlay có thể cuộn, đồng nhất với giao diện Neon Focus, gồm ba hướng dẫn ngắn: nghiêng để di chuyển, né chướng ngại vật để được một điểm, va chạm sẽ kết thúc ván.
- Thêm nút `Bắt đầu chơi` để vào ván ngay và nút `Quay lại` để về menu.
- Phím Back đóng hướng dẫn và về menu chính.

## Không nằm trong phạm vi

- Không thay đổi cảm biến, vật lý, sinh chướng ngại vật, tính điểm theo lần né, SQLite, đăng nhập hoặc Thành tích.
- Không thêm thư viện hay tài nguyên hình ảnh mới.

## Các hướng đã cân nhắc

1. **Overlay riêng với controller riêng — đã chọn:** giữ `GameplayUiController` chỉ chịu trách nhiệm vòng đời ván chơi; controller hướng dẫn chịu trách nhiệm mở/đóng/bắt đầu từ màn hướng dẫn. Dễ kiểm thử và không ảnh hưởng các luồng Pause/Game Over.
2. Gộp vào nội dung menu chính: ít mã hơn nhưng không đáp ứng yêu cầu màn Hướng dẫn chơi riêng và nội dung dễ bị bỏ qua.
3. Dùng hộp thoại Android: nhanh nhưng không đồng nhất với overlay Neon Focus và khó đọc trên màn hình nhỏ.

## Hành vi và giao diện

- Hướng dẫn chỉ mở khi `GameEngine` ở `READY` và menu chính đang được phép hiển thị.
- Khi mở, menu chính ẩn; overlay hướng dẫn hiện; game vẫn ở `READY`.
- `Bắt đầu chơi` đóng hướng dẫn rồi dùng đúng thao tác Start hiện có để bắt đầu game.
- `Quay lại` hoặc Back đóng hướng dẫn và hiện lại menu.
- Nội dung nằm trong `ScrollView` để vẫn truy cập được nút cuối trên màn hình thấp/phông chữ lớn.
- Tất cả nút dùng style Neon Focus hiện có và giữ kích thước chạm tối thiểu 48dp.

## Kiểm chứng

- Robolectric kiểm tra view/chuỗi tồn tại, trạng thái mở/đóng và hành vi bắt đầu.
- Chạy toàn bộ unit test, `assembleDebug`, `lintDebug` và cài APK release lên Pixel 7 để kiểm tra luồng thực tế.
