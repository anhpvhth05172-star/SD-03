package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.config.VNPayConfig;
import com.example.datn.dto.CaLamViecDto;
import com.example.datn.dto.DotGiamGia.GiaKhuyenMai;
import com.example.datn.dto.HoaDon.ChiTietHoaDonRequest;
import com.example.datn.dto.HoaDon.HoaDonDTO;
import com.example.datn.dto.HoaDon.HoaDonRequest;
import com.example.datn.dto.VNPayCallbackResponse;
import com.example.datn.dto.VNPayPaymentResponse;
import com.example.datn.entity.ChiTietHoaDon;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhuongThucThanhToan;
import com.example.datn.entity.SanPham;
import com.example.datn.entity.SanPhamChiTiet;
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
import com.example.datn.service.impls.GiaService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class ReviewFixesUnitTest {

    private static final Long SPCT_ID = 9L;
    private static final Long HD_ID = 500L;
    private static final Long PTTT_ID = 1L;
    private static final Long NV_ID = 1L;

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
    private final GiaoCaService giaoCaService = mock(GiaoCaService.class);
    private final GiaService giaService = mock(GiaService.class);
    private final CauHinhPhiShipService cauHinhPhiShipService = mock(CauHinhPhiShipService.class);
    private final EmailService emailService = mock(EmailService.class);

    private final HoaDonService hoaDonService = new HoaDonService(
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
            ghnService);

    private VNPayConfig vnPayConfig;
    private VNPayService vnPayService;
    private HoaDonService mockHoaDonServiceForVnPay;

    private NhanVien nhanVien;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(hoaDonService, "giaoCaService", giaoCaService);
        ReflectionTestUtils.setField(hoaDonService, "giaService", giaService);
        ReflectionTestUtils.setField(hoaDonService, "cauHinhPhiShipService", cauHinhPhiShipService);
        ReflectionTestUtils.setField(hoaDonService, "emailService", emailService);

        PhuongThucThanhToan pttt = new PhuongThucThanhToan();
        pttt.setId(PTTT_ID);
        pttt.setMaPhuongThuc("TIEN_MAT");
        pttt.setTenPhuongThuc("Tiền mặt");
        pttt.setTrangThai(true);
        when(phuongThucThanhToanRepository.findById(PTTT_ID)).thenReturn(Optional.of(pttt));

        nhanVien = new NhanVien();
        nhanVien.setId(NV_ID);
        nhanVien.setTenTaiKhoan("staff");
        nhanVien.setTrangThai(true);
        when(nhanVienRepository.findById(NV_ID)).thenReturn(Optional.of(nhanVien));

        when(cauHinhPhiShipService.getFreeshipThreshold()).thenReturn(new BigDecimal("10000000"));
        when(giaoCaService.getDangMoOf(NV_ID))
                .thenReturn(CaLamViecDto.builder().id(1L).trangThai("DANG_MO").build());

        when(hoaDonRepository.existsByMaHoaDon(anyString())).thenReturn(false);
        when(hoaDonRepository.existsByMaHoaDonAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(hoaDonRepository.existsById(HD_ID)).thenReturn(true);
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> {
            HoaDon h = inv.getArgument(0);
            if (h.getId() == null) {
                h.setId(HD_ID);
            }
            return h;
        });

        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct(new BigDecimal("500000"))));
        when(sanPhamChiTietRepository.findAllByIdForUpdate(anyCollection()))
                .thenReturn(List.of(spct(new BigDecimal("500000"))));
        when(giaService.tinhGia(anyCollection()))
                .thenReturn(Map.of(SPCT_ID, new GiaKhuyenMai(new BigDecimal("500000"), null, new BigDecimal("500000"))));
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());
        when(lichSuHoaDonRepository.findByIdHoaDonOrderByThoiGianDescIdDesc(anyLong())).thenReturn(List.of());

        vnPayConfig = new VNPayConfig();
        ReflectionTestUtils.setField(vnPayConfig, "vnpPayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnPayConfig, "vnpReturnUrl", "http://localhost:5173/thanh-toan-vnpay");
        ReflectionTestUtils.setField(vnPayConfig, "vnpTmnCode", "CGXZLS0Z");
        ReflectionTestUtils.setField(vnPayConfig, "vnpHashSecret", "XNBCJFAKAZQSGTARRLGCHVZWCIOIGSHN");
        ReflectionTestUtils.setField(vnPayConfig, "vnpVersion", "2.1.0");
        ReflectionTestUtils.setField(vnPayConfig, "vnpCommand", "pay");

        mockHoaDonServiceForVnPay = mock(HoaDonService.class);
        vnPayService = new VNPayService(
                vnPayConfig,
                mockHoaDonServiceForVnPay,
                hoaDonRepository,
                lichSuHoaDonRepository,
                phuongThucThanhToanRepository,
                emailService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SanPhamChiTiet spct(BigDecimal giaBan) {
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(SPCT_ID);
        spct.setMaChiTietSanPham("SPCT01");
        spct.setGiaBan(giaBan);
        spct.setSoLuong(20);
        spct.setTrongLuong(new BigDecimal("800"));
        spct.setTrangThai(true);
        SanPham sp = new SanPham();
        sp.setId(1L);
        sp.setMaSanPham("SP001");
        sp.setTenSanPham("Giày test");
        sp.setTrangThai(true);
        spct.setSanPham(sp);
        return spct;
    }

    private HoaDonRequest buildReq(String loaiDon, BigDecimal donGia, BigDecimal phiShip) {
        HoaDonRequest req = new HoaDonRequest();
        req.setMaHoaDon("HDFIX001");
        req.setLoaiDon(loaiDon);
        req.setTrangThai("Chờ xác nhận");
        req.setIdPhuongThucThanhToan(PTTT_ID);
        req.setPhiVanChuyen(phiShip);
        req.setTenKhachHang("Nguyễn Văn A");
        req.setSoDienThoaiKhachHang("0912345678");
        req.setDiaChiNhanHang("12 Lê Lợi, Phường Bến Nghé, TP. Hồ Chí Minh");
        req.setToProvinceId(202);
        req.setToDistrictId(1442);
        req.setToWardCode("20107");
        ChiTietHoaDonRequest item = new ChiTietHoaDonRequest();
        item.setIdSanPhamChiTiet(SPCT_ID);
        item.setSoLuong(1);
        item.setDonGia(donGia);
        req.setChiTiet(List.of(item));
        return req;
    }

    @Test
    void onlineCreate_khongTuDongGanNhanVien() {
        SecurityContextHolder.clearContext();
        HoaDonRequest req = buildReq("Online", new BigDecimal("500000"), new BigDecimal("30000"));
        req.setIdNhanVien(null);

        HoaDonDTO dto = hoaDonService.create(req);

        assertThat(dto.getIdNhanVien()).isNull();
        verify(nhanVienRepository, never()).findActiveOrderAssignees();
    }

    @Test
    void doiTrangThai_onlineChuaCoNhanVien_ganNhanVienSession() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(NV_ID, "n/a", List.of(new SimpleGrantedAuthority("ROLE_NHAN_VIEN"))));

        HoaDon hd = new HoaDon();
        hd.setId(HD_ID);
        hd.setMaHoaDon("HD500");
        hd.setLoaiDon("ONLINE");
        hd.setTrangThai("CHO_XAC_NHAN");
        hd.setDaXoa(false);
        hd.setNhanVien(null);
        hd.setDiaChiNhanHang("12 Lê Lợi, TP. Hồ Chí Minh");
        hd.setToProvinceId(202);
        hd.setToDistrictId(1442);
        hd.setToWardCode("20107");
        hd.setPhiVanChuyen(new BigDecimal("30000"));
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(hd));

        HoaDonDTO dto = hoaDonService.doiTrangThai(HD_ID, "Đã xác nhận", null);

        assertThat(hd.getNhanVien()).isNotNull();
        assertThat(hd.getNhanVien().getId()).isEqualTo(NV_ID);
        assertThat(dto.getIdNhanVien()).isEqualTo(NV_ID);
    }

    @Test
    void taiQuayGiaoHang_phiVanChuyenThuCong_khongBiGhiDeKhiVuotFreeship() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(NV_ID, "n/a", List.of(new SimpleGrantedAuthority("ROLE_NHAN_VIEN"))));

        BigDecimal highPrice = new BigDecimal("12000000");
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct(highPrice)));
        when(sanPhamChiTietRepository.findAllByIdForUpdate(anyCollection())).thenReturn(List.of(spct(highPrice)));
        when(giaService.tinhGia(anyCollection()))
                .thenReturn(Map.of(SPCT_ID, new GiaKhuyenMai(highPrice, null, highPrice)));

        HoaDonRequest req = buildReq("Tại quầy (Giao hàng)", highPrice, new BigDecimal("45000"));
        req.setIdNhanVien(NV_ID);

        HoaDonDTO dto = hoaDonService.create(req);

        assertThat(dto.getPhiVanChuyen()).isEqualByComparingTo("45000");
    }

    @Test
    void vnpayRetryPayment_choThanhToan_taoLaiUrlThanhCong() {
        HoaDon hd = new HoaDon();
        hd.setId(HD_ID);
        hd.setMaHoaDon("HD500");
        hd.setTrangThai("CHO_THANH_TOAN");
        hd.setTienSauGiamGia(new BigDecimal("500000"));
        hd.setDaXoa(false);
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(hd));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-FORWARDED-FOR")).thenReturn("127.0.0.1");
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");

        VNPayPaymentResponse res = vnPayService.retryPaymentAndUrl(req, HD_ID);

        assertThat(res.getStatus()).isEqualTo("OK");
        assertThat(res.getPaymentUrl()).contains("vnp_TxnRef=HD500_");
        assertThat(res.getQrUrl()).contains("addInfo=HD500");
    }

    @Test
    void vnpayRetryPayment_khongPhaiChoThanhToan_biTuChoi() {
        HoaDon hd = new HoaDon();
        hd.setId(HD_ID);
        hd.setMaHoaDon("HD500");
        hd.setTrangThai("CHO_XAC_NHAN");
        hd.setTienSauGiamGia(new BigDecimal("500000"));
        hd.setDaXoa(false);
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(hd));

        assertThatThrownBy(() -> vnPayService.retryPaymentAndUrl(mock(HttpServletRequest.class), HD_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chờ thanh toán");
    }

    @Test
    void vnpayXacNhanQR_canhBaoTonKho_traVeOutOfStockTrue() {
        HoaDon before = new HoaDon();
        before.setId(HD_ID);
        before.setMaHoaDon("HD500");
        before.setTrangThai("CHO_THANH_TOAN");
        before.setTienSauGiamGia(new BigDecimal("500000"));

        HoaDon after = new HoaDon();
        after.setId(HD_ID);
        after.setMaHoaDon("HD500");
        after.setTrangThai("CHO_XAC_NHAN");
        after.setTienSauGiamGia(new BigDecimal("500000"));
        after.setGhiChu("[CẢNH BÁO TỒN KHO: Sản phẩm A tạm hết hàng]");

        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(before), Optional.of(after));
        when(mockHoaDonServiceForVnPay.xacNhanThanhToanVNPay(HD_ID)).thenReturn(new HoaDonDTO());

        VNPayCallbackResponse resp = vnPayService.xacNhanThanhToanQR(HD_ID);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.isOutOfStock()).isTrue();
        assertThat(resp.getMessage()).contains("tạm hết hàng");
    }

    @Test
    void update_thayDoiSanPhamTrongDon_biTuChoi() {
        HoaDon hd = new HoaDon();
        hd.setId(HD_ID);
        hd.setMaHoaDon("HDFIX001");
        hd.setLoaiDon("ONLINE");
        hd.setTrangThai("CHO_XAC_NHAN");
        hd.setDaXoa(false);
        hd.setPhiVanChuyen(new BigDecimal("30000"));
        hd.setTienSauGiamGia(new BigDecimal("530000"));

        ChiTietHoaDon cu = new ChiTietHoaDon();
        cu.setHoaDon(hd);
        cu.setSanPhamChiTiet(spct(new BigDecimal("500000")));
        cu.setSoLuong(1);
        cu.setDonGia(new BigDecimal("500000"));

        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(hd));
        when(chiTietHoaDonRepository.findByHoaDonId(HD_ID)).thenReturn(List.of(cu));

        HoaDonRequest req = buildReq("Online", new BigDecimal("500000"), new BigDecimal("30000"));
        req.getChiTiet().get(0).setSoLuong(2);

        assertThatThrownBy(() -> hoaDonService.update(HD_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không được chỉnh sửa danh sách sản phẩm trong hóa đơn");
    }

    @Test
    void thongKe_phanLoaiMaPhuongThucThanhToanSauKhiBoHinhThucThanhToan() {
        assertThat(ThongKeService.daThanhToan("DA_XAC_NHAN", "ONLINE", "PT01", false)).isFalse();
        assertThat(ThongKeService.daThanhToan("DA_XAC_NHAN", "ONLINE", "PT11", false)).isFalse();
        assertThat(ThongKeService.daThanhToan("DA_XAC_NHAN", "ONLINE", "PT02", false)).isTrue();
        assertThat(ThongKeService.daThanhToan("DA_XAC_NHAN", "ONLINE", "PT04", false)).isTrue();
        assertThat(ThongKeService.daThanhToan("DA_HOAN_THANH", "ONLINE", "PT11", false)).isTrue();
    }
}
