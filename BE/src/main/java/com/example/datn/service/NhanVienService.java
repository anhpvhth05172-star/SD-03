package com.example.datn.service;

import com.example.datn.dto.NhanVienDTO;
import com.example.datn.dto.NhanVienFormDataResponse;
import com.example.datn.dto.NhanVienRequest;
import com.example.datn.dto.PageResponse;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import com.example.datn.util.DoTuoi;
import com.example.datn.util.MaNhanVien;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NhanVienService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String MAT_KHAU_MAC_DINH = "123456";
    private static final String TRANG_THAI_DANG_HOAT_DONG = "Đang hoạt động";
    private static final String TRANG_THAI_KHOA = "Đã khóa";
    public static final int TUOI_TOI_THIEU = 18;
    public static final String THONG_BAO_TUOI ="Nhân viên phải đủ 18 tuổi.";
    private static final int SO_LAN_TAO = 5;
    private static final int SO_MA_TOI_DA = 1_000_000;
    private static final String[][] GIOI_TINH_MAP = {
        {"NAM", "Nam"},
        {"NU", "Nữ"},
        {"KHAC", "Khác"}
    };
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern SO_DIEN_THAI_PATTERN = Pattern.compile("^0\\d{8,9}$");
    private static final Pattern MA_NHAN_VIEN_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9]{0,24}$");
    private static final String[] MA_VAI_TRO_HOP_LE = {"ADMIN", "STAFF"};

    private final NhanVienRepository nhanVienRepository;
    private final VaiTroRepository vaiTroRepository;
    private final TransactionTemplate transactionTemplate;
    private final EmailService emailService;

    public NhanVienService(
        NhanVienRepository nhanVienRepository,
        VaiTroRepository vaiTroRepository,
        PlatformTransactionManager transactionManager,
        EmailService emailService
    ) {
        this.nhanVienRepository = nhanVienRepository;
        this.vaiTroRepository = vaiTroRepository;
        this.emailService = emailService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

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

    public NhanVienDTO create(NhanVienRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        for (int lan = 1; lan <= SO_LAN_TAO; lan++) {
            try {
                NhanVienDTO dto = transactionTemplate.execute(status -> {
                    validate(req, null);
                    NhanVien nhanVien = new NhanVien();
                    fill(nhanVien, req, true);
                    return toDTO(nhanVienRepository.saveAndFlush(nhanVien));
                });
                if (dto != null) {
                    if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
                        emailService.guiMailNhanVienMoi(
                            dto.getEmail().trim(),
                            dto.getTenTaiKhoan(),
                            dto.getMaNhanVien(),
                            dto.getEmail().trim(),
                            isBlank(req.getMatKhau()) ? MAT_KHAU_MAC_DINH : req.getMatKhau().trim(),
                            dto.getTenVaiTro() != null ? dto.getTenVaiTro() : "Nhân viên"
                        );
                    }
                    return dto;
                }
            } catch (DataIntegrityViolationException ex) {
            }
        }
        throw new IllegalArgumentException("Không tạo được mã nhân viên duy nhất, vui lòng thử lại");
    }

    @Transactional(readOnly = true)
    public boolean checkTrungEmail(String email, Long excludeId) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String cleanEmail = email.trim();
        return excludeId == null
            ? nhanVienRepository.existsByEmail(cleanEmail)
            : nhanVienRepository.existsByEmailAndIdNot(cleanEmail, excludeId);
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
        if (nhanVien.getVaiTro() != null && "ADMIN".equalsIgnoreCase(nhanVien.getVaiTro().getMaVaiTro())) {
            throw new IllegalArgumentException("Không được phép xóa tài khoản Quản trị viên");
        }
        nhanVien.setTrangThai(false);
        nhanVien.setNgayCapNhat(LocalDateTime.now());
        nhanVien.setNguoiCapNhat("admin");
        nhanVienRepository.save(nhanVien);
    }

    @Transactional
    public NhanVienDTO capNhatTrangThai(Long id, String trangThaiMoi) {
        NhanVien nhanVien = findOrThrow(id);
        if (nhanVien.getVaiTro() != null && "ADMIN".equalsIgnoreCase(nhanVien.getVaiTro().getMaVaiTro())) {
            throw new IllegalArgumentException("Không được phép thay đổi trạng thái của tài khoản Quản trị viên");
        }
        boolean moi = toTrangThai(trangThaiMoi);
        nhanVien.setTrangThai(moi);
        nhanVien.setNgayCapNhat(LocalDateTime.now());
        nhanVien.setNguoiCapNhat("admin");
        return toDTO(nhanVienRepository.save(nhanVien));
    }

    @Transactional(readOnly = true)
    public NhanVienFormDataResponse formData() {
        List<NhanVienFormDataResponse.VaiTroOption> vaiTros = vaiTroRepository
            .findByMaVaiTroIn(List.of(MA_VAI_TRO_HOP_LE)).stream()
            .sorted(Comparator.<VaiTro>comparingInt(v -> indexOfMa(v.getMaVaiTro())))
            .map(v -> new NhanVienFormDataResponse.VaiTroOption(v.getId(), v.getMaVaiTro(), v.getTenVaiTro()))
            .toList();
        return new NhanVienFormDataResponse(vaiTros);
    }

    private static int indexOfMa(String ma) {
        for (int i = 0; i < MA_VAI_TRO_HOP_LE.length; i++) {
            if (MA_VAI_TRO_HOP_LE[i].equalsIgnoreCase(ma)) {
                return i;
            }
        }
        return MA_VAI_TRO_HOP_LE.length;
    }

    @Transactional(readOnly = true)
    public String maNhanVienTuDong(String ten) {
        return sinhMaNhanVien(ten);
    }

    private void validate(NhanVienRequest req, Long excludeId) {
        boolean taoMoi = excludeId == null;
        if (req == null || isBlank(req.getTenTaiKhoan())) {
            throw new IllegalArgumentException("Tên nhân viên không được để trống");
        }
        checkLength(req.getTenTaiKhoan(), 100, "Tên nhân viên");
        checkLength(req.getMaNhanVien(), MaNhanVien.TOI_DA, "Mã nhân viên");
        checkLength(req.getMatKhau(), 127, "Mật khẩu");
        checkLength(req.getEmail(), 150, "Email");
        checkLength(req.getSoDienThoai(), 10, "Số điện thoại");
        checkLength(req.getQueQuan(), 150, "Quê quán");
        checkLength(req.getPhuong(), 100, "Phường");
        checkLength(req.getDiaChiCuThe(), 255, "Địa chỉ cụ thể");
        checkLength(req.getAnhNhanVien(), 1000, "Ảnh nhân viên");
        checkLength(req.getNguoiCapNhat(), 100, "Người cập nhật");
        String ten = req.getTenTaiKhoan().trim();
        boolean trungTen = taoMoi
            ? nhanVienRepository.existsByTenTaiKhoan(ten)
            : nhanVienRepository.existsByTenTaiKhoanAndIdNot(ten, excludeId);
        if (trungTen) {
            throw new IllegalArgumentException("Tên nhân viên đã tồn tại");
        }
        if (!isBlank(req.getMaNhanVien())) {
            String ma = req.getMaNhanVien().trim();
            if (!MaNhanVien.hopLe(ma)) {
                throw new IllegalArgumentException(
                    "Mã nhân viên không hợp lệ (bắt đầu bằng chữ cái, chỉ chữ và số, tối đa "
                        + MaNhanVien.TOI_DA + " ký tự)");
            }

            if (!taoMoi && nhanVienRepository.existsByMaNhanVienAndIdNot(ma, excludeId)) {
                throw new IllegalArgumentException("Mã nhân viên đã tồn tại");
            }
        }
        if (taoMoi && req.getIdVaiTro() == null) {
            throw new IllegalArgumentException("Vai trò không được để trống");
        }
        if (req.getIdVaiTro() != null) {
            VaiTro vaiTro = vaiTroRepository.findById(req.getIdVaiTro())
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại"));
            if (!isAllowedRole(vaiTro.getMaVaiTro())) {
                throw new IllegalArgumentException(
                    "Hệ thống chỉ hỗ trợ 2 vai trò: Admin (ADMIN) và Nhân viên (STAFF)");
            }
        }
        if (taoMoi && isBlank(req.getEmail())) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        if (!isBlank(req.getEmail())) {
            String email = req.getEmail().trim();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new IllegalArgumentException("Email không hợp lệ");
            }
            boolean trungEmail = taoMoi
                ? nhanVienRepository.existsByEmail(email)
                : nhanVienRepository.existsByEmailAndIdNot(email, excludeId);
            if (trungEmail) {
                throw new IllegalArgumentException("Email đã được sử dụng bởi nhân viên khác");
            }
        }
        if (taoMoi && isBlank(req.getSoDienThoai())) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }
        if (!isBlank(req.getSoDienThoai())) {
            String sdt = req.getSoDienThoai().trim();
            if (!SO_DIEN_THAI_PATTERN.matcher(sdt).matches()) {
                throw new IllegalArgumentException(
                    "Số điện thoại không hợp lệ (bắt đầu bằng 0, gồm 9-10 chữ số, không chứa chữ cái)");
            }
        }
        if (taoMoi && req.getNgaySinh() == null) {
            throw new IllegalArgumentException("Ngày sinh không được để trống");
        }
        if (req.getNgaySinh() != null && req.getNgaySinh().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
        }
        String loiTuoi = DoTuoi.kiemTra(req.getNgaySinh(), TUOI_TOI_THIEU, THONG_BAO_TUOI);
        if (loiTuoi != null) {
            throw new IllegalArgumentException(loiTuoi);
        }
        if (taoMoi && isBlank(req.getGioiTinh())) {
            throw new IllegalArgumentException("Giới tính không được để trống");
        }
        if (!isBlank(req.getGioiTinh())) {
            toMaGioiTinh(req.getGioiTinh());
        }
        if (!isBlank(req.getMatKhau()) && req.getMatKhau().trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
    }

    private static boolean isAllowedRole(String maVaiTro) {
        for (String ma : MA_VAI_TRO_HOP_LE) {
            if (ma.equalsIgnoreCase(maVaiTro)) {
                return true;
            }
        }
        return false;
    }

    private void fill(NhanVien nhanVien, NhanVienRequest req, boolean taoMoi) {
        if (taoMoi) {
            nhanVien.setMaNhanVien(chonMaNhanVien(req.getMaNhanVien(), req.getTenTaiKhoan()));
        } else if (!isBlank(req.getMaNhanVien())) {
            nhanVien.setMaNhanVien(req.getMaNhanVien().trim());
        }
        nhanVien.setTenTaiKhoan(req.getTenTaiKhoan().trim());
        if (taoMoi || !isBlank(req.getMatKhau())) {
            nhanVien.setMatKhau(isBlank(req.getMatKhau()) ? MAT_KHAU_MAC_DINH : req.getMatKhau().trim());
        }
        if (taoMoi || !isBlank(req.getEmail())) {
            nhanVien.setEmail(trimToNull(req.getEmail()));
        }
        if (taoMoi || !isBlank(req.getSoDienThoai())) {
            nhanVien.setSoDienThoai(trimToNull(req.getSoDienThoai()));
        }
        nhanVien.setAnhNhanVien(trimToNull(req.getAnhNhanVien()));
        if (taoMoi || !isBlank(req.getGioiTinh())) {
            nhanVien.setGioiTinh(toMaGioiTinh(req.getGioiTinh()));
        }
        if (taoMoi || req.getNgaySinh() != null) {
            nhanVien.setNgaySinh(req.getNgaySinh());
        }
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

    private String chonMaNhanVien(String maGui, String tenNhanVien) {
        if (!isBlank(maGui)) {
            String ma = maGui.trim();
            if (MaNhanVien.hopLe(ma) && !nhanVienRepository.existsByMaNhanVien(ma)) {
                return ma;
            }
        }
        return sinhMaNhanVien(tenNhanVien);
    }

    private String sinhMaNhanVien(String tenNhanVien) {
        String goc = MaNhanVien.sinh(tenNhanVien);
        int so = 1;
        String ma = MaNhanVien.congSo(goc, so);
        while (nhanVienRepository.existsByMaNhanVien(ma)) {
            if (++so > SO_MA_TOI_DA) {
                throw new IllegalArgumentException("Không tìm được mã nhân viên còn trống");
            }
            ma = MaNhanVien.congSo(goc, so);
        }
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

    private static void checkLength(String value, int max, String fieldName) {
        if (value != null && value.trim().length() > max) {
            throw new IllegalArgumentException(fieldName + " không được vượt quá " + max + " ký tự");
        }
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String trimToNull(String s) {
        return blankToNull(s);
    }
}