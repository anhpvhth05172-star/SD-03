package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.HoaDon.HoaDonDTO;
import com.example.datn.entity.HoaDon;
import com.example.datn.repository.ChiTietHoaDonRepository;
import com.example.datn.repository.DiaChiKhachHangRepository;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.LichSuHoaDonRepository;
import com.example.datn.repository.LichSuSuDungPhieuGiamGiaRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.repository.PhuongThucThanhToanRepository;
import com.example.datn.repository.SanPhamChiTietRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class HoaDonHuyDonTest {

    private static final Long HD_ID = 800L;
    private static final String THONG_BAO_HUY = "Đơn hàng đã được xác nhận, không thể hủy đơn.";

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
    private final DiaChiKhachHangRepository diaChiKhachHangRepository =
        mock(DiaChiKhachHangRepository.class);
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
        diaChiKhachHangRepository,
        ghnService
    );

    private HoaDon online(String trangThai) {
        HoaDon h = new HoaDon();
        h.setId(HD_ID);
        h.setMaHoaDon("HD800");
        h.setTrangThai(trangThai);
        h.setLoaiDon("ONLINE");
        h.setDaXoa(false);
        h.setDiaChiNhanHang("12 Le Loi, Phuong Ben Nghe, Quan 1, TP Ho Chi Minh");
        h.setToProvinceId(202);
        h.setToDistrictId(1442);
        h.setToWardCode("20107");
        h.setPhiVanChuyen(new java.math.BigDecimal("36300"));
        return h;
    }

    private HoaDon pos(String trangThai) {
        HoaDon h = new HoaDon();
        h.setId(HD_ID);
        h.setMaHoaDon("HD800");
        h.setTrangThai(trangThai);
        h.setLoaiDon("TAI_QUAY");
        h.setDaXoa(false);
        return h;
    }

    private void coTheLuu(HoaDon h) {
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(h));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());
    }

    private void khongDuocHuy(String trangThaiHienTai) {
        coTheLuu(online(trangThaiHienTai));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã hủy", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(THONG_BAO_HUY);

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
        verify(lichSuHoaDonRepository, never()).save(any());
    }

    @Test
    void onlineDaXacNhanKhongDuocHuy_CoThongBaoRieng() {
        khongDuocHuy("DA_XAC_NHAN");
    }

    @Test
    void onlineChoGiaoHangKhongDuocHuy_CoThongBaoRieng() {
        khongDuocHuy("CHO_GIAO_HANG");
    }

    @Test
    void onlineDangGiaoHangKhongDuocHuy_CoThongBaoRieng() {
        khongDuocHuy("DANG_GIAO_HANG");
    }

    @Test
    void onlineChoXacNhanVanHuyDuoc() {
        coTheLuu(online("CHO_XAC_NHAN"));

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "DA_HUY", "Khách yêu cầu hủy");

        assertThat(dto.getMaTrangThai()).isEqualTo("DA_HUY");
        assertThat(dto.getTrangThai()).isEqualTo("Đã hủy");
        verify(lichSuHoaDonRepository).save(any());
    }

    @Test
    void posKhongDuocHuy_GiuNguyenQuyTacHienTai() {
        coTheLuu(pos("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã hủy", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Đã hủy");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
        verify(lichSuHoaDonRepository, never()).save(any());
    }
}
