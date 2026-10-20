package com.example.datn.service;

import com.example.datn.dto.NhanVien.NhanVienDTO;
import com.example.datn.dto.NhanVien.NhanVienExcelImportResult;
import com.example.datn.dto.NhanVien.NhanVienFormDataResponse;
import com.example.datn.dto.NhanVien.NhanVienRequest;
import com.example.datn.dto.NhanVien.GuiEmailHangLoatRequest;
import com.example.datn.dto.NhanVien.GuiEmailHangLoatResponse;
import com.example.datn.dto.Common.PageResponse;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import com.example.datn.util.DoTuoi;
import com.example.datn.util.MaNhanVien;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class NhanVienService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final String MAT_KHAU_MAC_DINH = "123456";
    private static final String TRANG_THAI_DANG_HOAT_DONG = "Đang hoạt động";
    private static final String TRANG_THAI_KHOA = "Đã khóa";
    public static final int TUOI_TOI_THIEU = 18;
    public static final String THONG_BAO_TUOI ="Nhân viên phải đủ 18 tuổi.";
    private static final int SO_LAN_TAO = 5;
    private static final int SO_MA_TOI_DA = 1_000_000;
    private static final int GUI_EMAIL_TOI_DA = 500;
    /** Số luồng gửi email hàng loạt song song — mỗi email mất ~3-5s nếu gửi tuần tiến. */
    private static final int GUI_EMAIL_SONG_SONG = 8;
    private static final String[][] GIOI_TINH_MAP = {
        {"NAM", "Nam"},
        {"NU", "Nữ"},
        {"KHAC", "Khác"}
    };
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern SO_DIEN_THAI_PATTERN = Pattern.compile("^0\\d{9}$");
    private static final Pattern MA_NHAN_VIEN_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9]{0,24}$");
    private static final String[] MA_VAI_TRO_HOP_LE = {"ADMIN", "STAFF"};
    private static final String[] COT_EXCEL = {
        "Họ và tên", "Email", "Số điện thoại", "Giới tính", "Ngày sinh",
        "Vai trò", "Địa chỉ", "Phường/Xã", "Quê quán", "Trạng thái"
    };
    private static final DateTimeFormatter NGAY_SINH_FORMAT =
        DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

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
        return create(req, true);
    }

    /**
     * Tạo nhân viên mới. Tham số {@code emailCapTaiKhoan} cho phép bỏ qua bước gửi email
     * cấp tài khoản (dùng khi import hàng loạt, không gửi email cho từng dòng).
     */
    public NhanVienDTO create(NhanVienRequest req, boolean emailCapTaiKhoan) {
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
                    dto.setEmailGuiThanhCong(emailCapTaiKhoan && guiEmailCapTaiKhoan(dto, req));
                    return dto;
                }
            } catch (DataIntegrityViolationException ex) {
            }
        }
        throw new IllegalArgumentException("Không tạo được mã nhân viên duy nhất, vui lòng thử lại");
    }

    /**
     * Gửi email cấp tài khoản nhân viên và trả về đúng trạng thái nhà cung cấp mail trả về.
     * Trả về false khi thiếu dịch vụ email hoặc thiếu địa chỉ nhận, tuyệt đối không báo thành công giả.
     */
    private boolean guiEmailCapTaiKhoan(NhanVienDTO dto, NhanVienRequest req) {
        if (emailService == null) {
            log.warn("Bỏ qua gửi email cấp tài khoản cho nhân viên {}: chưa cấu hình dịch vụ email",
                dto.getMaNhanVien());
            return false;
        }
        if (isBlank(dto.getEmail())) {
            log.warn("Bỏ qua gửi email cấp tài khoản cho nhân viên {}: không có địa chỉ email",
                dto.getMaNhanVien());
            return false;
        }
        return emailService.guiMailNhanVienMoi(
            dto.getEmail().trim(),
            dto.getTenTaiKhoan(),
            dto.getMaNhanVien(),
            dto.getEmail().trim(),
            isBlank(req.getMatKhau()) ? MAT_KHAU_MAC_DINH : req.getMatKhau().trim(),
            dto.getTenVaiTro() != null ? dto.getTenVaiTro() : "Nhân viên"
        );
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

    /** Kiểm tra dữ liệu nhân viên không thay đổi dữ liệu (dùng cho xem trước import Excel). */
    @Transactional(readOnly = true)
    public void kiemTraHopLe(NhanVienRequest req, Long excludeId) {
        validate(req, excludeId);
    }

    /**
     * Gửi cùng một nội dung email (tiêu đề + nội dung) tới nhiều nhân viên.
     * Nhân viên không có email hợp lệ hoặc gửi lỗi đều được ghi nhận lý do vào danh sách bỏ qua.
     */
    @Transactional(readOnly = true)
    public GuiEmailHangLoatResponse guiEmailHangLoat(GuiEmailHangLoatRequest req) {
        if (req == null || req.getIds() == null || req.getIds().isEmpty()) {
            throw new IllegalArgumentException("Danh sách nhân viên nhận email không được để trống");
        }
        if (isBlank(req.getTieuDe())) {
            throw new IllegalArgumentException("Tiêu đề email không được để trống");
        }
        checkLength(req.getTieuDe(), 200, "Tiêu đề email");
        if (isBlank(req.getNoiDung())) {
            throw new IllegalArgumentException("Nội dung email không được để trống");
        }
        checkLength(req.getNoiDung(), 5000, "Nội dung email");

        List<Long> ids = req.getIds().stream()
            .filter(java.util.Objects::nonNull)
            .distinct()
            .toList();
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Danh sách nhân viên nhận email không hợp lệ");
        }
        if (ids.size() > GUI_EMAIL_TOI_DA) {
            throw new IllegalArgumentException("Mỗi lần gửi tối đa " + GUI_EMAIL_TOI_DA + " nhân viên");
        }

        GuiEmailHangLoatResponse response = GuiEmailHangLoatResponse.builder()
            .tongYeuCau(ids.size())
            .build();

        java.util.Map<Long, NhanVien> nhanVienMap = nhanVienRepository.findAllById(ids).stream()
            .collect(java.util.stream.Collectors.toMap(NhanVien::getId, nv -> nv, (a, b) -> a));
        String tieuDe = req.getTieuDe().trim();
        String noiDung = req.getNoiDung().trim();

        // 1. Kiểm tra dữ liệu trên thread chính (email hợp lệ, nhân viên tồn tại)
        java.util.List<Long> canGui = new java.util.ArrayList<>();
        for (Long id : ids) {
            NhanVien nv = nhanVienMap.get(id);
            if (nv == null) {
                response.setThatBai(response.getThatBai() + 1);
                response.getBoQua().add(
                    new GuiEmailHangLoatResponse.BoQua(id, null, null, null, "Không tìm thấy nhân viên"));
                continue;
            }
            if (isBlank(nv.getEmail()) || !EMAIL_PATTERN.matcher(nv.getEmail().trim()).matches()) {
                response.setThatBai(response.getThatBai() + 1);
                response.getBoQua().add(new GuiEmailHangLoatResponse.BoQua(
                    id, nv.getMaNhanVien(), nv.getTenTaiKhoan(), nv.getEmail(),
                    "Nhân viên không có địa chỉ email hợp lệ"));
                continue;
            }
            canGui.add(id);
        }

        if (canGui.isEmpty()) {
            return response;
        }

        // 2. Gửi song song với pool giới hạn — mỗi email mất ~3-5s (SMTP handshake),
        //    tuần tiến khiến 50 người gửi mất hơn 3 phút và người dùng tưởng treo.
        java.util.concurrent.ExecutorService pool =
            java.util.concurrent.Executors.newFixedThreadPool(Math.min(GUI_EMAIL_SONG_SONG, canGui.size()));
        try {
            java.util.List<java.util.concurrent.Future<boolean[]>> futures = new java.util.ArrayList<>();
            for (Long id : canGui) {
                String email = nhanVienMap.get(id).getEmail().trim();
                futures.add(pool.submit(() -> new boolean[] { emailService.guiMailCustom(email, tieuDe, noiDung) }));
            }
            for (int i = 0; i < futures.size(); i++) {
                Long id = canGui.get(i);
                NhanVien nv = nhanVienMap.get(id);
                boolean guiOk = false;
                try {
                    guiOk = futures.get(i).get()[0];
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    log.error("Gửi email hàng loạt cho nhân viên {} gặp lỗi", id, e.getCause());
                }
                if (guiOk) {
                    response.setGuiThanhCong(response.getGuiThanhCong() + 1);
                } else {
                    response.setThatBai(response.getThatBai() + 1);
                    response.getBoQua().add(new GuiEmailHangLoatResponse.BoQua(
                        id, nv.getMaNhanVien(), nv.getTenTaiKhoan(), nv.getEmail(),
                        "Nhà cung cấp email từ chối hoặc có lỗi khi gửi"));
                }
            }
        } finally {
            pool.shutdown();
        }
        return response;
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
        Boolean parsedStatus = toTrangThai(trangThaiMoi);
        if (parsedStatus == null) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái tài khoản");
        }
        boolean moi = parsedStatus;
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

    public byte[] downloadExcelTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("NhanVien");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < COT_EXCEL.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(COT_EXCEL[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, 1);
            int[] rong = {30, 30, 15, 12, 14, 12, 30, 25, 25, 18};
            for (int i = 0; i < rong.length; i++) {
                sheet.setColumnWidth(i, rong[i] * 256);
            }

            Sheet huongDan = workbook.createSheet("Huong dan");
            String[] noiDung = {
                "HƯỚNG DẪN IMPORT NHÂN VIÊN",
                "1. Chỉ điền dữ liệu vào sheet NhanVien, giữ nguyên tên các cột.",
                "2. Ngày sinh dạng dd/MM/yyyy (ví dụ 15/01/1990), nhân viên phải đủ 18 tuổi.",
                "3. Vai trò chỉ nhận ADMIN hoặc STAFF.",
                "4. Trạng thái: 'Đang hoạt động' hoặc 'Đã khóa' (để trống = Đang hoạt động).",
                "5. Email và số điện thoại phải duy nhất; dòng trùng sẽ bị báo lỗi, các dòng khác vẫn import.",
                "6. Mã nhân viên tự động tạo, không cần điền.",
                "7. Mật khẩu mặc định: 123456 (nhân viên tự đổi sau khi đăng nhập).",
                "8. Nhân viên import không nhận mail cấp tài khoản, hãy gửi mật khẩu trực tiếp."
            };
            for (int i = 0; i < noiDung.length; i++) {
                Row dong = huongDan.createRow(i);
                dong.createCell(0).setCellValue(noiDung[i]);
            }
            huongDan.setColumnWidth(0, 110 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được file mẫu Excel", e);
        }
    }

    public NhanVienExcelImportResult importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File Excel không được để trống");
        }
        NhanVienExcelImportResult kq = NhanVienExcelImportResult.builder().build();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet == null ? null : sheet.getRow(0);
            if (header == null) {
                throw new IllegalArgumentException("File Excel thiếu dòng tiêu đề, vui lòng dùng file mẫu");
            }
            DataFormatter fmt = new DataFormatter();
            Map<String, Integer> cot = new HashMap<>();
            for (int i = 0; i < header.getLastCellNum(); i++) {
                String ten = fmt.formatCellValue(header.getCell(i)).trim().toLowerCase();
                if (!ten.isEmpty()) {
                    cot.putIfAbsent(ten, i);
                }
            }
            for (String tenCan : COT_EXCEL) {
                if (!cot.containsKey(tenCan.toLowerCase())) {
                    throw new IllegalArgumentException("File Excel thiếu cột: " + tenCan);
                }
            }

            Map<String, Integer> emailDaCo = new HashMap<>();
            Map<String, Integer> sdtDaCo = new HashMap<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlankRow(row, cot, fmt)) {
                    continue;
                }
                int soDong = r + 1;
                kq.setTongSoDong(kq.getTongSoDong() + 1);
                try {
                    NhanVienRequest req = docDong(row, cot, fmt, soDong, emailDaCo, sdtDaCo);
                    create(req, false);
                    kq.setSoDongThanhCong(kq.getSoDongThanhCong() + 1);
                } catch (IllegalArgumentException e) {
                    kq.setSoDongThatBai(kq.getSoDongThatBai() + 1);
                    kq.getDanhSachLoi().add("Dòng " + soDong + ": " + e.getMessage());
                } catch (RuntimeException e) {
                    log.warn("Import nhân viên dòng {} thất bại", soDong, e);
                    kq.setSoDongThatBai(kq.getSoDongThatBai() + 1);
                    kq.getDanhSachLoi().add("Dòng " + soDong + ": không lưu được dữ liệu");
                }
            }
            return kq;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Không đọc được file Excel, vui lòng dùng file .xlsx hoặc .xls từ file mẫu", e);
        }
    }

    private NhanVienRequest docDong(Row row, Map<String, Integer> cot, DataFormatter fmt, int soDong,
        Map<String, Integer> emailDaCo, Map<String, Integer> sdtDaCo) {
        NhanVienRequest req = new NhanVienRequest();
        req.setTenTaiKhoan(docText(row, cot.get(COT_EXCEL[0].toLowerCase()), fmt));
        String email = docText(row, cot.get(COT_EXCEL[1].toLowerCase()), fmt);
        if (!isBlank(email)) {
            Integer truoc = emailDaCo.putIfAbsent(email.toLowerCase(), soDong);
            if (truoc != null) {
                throw new IllegalArgumentException(
                    "Email '" + email + "' bị trùng với dòng " + truoc + " trong file");
            }
        }
        req.setEmail(email);
        String sdt = docText(row, cot.get(COT_EXCEL[2].toLowerCase()), fmt);
        if (!isBlank(sdt)) {
            Integer truoc = sdtDaCo.putIfAbsent(sdt, soDong);
            if (truoc != null) {
                throw new IllegalArgumentException(
                    "Số điện thoại '" + sdt + "' bị trùng với dòng " + truoc + " trong file");
            }
        }
        req.setSoDienThoai(sdt);
        req.setGioiTinh(docText(row, cot.get(COT_EXCEL[3].toLowerCase()), fmt));
        req.setNgaySinh(docNgaySinh(row.getCell(cot.get(COT_EXCEL[4].toLowerCase())), fmt));
        String maVaiTro = docText(row, cot.get(COT_EXCEL[5].toLowerCase()), fmt);
        if (!isBlank(maVaiTro)) {
            VaiTro vaiTro = vaiTroRepository
                .findByMaVaiTroIn(List.of(maVaiTro.trim().toUpperCase()))
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Vai trò không hợp lệ: " + maVaiTro + " (chỉ ADMIN hoặc STAFF)"));
            req.setIdVaiTro(vaiTro.getId());
        }
        req.setDiaChiCuThe(docText(row, cot.get(COT_EXCEL[6].toLowerCase()), fmt));
        req.setPhuong(docText(row, cot.get(COT_EXCEL[7].toLowerCase()), fmt));
        req.setQueQuan(docText(row, cot.get(COT_EXCEL[8].toLowerCase()), fmt));
        String trangThai = docText(row, cot.get(COT_EXCEL[9].toLowerCase()), fmt);
        req.setTrangThai(isBlank(trangThai) ? null : toTrangThai(trangThai));
        req.setNguoiCapNhat("import-excel");
        return req;
    }

    private static String docText(Row row, Integer viTri, DataFormatter fmt) {
        if (viTri == null) {
            return null;
        }
        Cell cell = row.getCell(viTri);
        return cell == null ? null : blankToNull(fmt.formatCellValue(cell));
    }

    private static LocalDate docNgaySinh(Cell cell, DataFormatter fmt) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        String text = blankToNull(fmt.formatCellValue(cell));
        if (text == null) {
            return null;
        }
        try {
            return LocalDate.parse(text, NGAY_SINH_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ, dùng dd/MM/yyyy (ví dụ 15/01/1990)");
        }
    }

    private static boolean isBlankRow(Row row, Map<String, Integer> cot, DataFormatter fmt) {
        for (Integer viTri : cot.values()) {
            Cell cell = row.getCell(viTri);
            if (cell != null && !isBlank(fmt.formatCellValue(cell))) {
                return false;
            }
        }
        return true;
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
        // anhNhanVien lưu chuỗi Base64 hoặc URL nên không giới hạn 1000 ký tự
        checkLength(req.getNguoiCapNhat(), 100, "Người cập nhật");

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
                throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng số 0 và gồm đúng 10 chữ số");
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
        // Chưa có mã trùng -> dùng mã gốc không hậu tố số (Phạm Hà Anh -> Anhph).
        if (!nhanVienRepository.existsByMaNhanVien(goc)) {
            return goc;
        }
        int so = 2;
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
