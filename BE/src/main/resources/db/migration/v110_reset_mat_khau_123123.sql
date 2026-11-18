-- v110_reset_mat_khau_123123.sql
-- Đổi toàn bộ mật khẩu tài khoản (nhân viên và khách hàng) về 123123 (mã hóa chuẩn BCrypt)
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

-- Hash BCrypt cho mật khẩu '123123'
UPDATE nhan_vien
SET mat_khau = '$2a$10$7d2th3QNp3oihZ0hCPvG5uBsr219OiI.Y8ArdSly38uPvV9dp84Bu';

UPDATE khach_hang
SET mat_khau = '$2a$10$7d2th3QNp3oihZ0hCPvG5uBsr219OiI.Y8ArdSly38uPvV9dp84Bu';
