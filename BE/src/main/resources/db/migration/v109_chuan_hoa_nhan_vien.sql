-- Chuẩn hóa mã nhân viên, họ tên và dữ liệu mẫu cho bảng nhan_vien
-- Quy tắc: Tên + viết hoa chữ cái đầu họ đệm (VD: Phạm Vũ Hoàng Anh -> AnhPVH).
-- Trùng mã: thêm số thứ tự trực tiếp đằng sau (VD: AnhPVH2, AnhPVH3).
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- Tránh đụng độ UNIQUE constraint tạm thời giữa các mã cũ
UPDATE nhan_vien SET ma_nhan_vien = 'TMP_' + CAST(id AS VARCHAR(20));

UPDATE nhan_vien
SET ma_nhan_vien = 'Admin',
    ten_tai_khoan = 'admin',
    email = 'admin@polyshoes.vn',
    so_dien_thoai = '0912345678',
    gioi_tinh = N'NAM',
    que_quan = N'Thành phố Hà Nội',
    phuong = N'Phường Văn Miếu - Quốc Tử Giám',
    dia_chi_cu_the = N'123 Tôn Đức Thắng',
    ngay_sinh = '1995-01-01'
WHERE id = 2;

UPDATE nhan_vien
SET ma_nhan_vien = 'AnhPVH',
    ten_tai_khoan = N'Phạm Vũ Hoàng Anh',
    email = 'anhpvhth05172@gmail.com',
    so_dien_thoai = '0987654321',
    gioi_tinh = N'NAM',
    que_quan = N'Thành phố Hà Nội',
    phuong = N'Phường Văn Miếu - Quốc Tử Giám',
    dia_chi_cu_the = N'242 Xã Đàn',
    ngay_sinh = '2004-06-16'
WHERE id = 3;

UPDATE nhan_vien
SET ma_nhan_vien = 'DucLH',
    ten_tai_khoan = N'Lê Huỳnh Đức',
    email = 'duc@gmail.com',
    so_dien_thoai = '0987654322',
    gioi_tinh = N'NAM',
    que_quan = N'Thành phố Hà Nội',
    phuong = N'Phường Dịch Vọng Hậu',
    dia_chi_cu_the = N'123 Cầu Giấy',
    ngay_sinh = '2003-03-15'
WHERE id = 4;

UPDATE nhan_vien
SET ma_nhan_vien = 'AnhPVH2',
    ten_tai_khoan = N'Phạm Vũ Hoàng Anh',
    email = 'jacksonks2008@gmail.com',
    so_dien_thoai = '0922000001',
    gioi_tinh = N'NAM',
    que_quan = N'Thành phố Hà Nội',
    phuong = N'Phường Bách Khoa',
    dia_chi_cu_the = N'Số 1 Đại Cồ Việt',
    ngay_sinh = '2002-11-20'
WHERE id = 5;

UPDATE nhan_vien
SET ma_nhan_vien = 'ThaiTV',
    ten_tai_khoan = N'Trần Văn Thái',
    email = 'thaitv@gmail.com',
    so_dien_thoai = '0988888006',
    gioi_tinh = N'NAM',
    que_quan = N'Thành phố Hà Nội',
    phuong = N'Phường Láng Thượng',
    dia_chi_cu_the = N'91 Chùa Láng',
    ngay_sinh = '2003-08-10'
WHERE id = 6;

UPDATE nhan_vien
SET ma_nhan_vien = 'ThuanNV',
    ten_tai_khoan = N'Nguyễn Văn Thuận',
    email = 'thuannv@gmail.com',
    so_dien_thoai = '0922010408',
    gioi_tinh = N'NAM',
    que_quan = N'Tỉnh Nam Định',
    phuong = N'Phường Vị Xuyên',
    dia_chi_cu_the = N'45 Trần Phú',
    ngay_sinh = '2003-05-25'
WHERE id = 7;

UPDATE nhan_vien
SET ma_nhan_vien = 'QuanDM',
    ten_tai_khoan = N'Đỗ Minh Quân',
    email = 'quandm@gmail.com',
    so_dien_thoai = '0987654123',
    gioi_tinh = N'NAM',
    que_quan = N'Thành phố Hải Phòng',
    phuong = N'Phường Lê Chân',
    dia_chi_cu_the = N'18 Lạch Tray',
    ngay_sinh = '2001-09-12'
WHERE id = 8;

UPDATE nhan_vien
SET ma_nhan_vien = 'MaiVT',
    ten_tai_khoan = N'Vũ Thị Mai',
    email = 'maivt@gmail.com',
    so_dien_thoai = '0900000001',
    gioi_tinh = N'NU',
    que_quan = N'Thành phố Hà Nội',
    phuong = N'Phường Kim Mã',
    dia_chi_cu_the = N'68 Kim Mã',
    ngay_sinh = '2002-04-18'
WHERE id = 9;

UPDATE nhan_vien
SET ma_nhan_vien = 'TrangHT',
    ten_tai_khoan = N'Hoàng Thu Trang',
    email = 'tranght@gmail.com',
    so_dien_thoai = '0900000002',
    gioi_tinh = N'NU',
    que_quan = N'Thành phố Hà Nội',
    phuong = N'Phường Hàng Bông',
    dia_chi_cu_the = N'88 Hàng Gai',
    ngay_sinh = '2003-12-05'
WHERE id = 10;

UPDATE nhan_vien
SET ma_nhan_vien = 'AnhBD',
    ten_tai_khoan = N'Bùi Đức Anh',
    email = 'anhbd@gmail.com',
    so_dien_thoai = '0911111111',
    gioi_tinh = N'NAM',
    que_quan = N'Tỉnh Quảng Ninh',
    phuong = N'Phường Bãi Cháy',
    dia_chi_cu_the = N'10 Hạ Long',
    ngay_sinh = '2002-07-22'
WHERE id = 11;

UPDATE nhan_vien
SET ma_nhan_vien = 'AnhPVH3',
    ten_tai_khoan = N'Phạm Vũ Hoàng Anh',
    email = 'anhpvh@gmail.com',
    so_dien_thoai = '0986754321',
    gioi_tinh = N'NAM',
    que_quan = N'Thành phố Hà Nội',
    phuong = N'Phường Phương Liên',
    dia_chi_cu_the = N'15 Ô Chợ Dừa',
    ngay_sinh = '2004-06-16'
WHERE id = 12;

UPDATE nhan_vien
SET ma_nhan_vien = 'HuyNQ',
    ten_tai_khoan = N'Ngô Quang Huy',
    email = 'huynq@gmail.com',
    so_dien_thoai = '0987654312',
    gioi_tinh = N'NAM',
    que_quan = N'Tỉnh Bắc Ninh',
    phuong = N'Phường Ninh Xá',
    dia_chi_cu_the = N'50 Lý Thái Tổ',
    ngay_sinh = '2003-10-15'
WHERE id = 271;
