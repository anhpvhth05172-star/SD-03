package com.example.datn.service;

import com.example.datn.dto.DiaChiKhachHangDTO;
import com.example.datn.dto.KhachHangDTO;
import com.example.datn.dto.KhachHangRequest;
import com.example.datn.dto.KhachHangThongKeDTO;
import com.example.datn.dto.PageResponse;
import com.example.datn.entity.DiaChiKhachHang;
import com.example.datn.entity.KhachHang;
import com.example.datn.repository.DiaChiKhachHangRepository;
import com.example.datn.repository.KhachHangRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KhachHangService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String MAT_KHAU_MAC_DINH = "123456";
    private static final String TRANG_THAI_DANG_HOAT_DONG = "Đang hoạt động";
    private static final String TRANG_THAI_KHOA = "Đã khóa";
    private static final BigDecimal BAC_MIN = new BigDecimal("5000000");
    private static final BigDecimal VANG_MIN = new BigDecimal("10000000");
    private static final BigDecimal KIM_CUONG_MIN = new BigDecimal("20000000");
    private static final String[][] HANG_MAP = {
        {"KIM_CUONG", "Kim cương"},
        {"VANG", "Vàng"},
        {"BAC", "Bạc"},
        {"THUONG", "Thường"}
    };
    private static final String[][] GIOI_TINH_MAP = {
        {"NAM", "Nam"},
        {"NU", "Nữ"},
        {"KHAC", "Khác"}
    };
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern SO_DIEN_THAI_PATTERN = Pattern.compile("^(0|\\+84)\\d{8,10}$");

    private final KhachHangRepository khachHangRepository;
    private final DiaChiKhachHangRepository diaChiKhachHangRepository;

    @Transactional(readOnly = true)
    public PageResponse<KhachHangDTO> list(
        String keyword, String hangThanhVien, String trangThai,
        LocalDate tuNgay, LocalDate denNgay, int page, int size
    ) {
        BigDecimal[] hangRange = hangRange(hangThanhVien);
        var result = khachHangRepository.findByFilters(
            blankToNull(keyword), toTrangThai(trangThai),
            tuNgay != null ? tuNgay.atStartOfDay() : null,
            denNgay != null ? denNgay.atTime(LocalTime.MAX) : null,
            hangRange[0], hangRange[1],
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "ngayTao")
            )
        );
        Map<Long, KhachHangThongKeDTO> thongKe = thongKeTheo(result.getContent());
        return PageResponse.from(result.map(k -> toDTO(k, thongKe)));
    }

    @Transactional(readOnly = true)
    public KhachHangDTO get(Long id) {
        return toFullDTO(findOrThrow(id));
    }

    @Transactional
    public KhachHangDTO create(KhachHangRequest req) {
        validate(req, null);
        KhachHang khachHang = new KhachHang();
        fill(khachHang, req, true);
        khachHang = khachHangRepository.save(khachHang);
        luuDiaChiMacDinh(khachHang, req);
        return toFullDTO(khachHang);
    }

    @Transactional
    public KhachHangDTO update(Long id, KhachHangRequest req) {
        validate(req, id);
        KhachHang khachHang = findOrThrow(id);
        fill(khachHang, req, false);
        khachHang = khachHangRepository.save(khachHang);
        luuDiaChiMacDinh(khachHang, req);
        return toFullDTO(khachHang);
    }

    @Transactional
    public void delete(Long id) {
        KhachHang khachHang = findOrThrow(id);
        khachHang.setTrangThai(false);
        khachHang.setNgayCapNhat(LocalDateTime.now());
        khachHang.setNguoiCapNhat("admin");
        khachHangRepository.save(khachHang);
    }

    // ===== private =====

    private void validate(KhachHangRequest req, Long excludeId) {
        if (req == null || isBlank(req.getTenKhachHang())) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }
        String ten = req.getTenKhachHang().trim();
        if (!isBlank(req.getTenTaiKhoan())) {
            String tk = req.getTenTaiKhoan().trim();
            boolean trung = excludeId == null
                ? khachHangRepository.existsByTenTaiKhoan(tk)
                : khachHangRepository.existsByTenTaiKhoanAndIdNot(tk, excludeId);
            if (trung) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
            }
        }
        if (!isBlank(req.getMaKhachHang())) {
            String ma = req.getMaKhachHang().trim();
            boolean trungMa = excludeId == null
                ? khachHangRepository.existsByMaKhachHang(ma)
                : khachHangRepository.existsByMaKhachHangAndIdNot(ma, excludeId);
            if (trungMa) {
                throw new IllegalArgumentException("Mã khách hàng đã tồn tại");
            }
        }
        if (!isBlank(req.getEmail()) && !EMAIL_PATTERN.matcher(req.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        if (!isBlank(req.getSoDienThoai()) && !SO_DIEN_THAI_PATTERN.matcher(req.getSoDienThoai().trim()).matches()) {
            throw new IllegalArgumentException("Số điện thoại chỉ gồm 9-11 chữ số và bắt đầu bằng 0 hoặc +84");
        }
        if (req.getNgaySinh() != null && req.getNgaySinh().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
        }
        if (!isBlank(req.getGioiTinh())) {
            toMaGioiTinh(req.getGioiTinh());
        }
        if (!isBlank(req.getMatKhau()) && req.getMatKhau().trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
    }

    private void fill(KhachHang khachHang, KhachHangRequest req, boolean taoMoi) {
        if (taoMoi || !isBlank(req.getMaKhachHang())) {
            khachHang.setMaKhachHang(
                isBlank(req.getMaKhachHang()) ? sinhMaKhachHang() : req.getMaKhachHang().trim()
            );
        }
        if (taoMoi || !isBlank(req.getTenTaiKhoan())) {
            khachHang.setTenTaiKhoan(
                isBlank(req.getTenTaiKhoan()) ? sinhTenTaiKhoan(req) : req.getTenTaiKhoan().trim()
            );
        }
        if (taoMoi || !isBlank(req.getMatKhau())) {
            khachHang.setMatKhau(isBlank(req.getMatKhau()) ? MAT_KHAU_MAC_DINH : req.getMatKhau().trim());
        }
        khachHang.setTenKhachHang(req.getTenKhachHang().trim());
        khachHang.setEmail(trimToNull(req.getEmail()));
        khachHang.setSoDienThoai(trimToNull(req.getSoDienThoai()));
        khachHang.setNgaySinh(req.getNgaySinh());
        khachHang.setGioiTinh(toMaGioiTinh(req.getGioiTinh()));
        if (req.getTrangThai() != null) {
            khachHang.setTrangThai(req.getTrangThai());
        } else if (taoMoi) {
            khachHang.setTrangThai(true);
        }
        String nguoiThaoTac = isBlank(req.getNguoiCapNhat()) ? "admin" : req.getNguoiCapNhat().trim();
        khachHang.setNguoiCapNhat(nguoiThaoTac);
        khachHang.setNgayCapNhat(LocalDateTime.now());
        if (taoMoi) {
            khachHang.setNguoiTao(nguoiThaoTac);
        }
    }

    private void luuDiaChiMacDinh(KhachHang khachHang, KhachHangRequest req) {
        if (isBlank(req.getDiaChiCuThe())) {
            return;
        }
        DiaChiKhachHang diaChi = diaChiKhachHangRepository
            .findFirstByKhachHangIdAndMacDinhTrue(khachHang.getId())
            .orElseGet(() -> {
                DiaChiKhachHang d = new DiaChiKhachHang();
                d.setKhachHang(khachHang);
                d.setMaDiaChi(sinhMaDiaChi(khachHang.getId()));
                d.setMacDinh(true);
                d.setTrangThai(true);
                return d;
            });
        diaChi.setTenChiChi(khachHang.getTenKhachHang());
        diaChi.setThanhPho(trimToNull(req.getTinhThanhPho()));
        diaChi.setPhuong(trimToNull(req.getPhuong()));
        diaChi.setDiaChiCuThe(req.getDiaChiCuThe().trim());
        diaChiKhachHangRepository.save(diaChi);
    }

    private String sinhMaKhachHang() {
        long next = khachHangRepository.findMaxId() + 1;
        String ma;
        do {
            ma = String.format("KH%03d", next++);
        } while (khachHangRepository.existsByMaKhachHang(ma));
        return ma;
    }

    private String sinhTenTaiKhoan(KhachHangRequest req) {
        if (!isBlank(req.getEmail())) {
            String base = req.getEmail().trim().toLowerCase();
            int at = base.indexOf('@');
            base = at > 0 ? base.substring(0, at) : base;
            base = base.replaceAll("[^a-z0-9._-]", "");
            if (!base.isBlank() && !khachHangRepository.existsByTenTaiKhoan(base)) {
                return base;
            }
        }
        long next = khachHangRepository.findMaxId() + 1;
        String tenTaiKhoan;
        do {
            tenTaiKhoan = String.format("kh%06d", next++);
        } while (khachHangRepository.existsByTenTaiKhoan(tenTaiKhoan));
        return tenTaiKhoan;
    }

    private String sinhMaDiaChi(Long idKhachHang) {
        return String.format("DC%03d-%02d", idKhachHang, diaChiKhachHangRepository.countByKhachHangId(idKhachHang) + 1);
    }

    private KhachHang findOrThrow(Long id) {
        return khachHangRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng #" + id));
    }

    private Map<Long, KhachHangThongKeDTO> thongKeTheo(List<KhachHang> ds) {
        List<Long> ids = ds.stream().map(KhachHang::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return khachHangRepository.thongKeTheoKhachHang(ids).stream()
            .collect(Collectors.toMap(KhachHangThongKeDTO::khachHangId, Function.identity()));
    }

    private KhachHangDTO toFullDTO(KhachHang k) {
        List<DiaChiKhachHang> diaChis = diaChiKhachHangRepository
            .findByKhachHangIdOrderByMacDinhDescIdDesc(k.getId());
        KhachHangDTO dto = toDTO(k, thongKeTheo(List.of(k)));
        dto.setDanhSachDiaChi(diaChis.stream().map(KhachHangService::toDiaChiDTO).toList());
        dto.setDiaChiMacDinh(diaChis.stream()
            .filter(d -> Boolean.TRUE.equals(d.getMacDinh()))
            .findFirst()
            .map(KhachHangService::toDiaChiDTO)
            .orElse(null));
        return dto;
    }

    private KhachHangDTO toDTO(KhachHang k, Map<Long, KhachHangThongKeDTO> thongKe) {
        KhachHangThongKeDTO tk = thongKe.get(k.getId());
        BigDecimal tongChiTieu = tk != null && tk.tongChiTieu() != null ? tk.tongChiTieu() : BigDecimal.ZERO;
        String maHang = toMaHangTheoTongChiTieu(tongChiTieu);
        return KhachHangDTO.builder()
            .id(k.getId())
            .maKhachHang(k.getMaKhachHang())
            .tenTaiKhoan(k.getTenTaiKhoan())
            .tenKhachHang(k.getTenKhachHang())
            .email(k.getEmail())
            .soDienThoai(k.getSoDienThoai())
            .ngaySinh(k.getNgaySinh())
            .gioiTinh(k.getGioiTinh())
            .trangThai(k.getTrangThai())
            .trangThaiLabel(toTenTrangThai(k.getTrangThai()))
            .ngayTao(k.getNgayTao())
            .ngayCapNhat(k.getNgayCapNhat())
            .nguoiCapNhat(k.getNguoiCapNhat())
            .maHangThanhVien(maHang)
            .hangThanhVien(toTenHang(maHang))
            .soDon(tk != null && tk.soDon() != null ? tk.soDon() : 0L)
            .tongChiTieu(tongChiTieu)
            .build();
    }

    private static DiaChiKhachHangDTO toDiaChiDTO(DiaChiKhachHang d) {
        return new DiaChiKhachHangDTO(
            d.getId(),
            d.getKhachHang() != null ? d.getKhachHang().getId() : null,
            d.getMaDiaChi(),
            d.getTenChiChi(),
            d.getThanhPho(),
            d.getPhuong(),
            d.getDiaChiCuThe(),
            d.getMacDinh(),
            d.getTrangThai()
        );
    }

    private BigDecimal[] hangRange(String hangThanhVien) {
        if (isBlank(hangThanhVien)) {
            return new BigDecimal[] {null, null};
        }
        return switch (toMaHangThanhVien(hangThanhVien)) {
            case "KIM_CUONG" -> new BigDecimal[] {KIM_CUONG_MIN, null};
            case "VANG" -> new BigDecimal[] {VANG_MIN, KIM_CUONG_MIN};
            case "BAC" -> new BigDecimal[] {BAC_MIN, VANG_MIN};
            default -> new BigDecimal[] {null, BAC_MIN};
        };
    }

    private static Boolean toTrangThai(String value) {
        if (isBlank(value)) {
            return null;
        }
        String s = value.trim();
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("active") || s.equals(TRANG_THAI_DANG_HOAT_DONG)) {
            return Boolean.TRUE;
        }
        if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("inactive") || s.equalsIgnoreCase("locked")
            || s.equalsIgnoreCase("suspend") || s.equalsIgnoreCase("leave") || s.equalsIgnoreCase("unverified")
            || s.equals(TRANG_THAI_KHOA)) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Trạng thái không hợp lệ (Đang hoạt động / Đã khóa)");
    }

    private static String toTenTrangThai(Boolean trangThai) {
        if (trangThai == null) {
            return null;
        }
        return trangThai ? TRANG_THAI_DANG_HOAT_DONG : TRANG_THAI_KHOA;
    }

    private static String toMaHangThanhVien(String value) {
        String s = value.trim();
        if (s.equalsIgnoreCase("diamond")) {
            return "KIM_CUONG";
        }
        if (s.equalsIgnoreCase("gold")) {
            return "VANG";
        }
        if (s.equalsIgnoreCase("silver")) {
            return "BAC";
        }
        if (s.equalsIgnoreCase("normal") || s.equalsIgnoreCase("default")) {
            return "THUONG";
        }
        for (String[] pair : HANG_MAP) {
            if (pair[0].equalsIgnoreCase(s) || pair[1].equalsIgnoreCase(s)) {
                return pair[0];
            }
        }
        throw new IllegalArgumentException("Hạng thành viên không hợp lệ (Kim cương / Vàng / Bạc / Thường)");
    }

    private static String toMaHangTheoTongChiTieu(BigDecimal tongChiTieu) {
        BigDecimal t = tongChiTieu == null ? BigDecimal.ZERO : tongChiTieu;
        if (t.compareTo(KIM_CUONG_MIN) >= 0) {
            return "KIM_CUONG";
        }
        if (t.compareTo(VANG_MIN) >= 0) {
            return "VANG";
        }
        if (t.compareTo(BAC_MIN) >= 0) {
            return "BAC";
        }
        return "THUONG";
    }

    private static String toTenHang(String maHang) {
        for (String[] pair : HANG_MAP) {
            if (pair[0].equals(maHang)) {
                return pair[1];
            }
        }
        return maHang;
    }

    private static String toMaGioiTinh(String value) {
        if (isBlank(value)) {
            return null;
        }
        String s = value.trim();
        for (String[] pair : GIOI_TINH_MAP) {
            if (pair[0].equalsIgnoreCase(s) || pair[1].equalsIgnoreCase(s)) {
                return pair[0];
            }
        }
        throw new IllegalArgumentException("Giới tính không hợp lệ (Nam / Nữ / Khác)");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String trimToNull(String s) {
        return blankToNull(s);
    }
}