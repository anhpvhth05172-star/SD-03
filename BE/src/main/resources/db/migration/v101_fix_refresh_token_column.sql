-- Fix refresh_token not null trong bang phien_dang_nhap
IF COL_LENGTH('dbo.phien_dang_nhap', 'refresh_token') IS NOT NULL
BEGIN
    ALTER TABLE dbo.phien_dang_nhap ALTER COLUMN refresh_token NVARCHAR(500) NULL;
END;
