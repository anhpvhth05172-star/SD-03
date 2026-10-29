-- Tao cac bang gio hang neu database chua co.
-- Script an toan khi chay lai va khong tao/xoa/sua du lieu hien co.

IF OBJECT_ID(N'dbo.gio_hang', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.gio_hang (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_gio_hang PRIMARY KEY,
        ma_gio_hang NVARCHAR(255) NULL,
        ngay_tao DATETIME2 NULL,
        ngay_cap_nhat DATETIME2 NULL,
        trang_thai NVARCHAR(255) NULL,
        id_khach_hang BIGINT NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.gio_hang_chi_tiet', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.gio_hang_chi_tiet (
        id INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_gio_hang_chi_tiet PRIMARY KEY,
        so_luong INT NULL,
        don_gia INT NULL,
        ngay_them DATETIME2 NULL,
        id_gio_hang INT NULL,
        id_san_pham_chi_tiet BIGINT NULL
    );
END;
GO

IF OBJECT_ID(N'dbo.gio_hang', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.khach_hang', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_gio_hang_khach_hang')
BEGIN
    ALTER TABLE dbo.gio_hang
        ADD CONSTRAINT FK_gio_hang_khach_hang
        FOREIGN KEY (id_khach_hang) REFERENCES dbo.khach_hang(id);
END;
GO

IF OBJECT_ID(N'dbo.gio_hang_chi_tiet', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.gio_hang', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_gio_hang_chi_tiet_gio_hang')
BEGIN
    ALTER TABLE dbo.gio_hang_chi_tiet
        ADD CONSTRAINT FK_gio_hang_chi_tiet_gio_hang
        FOREIGN KEY (id_gio_hang) REFERENCES dbo.gio_hang(id);
END;
GO

IF OBJECT_ID(N'dbo.gio_hang_chi_tiet', N'U') IS NOT NULL
   AND OBJECT_ID(N'dbo.san_pham_chi_tiet', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_gio_hang_chi_tiet_san_pham_chi_tiet')
BEGIN
    ALTER TABLE dbo.gio_hang_chi_tiet
        ADD CONSTRAINT FK_gio_hang_chi_tiet_san_pham_chi_tiet
        FOREIGN KEY (id_san_pham_chi_tiet) REFERENCES dbo.san_pham_chi_tiet(id);
END;
GO
