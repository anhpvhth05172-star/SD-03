-- v111: Don dep cac bang, cot va du lieu seed khong su dung (idempotent).
-- Luu y: SqlMigrationRunner tu boc toan bo script trong 1 transaction, khong dung BEGIN/COMMIT o day.

-- 1. Xoa bang lich_su_thanh_toan (0 dong, khong dung trong code)
IF OBJECT_ID('dbo.lich_su_thanh_toan', 'U') IS NOT NULL
BEGIN
    DROP TABLE dbo.lich_su_thanh_toan;
END;
GO

-- 2. Bo cac cot & bang than_giay, de_giay va 4 cot du thua tren san_pham_chi_tiet
IF OBJECT_ID('dbo.san_pham_chi_tiet', 'U') IS NOT NULL
BEGIN
    DECLARE @sqlSpct NVARCHAR(MAX) = N'';
    SELECT @sqlSpct = @sqlSpct
        + N'ALTER TABLE dbo.san_pham_chi_tiet DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
    WHERE fk.parent_object_id = OBJECT_ID('dbo.san_pham_chi_tiet')
      AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) IN ('id_than_giay', 'id_de_giay');

    IF @sqlSpct <> N'' EXEC sp_executesql @sqlSpct;
END;
GO

IF COL_LENGTH('dbo.san_pham_chi_tiet', 'id_than_giay') IS NOT NULL
    ALTER TABLE dbo.san_pham_chi_tiet DROP COLUMN id_than_giay;
IF COL_LENGTH('dbo.san_pham_chi_tiet', 'id_de_giay') IS NOT NULL
    ALTER TABLE dbo.san_pham_chi_tiet DROP COLUMN id_de_giay;
IF COL_LENGTH('dbo.san_pham_chi_tiet', 'chieu_cao_de') IS NOT NULL
    ALTER TABLE dbo.san_pham_chi_tiet DROP COLUMN chieu_cao_de;
IF COL_LENGTH('dbo.san_pham_chi_tiet', 'chieu_cao_got') IS NOT NULL
    ALTER TABLE dbo.san_pham_chi_tiet DROP COLUMN chieu_cao_got;
IF COL_LENGTH('dbo.san_pham_chi_tiet', 'do_chenh_got_mui') IS NOT NULL
    ALTER TABLE dbo.san_pham_chi_tiet DROP COLUMN do_chenh_got_mui;
IF COL_LENGTH('dbo.san_pham_chi_tiet', 'form_giay') IS NOT NULL
    ALTER TABLE dbo.san_pham_chi_tiet DROP COLUMN form_giay;
GO

IF OBJECT_ID('dbo.than_giay', 'U') IS NOT NULL
    DROP TABLE dbo.than_giay;
IF OBJECT_ID('dbo.de_giay', 'U') IS NOT NULL
    DROP TABLE dbo.de_giay;
GO

-- 3. Bo cot chet id_dot_giam_gia tren hoa_don va phieu_giam_gia
IF OBJECT_ID('dbo.hoa_don', 'U') IS NOT NULL
BEGIN
    DECLARE @sqlHd NVARCHAR(MAX) = N'';
    SELECT @sqlHd = @sqlHd
        + N'ALTER TABLE dbo.hoa_don DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
    WHERE fk.parent_object_id = OBJECT_ID('dbo.hoa_don')
      AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_dot_giam_gia';

    IF @sqlHd <> N'' EXEC sp_executesql @sqlHd;
END;
GO

IF COL_LENGTH('dbo.hoa_don', 'id_dot_giam_gia') IS NOT NULL
    ALTER TABLE dbo.hoa_don DROP COLUMN id_dot_giam_gia;
GO

IF OBJECT_ID('dbo.phieu_giam_gia', 'U') IS NOT NULL
BEGIN
    DECLARE @sqlPgg NVARCHAR(MAX) = N'';
    SELECT @sqlPgg = @sqlPgg
        + N'ALTER TABLE dbo.phieu_giam_gia DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
    WHERE fk.parent_object_id = OBJECT_ID('dbo.phieu_giam_gia')
      AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_dot_giam_gia';

    IF @sqlPgg <> N'' EXEC sp_executesql @sqlPgg;
END;
GO

IF COL_LENGTH('dbo.phieu_giam_gia', 'id_dot_giam_gia') IS NOT NULL
    ALTER TABLE dbo.phieu_giam_gia DROP COLUMN id_dot_giam_gia;
GO

-- 4. Bo bang hinh_thuc_thanh_toan + cot id_hinh_thuc_thanh_toan + don seed thua phuong_thuc_thanh_toan
IF OBJECT_ID('dbo.phuong_thuc_thanh_toan', 'U') IS NOT NULL
BEGIN
    DECLARE @sqlPttt NVARCHAR(MAX) = N'';
    SELECT @sqlPttt = @sqlPttt
        + N'ALTER TABLE dbo.phuong_thuc_thanh_toan DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
    WHERE fk.parent_object_id = OBJECT_ID('dbo.phuong_thuc_thanh_toan')
      AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_hinh_thuc_thanh_toan';

    IF @sqlPttt <> N'' EXEC sp_executesql @sqlPttt;
END;
GO

IF COL_LENGTH('dbo.phuong_thuc_thanh_toan', 'id_hinh_thuc_thanh_toan') IS NOT NULL
    ALTER TABLE dbo.phuong_thuc_thanh_toan DROP COLUMN id_hinh_thuc_thanh_toan;
GO

IF OBJECT_ID('dbo.hinh_thuc_thanh_toan', 'U') IS NOT NULL
    DROP TABLE dbo.hinh_thuc_thanh_toan;
GO

-- Remap hoa don seed cu dang tro vao cac phuong thuc thanh toan du thua truoc khi xoa
IF EXISTS (SELECT 1 FROM dbo.phuong_thuc_thanh_toan WHERE id = 1)
    UPDATE dbo.hoa_don
    SET id_phuong_thuc_thanh_toan = 1
    WHERE id_phuong_thuc_thanh_toan IN (17, 19);

IF EXISTS (SELECT 1 FROM dbo.phuong_thuc_thanh_toan WHERE id = 2)
    UPDATE dbo.hoa_don
    SET id_phuong_thuc_thanh_toan = 2
    WHERE id_phuong_thuc_thanh_toan IN (7, 8, 9, 10, 15, 16, 20);

IF EXISTS (SELECT 1 FROM dbo.phuong_thuc_thanh_toan WHERE id = 5)
    UPDATE dbo.hoa_don
    SET id_phuong_thuc_thanh_toan = 5
    WHERE id_phuong_thuc_thanh_toan IN (6, 12, 13, 14, 18);

DELETE FROM dbo.phuong_thuc_thanh_toan
WHERE id NOT IN (1, 2, 3, 4, 5, 11)
  AND NOT EXISTS (
      SELECT 1 FROM dbo.hoa_don h WHERE h.id_phuong_thuc_thanh_toan = dbo.phuong_thuc_thanh_toan.id
  );
GO

-- 5. Giu dung 2 vai tro (ADMIN, STAFF), xoa cac vai tro seed du thua khong co nhan vien tham chieu
IF OBJECT_ID('dbo.vai_tro', 'U') IS NOT NULL
BEGIN
    DELETE FROM dbo.vai_tro
    WHERE ma_vai_tro NOT IN ('ADMIN', 'STAFF')
      AND NOT EXISTS (
          SELECT 1 FROM dbo.nhan_vien nv WHERE nv.id_vai_tro = dbo.vai_tro.id
      );
END;
GO

-- 6. Bo cot thua id_ca (lich_lam_viec), id_ca_danh_muc (ca_lam_viec) va bang danh_muc_ca_lam_viec
IF OBJECT_ID('dbo.lich_lam_viec', 'U') IS NOT NULL
BEGIN
    DECLARE @sqlLlv NVARCHAR(MAX) = N'';
    SELECT @sqlLlv = @sqlLlv
        + N'ALTER TABLE dbo.lich_lam_viec DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
    WHERE fk.parent_object_id = OBJECT_ID('dbo.lich_lam_viec')
      AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_ca';

    IF @sqlLlv <> N'' EXEC sp_executesql @sqlLlv;

    IF EXISTS (
        SELECT 1 FROM sys.key_constraints
        WHERE name = 'uq_lich_lam_viec_nhan_vien_ca_ngay'
          AND parent_object_id = OBJECT_ID('dbo.lich_lam_viec')
    )
        ALTER TABLE dbo.lich_lam_viec DROP CONSTRAINT uq_lich_lam_viec_nhan_vien_ca_ngay;
END;
GO

IF COL_LENGTH('dbo.lich_lam_viec', 'id_ca') IS NOT NULL
    ALTER TABLE dbo.lich_lam_viec DROP COLUMN id_ca;
GO

IF OBJECT_ID('dbo.ca_lam_viec', 'U') IS NOT NULL
BEGIN
    DECLARE @sqlClv NVARCHAR(MAX) = N'';
    SELECT @sqlClv = @sqlClv
        + N'ALTER TABLE dbo.ca_lam_viec DROP CONSTRAINT ' + QUOTENAME(fk.name) + N';'
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
    WHERE fk.parent_object_id = OBJECT_ID('dbo.ca_lam_viec')
      AND COL_NAME(fkc.parent_object_id, fkc.parent_column_id) = 'id_ca_danh_muc';

    IF @sqlClv <> N'' EXEC sp_executesql @sqlClv;

    IF EXISTS (
        SELECT 1 FROM sys.indexes
        WHERE name = 'ix_ca_lam_viec_danh_muc'
          AND object_id = OBJECT_ID('dbo.ca_lam_viec')
    )
        DROP INDEX ix_ca_lam_viec_danh_muc ON dbo.ca_lam_viec;
END;
GO

IF COL_LENGTH('dbo.ca_lam_viec', 'id_ca_danh_muc') IS NOT NULL
    ALTER TABLE dbo.ca_lam_viec DROP COLUMN id_ca_danh_muc;
GO

IF OBJECT_ID('dbo.danh_muc_ca_lam_viec', 'U') IS NOT NULL
    DROP TABLE dbo.danh_muc_ca_lam_viec;
GO
