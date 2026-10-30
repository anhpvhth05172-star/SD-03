-- Them cot id_ca_lam (FK -> ca_lam) vao ca_lam_viec.
-- Nguon: commit f33ecab doi entity CaLamViec.caTheoLich tu id_ca_danh_muc sang id_ca_lam nhung khong co migration.
IF COL_LENGTH('dbo.ca_lam_viec', 'id_ca_lam') IS NULL
BEGIN
    ALTER TABLE dbo.ca_lam_viec ADD id_ca_lam BIGINT NULL;
    ALTER TABLE dbo.ca_lam_viec
        ADD CONSTRAINT fk_ca_lam_viec_ca_lam
        FOREIGN KEY (id_ca_lam) REFERENCES dbo.ca_lam(id);
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'ix_ca_lam_viec_ca_lam'
      AND object_id = OBJECT_ID('dbo.ca_lam_viec')
)
    CREATE INDEX ix_ca_lam_viec_ca_lam ON dbo.ca_lam_viec(id_ca_lam);
