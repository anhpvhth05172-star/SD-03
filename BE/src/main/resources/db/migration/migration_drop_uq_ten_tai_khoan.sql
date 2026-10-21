-- Migration: Cho phep nhieu nhan vien co cung ho ten, phan biet bang ma_nhan_vien duy nhat
IF EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UQ_nhan_vien_ten_tai_khoan' AND object_id = OBJECT_ID('dbo.nhan_vien'))
BEGIN
    ALTER TABLE dbo.nhan_vien DROP CONSTRAINT UQ_nhan_vien_ten_tai_khoan;
END;
