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
import com.example.datn.entity.LichSuHoaDon;
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
import com.example.datn.util.PhieuGiamGiaUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoaDonService {

    // ===== rule validation tung field (khong dung mot regex cho tat ca) =====
    // Ten: chu Unicode + khoang trang + . ' - (KHONG chua so, khong cho @ # $ % va ky tu dac biet)
    private static final Pattern TEN_KHACH_HANG_HOP_LE =
        Pattern.compile("^[\\p{L}\\s.'-]+$");
    // So dien thoai: chi so, bat dau 0, 10-11 chu so (theo du lieu hien tai cua project)
    private static final Pattern SO_DIEN_THOAI_HOP_LE =
        Pattern.compile("^0\\d{9,10}$");
    // Dia chi: chu + so + khoang trang + / - . , (cac ky tu dia chi thuc te)
    private static final Pattern DIA_CHI_HOP_LE =
        Pattern.compile("^[\\p{L}\\p{N}\\s.,/-]+$");
    // Ghi chu: text tu do nhung khong chua ki tu dieu khien (tru \n \t \r)
    private static final Pattern KY_TU_CONTROL =
        Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]");

    private static final int MA_TOI_DA = 50;
    private static final int TEN_TOI_DA = 150;
    private static final int DIA_CHI_TOI_DA = 500;
    private static final int GHI_CHU_TOI_DA = 1000;

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

    private static final Map<String, List<String>> TRANSITION_MAP = new HashMap<>();
    static {
        TRANSITION_MAP.put("CHO_XAC_NHAN", List.of("DA_XAC_NHAN", "DA_HUY"));
        TRANSITION_MAP.put("DA_XAC_NHAN", List.of("CHO_GIAO_HANG", "DA_HUY"));
        TRANSITION_MAP.put("CHO_GIAO_HANG", List.of("DANG_GIAO_HANG", "DA_HUY"));
        TRANSITION_MAP.put("DANG_GIAO_HANG", List.of("DA_GIAO_HANG"));
        TRANSITION_MAP.put("DA_GIAO_HANG", List.of("DA_HOAN_THANH"));
        TRANSITION_MAP.put("DA_HOAN_THANH", List.of("DA_HOAN_TIEN"));
        TRANSITION_MAP.put("DA_HUY", List.of());
        TRANSITION_MAP.put("DA_HOAN_TIEN", List.of());
    }

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final LichSuSuDungPhieuGiamGiaRepository lichSuSuDungRepository;

    @Transactional(readOnly = true)
    public PageResponse<HoaDonDTO> list(
        String ma, LocalDate tuNgay, LocalDate denNgay, String loaiDon, String trangThai, boolean daXoa, int page, int size
    ) {
        BoLoc loc = chuanHoaBoLoc(ma, tuNgay, denNgay, loaiDon, trangThai);
        var result = hoaDonRepository.findByFilters(
            loc.ma(), loc.tu(), loc.den(), loc.loaiDon(), loc.trangThai(), daXoa,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 10))
        );
        return PageResponse.from(result.map(this::toDTO));
    }

    private record BoLoc(String ma, LocalDateTime tu, LocalDateTime den, String loaiDon, String trangThai) {}

    private BoLoc chuanHoaBoLoc(String ma, LocalDate tuNgay, LocalDate denNgay, String loaiDon, String trangThai) {
        if (ma != null && ma.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Mã hóa đơn không được chứa dấu cách");
        }
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
        return new BoLoc(blankToNull(ma), tu, den, maLoaiDon, maTrangThai);
    }

    private static final String[] COT_EXCEL = {
        "STT", "Mã hóa đơn", "Tên khách hàng", "Số điện thoại", "Địa chỉ nhận hàng",
        "Loại hóa đơn", "Ngày tạo", "Tổng số lượng sản phẩm", "Tổng tiền hàng",
        "Tiền giảm giá", "Phí vận chuyển", "Tổng thanh toán", "Phương thức thanh toán",
        "Trạng thái thanh toán", "Trạng thái hóa đơn", "Ghi chú"
    };

    @Transactional(readOnly = true)
    public byte[] xuatExcel(String ma, LocalDate tuNgay, LocalDate denNgay, String loaiDon, String trangThai) {
        BoLoc loc = chuanHoaBoLoc(ma, tuNgay, denNgay, loaiDon, trangThai);
        List<HoaDon> ds = hoaDonRepository.findAllByFilters(
            loc.ma(), loc.tu(), loc.den(), loc.loaiDon(), loc.trangThai(), false);
        if (ds.isEmpty()) {
            throw new IllegalArgumentException("Không có hóa đơn nào khớp với bộ lọc hiện tại");
        }
        Map<Long, Long> soLuongTheoHoaDon = new HashMap<>();
        List<Long> ids = ds.stream().map(HoaDon::getId).toList();
        for (Object[] row : chiTietHoaDonRepository.tongSoLuongTheoHoaDon(ids)) {
            soLuongTheoHoaDon.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Danh sách hóa đơn");

            org.apache.poi.ss.usermodel.Font fontHeader = wb.createFont();
            fontHeader.setBold(true);
            fontHeader.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            fontHeader.setFontName("Calibri");
            fontHeader.setFontHeightInPoints((short) 11);

            org.apache.poi.xssf.usermodel.XSSFCellStyle styleHeader =
                wb.createCellStyle();
            styleHeader.setFont(fontHeader);
            styleHeader.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                new byte[]{0x1F, 0x29, 0x37}, null));
            styleHeader.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            styleHeader.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            styleHeader.setWrapText(true);

            org.apache.poi.ss.usermodel.CellStyle styleNgay = wb.createCellStyle();
            org.apache.poi.ss.usermodel.DataFormat fmtNgay = wb.createDataFormat();
            styleNgay.setDataFormat(fmtNgay.getFormat("dd/MM/yyyy HH:mm"));

            org.apache.poi.ss.usermodel.CellStyle styleTien = wb.createCellStyle();
            org.apache.poi.ss.usermodel.DataFormat fmtTien = wb.createDataFormat();
            styleTien.setDataFormat(fmtTien.getFormat("#,##0"));

            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < COT_EXCEL.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = header.createCell(i);
                cell.setCellValue(COT_EXCEL[i]);
                cell.setCellStyle(styleHeader);
            }

            int rowNum = 1;
            for (HoaDon h : ds) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum);
                BigDecimal phiVanChuyen = h.getPhiVanChuyen() != null ? h.getPhiVanChuyen() : BigDecimal.ZERO;
                BigDecimal tongTien = h.getTongTien() != null ? h.getTongTien() : BigDecimal.ZERO;
                BigDecimal tienGiam = h.getTienGiam() != null ? h.getTienGiam() : BigDecimal.ZERO;
                BigDecimal tongThanhToan = h.getTienSauGiamGia() != null ? h.getTienSauGiamGia() : BigDecimal.ZERO;
                BigDecimal tienHang = tongTien.subtract(phiVanChuyen);

                row.createCell(0).setCellValue(rowNum);
                row.createCell(1).setCellValue(h.getMaHoaDon() != null ? h.getMaHoaDon() : "");
                row.createCell(2).setCellValue(h.getTenKhachHang() != null ? h.getTenKhachHang() : "");
                row.createCell(3).setCellValue(h.getSoDienThoaiKhachHang() != null ? h.getSoDienThoaiKhachHang() : "");
                row.createCell(4).setCellValue(h.getDiaChiNhanHang() != null ? h.getDiaChiNhanHang() : "");
                row.createCell(5).setCellValue(toTenLoaiDon(h.getLoaiDon()));

                org.apache.poi.ss.usermodel.Cell cellNgay = row.createCell(6);
                if (h.getNgayTao() != null) {
                    cellNgay.setCellValue(org.apache.poi.ss.usermodel.DateUtil.getExcelDate(
                        java.util.Date.from(h.getNgayTao().atZone(java.time.ZoneId.systemDefault()).toInstant())));
                    cellNgay.setCellStyle(styleNgay);
                }

                Long tongSl = soLuongTheoHoaDon.getOrDefault(h.getId(), 0L);
                row.createCell(7).setCellValue(tongSl.doubleValue());

                org.apache.poi.ss.usermodel.Cell cellTienHang = row.createCell(8);
                cellTienHang.setCellValue(tienHang.doubleValue());
                cellTienHang.setCellStyle(styleTien);
                org.apache.poi.ss.usermodel.Cell cellGiam = row.createCell(9);
                cellGiam.setCellValue(tienGiam.doubleValue());
                cellGiam.setCellStyle(styleTien);
                org.apache.poi.ss.usermodel.Cell cellShip = row.createCell(10);
                cellShip.setCellValue(phiVanChuyen.doubleValue());
                cellShip.setCellStyle(styleTien);
                org.apache.poi.ss.usermodel.Cell cellTong = row.createCell(11);
                cellTong.setCellValue(tongThanhToan.doubleValue());
                cellTong.setCellStyle(styleTien);

                row.createCell(12).setCellValue(
                    h.getPhuongThucThanhToan() != null ? h.getPhuongThucThanhToan().getTenPhuongThuc() : "");
                row.createCell(13).setCellValue(tinhTrangThaiThanhToan(h.getTrangThai()));
                row.createCell(14).setCellValue(toTenTrangThai(h.getTrangThai()));
                row.createCell(15).setCellValue(h.getGhiChu() != null ? h.getGhiChu() : "");
                rowNum++;
            }

            int[] rong = {6, 16, 26, 14, 40, 12, 17, 13, 15, 13, 14, 15, 20, 17, 17, 30};
            for (int i = 0; i < rong.length; i++) {
                sheet.setColumnWidth(i, rong[i] * 256);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                0, 0, 0, COT_EXCEL.length - 1));

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Không tạo được file Excel: " + e.getMessage(), e);
        }
    }

    private static String tinhTrangThaiThanhToan(String maTrangThai) {
        if ("DA_HOAN_THANH".equals(maTrangThai) || "DA_GIAO_HANG".equals(maTrangThai)) {
            return "Đã thanh toán";
        }
        if ("DA_HOAN_TIEN".equals(maTrangThai)) {
            return "Đã hoàn tiền";
        }
        return "Chưa thanh toán";
    }

    @Transactional(readOnly = true)
    public HoaDonDTO get(Long id) {
        return toFullDTO(findOrThrow(id));
    }

    @Transactional
    public HoaDonDTO create(HoaDonRequest req) {
        validateHeader(req, null);
        PhieuGiamGia phieu = khoaVaKiemTraCoBan(req);
        if (phieu != null) {
            kiemTraGioiHanSuDung(phieu, req.getIdKhachHang(), null);
        }
        HoaDon hoaDon = new HoaDon();
        fillHeader(hoaDon, req);
        List<DongTinh> lines = tinhToanVaLuu(hoaDon, req);
        hoaDon = hoaDonRepository.save(hoaDon);
        luuCacDong(hoaDon, lines, req);
        if (phieu != null) {
            ghiLichSuSuDung(hoaDon, phieu);
        }
        return toFullDTO(hoaDon);
    }

    @Transactional
    public HoaDonDTO update(Long id, HoaDonRequest req) {
        HoaDon hoaDon = findForUpdateOrThrow(id);
        if (!"CHO_XAC_NHAN".equals(hoaDon.getTrangThai())) {
            throw new IllegalArgumentException(
                "Chỉ hóa đơn Chờ xác nhận mới được sửa (trạng thái hiện tại: " + toTenTrangThai(hoaDon.getTrangThai()) + ")"
            );
        }
        validateHeader(req, id);
        if (!req.getMaHoaDon().trim().equals(hoaDon.getMaHoaDon())) {
            throw new IllegalArgumentException("Không được thay đổi mã hóa đơn");
        }
        if (!isBlank(req.getTrangThai()) && !hoaDon.getTrangThai().equals(toMaTrangThai(req.getTrangThai()))) {
            throw new IllegalArgumentException(
                "Hãy sử dụng PUT /api/hoa-don/{id}/trang-thai để đổi trạng thái hóa đơn"
            );
        }
        fillHeader(hoaDon, req);
        PhieuGiamGia phieu = khoaVaKiemTraCoBan(req);
        lichSuSuDungRepository.deleteByHoaDonId(hoaDon.getId());
        if (phieu != null) {
            kiemTraGioiHanSuDung(phieu, req.getIdKhachHang(), hoaDon.getId());
        }
        List<DongTinh> lines = tinhToanVaLuu(hoaDon, req);
        chiTietHoaDonRepository.deleteByHoaDonId(hoaDon.getId());
        luuCacDong(hoaDon, lines, req);
        if (phieu != null) {
            ghiLichSuSuDung(hoaDon, phieu);
        }
        return toFullDTO(hoaDon);
    }

    @Transactional
    public void delete(Long id) {
        HoaDon hoaDon = findOrThrow(id);
        String trangThai = hoaDon.getTrangThai();
        if (!"CHO_XAC_NHAN".equals(trangThai) && !"DA_HUY".equals(trangThai)) {
            throw new IllegalArgumentException(
                "Chỉ hóa đơn Chờ xác nhận hoặc Đã hủy mới được xóa (trạng thái hiện tại: " + toTenTrangThai(trangThai) + ")"
            );
        }
        hoaDon.setDaXoa(true);
        hoaDon.setNguoiCapNhat("admin");
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepository.save(hoaDon);
    }

    @Transactional
    public HoaDonDTO restore(Long id) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn #" + id));
        if (!hoaDon.isDaXoa()) {
            throw new IllegalArgumentException("Hóa đơn " + hoaDon.getMaHoaDon() + " chưa bị xóa");
        }
        hoaDon.setDaXoa(false);
        hoaDon.setNguoiCapNhat("admin");
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        return toFullDTO(hoaDonRepository.save(hoaDon));
    }

    @Transactional
    public HoaDonDTO doiTrangThai(Long id, String trangThaiMoi) {
        HoaDon hoaDon = findOrThrow(id);
        if (isBlank(trangThaiMoi)) {
            throw new IllegalArgumentException("Trạng thái mới không được để trống");
        }
        String maMoi = toMaTrangThai(trangThaiMoi);
        String maHienTai = hoaDon.getTrangThai();
        if (maMoi.equals(maHienTai)) {
            throw new IllegalArgumentException("Trạng thái không thay đổi");
        }
        List<String> choPhep = TRANSITION_MAP.getOrDefault(maHienTai, List.of());
        if (!choPhep.contains(maMoi)) {
            throw new IllegalArgumentException(
                "Không được chuyển từ " + toTenTrangThai(maHienTai) + " sang " + toTenTrangThai(maMoi)
            );
        }
        hoaDon.setTrangThai(maMoi);
        hoaDon.setNguoiCapNhat("admin");
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        HoaDon saved = hoaDonRepository.save(hoaDon);
        ghiLichSuTrangThai(saved, maHienTai, maMoi);
        return toFullDTO(saved);
    }

    private void ghiLichSuTrangThai(HoaDon hoaDon, String tuMa, String denMa) {
        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(denMa);
        lichSu.setHanhDong("CHUYEN_TRANG_THAI");
        lichSu.setGhiChu("Chuyển từ " + toTenTrangThai(tuMa) + " sang " + toTenTrangThai(denMa));
        lichSuHoaDonRepository.save(lichSu);
    }

    @Transactional(readOnly = true)
    public FormDataResponse formData() {
        List<FormDataResponse.KhachHangOption> khachHangs = khachHangRepository.findAll().stream()
            .map(k -> new FormDataResponse.KhachHangOption(
                k.getId(), k.getTenKhachHang(), k.getSoDienThoai(), k.getTrangThai()
            ))
            .toList();
        List<FormDataResponse.NhanVienOption> nhanViens = nhanVienRepository.findAll().stream()
            .map(n -> new FormDataResponse.NhanVienOption(n.getId(), n.getTenTaiKhoan()))
            .toList();
        List<FormDataResponse.PhuongThucOption> phuongThucs = phuongThucThanhToanRepository.findAll().stream()
            .map(p -> new FormDataResponse.PhuongThucOption(p.getId(), p.getTenPhuongThuc()))
            .toList();
        List<FormDataResponse.PhieuGiamGiaOption> phieuGiamGias = phieuGiamGiaRepository.findAll().stream()
            .map(p -> new FormDataResponse.PhieuGiamGiaOption(
                p.getId(), p.getMaPhieuGiamGia(),
                PhieuGiamGiaUtil.tenHienThi(p.getLoaiGiamGia(), p.getGiaTriGiam()),
                p.getLoaiGiamGia(),
                p.getGiaTriGiam(), p.getGiamToiDa(), p.getHoaDonToiThieu(),
                p.getNgayBatDau(), p.getNgayKetThuc(), p.getSoLuong(), p.getSoLuongDaSuDung(),
                p.getGioiHanMoiTaiKhoan(), p.getTrangThai()
            ))
            .toList();
        List<FormDataResponse.SanPhamChiTietOption> spcts = sanPhamChiTietRepository.findAll().stream()
            .map(s -> new FormDataResponse.SanPhamChiTietOption(
                s.getId(), s.getMaChiTietSanPham(),
                s.getSanPham() != null ? s.getSanPham().getTenSanPham() : "",
                s.getKichCo() != null ? s.getKichCo().getTenKichCo() : "",
                s.getMauSac() != null ? s.getMauSac().getTenMau() : "",
                s.getGiaBan(), s.getSoLuong(),
                s.getTrangThai(),
                s.getSanPham() != null ? s.getSanPham().getTrangThai() : null
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
        if (ma.length() > MA_TOI_DA) {
            throw new IllegalArgumentException("Mã hóa đơn quá độ dài tối đa (" + MA_TOI_DA + " ký tự)");
        }
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
        if (req.getIdPhuongThucThanhToan() == null) {
            throw new IllegalArgumentException("Phương thức thanh toán không được để trống");
        }
        if (isBlank(req.getTrangThai())) {
            throw new IllegalArgumentException("Trạng thái không được để trống");
        }
        toMaTrangThai(req.getTrangThai());
        if (req.getIdNhanVien() == null) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên");
        }
        if (req.getPhiVanChuyen() == null) {
            throw new IllegalArgumentException("Phí vận chuyển không được để trống");
        }
        if (req.getPhiVanChuyen().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí vận chuyển không được âm");
        }
        if (req.getPhiVanChuyen().compareTo(new BigDecimal("1000")) <= 0) {
            throw new IllegalArgumentException("Phí vận chuyển phải lớn hơn 1000");
        }
        validateText(req);
        if (req.getChiTiet() == null || req.getChiTiet().isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn phải có ít nhất 1 sản phẩm");
        }
    }

    /**
     * Validate text tung field theo rule rieng (trim truoc, khong dung mot regex cho tat ca).
     * Field tuy chon ma de trang/space -> bo qua (khong luu khoang trang).
     */
    private void validateText(HoaDonRequest req) {
        if (isBlank(req.getTenKhachHang())) {
            throw new IllegalArgumentException("Tên khách hàng không được để trống");
        }
        String ten = req.getTenKhachHang().trim();
        if (ten.length() > TEN_TOI_DA) {
            throw new IllegalArgumentException(
                "Tên khách hàng quá độ dài tối đa (" + TEN_TOI_DA + " ký tự)");
        }
        if (!TEN_KHACH_HANG_HOP_LE.matcher(ten).matches()) {
            throw new IllegalArgumentException("Tên khách hàng chứa ký tự không hợp lệ");
        }
        if (isBlank(req.getSoDienThoaiKhachHang())) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }
        String sdt = req.getSoDienThoaiKhachHang().trim();
        if (!SO_DIEN_THOAI_HOP_LE.matcher(sdt).matches()) {
            throw new IllegalArgumentException(
                "Số điện thoại phải gồm 10 hoặc 11 chữ số và bắt đầu bằng 0");
        }
        boolean online = "ONLINE".equals(toMaLoaiDon(req.getLoaiDon()));
        if (isBlank(req.getDiaChiNhanHang())) {
            if (online) {
                throw new IllegalArgumentException("Đơn online phải có địa chỉ nhận hàng");
            }
        } else {
            String diaChi = req.getDiaChiNhanHang().trim();
            if (diaChi.length() > DIA_CHI_TOI_DA) {
                throw new IllegalArgumentException(
                    "Địa chỉ nhận hàng quá độ dài tối đa (" + DIA_CHI_TOI_DA + " ký tự)");
            }
            if (!DIA_CHI_HOP_LE.matcher(diaChi).matches()) {
                throw new IllegalArgumentException("Địa chỉ nhận hàng chứa ký tự không hợp lệ");
            }
        }
        if (!isBlank(req.getGhiChu())) {
            String ghiChu = req.getGhiChu().trim();
            if (ghiChu.length() > GHI_CHU_TOI_DA) {
                throw new IllegalArgumentException(
                    "Ghi chú quá độ dài tối đa (" + GHI_CHU_TOI_DA + " ký tự)");
            }
            if (KY_TU_CONTROL.matcher(ghiChu).find()) {
                throw new IllegalArgumentException("Ghi chú chứa ký tự không hợp lệ");
            }
        }
        if (req.getChiTiet() != null) {
            for (ChiTietHoaDonRequest d : req.getChiTiet()) {
                if (d != null && !isBlank(d.getGhiChu())
                    && d.getGhiChu().trim().length() > GHI_CHU_TOI_DA) {
                    throw new IllegalArgumentException(
                        "Ghi chú dòng sản phẩm quá độ dài tối đa (" + GHI_CHU_TOI_DA + " ký tự)");
                }
            }
        }
    }

    private void fillHeader(HoaDon hoaDon, HoaDonRequest req) {
        hoaDon.setMaHoaDon(req.getMaHoaDon().trim());
        hoaDon.setLoaiDon(toMaLoaiDon(req.getLoaiDon()));
        hoaDon.setPhiVanChuyen(req.getPhiVanChuyen() != null ? req.getPhiVanChuyen() : BigDecimal.ZERO);
        hoaDon.setTenKhachHang(blankToNull(req.getTenKhachHang()));
        hoaDon.setSoDienThoaiKhachHang(blankToNull(req.getSoDienThoaiKhachHang()));
        hoaDon.setDiaChiNhanHang(blankToNull(req.getDiaChiNhanHang()));
        hoaDon.setTrangThai(toMaTrangThai(req.getTrangThai()));
        hoaDon.setGhiChu(blankToNull(req.getGhiChu()));
        hoaDon.setNguoiCapNhat("admin");
        hoaDon.setNgayCapNhat(LocalDateTime.now());

        hoaDon.setKhachHang(req.getIdKhachHang() != null
            ? timKhachHang(req.getIdKhachHang())
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

    private KhachHang timKhachHang(Long id) {
        KhachHang kh = khachHangRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
        if (Boolean.FALSE.equals(kh.getTrangThai())) {
            throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa");
        }
        return kh;
    }

    private record DongTinh(SanPhamChiTiet spct, ChiTietHoaDonRequest req, BigDecimal thanhTien) {}

    private List<DongTinh> tinhToanVaLuu(HoaDon hoaDon, HoaDonRequest req) {
        List<DongTinh> dong = new ArrayList<>();
        Set<String> maDaCo = new HashSet<>();
        for (ChiTietHoaDonRequest d : req.getChiTiet()) {
            if (d.getIdSanPhamChiTiet() == null) {
                throw new IllegalArgumentException("Từng dòng chi tiết phải chọn sản phẩm");
            }
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(d.getIdSanPhamChiTiet())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chi tiết không tồn tại"));
            boolean spctNgungBan = Boolean.FALSE.equals(spct.getTrangThai());
            boolean spNgungBan = spct.getSanPham() != null
                && Boolean.FALSE.equals(spct.getSanPham().getTrangThai());
            if (spctNgungBan || spNgungBan) {
                throw new IllegalArgumentException(
                    "Sản phẩm " + spct.getMaChiTietSanPham() + " đã ngừng bán"
                );
            }
            if (!maDaCo.add(spct.getMaChiTietSanPham())) {
                throw new IllegalArgumentException(
                    "Sản phẩm " + spct.getMaChiTietSanPham() + " bị trùng ở nhiều dòng chi tiết"
                );
            }
            if (d.getSoLuong() == null || d.getSoLuong() < 1) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            }
            if (spct.getSoLuong() != null && d.getSoLuong() > spct.getSoLuong()) {
                throw new IllegalArgumentException(
                    "Sản phẩm " + spct.getMaChiTietSanPham() + " chỉ còn " + spct.getSoLuong() + " trong kho"
                );
            }
            if (d.getDonGia() == null || d.getDonGia().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Đơn giá phải lớn hơn 0");
            }
            BigDecimal donGia = d.getDonGia();
            BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(d.getSoLuong()));
            dong.add(new DongTinh(spct, d, thanhTien));
        }

        BigDecimal tong = dong.stream()
            .map(DongTinh::thanhTien)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(hoaDon.getPhiVanChuyen());
        hoaDon.setTongTien(tong);
        BigDecimal giam = tinhGiamGia(hoaDon);
        hoaDon.setTienGiam(giam);
        hoaDon.setTienSauGiamGia(tong.subtract(giam));
        PhieuGiamGia phieu = hoaDon.getPhieuGiamGia();
        if (phieu != null) {
            hoaDon.setDotGiamGia(phieu.getDotGiamGia());
            hoaDon.setTenGiamGiaUngDung(
                PhieuGiamGiaUtil.tenHienThi(phieu.getLoaiGiamGia(), phieu.getGiaTriGiam())
            );
        } else {
            hoaDon.setDotGiamGia(null);
            hoaDon.setTenGiamGiaUngDung(null);
        }
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

    /**
     * Khoa dong phieu (PESSIMISTIC_WRITE) de 2 request dong thoi khong cung vuot gioi han,
     * sau do kiem tra: ton tai, dang hoat dong, tai khoan hop le.
     */
    private PhieuGiamGia khoaVaKiemTraCoBan(HoaDonRequest req) {
        if (req.getIdPhieuGiamGia() == null) {
            return null;
        }
        PhieuGiamGia phieu = phieuGiamGiaRepository.findByIdForUpdate(req.getIdPhieuGiamGia())
            .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));
        if (!Boolean.TRUE.equals(phieu.getTrangThai())) {
            throw new IllegalArgumentException("Phiếu giảm giá không hoạt động");
        }
        if (req.getIdKhachHang() == null) {
            throw new IllegalArgumentException("Vui lòng chọn khách hàng để sử dụng phiếu giảm giá");
        }
        return phieu;
    }

    /**
     * Dem so lan tai khoan da su dung phieu (lich su) va kiem tra khong ghi trung tren cung hoa don.
     */
    private void kiemTraGioiHanSuDung(PhieuGiamGia phieu, Long idKhachHang, Long idHoaDon) {
        int gioiHan = phieu.getGioiHanMoiTaiKhoan() != null ? phieu.getGioiHanMoiTaiKhoan() : 1;
        long daDung = lichSuSuDungRepository.countByKhachHangIdAndPhieuGiamGiaId(idKhachHang, phieu.getId());
        if (daDung >= gioiHan) {
            throw new IllegalArgumentException(
                "Tài khoản đã dùng phiếu "
                    + PhieuGiamGiaUtil.tenHienThi(phieu.getLoaiGiamGia(), phieu.getGiaTriGiam())
                    + " đủ " + gioiHan + " lần"
            );
        }
        if (idHoaDon != null
            && lichSuSuDungRepository.existsByHoaDonIdAndPhieuGiamGiaId(idHoaDon, phieu.getId())) {
            throw new IllegalArgumentException("Hóa đơn đã sử dụng phiếu giảm giá này");
        }
    }

    private void ghiLichSuSuDung(HoaDon hoaDon, PhieuGiamGia phieu) {
        if (hoaDon.getKhachHang() == null) {
            return;
        }
        LichSuSuDungPhieuGiamGia lichSu = new LichSuSuDungPhieuGiamGia();
        lichSu.setKhachHang(hoaDon.getKhachHang());
        lichSu.setPhieuGiamGia(phieu);
        lichSu.setHoaDon(hoaDon);
        lichSu.setSoTienGiam(hoaDon.getTienGiam() != null ? hoaDon.getTienGiam() : BigDecimal.ZERO);
        lichSu.setThoiGian(LocalDateTime.now());
        lichSuSuDungRepository.save(lichSu);
    }

    private void luuCacDong(HoaDon hoaDon, List<DongTinh> dong, HoaDonRequest req) {
        List<ChiTietHoaDon> entities = new ArrayList<>();
        for (DongTinh d : dong) {
            ChiTietHoaDon e = new ChiTietHoaDon();
            e.setHoaDon(hoaDon);
            e.setSanPhamChiTiet(d.spct());
            e.setSoLuong(d.req().getSoLuong());
            e.setDonGia(d.req().getDonGia());
            e.setThanhTien(d.thanhTien());
            e.setGhiChu(blankToNull(d.req().getGhiChu()));
            e.setTrangThai(d.req().getTrangThai() == null || d.req().getTrangThai());
            entities.add(e);
        }
        chiTietHoaDonRepository.saveAll(entities);
    }

    private HoaDon findOrThrow(Long id) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn #" + id));
        if (hoaDon.isDaXoa()) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn #" + id);
        }
        return hoaDon;
    }

    private HoaDon findForUpdateOrThrow(Long id) {
        HoaDon hoaDon = hoaDonRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn #" + id));
        if (hoaDon.isDaXoa()) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn #" + id);
        }
        return hoaDon;
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
            .tenPhieuGiamGia(h.getTenGiamGiaUngDung() != null
                ? h.getTenGiamGiaUngDung()
                : (h.getPhieuGiamGia() != null ? h.getPhieuGiamGia().getTenPhieuGiamGia() : null))
            .idDotGiamGia(h.getDotGiamGia() != null ? h.getDotGiamGia().getId() : null)
            .tenDotGiamGia(h.getDotGiamGia() != null ? h.getDotGiamGia().getTenDotGiamGia() : null)
            .tienGiam(h.getTienGiam())
            .tenGiamGiaUngDung(h.getTenGiamGiaUngDung())
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
