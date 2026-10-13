package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.datn.dto.HoaDon.LichSuHoaDonDTO;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.LichSuHoaDon;
import com.example.datn.repository.ChiTietHoaDonRepository;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.LichSuHoaDonRepository;
import com.example.datn.repository.LichSuSuDungPhieuGiamGiaRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.repository.PhuongThucThanhToanRepository;
import com.example.datn.repository.SanPhamChiTietRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class HoaDonLichSuTest {

    private final HoaDonRepository hoaDonRepository = mock(HoaDonRepository.class);
    private final ChiTietHoaDonRepository chiTietHoaDonRepository = mock(ChiTietHoaDonRepository.class);
    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository =
        mock(PhuongThucThanhToanRepository.class);
    private final PhieuGiamGiaRepository phieuGiamGiaRepository = mock(PhieuGiamGiaRepository.class);
    private final SanPhamChiTietRepository sanPhamChiTietRepository = mock(SanPhamChiTietRepository.class);
    private final LichSuHoaDonRepository lichSuHoaDonRepository = mock(LichSuHoaDonRepository.class);
    private final LichSuSuDungPhieuGiamGiaRepository lichSuSuDungRepository =
        mock(LichSuSuDungPhieuGiamGiaRepository.class);
    private final GHNService ghnService = mock(GHNService.class);

    private final HoaDonService service = new HoaDonService(
        hoaDonRepository,
        chiTietHoaDonRepository,
        khachHangRepository,
        nhanVienRepository,
        phuongThucThanhToanRepository,
        phieuGiamGiaRepository,
        sanPhamChiTietRepository,
        lichSuHoaDonRepository,
        lichSuSuDungRepository,
        ghnService
    );

    private static final Long HD_ID = 900L;

    private HoaDon hoaDon() {
        HoaDon h = new HoaDon();
        h.setId(HD_ID);
        h.setMaHoaDon("HDLS001");
        h.setTrangThai("DA_XAC_NHAN");
        h.setLoaiDon("ONLINE");
        h.setDaXoa(false);
        return h;
    }

    private LichSuHoaDon lichSu(Long id, String maTrangThai, String ghiChu) {
        LichSuHoaDon ls = new LichSuHoaDon();
        ls.setId(id);
        ls.setHoaDon(hoaDon());
        ls.setTrangThai(maTrangThai);
        ls.setThoiGian(LocalDateTime.of(2026, 10, 13, 9, 15));
        ls.setGhiChu(ghiChu);
        ls.setHanhDong("CHUYEN_TRANG_THAI");
        return ls;
    }

    @Test
    void lichSu_parseTrangThaiTruocTuGhiChu() {
        when(hoaDonRepository.existsById(HD_ID)).thenReturn(true);
        when(lichSuHoaDonRepository.findByIdHoaDonOrderByThoiGianDescIdDesc(anyLong()))
            .thenReturn(List.of(lichSu(1L, "DA_XAC_NHAN", "Chuyển từ Chờ xác nhận sang Đã xác nhận")));

        List<LichSuHoaDonDTO> ketQua = service.lichSuHoaDon(HD_ID);

        assertThat(ketQua).hasSize(1);
        LichSuHoaDonDTO d = ketQua.get(0);
        assertThat(d.getTrangThaiTruoc()).isEqualTo("Chờ xác nhận");
        assertThat(d.getTrangThaiSau()).isEqualTo("Đã xác nhận");
        assertThat(d.getMaTrangThaiSau()).isEqualTo("DA_XAC_NHAN");
        assertThat(d.getMaHoaDon()).isEqualTo("HDLS001");
        assertThat(d.getHanhDong()).isEqualTo("CHUYEN_TRANG_THAI");
        assertThat(d.getGhiChu()).isNull();
    }

    @Test
    void lichSu_gomGhiChuNguoiDungSauDauCham() {
        when(hoaDonRepository.existsById(HD_ID)).thenReturn(true);
        when(lichSuHoaDonRepository.findByIdHoaDonOrderByThoiGianDescIdDesc(anyLong()))
            .thenReturn(List.of(lichSu(2L, "DA_HOAN_THANH", "Chuyển từ Đã giao hàng sang Đã hoàn thành. Smoke POS done")));

        List<LichSuHoaDonDTO> ketQua = service.lichSuHoaDon(HD_ID);

        LichSuHoaDonDTO d = ketQua.get(0);
        assertThat(d.getTrangThaiTruoc()).isEqualTo("Đã giao hàng");
        assertThat(d.getTrangThaiSau()).isEqualTo("Đã hoàn thành");
        assertThat(d.getGhiChu()).isEqualTo("Smoke POS done");
    }

    @Test
    void lichSu_ghiChuKhongDinhDang_traTrangThaiTruocNull() {
        when(hoaDonRepository.existsById(HD_ID)).thenReturn(true);
        when(lichSuHoaDonRepository.findByIdHoaDonOrderByThoiGianDescIdDesc(anyLong()))
            .thenReturn(List.of(lichSu(3L, "DA_HUY", "ghi chú tùy ý không theo format")));

        List<LichSuHoaDonDTO> ketQua = service.lichSuHoaDon(HD_ID);

        LichSuHoaDonDTO d = ketQua.get(0);
        assertThat(d.getTrangThaiTruoc()).isNull();
        assertThat(d.getTrangThaiSau()).isEqualTo("Đã hủy");
        assertThat(d.getGhiChu()).isNull();
    }

    @Test
    void lichSu_ghiChuRong_traTrangThaiTruocNull() {
        when(hoaDonRepository.existsById(HD_ID)).thenReturn(true);
        when(lichSuHoaDonRepository.findByIdHoaDonOrderByThoiGianDescIdDesc(anyLong()))
            .thenReturn(List.of(lichSu(4L, "CHO_XAC_NHAN", null)));

        List<LichSuHoaDonDTO> ketQua = service.lichSuHoaDon(HD_ID);

        assertThat(ketQua.get(0).getTrangThaiTruoc()).isNull();
        assertThat(ketQua.get(0).getGhiChu()).isNull();
    }

    @Test
    void lichSu_hoaDonKhongTonTai_throws() {
        when(hoaDonRepository.existsById(HD_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.lichSuHoaDon(HD_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không tìm thấy hóa đơn");
    }

    @Test
    void lichSu_idNull_throws() {
        assertThatThrownBy(() -> service.lichSuHoaDon(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lichSu_nhieuBanGhi_moiNhatLenDau() {
        when(hoaDonRepository.existsById(HD_ID)).thenReturn(true);
        LichSuHoaDon cu = lichSu(10L, "DA_XAC_NHAN", "Chuyển từ Chờ xác nhận sang Đã xác nhận");
        cu.setThoiGian(LocalDateTime.of(2026, 10, 13, 9, 15));
        LichSuHoaDon moi = lichSu(11L, "CHO_GIAO_HANG", "Chuyển từ Đã xác nhận sang Chờ giao hàng");
        moi.setThoiGian(LocalDateTime.of(2026, 10, 13, 9, 20));
        when(lichSuHoaDonRepository.findByIdHoaDonOrderByThoiGianDescIdDesc(anyLong()))
            .thenReturn(List.of(moi, cu));

        List<LichSuHoaDonDTO> ketQua = service.lichSuHoaDon(HD_ID);

        assertThat(ketQua).hasSize(2);
        assertThat(ketQua.get(0).getTrangThaiSau()).isEqualTo("Chờ giao hàng");
        assertThat(ketQua.get(1).getTrangThaiSau()).isEqualTo("Đã xác nhận");
    }
}
