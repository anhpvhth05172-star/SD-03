package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.ChiTietHoaDonRequest;
import com.example.datn.dto.HoaDonDTO;
import com.example.datn.dto.HoaDonRequest;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.KhachHang;
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

/**
 * Test validation chinh cua module hoa don:
 * don gia, ton kho, san pham ngung ban, khach hang vo hieu hoa,
 * khoang trang, chinh sua/xoa/chuyen trang thai theo trang thai hien tai.
 */
class HoaDonValidationTest {

    private static final Long SPCT_ID = 9L;
    private static final Long KH_ID = 100L;
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
        when(nhanVienRepository.findById(NV_ID)).thenReturn(Optional.of(nhanVien()));
    }

    private NhanVien nhanVien() {
        NhanVien nv = new NhanVien();
        nv.setId(NV_ID);
        nv.setTenTaiKhoan("admin");
        nv.setTrangThai(true);
        return nv;
    }

    private SanPhamChiTiet spct(boolean trangThaiSpct, boolean trangThaiSanPham) {
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(SPCT_ID);
        spct.setMaChiTietSanPham("SPCT01");
        spct.setGiaBan(new BigDecimal("500000"));
        spct.setSoLuong(10);
        spct.setTrangThai(trangThaiSpct);
        SanPham sp = new SanPham();
        sp.setId(1L);
        sp.setMaSanPham("SP001");
        sp.setTenSanPham("Giay test");
        sp.setTrangThai(trangThaiSanPham);
        spct.setSanPham(sp);
        return spct;
    }

    private void chuanBi(SanPhamChiTiet spct) {
        when(hoaDonRepository.existsByMaHoaDon(anyString())).thenReturn(false);
        when(sanPhamChiTietRepository.findById(SPCT_ID)).thenReturn(Optional.of(spct));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> {
            HoaDon h = inv.getArgument(0);
            h.setId(HD_ID);
            return h;
        });
        when(chiTietHoaDonRepository.findByHoaDonId(anyLong())).thenReturn(List.of());
    }

    private HoaDonRequest yeuCau(BigDecimal donGia) {
        HoaDonRequest req = new HoaDonRequest();
        req.setMaHoaDon("HDVAL001");
        req.setLoaiDon("Tại quầy");
        req.setTrangThai("Chờ xác nhận");
        req.setIdPhuongThucThanhToan(PTTT_ID);
        req.setIdNhanVien(NV_ID);
        req.setPhiVanChuyen(new BigDecimal("50000"));
        req.setTenKhachHang("Nguyen Van A");
        req.setSoDienThoaiKhachHang("0912345678");
        ChiTietHoaDonRequest dong = new ChiTietHoaDonRequest();
        dong.setIdSanPhamChiTiet(SPCT_ID);
        dong.setSoLuong(1);
        dong.setDonGia(donGia);
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

    // ===== so / chi tiet =====

    @Test
    void donGiaBang0_BiTuChoi() {
        chuanBi(spct(true, true));

        assertThatThrownBy(() -> service.create(yeuCau(BigDecimal.ZERO)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Đơn giá phải lớn hơn 0");
    }

    @Test
    void donGiaNull_BiTuChoi() {
        chuanBi(spct(true, true));

        assertThatThrownBy(() -> service.create(yeuCau(null)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Đơn giá phải lớn hơn 0");
    }

    @Test
    void sanPhamChiTietNgungBan_BiTuChoi() {
        chuanBi(spct(false, true));

        assertThatThrownBy(() -> service.create(yeuCau(new BigDecimal("500000"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã ngừng bán");
    }

    @Test
    void sanPhamChaNgungBan_BiTuChoi() {
        chuanBi(spct(true, false));

        assertThatThrownBy(() -> service.create(yeuCau(new BigDecimal("500000"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("đã ngừng bán");
    }

    // ===== khach hang / khoang trang =====

    @Test
    void khachHangVoHieuHoa_BiTuChoi() {
        chuanBi(spct(true, true));
        KhachHang kh = new KhachHang();
        kh.setId(KH_ID);
        kh.setTenKhachHang("Khách test");
        kh.setTrangThai(false);
        when(khachHangRepository.findById(KH_ID)).thenReturn(Optional.of(kh));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setIdKhachHang(KH_ID);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Khách hàng đã bị vô hiệu hóa");
    }

    @Test
    void tenKhachHangChiKhoangTrang_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTenKhachHang("     ");
        req.setGhiChu("   ");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên khách hàng không được để trống");
    }

    @Test
    void tenKhachHangRong_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTenKhachHang("");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên khách hàng không được để trống");
    }

    @Test
    void ghiChuChiKhoangTrang_VanTaoDuoc() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setGhiChu("   ");

        HoaDonDTO dto = service.create(req);

        assertThat(dto.getId()).isEqualTo(HD_ID);
        ArgumentCaptor<HoaDon> cap = ArgumentCaptor.forClass(HoaDon.class);
        verify(hoaDonRepository).save(cap.capture());
        assertThat(cap.getValue().getGhiChu()).isNull();
    }

    // ===== text field: ten / SĐT / địa chỉ / ghi chú / mã =====

    @Test
    void tenKhachHangKyTuCam_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTenKhachHang("Nguyen Van A@#$");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên khách hàng chứa ký tự không hợp lệ");
    }

    @Test
    void tenKhachHangChuaSo_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTenKhachHang("Nguyen Van A1");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên khách hàng chứa ký tự không hợp lệ");
    }

    @Test
    void tenKhachHangChuaSoVaKyTuDacBiet_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTenKhachHang("Nguyen Van 2@");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên khách hàng chứa ký tự không hợp lệ");
    }

    @Test
    void tenKhachHangQuaDai_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTenKhachHang("Nguyen Van A ".repeat(20));

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên khách hàng quá độ dài tối đa");
    }

    @Test
    void soDienThoaiCoChu_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setSoDienThoaiKhachHang("090abc1234");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số điện thoại phải gồm 10 hoặc 11 chữ số");
    }

    @Test
    void soDienThoaiCong84_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setSoDienThoaiKhachHang("+84901234567");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số điện thoại phải gồm 10 hoặc 11 chữ số");
    }

    @Test
    void soDienThoaiRong_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setSoDienThoaiKhachHang("  ");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số điện thoại không được để trống");
    }

    @Test
    void soDienThoai9So_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setSoDienThoaiKhachHang("091234567");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số điện thoại phải gồm 10 hoặc 11 chữ số");
    }

    @Test
    void soDienThoai12So_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setSoDienThoaiKhachHang("091234567890");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số điện thoại phải gồm 10 hoặc 11 chữ số");
    }

    @Test
    void soDienThoai11So_TaoDuoc() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setSoDienThoaiKhachHang("09123456789");

        HoaDonDTO dto = service.create(req);

        assertThat(dto.getId()).isEqualTo(HD_ID);
    }

    @Test
    void diaChiKyTuCam_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setDiaChiNhanHang("123 Le Loi # Q1");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Địa chỉ nhận hàng chứa ký tự không hợp lệ");
    }

    @Test
    void ghiChuQuaDai_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setGhiChu("a".repeat(1001));

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ghi chú quá độ dài tối đa");
    }

    @Test
    void maQuaDai_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setMaHoaDon("X".repeat(51));

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mã hóa đơn quá độ dài tối đa");
    }

    @Test
    void textHopLe_TaoThanhCong() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTenKhachHang("Nguyễn Văn A");
        req.setSoDienThoaiKhachHang("0912345678");
        req.setDiaChiNhanHang("123/45 Lê Lợi, Q.1 - TP.HCM");
        req.setGhiChu("Ghi chú dòng chính ok.");

        HoaDonDTO dto = service.create(req);

        assertThat(dto.getId()).isEqualTo(HD_ID);
        ArgumentCaptor<HoaDon> cap = ArgumentCaptor.forClass(HoaDon.class);
        verify(hoaDonRepository).save(cap.capture());
        assertThat(cap.getValue().getTenKhachHang()).isEqualTo("Nguyễn Văn A");
        assertThat(cap.getValue().getDiaChiNhanHang()).isEqualTo("123/45 Lê Lợi, Q.1 - TP.HCM");
    }

    // ===== phuong thuc thanh toan =====

    @Test
    void phuongThucThanhToanRong_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setIdPhuongThucThanhToan(null);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phương thức thanh toán không được để trống");
    }

    @Test
    void phuongThucThanhToanKhongTonTai_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setIdPhuongThucThanhToan(999L);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phương thức thanh toán không tồn tại");
    }

    // ===== nhan vien / phi van chuyen / trang thai / dia chi online =====

    @Test
    void nhanVienRong_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setIdNhanVien(null);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Vui lòng chọn nhân viên");
    }

    @Test
    void nhanVienKhongTonTai_BiTuChoi() {
        chuanBi(spct(true, true));
        when(nhanVienRepository.findById(999L)).thenReturn(Optional.empty());
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setIdNhanVien(999L);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Nhân viên không tồn tại");
    }

    @Test
    void phiVanChuyenNull_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setPhiVanChuyen(null);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phí vận chuyển không được để trống");
    }

    @Test
    void phiVanChuyenAm_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setPhiVanChuyen(new BigDecimal("-1000"));

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phí vận chuyển không được âm");
    }

    @Test
    void phiVanChuyenBang0_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setPhiVanChuyen(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phí vận chuyển phải lớn hơn 1000");
    }

    @Test
    void phiVanChuyenBang1000_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setPhiVanChuyen(new BigDecimal("1000"));

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Phí vận chuyển phải lớn hơn 1000");
    }

    @Test
    void phiVanChuyen1001_TaoDuoc() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setPhiVanChuyen(new BigDecimal("1001"));

        HoaDonDTO dto = service.create(req);

        assertThat(dto.getId()).isEqualTo(HD_ID);
    }

    @Test
    void trangThaiRong_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTrangThai("  ");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trạng thái không được để trống");
    }

    @Test
    void trangThaiNgoaiDanhSach_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setTrangThai("Dang giao");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trạng thái không hợp lệ");
    }

    @Test
    void onlineKhongDiaChi_BiTuChoi() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setLoaiDon("Online");

        assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Đơn online phải có địa chỉ nhận hàng");
    }

    @Test
    void taiQuayKhongDiaChi_TaoDuoc() {
        chuanBi(spct(true, true));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setLoaiDon("Tại quầy");

        HoaDonDTO dto = service.create(req);

        assertThat(dto.getId()).isEqualTo(HD_ID);
    }

    // ===== sua / xoa / chuyen trang thai =====

    @Test
    void khongChoSuaHoaDonDaXacNhan() {
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(hoaDon("DA_XAC_NHAN")));

        assertThatThrownBy(() -> service.update(HD_ID, yeuCau(new BigDecimal("500000"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chỉ hóa đơn Chờ xác nhận mới được sửa");
    }

    @Test
    void suaHoaDonChoXacNhan_GiuMaVaTrangThaiCapNhatTruong() {
        chuanBi(spct(true, true));
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(hoaDon("CHO_XAC_NHAN")));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setMaHoaDon("HD001");
        req.setTenKhachHang("Nguyen Van B");
        req.setSoDienThoaiKhachHang("0987654321");

        HoaDonDTO dto = service.update(HD_ID, req);

        assertThat(dto.getId()).isEqualTo(HD_ID);
        assertThat(dto.getMaHoaDon()).isEqualTo("HD001");
        assertThat(dto.getTrangThai()).isEqualTo("Chờ xác nhận");
        assertThat(dto.getTenKhachHang()).isEqualTo("Nguyen Van B");
    }

    @Test
    void khongChoDoiMaHoaDon_KhiSua() {
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(hoaDon("CHO_XAC_NHAN")));
        HoaDonRequest req = yeuCau(new BigDecimal("500000"));
        req.setMaHoaDon("HDKHAC001");

        assertThatThrownBy(() -> service.update(HD_ID, req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được thay đổi mã hóa đơn");
    }

    @Test
    void khongChoSuaHoaDonDaXacNhan_KhongTaoMoi() {
        when(hoaDonRepository.findByIdForUpdate(HD_ID)).thenReturn(Optional.of(hoaDon("DA_XAC_NHAN")));

        assertThatThrownBy(() -> service.update(HD_ID, yeuCau(new BigDecimal("500000"))))
            .isInstanceOf(IllegalArgumentException.class);

        verify(hoaDonRepository, never()).save(any(HoaDon.class));
    }

    @Test
    void khongChoXoaHoaDonDaHoanThanh() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(hoaDon("DA_HOAN_THANH")));

        assertThatThrownBy(() -> service.delete(HD_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chỉ hóa đơn Chờ xác nhận hoặc Đã hủy mới được xóa");
    }

    @Test
    void chuyenTrangThaiSai_BiTuChoi() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(hoaDon("CHO_XAC_NHAN")));

        assertThatThrownBy(() -> service.doiTrangThai(HD_ID, "Đã giao hàng"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được chuyển từ Chờ xác nhận sang Đã giao hàng");
    }

    @Test
    void chuyenTrangThaiHopLe_ThanhCongVaGhiLichSu() {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(hoaDon("CHO_XAC_NHAN")));
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(inv -> inv.getArgument(0));

        HoaDonDTO dto = service.doiTrangThai(HD_ID, "Đã xác nhận");

        assertThat(dto.getTrangThai()).isEqualTo("Đã xác nhận");
        verify(lichSuHoaDonRepository).save(any());
    }
}
