-- Migration: them ma dia chi GHN vao hoa_don cho tinh phi van chuyen
-- ddl-auto=none -> can chay thu cong truoc khi dung luong tinh phi online
IF COL_LENGTH('dbo.hoa_don', 'to_province_id') IS NULL
    ALTER TABLE dbo.hoa_don ADD to_province_id INT NULL;
IF COL_LENGTH('dbo.hoa_don', 'to_district_id') IS NULL
    ALTER TABLE dbo.hoa_don ADD to_district_id INT NULL;
IF COL_LENGTH('dbo.hoa_don', 'to_ward_code') IS NULL
    ALTER TABLE dbo.hoa_don ADD to_ward_code NVARCHAR(20) NULL;
