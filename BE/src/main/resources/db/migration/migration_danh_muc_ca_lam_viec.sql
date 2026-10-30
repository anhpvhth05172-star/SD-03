-- Tao bang danh muc ca lam viec (entity DanhMucCaLamViec + FK tu ca_lam_viec).
-- File nay PHAI co ten sort TRUOC migration_giao_ca_lich_lam.sql de chay truoc khi FK can.
IF OBJECT_ID('dbo.danh_muc_ca_lam_viec', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.danh_muc_ca_lam_viec (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        ma_ca NVARCHAR(50) NOT NULL CONSTRAINT uq_danh_muc_ca_ma UNIQUE,
        ten_ca NVARCHAR(100) NOT NULL CONSTRAINT uq_danh_muc_ca_ten UNIQUE,
        gio_bat_dau TIME NOT NULL,
        gio_ket_thuc TIME NOT NULL,
        dang_bat BIT NOT NULL DEFAULT 1
    );
    PRINT N'Da tao bang danh_muc_ca_lam_viec';
END
ELSE
BEGIN
    PRINT N'Bang danh_muc_ca_lam_viec da ton tai, bo qua';
END;
