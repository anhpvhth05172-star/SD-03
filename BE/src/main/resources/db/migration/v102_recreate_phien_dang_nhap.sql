-- Lam moi hoan toan bang phien_dang_nhap theo dung chuan Entity Hibernate
IF OBJECT_ID('dbo.phien_dang_nhap', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.phien_dang_nhap;
END;

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

CREATE UNIQUE INDEX UX_phien_dang_nhap_refresh_token_hash
    ON dbo.phien_dang_nhap (refresh_token_hash);

CREATE INDEX IDX_phien_dang_nhap_khach_hang
    ON dbo.phien_dang_nhap (id_khach_hang);

CREATE INDEX IDX_phien_dang_nhap_nhan_vien
    ON dbo.phien_dang_nhap (id_nhan_vien);
