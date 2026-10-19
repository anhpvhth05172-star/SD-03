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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HoaDonGiaoHangThatBaiTest {

    private static final Long HD_ID = 700L;

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

    private HoaDon online(String trangThai) {
        HoaDon h = new HoaDon();
        h.setId(HD_ID);
        h.setMaHoaDon("HD700");
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
        h.setMaHoaDon("HD700");
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

    @Test
    void onlineDangGiaoHangSangGiaoHangThatBai_HopLe() {
        coTheLuu(online("DANG_GIAO_HANG"));

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "Giao hàng thất bại", null);

        assertThat(dto.getMaTrangThai()).isEqualTo("GIAO_HANG_THAT_BAI");
        assertThat(dto.getTrangThai()).isEqualTo("Giao hàng thất bại");

        ArgumentCaptor<LichSuHoaDon> captor = ArgumentCaptor.forClass(LichSuHoaDon.class);
        verify(lichSuHoaDonRepository).save(captor.capture());
        LichSuHoaDon lichSu = captor.getValue();
        assertThat(lichSu.getHanhDong()).isEqualTo("CHUYEN_TRANG_THAI");
        assertThat(lichSu.getGhiChu())
            .contains("Chuyển từ Đang giao hàng sang Giao hàng thất bại");
    }

    @Test
    void onlineChoXacNhanSangGiaoHangThatBai_BiTuChoi() {
        coTheLuu(online("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Giao hàng thất bại", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Giao hàng thất bại");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
        verify(lichSuHoaDonRepository, never()).save(any());
    }

    @Test
    void onlineGiaoHangThatBaiKhongDuocSangDaGiaoHang() {
        coTheLuu(online("GIAO_HANG_THAT_BAI"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã giao hàng", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Giao hàng thất bại sang Đã giao hàng");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
        verify(lichSuHoaDonRepository, never()).save(any());
    }

    @Test
    void onlineGiaoHangThatBaiSangDangGiaoHang_HopLe() {
        coTheLuu(online("GIAO_HANG_THAT_BAI"));

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "Đang giao hàng", null);

        assertThat(dto.getMaTrangThai()).isEqualTo("DANG_GIAO_HANG");
        assertThat(dto.getTrangThai()).isEqualTo("Đang giao hàng");

        ArgumentCaptor<LichSuHoaDon> captor = ArgumentCaptor.forClass(LichSuHoaDon.class);
        verify(lichSuHoaDonRepository).save(captor.capture());
        assertThat(captor.getValue().getGhiChu())
            .contains("Chuyển từ Giao hàng thất bại sang Đang giao hàng");
    }

    @Test
    void onlineGiaoHangThatBaiChiDuocSangDangGiaoHang() {
        String[] dauSauKhongHopLe = {"Đã xác nhận", "Chờ giao hàng", "Đã giao hàng", "Đã hoàn thành", "Đã hủy"};
        for (String dauSau : dauSauKhongHopLe) {
            coTheLuu(online("GIAO_HANG_THAT_BAI"));

            String messageKyVong = "Đã hủy".equals(dauSau)
                ? "không thể hủy đơn"
                : "Không được chuyển từ Giao hàng thất bại sang " + dauSau;
            assertThatThrownBy(() -> service.doiTrangThai(HD_ID, dauSau, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(messageKyVong);

            verify(hoaDonRepository, never()).save(any(HoaDon.class));
            verify(lichSuHoaDonRepository, never()).save(any());
        }
    }

    @Test
    void getTiepTheoOnline_GiaoHangThatBai_ChiCoDangGiaoHang() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(online("GIAO_HANG_THAT_BAI")));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.get(HD_ID);

        assertThat(dto.getTrangThaiTiepTheo()).containsExactly("Đang giao hàng");
    }

    @Test
    void onlineLanThatBaiThuHai_VanChoThuLai() {
        coTheLuu(online("DANG_GIAO_HANG"));
        when(lichSuHoaDonRepository.countByHoaDonIdAndTrangThai(HD_ID, "GIAO_HANG_THAT_BAI")).thenReturn(1L);

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "Giao hàng thất bại", null);

        assertThat(dto.getMaTrangThai()).isEqualTo("GIAO_HANG_THAT_BAI");
        assertThat(dto.getTrangThai()).isEqualTo("Giao hàng thất bại");
        assertThat(dto.getTrangThaiTiepTheo()).containsExactly("Đang giao hàng");
        verify(lichSuHoaDonRepository, org.mockito.Mockito.times(1)).save(any(LichSuHoaDon.class));
    }

    @Test
    void onlineLanThatBaiThuBa_TuDongHuyDon() {
        HoaDon h = online("DANG_GIAO_HANG");
        coTheLuu(h);
        when(lichSuHoaDonRepository.countByHoaDonIdAndTrangThai(HD_ID, "GIAO_HANG_THAT_BAI")).thenReturn(2L);

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "Giao hàng thất bại", null);

        assertThat(dto.getMaTrangThai()).isEqualTo("DA_HUY");
        assertThat(dto.getTrangThai()).isEqualTo("Đã hủy");
        assertThat(dto.getTrangThaiTiepTheo()).isEmpty();
        assertThat(h.getTrangThai()).isEqualTo("DA_HUY");

        ArgumentCaptor<LichSuHoaDon> captor = ArgumentCaptor.forClass(LichSuHoaDon.class);
        verify(lichSuHoaDonRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<LichSuHoaDon> lichSuList = captor.getAllValues();
        assertThat(lichSuList.get(0).getTrangThai()).isEqualTo("GIAO_HANG_THAT_BAI");
        assertThat(lichSuList.get(1).getTrangThai()).isEqualTo("DA_HUY");
        assertThat(lichSuList.get(1).getGhiChu())
            .contains("Chuyển từ Giao hàng thất bại sang Đã hủy")
            .contains("Tự động hủy sau 3 lần giao hàng thất bại");
    }

    @Test
    void posKhongDuocChuyenSangGiaoHangThatBai() {
        coTheLuu(pos("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Giao hàng thất bại", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Giao hàng thất bại");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
        verify(lichSuHoaDonRepository, never()).save(any());
    }

    @Test
    void getTiepTheoOnline_DangGiaoHang_CoGiaoHangThatBai() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(online("DANG_GIAO_HANG")));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.get(HD_ID);

        assertThat(dto.getMaTrangThai()).isEqualTo("DANG_GIAO_HANG");
        assertThat(dto.getTrangThai()).isEqualTo("Đang giao hàng");
        assertThat(dto.getTrangThaiTiepTheo())
            .containsExactly("Đã giao hàng", "Giao hàng thất bại");
    }
}
