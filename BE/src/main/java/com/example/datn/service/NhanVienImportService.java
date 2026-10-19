package com.example.datn.service;

import com.example.datn.dto.NhanVien.NhanVienImportDong;
import com.example.datn.dto.NhanVien.NhanVienImportKetQuaResponse;
import com.example.datn.dto.NhanVien.NhanVienImportPreviewResponse;
import com.example.datn.dto.NhanVien.NhanVienRequest;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Import nhân viên từ tệp Excel (.xlsx/.xls).
 * Luồng: đọc tệp mới ở mỗi lần gọi (không giữ dữ liệu cũ giữa các lần),
 * xem trước có kiểm tra từng dòng rồi mới xác nhận import.
 * Mã nhân viên để trống sẽ được tự sinh theo họ tên (giống form thêm mới).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NhanVienImportService {

    private static final int SO_DONG_TOI_DA = 2000;
    private static final String HANH_DONG_THEM = "THEM";
    private static final String HANH_DONG_CAP_NHAT = "CAP_NHAT";
    private static final DateTimeFormatter NGAY_DMY = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final DateTimeFormatter NGAY_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] TIEU_DE = {
        "Mã nhân viên (trống = tự sinh)",
        "Họ và tên *",
        "Email",
        "Số điện thoại",
        "Giới tính (Nam / Nữ / Khác)",
        "Ngày sinh (dd/MM/yyyy)",
        "Vai trò (ADMIN / STAFF)",
        "Tỉnh/Thành phố",
        "Xã/Phường/Thị trấn",
        "Địa chỉ cụ thể"
    };

    /** Chuỗi chuẩn hóa (không dấu, viết thường) mà mỗi cột tiêu đề phải bắt đầu bằng. */
    private static final String[] KY_MON_TIEU_DE = {
        "ma nhan vien", "ho va ten", "email", "so dien thoai", "gioi tinh",
        "ngay sinh", "vai tro", "tinh", "xa", "dia chi"
    };

    private final NhanVienService nhanVienService;
    private final NhanVienRepository nhanVienRepository;
    private final VaiTroRepository vaiTroRepository;

    public byte[] taoFileMau() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Nhan vien");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < TIEU_DE.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(TIEU_DE[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.max(20, TIEU_DE[i].length() + 6) * 256);
            }

            Sheet huongDan = workbook.createSheet("Hướng dẫn");
            String[] ghiChu = {
                "Cách điền tệp nhập nhân viên:",
                "1. Chỉ điền dữ liệu từ dòng thứ 2 trở đi, không đổi thứ tự cột.",
                "2. Họ và tên là bắt buộc. Mã nhân viên để trống nếu muốn hệ thống tự sinh theo họ tên.",
                "3. Khi thêm mới: email, số điện thoại, ngày sinh, giới tính, vai trò là bắt buộc.",
                "4. Mã nhân viên đã tồn tại: dòng đó sẽ CẬP NHẬT nhân viên đó, ô trống giữ nguyên dữ liệu cũ.",
                "5. Vai trò nhập ADMIN hoặc STAFF (không phân biệt hoa thường) hoặc tên vai trò.",
                "6. Ngày sinh định dạng dd/MM/yyyy (ví dụ 15/03/1990).",
                "7. Mỗi lần nhập tối đa " + SO_DONG_TOI_DA + " dòng dữ liệu."
            };
            for (int i = 0; i < ghiChu.length; i++) {
                huongDan.createRow(i).createCell(0).setCellValue(ghiChu[i]);
            }
            huongDan.setColumnWidth(0, 100 * 256);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalArgumentException("Không tạo được tệp Excel mẫu: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public NhanVienImportPreviewResponse xemTruoc(MultipartFile file) {
        List<DongPhanTich> rows = docDong(file);
        List<VaiTro> danhSachVaiTro = vaiTroRepository.findAll();
        NhanVienImportPreviewResponse preview = NhanVienImportPreviewResponse.builder().build();
        Set<String> maDaXuatHien = new HashSet<>();

        for (DongPhanTich r : rows) {
            preview.setTongSoDong(preview.getTongSoDong() + 1);
            NhanVienImportDong dong = toImportDong(r);
            try {
                Long idHienTai = timIdTheoMa(r, maDaXuatHien);
                NhanVienRequest req = taoRequest(r, idHienTai, danhSachVaiTro);
                nhanVienService.kiemTraHopLe(req, idHienTai);
                dong.setHanhDong(idHienTai != null ? HANH_DONG_CAP_NHAT : HANH_DONG_THEM);
            } catch (IllegalArgumentException ex) {
                dong.setLyDoLoi(ex.getMessage());
            }

            if (dong.getLyDoLoi() == null) {
                preview.setDongHopLe(preview.getDongHopLe() + 1);
                if (HANH_DONG_THEM.equals(dong.getHanhDong())) {
                    preview.setThemMoiDuKien(preview.getThemMoiDuKien() + 1);
                } else {
                    preview.setCapNhatDuKien(preview.getCapNhatDuKien() + 1);
                }
            } else {
                preview.setDongLoi(preview.getDongLoi() + 1);
                preview.getLoi().add("Dòng " + r.soDong + ": " + dong.getLyDoLoi());
            }
            preview.getDong().add(dong);
        }
        return preview;
    }

    /**
     * Xác nhận import: đọc lại trực tiếp từ tệp được gửi lên (không dùng dữ liệu đã phân tích trước),
     * từng dòng xử lý độc lập nên dòng lỗi không ảnh hưởng dòng khác.
     */
    public NhanVienImportKetQuaResponse xacNhan(MultipartFile file) {
        List<DongPhanTich> rows = docDong(file);
        List<VaiTro> danhSachVaiTro = vaiTroRepository.findAll();
        NhanVienImportKetQuaResponse result = NhanVienImportKetQuaResponse.builder().build();
        Set<String> maDaXuatHien = new HashSet<>();

        for (DongPhanTich r : rows) {
            try {
                Long idHienTai = timIdTheoMa(r, maDaXuatHien);
                NhanVienRequest req = taoRequest(r, idHienTai, danhSachVaiTro);
                if (idHienTai != null) {
                    nhanVienService.update(idHienTai, req);
                    result.setCapNhatThanhCong(result.getCapNhatThanhCong() + 1);
                } else {
                    // import hàng loạt không gửi email cấp tài khoản cho từng nhân viên
                    nhanVienService.create(req, false);
                    result.setThemThanhCong(result.getThemThanhCong() + 1);
                }
            } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
                result.setThatBai(result.getThatBai() + 1);
                String lyDo = ex.getMessage() != null ? ex.getMessage() : "Lỗi dữ liệu không xác định";
                result.getChiTietLoi().add("Dòng " + r.soDong + ": " + lyDo);
            }
        }
        return result;
    }

    /**
     * Tìm id nhân viên theo mã; đồng thời phát hiện mã lặp trong cùng tệp.
     * Trả về null nếu mã trống (dòng sẽ được thêm mới).
     */
    private Long timIdTheoMa(DongPhanTich r, Set<String> maDaXuatHien) {
        if (r.maNhanVien == null || r.maNhanVien.isBlank()) {
            return null;
        }
        String ma = r.maNhanVien.trim();
        if (!maDaXuatHien.add(ma.toUpperCase())) {
            throw new IllegalArgumentException("Mã nhân viên '" + ma + "' bị lặp lại trong tệp");
        }
        return nhanVienRepository.findByMaNhanVien(ma).map(NhanVien::getId).orElse(null);
    }

    private NhanVienImportDong toImportDong(DongPhanTich r) {
        // ngày sinh không hợp lệ: để trống ở đây, lỗi sẽ do taoRequest báo đúng từng dòng
        LocalDate ngaySinh = null;
        try {
            ngaySinh = docNgaySinh(r);
        } catch (IllegalArgumentException ex) {
            ngaySinh = null;
        }
        return NhanVienImportDong.builder()
            .dong(r.soDong)
            .maNhanVien(r.maNhanVien)
            .tenTaiKhoan(r.tenTaiKhoan)
            .email(r.email)
            .soDienThoai(r.soDienThoai)
            .gioiTinh(r.gioiTinh)
            .ngaySinh(ngaySinh)
            .vaiTro(r.vaiTroText)
            .queQuan(r.queQuan)
            .phuong(r.phuong)
            .diaChiCuThe(r.diaChiCuThe)
            .build();
    }

    /**
     * Dựng request từ dòng Excel. Với dòng cập nhật, các ô trống giữ nguyên dữ liệu hiện tại
     * (bao gồm ảnh đại diện) để không xóa mất thông tin cũ.
     */
    private NhanVienRequest taoRequest(DongPhanTich r, Long idHienTai, List<VaiTro> danhSachVaiTro) {
        NhanVienRequest req = new NhanVienRequest();
        req.setTenTaiKhoan(r.tenTaiKhoan);
        req.setMaNhanVien(r.maNhanVien == null || r.maNhanVien.isBlank() ? null : r.maNhanVien.trim());
        req.setEmail(blankToNull(r.email));
        req.setSoDienThoai(blankToNull(r.soDienThoai));
        req.setGioiTinh(blankToNull(r.gioiTinh));
        req.setNgaySinh(docNgaySinh(r));
        req.setQueQuan(blankToNull(r.queQuan));
        req.setPhuong(blankToNull(r.phuong));
        req.setDiaChiCuThe(blankToNull(r.diaChiCuThe));
        req.setIdVaiTro(resolveIdVaiTro(r.vaiTroText, danhSachVaiTro));
        req.setNguoiCapNhat("import-excel");

        if (idHienTai != null) {
            NhanVien cu = nhanVienRepository.findById(idHienTai)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên theo mã"));
            if (isBlank(req.getEmail())) {
                req.setEmail(cu.getEmail());
            }
            if (isBlank(req.getSoDienThoai())) {
                req.setSoDienThoai(cu.getSoDienThoai());
            }
            if (isBlank(req.getGioiTinh())) {
                req.setGioiTinh(cu.getGioiTinh());
            }
            if (req.getNgaySinh() == null) {
                req.setNgaySinh(cu.getNgaySinh());
            }
            if (isBlank(req.getQueQuan())) {
                req.setQueQuan(cu.getQueQuan());
            }
            if (isBlank(req.getPhuong())) {
                req.setPhuong(cu.getPhuong());
            }
            if (isBlank(req.getDiaChiCuThe())) {
                req.setDiaChiCuThe(cu.getDiaChiCuThe());
            }
            req.setAnhNhanVien(cu.getAnhNhanVien());
            if (req.getIdVaiTro() == null && cu.getVaiTro() != null) {
                req.setIdVaiTro(cu.getVaiTro().getId());
            }
            req.setTrangThai(cu.getTrangThai());
        }
        return req;
    }

    private Long resolveIdVaiTro(String text, List<VaiTro> danhSachVaiTro) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();
        for (VaiTro vt : danhSachVaiTro) {
            if (vt.getMaVaiTro() != null && vt.getMaVaiTro().equalsIgnoreCase(s)) {
                return vt.getId();
            }
        }
        for (VaiTro vt : danhSachVaiTro) {
            if (vt.getTenVaiTro() != null && vt.getTenVaiTro().equalsIgnoreCase(s)) {
                return vt.getId();
            }
        }
        throw new IllegalArgumentException(
            "Vai trò '" + text + "' không hợp lệ (chỉ hỗ trợ ADMIN hoặc STAFF)");
    }

    private LocalDate docNgaySinh(DongPhanTich r) {
        if (r.ngaySinhDate != null) {
            return r.ngaySinhDate;
        }
        if (r.ngaySinhText == null || r.ngaySinhText.isBlank()) {
            return null;
        }
        String s = r.ngaySinhText.trim();
        try {
            if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(s, NGAY_ISO);
            }
            return LocalDate.parse(s, NGAY_DMY);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                "Ngày sinh '" + s + "' không hợp lệ (định dạng dd/MM/yyyy)");
        }
    }

    private List<DongPhanTich> docDong(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp Excel nhân viên");
        }
        String fileName = file.getOriginalFilename() != null
            ? file.getOriginalFilename().toLowerCase()
            : "";
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new IllegalArgumentException("Hệ thống chỉ chấp nhận tệp Excel (.xlsx, .xls)");
        }

        List<DongPhanTich> list = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new IllegalArgumentException("Tệp Excel không có dữ liệu để nhập");
            }
            DataFormatter formatter = new DataFormatter();

            // Hàng tiêu đề phải đúng mẫu 10 cột theo đúng thứ tự — nếu sai/thiếu thì báo rõ
            // thay vì âm thầm đọc nhầm dữ liệu theo vị trí cột.
            com.example.datn.util.ExcelTieuDeUtil.kiemTraTieuDe(sheet, formatter, KY_MON_TIEU_DE, TIEU_DE);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                if (list.size() >= SO_DONG_TOI_DA) {
                    throw new IllegalArgumentException(
                        "Mỗi lần nhập tối đa " + SO_DONG_TOI_DA + " dòng dữ liệu");
                }
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                DongPhanTich d = new DongPhanTich();
                d.soDong = r + 1;
                d.maNhanVien = cellText(row.getCell(0), formatter);
                d.tenTaiKhoan = cellText(row.getCell(1), formatter);
                d.email = cellText(row.getCell(2), formatter);
                d.soDienThoai = cellText(row.getCell(3), formatter);
                d.gioiTinh = cellText(row.getCell(4), formatter);
                Cell cellNgay = row.getCell(5);
                if (cellNgay != null && cellNgay.getCellType() == CellType.NUMERIC
                    && DateUtil.isCellDateFormatted(cellNgay)) {
                    d.ngaySinhDate = cellNgay.getDateCellValue().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                } else {
                    d.ngaySinhText = cellText(cellNgay, formatter);
                }
                d.vaiTroText = cellText(row.getCell(6), formatter);
                d.queQuan = cellText(row.getCell(7), formatter);
                d.phuong = cellText(row.getCell(8), formatter);
                d.diaChiCuThe = cellText(row.getCell(9), formatter);

                // dòng hoàn toàn trống -> bỏ qua, không tính vào số dòng import
                if (isBlank(d.maNhanVien) && isBlank(d.tenTaiKhoan) && isBlank(d.email)
                    && isBlank(d.soDienThoai)) {
                    continue;
                }
                list.add(d);
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Đọc tệp Excel nhân viên thất bại: {}", ex.toString(), ex);
            throw new IllegalArgumentException("Không đọc được tệp Excel: " + ex.getMessage(), ex);
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Tệp Excel không có dòng dữ liệu để nhập");
        }
        return list;
    }

    private static String cellText(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            return BigDecimal.valueOf(cell.getNumericCellValue())
                .stripTrailingZeros()
                .toPlainString();
        }
        return formatter.formatCellValue(cell).trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static class DongPhanTich {
        int soDong;
        String maNhanVien;
        String tenTaiKhoan;
        String email;
        String soDienThoai;
        String gioiTinh;
        LocalDate ngaySinhDate;
        String ngaySinhText;
        String vaiTroText;
        String queQuan;
        String phuong;
        String diaChiCuThe;
    }
}
