-- Migration fix schema drift sau merge hoang-anh — them cot mo_ta / ma_* thieu
SET QUOTED_IDENTIFIER ON;

IF COL_LENGTH('dbo.kich_co', 'mo_ta') IS NULL
    ALTER TABLE dbo.kich_co ADD mo_ta NVARCHAR(255) NULL;

IF COL_LENGTH('dbo.mau_sac', 'mo_ta') IS NULL
    ALTER TABLE dbo.mau_sac ADD mo_ta NVARCHAR(255) NULL;

IF COL_LENGTH('dbo.kieu_dang', 'mo_ta') IS NULL
    ALTER TABLE dbo.kieu_dang ADD mo_ta NVARCHAR(255) NULL;

IF COL_LENGTH('dbo.dot_giam_gia', 'mo_ta') IS NULL
    ALTER TABLE dbo.dot_giam_gia ADD mo_ta NVARCHAR(500) NULL;

IF COL_LENGTH('dbo.xuat_xu', 'mo_ta') IS NULL
    ALTER TABLE dbo.xuat_xu ADD mo_ta NVARCHAR(255) NULL;

IF COL_LENGTH('dbo.de_giay', 'ma_de_giay') IS NULL
    ALTER TABLE dbo.de_giay ADD ma_de_giay NVARCHAR(50) NULL;

IF COL_LENGTH('dbo.than_giay', 'ma_than_giay') IS NULL
    ALTER TABLE dbo.than_giay ADD ma_than_giay NVARCHAR(50) NULL;
