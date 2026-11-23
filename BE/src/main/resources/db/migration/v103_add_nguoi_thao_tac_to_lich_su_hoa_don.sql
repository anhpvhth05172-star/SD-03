IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'lich_su_hoa_don' AND COLUMN_NAME = 'nguoi_thao_tac'
)
BEGIN
    ALTER TABLE lich_su_hoa_don ADD nguoi_thao_tac NVARCHAR(150) NULL;
END
