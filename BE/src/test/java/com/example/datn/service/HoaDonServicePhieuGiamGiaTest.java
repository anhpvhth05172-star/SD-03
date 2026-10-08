package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.ChiTietHoaDonRequest;
import com.example.datn.dto.HoaDonDTO;
import com.example.datn.dto.HoaDonRequest;
import com.example.datn.entity.DotGiamGia;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.LichSuSuDungPhieuGiamGia;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.entity.PhuongThucThanhToan;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HoaDonServicePhieuGiamGiaTest {

    private static final Long KH_A = 100L;
    private static final Long KH_B = 200L;
    private static final Long PGG_ID = 1L;
    private static final Long SPCT_ID = 9L;
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

    private final HoaDonService service = new HoaDonService(
        hoaDonRepository,
        chiTietHoaDonRepository,
        khachHangRepository,
        nhanVienRepository,
        phuongThucThanhToanRepository,
        phieuGiamGiaRepository,
        sanPhamChiTietRepository,
        lichSuHoaDonRepository,
        lichSuSuDungRepository
    );

    private PhieuGiamGia phieu(String loai, String giaTri, int gioiHan) {
        PhieuGiamGia p = new PhieuGiamGia();
        p.setId(PGG_ID);
        p.setMaPhieuGiamGia("GG001");
        p.setTenPhieuGiamGia("Phiếu giảm giá 1");
        p.setLoaiGiamGia(loai);
        p.setGiaTriGiam(new BigDecimal(giaTri));
        p.setNgayBatDau(LocalDateTime.now().minusDays(1));
        p.setNgayKetThuc(LocalDateTime.now().plusDays(30));
        p.setSoLuong(100);
        p.setSoLuongDaSuDung(0);
        p.setTrangThai(true);
        p.setGioiHanMoiTaiKhoan(gioiHan);
        return p;
    }

    private KhachHang khachHang(Long id, String ten) {
        KhachHang kh = new KhachHang();
        kh.setId(id);
        kh.setTenKhachHang(ten);
        kh.setSoDienThoai("0900000001");
        return kh;
    }

    private PhuongThucThanhToan phuongThucThanhToan() {
        PhuongThucThanhToan p = new PhuongThucThanhToan();
        p.setId(PTTT_ID);
        p.setMaPhuongThuc("TIEN_MAT");
        p.setTenPhuongThuc("Tiền mặt tại quầy");
        p.setTrangThai(true);
        return p;
    }

    @BeforeEach
    void datPhuongThucThanhToan() {
        when(phuongThucThanhToanRepository.findById(PTTT_ID))
            .thenReturn(Optional.of(phuongThucThanhToan()));
        NhanVien nv = new NhanVien();
        nv.setId(NV_ID);
        nv.setTenTaiKhoan("admin");
        nv.setTrangThai(true);
        when(nhanVienRepository.findById(NV_ID)).thenReturn(Optional.of(nv));
    }

    private void chuanBi(PhieuGiamGia pgg) {
        when(hoaDonRepository.existsByMaHoaDon(anyString())).thenReturn(false);
        if (pgg != null) {
            when(phieuGiamGiaRepository.findByIdForUpdate(PGG_ID)).thenReturn(Optional.of(pgg));
            when(phieuGiamGiaRepository.findById(PGG_ID)).thenReturn(Optional.of(pgg));
        }
        when(khachHangRepository.findById(KH_A)).thenReturn(Optional.of(khachHang(KH_A, "Khách A")));
        when(khachHangRepository.findById(KH_B)).thenReturn(Optional.of(khachHang(KH_B, "Khách B")));
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(SPCT_ID);
        spct.setMaChiTietSanPham("SPCT01");
        spct.setGiaBan(new BigDecimal("1000000"));
        spct.setSoLuong(10);
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> {
            HoaDon h = inv.getArgument(0);
            h.setId(10L);
            return h;
        });
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());
        when(lichSuSuDungRepository.countByKhachHangIdAndPhieuGiamGiaId(anyLong(), anyLong()))
            .thenReturn(0L);
    }

    private HoaDonRequest yeuCau(Long idPhieu, Long idKhachHang) {
        HoaDonRequest req = new HoaDonRequest();
        req.setMaHoaDon("HDTEST001");
        req.setLoaiDon("Tại quầy");
        req.setTrangThai("Chờ xác nhận");
        req.setIdPhuongThucThanhToan(PTTT_ID);
        req.setIdNhanVien(NV_ID);
        req.setPhiVanChuyen(new BigDecimal("50000"));
        req.setTenKhachHang("Nguyen Van A");
        req.setSoDienThoaiKhachHang("0912345678");
        req.setIdKhachHang(idKhachHang);
        req.setIdPhieuGiamGia(idPhieu);
        ChiTietHoaDonRequest dong = new ChiTietHoaDonRequest();
        dong.setIdSanPhamChiTiet(SPCT_ID);
        dong.setSoLuong(1);
        dong.setDonGia(new BigDecimal("1000000"));
        req.setChiTiet(List.of(dong));
        return req;
    }

    @Test
    void test5_Tong1000000_Giam20PhanTram_TienGiamVaSnapshotDuocLuu() {
        chuanBi(phieu("PERCENT", "20", 1));

        HoaDonDTO dto = service.create(yeuCau(PGG_ID, KH_A));

        assertThat(dto.getTongTien()).isEqualByComparingTo("1050000");
        assertThat(dto.getTienGiam()).isEqualByComparingTo("210000");
        assertThat(dto.getTienSauGiamGia()).isEqualByComparingTo("840000");
        assertThat(dto.getTenGiamGiaUngDung()).isEqualTo("Giảm 20%");

        ArgumentCaptor<HoaDon> capHd = ArgumentCaptor.forClass(HoaDon.class);
        verify(hoaDonRepository).save(capHd.capture());
        assertThat(capHd.getValue().getTienGiam()).isEqualByComparingTo("210000");
        assertThat(capHd.getValue().getTienSauGiamGia()).isEqualByComparingTo("840000");
        assertThat(capHd.getValue().getTenGiamGiaUngDung()).isEqualTo("Giảm 20%");

        ArgumentCaptor<LichSuSuDungPhieuGiamGia> capLs = ArgumentCaptor.forClass(LichSuSuDungPhieuGiamGia.class);
        verify(lichSuSuDungRepository).save(capLs.capture());
        assertThat(capLs.getValue().getKhachHang().getId()).isEqualTo(KH_A);
        assertThat(capLs.getValue().getPhieuGiamGia().getId()).isEqualTo(PGG_ID);
        assertThat(capLs.getValue().getHoaDon().getId()).isEqualTo(10L);
        assertThat(capLs.getValue().getSoTienGiam()).isEqualByComparingTo("210000");
        assertThat(capLs.getValue().getThoiGian()).isNotNull();
    }

    @Test
    void test6_DotGiamGiaKhongDuocCongVaoPhieu() {
        PhieuGiamGia pgg = phieu("PERCENT", "20", 1);
        DotGiamGia dot = new DotGiamGia();
        dot.setId(5L);
        dot.setMaDotGiamGia("DOT001");
        dot.setTenDotGiamGia("Sale tháng 10");
        dot.setPhanTramGiam(new BigDecimal("10"));
        pgg.setDotGiamGia(dot);
        chuanBi(pgg);

        HoaDonDTO dto = service.create(yeuCau(PGG_ID, KH_A));

        assertThat(dto.getTienGiam()).isEqualByComparingTo("210000");
        assertThat(dto.getTienSauGiamGia()).isEqualByComparingTo("840000");
        assertThat(dto.getIdDotGiamGia()).isEqualTo(5L);
        assertThat(dto.getTenDotGiamGia()).isEqualTo("Sale tháng 10");
    }

    @Test
    void test2_GioiHan2Lan_A1Pass_A2Pass_A3Fail_B1Pass() {
        chuanBi(phieu("PERCENT", "20", 2));
        when(lichSuSuDungRepository.countByKhachHangIdAndPhieuGiamGiaId(KH_A, PGG_ID))
            .thenReturn(0L, 1L, 2L);

        assertThatCode(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .doesNotThrowAnyException();
        assertThatCode(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đủ 2 lần");

        assertThatCode(() -> service.create(yeuCau(PGG_ID, KH_B)))
            .doesNotThrowAnyException();
    }

    @Test
    void phieuKhongTonTai_BiTuChoi() {
        when(hoaDonRepository.existsByMaHoaDon(anyString())).thenReturn(false);
        when(phieuGiamGiaRepository.findByIdForUpdate(PGG_ID)).thenReturn(Optional.empty());
        when(khachHangRepository.findById(KH_A)).thenReturn(Optional.of(khachHang(KH_A, "Khách A")));

        assertThatThrownBy(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không tồn tại");
    }

    @Test
    void phieuKhongHoatDong_BiTuChoi() {
        PhieuGiamGia pgg = phieu("PERCENT", "20", 1);
        pgg.setTrangThai(false);
        chuanBi(pgg);

        assertThatThrownBy(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không hoạt động");
    }

    @Test
    void suDungPhieuKhongChonKhachHang_BiTuChoi() {
        chuanBi(phieu("PERCENT", "20", 1));

        assertThatThrownBy(() -> service.create(yeuCau(PGG_ID, null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chọn khách hàng");
    }

    @Test
    void phieuHetHan_BiTuChoi() {
        PhieuGiamGia pgg = phieu("PERCENT", "20", 1);
        pgg.setNgayKetThuc(LocalDateTime.now().minusDays(1));
        chuanBi(pgg);

        assertThatThrownBy(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hết hạn");
    }

    @Test
    void phieuChuaDenHan_BiTuChoi() {
        PhieuGiamGia pgg = phieu("PERCENT", "20", 1);
        pgg.setNgayBatDau(LocalDateTime.now().plusDays(1));
        chuanBi(pgg);

        assertThatThrownBy(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chưa đến ngày sử dụng");
    }

    @Test
    void khongDatHoaDonToiThieu_BiTuChoi() {
        PhieuGiamGia pgg = phieu("PERCENT", "20", 1);
        pgg.setHoaDonToiThieu(new BigDecimal("5000000"));
        chuanBi(pgg);

        assertThatThrownBy(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Hóa đơn phải từ");
    }

    @Test
    void hetLuongToanCuc_BiTuChoi() {
        PhieuGiamGia pgg = phieu("PERCENT", "20", 1);
        pgg.setSoLuong(1);
        pgg.setSoLuongDaSuDung(1);
        chuanBi(pgg);

        assertThatThrownBy(() -> service.create(yeuCau(PGG_ID, KH_A)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã hết lượt sử dụng");
    }

    @Test
    void tienAMount_Giam60000() {
        chuanBi(phieu("AMOUNT", "60000", 1));

        HoaDonDTO dto = service.create(yeuCau(PGG_ID, KH_A));

        assertThat(dto.getTienGiam()).isEqualByComparingTo("60000");
        assertThat(dto.getTienSauGiamGia()).isEqualByComparingTo("990000");
        assertThat(dto.getTenGiamGiaUngDung()).isEqualTo("Giảm 60.000đ");
    }

    @Test
    void khongChonPhieu_KhongGhiLichSu() {
        when(hoaDonRepository.existsByMaHoaDon(anyString())).thenReturn(false);
        when(khachHangRepository.findById(KH_A)).thenReturn(Optional.of(khachHang(KH_A, "Khách A")));
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(SPCT_ID);
        spct.setMaChiTietSanPham("SPCT01");
        spct.setGiaBan(new BigDecimal("1000000"));
        spct.setSoLuong(10);
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> {
            HoaDon h = inv.getArgument(0);
            h.setId(11L);
            return h;
        });
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());

        HoaDonDTO dto = service.create(yeuCau(null, KH_A));

        assertThat(dto.getTienGiam()).isEqualByComparingTo("0");
        assertThat(dto.getTienSauGiamGia()).isEqualByComparingTo("1050000");
        assertThat(dto.getTenGiamGiaUngDung()).isNull();
        verify(lichSuSuDungRepository, org.mockito.Mockito.never())
            .save(any(LichSuSuDungPhieuGiamGia.class));
    }

    @Test
    void trungSanPhamNhieuDong_BatLoi() {
        chuanBi(null);

        HoaDonRequest req = yeuCau(null, KH_A);
        ChiTietHoaDonRequest dong2 = new ChiTietHoaDonRequest();
        dong2.setIdSanPhamChiTiet(SPCT_ID);
        dong2.setSoLuong(2);
        dong2.setDonGia(new BigDecimal("1000000"));
        req.setChiTiet(List.of(req.getChiTiet().get(0), dong2));

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bị trùng ở nhiều dòng");
    }
}
