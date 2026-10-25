/*
 * Lưu danh mục ca làm và lịch làm việc lên SQL Server.
 * Chạy một lần trên PolyShoesDB; chỉ tạo thêm bảng, không xóa hoặc sửa dữ liệu hiện có.
 */
IF OBJECT_ID('dbo.danh_muc_ca_lam_viec', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.danh_muc_ca_lam_viec (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        ma_ca NVARCHAR(50) NOT NULL,
        ten_ca NVARCHAR(100) NOT NULL,
        gio_bat_dau TIME(0) NOT NULL,
        gio_ket_thuc TIME(0) NOT NULL,
        dang_bat BIT NOT NULL CONSTRAINT df_danh_muc_ca_dang_bat DEFAULT 1,
        CONSTRAINT uq_danh_muc_ca_ma UNIQUE (ma_ca),
        CONSTRAINT uq_danh_muc_ca_ten UNIQUE (ten_ca),
        CONSTRAINT ck_danh_muc_ca_gio CHECK (gio_ket_thuc > gio_bat_dau)
    );
END;

IF OBJECT_ID('dbo.lich_lam_viec', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.lich_lam_viec (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        id_nhan_vien BIGINT NOT NULL,
        id_ca BIGINT NOT NULL,
        ngay_lam DATE NOT NULL,
        CONSTRAINT fk_lich_lam_viec_nhan_vien FOREIGN KEY (id_nhan_vien) REFERENCES dbo.nhan_vien(id),
        CONSTRAINT fk_lich_lam_viec_ca FOREIGN KEY (id_ca) REFERENCES dbo.danh_muc_ca_lam_viec(id),
        CONSTRAINT uq_lich_lam_viec_nhan_vien_ca_ngay UNIQUE (id_nhan_vien, id_ca, ngay_lam)
    );
    CREATE INDEX ix_lich_lam_viec_ngay ON dbo.lich_lam_viec(ngay_lam);
END;
