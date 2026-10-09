-- Migration: sua ten_tai_khoan (varchar -> nvarchar, goc loi 'Ph?m') + them quyen tai khoan
-- Da chay tren PolyShoesDB

-- 1. Sua ten_tai_khoan sang NVARCHAR (buoc phai drop UNIQUE constraint truoc)
ALTER TABLE khach_hang DROP CONSTRAINT [UQ__khach_ha__17112F09E5FC2799];
ALTER TABLE khach_hang ALTER COLUMN ten_tai_khoan NVARCHAR(100) NOT NULL;
ALTER TABLE khach_hang ADD CONSTRAINT UQ_khach_hang_ten_tai_khoan UNIQUE (ten_tai_khoan);

-- 2. Them cot vai tro (mac dinh USER)
IF COL_LENGTH('khach_hang', 'vai_tro') IS NULL
    ALTER TABLE khach_hang ADD vai_tro NVARCHAR(20) NOT NULL DEFAULT 'USER';

-- 3. Du lieu: sua ten tai khoan id=3 bi loi va cap quyen ADMIN
UPDATE khach_hang SET ten_tai_khoan = ten_khach_hang WHERE id = 3;
UPDATE khach_hang SET vai_tro = 'ADMIN' WHERE id = 3;
