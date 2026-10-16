-- Sua hoa don POS cu: phi van chuyen TAI_QUAY ve 0, tinh lai tong tien / tien giam / tien sau giam gia
-- Cong thuc giong HoaDonService.tinhGiamGia: PERCENT = FLOOR(hang * % / 100), cap giam_toi_da, cap hang.
SET NOCOUNT ON;

WITH affected AS (
    SELECT
        h.id,
        h.tong_tien - h.phi_van_chuyen AS hang,
        p.loai_giam_gia,
        p.gia_tri_giam,
        p.giam_toi_da
    FROM hoa_don h
    LEFT JOIN phieu_giam_gia p ON p.id = h.id_phieu_giam_gia
    WHERE h.loai_don = 'TAI_QUAY'
      AND h.phi_van_chuyen > 0
),
recalc AS (
    SELECT
        id,
        hang,
        CASE
            WHEN giam_toi_da IS NOT NULL AND giam_toi_da > 0
                 AND (CASE
                        WHEN loai_giam_gia IS NULL OR gia_tri_giam IS NULL THEN CAST(0 AS DECIMAL(19,2))
                        WHEN UPPER(loai_giam_gia) = 'PERCENT' THEN FLOOR(hang * gia_tri_giam / 100)
                        ELSE gia_tri_giam
                      END) > giam_toi_da
            THEN giam_toi_da
            ELSE (CASE
                    WHEN loai_giam_gia IS NULL OR gia_tri_giam IS NULL THEN CAST(0 AS DECIMAL(19,2))
                    WHEN UPPER(loai_giam_gia) = 'PERCENT' THEN FLOOR(hang * gia_tri_giam / 100)
                    ELSE gia_tri_giam
                  END)
        END AS giam_raw
    FROM affected
),
final AS (
    SELECT
        id,
        hang,
        CASE
            WHEN giam_raw > hang THEN hang
            WHEN giam_raw < 0 THEN CAST(0 AS DECIMAL(19,2))
            ELSE giam_raw
        END AS giam
    FROM recalc
)
UPDATE h
SET h.phi_van_chuyen = 0,
    h.tong_tien = f.hang,
    h.tien_giam = f.giam,
    h.tien_sau_giam_gia = f.hang - f.giam,
    h.ngay_cap_nhat = GETDATE(),
    h.nguoi_cap_nhat = 'system-fix-phi-pos'
FROM hoa_don h
JOIN final f ON f.id = h.id;

SELECT @@ROWCOUNT AS da_cap_nhat;
