-- Lien ket phien giao ca thuc te voi ca danh muc da xep trong lich lam viec.
-- Chay mot lan sau migration_lich_lam_viec.sql va migration_giao_ca.sql.
IF COL_LENGTH('dbo.ca_lam_viec', 'id_ca_danh_muc') IS NULL
BEGIN
    ALTER TABLE dbo.ca_lam_viec ADD id_ca_danh_muc BIGINT NULL;
    ALTER TABLE dbo.ca_lam_viec
        ADD CONSTRAINT fk_ca_lam_viec_danh_muc
        FOREIGN KEY (id_ca_danh_muc) REFERENCES dbo.danh_muc_ca_lam_viec(id);
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'ix_ca_lam_viec_danh_muc'
      AND object_id = OBJECT_ID('dbo.ca_lam_viec')
)
    CREATE INDEX ix_ca_lam_viec_danh_muc ON dbo.ca_lam_viec(id_ca_danh_muc);
