-- Script tao tai khoan Admin
-- Ten dang nhap: admin
-- Mat khau: 123456 (BCrypt hash)

-- 1. Tao / Cap nhat trong bang khach_hang (Dung cho he thong dang nhap JWT hien tai o /auth/dang-nhap)
IF EXISTS (SELECT 1 FROM khach_hang WHERE ten_tai_khoan = 'admin' OR email = 'admin@gmail.com')
BEGIN
    UPDATE khach_hang
    SET ten_tai_khoan = 'admin',
        ten_khach_hang = N'Quản trị viên',
        email = 'admin@gmail.com',
        mat_khau = '$2a$10$uPENVJVBYIJC6IjGzEOjBeHJ4u31sC6pHDhBRQnPVLEuxDah0o7Q6',
        so_dien_thoai = '0988888888',
        trang_thai = 1,
        vai_tro = 'ADMIN'
    WHERE ten_tai_khoan = 'admin' OR email = 'admin@gmail.com';
    PRINT N'Da cap nhat tai khoan admin trong khach_hang';
END
ELSE
BEGIN
    INSERT INTO khach_hang (ma_khach_hang, ten_tai_khoan, ten_khach_hang, email, mat_khau, so_dien_thoai, trang_thai, vai_tro, nguoi_tao)
    VALUES ('KH_ADMIN', 'admin', N'Quản trị viên', 'admin@gmail.com', '$2a$10$uPENVJVBYIJC6IjGzEOjBeHJ4u31sC6pHDhBRQnPVLEuxDah0o7Q6', '0988888888', 1, 'ADMIN', 'he-thong');
    PRINT N'Da tao moi tai khoan admin trong khach_hang';
END;

-- 2. Tao / Cap nhat trong bang nhan_vien (neu he thong can dong bo nhan vien)
DECLARE @idVaiTroAdmin BIGINT;
SELECT TOP 1 @idVaiTroAdmin = id FROM vai_tro WHERE ma_vai_tro = 'ADMIN';

IF @idVaiTroAdmin IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM nhan_vien WHERE ten_tai_khoan = 'admin' OR email = 'admin@gmail.com')
    BEGIN
        UPDATE nhan_vien
        SET mat_khau = '$2a$10$uPENVJVBYIJC6IjGzEOjBeHJ4u31sC6pHDhBRQnPVLEuxDah0o7Q6',
            id_vai_tro = @idVaiTroAdmin,
            trang_thai = 1
        WHERE ten_tai_khoan = 'admin' OR email = 'admin@gmail.com';
        PRINT N'Da cap nhat tai khoan admin trong nhan_vien';
    END
    ELSE
    BEGIN
        INSERT INTO nhan_vien (id_vai_tro, ma_nhan_vien, ten_tai_khoan, mat_khau, email, so_dien_thoai, trang_thai)
        VALUES (@idVaiTroAdmin, 'NV_ADMIN', 'admin', '$2a$10$uPENVJVBYIJC6IjGzEOjBeHJ4u31sC6pHDhBRQnPVLEuxDah0o7Q6', 'admin@gmail.com', '0988888888', 1);
        PRINT N'Da tao moi tai khoan admin trong nhan_vien';
    END
END;
