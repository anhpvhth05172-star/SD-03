package com.example.datn.service;

import java.util.Objects;
import java.util.stream.Collectors;
import com.example.datn.dto.CaLamViecDto;
import com.example.datn.dto.HoaDon.ChiTietHoaDonDTO;
import com.example.datn.dto.HoaDon.ChiTietHoaDonRequest;
import com.example.datn.dto.SanPham.FormDataResponse;
import com.example.datn.dto.HoaDon.HoaDonDTO;
import com.example.datn.dto.HoaDon.HoaDonRequest;
import com.example.datn.dto.HoaDon.LichSuHoaDonDTO;
import com.example.datn.dto.Common.PageResponse;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.dto.DotGiamGia.GiaKhuyenMai;
import com.example.datn.entity.ChiTietHoaDon;
import com.example.datn.entity.DiaChiKhachHang;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.KhachHangPhieuGiamGia;
import com.example.datn.entity.LichSuHoaDon;
import com.example.datn.entity.LichSuSuDungPhieuGiamGia;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.entity.PhuongThucThanhToan;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.ChiTietHoaDonRepository;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.repository.DiaChiKhachHangRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.KhachHangPhieuGiamGiaRepository;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HoaDonService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HoaDonService.class);
    private static final Pattern TEN_KHACH_HANG_HOP_LE =
        Pattern.compile("^[\\p{L}\\s.'-]+$");
    private static final Pattern SO_DIEN_THOAI_HOP_LE =
        Pattern.compile("^0\\d{9,10}$");
    private static final Pattern DIA_CHI_HOP_LE =
        Pattern.compile("^[\\p{L}\\p{N}\\s.,/()-]+$");
    private static final Pattern KY_TU_CONTROL =
        Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]");
    private static final Pattern LICH_SU_CHUYEN_TRANG_THAI =
        Pattern.compile("^Chuyển từ (.+?) sang (.+?)(?:\\.\\s*(.*))?$", Pattern.DOTALL);

    private static final int MA_TOI_DA = 50;
    private static final int TEN_TOI_DA = 150;
    private static final int DIA_CHI_TOI_DA = 500;
    private static final int GHI_CHU_TOI_DA = 1000;
    private static final int SO_LAN_GIAO_THAT_BAI_TU_DONG_HUY = 3;

    private static final String[][] TRANG_THAI_MAP = {
        {"CHO_THANH_TOAN", "Chờ thanh toán"},
        {"CHO_XAC_NHAN", "Chờ xác nhận"},
        {"DA_XAC_NHAN", "Đã xác nhận"},
        {"CHO_GIAO_HANG", "Chờ giao hàng"},
        {"DANG_GIAO_HANG", "Đang giao hàng"},
        {"DA_GIAO_HANG", "Đã giao hàng"},
        {"GIAO_HANG_THAT_BAI", "Giao hàng thất bại"},
        {"DA_HOAN_THANH", "Đã hoàn thành"},
        {"DA_HUY", "Đã hủy"},
        {"DA_HOAN_TIEN", "Đã hoàn tiền"},
    };

    private static final Map<String, List<String>> TRANSITION_MAP = new HashMap<>();
    static {
        TRANSITION_MAP.put("CHO_THANH_TOAN", List.of("CHO_XAC_NHAN", "DA_XAC_NHAN", "DA_HUY"));
        TRANSITION_MAP.put("CHO_XAC_NHAN", List.of("DA_XAC_NHAN", "DA_HUY"));
        TRANSITION_MAP.put("DA_XAC_NHAN", List.of("CHO_GIAO_HANG", "DA_HUY"));
        TRANSITION_MAP.put("CHO_GIAO_HANG", List.of("DANG_GIAO_HANG", "DA_HUY"));
        TRANSITION_MAP.put("DANG_GIAO_HANG", List.of("DA_GIAO_HANG", "GIAO_HANG_THAT_BAI"));
        TRANSITION_MAP.put("DA_GIAO_HANG", List.of("DA_HOAN_THANH"));
        TRANSITION_MAP.put("DA_HOAN_THANH", List.of("DA_HOAN_TIEN"));
        TRANSITION_MAP.put("GIAO_HANG_THAT_BAI", List.of("DANG_GIAO_HANG"));
        TRANSITION_MAP.put("DA_HUY", List.of());
        TRANSITION_MAP.put("DA_HOAN_TIEN", List.of());
    }

    private static final Map<String, List<String>> TRANSITION_MAP_POS = new HashMap<>();
    static {
        TRANSITION_MAP_POS.put("CHO_XAC_NHAN", List.of("DA_HOAN_THANH", "DA_HUY"));
        TRANSITION_MAP_POS.put("DA_HOAN_THANH", List.of("DA_HOAN_TIEN"));
        TRANSITION_MAP_POS.put("DA_HUY", List.of());
        TRANSITION_MAP_POS.put("DA_HOAN_TIEN", List.of());
    }

    private static final int GHI_CHU_TRANG_THAI_TOI_DA = 500;

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final LichSuSuDungPhieuGiamGiaRepository lichSuSuDungRepository;
    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final GHNService ghnService;

    @Autowired
    private GiaoCaService giaoCaService;
    @Autowired
    private com.example.datn.repository.CaLamViecRepository caLamViecRepository;
    @Autowired
    private EmailService emailService;

    @Autowired
    private com.example.datn.service.impls.GiaService giaService;

    @Autowired
    private KhachHangPhieuGiamGiaRepository khachHangPhieuGiamGiaRepository;

    @Autowired(required = false)
    private com.example.datn.repository.GioHangChiTietRepository gioHangChiTietRepository;

    public HoaDonService(
        HoaDonRepository hoaDonRepository,
        ChiTietHoaDonRepository chiTietHoaDonRepository,
        KhachHangRepository khachHangRepository,
        NhanVienRepository nhanVienRepository,
        PhuongThucThanhToanRepository phuongThucThanhToanRepository,
        PhieuGiamGiaRepository phieuGiamGiaRepository,
        SanPhamChiTietRepository sanPhamChiTietRepository,
        LichSuHoaDonRepository lichSuHoaDonRepository,
        LichSuSuDungPhieuGiamGiaRepository lichSuSuDungRepository,
        DiaChiKhachHangRepository diaChiKhachHangRepository,
        GHNService ghnService
    ) {
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.khachHangRepository = khachHangRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.phuongThucThanhToanRepository = phuongThucThanhToanRepository;
        this.phieuGiamGiaRepository = phieuGiamGiaRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.lichSuHoaDonRepository = lichSuHoaDonRepository;
        this.lichSuSuDungRepository = lichSuSuDungRepository;
        this.diaChiKhachHangRepository = diaChiKhachHangRepository;
        this.ghnService = ghnService;
    }

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
        // Cho phép tìm kiếm theo tên hoặc SĐT có khoảng trắng
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
    public void guiLaiEmailHoaDon(Long id, String emailNhan) {
        HoaDon h = findOrThrow(id);
        String email = emailNhan;
        if (isBlank(email) && h.getKhachHang() != null) {
            email = h.getKhachHang().getEmail();
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("Khách hàng không có địa chỉ email hợp lệ để nhận thông tin.");
        }

        List<ChiTietHoaDon> chiTietList = chiTietHoaDonRepository.findByHoaDonId(h.getId());
        List<EmailService.ChiTietDongMail> dongList = new ArrayList<>();
        java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));

        for (ChiTietHoaDon ct : chiTietList) {
            SanPhamChiTiet s = ct.getSanPhamChiTiet();
            String tenSp = (s != null && s.getSanPham() != null) ? s.getSanPham().getTenSanPham() : "Sản phẩm";
            String bienThe = "";
            if (s != null) {
                String size = s.getKichCo() != null ? s.getKichCo().getTenKichCo() : "";
                String color = s.getMauSac() != null ? s.getMauSac().getTenMau() : "";
                bienThe = "Size " + size + (color.isEmpty() ? "" : " - " + color);
            }
            int sl = ct.getSoLuong() != null ? ct.getSoLuong() : 1;
            String donGia = ct.getDonGia() != null ? nf.format(ct.getDonGia()) : "0 đ";
            String thanhTien = ct.getThanhTien() != null ? nf.format(ct.getThanhTien()) : "0 đ";
            dongList.add(new EmailService.ChiTietDongMail(tenSp, bienThe, sl, donGia, thanhTien));
        }

        String tenKh = !isBlank(h.getTenKhachHang()) ? h.getTenKhachHang()
            : (h.getKhachHang() != null ? h.getKhachHang().getTenKhachHang() : "Quý khách");

        BigDecimal phiShip = h.getPhiVanChuyen() != null ? h.getPhiVanChuyen() : BigDecimal.ZERO;
        BigDecimal tienSauGiam = h.getTienSauGiamGia() != null ? h.getTienSauGiamGia() : BigDecimal.ZERO;
        BigDecimal tienGiam = h.getTienGiam() != null ? h.getTienGiam() : BigDecimal.ZERO;
        BigDecimal tongHang = h.getTongTien() != null ? h.getTongTien().subtract(phiShip) : BigDecimal.ZERO;

        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        String ngayTao = h.getNgayTao() != null ? h.getNgayTao().format(dtf) : "";

        emailService.guiMailHoaDon(
            email.trim(),
            tenKh,
            h.getMaHoaDon(),
            toTenTrangThai(h.getTrangThai()),
            toTenLoaiDon(h.getLoaiDon()),
            !isBlank(h.getDiaChiNhanHang()) ? h.getDiaChiNhanHang() : "Nhận tại quầy",
            nf.format(tongHang),
            nf.format(tienGiam),
            nf.format(phiShip),
            nf.format(tienSauGiam),
            ngayTao,
            dongList
        );
    }

    public void guiEmailHoaDonTuDong(HoaDon h, String emailNhan) {
        if (h == null) return;
        try {
            String email = emailNhan;
            if (isBlank(email) && h.getKhachHang() != null) {
                email = h.getKhachHang().getEmail();
            }
            if (isBlank(email)) {
                log.info("Đơn hàng #{} không có email người nhận, bỏ qua gửi email tự động.", h.getMaHoaDon());
                return;
            }

            List<ChiTietHoaDon> chiTietList = chiTietHoaDonRepository.findByHoaDonId(h.getId());
            List<EmailService.ChiTietDongMail> dongList = new ArrayList<>();
            java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));

            for (ChiTietHoaDon ct : chiTietList) {
                SanPhamChiTiet s = ct.getSanPhamChiTiet();
                String tenSp = (s != null && s.getSanPham() != null) ? s.getSanPham().getTenSanPham() : "Sản phẩm";
                String bienThe = "";
                if (s != null) {
                    String size = s.getKichCo() != null ? s.getKichCo().getTenKichCo() : "";
                    String color = s.getMauSac() != null ? s.getMauSac().getTenMau() : "";
                    bienThe = "Size " + size + (color.isEmpty() ? "" : " - " + color);
                }
                int sl = ct.getSoLuong() != null ? ct.getSoLuong() : 1;
                String donGia = ct.getDonGia() != null ? nf.format(ct.getDonGia()) : "0 đ";
                String thanhTien = ct.getThanhTien() != null ? nf.format(ct.getThanhTien()) : "0 đ";
                dongList.add(new EmailService.ChiTietDongMail(tenSp, bienThe, sl, donGia, thanhTien));
            }

            String tenKh = !isBlank(h.getTenKhachHang()) ? h.getTenKhachHang()
                : (h.getKhachHang() != null ? h.getKhachHang().getTenKhachHang() : "Quý khách");

            BigDecimal phiShip = h.getPhiVanChuyen() != null ? h.getPhiVanChuyen() : BigDecimal.ZERO;
            BigDecimal tienSauGiam = h.getTienSauGiamGia() != null ? h.getTienSauGiamGia() : BigDecimal.ZERO;
            BigDecimal tienGiam = h.getTienGiam() != null ? h.getTienGiam() : BigDecimal.ZERO;
            BigDecimal tongHang = h.getTongTien() != null ? h.getTongTien().subtract(phiShip) : BigDecimal.ZERO;

            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
            String ngayTao = h.getNgayTao() != null ? h.getNgayTao().format(dtf) : "";

            emailService.guiMailHoaDonAsync(
                email.trim(),
                tenKh,
                h.getMaHoaDon(),
                toTenTrangThai(h.getTrangThai()),
                toTenLoaiDon(h.getLoaiDon()),
                !isBlank(h.getDiaChiNhanHang()) ? h.getDiaChiNhanHang() : "Nhận tại quầy",
                nf.format(tongHang),
                nf.format(tienGiam),
                nf.format(phiShip),
                nf.format(tienSauGiam),
                ngayTao,
                dongList
            );
            log.info("Đã kích hoạt gửi email tự động cho hóa đơn #{} tới {}", h.getMaHoaDon(), email.trim());
        } catch (Exception e) {
            log.warn("Lỗi chuẩn bị gửi email tự động cho hóa đơn #{}: {}", h.getMaHoaDon(), e.getMessage());
        }
    }


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

    @Transactional(readOnly = true)
    public PageResponse<HoaDonDTO> listCuaToi(String keyword, int page, int size) {
        Long idKhachHang = idKhachHangDangNhap();
        String tuKhoa = isBlank(keyword) ? null : keyword.trim();
        var result = hoaDonRepository.findOrdersForCustomer(
            idKhachHang,
            tuKhoa,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))
        );
        return PageResponse.from(result.map(this::toDTO));
    }

    @Transactional(readOnly = true)
    public HoaDonDTO getCuaToi(Long id) {
        Long idKhachHang = idKhachHangDangNhap();
        HoaDon hoaDon = hoaDonRepository.findById(id)
            .filter(h -> !h.isDaXoa() && h.getKhachHang() != null
                && idKhachHang.equals(h.getKhachHang().getId()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        return toFullDTO(hoaDon);
    }

    private Long idKhachHangDangNhap() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)
            || auth.getAuthorities().stream().noneMatch(a -> "ROLE_KHACH_HANG".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập tài khoản khách hàng");
        }
        return id;
    }

    @Transactional
    public HoaDonDTO create(HoaDonRequest req) {
        kiemTraNguoiTaoHoaDon(req);
        validateHeader(req, null);
        phanCongNhanVienDonOnline(req);
        CaLamViecDto caDangMo = chuanBiTaoHdTaiQuay(req);
        xacDinhPhiVanChuyenOnline(req);
        PhieuGiamGia phieu = khoaVaKiemTraCoBan(req);
        if (phieu != null && req.getIdKhachHang() != null) {
            kiemTraGioiHanSuDung(phieu, req.getIdKhachHang(), null);
        }
        HoaDon hoaDon = new HoaDon();
        fillHeader(hoaDon, req);
        if (caDangMo != null) {
            hoaDon.setIdCaLamViec(caDangMo.getId());
        }
        List<DongTinh> lines = tinhToanVaLuu(hoaDon, req);
        hoaDon = hoaDonRepository.save(hoaDon);
        luuDiaChiKhachHangVaoSo(hoaDon);
        luuCacDong(hoaDon, lines, req);
        if (phieu != null) {
            ghiLichSuSuDung(hoaDon, phieu);
        }
        ghiLichSuCapNhat(hoaDon, req.getGhiChu());
        if (!"CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
            guiEmailHoaDonTuDong(hoaDon, req.getEmail());
        }
        return toFullDTO(hoaDon);
    }

    private void ghiLichSuCapNhat(HoaDon hoaDon, String ghiChu) {
        ghiLichSuCapNhat(hoaDon, ghiChu, null, null);
    }

    private void phanCongNhanVienDonOnline(HoaDonRequest req) {
        if (!"ONLINE".equals(toMaLoaiDon(req.getLoaiDon()))) return;
        List<NhanVien> nhanViens = nhanVienRepository.findActiveOrderAssignees();
        if (nhanViens.isEmpty()) {
            // Đơn online vẫn được phép đặt; để trống nhân viên để cửa hàng phân công sau.
            req.setIdNhanVien(null);
            return;
        }
        Map<Long, Long> soDonDangXuLy = new HashMap<>();
        for (Object[] row : hoaDonRepository.countOpenOrdersByEmployee()) {
            soDonDangXuLy.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        NhanVien nhanDon = nhanViens.stream()
            .min(Comparator.comparingLong((NhanVien n) -> soDonDangXuLy.getOrDefault(n.getId(), 0L))
                .thenComparing(NhanVien::getId))
            .orElseThrow();
        req.setIdNhanVien(nhanDon.getId());
    }

    private void kiemTraNguoiTaoHoaDon(HoaDonRequest req) {
        if (req == null) throw new IllegalArgumentException("Thiếu thông tin hóa đơn");
        String loaiDon = toMaLoaiDon(req.getLoaiDon());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean nhanVien = auth != null && auth.getPrincipal() instanceof Long
            && auth.getAuthorities().stream().anyMatch(a -> "ROLE_NHAN_VIEN".equals(a.getAuthority()));
        boolean admin = auth != null && auth.getPrincipal() instanceof Long
            && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        boolean khachHang = auth != null && auth.getPrincipal() instanceof Long
            && auth.getAuthorities().stream().anyMatch(a -> "ROLE_KHACH_HANG".equals(a.getAuthority()));

        if (nhanVien || admin) {
            req.setIdNhanVien((Long) auth.getPrincipal());
            return;
        }
        if (!"ONLINE".equals(loaiDon)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ nhân viên mới được tạo hóa đơn tại quầy");
        }
        if (khachHang) {
            Long idDangNhap = (Long) auth.getPrincipal();
            if (req.getIdKhachHang() != null && !req.getIdKhachHang().equals(idDangNhap)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể đặt hàng thay cho tài khoản khác");
            }
            req.setIdKhachHang(idDangNhap);
        } else {
            req.setIdKhachHang(null);
        }
        req.setIdNhanVien(null);
    }

    /**
     * Mua hàng có địa chỉ nhận mà khách chưa có sổ địa chỉ -> lưu làm địa chỉ mặc định.
     * Khách đã có địa chỉ thì không đụng vào.
     */
    private void luuDiaChiKhachHangVaoSo(HoaDon hoaDon) {
        KhachHang khachHang = hoaDon.getKhachHang();
        String diaChi = hoaDon.getDiaChiNhanHang();
        if (khachHang == null || khachHang.getId() == null || isBlank(diaChi)) {
            return;
        }
        if (diaChiKhachHangRepository.findFirstByKhachHangIdAndMacDinhTrue(khachHang.getId()).isPresent()) {
            return;
        }
        DiaChiKhachHang diaChiMoi = new DiaChiKhachHang();
        diaChiMoi.setKhachHang(khachHang);
        diaChiMoi.setMaDiaChi(String.format("DC%03d-%02d", khachHang.getId(),
            diaChiKhachHangRepository.countByKhachHangId(khachHang.getId()) + 1));
        diaChiMoi.setTenChiChi(khachHang.getTenKhachHang());
        diaChiMoi.setDiaChiCuThe(diaChi.trim());
        diaChiMoi.setMacDinh(true);
        diaChiMoi.setTrangThai(true);
        diaChiKhachHangRepository.save(diaChiMoi);
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
        if (req.getToProvinceId() == null && hoaDon.getToProvinceId() != null) {
            req.setToProvinceId(hoaDon.getToProvinceId());
            req.setToDistrictId(hoaDon.getToDistrictId());
            req.setToWardCode(hoaDon.getToWardCode());
        }
        if (req.getPhiVanChuyen() == null) {
            xacDinhPhiVanChuyenOnline(req);
        } else if ("ONLINE".equals(toMaLoaiDon(req.getLoaiDon()))) {
            if (req.getToProvinceId() == null) req.setToProvinceId(201);
            if (req.getToDistrictId() == null) req.setToDistrictId(1);
            if (isBlank(req.getToWardCode())) req.setToWardCode("001");
        }
        fillHeader(hoaDon, req);
        PhieuGiamGia phieu = khoaVaKiemTraCoBan(req);
        lichSuSuDungRepository.deleteByHoaDonId(hoaDon.getId());
        if (phieu != null && req.getIdKhachHang() != null) {
            kiemTraGioiHanSuDung(phieu, req.getIdKhachHang(), hoaDon.getId());
        }
        List<Long> idsCanKhoa = new ArrayList<>();
        chiTietHoaDonRepository.findByHoaDonId(hoaDon.getId()).stream()
            .map(c -> c.getSanPhamChiTiet().getId()).forEach(idsCanKhoa::add);
        if (req.getChiTiet() != null) {
            req.getChiTiet().stream().map(ChiTietHoaDonRequest::getIdSanPhamChiTiet)
                .filter(java.util.Objects::nonNull).forEach(idsCanKhoa::add);
        }
        if (!idsCanKhoa.isEmpty()) {
            sanPhamChiTietRepository.findAllByIdForUpdate(idsCanKhoa.stream().distinct().sorted().toList());
        }
        if (daTruKho(hoaDon)) {
            hoanTonKho(hoaDon.getId());
        }
        BigDecimal tienSauGiamCu = hoaDon.getTienSauGiamGia() != null ? hoaDon.getTienSauGiamGia() : BigDecimal.ZERO;
        List<DongTinh> lines = tinhToanVaLuu(hoaDon, req);
        chiTietHoaDonRepository.deleteByHoaDonId(hoaDon.getId());
        luuCacDong(hoaDon, lines, req);
        if (phieu != null) {
            ghiLichSuSuDung(hoaDon, phieu);
        }
        BigDecimal tienSauGiamMoi = hoaDon.getTienSauGiamGia() != null ? hoaDon.getTienSauGiamGia() : BigDecimal.ZERO;
        ghiLichSuCapNhat(hoaDon, req.getGhiChu(), tienSauGiamCu, tienSauGiamMoi);
        return toFullDTO(hoaDon);
    }

    private void ghiLichSuCapNhat(HoaDon hoaDon, String ghiChu, BigDecimal tienCu, BigDecimal tienMoi) {
        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(hoaDon.getTrangThai());
        lichSu.setHanhDong("CAP_NHAT_DON_HANG");
        StringBuilder sb = new StringBuilder("Cập nhật thông tin đơn hàng khi Chờ xác nhận");
        if (tienCu != null && tienMoi != null) {
            BigDecimal diff = tienMoi.subtract(tienCu);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                sb.append(String.format(" (Phát sinh phụ phí thu thêm: +%,.0f đ)", diff));
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                sb.append(String.format(" (Phát sinh hoàn phí cho khách: -%,.0f đ)", diff.abs()));
            } else {
                sb.append(" (Tổng tiền không đổi)");
            }
        }
        if (!isBlank(ghiChu)) {
            sb.append(". ").append(ghiChu.trim());
        }
        lichSu.setGhiChu(sb.toString());
        lichSu.setNguoiThaoTac(tenNguoiThaoTacHienTai());
        lichSuHoaDonRepository.save(lichSu);
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
        if ("CHO_XAC_NHAN".equals(trangThai) && daTruKho(hoaDon)) {
            hoanTraTonKho(hoaDon);
        }
        hoaDon.setDaXoa(true);
        hoaDon.setNguoiCapNhat(nguoiCapNhatHienTai());
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
        hoaDon.setNguoiCapNhat(nguoiCapNhatHienTai());
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        return toFullDTO(hoaDonRepository.save(hoaDon));
    }

    @Transactional
    public HoaDonDTO doiTrangThai(Long id, String trangThaiMoi) {
        return doiTrangThai(id, trangThaiMoi, null, null);
    }

    @Transactional
    public HoaDonDTO doiTrangThai(Long id, String trangThaiMoi, String ghiChu) {
        return doiTrangThai(id, trangThaiMoi, ghiChu, null);
    }

    @Transactional
    public HoaDonDTO doiTrangThai(Long id, String trangThaiMoi, String ghiChu, String nguoiThaoTac) {
        HoaDon hoaDon = findForUpdateOrThrow(id);
        if (isBlank(trangThaiMoi)) {
            throw new IllegalArgumentException("Trạng thái mới không được để trống");
        }
        String ghiChuHopLe = chuanHoaGhiChuTrangThai(ghiChu);
        String maMoi = toMaTrangThai(trangThaiMoi);
        String maHienTai = hoaDon.getTrangThai();
        if (maMoi.equals(maHienTai)) {
            throw new IllegalArgumentException("Trạng thái không thay đổi");
        }
        if ("DA_HUY".equals(maMoi) && !"CHO_XAC_NHAN".equals(maHienTai) && !"DA_XAC_NHAN".equals(maHienTai)) {
            throw new IllegalArgumentException("Đơn hàng đã được xác nhận, không thể hủy đơn.");
        }
        List<String> choPhep = chonBangChuyenTrangThai(hoaDon).getOrDefault(maHienTai, List.of());
        if (!choPhep.contains(maMoi)) {
            throw new IllegalArgumentException(
                "Không được chuyển từ " + toTenTrangThai(maHienTai) + " sang " + toTenTrangThai(maMoi)
            );
        }
        if ("DA_XAC_NHAN".equals(maMoi) && "ONLINE".equalsIgnoreCase(hoaDon.getLoaiDon())) {
            boolean duDiaChi = !isBlank(hoaDon.getDiaChiNhanHang())
                && hoaDon.getToDistrictId() != null
                && !isBlank(hoaDon.getToWardCode());
            boolean duPhi = hoaDon.getPhiVanChuyen() != null; // Cho phép 0 nếu là đơn miễn phí vận chuyển (Free Ship)
            if (!duDiaChi || !duPhi) {
                throw new IllegalArgumentException(
                    "Đơn online chưa đủ địa chỉ nhận hàng/phí vận chuyển để xác nhận");
            }
            truKho(hoaDon);
        } else if ("DA_XAC_NHAN".equals(maMoi)
            && "TAI_QUAY_GIAO_HANG".equalsIgnoreCase(hoaDon.getLoaiDon())
            && isBlank(hoaDon.getDiaChiNhanHang())) {
            throw new IllegalArgumentException("Đơn giao hàng cần có địa chỉ nhận hàng hợp lệ để xác nhận");
        }
        boolean laLanThatBaiThuBa = "GIAO_HANG_THAT_BAI".equals(maMoi)
            && lichSuHoaDonRepository.countByHoaDonIdAndTrangThai(id, "GIAO_HANG_THAT_BAI")
                >= SO_LAN_GIAO_THAT_BAI_TU_DONG_HUY - 1;
        if (("DA_HUY".equals(maMoi) || "DA_HOAN_TIEN".equals(maMoi)) && daTruKho(hoaDon)) {
            hoanTonKho(id);
        }
        hoaDon.setTrangThai(maMoi);
        hoaDon.setNguoiCapNhat(nguoiCapNhatHienTai());
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        HoaDon saved = hoaDonRepository.save(hoaDon);
        String nguoiThucHien = !isBlank(nguoiThaoTac) ? nguoiThaoTac.trim() : tenNguoiThaoTacHienTai();
        ghiLichSuTrangThai(saved, maHienTai, maMoi, ghiChuHopLe, nguoiThucHien);
        // Lưu ý: kho đã hoàn ở trên (DA_HUY/DA_HOAN_TIEN) — KHÔNG hoanTonKho lần 2 ở đây.
        if ("DA_HUY".equals(maMoi) && hoaDon.getPhieuGiamGia() != null && hoaDon.getKhachHang() != null) {
            if ("CA_NHAN".equalsIgnoreCase(hoaDon.getPhieuGiamGia().getPhamVi())) {
                khachHangPhieuGiamGiaRepository.findByPhieuGiamGia_IdAndKhachHang_Id(hoaDon.getPhieuGiamGia().getId(), hoaDon.getKhachHang().getId())
                    .ifPresent(khPg -> {
                        khPg.setNgaySuDung(null);
                        khPg.setTrangThai(true);
                        khachHangPhieuGiamGiaRepository.save(khPg);
                    });
            }
            lichSuSuDungRepository.deleteByHoaDonId(hoaDon.getId());
        }
        if (laLanThatBaiThuBa) {
            hoaDon.setTrangThai("DA_HUY");
            hoaDon.setNguoiCapNhat(nguoiCapNhatHienTai());
            hoaDon.setNgayCapNhat(LocalDateTime.now());
            saved = hoaDonRepository.save(hoaDon);
            // Giao thất bại lần thứ ba tự hủy đơn; hoàn tồn đúng một lần.
            hoanTonKho(id);
            ghiLichSuTrangThai(
                saved,
                "GIAO_HANG_THAT_BAI",
                "DA_HUY",
                "Tự động hủy sau " + SO_LAN_GIAO_THAT_BAI_TU_DONG_HUY + " lần giao hàng thất bại",
                "Hệ thống"
            );
        }
        return toFullDTO(saved);
    }

    private Map<String, List<String>> chonBangChuyenTrangThai(HoaDon hoaDon) {
        String loaiDon = hoaDon.getLoaiDon();
        boolean laOnline = "ONLINE".equalsIgnoreCase(loaiDon) || "Online".equalsIgnoreCase(loaiDon);
        if (laOnline) {
            return TRANSITION_MAP;
        }
        boolean laPos = "TAI_QUAY".equalsIgnoreCase(loaiDon) || "Tại quầy".equalsIgnoreCase(loaiDon)
            || "TAI_QUAY_GIAO_HANG".equalsIgnoreCase(loaiDon);
        if (laPos) {
            boolean coGiaoHang = !isBlank(hoaDon.getDiaChiNhanHang())
                || (hoaDon.getPhiVanChuyen() != null && hoaDon.getPhiVanChuyen().compareTo(BigDecimal.ZERO) > 0);
            return coGiaoHang ? TRANSITION_MAP : TRANSITION_MAP_POS;
        }
        throw new IllegalArgumentException(
            "Không xác định được loại hóa đơn để đổi trạng thái (loại đơn hiện tại: " + loaiDon + ")");
    }

    private boolean laLoaiDonHopLe(HoaDon hoaDon) {
        String loaiDon = hoaDon.getLoaiDon();
        return "TAI_QUAY".equalsIgnoreCase(loaiDon) || "Tại quầy".equalsIgnoreCase(loaiDon)
            || "TAI_QUAY_GIAO_HANG".equalsIgnoreCase(loaiDon)
            || "ONLINE".equalsIgnoreCase(loaiDon) || "Online".equalsIgnoreCase(loaiDon);
    }

    private String chuanHoaGhiChuTrangThai(String ghiChu) {
        if (isBlank(ghiChu)) {
            return null;
        }
        String ghiChuTrim = ghiChu.trim();
        if (ghiChuTrim.length() > GHI_CHU_TRANG_THAI_TOI_DA) {
            throw new IllegalArgumentException(
                "Ghi chú tối đa " + GHI_CHU_TRANG_THAI_TOI_DA + " ký tự");
        }
        if (KY_TU_CONTROL.matcher(ghiChuTrim).find()) {
            throw new IllegalArgumentException("Ghi chú chứa ký tự không hợp lệ");
        }
        return ghiChuTrim;
    }

    private static String nguoiCapNhatHienTai() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof Long id ? String.valueOf(id) : "KHACH_AN_DANH";
    }

    private String tenNguoiThaoTacHienTai() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "Hệ thống";
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) {
            Optional<NhanVien> nvOpt = nhanVienRepository.findById(id);
            if (nvOpt.isPresent()) {
                NhanVien nv = nvOpt.get();
                String ten = nv.getTenTaiKhoan();
                String role = (nv.getVaiTro() != null && !isBlank(nv.getVaiTro().getTenVaiTro()))
                    ? nv.getVaiTro().getTenVaiTro() : null;
                return role != null ? ten + " (" + role + ")" : ten;
            }
            Optional<KhachHang> khOpt = khachHangRepository.findById(id);
            if (khOpt.isPresent()) {
                KhachHang kh = khOpt.get();
                return !isBlank(kh.getTenKhachHang()) ? kh.getTenKhachHang() : "Khách hàng #" + id;
            }
            return "Người dùng #" + id;
        } else if (principal instanceof String str && !"anonymousUser".equalsIgnoreCase(str)) {
            return str;
        }
        return "Hệ thống";
    }

    private void ghiLichSuTrangThai(HoaDon hoaDon, String tuMa, String denMa, String ghiChu) {
        ghiLichSuTrangThai(hoaDon, tuMa, denMa, ghiChu, null);
    }

    private void ghiLichSuTrangThai(HoaDon hoaDon, String tuMa, String denMa, String ghiChu, String nguoiThaoTac) {
        LichSuHoaDon lichSu = new LichSuHoaDon();
        lichSu.setHoaDon(hoaDon);
        lichSu.setTrangThai(denMa);
        lichSu.setHanhDong("CHUYEN_TRANG_THAI");
        String noiDung = "Chuyển từ " + toTenTrangThai(tuMa) + " sang " + toTenTrangThai(denMa);
        if (!isBlank(ghiChu)) {
            noiDung += ". " + ghiChu;
        }
        lichSu.setGhiChu(noiDung);
        lichSu.setNguoiThaoTac(!isBlank(nguoiThaoTac) ? nguoiThaoTac.trim() : tenNguoiThaoTacHienTai());
        lichSuHoaDonRepository.save(lichSu);
    }

    @Transactional(readOnly = true)
    public List<LichSuHoaDonDTO> lichSuHoaDon(Long idHoaDon) {
        if (idHoaDon == null || !hoaDonRepository.existsById(idHoaDon)) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn #" + idHoaDon);
        }
        return lichSuHoaDonRepository.findByIdHoaDonOrderByThoiGianDescIdDesc(idHoaDon).stream()
            .map(this::toLichSuDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LichSuHoaDonDTO> lichSuHoaDonCuaToi(Long idHoaDon) {
        getCuaToi(idHoaDon); // xác thực đơn thuộc khách đang đăng nhập trước khi trả lịch sử
        return lichSuHoaDon(idHoaDon);
    }

    private LichSuHoaDonDTO toLichSuDTO(LichSuHoaDon e) {
        HoaDon h = e.getHoaDon();
        String ghiChuGoc = e.getGhiChu();
        String trangThaiTruoc = null;
        String ghiChuNguoiDung = null;
        if (!isBlank(ghiChuGoc)) {
            var m = LICH_SU_CHUYEN_TRANG_THAI.matcher(ghiChuGoc.trim());
            if (m.matches()) {
                trangThaiTruoc = m.group(1).trim();
                String ghiChuPhu = m.group(3);
                if (!isBlank(ghiChuPhu)) {
                    ghiChuNguoiDung = ghiChuPhu.trim();
                }
            }
        }
        String nguoiThaoTac = e.getNguoiThaoTac();
        if (isBlank(nguoiThaoTac)) {
            nguoiThaoTac = "Hệ thống";
        }
        return LichSuHoaDonDTO.builder()
            .id(e.getId())
            .idHoaDon(h != null ? h.getId() : null)
            .maHoaDon(h != null ? h.getMaHoaDon() : null)
            .thoiGian(e.getThoiGian())
            .trangThaiTruoc(trangThaiTruoc)
            .trangThaiSau(toTenTrangThai(e.getTrangThai()))
            .maTrangThaiSau(e.getTrangThai())
            .ghiChu(ghiChuNguoiDung)
            .hanhDong(e.getHanhDong())
            .nguoiThaoTac(nguoiThaoTac)
            .build();
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
        List<KhachHangPhieuGiamGia> allKhPg = khachHangPhieuGiamGiaRepository.findAll();
        List<LichSuSuDungPhieuGiamGia> allLichSu = lichSuSuDungRepository.findAll();

        java.util.Map<Long, java.util.Map<Long, Long>> usageByPhieuAndKhach = allLichSu.stream()
            .filter(l -> l.getPhieuGiamGia() != null && l.getKhachHang() != null && l.getPhieuGiamGia().getId() != null && l.getKhachHang().getId() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                l -> l.getPhieuGiamGia().getId(),
                java.util.stream.Collectors.groupingBy(
                    l -> l.getKhachHang().getId(),
                    java.util.stream.Collectors.counting()
                )
            ));

        java.util.Map<Long, List<Long>> khChuaDungMap = allKhPg.stream()
            .filter(r -> Boolean.TRUE.equals(r.getTrangThai()) && r.getNgaySuDung() == null && r.getPhieuGiamGia() != null && r.getKhachHang() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                r -> r.getPhieuGiamGia().getId(),
                java.util.stream.Collectors.mapping(r -> r.getKhachHang().getId(), java.util.stream.Collectors.toList())
            ));

        List<FormDataResponse.PhieuGiamGiaOption> phieuGiamGias = phieuGiamGiaRepository.findAll().stream()
            .map(p -> {
                int limit = p.getGioiHanMoiTaiKhoan() != null && p.getGioiHanMoiTaiKhoan() > 0 ? p.getGioiHanMoiTaiKhoan() : 1;
                java.util.Map<Long, Long> khUsage = usageByPhieuAndKhach.getOrDefault(p.getId(), java.util.Map.of());

                java.util.Set<Long> daDungSet = new java.util.HashSet<>();
                khUsage.forEach((khId, count) -> {
                    if (count >= limit) {
                        daDungSet.add(khId);
                    }
                });

                if ("CA_NHAN".equalsIgnoreCase(p.getPhamVi())) {
                    allKhPg.stream()
                        .filter(r -> r.getPhieuGiamGia() != null && p.getId().equals(r.getPhieuGiamGia().getId()) && r.getKhachHang() != null)
                        .forEach(r -> {
                            if (Boolean.FALSE.equals(r.getTrangThai()) || r.getNgaySuDung() != null) {
                                daDungSet.add(r.getKhachHang().getId());
                            }
                        });
                }

                List<Long> chuaDung = khChuaDungMap.getOrDefault(p.getId(), List.of()).stream()
                    .filter(khId -> !daDungSet.contains(khId))
                    .toList();

                return new FormDataResponse.PhieuGiamGiaOption(
                    p.getId(), p.getMaPhieuGiamGia(),
                    PhieuGiamGiaUtil.tenHienThi(p.getLoaiGiamGia(), p.getGiaTriGiam()),
                    p.getLoaiGiamGia(),
                    p.getGiaTriGiam(), p.getGiamToiDa(), p.getHoaDonToiThieu(),
                    p.getNgayBatDau(), p.getNgayKetThuc(), p.getSoLuong(), p.getSoLuongDaSuDung(),
                    p.getGioiHanMoiTaiKhoan(), p.getTrangThai(),
                    p.getPhamVi(),
                    chuaDung,
                    new java.util.ArrayList<>(daDungSet)
                );
            })
            .toList();
        List<FormDataResponse.SanPhamChiTietOption> spcts = sanPhamChiTietRepository.findAllWithDetails().stream()
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
        String maTrangThaiKhoiTao = toMaTrangThai(req.getTrangThai());
        if (!"CHO_THANH_TOAN".equals(maTrangThaiKhoiTao) && !"CHO_XAC_NHAN".equals(maTrangThaiKhoiTao) && !"DA_XAC_NHAN".equals(maTrangThaiKhoiTao) && !"DA_HOAN_THANH".equals(maTrangThaiKhoiTao)) {
            throw new IllegalArgumentException(
                "Trạng thái khởi tạo không hợp lệ (Chờ thanh toán / Chờ xác nhận / Đã hoàn thành)");
        }
        if ("TAI_QUAY".equals(toMaLoaiDon(req.getLoaiDon())) && req.getIdNhanVien() == null) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên");
        }
        if (req.getPhiVanChuyen() == null) {
            req.setPhiVanChuyen(BigDecimal.ZERO);
        }
        if (req.getPhiVanChuyen().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Phí vận chuyển không được âm");
        }
        if ("TAI_QUAY".equals(toMaLoaiDon(req.getLoaiDon()))) {
            req.setPhiVanChuyen(BigDecimal.ZERO);
        }
        validateText(req);
        if (req.getChiTiet() == null || req.getChiTiet().isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn phải có ít nhất 1 sản phẩm");
        }
    }

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
boolean isTaiQuay = "TAI_QUAY".equals(toMaLoaiDon(req.getLoaiDon())) || "TAI_QUAY_GIAO_HANG".equals(toMaLoaiDon(req.getLoaiDon()));
        if (isBlank(req.getSoDienThoaiKhachHang())) {
            if ("TAI_QUAY".equals(toMaLoaiDon(req.getLoaiDon()))) {
                req.setSoDienThoaiKhachHang("0988888888");
            } else {
                throw new IllegalArgumentException("Số điện thoại không được để trống");
            }
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

    private void xacDinhPhiVanChuyenOnline(HoaDonRequest req) {
        if (!"ONLINE".equals(toMaLoaiDon(req.getLoaiDon()))) {
            return;
        }
        // Chính sách Free Ship cho đơn hàng có tổng tiền hàng >= 10.000.000đ
        BigDecimal tienHangTam = req.getChiTiet() != null ? req.getChiTiet().stream()
            .map(d -> (d.getDonGia() != null ? d.getDonGia() : BigDecimal.ZERO).multiply(BigDecimal.valueOf(d.getSoLuong() != null ? d.getSoLuong() : 1)))
            .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;
        if (tienHangTam.compareTo(BigDecimal.valueOf(10000000)) >= 0 || (req.getPhiVanChuyen() != null && req.getPhiVanChuyen().compareTo(BigDecimal.ZERO) == 0)) {
            req.setPhiVanChuyen(BigDecimal.ZERO);
            return;
        }
        if (req.getToProvinceId() == null) {
            throw new IllegalArgumentException("Đơn online phải chọn tỉnh/thành phố để tính phí vận chuyển");
        }
        if (req.getToDistrictId() == null) {
            throw new IllegalArgumentException("Đơn online phải chọn quận/huyện để tính phí vận chuyển");
        }
        if (isBlank(req.getToWardCode())) {
            throw new IllegalArgumentException("Đơn online phải chọn phường/xã để tính phí vận chuyển");
        }
        if (req.getToWardCode().trim().length() > 20) {
            throw new IllegalArgumentException("Mã phường/xã không hợp lệ");
        }
        if (req.getChiTiet() == null || req.getChiTiet().isEmpty()) {
            throw new IllegalArgumentException("Chưa có sản phẩm để tính phí vận chuyển");
        }
        if (req.getToDistrictId() <= 1 || "001".equals(req.getToWardCode().trim())) {
            BigDecimal fallbackPhi = req.getPhiVanChuyen();
            if (fallbackPhi == null || fallbackPhi.compareTo(BigDecimal.valueOf(1000)) <= 0) {
                fallbackPhi = BigDecimal.valueOf(30000);
            }
            req.setPhiVanChuyen(fallbackPhi);
            return;
        }
        ShippingFeeRequest phiReq = new ShippingFeeRequest(
            req.getToDistrictId(),
            req.getToWardCode().trim(),
            req.getChiTiet().stream()
                .map(d -> new ShippingFeeRequest.DongHang(d.getIdSanPhamChiTiet(), d.getSoLuong()))
                .toList()
        );
        try {
            int phi = ghnService.tinhPhi(phiReq);
            if (phi <= 1000) {
                throw new IllegalArgumentException("Phí vận chuyển từ GHN không hợp lệ");
            }
            req.setPhiVanChuyen(BigDecimal.valueOf(phi));
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase(java.util.Locale.ROOT) : "";
            if (msg.contains("to ward not found") || msg.contains("from ward not found")) {
                BigDecimal fallbackPhi = req.getPhiVanChuyen();
                if (fallbackPhi == null || fallbackPhi.compareTo(BigDecimal.valueOf(1000)) <= 0) {
                    fallbackPhi = BigDecimal.valueOf(30000);
                }
                req.setPhiVanChuyen(fallbackPhi);
                return;
            }
            throw ex;
        }
    }

    private void fillHeader(HoaDon hoaDon, HoaDonRequest req) {
        hoaDon.setMaHoaDon(req.getMaHoaDon().trim());
        String maLoai = toMaLoaiDon(req.getLoaiDon());
        // Bảo đảm tương thích với CHECK constraint CK_hoa_don_loai_don trong SQL Server (chỉ nhận TAI_QUAY hoặc ONLINE)
        hoaDon.setLoaiDon("ONLINE".equalsIgnoreCase(maLoai) ? "ONLINE" : "TAI_QUAY");
        hoaDon.setPhiVanChuyen(req.getPhiVanChuyen() != null ? req.getPhiVanChuyen() : BigDecimal.ZERO);
        hoaDon.setTenKhachHang(blankToNull(req.getTenKhachHang()));
        hoaDon.setSoDienThoaiKhachHang(blankToNull(req.getSoDienThoaiKhachHang()));
        hoaDon.setDiaChiNhanHang(blankToNull(req.getDiaChiNhanHang()));
        hoaDon.setToProvinceId(req.getToProvinceId());
        hoaDon.setToDistrictId(req.getToDistrictId());
        hoaDon.setToWardCode(blankToNull(req.getToWardCode()));
        hoaDon.setTrangThai(toMaTrangThai(req.getTrangThai()));
        hoaDon.setGhiChu(blankToNull(req.getGhiChu()));
        hoaDon.setNguoiCapNhat(nguoiCapNhatHienTai());
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
                .or(() -> "ONLINE".equalsIgnoreCase(maLoai)
                    ? phuongThucThanhToanRepository.findAll().stream().findFirst()
                    : Optional.empty())
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

private NhanVien timNhanVien(Long id) {
        NhanVien nv = nhanVienRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại"));
        if (Boolean.FALSE.equals(nv.getTrangThai())) {
            throw new IllegalArgumentException("Tài khoản nhân viên đã bị khóa hoặc ngừng hoạt động");
        }
        return nv;
    }
    private CaLamViecDto chuanBiTaoHdTaiQuay(HoaDonRequest req) {
        String maLoai = toMaLoaiDon(req.getLoaiDon());
        if (!"TAI_QUAY".equals(maLoai) && !"TAI_QUAY_GIAO_HANG".equals(maLoai)) {
            return null;
        }
        Long idNhanVienSession = idNhanVienSession();
        if (idNhanVienSession == null) {
            return null;
        }
        req.setIdNhanVien(idNhanVienSession);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        CaLamViecDto ca = null;
        if (req.getIdCaLamViec() != null) {
            ca = caLamViecRepository.findByIdAndDaXoaFalse(req.getIdCaLamViec())
                .filter(c -> "DANG_MO".equals(c.getTrangThai()))
                .map(giaoCaService::toDto)
                .orElse(null);
        }
        if (ca == null) {
            ca = giaoCaService.getDangMoOf(idNhanVienSession);
        }

        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return ca;
        }
        if (ca == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CA_CHUA_MO: Nhân viên chưa mở ca làm việc");
        }
        return ca;
    }

    private static Long idNhanVienSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            return null;
        }
        boolean laNhanVienOrAdmin = auth.getAuthorities().stream().anyMatch(a ->
            "ROLE_NHAN_VIEN".equals(a.getAuthority())
                || "ROLE_ADMIN".equals(a.getAuthority()));
        return laNhanVienOrAdmin ? id : null;
    }

    private KhachHang timKhachHang(Long id) {
        KhachHang kh = khachHangRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
        if (Boolean.FALSE.equals(kh.getTrangThai())) {
            throw new IllegalArgumentException("Khách hàng đã bị vô hiệu hóa");
        }
        return kh;
    }

    private record DongTinh(SanPhamChiTiet spct, ChiTietHoaDonRequest req, BigDecimal donGia, BigDecimal thanhTien) {}

    private List<DongTinh> tinhToanVaLuu(HoaDon hoaDon, HoaDonRequest req) {
        List<DongTinh> dong = new ArrayList<>();
        Set<String> maDaCo = new HashSet<>();
        if (req.getChiTiet() == null || req.getChiTiet().isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn phải có ít nhất một sản phẩm");
        }
        List<Long> ids = req.getChiTiet().stream()
            .map(ChiTietHoaDonRequest::getIdSanPhamChiTiet)
            .filter(java.util.Objects::nonNull)
            .distinct()
            .sorted()
            .toList();
        if (ids.size() != req.getChiTiet().size()) {
            throw new IllegalArgumentException("Mỗi dòng chi tiết phải chọn một biến thể riêng");
        }
        Map<Long, SanPhamChiTiet> lockedById = new HashMap<>();
        sanPhamChiTietRepository.findAllByIdForUpdate(ids)
            .forEach(item -> lockedById.put(item.getId(), item));
        if (lockedById.size() != ids.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều biến thể sản phẩm không tồn tại");
        }
        Map<Long, GiaKhuyenMai> giaTheoBienThe = giaService.tinhGia(lockedById.values());
        for (ChiTietHoaDonRequest d : req.getChiTiet()) {
            SanPhamChiTiet spct = lockedById.get(d.getIdSanPhamChiTiet());
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
            BigDecimal donGia = giaTheoBienThe.get(spct.getId()).giaSauGiam();
            if (donGia == null || donGia.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá bán của biến thể không hợp lệ");
            }
            BigDecimal thanhTien = donGia.multiply(BigDecimal.valueOf(d.getSoLuong()));
            dong.add(new DongTinh(spct, d, donGia, thanhTien));
        }

        BigDecimal tongTienHang = dong.stream()
            .map(DongTinh::thanhTien)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Chính sách Free Ship cho đơn hàng có tổng tiền hàng >= 10.000.000đ
        if (tongTienHang.compareTo(BigDecimal.valueOf(10000000)) >= 0) {
            hoaDon.setPhiVanChuyen(BigDecimal.ZERO);
            if (req != null) {
                req.setPhiVanChuyen(BigDecimal.ZERO);
            }
        }

        BigDecimal tong = tongTienHang.add(hoaDon.getPhiVanChuyen() != null ? hoaDon.getPhiVanChuyen() : BigDecimal.ZERO);
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

    private PhieuGiamGia khoaVaKiemTraCoBan(HoaDonRequest req) {
        if (req.getIdPhieuGiamGia() == null) {
            return null;
        }
        PhieuGiamGia phieu = phieuGiamGiaRepository.findByIdForUpdate(req.getIdPhieuGiamGia())
            .orElseThrow(() -> new IllegalArgumentException("Phiếu giảm giá không tồn tại"));
        if (!Boolean.TRUE.equals(phieu.getTrangThai())) {
            throw new IllegalArgumentException("Phiếu giảm giá không hoạt động");
        }
        boolean laPos = "TAI_QUAY".equals(toMaLoaiDon(req.getLoaiDon())) || "TAI_QUAY_GIAO_HANG".equals(toMaLoaiDon(req.getLoaiDon()));
        if (!laPos && req.getIdKhachHang() == null) {
            throw new IllegalArgumentException("Vui lòng chọn khách hàng để sử dụng phiếu giảm giá");
        }
        if ("CA_NHAN".equalsIgnoreCase(phieu.getPhamVi())) {
            if (req.getIdKhachHang() == null
                || !khachHangPhieuGiamGiaRepository.existsByPhieuGiamGia_IdAndKhachHang_IdAndTrangThaiTrue(
                    phieu.getId(), req.getIdKhachHang())) {
                throw new IllegalArgumentException("Phiếu giảm giá cá nhân không được cấp cho khách hàng này");
            }
        }
        return phieu;
    }

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

        // Đánh dấu khách hàng đã sử dụng phiếu cá nhân (dùng 1 lần)
        if ("CA_NHAN".equalsIgnoreCase(phieu.getPhamVi())) {
            khachHangPhieuGiamGiaRepository.findByPhieuGiamGia_IdAndKhachHang_Id(phieu.getId(), hoaDon.getKhachHang().getId())
                .ifPresent(khPg -> {
                    khPg.setNgaySuDung(LocalDateTime.now());
                    khPg.setTrangThai(false);
                    khachHangPhieuGiamGiaRepository.save(khPg);
                });
        }
    }

    
    private void hoanTraTonKho(HoaDon hoaDon) {
        if (hoaDon == null || hoaDon.getId() == null) {
            return;
        }
        List<ChiTietHoaDon> chiTiets = chiTietHoaDonRepository.findByHoaDonId(hoaDon.getId());
        for (ChiTietHoaDon ct : chiTiets) {
            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
            if (spct != null && ct.getSoLuong() != null && ct.getSoLuong() > 0) {
                int tonHienTai = spct.getSoLuong() != null ? spct.getSoLuong() : 0;
                spct.setSoLuong(tonHienTai + ct.getSoLuong());
                sanPhamChiTietRepository.save(spct);
            }
        }
    }

    private void luuCacDong(HoaDon hoaDon, List<DongTinh> dong, HoaDonRequest req) {
        List<ChiTietHoaDon> entities = new ArrayList<>();
        Long idKhachHang = req != null && req.getIdKhachHang() != null ? req.getIdKhachHang()
                : (hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getId() : null);
        for (DongTinh d : dong) {
            ChiTietHoaDon e = new ChiTietHoaDon();
            e.setHoaDon(hoaDon);
            e.setSanPhamChiTiet(d.spct());
            e.setSoLuong(d.req().getSoLuong());
            e.setDonGia(d.donGia());
            e.setThanhTien(d.thanhTien());
            e.setGhiChu(blankToNull(d.req().getGhiChu()));
            e.setTrangThai(d.req().getTrangThai() == null || d.req().getTrangThai());
            entities.add(e);

            // Đơn ONLINE chưa trừ kho lúc tạo (chỉ trừ khi nhân viên xác nhận DA_XAC_NHAN). Đơn TAI_QUAY trừ ngay trừ khi CHO_THANH_TOAN.
            boolean laOnline = "ONLINE".equalsIgnoreCase(hoaDon.getLoaiDon());
            if (!laOnline && !"CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
                SanPhamChiTiet spct = d.spct();
                spct.setSoLuong(spct.getSoLuong() - d.req().getSoLuong());
                sanPhamChiTietRepository.save(spct);
            }

            // Xóa sản phẩm tương ứng khỏi giỏ hàng nếu khách hàng đã đăng nhập
            if (idKhachHang != null && gioHangChiTietRepository != null && d.spct() != null && d.spct().getId() != null) {
                try {
                    gioHangChiTietRepository.xoaTheoKhachHangVaSpct(idKhachHang, d.spct().getId());
                } catch (Exception ex) {
                    log.warn("Không thể xóa sản phẩm #{} khỏi giỏ hàng của KH #{}: {}", d.spct().getId(), idKhachHang, ex.getMessage());
                }
            }
        }
        chiTietHoaDonRepository.saveAll(entities);
    }

    private void hoanTonKho(Long idHoaDon) {
        List<ChiTietHoaDon> chiTiet = chiTietHoaDonRepository.findByHoaDonId(idHoaDon);
        if (chiTiet.isEmpty()) return;
        List<Long> ids = chiTiet.stream().map(c -> c.getSanPhamChiTiet().getId()).distinct().sorted().toList();
        Map<Long, SanPhamChiTiet> locked = new HashMap<>();
        sanPhamChiTietRepository.findAllByIdForUpdate(ids).forEach(s -> locked.put(s.getId(), s));
        for (ChiTietHoaDon line : chiTiet) {
            SanPhamChiTiet variant = locked.get(line.getSanPhamChiTiet().getId());
            if (variant != null && variant.getSoLuong() != null) {
                variant.setSoLuong(variant.getSoLuong() + line.getSoLuong());
                sanPhamChiTietRepository.save(variant);
            }
        }
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
            .loaiDon(toTenLoaiDon(h.getLoaiDon(), h.getDiaChiNhanHang()))
            .maLoaiDon(toMaLoaiDonChuan(h.getLoaiDon(), h.getDiaChiNhanHang()))
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
            .maTrangThai(h.getTrangThai())
            .ghiChu(h.getGhiChu())
            .idKhachHang(h.getKhachHang() != null ? h.getKhachHang().getId() : null)
            .idNhanVien(h.getNhanVien() != null ? h.getNhanVien().getId() : null)
            .maNhanVien(h.getNhanVien() != null ? h.getNhanVien().getMaNhanVien() : null)
            .tenNhanVien(h.getNhanVien() != null ? h.getNhanVien().getTenTaiKhoan() : null)
            .idPhuongThucThanhToan(h.getPhuongThucThanhToan() != null ? h.getPhuongThucThanhToan().getId() : null)
            .tenPhuongThucThanhToan(h.getPhuongThucThanhToan() != null ? h.getPhuongThucThanhToan().getTenPhuongThuc() : null)
            .idPhieuGiamGia(h.getPhieuGiamGia() != null ? h.getPhieuGiamGia().getId() : null)
            .maPhieuGiamGia(h.getPhieuGiamGia() != null ? h.getPhieuGiamGia().getMaPhieuGiamGia() : null)
            .tenPhieuGiamGia(h.getTenGiamGiaUngDung() != null
                ? h.getTenGiamGiaUngDung()
                : (h.getPhieuGiamGia() != null ? h.getPhieuGiamGia().getTenPhieuGiamGia() : null))
            .loaiGiamGia(h.getPhieuGiamGia() != null ? h.getPhieuGiamGia().getLoaiGiamGia() : null)
            .giaTriGiam(h.getPhieuGiamGia() != null ? h.getPhieuGiamGia().getGiaTriGiam() : null)
            .idDotGiamGia(h.getDotGiamGia() != null ? h.getDotGiamGia().getId() : null)
            .tenDotGiamGia(h.getDotGiamGia() != null ? h.getDotGiamGia().getTenDotGiamGia() : null)
            .tienGiam(h.getTienGiam())
            .tenGiamGiaUngDung(h.getTenGiamGiaUngDung())
            .toProvinceId(h.getToProvinceId())
            .toDistrictId(h.getToDistrictId())
            .toWardCode(h.getToWardCode())
            .build();
    }

    private HoaDonDTO toFullDTO(HoaDon h) {
        HoaDonDTO dto = toDTO(h);
        List<ChiTietHoaDonDTO> lines = chiTietHoaDonRepository.findByHoaDonId(h.getId()).stream()
            .map(this::toLineDTO)
            .toList();
        dto.setChiTiet(lines);
        dto.setTrangThaiTiepTheo(tenTrangThaiTiepTheo(h));
        return dto;
    }

    private List<String> tenTrangThaiTiepTheo(HoaDon h) {
        if (!laLoaiDonHopLe(h)) {
            return List.of();
        }
        return chonBangChuyenTrangThai(h).getOrDefault(h.getTrangThai(), List.of()).stream()
            .map(HoaDonService::toTenTrangThai)
            .toList();
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
            throw new IllegalArgumentException("Loại đơn không hợp lệ");
        }
        String s = loaiDon.trim();
        if (s.equalsIgnoreCase("TAI_QUAY_GIAO_HANG")
            || s.equalsIgnoreCase("Tại quầy (Giao hàng)")
            || s.equalsIgnoreCase("Giao hàng tại quầy")
            || s.equalsIgnoreCase("Tại quầy - Giao hàng")) {
            return "TAI_QUAY_GIAO_HANG";
        }
        if (s.equalsIgnoreCase("TAI_QUAY")
            || s.equalsIgnoreCase("Tại quầy")
            || s.equalsIgnoreCase("Tại quầy (Nhận ngay)")
            || s.equalsIgnoreCase("Tại quầy - Nhận ngay")) {
            return "TAI_QUAY";
        }
        if (s.equalsIgnoreCase("ONLINE") || s.equalsIgnoreCase("Online")) {
            return "ONLINE";
        }
        return s.toUpperCase();
    }

    private static String toTenLoaiDon(String ma, String diaChiNhanHang) {
        if (ma == null) return null;
        if (ma.equalsIgnoreCase("ONLINE")) return "Online";
        if (ma.equalsIgnoreCase("TAI_QUAY_GIAO_HANG") || (ma.equalsIgnoreCase("TAI_QUAY") && !isBlank(diaChiNhanHang))) {
            return "Tại quầy (Giao hàng)";
        }
        if (ma.equalsIgnoreCase("TAI_QUAY")) {
            return "Tại quầy";
        }
        return ma;
    }

    private static String toTenLoaiDon(String ma) {
        return toTenLoaiDon(ma, null);
    }

    private static String toMaLoaiDonChuan(String ma, String diaChiNhanHang) {
        if (ma == null) return "TAI_QUAY";
        if (ma.equalsIgnoreCase("ONLINE")) return "ONLINE";
        if (ma.equalsIgnoreCase("TAI_QUAY_GIAO_HANG") || (ma.equalsIgnoreCase("TAI_QUAY") && !isBlank(diaChiNhanHang))) {
            return "TAI_QUAY_GIAO_HANG";
        }
        return "TAI_QUAY";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String blankToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    /**
     * Hủy đơn CHO_THANH_TOAN (VNPay thất bại) - hard delete để giỏ hàng giữ nguyên.
     * Đơn CHO_THANH_TOAN chưa trừ kho nên không cần hoàn kho.
     */
    @Transactional
    public void huyDonChoThanhToan(Long id) {
        HoaDon hoaDon = hoaDonRepository.findByIdForUpdate(id).orElse(null);
        if (hoaDon == null) return;
        if (!"CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ hủy được đơn ở trạng thái Chờ thanh toán");
        }
        chiTietHoaDonRepository.deleteByHoaDonId(id);
        lichSuHoaDonRepository.deleteByHoaDonId(id);
        hoaDonRepository.deleteById(id);
    }

    /**
     * VNPay thành công: CHO_THANH_TOAN -> CHO_XAC_NHAN (chờ nhân viên xác nhận và đóng gói).
     * Idempotent: return URL + IPN gọi 2 lần thì lần sau thoát sớm.
     */
    @Transactional
    public HoaDonDTO xacNhanThanhToanVNPay(Long id) {
        HoaDon hoaDon = hoaDonRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn #" + id));
        if (!"CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
            return toFullDTO(hoaDon);
        }
        String tu = hoaDon.getTrangThai();
        if (hoaDon.getKhachHang() != null && hoaDon.getKhachHang().getId() != null && gioHangChiTietRepository != null) {
            List<ChiTietHoaDon> lines = chiTietHoaDonRepository.findByHoaDonId(hoaDon.getId());
            for (ChiTietHoaDon line : lines) {
                if (line.getSanPhamChiTiet() != null && line.getSanPhamChiTiet().getId() != null) {
                    try {
                        gioHangChiTietRepository.xoaTheoKhachHangVaSpct(
                            hoaDon.getKhachHang().getId(), line.getSanPhamChiTiet().getId());
                    } catch (Exception ex) {
                        log.warn("Không thể xóa sp #{} khỏi giỏ KH #{}: {}", line.getSanPhamChiTiet().getId(), hoaDon.getKhachHang().getId(), ex.getMessage());
                    }
                }
            }
        }
        hoaDon.setTrangThai("CHO_XAC_NHAN");
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        hoaDon.setNguoiCapNhat("VNPAY");
        HoaDon saved = hoaDonRepository.save(hoaDon);
        ghiLichSuTrangThai(saved, tu, "CHO_XAC_NHAN", "Khách hàng thanh toán qua VNPAY thành công", "VNPAY (Hệ thống)");
        guiEmailHoaDonTuDong(saved, null);
        return toFullDTO(saved);
    }

    private Map<Long, SanPhamChiTiet> khoaSanPham(List<ChiTietHoaDon> lines) {
        Set<Long> ids = new HashSet<>();
        for (ChiTietHoaDon l : lines) {
            if (l.getSanPhamChiTiet() != null && l.getSanPhamChiTiet().getId() != null) {
                ids.add(l.getSanPhamChiTiet().getId());
            }
        }
        Map<Long, SanPhamChiTiet> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        List<SanPhamChiTiet> locked = sanPhamChiTietRepository.findAllByIdForUpdate(ids);
        for (SanPhamChiTiet s : locked) {
            map.put(s.getId(), s);
        }
        return map;
    }

    private void truKho(HoaDon hoaDon) {
        List<ChiTietHoaDon> lines = chiTietHoaDonRepository.findByHoaDonId(hoaDon.getId());
        Map<Long, SanPhamChiTiet> spctMap = khoaSanPham(lines);
        for (ChiTietHoaDon l : lines) {
            SanPhamChiTiet s = spctMap.get(l.getSanPhamChiTiet().getId());
            if (s != null) {
                int ton = s.getSoLuong() != null ? s.getSoLuong() : 0;
                int canTru = l.getSoLuong() != null ? l.getSoLuong() : 0;
                if (canTru > ton) {
                    throw new IllegalArgumentException(
                            "Sản phẩm " + s.getMaChiTietSanPham() + " chỉ còn " + ton + " trong kho");
                }
                s.setSoLuong(ton - canTru);
            }
        }
        sanPhamChiTietRepository.saveAll(spctMap.values());
    }

    private static final Set<String> TRANG_THAI_DA_TRU_KHO_ONLINE = Set.of(
        "DA_XAC_NHAN", "CHO_GIAO_HANG", "DANG_GIAO_HANG", "DA_GIAO_HANG", "DA_HOAN_THANH", "GIAO_HANG_THAT_BAI"
    );

    public boolean daTruKho(HoaDon hoaDon) {
        if (hoaDon == null) return false;
        if ("ONLINE".equalsIgnoreCase(hoaDon.getLoaiDon())) {
            return TRANG_THAI_DA_TRU_KHO_ONLINE.contains(hoaDon.getTrangThai());
        }
        return !"CHO_THANH_TOAN".equals(hoaDon.getTrangThai());
    }

    @Transactional(readOnly = true)
    public List<HoaDonDTO> traCuuPublicDanhSach(String maHoaDon) {
        if (isBlank(maHoaDon)) {
            throw new IllegalArgumentException("Mã hóa đơn hoặc số điện thoại không được để trống");
        }
        String clean = maHoaDon.trim();
        List<HoaDon> hoaDons;
        var byMa = hoaDonRepository.findByMaHoaDonPublic(clean);
        if (byMa.isPresent()) {
            hoaDons = List.of(byMa.get());
        } else {
            hoaDons = hoaDonRepository.findByPhonePublic(clean);
        }

        if (hoaDons.isEmpty()) {
            throw new com.example.datn.exception.ResourceNotFoundException("Không tìm thấy đơn hàng: " + clean);
        }

        List<Long> hdIds = hoaDons.stream().map(HoaDon::getId).toList();
        List<ChiTietHoaDon> allChiTiet = chiTietHoaDonRepository.findWithDetailsByHoaDonIdIn(hdIds);
        Map<Long, List<ChiTietHoaDonDTO>> linesByHdId = allChiTiet.stream()
            .collect(Collectors.groupingBy(
                c -> c.getHoaDon().getId(),
                Collectors.mapping(this::toLineDTO, Collectors.toList())
            ));

        return hoaDons.stream().map(h -> {
            HoaDonDTO dto = toDTO(h);
            dto.setChiTiet(linesByHdId.getOrDefault(h.getId(), List.of()));
            dto.setTrangThaiTiepTheo(tenTrangThaiTiepTheo(h));
            enrichGhiChuHuy(h, dto);
            return dto;
        }).toList();
    }

    private void enrichGhiChuHuy(HoaDon h, HoaDonDTO dto) {
        if ("DA_HUY".equals(h.getTrangThai())) {
            var lichSu = lichSuHoaDonRepository.findByIdHoaDonOrderByThoiGianDescIdDesc(h.getId());
            lichSu.stream()
                .filter(item -> "DA_HUY".equals(item.getTrangThai()) && item.getGhiChu() != null)
                .findFirst()
                .ifPresent(item -> dto.setGhiChu(item.getGhiChu()));
        }
    }

    @Transactional(readOnly = true)
    public HoaDonDTO traCuuPublic(String maHoaDon) {
        return traCuuPublicDanhSach(maHoaDon).get(0);
    }

    private String taoMaPosChoMoi() {
        String base = "HD" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String code = base;
        int i = 1;
        while (hoaDonRepository.existsByMaHoaDon(code)) {
            code = base + "_" + (i++);
        }
        return code;
    }

    @Transactional(readOnly = true)
    public List<HoaDonDTO> getPosCho() {
        Long idNv = idNhanVienSession();
        if (idNv == null) {
            return List.of();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean laAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        List<HoaDon> list;
        if (laAdmin) {
            list = hoaDonRepository.findPosWaitingOrders();
        } else {
            list = hoaDonRepository.findPosWaitingOrdersByEmployee(idNv);
        }
        return list.stream().map(this::toFullDTO).toList();
    }

    @Transactional
    public HoaDonDTO luuPosCho(HoaDonRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu đơn chờ không hợp lệ");
        }
        req.setLoaiDon("TAI_QUAY");
        req.setTrangThai("CHO_XAC_NHAN");
        CaLamViecDto ca = chuanBiTaoHdTaiQuay(req);
        Long idNv = idNhanVienSession();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean laAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        HoaDon hoaDon;
        if (req.getId() != null) {
            hoaDon = hoaDonRepository.findByIdForUpdate(req.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn chờ #" + req.getId()));
            if (hoaDon.isDaXoa()) {
                throw new IllegalArgumentException("Đơn chờ đã bị xóa");
            }
            if (!"CHO_XAC_NHAN".equals(hoaDon.getTrangThai()) || !"TAI_QUAY".equals(hoaDon.getLoaiDon())) {
                throw new IllegalArgumentException("Đơn này không còn ở trạng thái đơn chờ tại quầy");
            }
            if (!laAdmin && hoaDon.getNhanVien() != null && idNv != null && !hoaDon.getNhanVien().getId().equals(idNv)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền sửa đơn chờ của nhân viên khác");
            }
            // Hoàn lại tồn kho cũ của hóa đơn này trước khi tính toán tồn kho mới
            hoanTonKho(hoaDon.getId());
        } else {
            hoaDon = new HoaDon();
            if (isBlank(req.getMaHoaDon())) {
                req.setMaHoaDon(taoMaPosChoMoi());
            } else if (hoaDonRepository.existsByMaHoaDon(req.getMaHoaDon().trim())) {
                req.setMaHoaDon(taoMaPosChoMoi());
            }
            hoaDon.setNgayTao(LocalDateTime.now());
            if (ca != null) {
                hoaDon.setIdCaLamViec(ca.getId());
            }
            if (idNv != null) {
                hoaDon.setNhanVien(nhanVienRepository.findById(idNv).orElse(null));
            }
        }

        if (isBlank(req.getTenKhachHang())) {
            req.setTenKhachHang("Khách lẻ");
        }
        fillHeader(hoaDon, req);

        // Khóa các biến thể sản phẩm liên quan
        List<ChiTietHoaDonRequest> details = req.getChiTiet() != null ? req.getChiTiet() : List.of();
        List<DongTinh> lines = new ArrayList<>();
        if (!details.isEmpty()) {
            List<Long> ids = details.stream().map(ChiTietHoaDonRequest::getIdSanPhamChiTiet).filter(Objects::nonNull).distinct().sorted().toList();
            if (!ids.isEmpty()) {
                sanPhamChiTietRepository.findAllByIdForUpdate(ids);
            }
            lines = tinhToanVaLuu(hoaDon, req);
        } else {
            hoaDon.setTongTien(BigDecimal.ZERO);
            hoaDon.setTienGiam(BigDecimal.ZERO);
            hoaDon.setTienSauGiamGia(BigDecimal.ZERO);
        }

        hoaDon = hoaDonRepository.save(hoaDon);
        chiTietHoaDonRepository.deleteByHoaDonId(hoaDon.getId());
        chiTietHoaDonRepository.flush();

        // Trừ tồn kho và lưu các dòng chi tiết
        if (!lines.isEmpty()) {
            List<ChiTietHoaDon> entities = new ArrayList<>();
            for (DongTinh d : lines) {
                ChiTietHoaDon e = new ChiTietHoaDon();
                e.setHoaDon(hoaDon);
                e.setSanPhamChiTiet(d.spct());
                e.setSoLuong(d.req().getSoLuong());
                e.setDonGia(d.donGia());
                e.setThanhTien(d.thanhTien());
                e.setGhiChu(blankToNull(d.req().getGhiChu()));
                e.setTrangThai(true);
                entities.add(e);

                SanPhamChiTiet spct = d.spct();
                int ton = spct.getSoLuong() != null ? spct.getSoLuong() : 0;
                spct.setSoLuong(ton - d.req().getSoLuong());
                sanPhamChiTietRepository.save(spct);
            }
            chiTietHoaDonRepository.saveAll(entities);
        }

        return toFullDTO(hoaDon);
    }

    @Transactional
    public void huyPosCho(Long id) {
        HoaDon hoaDon = hoaDonRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn chờ #" + id));
        Long idNv = idNhanVienSession();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean laAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!laAdmin && hoaDon.getNhanVien() != null && idNv != null && !hoaDon.getNhanVien().getId().equals(idNv)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền hủy đơn chờ của nhân viên khác");
        }
        hoanTonKho(hoaDon.getId());
        hoaDon.setDaXoa(true);
        hoaDon.setTrangThai("DA_HUY");
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        hoaDon.setNguoiCapNhat(nguoiCapNhatHienTai());
        hoaDonRepository.save(hoaDon);
    }

    @Transactional
    public HoaDonDTO thanhToanPosCho(Long id, HoaDonRequest req) {
        if (req != null) req.setId(id);
        HoaDonDTO updated = luuPosCho(req);
        HoaDon hoaDon = hoaDonRepository.findByIdForUpdate(updated.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn #" + updated.getId()));

        if (hoaDon.getTongTien() == null || hoaDon.getTongTien().compareTo(BigDecimal.ZERO) <= 0
            || chiTietHoaDonRepository.findByHoaDonId(hoaDon.getId()).isEmpty()) {
            throw new IllegalArgumentException("Hóa đơn phải có ít nhất 1 sản phẩm để thanh toán");
        }

        PhieuGiamGia phieu = khoaVaKiemTraCoBan(req);
        if (phieu != null && req.getIdKhachHang() != null) {
            kiemTraGioiHanSuDung(phieu, req.getIdKhachHang(), hoaDon.getId());
            ghiLichSuSuDung(hoaDon, phieu);
            int daDung = phieu.getSoLuongDaSuDung() != null ? phieu.getSoLuongDaSuDung() : 0;
            phieu.setSoLuongDaSuDung(daDung + 1);
            phieuGiamGiaRepository.save(phieu);
        }

        boolean coGiaoHang = !isBlank(hoaDon.getDiaChiNhanHang())
            || (hoaDon.getPhiVanChuyen() != null && hoaDon.getPhiVanChuyen().compareTo(BigDecimal.ZERO) > 0);

        String trangThaiMoi = coGiaoHang ? "CHO_XAC_NHAN" : "DA_HOAN_THANH";
        String tu = hoaDon.getTrangThai();
        hoaDon.setTrangThai(trangThaiMoi);
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        hoaDon.setNguoiCapNhat(nguoiCapNhatHienTai());
        HoaDon saved = hoaDonRepository.save(hoaDon);
        ghiLichSuTrangThai(saved, tu, trangThaiMoi, "Thanh toán thành công tại quầy", tenNguoiThaoTacHienTai());
        guiEmailHoaDonTuDong(saved, req != null ? req.getEmail() : null);
        return toFullDTO(saved);
    }
}
