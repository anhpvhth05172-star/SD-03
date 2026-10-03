package com.example.datn.service;

import com.example.datn.dto.ChiTietHoaDonDTO;
import com.example.datn.dto.ChiTietHoaDonRequest;
import com.example.datn.dto.FormDataResponse;
import com.example.datn.dto.HoaDonDTO;
import com.example.datn.dto.HoaDonRequest;
import com.example.datn.dto.PageResponse;
import com.example.datn.entity.ChiTietHoaDon;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.entity.PhuongThucThanhToan;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.ChiTietHoaDonRepository;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.repository.PhuongThucThanhToanRepository;
import com.example.datn.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoaDonService {

    private static final String[][] TRANG_THAI_MAP = {
        {"CHO_XAC_NHAN", "Chờ xác nhận"},
        {"DA_XAC_NHAN", "Đã xác nhận"},
        {"CHO_GIAO_HANG", "Chờ giao hàng"},
        {"DANG_GIAO_HANG", "Đang giao hàng"},
        {"DA_GIAO_HANG", "Đã giao hàng"},
        {"DA_HOAN_THANH", "Đã hoàn thành"},
        {"DA_HUY", "Đã hủy"},
        {"DA_HOAN_TIEN", "Đã hoàn tiền"},
    };

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    @Transactional(readOnly = true)
    public PageResponse<HoaDonDTO> list(
        String ma, LocalDate tuNgay, LocalDate denNgay, String loaiDon, String trangThai, int page, int size
    ) {
        LocalDateTime tu = tuNgay != null ? tuNgay.atStartOfDay() : null;
        LocalDateTime den = denNgay != null ? denNgay.atTime(LocalTime.MAX) : null;
        String maLoaiDon = null;
        if (!isBlank(loaiDon)) {
            try {
                maLoaiDon = toMaLoaiDon(loaiDon);
            } catch (IllegalArgumentException e) {
                maLoaiDon = loaiDon.trim();
            }
        }
        String maTrangThai = null;
        if (!isBlank(trangThai)) {
            try {
                maTrangThai = toMaTrangThai(trangThai);
            } catch (IllegalArgumentException e) {
                maTrangThai = trangThai.trim();
            }
        }
        var result = hoaDonRepository.findByFilters(
            blankToNull(ma), tu, den, maLoaiDon, maTrangThai,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 10), Sort.by(Sort.Direction.DESC, "ngayTao"))
        );
        return PageResponse.from(result.map(this::toDTO));
    }

    @Transactional(readOnly = true)
    public HoaDonDTO get(Long id) {
        return toFullDTO(findOrThrow(id));
    }

    @Transactional
    public HoaDonDTO create(HoaDonRequest req) {
        validateHeader(req, null);
        HoaDon hoaDon = new HoaDon();
        fillHeader(hoaDon, req);
        List<DongTinh> lines = tinhToanVaLuu(hoaDon, req);
        hoaDon = hoaDonRepository.save(hoaDon);
        luuCacDong(hoaDon, lines, req);
        return toFullDTO(hoaDon);
    }

    @Transactional
    public HoaDonDTO update(Long id, HoaDonRequest req) {
        validateHeader(req, id);
        HoaDon hoaDon = findOrThrow(id);
        fillHeader(hoaDon, req);
        List<DongTinh> lines = tinhToanVaLuu(hoaDon, req);
        chiTietHoaDonRepository.deleteByHoaDonId(hoaDon.getId());
        luuCacDong(hoaDon, lines, req);
        return toFullDTO(hoaDon);
    }

    @Transactional
    public void delete(Long id) {
        HoaDon hoaDon = findOrThrow(id);
        chiTietHoaDonRepository.deleteByHoaDonId(hoaDon.getId());
        hoaDonRepository.delete(hoaDon);
    }

    @Transactional(readOnly = true)
    public FormDataResponse formData() {
        List<FormDataResponse.KhachHangOption> khachHangs = khachHangRepository.findAll().stream()
            .map(k -> new FormDataResponse.KhachHangOption(k.getId(), k.getTenKhachHang(), k.getSoDienThoai()))
            .toList();
        List<FormDataResponse.NhanVienOption> nhanViens = nhanVienRepository.findAll().stream()
            .map(n -> new FormDataResponse.NhanVienOption(n.getId(), n.getTenTaiKhoan()))
            .toList();
        List<FormDataResponse.PhuongThucOption> phuongThucs = phuongThucThanhToanRepository.findAll().stream()
            .map(p -> new FormDataResponse.PhuongThucOption(p.getId(), p.getTenPhuongThuc()))
            .toList();
        List<FormDataResponse.PhieuGiamGiaOption> phieuGiamGias = phieuGiamGiaRepository.findAll().stream()
            .map(p -> new FormDataResponse.PhieuGiamGiaOption(
                p.getId(), p.getMaPhieuGiamGia(), p.getTenPhieuGiamGia(), p.getLoaiGiamGia(),
                p.getGiaTriGiam(), p.getGiamToiDa(), p.getHoaDonToiThieu(),
                p.getNgayBatDau(), p.getNgayKetThuc(), p.getSoLuong(), p.getSoLuongDaSuDung()
            ))
            .toList();
        List<FormDataResponse.SanPhamChiTietOption> spcts = sanPhamChiTietRepository.findAll().stream()
            .map(s -> new FormDataResponse.SanPhamChiTietOption(
                s.getId(), s.getMaChiTietSanPham(),
                s.getSanPham() != null ? s.getSanPham().getTenSanPham() : "",
                s.getKichCo() != null ? s.getKichCo().getTenKichCo() : "",
                s.getMauSac() != null ? s.getMauSac().getTenMau() : "",
                s.getGiaBan(), s.getSoLuong()
            ))
            .toList();
        return new FormDataResponse(khachHangs, nhanViens, phuongThucs, phieuGiamGias, spcts);
    }

    // ===== private =====

    private void validateHeader(HoaDonRequest req, Long excludeId) {
        if (req == null || isBlank(req.getMaHoaDon())) {
            throw new IllegalArgumentException("Mã hóa đơn không được để trống");
        }
        String ma = req.getMaHoaDon().trim();
        boolean maTrung = excludeId == null
            ? hoaDonRepository.existsByMaHoaDon(ma)
            : hoaDonRepository.existsByMaHoaDonAndIdNot(ma, excludeId);
        if (maTrung) {
            throw new IllegalArgumentException("Mã hóa đơn đã tồn tại");
        }
        if (isBlank(req.getLoaiDon())) {
            throw new IllegalArgumentException("Loại đơn không hợp lệ (Tại quầy / Online)");
        }
        toMaLoaiDon(req.getLoaiDon());
        if (!isBlank(req.getTrangThai())) {
            toMaTrangThai(req.getTrangThai());
        }
        if (req.getPhiVanChuyen() != null && req.getPhiVanChuyen().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí vận chuyển không được âm");
        }
        if (req.getChiTiet() == null || req.getChiTiet().isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn phải có ít nhất 1 sản phẩm");
        }
    }

    private void fillHeader(HoaDon hoaDon, HoaDonRequest req) {
        hoaDon.setMaHoaDon(req.getMaHoaDon().trim());
        hoaDon.setLoaiDon(toMaLoaiDon(req.getLoaiDon()));
        hoaDon.setPhiVanChuyen(req.getPhiVanChuyen() != null ? req.getPhiVanChuyen() : BigDecimal.ZERO);
        hoaDon.setTenKhachHang(req.getTenKhachHang());
        hoaDon.setSoDienThoaiKhachHang(req.getSoDienThoaiKhachHang());
        hoaDon.setDiaChiNhanHang(req.getDiaChiNhanHang());
        hoaDon.setTrangThai(toMaTrangThai(req.getTrangThai()));
        hoaDon.setGhiChu(req.getGhiChu());
        hoaDon.setNguoiCapNhat(isBlank(req.getNguoiCapNhat()) ? "admin" : req.getNguoiCapNhat().trim());
        hoaDon.setNgayCapNhat(LocalDateTime.now());

        hoaDon.setKhachHang(req.getIdKhachHang() != null
            ? khachHangRepository.findById(req.getIdKhachHang())
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"))
            : null);
        hoaDon.setNhanVien(req.getIdNhanVien() != null
            ? nhanVienRepository.findById(req.getIdNhanVien())
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại"))
            : null);
        hoaDon.setPhuongThucThanhToan(req.getIdPhuongThucThanhToan() != null
            ? phuongThucThanhToanRepository.findById(req.getIdPhuongThucThanhToan())
                .orElseThrow(() -> new IllegalArgumentException("Phương thức thanh toán không tồn tại"))
            : null);
        hoaDon.setPhieuGiamGia(req.getIdPhieuGiamGia() != null
            ? phieuGiamGiaRepository.findById(req.getIdPhieuGiamGia())
                .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"))
            : null);

        if (hoaDon.getKhachHang() != null) {
            if (isBlank(hoaDon.getTenKhachHang())) {
                hoaDon.setTenKhachHang(hoaDon.getKhachHang().getTenKhachHang());
            }
            if (isBlank(hoaDon.getSoDienThoaiKhachHang())) {
                hoaDon.setSoDienThoaiKhachHang(hoaDon.getKhachHang().getSoDienThoai());
            }
        }
    }

    private record DongTinh(SanPhamChiTiet spct, ChiTietHoaDonRequest req, BigDecimal thanhTien) {}

    private List<DongTinh> tinhToanVaLuu(HoaDon hoaDon, HoaDonRequest req) {
        List<DongTinh> dong = new ArrayList<>();
        for (ChiTietHoaDonRequest d : req.getChiTiet()) {
            if (d.getIdSanPhamChiTiet() == null) {
                throw new IllegalArgumentException("Từng dòng chi tiết phải chọn sản phẩm");
            }
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(d.getIdSanPhamChiTiet())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chi tiết không tồn tại"));
            if (d.getSoLuong() == null || d.getSoLuong() < 1) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            }
            if (spct.getSoLuong() != null && d.getSoLuong() > spct.getSoLuong()) {
                throw new IllegalArgumentException(
                    "Sản phẩm " + spct.getMaChiTietSanPham() + " chỉ còn " + spct.getSoLuong() + " trong kho"
                );
            }
            BigDecimal donGia = d.getDonGia() != null ? d.getDonGia() : spct.getGiaBan();
            if (donGia == null || donGia.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Đơn giá không hợp lệ");
            }
            BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(d.getSoLuong()));
            dong.add(new DongTinh(spct, d, thanhTien));
        }

        BigDecimal tong = dong.stream()
            .map(DongTinh::thanhTien)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(hoaDon.getPhiVanChuyen());
        hoaDon.setTongTien(tong);
        BigDecimal giam = tinhGiamGia(hoaDon);
        hoaDon.setTienSauGiamGia(tong.subtract(giam));
        return dong;
    }

    private BigDecimal tinhGiamGia(HoaDon hoaDon) {
        PhieuGiamGia pgg = hoaDon.getPhieuGiamGia();
        if (pgg == null) {
            return BigDecimal.ZERO;
        }
        LocalDateTime now = LocalDateTime.now();
        if (pgg.getNgayBatDau() != null && now.isBefore(pgg.getNgayBatDau())) {
            throw new IllegalArgumentException("Phiếu giảm giá chưa đến ngày sử dụng");
        }
        if (pgg.getNgayKetThuc() != null && now.isAfter(pgg.getNgayKetThuc())) {
            throw new IllegalArgumentException("Phiếu giảm giá đã hết hạn");
        }
        if (pgg.getSoLuong() != null && pgg.getSoLuongDaSuDung() != null
            && pgg.getSoLuongDaSuDung() >= pgg.getSoLuong()) {
            throw new IllegalArgumentException("Phiếu giảm giá đã hết lượt sử dụng");
        }
        if (pgg.getHoaDonToiThieu() != null && hoaDon.getTongTien().compareTo(pgg.getHoaDonToiThieu()) < 0) {
            throw new IllegalArgumentException(
                "Hóa đơn phải từ " + pgg.getHoaDonToiThieu().toPlainString() + "đ mới dùng phiếu này"
            );
        }
        BigDecimal giam;
        if ("PERCENT".equalsIgnoreCase(pgg.getLoaiGiamGia())) {
            giam = hoaDon.getTongTien()
                .multiply(pgg.getGiaTriGiam())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        } else {
            giam = pgg.getGiaTriGiam();
        }
        if (pgg.getGiamToiDa() != null && pgg.getGiamToiDa().compareTo(BigDecimal.ZERO) > 0
            && giam.compareTo(pgg.getGiamToiDa()) > 0) {
            giam = pgg.getGiamToiDa();
        }
        if (giam.compareTo(hoaDon.getTongTien()) > 0) {
            giam = hoaDon.getTongTien();
        }
        return giam.max(BigDecimal.ZERO);
    }

    private void luuCacDong(HoaDon hoaDon, List<DongTinh> dong, HoaDonRequest req) {
        List<ChiTietHoaDon> entities = new ArrayList<>();
        for (DongTinh d : dong) {
            ChiTietHoaDon e = new ChiTietHoaDon();
            e.setHoaDon(hoaDon);
            e.setSanPhamChiTiet(d.spct());
            e.setSoLuong(d.req().getSoLuong());
            e.setDonGia(d.req().getDonGia() != null ? d.req().getDonGia() : d.spct().getGiaBan());
            e.setThanhTien(d.thanhTien());
            e.setGhiChu(d.req().getGhiChu());
            e.setTrangThai(d.req().getTrangThai() == null || d.req().getTrangThai());
            entities.add(e);
        }
        chiTietHoaDonRepository.saveAll(entities);
    }

    private HoaDon findOrThrow(Long id) {
        return hoaDonRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn #" + id));
    }

    private HoaDonDTO toDTO(HoaDon h) {
        return HoaDonDTO.builder()
            .id(h.getId())
            .maHoaDon(h.getMaHoaDon())
            .loaiDon(toTenLoaiDon(h.getLoaiDon()))
            .phiVanChuyen(h.getPhiVanChuyen())
            .tongTien(h.getTongTien())
            .tienSauGiamGia(h.getTienSauGiamGia())
            .tenKhachHang(h.getTenKhachHang())
            .soDienThoaiKhachHang(h.getSoDienThoaiKhachHang())
            .diaChiNhanHang(h.getDiaChiNhanHang())
            .ngayTao(h.getNgayTao())
            .ngayCapNhat(h.getNgayCapNhat())
            .nguoiCapNhat(h.getNguoiCapNhat())
            .trangThai(toTenTrangThai(h.getTrangThai()))
            .ghiChu(h.getGhiChu())
            .idKhachHang(h.getKhachHang() != null ? h.getKhachHang().getId() : null)
            .idNhanVien(h.getNhanVien() != null ? h.getNhanVien().getId() : null)
            .tenNhanVien(h.getNhanVien() != null ? h.getNhanVien().getTenTaiKhoan() : null)
            .idPhuongThucThanhToan(h.getPhuongThucThanhToan() != null ? h.getPhuongThucThanhToan().getId() : null)
            .tenPhuongThucThanhToan(h.getPhuongThucThanhToan() != null ? h.getPhuongThucThanhToan().getTenPhuongThuc() : null)
            .idPhieuGiamGia(h.getPhieuGiamGia() != null ? h.getPhieuGiamGia().getId() : null)
            .tenPhieuGiamGia(h.getPhieuGiamGia() != null ? h.getPhieuGiamGia().getTenPhieuGiamGia() : null)
            .build();
    }

    private HoaDonDTO toFullDTO(HoaDon h) {
        HoaDonDTO dto = toDTO(h);
        List<ChiTietHoaDonDTO> lines = chiTietHoaDonRepository.findByHoaDonId(h.getId()).stream()
            .map(this::toLineDTO)
            .toList();
        dto.setChiTiet(lines);
        return dto;
    }

    private ChiTietHoaDonDTO toLineDTO(ChiTietHoaDon e) {
        SanPhamChiTiet s = e.getSanPhamChiTiet();
        return ChiTietHoaDonDTO.builder()
            .id(e.getId())
            .idSanPhamChiTiet(s != null ? s.getId() : null)
            .maSanPhamChiTiet(s != null ? s.getMaChiTietSanPham() : null)
            .tenSanPham(s != null && s.getSanPham() != null ? s.getSanPham().getTenSanPham() : null)
            .tenKichCo(s != null && s.getKichCo() != null ? s.getKichCo().getTenKichCo() : null)
            .tenMau(s != null && s.getMauSac() != null ? s.getMauSac().getTenMau() : null)
            .soLuong(e.getSoLuong())
            .donGia(e.getDonGia())
            .thanhTien(e.getThanhTien())
            .ghiChu(e.getGhiChu())
            .trangThai(e.getTrangThai())
            .build();
    }

    private static String toMaTrangThai(String trangThai) {
        if (isBlank(trangThai)) {
            return "CHO_XAC_NHAN";
        }
        String s = trangThai.trim();
        for (String[] pair : TRANG_THAI_MAP) {
            if (pair[0].equalsIgnoreCase(s) || pair[1].equals(s)) {
                return pair[0];
            }
        }
        throw new IllegalArgumentException("Trạng thái không hợp lệ");
    }

    private static String toTenTrangThai(String ma) {
        if (ma == null) {
            return null;
        }
        for (String[] pair : TRANG_THAI_MAP) {
            if (pair[0].equalsIgnoreCase(ma)) {
                return pair[1];
            }
        }
        return ma;
    }

    private static String toMaLoaiDon(String loaiDon) {
        if (isBlank(loaiDon)) {
            throw new IllegalArgumentException("Loại đơn không hợp lệ (Tại quầy / Online)");
        }
        String s = loaiDon.trim();
        if (s.equalsIgnoreCase("TAI_QUAY") || s.equals("Tại quầy")) {
            return "TAI_QUAY";
        }
        if (s.equalsIgnoreCase("ONLINE") || s.equals("Online")) {
            return "ONLINE";
        }
        throw new IllegalArgumentException("Loại đơn không hợp lệ (Tại quầy / Online)");
    }

    private static String toTenLoaiDon(String ma) {
        if (ma == null) {
            return null;
        }
        if (ma.equalsIgnoreCase("TAI_QUAY")) {
            return "Tại quầy";
        }
        if (ma.equalsIgnoreCase("ONLINE")) {
            return "Online";
        }
        return ma;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
