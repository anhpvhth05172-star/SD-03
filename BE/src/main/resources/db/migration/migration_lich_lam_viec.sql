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
END
ELSE
BEGIN
    -- Neu bang da ton tai tu truoc nhung cot id_ca chua doi ten
    IF COL_LENGTH('dbo.lich_lam_viec', 'id_ca_lam') IS NULL AND COL_LENGTH('dbo.lich_lam_viec', 'id_ca') IS NOT NULL
    BEGIN
        DECLARE @fkName NVARCHAR(256);
        SELECT TOP 1 @fkName = fk.name FROM sys.foreign_keys fk
        INNER JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
        WHERE fkc.parent_object_id = OBJECT_ID('dbo.lich_lam_viec')
          AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_ca';
        IF @fkName IS NOT NULL
            EXEC('ALTER TABLE dbo.lich_lam_viec DROP CONSTRAINT ' + @fkName);

        EXEC sp_rename 'dbo.lich_lam_viec.id_ca', 'id_ca_lam', 'COLUMN';
        ALTER TABLE dbo.lich_lam_viec ADD CONSTRAINT FK_lich_ca FOREIGN KEY (id_ca_lam) REFERENCES dbo.ca_lam(id);
    END;

    IF COL_LENGTH('dbo.lich_lam_viec', 'ngay_tao') IS NULL
        ALTER TABLE dbo.lich_lam_viec ADD ngay_tao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME();
    IF COL_LENGTH('dbo.lich_lam_viec', 'ngay_cap_nhat') IS NULL
        ALTER TABLE dbo.lich_lam_viec ADD ngay_cap_nhat DATETIME2 NULL;
    IF COL_LENGTH('dbo.lich_lam_viec', 'nguoi_tao') IS NULL
        ALTER TABLE dbo.lich_lam_viec ADD nguoi_tao NVARCHAR(100) NULL;
    IF COL_LENGTH('dbo.lich_lam_viec', 'nguoi_cap_nhat') IS NULL
        ALTER TABLE dbo.lich_lam_viec ADD nguoi_cap_nhat NVARCHAR(100) NULL;
END;

-- 3. Seed cac ca mac dinh
IF NOT EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca = N'sang')
    INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat) VALUES (N'sang', N'Ca sáng', '08:00', '15:00', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca = N'chieu')
    INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat) VALUES (N'chieu', N'Ca chiều', '15:00', '22:00', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca = N'ca-em')
    INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat) VALUES (N'ca-em', N'Ca đêm', '22:00', '23:59', 1);
