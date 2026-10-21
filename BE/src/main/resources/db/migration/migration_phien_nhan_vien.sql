-- Phien dang nhap ho tro nhan vien: them id_nhan_vien, cho phep id_khach_hang null
IF COL_LENGTH('dbo.phien_dang_nhap', 'id_nhan_vien') IS NULL
    ALTER TABLE dbo.phien_dang_nhap ADD id_nhan_vien BIGINT NULL;

IF EXISTS (SELECT 1 FROM sys.columns WHERE name = 'id_khach_hang' AND is_nullable = 0 AND object_id = OBJECT_ID('dbo.phien_dang_nhap'))
    ALTER TABLE dbo.phien_dang_nhap ALTER COLUMN id_khach_hang BIGINT NULL;
