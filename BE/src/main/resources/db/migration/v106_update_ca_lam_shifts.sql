-- v106_update_ca_lam_shifts.sql: Cập nhật 3 ca chuẩn PolyShoes
-- Ca sáng: 08:00 - 15:00
-- Ca tối: 15:00 - 22:00
-- Ca đêm: 22:00 - 08:00 (hôm sau) (đổi từ ca as / ca-as / ca-em thành Ca đêm)

SET QUOTED_IDENTIFIER ON;

-- 1. BẢNG dbo.ca_lam
IF OBJECT_ID('dbo.ca_lam', 'U') IS NOT NULL
BEGIN
    -- 1.1. Ca sáng (08:00 - 15:00)
    IF EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca = N'sang' OR ten_ca LIKE N'%sáng%')
    BEGIN
        UPDATE dbo.ca_lam
        SET ma_ca = N'sang',
            ten_ca = N'Ca sáng',
            bat_dau = '08:00:00',
            ket_thuc = '15:00:00',
            dang_bat = 1,
            ngay_cap_nhat = SYSUTCDATETIME()
        WHERE ma_ca = N'sang' OR ten_ca LIKE N'%sáng%';
    END
    ELSE
    BEGIN
        INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat)
        VALUES (N'sang', N'Ca sáng', '08:00:00', '15:00:00', 1);
    END;

    -- 1.2. Ca tối (15:00 - 22:00)
    IF EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca IN (N'chieu', N'toi') OR ten_ca LIKE N'%tối%' OR ten_ca LIKE N'%chiều%')
    BEGIN
        UPDATE dbo.ca_lam
        SET ma_ca = N'toi',
            ten_ca = N'Ca tối',
            bat_dau = '15:00:00',
            ket_thuc = '22:00:00',
            dang_bat = 1,
            ngay_cap_nhat = SYSUTCDATETIME()
        WHERE ma_ca IN (N'chieu', N'toi') OR ten_ca LIKE N'%tối%' OR ten_ca LIKE N'%chiều%';
    END
    ELSE
    BEGIN
        INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat)
        VALUES (N'toi', N'Ca tối', '15:00:00', '22:00:00', 1);
    END;

    -- 1.3. Ca đêm (22:00 - 08:00 hôm sau) - Đổi từ 'ca as', 'as', 'ca-em', 'ca-as' sang 'ca-dem'
    IF EXISTS (SELECT 1 FROM dbo.ca_lam WHERE ma_ca IN (N'ca-as', N'as', N'ca-em', N'ca-dem', N'dem') OR ten_ca LIKE N'%as%' OR ten_ca LIKE N'%đêm%')
    BEGIN
        UPDATE dbo.ca_lam
        SET ma_ca = N'ca-dem',
            ten_ca = N'Ca đêm',
            bat_dau = '22:00:00',
            ket_thuc = '08:00:00',
            dang_bat = 1,
            ngay_cap_nhat = SYSUTCDATETIME()
        WHERE ma_ca IN (N'ca-as', N'as', N'ca-em', N'ca-dem', N'dem') OR ten_ca LIKE N'%as%' OR ten_ca LIKE N'%đêm%';
    END
    ELSE
    BEGIN
        INSERT INTO dbo.ca_lam (ma_ca, ten_ca, bat_dau, ket_thuc, dang_bat)
        VALUES (N'ca-dem', N'Ca đêm', '22:00:00', '08:00:00', 1);
    END;
END;

-- 2. ĐỒNG BỘ BẢNG dbo.danh_muc_ca_lam_viec (nếu có)
IF OBJECT_ID('dbo.danh_muc_ca_lam_viec', 'U') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.danh_muc_ca_lam_viec WHERE ma_ca = N'sang' OR ten_ca LIKE N'%sáng%')
    BEGIN
        UPDATE dbo.danh_muc_ca_lam_viec
        SET ma_ca = N'sang',
            ten_ca = N'Ca sáng',
            gio_bat_dau = '08:00:00',
            gio_ket_thuc = '15:00:00',
            dang_bat = 1
        WHERE ma_ca = N'sang' OR ten_ca LIKE N'%sáng%';
    END
    ELSE
    BEGIN
        INSERT INTO dbo.danh_muc_ca_lam_viec (ma_ca, ten_ca, gio_bat_dau, gio_ket_thuc, dang_bat)
        VALUES (N'sang', N'Ca sáng', '08:00:00', '15:00:00', 1);
    END;

    IF EXISTS (SELECT 1 FROM dbo.danh_muc_ca_lam_viec WHERE ma_ca IN (N'chieu', N'toi') OR ten_ca LIKE N'%tối%' OR ten_ca LIKE N'%chiều%')
    BEGIN
        UPDATE dbo.danh_muc_ca_lam_viec
        SET ma_ca = N'toi',
            ten_ca = N'Ca tối',
            gio_bat_dau = '15:00:00',
            gio_ket_thuc = '22:00:00',
            dang_bat = 1
        WHERE ma_ca IN (N'chieu', N'toi') OR ten_ca LIKE N'%tối%' OR ten_ca LIKE N'%chiều%';
    END
    ELSE
    BEGIN
        INSERT INTO dbo.danh_muc_ca_lam_viec (ma_ca, ten_ca, gio_bat_dau, gio_ket_thuc, dang_bat)
        VALUES (N'toi', N'Ca tối', '15:00:00', '22:00:00', 1);
    END;

    IF EXISTS (SELECT 1 FROM dbo.danh_muc_ca_lam_viec WHERE ma_ca IN (N'ca-as', N'as', N'ca-em', N'ca-dem', N'dem') OR ten_ca LIKE N'%as%' OR ten_ca LIKE N'%đêm%')
    BEGIN
        UPDATE dbo.danh_muc_ca_lam_viec
        SET ma_ca = N'ca-dem',
            ten_ca = N'Ca đêm',
            gio_bat_dau = '22:00:00',
            gio_ket_thuc = '08:00:00',
            dang_bat = 1
        WHERE ma_ca IN (N'ca-as', N'as', N'ca-em', N'ca-dem', N'dem') OR ten_ca LIKE N'%as%' OR ten_ca LIKE N'%đêm%';
    END
    ELSE
    BEGIN
        INSERT INTO dbo.danh_muc_ca_lam_viec (ma_ca, ten_ca, gio_bat_dau, gio_ket_thuc, dang_bat)
        VALUES (N'ca-dem', N'Ca đêm', '22:00:00', '08:00:00', 1);
    END;
END;
