-- Chat nhan vien: bang hoi_thoai + tin_nhan (chi tao neu chua co)
IF OBJECT_ID('dbo.hoi_thoai', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.hoi_thoai (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        loai NVARCHAR(20) NOT NULL DEFAULT 'NHAN_VIEN',
        trang_thai NVARCHAR(20) NOT NULL DEFAULT 'DANG_MO',
        id_khach_hang BIGINT NULL,
        ma_an_danh NVARCHAR(64) NULL,
        id_nhan_vien_phu_trach BIGINT NULL,
        ten_khach NVARCHAR(100) NULL,
        tong_tin_khach INT NOT NULL DEFAULT 0,
        tong_tin_nhan_vien INT NOT NULL DEFAULT 0,
        ngay_tao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        ngay_cap_nhat DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_hoi_thoai_khach_hang FOREIGN KEY (id_khach_hang) REFERENCES dbo.khach_hang(id),
        CONSTRAINT FK_hoi_thoai_nhan_vien FOREIGN KEY (id_nhan_vien_phu_trach) REFERENCES dbo.nhan_vien(id)
    );
    CREATE INDEX IX_hoi_thoai_ma_an_danh ON dbo.hoi_thoai(ma_an_danh);
    CREATE INDEX IX_hoi_thoai_khach_hang ON dbo.hoi_thoai(id_khach_hang);
    CREATE INDEX IX_hoi_thoai_loai_trang_thai ON dbo.hoi_thoai(loai, trang_thai);
END;

IF OBJECT_ID('dbo.tin_nhan', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.tin_nhan (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        id_hoi_thoai BIGINT NOT NULL,
        loai_nguoi_gui NVARCHAR(20) NOT NULL,
        id_nguoi_gui BIGINT NULL,
        noi_dung NVARCHAR(2000) NOT NULL,
        da_doc BIT NOT NULL DEFAULT 0,
        ngay_tao DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_tin_nhan_hoi_thoai FOREIGN KEY (id_hoi_thoai) REFERENCES dbo.hoi_thoai(id)
    );
    CREATE INDEX IX_tin_nhan_hoi_thoai_id ON dbo.tin_nhan(id_hoi_thoai, id);
END;
