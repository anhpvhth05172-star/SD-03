IF COL_LENGTH('dbo.hinh_anh_san_pham', 'duong_dan') IS NOT NULL
BEGIN
    ALTER TABLE dbo.hinh_anh_san_pham ALTER COLUMN duong_dan NVARCHAR(MAX) NULL;
END;

IF COL_LENGTH('dbo.hinh_anh_san_pham', 'ten_anh') IS NOT NULL
BEGIN
    ALTER TABLE dbo.hinh_anh_san_pham ALTER COLUMN ten_anh NVARCHAR(255) NULL;
END;

IF COL_LENGTH('dbo.hinh_anh_san_pham', 'id_san_pham_chi_tiet') IS NOT NULL
BEGIN
    ALTER TABLE dbo.hinh_anh_san_pham ALTER COLUMN id_san_pham_chi_tiet BIGINT NULL;
END;
