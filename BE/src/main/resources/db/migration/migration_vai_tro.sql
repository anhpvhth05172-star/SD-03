-- Migration: sua ten_tai_khoan (varchar -> nvarchar, goc loi 'Ph?m') + them quyen tai khoan
-- Da chay tren PolyShoesDB

-- 1. Sua ten_tai_khoan sang NVARCHAR (buoc phai drop UNIQUE constraint truoc)
-- Tim ten constraint/index unique dang co tren cot (khong hardcode ten auto-gen)
DECLARE @idx NVARCHAR(200);
SELECT @idx = i.name
FROM sys.indexes i
WHERE i.object_id = OBJECT_ID('dbo.khach_hang')
  AND i.is_unique = 1 AND i.type > 0
  AND EXISTS (SELECT 1 FROM sys.index_columns ic
              WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id
                AND ic.column_id = COLUMNPROPERTY(i.object_id, 'ten_tai_khoan', 'ColumnId'));
IF @idx IS NOT NULL
    EXEC('ALTER TABLE dbo.khach_hang DROP CONSTRAINT [' + @idx + ']');
ALTER TABLE khach_hang ALTER COLUMN ten_tai_khoan NVARCHAR(100) NOT NULL;
ALTER TABLE khach_hang ADD CONSTRAINT UQ_khach_hang_ten_tai_khoan UNIQUE (ten_tai_khoan);

-- 2. Them cot vai tro (mac dinh USER)
IF COL_LENGTH('khach_hang', 'vai_tro') IS NULL
    ALTER TABLE khach_hang ADD vai_tro NVARCHAR(20) NOT NULL DEFAULT 'USER';

-- 3. Du lieu: sua ten tai khoan id=3 bi loi va cap quyen ADMIN
UPDATE khach_hang SET ten_tai_khoan = ten_khach_hang WHERE id = 3;
UPDATE khach_hang SET vai_tro = 'ADMIN' WHERE id = 3;
