IF OBJECT_ID('dbo.cau_hinh_he_thong', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.cau_hinh_he_thong (
        ma_cau_hinh VARCHAR(100) NOT NULL PRIMARY KEY,
        gia_tri NVARCHAR(500) NOT NULL,
        ngay_cap_nhat DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        nguoi_cap_nhat NVARCHAR(150) NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM dbo.cau_hinh_he_thong WHERE ma_cau_hinh = 'FREESHIP_THRESHOLD')
BEGIN
    INSERT INTO dbo.cau_hinh_he_thong (ma_cau_hinh, gia_tri)
    VALUES ('FREESHIP_THRESHOLD', '10000000');
END;
