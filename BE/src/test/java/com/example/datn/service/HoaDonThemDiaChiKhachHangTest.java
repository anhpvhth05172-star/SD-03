package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.HoaDon.ChiTietHoaDonRequest;
import com.example.datn.dto.HoaDon.HoaDonRequest;
import com.example.datn.entity.DiaChiKhachHang;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhuongThucThanhToan;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HoaDonThemDiaChiKhachHangTest {

    private static final Long KH_ID = 100L;
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

    @BeforeEach
    void chuanBi() {
        when(hoaDonRepository.existsByMaHoaDon(anyString())).thenReturn(false);

        KhachHang kh = new KhachHang();
        kh.setId(KH_ID);
        kh.setTenKhachHang("Khách A");
        kh.setSoDienThoai("0900000001");
        when(khachHangRepository.findById(KH_ID)).thenReturn(Optional.of(kh));

        NhanVien nv = new NhanVien();
        nv.setId(NV_ID);
        nv.setTenTaiKhoan("admin");
        nv.setTrangThai(true);
        when(nhanVienRepository.findById(NV_ID)).thenReturn(Optional.of(nv));

        PhuongThucThanhToan p = new PhuongThucThanhToan();
        p.setId(PTTT_ID);
        p.setMaPhuongThuc("TIEN_MAT");
        p.setTenPhuongThuc("Tiền mặt tại quầy");
        p.setTrangThai(true);
        when(phuongThucThanhToanRepository.findById(PTTT_ID)).thenReturn(Optional.of(p));

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
        when(chiTietHoaDonRepository.findByHoaDonId(org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(List.of());
        when(diaChiKhachHangRepository.countByKhachHangId(KH_ID)).thenReturn(0L);
        when(diaChiKhachHangRepository.findFirstByKhachHangIdAndMacDinhTrue(KH_ID))
            .thenReturn(Optional.empty());
        when(diaChiKhachHangRepository.save(any(DiaChiKhachHang.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private HoaDonRequest yeuCau(String diaChiNhanHang) {
        HoaDonRequest req = new HoaDonRequest();
        req.setMaHoaDon("HDADDR001");
        req.setLoaiDon("Tại quầy");
        req.setTrangThai("Chờ xác nhận");
        req.setIdPhuongThucThanhToan(PTTT_ID);
        req.setIdNhanVien(NV_ID);
        req.setPhiVanChuyen(BigDecimal.ZERO);
        req.setTenKhachHang("Khách A");
        req.setSoDienThoaiKhachHang("0912345678");
        req.setIdKhachHang(KH_ID);
        req.setDiaChiNhanHang(diaChiNhanHang);
        ChiTietHoaDonRequest dong = new ChiTietHoaDonRequest();
        dong.setIdSanPhamChiTiet(SPCT_ID);
        dong.setSoLuong(1);
        dong.setDonGia(new BigDecimal("1000000"));
        req.setChiTiet(List.of(dong));
        return req;
    }

    @Test
    void taoDonCoDiaChiNhanHangVaKhachChuaCoDiaChiThemVaoSo() {
        service.create(yeuCau("Số 10, Cầu Giấy, Hà Nội"));

        ArgumentCaptor<DiaChiKhachHang> cap = ArgumentCaptor.forClass(DiaChiKhachHang.class);
        verify(diaChiKhachHangRepository).save(cap.capture());
        DiaChiKhachHang dc = cap.getValue();
        assertThat(dc.getKhachHang().getId()).isEqualTo(KH_ID);
        assertThat(dc.getDiaChiCuThe()).isEqualTo("Số 10, Cầu Giấy, Hà Nội");
        assertThat(dc.getMacDinh()).isTrue();
        assertThat(dc.getTrangThai()).isTrue();
        assertThat(dc.getMaDiaChi()).isEqualTo("DC100-01");
    }

    @Test
    void khachDaCoDiaChiMacDinhKhongThemMoi() {
        DiaChiKhachHang cu = new DiaChiKhachHang();
        cu.setId(5L);
        cu.setDiaChiCuThe("Cũ");
        when(diaChiKhachHangRepository.findFirstByKhachHangIdAndMacDinhTrue(KH_ID))
            .thenReturn(Optional.of(cu));

        service.create(yeuCau("Số 10, Cầu Giấy, Hà Nội"));

        verify(diaChiKhachHangRepository, never()).save(any(DiaChiKhachHang.class));
    }

    @Test
    void donKhongCoDiaChiNhanHangKhongThemDiaChi() {
        service.create(yeuCau(null));

        verify(diaChiKhachHangRepository, never()).save(any(DiaChiKhachHang.class));
    }
}
