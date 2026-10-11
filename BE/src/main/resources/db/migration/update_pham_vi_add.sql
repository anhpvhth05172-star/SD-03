IF COL_LENGTH('dbo.phieu_giam_gia', 'pham_vi') IS NULL
    ALTER TABLE dbo.phieu_giam_gia ADD pham_vi NVARCHAR(20) NULL;
