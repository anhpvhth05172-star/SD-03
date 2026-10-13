/*
 * Migration: bang phien_dang_nhap (Access + Refresh Token / session 30 phut inactivity)
 * PolyShoesDB
 *
 * - Luu SHA-256 hash cua refresh token (khong luu token thoi).
 * - hoat_dong_cuoi = lan hoat dong hop le cuoi cung; phien het han khi
 *   hoat_dong_cuoi + app.auth.session-inactivity-ms (30 phut) < bay gio.
 * - revoked + ngay_dang_xuat cho phep logout/revoke.
 *
 * ddl-auto=none -> chay script thu cong. Khong DROP, khong DELETE du lieu nghiep vu.
 * Chay: sqlcmd -S localhost -U sa -P jacksonks@0104 -d PolyShoesDB -i migration_phien_dang_nhap.sql
 */
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

IF OBJECT_ID('dbo.phien_dang_nhap', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.phien_dang_nhap (
        id BIGINT IDENTITY(1, 1) NOT NULL PRIMARY KEY,
        id_khach_hang BIGINT NOT NULL,
        refresh_token_hash VARCHAR(64) NOT NULL,
        ngay_tao DATETIME2 NOT NULL,
        hoat_dong_cuoi DATETIME2 NOT NULL,
        revoked BIT NOT NULL CONSTRAINT DF_phien_dang_nhap_revoked DEFAULT 0,
        ngay_dang_xuat DATETIME2 NULL,
        CONSTRAINT FK_phien_dang_nhap_khach_hang
            FOREIGN KEY (id_khach_hang) REFERENCES dbo.khach_hang (id)
    );

    CREATE UNIQUE INDEX UX_phien_dang_nhap_refresh_token_hash
        ON dbo.phien_dang_nhap (refresh_token_hash);

    CREATE INDEX IDX_phien_dang_nhap_khach_hang
        ON dbo.phien_dang_nhap (id_khach_hang);
END;

COMMIT TRANSACTION;
