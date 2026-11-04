-- Fix schema drift: entity LichLamViec khong con map cot id_ca (da doi sang id_ca_lam)
-- nhung cot id_ca van NOT NULL nen INSERT that that bai (NULL).
-- Cho phep NULL, GIU cot + FK cu (khong DROP). Idempotent.
SET QUOTED_IDENTIFIER ON;

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.lich_lam_viec')
      AND name = 'id_ca'
      AND is_nullable = 0
)
    ALTER TABLE dbo.lich_lam_viec ALTER COLUMN id_ca BIGINT NULL;
