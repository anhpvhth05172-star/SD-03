package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.datn.dto.ThongKe.ChiTietThongKeRow;
import com.example.datn.dto.ThongKe.HoaDonThongKeRow;
import com.example.datn.dto.ThongKe.KySoSanhDTO;
import com.example.datn.dto.ThongKe.SanPhamThongKeRow;
import com.example.datn.dto.ThongKe.SoSanhDTO;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.dto.ThongKe.TopSanPhamKyDTO;
import com.example.datn.dto.ThongKe.TrangThaiDonDTO;
import com.example.datn.repository.ThongKeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ThongKeServiceTest {

    private final ThongKeRepository repo = mock(ThongKeRepository.class);

    private ThongKeService service() {
        return new ThongKeService(repo);
    }

    private static HoaDonThongKeRow hd(long id, String trangThai, String tien, String maHinhThuc) {
        return new HoaDonThongKeRow(id, trangThai, new BigDecimal(tien), false, maHinhThuc);
    }

    private static HoaDonThongKeRow hdXoa(long id, String trangThai, String tien) {
        return new HoaDonThongKeRow(id, trangThai, new BigDecimal(tien), true, "TM");
    }

    private static ChiTietThongKeRow ct(long idHd, String tenTh, String thanhTien, int sl) {
        return new ChiTietThongKeRow(idHd, "DA_HOAN_THANH", false, tenTh, new BigDecimal(thanhTien), sl);
    }

    private static ChiTietThongKeRow ctTrangThai(long idHd, String trangThai, String tenTh, String thanhTien, int sl) {
        return new ChiTietThongKeRow(idHd, trangThai, false, tenTh, new BigDecimal(thanhTien), sl);
    }

    private void mockRows(List<HoaDonThongKeRow> hoaDons, List<ChiTietThongKeRow> chiTiets, long khachMoi) {
        when(repo.findHoaDonTrongKhoang(any(), any())).thenReturn(hoaDons);
        when(repo.findChiTietTrongKhoang(any(), any())).thenReturn(chiTiets);
        when(repo.countKhachMoi(any(), any())).thenReturn(khachMoi);
    }

    @Test
    void tongQuan_tinhDungCongThucCoBan() {
        mockRows(
            List.of(
                hd(1, "DA_HOAN_THANH", "1000000", "TM"),
                hd(2, "CHO_XAC_NHAN", "2000000", "BANK"),
                hd(3, "DA_HUY", "9000000", "TM")
            ),
            List.of(
                ct(1, "Nike", "1000000", 2),
                new ChiTietThongKeRow(2L, "CHO_XAC_NHAN", false, "Adidas", new BigDecimal("2000000"), 3)
            ),
            7
        );
        TongQuanDTO r = service().tongQuan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 19), null);
        assertEquals(3_000_000L, r.doanhThu());
        assertEquals(2L, r.soDon());
        assertEquals(1_500_000L, r.aov());
        assertEquals(1_000_000L, r.tienMat());
        assertEquals(2_000_000L, r.tienChuyenKhoan());
        assertEquals(5L, r.sanPhamBan());
        assertEquals(7L, r.khachMoi());
    }

    @Test
    void tongQuan_loaiDonHuyHoanTienVaDaXoa() {
        mockRows(
            List.of(
                hd(1, "DA_HOAN_THANH", "1000000", "TM"),
                hd(2, "DA_HUY", "2000000", "TM"),
                hd(3, "DA_HOAN_TIEN", "3000000", "TM"),
                hdXoa(4, "DA_HOAN_THANH", "4000000")
            ),
            List.of(),
            0
        );
        TongQuanDTO r = service().tongQuan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 19), null);
        assertEquals(1_000_000L, r.doanhThu());
        assertEquals(1L, r.soDon());
        assertEquals(0L, r.sanPhamBan());
    }

    @Test
    void tongQuan_locThuongHieuTinhTienMatTheoTyTrong() {
        // HD1 tien mat 1 trieu: dong Nike 750k + Adidas 250k. HD2 BANK 2 trieu: dong Nike 2 trieu.
        mockRows(
            List.of(
                hd(1, "DA_HOAN_THANH", "1000000", "TM"),
                hd(2, "DA_HOAN_THANH", "2000000", "BANK")
            ),
            List.of(
                ct(1, "Nike", "750000", 2),
                ct(1, "Adidas", "250000", 1),
                ct(2, "Nike", "2000000", 3)
            ),
            0
        );
        TongQuanDTO r = service().tongQuan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 19), "Nike");
        assertEquals(2_750_000L, r.doanhThu());
        assertEquals(2L, r.soDon());
        assertEquals(5L, r.sanPhamBan());
        assertEquals(750_000L, r.tienMat());
        assertEquals(2_000_000L, r.tienChuyenKhoan());
    }

    @Test
    void tongQuan_khongCoDonTraVe0() {
        mockRows(List.of(), List.of(), 0);
        TongQuanDTO r = service().tongQuan(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 19), null);
        assertEquals(0L, r.doanhThu());
        assertEquals(0L, r.soDon());
        assertEquals(0L, r.aov());
        assertEquals(0L, r.tienMat());
        assertEquals(0L, r.tienChuyenKhoan());
        assertEquals(0L, r.sanPhamBan());
        assertEquals(0L, r.khachMoi());
    }

    @Test
    void trangThaiDon_du8DongTheoThuTuVi() {
        mockRows(
            List.of(
                hd(1, "CHO_XAC_NHAN", "1000", "TM"),
                hd(2, "CHO_XAC_NHAN", "2000", "TM"),
                hd(3, "DA_HUY", "3000", "TM")
            ),
            List.of(),
            0
        );
        List<TrangThaiDonDTO> rows = service().trangThaiDon(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 19), null);
        assertEquals(8, rows.size());
        List<String> vi = List.of(
            "Chờ xác nhận", "Đã xác nhận", "Chờ giao hàng", "Đang giao hàng",
            "Đã giao hàng", "Đã hoàn thành", "Đã hủy", "Đã hoàn tiền"
        );
        for (int i = 0; i < 8; i++) {
            assertEquals(vi.get(i), rows.get(i).ten());
        }
        assertEquals(2L, rows.get(0).soLuong());
        assertEquals(0L, rows.get(1).soLuong());
        assertEquals(1L, rows.get(6).soLuong());
        assertEquals(0L, rows.get(7).soLuong());
    }

    @Test
    void trangThaiDon_khiLocTHDemDonKhacNhau() {
        // 1 don chua 2 dong Nike -> dem 1 (khong phai 2)
        mockRows(
            List.of(hd(1, "CHO_XAC_NHAN", "1000", "TM")),
            List.of(
                ct(1, "Nike", "500", 1),
                ct(1, "Nike", "500", 1)
            ),
            0
        );
        List<TrangThaiDonDTO> rows = service().trangThaiDon(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 10, 19), "Nike");
        assertEquals(1L, rows.get(0).soLuong());
    }

    /* ---------------------------------- So sanh doanh thu ---------------------------------- */

    private void mockRowsTheoKy(
        java.util.Map<LocalDate, List<HoaDonThongKeRow>> hoaDonsTheoKy,
        java.util.Map<LocalDate, List<ChiTietThongKeRow>> chiTietsTheoKy
    ) {
        when(repo.findHoaDonTrongKhoang(any(), any())).thenAnswer(inv -> {
            LocalDateTime tu = inv.getArgument(0);
            return hoaDonsTheoKy.getOrDefault(tu.toLocalDate(), List.of());
        });
        when(repo.findChiTietTrongKhoang(any(), any())).thenAnswer(inv -> {
            LocalDateTime tu = inv.getArgument(0);
            return chiTietsTheoKy.getOrDefault(tu.toLocalDate(), List.of());
        });
    }

    @Test
    void soSanh_thangTraVe12KyTheoThuTuNam() {
        mockRows(List.of(), List.of(), 0);
        List<KySoSanhDTO> ky = service().tinhKySoSanh("thang", 2026, null, null, null, null, null);
        assertEquals(12, ky.size());
        assertEquals("T1", ky.get(0).maKy());
        assertEquals(LocalDate.of(2026, 1, 1), ky.get(0).tu());
        assertEquals(LocalDate.of(2026, 1, 31), ky.get(0).den());
        assertEquals("Tháng 12", ky.get(11).tenKy());
        assertEquals(LocalDate.of(2026, 12, 31), ky.get(11).den());
    }

    @Test
    void soSanh_quyTraVe4Ky() {
        mockRows(List.of(), List.of(), 0);
        List<KySoSanhDTO> ky = service().tinhKySoSanh("quy", 2026, null, null, null, null, null);
        assertEquals(4, ky.size());
        assertEquals("Q1", ky.get(0).maKy());
        assertEquals(LocalDate.of(2026, 1, 1), ky.get(0).tu());
        assertEquals(LocalDate.of(2026, 3, 31), ky.get(0).den());
        assertEquals(LocalDate.of(2026, 12, 31), ky.get(3).den());
    }

    @Test
    void soSanh_tinhChenhLechVaTangTruongTheoCongThucChuan() {
        // Chi ky 10/2026 co doanh thu; T9/T11 khong -> prev = 0 theo cong thuc chuan
        mockRowsTheoKy(
            java.util.Map.of(LocalDate.of(2026, 10, 1), List.of(hd(1, "DA_HOAN_THANH", "102352150", "TM"))),
            java.util.Map.of()
        );
        List<KySoSanhDTO> ky = service().tinhKySoSanh("thang", 2026, null, null, null, null, null);
        assertEquals(102_352_150L, ky.get(9).doanhThu());
        assertEquals(102_352_150L, ky.get(9).chenhLech());
        assertEquals(100, ky.get(9).tangTruong());
        assertTrue(ky.get(9).laCaoNhat());
        assertEquals(-102_352_150L, ky.get(10).chenhLech());
        assertEquals(-100, ky.get(10).tangTruong());
        assertEquals(0L, ky.get(0).chenhLech());
        assertEquals(0, ky.get(0).tangTruong());
    }

    @Test
    void soSanh_khongCoDuLieuTatCaKyBang0() {
        mockRows(List.of(), List.of(), 0);
        List<KySoSanhDTO> ky = service().tinhKySoSanh("thang", 2026, null, null, null, null, null);
        assertEquals(12, ky.size());
        for (KySoSanhDTO k : ky) {
            assertEquals(0L, k.doanhThu());
            assertEquals(0L, k.tienMat());
            assertEquals(0L, k.tienChuyenKhoan());
            assertEquals(0L, k.soDon());
            assertEquals(0L, k.aov());
            assertEquals(0L, k.sanPhamBan());
            assertEquals(0L, k.chenhLech());
            assertEquals(0, k.tangTruong());
            assertFalse(k.laCaoNhat());
        }
    }

    @Test
    void soSanh_namGioiHan5NamGanNhatCoDuLieu() {
        when(repo.cacNamCoDuLieu()).thenReturn(List.of(2026, 2025, 2024, 2023, 2022, 2021));
        mockRows(List.of(), List.of(), 0);
        List<KySoSanhDTO> ky = service().tinhKySoSanh("nam", 2026, null, null, null, null, null);
        assertEquals(5, ky.size());
        assertEquals("2026", ky.get(0).maKy());
        assertEquals(LocalDate.of(2026, 1, 1), ky.get(0).tu());
        assertEquals(LocalDate.of(2022, 12, 31), ky.get(4).den());
    }

    @Test
    void soSanh_2kyValidateThieuNgayNemIAE() {
        assertThrows(IllegalArgumentException.class, () ->
            service().tinhKySoSanh("2ky", null,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 27),
                LocalDate.of(2026, 9, 1), null, null));
    }

    @Test
    void soSanh_cheDoKhongHopLeNemIAE() {
        assertThrows(IllegalArgumentException.class, () ->
            service().tinhKySoSanh("abc", 2026, null, null, null, null, null));
    }

    @Test
    void soSanh_locThuongHieuTinhDungTienMatTyTrong() {
        // Cung fixture voi tongQuan_locThuongHieuTinhTienMatTheoTyTrong nhung nam trong ky 10/2026
        List<HoaDonThongKeRow> hoaDons = List.of(
            hd(1, "DA_HOAN_THANH", "1000000", "TM"),
            hd(2, "DA_HOAN_THANH", "2000000", "BANK")
        );
        List<ChiTietThongKeRow> chiTiets = List.of(
            ct(1, "Nike", "750000", 2),
            ct(1, "Adidas", "250000", 1),
            ct(2, "Nike", "2000000", 3)
        );
        mockRowsTheoKy(
            java.util.Map.of(LocalDate.of(2026, 10, 1), hoaDons),
            java.util.Map.of(LocalDate.of(2026, 10, 1), chiTiets)
        );
        List<KySoSanhDTO> ky = service().tinhKySoSanh("thang", 2026, null, null, null, null, "Nike");
        KySoSanhDTO t10 = ky.get(9);
        assertEquals(2_750_000L, t10.doanhThu());
        assertEquals(2L, t10.soDon());
        assertEquals(5L, t10.sanPhamBan());
        assertEquals(750_000L, t10.tienMat());
        assertEquals(2_000_000L, t10.tienChuyenKhoan());
    }

    @Test
    void soSanh_kyCungKhoangTraVeDoanhThuGiongTongQuan() {
        mockRows(
            List.of(
                hd(1, "DA_HOAN_THANH", "1000000", "TM"),
                hd(2, "CHO_XAC_NHAN", "2000000", "BANK"),
                hd(3, "DA_HUY", "9000000", "TM")
            ),
            List.of(ct(1, "Nike", "1000000", 2)),
            0
        );
        TongQuanDTO tong = service().tongQuan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null);
        List<KySoSanhDTO> ky = service().tinhKySoSanh("thang", 2026, null, null, null, null, null);
        assertEquals(tong.doanhThu(), ky.get(9).doanhThu());
        assertEquals(tong.soDon(), ky.get(9).soDon());
        assertEquals(tong.tienMat(), ky.get(9).tienMat());
        assertEquals(tong.tienChuyenKhoan(), ky.get(9).tienChuyenKhoan());
        assertEquals(tong.sanPhamBan(), ky.get(9).sanPhamBan());
    }

    /* ---------------------------------- Top san pham theo ky ---------------------------------- */

    private static SanPhamThongKeRow spr(long idHd, LocalDateTime ngayTao, String ma, String ten, String tenTh, String thanhTien, int sl) {
        return new SanPhamThongKeRow(idHd, "DA_HOAN_THANH", false, ngayTao, ma, ten, tenTh, new BigDecimal(thanhTien), sl);
    }

    private static final LocalDateTime THANG10 = LocalDateTime.of(2026, 10, 15, 10, 0);

    @Test
    void soSanh_topSapXepTheoTongBanGiamDanVaGioiHan10() {
        mockRows(List.of(), List.of(), 0);
        List<SanPhamThongKeRow> rows = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            rows.add(spr(i, THANG10, "SP" + i, "San pham " + i, null, "100000", i));
        }
        when(repo.findSanPhamTrongKhoang(any(), any())).thenReturn(rows);

        SoSanhDTO r = service().soSanh("thang", 2026, null, null, null, null, null, null);

        assertEquals(12, r.ky().size());
        List<TopSanPhamKyDTO> top = r.topSanPham();
        assertEquals(10, top.size());
        assertEquals("SP12", top.get(0).maSanPham());
        assertEquals(12L, top.get(0).tongBan());
        assertEquals("SP3", top.get(9).maSanPham());
        assertEquals(12, top.get(0).banTheoKy().size());
        assertEquals(12L, top.get(0).banTheoKy().get(9));
    }

    @Test
    void soSanh_topLocKeywordTheoMaVaTen() {
        mockRows(List.of(), List.of(), 0);
        when(repo.findSanPhamTrongKhoang(any(), any())).thenReturn(List.of(
            spr(1, THANG10, "SP001", "Ao thun co tron", null, "500000", 5),
            spr(2, THANG10, "SP002", "Ao polo nam", null, "300000", 3),
            spr(3, THANG10, "SP007", "moi 2", null, "100000", 1)
        ));

        assertEquals(3, service().soSanh("thang", 2026, null, null, null, null, null, "sp00").topSanPham().size());

        List<TopSanPhamKyDTO> theoTen = service().soSanh("thang", 2026, null, null, null, null, null, "THUN").topSanPham();
        assertEquals(1, theoTen.size());
        assertEquals("SP001", theoTen.get(0).maSanPham());

        assertEquals(0, service().soSanh("thang", 2026, null, null, null, null, null, "khong ton tai").topSanPham().size());
    }

    @Test
    void soSanh_topLocTheoThuongHieu() {
        mockRows(List.of(), List.of(), 0);
        when(repo.findSanPhamTrongKhoang(any(), any())).thenReturn(List.of(
            spr(1, THANG10, "SP001", "Ao FF", "FF Shirt", "700000", 7),
            spr(2, THANG10, "SP002", "Ao local", "Local Brand", "900000", 9),
            spr(3, THANG10, "SP003", "Ao FF 2", "FF Shirt", "100000", 1)
        ));

        List<TopSanPhamKyDTO> top = service().soSanh("thang", 2026, null, null, null, null, "FF Shirt", null).topSanPham();

        assertEquals(2, top.size());
        assertEquals("SP001", top.get(0).maSanPham());
        assertEquals("SP003", top.get(1).maSanPham());
    }

    @Test
    void soSanh_topRongKhiKhongCoDuLieu() {
        mockRows(List.of(), List.of(), 0);
        when(repo.findSanPhamTrongKhoang(any(), any())).thenReturn(List.of());

        SoSanhDTO r = service().soSanh("thang", 2026, null, null, null, null, null, null);

        assertEquals(12, r.ky().size());
        assertEquals(0, r.topSanPham().size());
    }
}
