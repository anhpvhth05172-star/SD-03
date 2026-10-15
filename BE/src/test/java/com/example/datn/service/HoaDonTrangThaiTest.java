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

class HoaDonTrangThaiTest {

    private static final Long HD_ID = 500L;

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

    private HoaDon hoaDon(String trangThai, String maLoaiDon) {
        HoaDon h = new HoaDon();
        h.setId(HD_ID);
        h.setMaHoaDon("HD001");
        h.setTrangThai(trangThai);
        h.setLoaiDon(maLoaiDon);
        h.setDaXoa(false);
        return h;
    }

    private HoaDon online(String trangThai) {
        HoaDon h = hoaDon(trangThai, "ONLINE");
        h.setDiaChiNhanHang("12 Le Loi, Phuong Ben Nghe, Quan 1, TP Ho Chi Minh");
        h.setToProvinceId(202);
        h.setToDistrictId(1442);
        h.setToWardCode("20107");
        h.setPhiVanChuyen(new java.math.BigDecimal("36300"));
        return h;
    }

    private HoaDon onlineThieuDieuKienXacNhan(String trangThai, java.math.BigDecimal phi) {
        HoaDon h = hoaDon(trangThai, "ONLINE");
        if (phi != null) {
            h.setPhiVanChuyen(phi);
        }
        return h;
    }

    private HoaDon pos(String trangThai) {
        return hoaDon(trangThai, "TAI_QUAY");
    }

    private void coTheLuu(HoaDon h) {
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(h));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());
    }

    @Test
    void posChoXacNhanSangDaHoanThanh_HopLe() {
        coTheLuu(pos("CHO_XAC_NHAN"));

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "Đã hoàn thành", null);

        assertThat(dto.getTrangThai()).isEqualTo("Đã hoàn thành");
        verify(lichSuHoaDonRepository).save(any());
    }

    @Test
    void posKhongDuocChuyenSangDangGiaoHang() {
        coTheLuu(pos("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đang giao hàng", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Đang giao hàng");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void posKhongDuocChuyenSangDaXacNhan() {
        coTheLuu(pos("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Đã xác nhận");
    }

    @Test
    void posDaHoanThanhKhongQuayLai() {
        coTheLuu(pos("DA_HOAN_THANH"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Chờ xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Đã hoàn thành sang Chờ xác nhận");
    }

    @Test
    void posDaHuyKhongChuyenDuoc() {
        coTheLuu(pos("DA_HUY"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã hoàn thành", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Đã hủy sang Đã hoàn thành");
    }

    @Test
    void loaiDonLaTenTiengViet_VanDungQuyTacPos() {
        coTheLuu(hoaDon("CHO_XAC_NHAN", "Tại quầy"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đang giao hàng", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Đang giao hàng");
    }

    @Test
    void onlineXacNhanHopLe_GhiLichSuCoGhiChu() {
        coTheLuu(online("CHO_XAC_NHAN"));

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "Đã xác nhận", "Khách yêu cầu");

        assertThat(dto.getTrangThai()).isEqualTo("Đã xác nhận");
        ArgumentCaptor<LichSuHoaDon> captor = ArgumentCaptor.forClass(LichSuHoaDon.class);
        verify(lichSuHoaDonRepository).save(captor.capture());
        LichSuHoaDon lichSu = captor.getValue();
        assertThat(lichSu.getTrangThai()).isEqualTo("DA_XAC_NHAN");
        assertThat(lichSu.getHanhDong()).isEqualTo("CHUYEN_TRANG_THAI");
        assertThat(lichSu.getGhiChu()).contains("Chuyển từ Chờ xác nhận sang Đã xác nhận");
        assertThat(lichSu.getGhiChu()).contains("Khách yêu cầu");
    }

    @Test
    void onlineXacNhanKhongDiaChiNhanHang_BiChan() {
        coTheLuu(onlineThieuDieuKienXacNhan("CHO_XAC_NHAN", new java.math.BigDecimal("36300")));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chưa đủ địa chỉ nhận hàng/phí vận chuyển");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
        verify(lichSuHoaDonRepository, never()).save(any());
    }

    @Test
    void onlineXacNhanKhongMaPhuongXa_BiChan() {
        HoaDon h = online("CHO_XAC_NHAN");
        h.setToWardCode(null);
        coTheLuu(h);

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chưa đủ địa chỉ nhận hàng/phí vận chuyển");
    }

    @Test
    void onlineXacNhanPhiBang0_BiChan() {
        coTheLuu(onlineThieuDieuKienXacNhan("CHO_XAC_NHAN", java.math.BigDecimal.ZERO));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chưa đủ địa chỉ nhận hàng/phí vận chuyển");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void onlineKhongDuocBoQuaBuoc() {
        coTheLuu(online("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Chờ giao hàng", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Chờ giao hàng");
    }

    @Test
    void onlineChuyenTungBuoc_HopLe() {
        coTheLuu(online("DA_XAC_NHAN"));

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "Chờ giao hàng", null);

        assertThat(dto.getTrangThai()).isEqualTo("Chờ giao hàng");
        verify(lichSuHoaDonRepository).save(any());
    }

    @Test
    void onlineDaHoanThanhKhongQuayLaiGiaoHang() {
        coTheLuu(online("DA_HOAN_THANH"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đang giao hàng", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Đã hoàn thành sang Đang giao hàng");
    }

    @Test
    void daHuyKhongTiepTucGiaoHang() {
        coTheLuu(online("DA_HUY"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Đã hủy sang Đã xác nhận");
    }

    @Test
    void trangThaiMoiTrong_KhongDoiGiu() {
        coTheLuu(online("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Chờ xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trạng thái không thay đổi");
    }

    @Test
    void trangThaiKhongHopLe() {
        coTheLuu(online("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "ABC", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trạng thái không hợp lệ");
    }

    @Test
    void hoaDonDaXoaKhongDoiDuoc() {
        HoaDon h = online("CHO_XAC_NHAN");
        h.setDaXoa(true);
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(h));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không tìm thấy hóa đơn");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
        verify(lichSuHoaDonRepository, never()).save(any());
    }

    @Test
    void chuyenTrangThaiThatBai_KhongGhiLichSu() {
        coTheLuu(online("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Chờ giao hàng", null))
            .isInstanceOf(IllegalArgumentException.class);

        verify(lichSuHoaDonRepository, never()).save(any());
    }

    @Test
    void dongThoi_DungKhoaChongDong() {
        coTheLuu(online("CHO_XAC_NHAN"));

        service.doiTrangThai(HD_ID, "Đã xác nhận", null);

        verify(hoaDonRepository).findByIdForUpdate(HD_ID);
        verify(hoaDonRepository, never()).findById(anyLong());
    }

    @Test
    void ghiChuQua500KyTu_BiTuChoi() {
        coTheLuu(online("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", "x".repeat(501)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("500");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void ghiChuKyTuDieuKhien_BiTuChoi() {
        coTheLuu(online("CHO_XAC_NHAN"));

        String ghiChu = "abc" + (char) 7 + "def";
        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", ghiChu))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ký tự không hợp lệ");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void getCoMaTrangThaiVaTiepTheo_ONLINE() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(online("CHO_XAC_NHAN")));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.get(HD_ID);

        assertThat(dto.getMaTrangThai()).isEqualTo("CHO_XAC_NHAN");
        assertThat(dto.getTrangThaiTiepTheo()).containsExactly("Đã xác nhận", "Đã hủy");
    }

    @Test
    void getTiepTheoPos_TaiQuay() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(pos("CHO_XAC_NHAN")));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.get(HD_ID);

        assertThat(dto.getTrangThaiTiepTheo()).containsExactly("Đã hoàn thành");
    }

    @Test
    void getTiepTheoPos_DaHoanThanh_Rong() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(pos("DA_HOAN_THANH")));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.get(HD_ID);

        assertThat(dto.getTrangThaiTiepTheo()).isEmpty();
    }

    @Test
    void getTiepTheoOnline_DaHuy_Rong() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(online("DA_HUY")));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.get(HD_ID);

        assertThat(dto.getTrangThaiTiepTheo()).isEmpty();
    }

    @Test
    void getTiepTheoLoaiDonTiengViet_DungQuyTacPos() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(hoaDon("CHO_XAC_NHAN", "Tại quầy")));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.get(HD_ID);

        assertThat(dto.getTrangThaiTiepTheo()).containsExactly("Đã hoàn thành");
    }

    @Test
    void loaiDonNull_KhongDuocDoiTrangThai() {
        coTheLuu(hoaDon("CHO_XAC_NHAN", null));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("loại hóa đơn");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
        verify(lichSuHoaDonRepository, never()).save(any());
    }

    @Test
    void loaiDonKhongHopLe_KhongDuocDoiTrangThai() {
        coTheLuu(hoaDon("CHO_XAC_NHAN", "KHAC"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("loại hóa đơn");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void loaiDonNull_KhongCoTiepTheoTrongGet() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(hoaDon("CHO_XAC_NHAN", null)));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.get(HD_ID);

        assertThat(dto.getTrangThaiTiepTheo()).isEmpty();
    }

    @Test
    void onlineChuyenLui_BiTuChoi() {
        coTheLuu(online("DA_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Chờ xác nhận", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Đã xác nhận sang Chờ xác nhận");
    }

    @Test
    void onlineTuDaHoanThanhVeTruoc_BiTuChoi() {
        coTheLuu(online("DA_HOAN_THANH"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã giao hàng", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Đã hoàn thành sang Đã giao hàng");
    }

    @Test
    void onlineBocQua2Buoc_BiTuChoi() {
        coTheLuu(online("CHO_XAC_NHAN"));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đang giao hàng", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Đang giao hàng");
    }
}