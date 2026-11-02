-- Kiem tra va them cot cho bang phien_dang_nhap
IF OBJECT_ID('dbo.phien_dang_nhap', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.phien_dang_nhap (
        id BIGINT IDENTITY(1, 1) NOT NULL PRIMARY KEY,
        id_khach_hang BIGINT NULL,
        id_nhan_vien BIGINT NULL,
        refresh_token_hash VARCHAR(64) NOT NULL,
        ngay_tao DATETIME2 NOT NULL,
        hoat_dong_cuoi DATETIME2 NOT NULL,
        revoked BIT NOT NULL CONSTRAINT DF_phien_dang_nhap_revoked DEFAULT 0,
        ngay_dang_xuat DATETIME2 NULL
    );
END
ELSE
BEGIN
    IF COL_LENGTH('dbo.phien_dang_nhap', 'ngay_tao') IS NULL
        ALTER TABLE dbo.phien_dang_nhap ADD ngay_tao DATETIME2 NULL;

    IF COL_LENGTH('dbo.phien_dang_nhap', 'hoat_dong_cuoi') IS NULL
        ALTER TABLE dbo.phien_dang_nhap ADD hoat_dong_cuoi DATETIME2 NULL;

    IF COL_LENGTH('dbo.phien_dang_nhap', 'id_nhan_vien') IS NULL
        ALTER TABLE dbo.phien_dang_nhap ADD id_nhan_vien BIGINT NULL;

    IF COL_LENGTH('dbo.phien_dang_nhap', 'id_khach_hang') IS NOT NULL
        ALTER TABLE dbo.phien_dang_nhap ALTER COLUMN id_khach_hang BIGINT NULL;

    IF COL_LENGTH('dbo.phien_dang_nhap', 'revoked') IS NULL
        ALTER TABLE dbo.phien_dang_nhap ADD revoked BIT NOT NULL CONSTRAINT DF_phien_dang_nhap_revoked_fix DEFAULT 0;

    IF COL_LENGTH('dbo.phien_dang_nhap', 'ngay_dang_xuat') IS NULL
        ALTER TABLE dbo.phien_dang_nhap ADD ngay_dang_xuat DATETIME2 NULL;
END;
