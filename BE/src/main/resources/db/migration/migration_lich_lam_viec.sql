-- Migration lich lam viec (P2) — Tao bang ca_lam, lich_lam_viec; seed 3 ca mac dinh
SET QUOTED_IDENTIFIER ON;
-- NOTE: chay bang sqlcmd -f 65001 (file UTF-8, chu Viet co dau)

-- 1. Bang ca_lam
IF OBJECT_ID('dbo.ca_lam', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ca_lam (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ma_ca NVARCHAR(50) NOT NULL UNIQUE,
        ten_ca NVARCHAR(100) NOT NULL,
        bat_dau TIME NOT NULL,
        ket_thuc TIME NOT NULL,
        dang_bat BIT NOT NULL DEFAULT 1,
        ngay_tao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        ngay_cap_nhat DATETIME2 NULL,
        nguoi_tao NVARCHAR(100) NULL,
        nguoi_cap_nhat NVARCHAR(100) NULL
    );
END;

-- 2. Bang lich_lam_viec
IF OBJECT_ID('dbo.lich_lam_viec', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.lich_lam_viec (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        id_nhan_vien BIGINT NOT NULL REFERENCES dbo.nhan_vien(id),
        id_ca_lam BIGINT NOT NULL REFERENCES dbo.ca_lam(id),
        ngay_lam DATE NOT NULL,
        ngay_tao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        ngay_cap_nhat DATETIME2 NULL,
        nguoi_tao NVARCHAR(100) NULL,
        nguoi_cap_nhat NVARCHAR(100) NULL,
        CONSTRAINT UQ_lich UNIQUE(id_nhan_vien, id_ca_lam, ngay_lam)
    );
END;

-- 3. Seed 3 ca mac dinh
IF NOT EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca = N'sang')
    INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat) VALUES (N'sang', N'Ca sáng', '08:00', '12:00', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca = N'chieu')
    INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat) VALUES (N'chieu', N'Ca chiều', '13:00', '17:00', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca = N'toi')
    INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat) VALUES (N'toi', N'Ca tối', '17:30', '21:30', 1);
