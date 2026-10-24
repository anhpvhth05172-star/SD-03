package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.datn.dto.ThongKe.ChiTietThongKeRow;
import com.example.datn.dto.ThongKe.HoaDonThongKeRow;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.dto.ThongKe.TrangThaiDonDTO;
import com.example.datn.repository.ThongKeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
}
