package com.example.datn.service;

import com.example.datn.dto.DiaChiKhachHangDTO;
import com.example.datn.dto.DiaChiKhachHangRequest;
import com.example.datn.dto.KhachHangDTO;
import com.example.datn.dto.KhachHangRequest;
import com.example.datn.dto.KhachHangThongKeDTO;
import com.example.datn.dto.PageResponse;
import com.example.datn.entity.DiaChiKhachHang;
import com.example.datn.entity.KhachHang;
import com.example.datn.repository.DiaChiKhachHangRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.util.DoTuoi;
import com.example.datn.util.MaNhanVien;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class KhachHangService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String MAT_KHAU_MAC_DINH = "123456";
    private static final String TRANG_THAI_DANG_HOAT_DONG = "Đang hoạt động";
    private static final String TRANG_THAI_KHOA = "Đã khóa";
    public static final int TUOI_TOI_THIEU = 16;
    public static final String THONG_BAO_TUOI =
        "Khách hàng phải đủ 16 tuổi.";
    /** Số lần thử lại khi hai yêu cầu tạo cùng lúc giành cùng một mã. */
    private static final int SO_LAN_TAO = 5;
    private static final int SO_MA_TOI_DA = 1_000_000;
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
    private static final Pattern SO_DIEN_THAI_PATTERN = Pattern.compile("^0\\d{8,9}$");
    private static final Pattern MA_KHACH_HANG_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9]{0,24}$");

    private final KhachHangRepository khachHangRepository;
    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final EmailService emailService;
    private final TransactionTemplate transactionTemplate;

    public KhachHangService(
        KhachHangRepository khachHangRepository,
        DiaChiKhachHangRepository diaChiKhachHangRepository,
        PlatformTransactionManager transactionManager
    ) {
        this(khachHangRepository, diaChiKhachHangRepository, null, transactionManager);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public KhachHangService(
        KhachHangRepository khachHangRepository,
        DiaChiKhachHangRepository diaChiKhachHangRepository,
        EmailService emailService,
        PlatformTransactionManager transactionManager
    ) {
        this.khachHangRepository = khachHangRepository;
        this.diaChiKhachHangRepository = diaChiKhachHangRepository;
        this.emailService = emailService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // Mỗi lần thử là một transaction riêng để có thể thử lại sau khi rollback.
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

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

    /**
     * Tạo khách hàng. Nếu hai yêu cầu đồng thời giành cùng một mã, ràng buộc duy nhất
     * của database sẽ chặn bản ghi trùng và toàn bộ thao tác được thử lại với mã kế tiếp.
     */
    public KhachHangDTO create(KhachHangRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        }
        for (int lan = 1; lan <= SO_LAN_TAO; lan++) {
            try {
                KhachHangDTO dto = transactionTemplate.execute(status -> {
                    validate(req, null);
                    KhachHang khachHang = new KhachHang();
                    fill(khachHang, req, true);
                    khachHang = khachHangRepository.saveAndFlush(khachHang);
                    luuDiaChiMacDinh(khachHang, req);
                    return toFullDTO(khachHang);
                });
                if (dto != null) {
                    if (emailService != null && dto.getEmail() != null && !dto.getEmail().isBlank()) {
                        emailService.guiMailDangKyThanhCong(
                            dto.getEmail(),
                            dto.getTenKhachHang(),
                            dto.getMaKhachHang(),
                            dto.getTenTaiKhoan(),
                            MAT_KHAU_MAC_DINH
                        );
                    }
                    return dto;
                }
            } catch (DataIntegrityViolationException ex) {
                // Trùng mã/duy nhất ở lần trước -> vòng lặp tiếp theo sẽ chọn mã khác.
            }
        }
        throw new IllegalArgumentException("Không tạo được mã khách hàng duy nhất, vui lòng thử lại");
    }

    @Transactional(readOnly = true)
    public boolean checkTrungEmail(String email, Long excludeId) {
        if (isBlank(email)) {
            return false;
        }
        String trimmed = email.trim();
        return excludeId != null
            ? khachHangRepository.existsByEmailAndIdNot(trimmed, excludeId)
            : khachHangRepository.existsByEmail(trimmed);
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

    @Transactional(readOnly = true)
    public KhachHangDTO capNhatTrangThai(Long id, String trangThaiMoi) {
        KhachHang khachHang = findOrThrow(id);
        khachHang.setTrangThai(toTrangThai(trangThaiMoi));
        khachHang.setNgayCapNhat(LocalDateTime.now());
        khachHang.setNguoiCapNhat("admin");
        return toFullDTO(khachHangRepository.save(khachHang));
    }

    /** Mã khách hàng đề xuất theo họ tên (chưa tính các mã đã tồn tại trên FE). */
    @Transactional(readOnly = true)
    public String maKhachHangTuDong(String ten) {
        return sinhMaKhachHang(ten);
    }

    // ===== địa chỉ của khách hàng =====

    @Transactional(readOnly = true)
    public List<DiaChiKhachHangDTO> dsDiaChi(Long idKhachHang) {
        findOrThrow(idKhachHang);
        return diaChiKhachHangRepository.findByKhachHangIdOrderByMacDinhDescIdDesc(idKhachHang).stream()
            .map(KhachHangService::toDiaChiDTO)
            .toList();
    }

    @Transactional
    public DiaChiKhachHangDTO themDiaChi(Long idKhachHang, DiaChiKhachHangRequest req) {
        KhachHang khachHang = findOrThrow(idKhachHang);
        validateDiaChi(req, null);
        boolean macDinh = Boolean.TRUE.equals(req.getMacDinh())
            || diaChiKhachHangRepository.countByKhachHangId(idKhachHang) == 0;
        DiaChiKhachHang diaChi = new DiaChiKhachHang();
        diaChi.setKhachHang(khachHang);
        diaChi.setMaDiaChi(sinhMaDiaChi(idKhachHang));
        fillDiaChi(diaChi, req, macDinh);
        if (macDinh) {
            boMacDinhKhac(idKhachHang, diaChi.getId());
        }
        return toDiaChiDTO(diaChiKhachHangRepository.save(diaChi));
    }

    @Transactional
    public DiaChiKhachHangDTO capNhatDiaChi(Long idKhachHang, Long idDiaChi, DiaChiKhachHangRequest req) {
        findOrThrow(idKhachHang);
        DiaChiKhachHang diaChi = timDiaChi(idKhachHang, idDiaChi);
        validateDiaChi(req, idDiaChi);
        fillDiaChi(diaChi, req, Boolean.TRUE.equals(req.getMacDinh()));
        if (Boolean.TRUE.equals(req.getMacDinh())) {
            boMacDinhKhac(idKhachHang, diaChi.getId());
        }
        return toDiaChiDTO(diaChiKhachHangRepository.save(diaChi));
    }

    @Transactional
    public void xoaDiaChi(Long idKhachHang, Long idDiaChi) {
        findOrThrow(idKhachHang);
        DiaChiKhachHang diaChi = timDiaChi(idKhachHang, idDiaChi);
        boolean laMacDinh = Boolean.TRUE.equals(diaChi.getMacDinh());
        diaChiKhachHangRepository.delete(diaChi);
        if (laMacDinh) {
            diaChiKhachHangRepository.findByKhachHangIdOrderByMacDinhDescIdDesc(idKhachHang).stream()
                .findFirst()
                .ifPresent(d -> {
                    d.setMacDinh(true);
                    diaChiKhachHangRepository.save(d);
                });
        }
    }

    @Transactional
    public DiaChiKhachHangDTO datMacDinh(Long idKhachHang, Long idDiaChi) {
        findOrThrow(idKhachHang);
        DiaChiKhachHang diaChi = timDiaChi(idKhachHang, idDiaChi);
        boMacDinhKhac(idKhachHang, idDiaChi);
        diaChi.setMacDinh(true);
        diaChi.setTrangThai(true);
        return toDiaChiDTO(diaChiKhachHangRepository.save(diaChi));
    }

    private DiaChiKhachHang timDiaChi(Long idKhachHang, Long idDiaChi) {
        return diaChiKhachHangRepository.findById(idDiaChi)
            .filter(d -> d.getKhachHang() != null && idKhachHang.equals(d.getKhachHang().getId()))
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ #" + idDiaChi));
    }

    private void boMacDinhKhac(Long idKhachHang, Long idDiaChiGiuLai) {
        for (DiaChiKhachHang d : diaChiKhachHangRepository.findByKhachHangIdOrderByMacDinhDescIdDesc(idKhachHang)) {
            if (Boolean.TRUE.equals(d.getMacDinh()) && !d.getId().equals(idDiaChiGiuLai)) {
                d.setMacDinh(false);
                diaChiKhachHangRepository.save(d);
            }
        }
    }

    private void validateDiaChi(DiaChiKhachHangRequest req, Long excludeId) {
        if (req == null || isBlank(req.getDiaChiCuThe())) {
            throw new IllegalArgumentException("Địa chỉ cụ thể không được để trống");
        }
        checkLength(req.getTenDiaChi(), 100, "Tên địa chỉ");
        checkLength(req.getTinhThanhPho(), 100, "Tỉnh/Thành phố");
        checkLength(req.getPhuong(), 100, "Phường/Xã");
        checkLength(req.getDiaChiCuThe(), 255, "Địa chỉ cụ thể");
    }

    private void fillDiaChi(DiaChiKhachHang diaChi, DiaChiKhachHangRequest req, boolean macDinh) {
        diaChi.setTenChiChi(blankToNull(req.getTenDiaChi()));
        diaChi.setThanhPho(blankToNull(req.getTinhThanhPho()));
        diaChi.setPhuong(blankToNull(req.getPhuong()));
        diaChi.setDiaChiCuThe(req.getDiaChiCuThe().trim());
        diaChi.setMacDinh(macDinh);
        if (req.getTrangThai() != null) {
            diaChi.setTrangThai(req.getTrangThai());
        } else if (diaChi.getTrangThai() == null) {
            diaChi.setTrangThai(true);
        }
    }

    // ===== private =====

    private void validate(KhachHangRequest req, Long excludeId) {
        boolean taoMoi = excludeId == null;
        if (req == null || isBlank(req.getTenKhachHang())) {
            throw new IllegalArgumentException("Họ và tên không được để trống");
        }
        checkLength(req.getTenKhachHang(), 200, "Họ và tên");
        checkLength(req.getMaKhachHang(), 50, "Mã khách hàng");
        checkLength(req.getTenTaiKhoan(), 100, "Tên đăng nhập");
        checkLength(req.getMatKhau(), 127, "Mật khẩu");
        checkLength(req.getEmail(), 150, "Email");
        checkLength(req.getSoDienThoai(), 10, "Số điện thoại");
        checkLength(req.getTinhThanhPho(), 100, "Tỉnh thành phố");
        checkLength(req.getPhuong(), 100, "Phường");
        checkLength(req.getDiaChiCuThe(), 255, "Địa chỉ cụ thể");
        checkLength(req.getNguoiCapNhat(), 100, "Người cập nhật");
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
            if (!MA_KHACH_HANG_PATTERN.matcher(ma).matches()) {
                throw new IllegalArgumentException(
                    "Mã khách hàng không hợp lệ (bắt đầu bằng chữ cái, chỉ chữ và số, tối đa "
                        + MaNhanVien.TOI_DA + " ký tự)");
            }
            // Khi tạo: mã trùng sẽ được sinh lại ở fill() (FE gửi lên chỉ là mã đề xuất).
            // Khi sửa: không được đổi sang mã của khách hàng khác.
            if (!taoMoi && khachHangRepository.existsByMaKhachHangAndIdNot(ma, excludeId)) {
                throw new IllegalArgumentException("Mã khách hàng đã tồn tại");
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
                ? khachHangRepository.existsByEmail(email)
                : khachHangRepository.existsByEmailAndIdNot(email, excludeId);
            if (trungEmail) {
                throw new IllegalArgumentException("Email đã được sử dụng bởi khách hàng khác");
            }
        }
        if (!isBlank(req.getSoDienThoai()) && !SO_DIEN_THAI_PATTERN.matcher(req.getSoDienThoai().trim()).matches()) {
            throw new IllegalArgumentException(
                "Số điện thoại không hợp lệ (bắt đầu bằng 0, gồm 9-10 chữ số, không chứa chữ cái)");
        }
        if (req.getNgaySinh() != null && req.getNgaySinh().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày sinh không được ở tương lai");
        }
        // Không chỉ khi tạo: chặn gọi API trực tiếp để sửa ngày sinh thành chưa đủ tuổi.
        String loiTuoi = DoTuoi.kiemTra(req.getNgaySinh(), TUOI_TOI_THIEU, THONG_BAO_TUOI);
        if (loiTuoi != null) {
            throw new IllegalArgumentException(loiTuoi);
        }
        if (!isBlank(req.getGioiTinh())) {
            toMaGioiTinh(req.getGioiTinh());
        }
        if (!isBlank(req.getMatKhau()) && req.getMatKhau().trim().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
    }

    private void fill(KhachHang khachHang, KhachHangRequest req, boolean taoMoi) {
        if (taoMoi) {
            khachHang.setMaKhachHang(chonMaKhachHang(req.getMaKhachHang(), req.getTenKhachHang()));
        } else if (!isBlank(req.getMaKhachHang())) {
            khachHang.setMaKhachHang(req.getMaKhachHang().trim());
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
        if (taoMoi || !isBlank(req.getEmail())) {
            khachHang.setEmail(trimToNull(req.getEmail()));
        }
        if (taoMoi || !isBlank(req.getSoDienThoai())) {
            khachHang.setSoDienThoai(trimToNull(req.getSoDienThoai()));
        }
        if (taoMoi || req.getNgaySinh() != null) {
            khachHang.setNgaySinh(req.getNgaySinh());
        }
        if (taoMoi || !isBlank(req.getGioiTinh())) {
            khachHang.setGioiTinh(toMaGioiTinh(req.getGioiTinh()));
        }
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
        boolean coDiaChiCuThe = !isBlank(req.getDiaChiCuThe());
        boolean coTinh = !isBlank(req.getTinhThanhPho());
        boolean coPhuong = !isBlank(req.getPhuong());
        if (!coDiaChiCuThe && !coTinh && !coPhuong) {
            return;
        }

        String diaChiCuThe = coDiaChiCuThe
            ? req.getDiaChiCuThe().trim()
            : java.util.stream.Stream.of(req.getPhuong(), req.getTinhThanhPho())
                .filter(s -> !isBlank(s))
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(", "));
        if (isBlank(diaChiCuThe)) {
            diaChiCuThe = "—";
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
        diaChi.setDiaChiCuThe(diaChiCuThe);
        diaChiKhachHangRepository.save(diaChi);
    }

    /**
     * Chọn mã khi tạo: dùng mã FE gửi lên nếu còn trống, ngược lại sinh mã mới
     * từ họ tên với số thứ tự kế tiếp chưa tồn tại.
     */
    private String chonMaKhachHang(String maGui, String tenKhachHang) {
        if (!isBlank(maGui)) {
            String ma = maGui.trim();
            if (MA_KHACH_HANG_PATTERN.matcher(ma).matches() && !khachHangRepository.existsByMaKhachHang(ma)) {
                return ma;
            }
        }
        return sinhMaKhachHang(tenKhachHang);
    }

    private String sinhMaKhachHang(String tenKhachHang) {
        String goc = MaNhanVien.sinh(tenKhachHang);
        int so = 1;
        String ma = MaNhanVien.congSo(goc, so);
        while (khachHangRepository.existsByMaKhachHang(ma)) {
            if (++so > SO_MA_TOI_DA) {
                throw new IllegalArgumentException("Không tìm được mã khách hàng còn trống");
            }
            ma = MaNhanVien.congSo(goc, so);
        }
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
        KhachHangDTO dto = KhachHangDTO.builder()
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
        diaChiKhachHangRepository.findFirstByKhachHangIdAndMacDinhTrue(k.getId())
            .or(() -> diaChiKhachHangRepository.findByKhachHangIdOrderByMacDinhDescIdDesc(k.getId()).stream().findFirst())
            .map(KhachHangService::toDiaChiDTO)
            .ifPresent(dto::setDiaChiMacDinh);
        return dto;
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