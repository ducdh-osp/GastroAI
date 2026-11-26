# Dữ liệu vị trí IP

- Nguồn: DB-IP City Lite (https://db-ip.com), bản tháng 6/2026, lấy qua gói npm @ip-location-db/dbip-city-mmdb.
- Giấy phép: Creative Commons Attribution 4.0 (CC BY 4.0) — https://creativecommons.org/licenses/by/4.0/
- Đã thay đổi so với bản gốc: chỉ giữ quốc gia và tỉnh/bang (bỏ thành phố, toạ độ, mã bưu chính, múi giờ);
  gộp các dải IP liền nhau cùng nước và tỉnh; chỉ lưu IP bắt đầu của mỗi dải (IPv6 giữ 64 bit cao);
  chuyển sang định dạng nhị phân riêng và nén gzip.
- Định dạng file: xem IpLocationService.java.
