/*
 * Phase 2 migration - PolyShoesDB
 * 1) phieu_giam_gia.gioi_han_moi_tai_khoan   (gioi han su dung theo tung tai khoan)
 * 2) lich_su_su_dung_phieu_giam_gia           (lich su su dung theo tai khoan)
 * 3) phieu_giam_gia.id_dot_giam_gia           (lien ket phieu <-> dot)
 * 4) hoa_don.id_dot_giam_gia / tien_giam / ten_giam_gia_ung_dung (snapshot)
 * 5) UPDATE ten phieu theo gia tri thuc te    (Thuc thi rieng bang PowerShell, xem bao cao)
 *
 * ddl-auto=none -> chay script thu cong. Khong DROP, khong DELETE.
 * Chay: sqlcmd -S localhost -U sa -P jacksonks@0104 -d PolyShoesDB -i migration_phase2.sql
 */
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

/* ===== 1) Gioi han moi tai khoan ===== */
IF COL_LENGTH('dbo.phieu_giam_gia', 'gioi_han_moi_tai_khoan') IS NULL
BEGIN
    ALTER TABLE dbo.phieu_giam_gia
        ADD gioi_han_moi_tai_khoan INT NOT NULL
        CONSTRAINT DF_phieu_giam_gia_gioi_han DEFAULT 1;
END;

/* ===== 2) Bang lich su su dung phieu ===== */
IF OBJECT_ID('dbo.lich_su_su_dung_phieu_giam_gia', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.lich_su_su_dung_phieu_giam_gia (
        id BIGINT IDENTITY(1, 1) NOT NULL PRIMARY KEY,
        id_khach_hang BIGINT NOT NULL,
        id_phieu_giam_gia BIGINT NOT NULL,
        id_hoa_don BIGINT NULL,
        so_tien_giam DECIMAL(18, 2) NULL,
        thoi_gian DATETIME2 NOT NULL
            CONSTRAINT DF_lich_su_su_dung_phieu_giam_gia_thoi_gian DEFAULT SYSDATETIME(),
        CONSTRAINT FK_lssd_khach_hang
            FOREIGN KEY (id_khach_hang) REFERENCES dbo.khach_hang (id),
        CONSTRAINT FK_lssd_phieu_giam_gia
            FOREIGN KEY (id_phieu_giam_gia) REFERENCES dbo.phieu_giam_gia (id),
        CONSTRAINT FK_lssd_hoa_don
            FOREIGN KEY (id_hoa_don) REFERENCES dbo.hoa_don (id),
        CONSTRAINT UQ_lssd_khach_hang_hoa_don_phieu
            UNIQUE (id_khach_hang, id_hoa_don, id_phieu_giam_gia)
    );
END;

/* ===== 3) Phieu -> Dot ===== */
IF COL_LENGTH('dbo.phieu_giam_gia', 'id_dot_giam_gia') IS NULL
BEGIN
    ALTER TABLE dbo.phieu_giam_gia ADD id_dot_giam_gia BIGINT NULL;
    ALTER TABLE dbo.phieu_giam_gia
        ADD CONSTRAINT FK_pgg_dot_giam_gia
        FOREIGN KEY (id_dot_giam_gia) REFERENCES dbo.dot_giam_gia (id);
END;

/* ===== 4) Hoa don them cot snapshot ===== */
IF COL_LENGTH('dbo.hoa_don', 'id_dot_giam_gia') IS NULL
BEGIN
    ALTER TABLE dbo.hoa_don ADD id_dot_giam_gia BIGINT NULL;
    ALTER TABLE dbo.hoa_don
        ADD CONSTRAINT FK_hoa_don_dot_giam_gia
        FOREIGN KEY (id_dot_giam_gia) REFERENCES dbo.dot_giam_gia (id);
END;

IF COL_LENGTH('dbo.hoa_don', 'tien_giam') IS NULL
    ALTER TABLE dbo.hoa_don ADD tien_giam DECIMAL(18, 2) NULL;

IF COL_LENGTH('dbo.hoa_don', 'ten_giam_gia_ung_dung') IS NULL
    ALTER TABLE dbo.hoa_don ADD ten_giam_gia_ung_dung NVARCHAR(200) NULL;

COMMIT TRANSACTION;

/* ===== Kiem tra sau migration ===== */
SELECT
    COL_LENGTH('dbo.phieu_giam_gia', 'gioi_han_moi_tai_khoan') AS pgg_gioi_han,
    COL_LENGTH('dbo.phieu_giam_gia', 'id_dot_giam_gia') AS pgg_id_dot,
    COL_LENGTH('dbo.hoa_don', 'id_dot_giam_gia') AS hd_id_dot,
    COL_LENGTH('dbo.hoa_don', 'tien_giam') AS hd_tien_giam,
    COL_LENGTH('dbo.hoa_don', 'ten_giam_gia_ung_dung') AS hd_ten_ung_dung,
    CASE WHEN OBJECT_ID('dbo.lich_su_su_dung_phieu_giam_gia', 'U') IS NULL THEN 0 ELSE 1 END AS bang_lssd;
