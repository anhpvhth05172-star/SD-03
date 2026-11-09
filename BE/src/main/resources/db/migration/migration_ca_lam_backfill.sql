-- Phuong an A: dua DB theo code (commit f33ecab - duc) cho nhom ca lam.
-- Chay TRUOC migration_ca_lam_viec_id_ca_lam.sql (sort A-Z: "ca_lam_backfill" < "ca_lam_viec_id_ca_lam").
-- 1. Tao dbo.ca_lam neu chua co (theo entity CaLam).
-- 2. Seed ca_lam tu danh_muc_ca_lam_viec map theo ma_ca (idempotent, khong xoa/doi du lieu cu).
-- 3. Them lich_lam_viec.id_ca_lam + ca_lam_viec.id_ca_lam (FK -> ca_lam), backfill tu id_ca/id_ca_danh_muc.
-- 4. Giu nguyen cot id_ca / id_ca_danh_muc cu (khong DROP, khong DELETE).
-- Sau khi file nay chay, migration_ca_lam_viec_id_ca_lam.sql tro thanh no-op (cot da ton tai).
SET QUOTED_IDENTIFIER ON;
-- NOTE: chay bang sqlcmd -f 65001 (file UTF-8)

-- 1. Bang ca_lam
IF OBJECT_ID('dbo.ca_lam', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.ca_lam (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ma_ca NVARCHAR(50) NOT NULL CONSTRAINT uq_ca_lam_ma UNIQUE,
        ten_ca NVARCHAR(100) NOT NULL,
        bat_dau TIME NOT NULL,
        ket_thuc TIME NOT NULL,
        dang_bat BIT NOT NULL DEFAULT 1,
        ngay_tao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
        ngay_cap_nhat DATETIME2 NULL,
        nguoi_tao NVARCHAR(100) NULL,
        nguoi_cap_nhat NVARCHAR(100) NULL
    );
    PRINT N'Da tao bang ca_lam';
END
ELSE
    PRINT N'Bang ca_lam da ton tai, bo qua tao bang';

-- 2. Seed ca_lam tu danh_muc_ca_lam_viec (chi them ma_ca chua co)
INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat)
SELECT d.ma_ca, d.ten_ca, d.gio_bat_dau, d.gio_ket_thuc, 1
FROM dbo.danh_muc_ca_lam_viec d
WHERE NOT EXISTS (SELECT 1 FROM dbo.ca_lam c WHERE c.ma_ca = d.ma_ca);

IF EXISTS (SELECT 1 FROM dbo.danh_muc_ca_lam_viec d
           WHERE NOT EXISTS (SELECT 1 FROM dbo.ca_lam c WHERE c.ma_ca = d.ma_ca))
    PRINT N'Loi: con ma_ca trong danh_muc chua co trong ca_lam';
ELSE
    PRINT N'Da seed ca_lam theo danh_muc_ca_lam_viec';

-- 3a. lich_lam_viec.id_ca_lam (id_ca cu giu nguyen)
IF COL_LENGTH('dbo.lich_lam_viec', 'id_ca_lam') IS NULL
BEGIN
    ALTER TABLE dbo.lich_lam_viec ADD id_ca_lam BIGINT NULL;
    ALTER TABLE dbo.lich_lam_viec
        ADD CONSTRAINT fk_lich_lam_viec_ca_lam
        FOREIGN KEY (id_ca_lam) REFERENCES dbo.ca_lam(id);
    PRINT N'Da them lich_lam_viec.id_ca_lam';
END;

IF COL_LENGTH('dbo.lich_lam_viec', 'id_ca') IS NOT NULL
BEGIN
    EXEC('UPDATE l
    SET id_ca_lam = c.id
    FROM dbo.lich_lam_viec l
    JOIN dbo.danh_muc_ca_lam_viec d ON d.id = l.id_ca
    JOIN dbo.ca_lam c ON c.ma_ca = d.ma_ca
    WHERE l.id_ca_lam IS NULL');
END;

-- 3b. ca_lam_viec.id_ca_lam (id_ca_danh_muc cu giu nguyen)
IF COL_LENGTH('dbo.ca_lam_viec', 'id_ca_lam') IS NULL
BEGIN
    ALTER TABLE dbo.ca_lam_viec ADD id_ca_lam BIGINT NULL;
    ALTER TABLE dbo.ca_lam_viec
        ADD CONSTRAINT fk_ca_lam_viec_ca_lam
        FOREIGN KEY (id_ca_lam) REFERENCES dbo.ca_lam(id);
    PRINT N'Da them ca_lam_viec.id_ca_lam';
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'ix_ca_lam_viec_ca_lam'
      AND object_id = OBJECT_ID('dbo.ca_lam_viec')
)
    CREATE INDEX ix_ca_lam_viec_ca_lam ON dbo.ca_lam_viec(id_ca_lam);

IF COL_LENGTH('dbo.ca_lam_viec', 'id_ca_danh_muc') IS NOT NULL
BEGIN
    EXEC('UPDATE clv
    SET id_ca_lam = c.id
    FROM dbo.ca_lam_viec clv
    JOIN dbo.danh_muc_ca_lam_viec d ON d.id = clv.id_ca_danh_muc
    JOIN dbo.ca_lam c ON c.ma_ca = d.ma_ca
    WHERE clv.id_ca_lam IS NULL
      AND clv.id_ca_danh_muc IS NOT NULL');
END;
