-- Migration giao/ket ca (P1) — Tao bang ca_lam_viec, lich_su_ca; them cot hoa_don.id_ca_lam_viec
SET QUOTED_IDENTIFIER ON;

-- 1. Bang ca_lam_viec
IF OBJECT_ID('dbo.ca_lam_viec', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ca_lam_viec (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ma_ca VARCHAR(30) NOT NULL UNIQUE,
        id_nhan_vien BIGINT NOT NULL REFERENCES dbo.nhan_vien(id),
        ten_quy NVARCHAR(50) NOT NULL DEFAULT N'Quầy tổng',
        thoi_gian_mo DATETIME2 NOT NULL,
        thoi_gian_ket DATETIME2 NULL,
        tien_dau_ca DECIMAL(18,2) NOT NULL CHECK (tien_dau_ca >= 0),
        trang_thai VARCHAR(20) NOT NULL,   -- DANG_MO | DA_KET_CA
        tong_hoa_don INT NULL,
        tong_doanh_thu DECIMAL(18,2) NULL,
        tien_mat_thu DECIMAL(18,2) NULL,
        tien_khong_mat DECIMAL(18,2) NULL,
        tien_hoan_mat DECIMAL(18,2) NULL,
        tien_du_kien DECIMAL(18,2) NULL,
        tien_thuc_te DECIMAL(18,2) NULL,
        chenh_lech DECIMAL(18,2) NULL,
        ly_do_lech NVARCHAR(255) NULL,
        ghi_chu_mo NVARCHAR(255) NULL,
        da_xoa BIT NOT NULL DEFAULT 0,
        ngay_tao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        ngay_cap_nhat DATETIME2 NULL,
        nguoi_cap_nhat NVARCHAR(100) NULL
    );
END;

-- 2. Them cot hoa_don.id_ca_lam_viec
IF COL_LENGTH('dbo.hoa_don', 'id_ca_lam_viec') IS NULL
    ALTER TABLE dbo.hoa_don ADD id_ca_lam_viec BIGINT NULL REFERENCES dbo.ca_lam_viec(id);

-- 3. Bang lich_su_ca
IF OBJECT_ID('dbo.lich_su_ca', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.lich_su_ca (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        id_ca_lam_viec BIGINT NOT NULL REFERENCES dbo.ca_lam_viec(id),
        hanh_dong VARCHAR(30) NOT NULL,   -- MO_CA | KET_CA | BAN_GIAO
        id_nhan_vien BIGINT NOT NULL REFERENCES dbo.nhan_vien(id),
        thoi_gian DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        truoc_sau NVARCHAR(MAX) NULL,
        ghi_chu NVARCHAR(255) NULL
    );
END;

-- 4. Filtered unique index (chong race: chi mot ca DANG_MO / nhan vien, va mot ca DANG_MO he thong)
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_ca_dang_mo_nv' AND object_id = OBJECT_ID('dbo.ca_lam_viec'))
    CREATE UNIQUE INDEX uq_ca_dang_mo_nv ON dbo.ca_lam_viec(id_nhan_vien) WHERE trang_thai = 'DANG_MO' AND da_xoa = 0;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'uq_ca_dang_mo_he_thong' AND object_id = OBJECT_ID('dbo.ca_lam_viec'))
    CREATE UNIQUE INDEX uq_ca_dang_mo_he_thong ON dbo.ca_lam_viec(trang_thai) WHERE trang_thai = 'DANG_MO' AND da_xoa = 0;
