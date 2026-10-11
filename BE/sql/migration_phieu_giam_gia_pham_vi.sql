/*
 * Migration: Thêm cột pham_vi vào bảng phieu_giam_gia
 * Hỗ trợ phạm vi: CONG_KHAI (công khai) hoặc CA_NHAN (cá nhân)
 */
SET NOCOUNT ON;

IF COL_LENGTH('dbo.phieu_giam_gia', 'pham_vi') IS NULL
BEGIN
    ALTER TABLE dbo.phieu_giam_gia
        ADD pham_vi NVARCHAR(50) NOT NULL
        CONSTRAINT DF_phieu_giam_gia_pham_vi DEFAULT N'CONG_KHAI';
    PRINT 'Đã thêm cột pham_vi vào bảng phieu_giam_gia thành công.';
END
ELSE
BEGIN
    PRINT 'Cột pham_vi đã tồn tại.';
END;
