# Thiết kế giao diện Neon Focus

Ngày: 2026-11-06

## Mục tiêu

Làm giao diện Tilt Escape rõ ràng, hiện đại và có cảm giác game hơn, đồng thời sửa trực tiếp các vấn đề đang thấy trên Pixel 7: chữ gợi ý màu tối khó đọc, nút Android mặc định, form rời rạc và khoảng trống quá lớn.

## Phạm vi

- Áp dụng một hệ thống màu và component thống nhất cho Đăng nhập, Đăng ký, menu chính, Tạm dừng, Kết thúc và Thành tích.
- Giữ nguyên toàn bộ ID view, luồng tài khoản, SQLite và gameplay.
- Tối ưu cho màn hình dọc, bàn phím ảo và máy nhỏ bằng ScrollView cùng khoảng cách co giãn.
- Không thêm thư viện UI mới hoặc tài nguyên ảnh bên ngoài.

## Các hướng đã cân nhắc

1. **Neon Focus — đã chọn:** thẻ tối nổi trên nền game, tím–xanh làm hành động chính, xanh ngọc làm điểm nhấn. Cá tính nhưng vẫn dễ đọc và phù hợp bảng màu hiện tại.
2. **Midnight Minimal:** sạch và an toàn hơn nhưng ít cảm giác game.
3. **Arcade Pulse:** cá tính retro mạnh nhưng dễ gây rối và nhanh lỗi thời.

## Hệ thống hình ảnh

- Nền chính: `#090B16`; bề mặt: `#131831`; bề mặt nổi: `#191F3D`.
- Primary gradient: `#6D5DFC` đến `#3D9BFF`; accent: `#2AD9BA`.
- Chữ chính: `#F8FAFC`; chữ phụ/hint: `#AEB8DC`; lỗi: `#FF667A`.
- Card bo góc 24dp, viền tím mờ; input bo 14dp, cao tối thiểu 52dp; nút cao 52dp.
- Tất cả chữ và hint được khai báo màu rõ ràng, không phụ thuộc màu mặc định của Android.
- Nút chính dùng nền gradient; nút phụ dùng nền trong suốt có viền; trạng thái disabled có độ tương phản riêng.

## Bố cục và trạng thái

- HUD gọn ở phía trên, điểm số nổi bật và nút tạm dừng dạng pill.
- Form xác thực nằm trong card có chiều rộng tối đa hợp lý, căn giữa, vẫn cuộn được khi bàn phím mở.
- Tiêu đề có nhãn thương hiệu “TILT ESCAPE”, mô tả ngắn và khoảng cách đều giữa các trường.
- Menu, Pause và Game Over dùng cùng card/component; Thành tích dùng các hàng kết quả dạng card.
- Player và nền game vẫn hiện nhẹ phía sau overlay để giữ ngữ cảnh game, nhưng form luôn có độ tương phản cao.
- Loading, lỗi, trạng thái thiếu cảm biến và disabled phải giữ nguyên hành vi hiện tại, chỉ thay cách trình bày.

## Khả dụng và kiểm chứng

- Vùng chạm tối thiểu 48dp; hỗ trợ font hệ thống và nội dung tiếng Việt.
- Không đổi logic controller hoặc cấu trúc dữ liệu.
- Thêm/cập nhật kiểm thử tài nguyên cho ID, màu chữ và background quan trọng.
- Chạy toàn bộ unit test, `assembleDebug`, `lintDebug`; kiểm tra thủ công trên Pixel 7 ở màn Đăng nhập, Đăng ký, Menu, Pause, Game Over và Thành tích.

## Không nằm trong phạm vi

- Animation phức tạp, âm thanh, ảnh nền mới hoặc thay đổi sprite.
- Thay đổi cơ chế điều khiển, tính điểm, tài khoản hay cơ sở dữ liệu.

