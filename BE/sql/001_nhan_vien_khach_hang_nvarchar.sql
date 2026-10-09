/*
 * PolyShoesDB - Sua cot luu tieng Viet cho man hinh nhan vien / khach hang
 *
 * Nguyen nhan: nhan_vien.ten_tai_khoan, khach_hang.ten_tai_khoan, *.email dang la
 * varchar voi collation SQL_Latin1_General_CP1_CI_AS (CP1252). Ky tu tieng Viet
 * (Da, ę, ộ...) khong co trong CP1252 bi JDBC ghi thanh dau '?' khi luu.
 * Vi du: "Dang Thi Y" luu thanh hex D03F6E67... => doc lai "D?ng Th? Y".
 *
 * Chay mot lan tren DB dev. Spring JPA dat ddl-auto=none nen khong sinh lai schema.
 * Luu y: cot ten_tai_khoan dang co index unique => phai drop index truoc khi doi kieu.
 */

-- 1) nhan_vien.ten_tai_khoan: varchar(100) -> nvarchar(100)
DECLARE @idx NVARCHAR(200);
SELECT @idx = i.name
FROM sys.indexes i
WHERE i.object_id = OBJECT_ID('dbo.nhan_vien')
  AND i.is_unique = 1 AND i.type > 0
  AND EXISTS (SELECT 1 FROM sys.index_columns ic
              WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
                AND ic.column_id = COLUMNPROPERTY(i.object_id, 'ten_tai_khoan', 'ColumnId'));
IF @idx IS NOT NULL
    EXEC('ALTER TABLE dbo.nhan_vien DROP CONSTRAINT [' + @idx + ']');
ALTER TABLE dbo.nhan_vien ALTER COLUMN ten_tai_khoan NVARCHAR(100) NOT NULL;
ALTER TABLE dbo.nhan_vien ADD CONSTRAINT UQ_nhan_vien_ten_tai_khoan UNIQUE (ten_tai_khoan);

-- 2) khach_hang.ten_tai_khoan: varchar(100) -> nvarchar(100)
SET @idx = NULL;
SELECT @idx = i.name
FROM sys.indexes i
WHERE i.object_id = OBJECT_ID('dbo.khach_hang')
  AND i.is_unique = 1 AND i.type > 0
  AND EXISTS (SELECT 1 FROM sys.index_columns ic
              WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
                AND ic.column_id = COLUMNPROPERTY(i.object_id, 'ten_tai_khoan', 'ColumnId'));
IF @idx IS NOT NULL
    EXEC('ALTER TABLE dbo.khach_hang DROP CONSTRAINT [' + @idx + ']');
ALTER TABLE dbo.khach_hang ALTER COLUMN ten_tai_khoan NVARCHAR(100) NOT NULL;
ALTER TABLE dbo.khach_hang ADD CONSTRAINT UQ_khach_hang_ten_tai_khoan UNIQUE (ten_tai_khoan);

-- 3) email: varchar(150) -> nvarchar(150) (regex hop le cho phep ky tu Unicode)
ALTER TABLE dbo.nhan_vien ALTER COLUMN email NVARCHAR(150) NULL;
ALTER TABLE dbo.khach_hang ALTER COLUMN email NVARCHAR(150) NULL;

-- 4) ma_nhan_vien / ma_khach_hang / so_dien_thoai van giu varchar (toan ASCII theo validate)
-- 5) mat_khau van giu varchar (khong hien thi, khong anh huong hien thi tieng Viet)
