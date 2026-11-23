-- v108_chuan_hoa_ma_toan_bo_project.sql
-- Chuẩn hóa mã hóa đơn, mã khách hàng, mã phiếu giảm giá, mã đợt giảm giá về quy chuẩn thống nhất toàn bộ project

SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

BEGIN TRANSACTION;

-- 1. Chuẩn hóa mã hóa đơn theo thứ tự ngày phát sinh (HD000001 -> HD000233)
WITH CTE_HD AS (
    SELECT id, ma_hoa_don, ROW_NUMBER() OVER (ORDER BY ngay_tao ASC, id ASC) AS rn
    FROM hoa_don
)
UPDATE CTE_HD
SET ma_hoa_don = 'HD' + RIGHT('000000' + CAST(rn AS VARCHAR(10)), 6);

-- 2. Chuẩn hóa mã khách hàng theo ID (trừ tài khoản quản trị hệ thống)
WITH CTE_KH AS (
    SELECT id, ma_khach_hang, ROW_NUMBER() OVER (ORDER BY id ASC) AS rn
    FROM khach_hang
    WHERE ten_tai_khoan != 'admin' 
      AND (vai_tro IS NULL OR vai_tro != 'ADMIN') 
      AND ma_khach_hang NOT LIKE '%ADMIN%'
)
UPDATE CTE_KH
SET ma_khach_hang = 'KH' + RIGHT('000000' + CAST(rn AS VARCHAR(10)), 6);

-- 3. Chuẩn hóa toàn bộ mã phiếu giảm giá tuần tự (PGG001 -> PGG026)
WITH CTE_PGG AS (
    SELECT id, ma_phieu_giam_gia, ROW_NUMBER() OVER (ORDER BY id ASC) AS rn
    FROM phieu_giam_gia
)
UPDATE CTE_PGG
SET ma_phieu_giam_gia = 'PGG' + RIGHT('000' + CAST(rn AS VARCHAR(10)), 3);

-- 4. Chuẩn hóa toàn bộ mã đợt giảm giá tuần tự (DGG001 -> DGG025)
WITH CTE_DGG AS (
    SELECT id, ma_dot_giam_gia, ROW_NUMBER() OVER (ORDER BY id ASC) AS rn
    FROM dot_giam_gia
)
UPDATE CTE_DGG
SET ma_dot_giam_gia = 'DGG' + RIGHT('000' + CAST(rn AS VARCHAR(10)), 3);

COMMIT TRANSACTION;
