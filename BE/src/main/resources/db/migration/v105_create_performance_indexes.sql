-- ====================================================================
-- Migration: v105_create_performance_indexes.sql
-- Mục đích: Đánh Non-Clustered Indexes tối ưu tốc độ truy vấn cho các
-- bảng thường xuyên lọc, sắp xếp, và JOIN trong hệ thống PolyShoes.
-- ====================================================================

-- 1. BẢNG hoa_don
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_hoa_don_trang_thai_ngay_tao' AND object_id = OBJECT_ID('hoa_don'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_hoa_don_trang_thai_ngay_tao
    ON hoa_don (trang_thai, ngay_tao DESC, da_xoa)
    INCLUDE (ma_hoa_don, loai_don, tong_tien, tien_sau_giam_gia, id_khach_hang, id_nhan_vien, id_phuong_thuc_thanh_toan, id_ca_lam_viec);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_hoa_don_id_khach_hang' AND object_id = OBJECT_ID('hoa_don'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_hoa_don_id_khach_hang
    ON hoa_don (id_khach_hang, da_xoa)
    INCLUDE (trang_thai, ngay_tao, tong_tien, tien_sau_giam_gia);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_hoa_don_id_ca_lam_viec' AND object_id = OBJECT_ID('hoa_don'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_hoa_don_id_ca_lam_viec
    ON hoa_don (id_ca_lam_viec)
    WHERE id_ca_lam_viec IS NOT NULL;
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_hoa_don_ma_hoa_don' AND object_id = OBJECT_ID('hoa_don'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_hoa_don_ma_hoa_don
    ON hoa_don (ma_hoa_don);
END;

-- 2. BẢNG chi_tiet_hoa_don
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_chi_tiet_hoa_don_id_hoa_don' AND object_id = OBJECT_ID('chi_tiet_hoa_don'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_chi_tiet_hoa_don_id_hoa_don
    ON chi_tiet_hoa_don (id_hoa_don)
    INCLUDE (id_san_pham_chi_tiet, so_luong, don_gia, thanh_tien, trang_thai);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_chi_tiet_hoa_don_id_spct' AND object_id = OBJECT_ID('chi_tiet_hoa_don'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_chi_tiet_hoa_don_id_spct
    ON chi_tiet_hoa_don (id_san_pham_chi_tiet);
END;

-- 3. BẢNG san_pham_chi_tiet
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_spct_id_san_pham_trang_thai' AND object_id = OBJECT_ID('san_pham_chi_tiet'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_spct_id_san_pham_trang_thai
    ON san_pham_chi_tiet (id_san_pham, trang_thai)
    INCLUDE (id_mau_sac, id_kich_co, so_luong, gia_ban, ma_chi_tiet_san_pham);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_spct_trang_thai' AND object_id = OBJECT_ID('san_pham_chi_tiet'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_spct_trang_thai
    ON san_pham_chi_tiet (trang_thai)
    INCLUDE (id_san_pham, id_mau_sac, id_kich_co, so_luong, gia_ban, ma_chi_tiet_san_pham);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_spct_ma' AND object_id = OBJECT_ID('san_pham_chi_tiet'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_spct_ma
    ON san_pham_chi_tiet (ma_chi_tiet_san_pham);
END;

-- 4. BẢNG san_pham
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_san_pham_trang_thai' AND object_id = OBJECT_ID('san_pham'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_san_pham_trang_thai
    ON san_pham (trang_thai)
    INCLUDE (ma_san_pham, ten_san_pham, id_thuong_hieu, id_loai_giay);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_san_pham_thuong_hieu' AND object_id = OBJECT_ID('san_pham'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_san_pham_thuong_hieu
    ON san_pham (id_thuong_hieu);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_san_pham_loai_giay' AND object_id = OBJECT_ID('san_pham'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_san_pham_loai_giay
    ON san_pham (id_loai_giay);
END;

-- 5. BẢNG hinh_anh_san_pham
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_hasp_id_san_pham' AND object_id = OBJECT_ID('hinh_anh_san_pham'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_hasp_id_san_pham
    ON hinh_anh_san_pham (id_san_pham)
    INCLUDE (duong_dan);
END;

-- 6. BẢNG lich_su_hoa_don
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lshd_id_hoa_don' AND object_id = OBJECT_ID('lich_su_hoa_don'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_lshd_id_hoa_don
    ON lich_su_hoa_don (id_hoa_don, thoi_gian DESC);
END;

-- 7. BẢNG phieu_giam_gia
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_pgg_trang_thai_ngay' AND object_id = OBJECT_ID('phieu_giam_gia'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_pgg_trang_thai_ngay
    ON phieu_giam_gia (trang_thai, ngay_bat_dau, ngay_ket_thuc)
    INCLUDE (ma_phieu_giam_gia, loai_giam_gia, gia_tri_giam, so_luong, so_luong_da_su_dung, hoa_don_toi_thieu, giam_toi_da);
END;

-- 8. BẢNG lich_su_su_dung_phieu_giam_gia
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lssd_kh_pgg' AND object_id = OBJECT_ID('lich_su_su_dung_phieu_giam_gia'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_lssd_kh_pgg
    ON lich_su_su_dung_phieu_giam_gia (id_khach_hang, id_phieu_giam_gia);
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_lssd_id_hoa_don' AND object_id = OBJECT_ID('lich_su_su_dung_phieu_giam_gia'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_lssd_id_hoa_don
    ON lich_su_su_dung_phieu_giam_gia (id_hoa_don);
END;

-- 9. BẢNG ca_lam_viec
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_ca_lam_viec_trang_thai' AND object_id = OBJECT_ID('ca_lam_viec'))
BEGIN
    CREATE NONCLUSTERED INDEX IX_ca_lam_viec_trang_thai
    ON ca_lam_viec (trang_thai, thoi_gian_mo DESC);
END;
