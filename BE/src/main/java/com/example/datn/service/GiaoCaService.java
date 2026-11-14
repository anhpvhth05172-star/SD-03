package com.example.datn.service;

import com.example.datn.dto.CaLamViecDto;
import com.example.datn.dto.LichSuCaDto;
import com.example.datn.dto.TongKetDto;
import com.example.datn.entity.CaLamViec;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.LichSuCa;
import com.example.datn.entity.NhanVien;
import com.example.datn.exception.KhongCoQuyenException;
import com.example.datn.repository.CaLamViecRepository;
import com.example.datn.repository.LichSuCaRepository;
import com.example.datn.repository.LichLamViecRepository;
import com.example.datn.repository.NhanVienRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GiaoCaService {

    public static final String TRANG_THAI_DANG_MO = "DANG_MO";
    public static final String TRANG_THAI_DA_KET_CA = "DA_KET_CA";
    public static final String TEN_TIEN_MAT = "Tiền mặt";

    private static final int MA_HOA_DON_TONG_KET_TOI_DA = 100;

    private final CaLamViecRepository caLamViecRepository;
    private final LichSuCaRepository lichSuCaRepository;
    private final NhanVienRepository nhanVienRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final com.example.datn.repository.CaLamRepository caLamRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CaLamViecDto moCa(Long tienDauCa, String ghiChu) {
        return moCa(new com.example.datn.dto.MoCaRequest(tienDauCa, ghiChu));
    }

    @Transactional
    public CaLamViecDto moCa(com.example.datn.dto.MoCaRequest request) {
        Long idNhanVien = idNhanVienHienTai();
        Long tienDauCa = request != null ? request.getTienDauCa() : null;
        String ghiChu = request != null ? request.getGhiChu() : null;
        Long idCaLich = request != null ? request.getIdCaLich() : null;

        if (tienDauCa == null || tienDauCa < 0) {
            throw bad("TIEN_DAU_CA", "Tiền đầu ca không được âm");
        }
        if (caLamViecRepository.findFirstByTrangThaiAndDaXoaFalse(TRANG_THAI_DANG_MO).isPresent()) {
            throw bad("CA_DA_MO", "Hệ thống đã có ca làm việc đang mở. Vui lòng kết ca trước đó!");
        }
        NhanVien nhanVien = nhanVienRepository.findById(idNhanVien)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));

        LocalDateTime thoiGianMo = LocalDateTime.now();
        LocalTime gioHienTai = thoiGianMo.toLocalTime();
        LocalDate ngayHienTai = thoiGianMo.toLocalDate();

        com.example.datn.entity.CaLam caTheoLich = null;

        // 1. Nếu nhân viên chủ động chọn ca được chia sẵn (Ca sáng, Ca chiều...):
        if (idCaLich != null) {
            caTheoLich = caLamRepository.findById(idCaLich)
                .filter(c -> Boolean.TRUE.equals(c.getDangBat()))
                .orElse(null);
        }

        // 2. Nếu không chọn cụ thể, tìm theo lịch phân công hôm nay hoặc ca theo giờ
        if (caTheoLich == null) {
            List<com.example.datn.entity.LichLamViec> lichHomNay = lichLamViecRepository
                .findCaDangBatDuocXep(idNhanVien, ngayHienTai);

            List<com.example.datn.entity.LichLamViec> lichDuocXep = lichHomNay.stream()
                .filter(lich -> {
                    com.example.datn.entity.CaLam c = lich.getCaLam();
                    if (c == null || c.getBatDau() == null || c.getKetThuc() == null) return false;
                    return trongThoiGianNhanCa(gioHienTai, c.getBatDau(), c.getKetThuc());
                })
                .toList();

            if (!lichDuocXep.isEmpty()) {
                caTheoLich = lichDuocXep.get(0).getCaLam();
            } else if (!lichHomNay.isEmpty()) {
                caTheoLich = lichHomNay.get(0).getCaLam();
            } else {
                // Tự động gán vào ca làm việc hiện tại của cửa hàng
                caTheoLich = timCaTheoGioHienTai(gioHienTai);
            }
        }

        if (caTheoLich == null) {
            throw bad("KHONG_TRONG_THOI_GIAN_NHAN_CA",
                "Hiện tại (" + gioHienTai.format(DateTimeFormatter.ofPattern("HH:mm"))
                + ") không trong thời gian nhận ca làm việc nào. Bạn chỉ có thể vào ca trước giờ bắt đầu tối đa 30 phút và trước khi ca kết thúc.");
        }

        // Nếu là Nhân viên thông thường, kiểm tra nghiêm ngặt ca có đang trong thời gian nhận ca không
        if (!laAdmin() && !trongThoiGianNhanCa(gioHienTai, caTheoLich.getBatDau(), caTheoLich.getKetThuc())) {
            throw bad("KHONG_TRONG_THOI_GIAN_NHAN_CA",
                "Hiện tại (" + gioHienTai.format(DateTimeFormatter.ofPattern("HH:mm")) + ") không trong thời gian nhận "
                + caTheoLich.getTenCa() + " (" + caTheoLich.getBatDau() + " - " + caTheoLich.getKetThuc()
                + "). Bạn chỉ có thể vào ca trước giờ bắt đầu tối đa 30 phút và trước khi ca kết thúc.");
        }

        CaLamViec ca = new CaLamViec();
        ca.setNhanVien(nhanVien);
        ca.setCaTheoLich(caTheoLich);
        ca.setTenQuy("Quầy tổng");
        ca.setThoiGianMo(thoiGianMo);
        ca.setTienDauCa(BigDecimal.valueOf(tienDauCa));
        ca.setTrangThai(TRANG_THAI_DANG_MO);
        ca.setDaXoa(false);
        ca.setGhiChuMo(chuanHoaGhiChu(ghiChu));
        ca.setMaCa(taoMaCa(ca.getThoiGianMo(), idNhanVien));
        CaLamViec saved;
        try {
            saved = caLamViecRepository.save(ca);
        } catch (DataIntegrityViolationException ex) {
            throw bad("CA_DA_MO", "Nhân viên đã có ca đang mở");
        }
        ghiLichSu(saved, "MO_CA", saved.getGhiChuMo(), toDto(saved));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public CaLamViecDto getDangMoOf(Long idNhanVien) {
        return caLamViecRepository
            .findFirstByTrangThaiAndDaXoaFalse(TRANG_THAI_DANG_MO)
            .map(this::toDto)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public CaLamViecDto getCaDangMo() {
        return caLamViecRepository.findFirstByTrangThaiAndDaXoaFalse(TRANG_THAI_DANG_MO)
            .map(this::toDto)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public TongKetDto tongKet(Long id) {
        CaLamViec ca = timVaKiemQuyen(id);
        return toTongKet(ca);
    }

    @Transactional
    public CaLamViecDto ketCa(Long id, Long tienThucTe, String lyDoLech, String ghiChu) {
        return ketCa(id, tienThucTe, 0L, lyDoLech, ghiChu);
    }

    @Transactional
    public CaLamViecDto ketCa(Long id, Long tienThucTe, Long tienChi, String lyDoLech, String ghiChu) {
        CaLamViec ca = timVaKiemQuyen(id);
        Long idNguoiGoi = idNhanVienHienTai();
        boolean admin = laAdmin();
        if (!admin && !ca.getNhanVien().getId().equals(idNguoiGoi)) {
            throw new KhongCoQuyenException("Chỉ người trực ca hoặc Quản lý mới có quyền kết thúc ca này");
        }
        TongKetDto tongKet = toTongKet(ca);
        kiemTraHoaDonDangXuLy(id);

        if (tienThucTe == null || tienThucTe < 0 || tienChi == null || tienChi < 0) {
            throw bad("TIEN_THUC_TE", "Tiền thực tế không được âm");
        }
        BigDecimal tienChiDec = BigDecimal.valueOf(tienChi);
        if (tienChiDec.compareTo(tongKet.getTienDuKien()) > 0) {
            throw bad("TIEN_CHI_VUOT_DU_KIEN", "Tiền chi trong ca không được vượt tiền mặt dự kiến");
        }
        BigDecimal tienDuKienSauChi = tongKet.getTienDuKien().subtract(tienChiDec);
        BigDecimal tienThucTeDec = BigDecimal.valueOf(tienThucTe);
        BigDecimal chenhLech = tienThucTeDec.subtract(tienDuKienSauChi);
        LocalDateTime thoiGianKet = LocalDateTime.now();
        String lyDoChuan = chuanHoaGhiChu(lyDoLech);
        if (chenhLech.compareTo(BigDecimal.ZERO) != 0 && lyDoChuan == null) {
            throw bad("LY_DO_LECH_THIEU", "Chênh lệch khác 0, bắt buộc nhập lý do");
        }

        String nguoiCapNhat = String.valueOf(idNhanVienHienTai());
        int affected = caLamViecRepository.ketCaConditional(
            id,
            thoiGianKet,
            tongKet.getTongHoaDon(),
            tongKet.getTongDoanhThu(),
            tongKet.getTienMatThu(),
            tongKet.getTienKhongMat(),
            tongKet.getTienHoanMat(),
            tienDuKienSauChi,
            tienThucTeDec,
            chenhLech,
            lyDoChuan,
            LocalDateTime.now(),
            nguoiCapNhat
        );
        if (affected == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CA_DANG_KET");
        }
        CaLamViec daKet = caLamViecRepository.findByIdAndDaXoaFalse(id)
            .orElseThrow(() -> bad("CA_KHONG_TON_TAI", "Không tìm thấy ca làm việc"));
        daKet.setTienChi(tienChiDec);
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("id", daKet.getId());
        snapshot.put("maCa", daKet.getMaCa());
        snapshot.put("trangThai", daKet.getTrangThai());
        snapshot.put("thoiGianKet", String.valueOf(daKet.getThoiGianKet()));
        snapshot.put("tongHoaDon", daKet.getTongHoaDon() == null ? 0 : daKet.getTongHoaDon());
        snapshot.put("tongDoanhThu", String.valueOf(daKet.getTongDoanhThu()));
        snapshot.put("tienMatThu", String.valueOf(daKet.getTienMatThu()));
        snapshot.put("tienKhongMat", String.valueOf(daKet.getTienKhongMat()));
        snapshot.put("tienHoanMat", String.valueOf(daKet.getTienHoanMat()));
        snapshot.put("tienDuKien", String.valueOf(daKet.getTienDuKien()));
        snapshot.put("tienThucTe", String.valueOf(daKet.getTienThucTe()));
        snapshot.put("tienChi", String.valueOf(daKet.getTienChi()));
        snapshot.put("chenhLech", String.valueOf(daKet.getChenhLech()));
        snapshot.put("lyDoLech", daKet.getLyDoLech());
        ghiLichSu(daKet, "KET_CA", chuanHoaGhiChu(ghiChu), snapshot);
        return toDto(daKet);
    }

    @Transactional(readOnly = true)
    public CaLamViecDto doiSoat(Long id) {
        CaLamViec ca = timVaKiemQuyen(id);
        CaLamViecDto dto = toDto(ca);
        if (TRANG_THAI_DANG_MO.equals(ca.getTrangThai())) {
            TongKetDto tongKet = toTongKet(ca);
            dto.setTongHoaDon(tongKet.getTongHoaDon());
            dto.setTongDoanhThu(tongKet.getTongDoanhThu());
            dto.setTienMatThu(tongKet.getTienMatThu());
            dto.setTienKhongMat(tongKet.getTienKhongMat());
            dto.setTienHoanMat(tongKet.getTienHoanMat());
            dto.setTienDuKien(tongKet.getTienDuKien());
        }
        dto.setLichSu(lichSuCuaCa(id));
        if (!TRANG_THAI_DANG_MO.equals(ca.getTrangThai())) {
            dto.setTienChi(tienChiTuLichSu(id));
        }
        return dto;
    }

    private BigDecimal tienChiTuLichSu(Long idCa) {
        return lichSuCaRepository.findByCaLamViecId(idCa).stream()
            .filter(l -> "KET_CA".equals(l.getHanhDong()) && l.getTruocSau() != null)
            .findFirst()
            .map(l -> {
                try {
                    String value = objectMapper.readTree(l.getTruocSau()).path("tienChi").asText("0");
                    return new BigDecimal(value);
                } catch (RuntimeException ex) {
                    return BigDecimal.ZERO;
                }
            })
            .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Page<CaLamViecDto> lichSu(
        int page, int size, LocalDate tuNgay, LocalDate denNgay,
        String trangThai, Long idNhanVien, String keyword, Long callerId, boolean callerAdmin
    ) {
        Long idLoc = callerAdmin ? idNhanVien : callerId;
        LocalDateTime tu = tuNgay == null ? null : tuNgay.atStartOfDay();
        LocalDateTime den = denNgay == null ? null : denNgay.plusDays(1).atTime(LocalTime.MIDNIGHT);
        String tuKhoa = keyword == null || keyword.isBlank() ? null : keyword.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<CaLamViec> trang = caLamViecRepository.lichSu(idLoc, trangThai, tu, den, tuKhoa, pageable);

        List<CaLamViecDto> noiDung = trang.getContent().stream().map(this::toDto).toList();
        return new PageImpl<>(noiDung, trang.getPageable(), trang.getTotalElements());
    }

    private String timNguoiNhanTiepTheo(CaLamViec ca, LocalDateTime thoiGianKet) {
        if (ca.getCaTheoLich() == null) return "Cuối ngày";
        LocalDate ngay = thoiGianKet != null ? thoiGianKet.toLocalDate() : LocalDate.now();

        // 1. Tìm trong ngày hiện tại
        Optional<String> nguoiNhanHomNay = lichLamViecRepository.findByNgayLamBetween(ngay, ngay).stream()
            .filter(l -> Boolean.TRUE.equals(l.getCaLam().getDangBat()))
            .filter(l -> !l.getCaLam().getId().equals(ca.getCaTheoLich().getId()))
            .filter(l -> l.getCaLam().getBatDau() != null && ca.getCaTheoLich().getKetThuc() != null
                && !l.getCaLam().getBatDau().isBefore(ca.getCaTheoLich().getKetThuc()))
            .sorted(Comparator.comparing(l -> l.getCaLam().getBatDau()))
            .map(l -> l.getNhanVien().getTenTaiKhoan() + " (" + l.getCaLam().getTenCa() + ")")
            .findFirst();
        if (nguoiNhanHomNay.isPresent()) {
            return nguoiNhanHomNay.get();
        }

        // 2. Tìm ca đầu tiên của ngày hôm sau (cho ca đêm hoặc ca cuối ngày)
        Optional<String> nguoiNhanNgayMai = lichLamViecRepository.findByNgayLamBetween(ngay.plusDays(1), ngay.plusDays(1)).stream()
            .filter(l -> Boolean.TRUE.equals(l.getCaLam().getDangBat()))
            .sorted(Comparator.comparing(l -> l.getCaLam().getBatDau()))
            .map(l -> l.getNhanVien().getTenTaiKhoan() + " (" + l.getCaLam().getTenCa() + ")")
            .findFirst();
        if (nguoiNhanNgayMai.isPresent()) {
            return nguoiNhanNgayMai.get();
        }

        // 3. Nếu chưa có nhân viên được xếp lịch, tìm ca kế tiếp theo danh mục ca
        List<com.example.datn.entity.CaLam> dsCa = caLamRepository.findByDangBatTrueOrderByBatDauAsc();
        if (!dsCa.isEmpty()) {
            for (int i = 0; i < dsCa.size(); i++) {
                if (dsCa.get(i).getId().equals(ca.getCaTheoLich().getId())) {
                    com.example.datn.entity.CaLam caKe = dsCa.get((i + 1) % dsCa.size());
                    return "Chờ bàn giao: " + caKe.getTenCa();
                }
            }
        }
        return "Cuối ngày";
    }

    // ---- helpers ----

    private CaLamViec timVaKiemQuyen(Long id) {
        return caLamViecRepository.findByIdAndDaXoaFalse(id)
            .orElseThrow(() -> bad("CA_KHONG_TON_TAI", "Không tìm thấy ca làm việc"));
    }

    private void kiemTraHoaDonDangXuLy(Long idCa) {
        List<HoaDon> dangXuLy = caLamViecRepository.hoaDonDangXuLy(idCa);
        if (!dangXuLy.isEmpty()) {
            String maCacHd = dangXuLy.stream().map(HoaDon::getMaHoaDon).reduce((a, b) -> a + " " + b).orElse("");
            throw bad("CON_HD_DANG_XU_LY", "Còn hóa đơn tại quầy chưa hoàn tất (" + maCacHd + "). Vui lòng thanh toán hoặc hủy đơn chờ trước khi kết ca!");
        }
    }

    private TongKetDto toTongKet(CaLamViec ca) {
        Long idCa = ca.getId();
        int tongHoaDon = caLamViecRepository.demHoaDonTaiQuayHoanThanh(idCa);
        BigDecimal tongDoanhThu = caLamViecRepository.tongDoanhThuHieuLuc(idCa);
        BigDecimal tienMatThu = caLamViecRepository.tongTienMatHieuLuc(idCa, TEN_TIEN_MAT);
        BigDecimal tienKhongMat = caLamViecRepository.tongTienKhongMatHieuLuc(idCa, TEN_TIEN_MAT);
        BigDecimal tienHoanMat = caLamViecRepository.tongTienHoanMat(idCa, TEN_TIEN_MAT);
        BigDecimal tienDuKien = ca.getTienDauCa().add(tienMatThu).subtract(tienHoanMat);

        List<TongKetDto.HoaDonTongHopItem> hoaDonGanDay = caLamViecRepository
            .hoaDonGanDay(idCa, PageRequest.of(0, MA_HOA_DON_TONG_KET_TOI_DA))
            .stream()
            .map(h -> TongKetDto.HoaDonTongHopItem.builder()
                .maHoaDon(h.getMaHoaDon())
                .ngayTao(h.getNgayTao())
                .tenPhuongThuc(h.getPhuongThucThanhToan() != null
                    ? h.getPhuongThucThanhToan().getTenPhuongThuc()
                    : null)
                .tienSauGiamGia(h.getTienSauGiamGia())
                .trangThai(h.getTrangThai())
                .build())
            .toList();

        NhanVien nv = ca.getNhanVien();
        return TongKetDto.builder()
            .id(ca.getId())
            .maCa(ca.getMaCa())
            .tenCaLich(ca.getCaTheoLich() != null ? ca.getCaTheoLich().getTenCa() : null)
            .idNhanVien(nv.getId())
            .tenNhanVien(nv.getTenTaiKhoan())
            .tenNguoiNhan(timNguoiNhanTiepTheo(ca, LocalDateTime.now()))
            .tenQuy(ca.getTenQuy())
            .thoiGianMo(ca.getThoiGianMo())
            .trangThai(ca.getTrangThai())
            .tienDauCa(ca.getTienDauCa())
            .tongHoaDon(tongHoaDon)
            .tongDoanhThu(tongDoanhThu)
            .tienMatThu(tienMatThu)
            .tienKhongMat(tienKhongMat)
            .tienHoanMat(tienHoanMat)
            .tienDuKien(tienDuKien)
            .hoaDonGanDay(hoaDonGanDay)
            .build();
    }

    private List<LichSuCaDto> lichSuCuaCa(Long idCa) {
        return lichSuCaRepository.findByCaLamViecId(idCa).stream()
            .map(l -> LichSuCaDto.builder()
                .hanhDong(l.getHanhDong())
                .tenNhanVien(l.getNhanVien() != null ? l.getNhanVien().getTenTaiKhoan() : null)
                .thoiGian(l.getThoiGian())
                .ghiChu(l.getGhiChu())
                .build())
            .toList();
    }

    private void ghiLichSu(CaLamViec ca, String hanhDong, String ghiChu, Object truocSau) {
        LichSuCa lichSu = new LichSuCa();
        lichSu.setCaLamViec(ca);
        lichSu.setHanhDong(hanhDong);
        lichSu.setNhanVien(ca.getNhanVien());
        lichSu.setGhiChu(ghiChu);
        lichSu.setTruocSau(chuyenJson(truocSau));
        lichSuCaRepository.save(lichSu);
    }

    private String chuyenJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    public CaLamViecDto toDto(CaLamViec ca) {
        NhanVien nv = ca.getNhanVien();
        return CaLamViecDto.builder()
            .id(ca.getId())
            .maCa(ca.getMaCa())
            .idCaLich(ca.getCaTheoLich() != null ? ca.getCaTheoLich().getId() : null)
            .tenCaLich(ca.getCaTheoLich() != null ? ca.getCaTheoLich().getTenCa() : null)
            .idNhanVien(nv.getId())
            .maNhanVien(nv.getMaNhanVien())
            .tenNhanVien(nv.getTenTaiKhoan())
            .tenQuy(ca.getTenQuy())
            .thoiGianMo(ca.getThoiGianMo())
            .thoiGianKet(ca.getThoiGianKet())
            .tienDauCa(ca.getTienDauCa())
            .trangThai(ca.getTrangThai())
            .tongHoaDon(ca.getTongHoaDon())
            .tongDoanhThu(ca.getTongDoanhThu())
            .tienMatThu(ca.getTienMatThu())
            .tienKhongMat(ca.getTienKhongMat())
            .tienHoanMat(ca.getTienHoanMat())
            .tienDuKien(ca.getTienDuKien())
            .tienThucTe(ca.getTienThucTe())
            .tienChi(ca.getTienChi())
            .tenNguoiNhan(timNguoiNhanTiepTheo(ca,
                ca.getThoiGianKet() != null ? ca.getThoiGianKet() : LocalDateTime.now()))
            .chenhLech(ca.getChenhLech())
            .lyDoLech(ca.getLyDoLech())
            .ghiChuMo(ca.getGhiChuMo())
            .build();
    }

    private static String taoMaCa(LocalDateTime thoiGianMo, Long idNhanVien) {
        return "GC-" + String.format("%04d%02d%02d-%02d%02d%02d-%d",
            thoiGianMo.getYear(), thoiGianMo.getMonthValue(), thoiGianMo.getDayOfMonth(),
            thoiGianMo.getHour(), thoiGianMo.getMinute(), thoiGianMo.getSecond(),
            idNhanVien);
    }

    private static Long idNhanVienHienTai() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        boolean laNhanVien = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_NHAN_VIEN".equals(a.getAuthority()));
        if (!laNhanVien) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cần đăng nhập nhân viên");
        }
        return id;
    }

    private static boolean laAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static ResponseStatusException bad(String ma, String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, ma + ": " + message);
    }

    public static boolean trongThoiGianNhanCa(LocalTime gioHienTai, LocalTime batDau, LocalTime ketThuc) {
        if (batDau == null || ketThuc == null) return false;
        LocalTime bdChoPhep = batDau.minusMinutes(30);
        if (ketThuc.isAfter(batDau)) {
            // Ca trong ngày (ví dụ 08:00 - 15:00 -> nhận ca từ 07:30 đến 15:00)
            return !gioHienTai.isBefore(bdChoPhep) && gioHienTai.isBefore(ketThuc);
        } else {
            // Ca qua đêm (ví dụ 22:00 - 08:00 -> nhận ca từ 21:30 đến 08:00 hôm sau)
            return !gioHienTai.isBefore(bdChoPhep) || gioHienTai.isBefore(ketThuc);
        }
    }

    private com.example.datn.entity.CaLam timCaTheoGioHienTai(LocalTime gioHienTai) {
        List<com.example.datn.entity.CaLam> caLamList = caLamRepository.findByDangBatTrueOrderByBatDauAsc();
        if (caLamList.isEmpty()) {
            return null;
        }
        for (com.example.datn.entity.CaLam c : caLamList) {
            if (trongThoiGianNhanCa(gioHienTai, c.getBatDau(), c.getKetThuc())) {
                return c;
            }
        }
        return null;
    }

    private static String chuanHoaGhiChu(String ghiChu) {
        return ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim();
    }
}
