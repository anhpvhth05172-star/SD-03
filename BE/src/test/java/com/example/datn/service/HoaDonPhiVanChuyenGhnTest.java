package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.datn.dto.HoaDon.ChiTietHoaDonRequest;
import com.example.datn.dto.HoaDon.HoaDonDTO;
import com.example.datn.dto.HoaDon.HoaDonRequest;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhuongThucThanhToan;
import com.example.datn.entity.SanPham;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.ChiTietHoaDonRepository;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.LichSuHoaDonRepository;
import com.example.datn.repository.LichSuSuDungPhieuGiamGiaRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.repository.PhuongThucThanhToanRepository;
import com.example.datn.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HoaDonPhiVanChuyenGhnTest {

    private static final Long SPCT_ID = 9L;
    private static final Long HD_ID = 500L;
    private static final Long PTTT_ID = 1L;
    private static final Long NV_ID = 1L;

    private final HoaDonRepository hoaDonRepository = mock(HoaDonRepository.class);
    private final ChiTietHoaDonRepository chiTietHoaDonRepository =
        mock(ChiTietHoaDonRepository.class);
    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository =
        mock(PhuongThucThanhToanRepository.class);
    private final PhieuGiamGiaRepository phieuGiamGiaRepository =
        mock(PhieuGiamGiaRepository.class);
    private final SanPhamChiTietRepository sanPhamChiTietRepository =
        mock(SanPhamChiTietRepository.class);
    private final LichSuHoaDonRepository lichSuHoaDonRepository =
        mock(LichSuHoaDonRepository.class);
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

    @BeforeEach
    void chuanBi() {
        PhuongThucThanhToan p = new PhuongThucThanhToan();
        p.setId(PTTT_ID);
        p.setMaPhuongThuc("TIEN_MAT");
        p.setTenPhuongThuc("Tiền mặt");
        p.setTrangThai(true);
        when(phuongThucThanhToanRepository.findById(PTTT_ID)).thenReturn(Optional.of(p));

        NhanVien nv = new NhanVien();
        nv.setId(NV_ID);
        nv.setTenTaiKhoan("admin");
        nv.setTrangThai(true);
        when(nhanVienRepository.findById(NV_ID)).thenReturn(Optional.of(nv));

        when(hoaDonRepository.existsByMaHoaDon(anyString())).thenReturn(false);
        when(hoaDonRepository.existsByMaHoaDonAndIdNot(anyString(), any(Long.class))).thenReturn(false);
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> {
            HoaDon h = inv.getArgument(0);
            h.setId(HD_ID);
            return h;
        });
        when(chiTietHoaDonRepository.findByHoaDonId(any(Long.class))).thenReturn(List.of());
    }

    private SanPhamChiTiet spct() {
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(SPCT_ID);
        spct.setMaChiTietSanPham("SPCT01");
        spct.setGiaBan(new BigDecimal("500000"));
        spct.setSoLuong(10);
        spct.setTrongLuong(new BigDecimal("800"));
        spct.setTrangThai(true);
        SanPham sp = new SanPham();
        sp.setId(1L);
        sp.setMaSanPham("SP001");
        sp.setTenSanPham("Giay test");
        sp.setTrangThai(true);
        spct.setSanPham(sp);
        return spct;
    }

    private HoaDonRequest yeuCau(String loaiDon) {
        HoaDonRequest req = new HoaDonRequest();
        req.setMaHoaDon("HDGHN001");
        req.setLoaiDon(loaiDon);
        req.setTrangThai("Chờ xác nhận");
        req.setIdPhuongThucThanhToan(PTTT_ID);
        req.setIdNhanVien(NV_ID);
        req.setPhiVanChuyen(new BigDecimal("50000"));
        req.setTenKhachHang("Nguyen Van A");
        req.setSoDienThoaiKhachHang("0912345678");
        if ("Online".equals(loaiDon)) {
            req.setDiaChiNhanHang("12 Le Loi, Phuong Ben Nghe, Quan 1, TP Ho Chi Minh");
            req.setToProvinceId(202);
            req.setToDistrictId(1442);
            req.setToWardCode("20107");
        }
        ChiTietHoaDonRequest dong = new ChiTietHoaDonRequest();
        dong.setIdSanPhamChiTiet(SPCT_ID);
        dong.setSoLuong(1);
        dong.setDonGia(new BigDecimal("500000"));
        req.setChiTiet(List.of(dong));
        return req;
    }

    private HoaDon hoaDon(String trangThai) {
        HoaDon h = new HoaDon();
        h.setId(HD_ID);
        h.setMaHoaDon("HD001");
        h.setTrangThai(trangThai);
        h.setDaXoa(false);
        return h;
    }

    @Test
    void onlineThieuTinhThanh_BiTuChoi() {
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct()));
        HoaDonRequest req = yeuCau("Online");
        req.setToProvinceId(null);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("phải chọn tỉnh/thành phố");

        verifyNoInteractions(ghnService);
        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void onlineThieuQuanHuyen_BiTuChoi() {
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct()));
        HoaDonRequest req = yeuCau("Online");
        req.setToDistrictId(null);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("phải chọn quận/huyện");

        verifyNoInteractions(ghnService);
    }

    @Test
    void onlineThieuPhuongXa_BiTuChoi() {
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct()));
        HoaDonRequest req = yeuCau("Online");
        req.setToWardCode("  ");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("phải chọn phường/xã");

        verifyNoInteractions(ghnService);
    }

    @Test
    void onlineMaPhuongXaQuaDai_BiTuChoi() {
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct()));
        HoaDonRequest req = yeuCau("Online");
        req.setToWardCode("123456789012345678901");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mã phường/xã không hợp lệ");
    }

    @Test
    void online_TinhLaiPhiTuGhn_BoPhiClientGuiLen() {
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct()));
        when(ghnService.tinhPhi(any(ShippingFeeRequest.class))).thenReturn(36300);
        HoaDonRequest req = yeuCau("Online");
        req.setPhiVanChuyen(new BigDecimal("999000"));

        HoaDonDTO dto = service.create(req);

        ArgumentCaptor<ShippingFeeRequest> phiCap =
            ArgumentCaptor.forClass(ShippingFeeRequest.class);
        verify(ghnService).tinhPhi(phiCap.capture());
        assertThat(phiCap.getValue().getToDistrictId()).isEqualTo(1442);
        assertThat(phiCap.getValue().getToWardCode()).isEqualTo("20107");
        assertThat(phiCap.getValue().getItems()).hasSize(1);
        assertThat(phiCap.getValue().getItems().get(0).idSanPhamChiTiet()).isEqualTo(SPCT_ID);
        assertThat(phiCap.getValue().getItems().get(0).soLuong()).isEqualTo(1);

        ArgumentCaptor<HoaDon> hoaDonCap = ArgumentCaptor.forClass(HoaDon.class);
        verify(hoaDonRepository).save(hoaDonCap.capture());
        assertThat(hoaDonCap.getValue().getPhiVanChuyen()).isEqualByComparingTo("36300");
        assertThat(hoaDonCap.getValue().getToProvinceId()).isEqualTo(202);
        assertThat(hoaDonCap.getValue().getToDistrictId()).isEqualTo(1442);
        assertThat(hoaDonCap.getValue().getToWardCode()).isEqualTo("20107");
        assertThat(dto.getPhiVanChuyen()).isEqualByComparingTo("36300");
    }

    @Test
    void onlineGhnLoi_KhongTaoDuocDonKhongGiPhiGia() {
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct()));
        when(ghnService.tinhPhi(any(ShippingFeeRequest.class)))
            .thenThrow(new IllegalArgumentException("GHN không tính được phí vận chuyển: District not found"));

        assertThatThrownBy(() -> service.create(yeuCau("Online")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("District not found");

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void taiQuay_KhongGoiGHN_VaPhiBiDongBang0() {
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct()));

        HoaDonDTO dto = service.create(yeuCau("Tại quầy"));

        assertThat(dto.getId()).isEqualTo(HD_ID);
        assertThat(dto.getPhiVanChuyen()).isEqualByComparingTo("0");
        verifyNoInteractions(ghnService);
    }

    @Test
    void updateOnline_TinhLaiPhiVaLuuMaDiaChi() {
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct()));
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(hoaDon("CHO_XAC_NHAN")));
        when(ghnService.tinhPhi(any(ShippingFeeRequest.class))).thenReturn(25000);
        HoaDonRequest req = yeuCau("Online");
        req.setMaHoaDon("HD001");

        HoaDonDTO dto = service.update(HD_ID, req);

        verify(ghnService).tinhPhi(any(ShippingFeeRequest.class));
        assertThat(dto.getPhiVanChuyen()).isEqualByComparingTo("25000");
        assertThat(dto.getToDistrictId()).isEqualTo(1442);
        assertThat(dto.getToWardCode()).isEqualTo("20107");
        assertThat(dto.getToProvinceId()).isEqualTo(202);
    }
}
