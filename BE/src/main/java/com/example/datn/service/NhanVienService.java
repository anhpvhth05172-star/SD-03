package com.example.datn.service;

import com.example.datn.dto.NhanVienDTO;
import com.example.datn.dto.NhanVienFormDataResponse;
import com.example.datn.dto.NhanVienRequest;
import com.example.datn.dto.PageResponse;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NhanVienService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String MAT_KHAU_MAC_DINH = "123456";
    private static final String TRANG_THAI_DANG_HOAT_DONG = "Đang hoạt động";
    private static final String TRANG_THAI_KHOA = "Đã khóa";
    private static final String[][] GIOI_TINH_MAP = {
        {"NAM", "Nam"},
        {"NU", "Nữ"},
        {"KHAC", "Khác"}
    };
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern SO_DIEN_THAI_PATTERN = Pattern.compile("^(0|\\+84)\\d{8,10}$");

    private final NhanVienRepository nhanVienRepository;
    private final VaiTroRepository vaiTroRepository;

    @Transactional(readOnly = true)
    public PageResponse<NhanVienDTO> list(
        String keyword, Long idVaiTro, String trangThai, LocalDate tuNgay, LocalDate denNgay, int page, int size
    ) {
        if (idVaiTro != null && !vaiTroRepository.existsById(idVaiTro)) {
            throw new IllegalArgumentException("Vai trò không tồn tại");
        }
        var result = nhanVienRepository.findByFilters(
            blankToNull(keyword), idVaiTro, toTrangThai(trangThai),
            tuNgay != null ? tuNgay.atStartOfDay() : null,
            denNgay != null ? denNgay.atTime(LocalTime.MAX) : null,
            PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "ngayTao")
            )
        );
        return PageResponse.from(result.map(this::toDTO));
    }

    @Transactional(readOnly = true)
    public NhanVienDTO get(Long id) {
        return toDTO(findOrThrow(id));
    }

    @Transactional
    public NhanVienDTO create(NhanVienRequest req) {
        validate(req, null);
        NhanVien nhanVien = new NhanVien();
        fill(nhanVien, req, true);
        return toDTO(nhanVienRepository.save(nhanVien));
    }

    @Transactional
    public NhanVienDTO update(Long id, NhanVienRequest req) {
        validate(req, id);
        NhanVien nhanVien = findOrThrow(id);
        fill(nhanVien, req, false);
        return toDTO(nhanVienRepository.save(nhanVien));
    }

    @Transactional
    public void delete(Long id) {
        NhanVien nhanVien = findOrThrow(id);
        nhanVien.setTrangThai(false);
        nhanVien.setNgayCapNhat(LocalDateTime.now());
        nhanVien.setNguoiCapNhat("admin");
        nhanVienRepository.save(nhanVien);
    }

    @Transactional(readOnly = true)
    public NhanVienFormDataResponse formData() {
        List<NhanVienFormDataResponse.VaiTroOption> vaiTros = vaiTroRepository
            .findByTrangThaiTrueOrderByTenVaiTroAsc().stream()
            .map(v -> new NhanVienFormDataResponse.VaiTroOption(v.getId(), v.getMaVaiTro(), v.getTenVaiTro()))
            .toList();
        return new NhanVienFormDataResponse(vaiTros);
    }

    // ===== private =====

    private void validate(NhanVienRequest req, Long excludeId) {
        if (req == null || isBlank(req.getTenTaiKhoan())) {
            throw new IllegalArgumentException("Tên nhân viên không được để trống");
        }
        String ten = req.getTenTaiKhoan().trim();
        boolean trungTen = excludeId == null
            ? nhanVienRepository.existsByTenTaiKhoan(ten)
            : nhanVienRepository.existsByTenTaiKhoanAndIdNot(ten, excludeId);
        if (trungTen) {
            throw new IllegalArgumentException("Tên nhân viên đã tồn tại");
        }
        if (!isBlank(req.getMaNhanVien())) {
            String ma = req.getMaNhanVien().trim();
            boolean trungMa = excludeId == null
                ? nhanVienRepository.existsByMaNhanVien(ma)
                : nhanVienRepository.existsByMaNhanVienAndIdNot(ma, excludeId);
            if (trungMa) {
                throw new IllegalArgumentException("Mã nhân viên đã tồn tại");
            }
        }
        if (excludeId == null && req.getIdVaiTro() == null) {
            throw new IllegalArgumentException("Vai trò không được để trống");
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

    private void fill(NhanVien nhanVien, NhanVienRequest req, boolean taoMoi) {
        if (taoMoi || !isBlank(req.getMaNhanVien())) {
            nhanVien.setMaNhanVien(isBlank(req.getMaNhanVien()) ? sinhMaNhanVien() : req.getMaNhanVien().trim());
        }
        nhanVien.setTenTaiKhoan(req.getTenTaiKhoan().trim());
        if (taoMoi || !isBlank(req.getMatKhau())) {
            nhanVien.setMatKhau(isBlank(req.getMatKhau()) ? MAT_KHAU_MAC_DINH : req.getMatKhau().trim());
        }
        nhanVien.setEmail(trimToNull(req.getEmail()));
        nhanVien.setSoDienThoai(trimToNull(req.getSoDienThoai()));
        nhanVien.setAnhNhanVien(trimToNull(req.getAnhNhanVien()));
        nhanVien.setGioiTinh(toMaGioiTinh(req.getGioiTinh()));
        nhanVien.setNgaySinh(req.getNgaySinh());
        nhanVien.setQueQuan(trimToNull(req.getQueQuan()));
        nhanVien.setPhuong(trimToNull(req.getPhuong()));
        nhanVien.setDiaChiCuThe(trimToNull(req.getDiaChiCuThe()));
        if (req.getTrangThai() != null) {
            nhanVien.setTrangThai(req.getTrangThai());
        } else if (taoMoi) {
            nhanVien.setTrangThai(true);
        }
        if (req.getIdVaiTro() != null) {
            nhanVien.setVaiTro(vaiTroRepository.findById(req.getIdVaiTro())
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại")));
        }
        String nguoiThaoTac = isBlank(req.getNguoiCapNhat()) ? "admin" : req.getNguoiCapNhat().trim();
        nhanVien.setNguoiCapNhat(nguoiThaoTac);
        nhanVien.setNgayCapNhat(LocalDateTime.now());
        if (taoMoi) {
            nhanVien.setNguoiTao(nguoiThaoTac);
        }
    }

    private String sinhMaNhanVien() {
        long next = nhanVienRepository.findMaxId() + 1;
        String ma;
        do {
            ma = String.format("NV%03d", next++);
        } while (nhanVienRepository.existsByMaNhanVien(ma));
        return ma;
    }

    private NhanVien findOrThrow(Long id) {
        return nhanVienRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên #" + id));
    }

    private NhanVienDTO toDTO(NhanVien n) {
        VaiTro v = n.getVaiTro();
        return NhanVienDTO.builder()
            .id(n.getId())
            .maNhanVien(n.getMaNhanVien())
            .tenTaiKhoan(n.getTenTaiKhoan())
            .email(n.getEmail())
            .soDienThoai(n.getSoDienThoai())
            .anhNhanVien(n.getAnhNhanVien())
            .gioiTinh(n.getGioiTinh())
            .queQuan(n.getQueQuan())
            .phuong(n.getPhuong())
            .diaChiCuThe(n.getDiaChiCuThe())
            .ngaySinh(n.getNgaySinh())
            .trangThai(n.getTrangThai())
            .trangThaiLabel(toTenTrangThai(n.getTrangThai()))
            .ngayTao(n.getNgayTao())
            .ngayCapNhat(n.getNgayCapNhat())
            .nguoiCapNhat(n.getNguoiCapNhat())
            .idVaiTro(v != null ? v.getId() : null)
            .maVaiTro(v != null ? v.getMaVaiTro() : null)
            .tenVaiTro(v != null ? v.getTenVaiTro() : null)
            .build();
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
            || s.equalsIgnoreCase("suspend") || s.equalsIgnoreCase("leave") || s.equals(TRANG_THAI_KHOA)) {
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