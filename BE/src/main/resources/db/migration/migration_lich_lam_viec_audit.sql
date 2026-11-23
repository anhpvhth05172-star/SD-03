-- Fix schema drift: entity LichLamViec map 4 cot audit nhung DB that chua co.
-- (migration_lich_lam_viec.sql co nhung da bi mark chay truoc khi nhung cot nay duoc them.)
-- Idempotent: chi them cot thieu, khong DROP/DELETE/SUA du lieu cu.
SET QUOTED_IDENTIFIER ON;

IF COL_LENGTH('dbo.lich_lam_viec', 'ngay_tao') IS NULL
    ALTER TABLE dbo.lich_lam_viec ADD ngay_tao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME();

IF COL_LENGTH('dbo.lich_lam_viec', 'ngay_cap_nhat') IS NULL
    ALTER TABLE dbo.lich_lam_viec ADD ngay_cap_nhat DATETIME2 NULL;

IF COL_LENGTH('dbo.lich_lam_viec', 'nguoi_tao') IS NULL
    ALTER TABLE dbo.lich_lam_viec ADD nguoi_tao NVARCHAR(100) NULL;

IF COL_LENGTH('dbo.lich_lam_viec', 'nguoi_cap_nhat') IS NULL
    ALTER TABLE dbo.lich_lam_viec ADD nguoi_cap_nhat NVARCHAR(100) NULL;
