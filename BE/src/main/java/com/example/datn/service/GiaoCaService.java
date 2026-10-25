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
import com.example.datn.repository.NhanVienRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public CaLamViecDto moCa(Long tienDauCa, String ghiChu) {
        Long idNhanVien = idNhanVienHienTai();
        if (tienDauCa == null || tienDauCa < 0) {
            throw bad("TIEN_DAU_CA", "Tiền đầu ca không được âm");
        }
        if (caLamViecRepository.findFirstByNhanVienIdAndTrangThaiAndDaXoaFalse(idNhanVien, TRANG_THAI_DANG_MO).isPresent()) {
            throw bad("CA_DA_MO", "Nhân viên đã có ca đang mở");
        }
        if (caLamViecRepository.findFirstByTrangThaiAndDaXoaFalse(TRANG_THAI_DANG_MO).isPresent()) {
            throw bad("CA_DA_MO", "Hệ thống đã có ca đang mở");
        }
        NhanVien nhanVien = nhanVienRepository.findById(idNhanVien)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));

        CaLamViec ca = new CaLamViec();
        ca.setNhanVien(nhanVien);
        ca.setTenQuy("Quầy tổng");
        ca.setThoiGianMo(LocalDateTime.now());
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
            .findFirstByNhanVienIdAndTrangThaiAndDaXoaFalse(idNhanVien, TRANG_THAI_DANG_MO)
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
        CaLamViec ca = timVaKiemQuyen(id);
        TongKetDto tongKet = toTongKet(ca);
        kiemTraHoaDonDangXuLy(id);

        if (tienThucTe == null || tienThucTe < 0) {
            throw bad("TIEN_THUC_TE", "Tiền thực tế không được âm");
        }
        BigDecimal tienThucTeDec = BigDecimal.valueOf(tienThucTe);
        BigDecimal chenhLech = tienThucTeDec.subtract(tongKet.getTienDuKien());
        String lyDoChuan = chuanHoaGhiChu(lyDoLech);
        if (chenhLech.compareTo(BigDecimal.ZERO) != 0 && lyDoChuan == null) {
            throw bad("LY_DO_LECH_THIEU", "Chênh lệch khác 0, bắt buộc nhập lý do");
        }

        LocalDateTime thoiGianKet = LocalDateTime.now();
        String nguoiCapNhat = String.valueOf(idNhanVienHienTai());
        int affected = caLamViecRepository.ketCaConditional(
            id,
            thoiGianKet,
            tongKet.getTongHoaDon(),
            tongKet.getTongDoanhThu(),
            tongKet.getTienMatThu(),
            tongKet.getTienKhongMat(),
            tongKet.getTienHoanMat(),
            tongKet.getTienDuKien(),
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
        snapshot.put("chenhLech", String.valueOf(daKet.getChenhLech()));
        snapshot.put("lyDoLech", daKet.getLyDoLech());
        ghiLichSu(daKet, "KET_CA", chuanHoaGhiChu(ghiChu), snapshot);
        return toDto(daKet);
    }

    @Transactional(readOnly = true)
    public CaLamViecDto doiSoat(Long id) {
        CaLamViec ca = timVaKiemQuyen(id);
        CaLamViecDto dto = toDto(ca);
        dto.setLichSu(lichSuCuaCa(id));
        return dto;
    }

    @Transactional(readOnly = true)
    public Page<CaLamViecDto> lichSu(
        int page, int size, LocalDate tuNgay, LocalDate denNgay,
        String trangThai, Long idNhanVien, Long callerId, boolean callerAdmin
    ) {
        Long idLoc = callerAdmin ? idNhanVien : callerId;
        LocalDateTime tu = tuNgay == null ? null : tuNgay.atStartOfDay();
        LocalDateTime den = denNgay == null ? null : denNgay.plusDays(1).atTime(LocalTime.MIDNIGHT);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return caLamViecRepository.lichSu(idLoc, trangThai, tu, den, pageable).map(this::toDto);
    }

    // ---- helpers ----

    private CaLamViec timVaKiemQuyen(Long id) {
        CaLamViec ca = caLamViecRepository.findByIdAndDaXoaFalse(id)
            .orElseThrow(() -> bad("CA_KHONG_TON_TAI", "Không tìm thấy ca làm việc"));
        Long idNguoiGoi = idNhanVienHienTai();
        boolean admin = laAdmin();
        if (!admin && !ca.getNhanVien().getId().equals(idNguoiGoi)) {
            throw new KhongCoQuyenException("Không có quyền thao tác ca này");
        }
        return ca;
    }

    private void kiemTraHoaDonDangXuLy(Long idCa) {
        List<HoaDon> dangXuLy = caLamViecRepository.hoaDonDangXuLy(idCa);
        if (!dangXuLy.isEmpty()) {
            String maCacHd = dangXuLy.stream().map(HoaDon::getMaHoaDon).reduce((a, b) -> a + " " + b).orElse("");
            throw bad("CON_HD_DANG_XU_LY", "Còn hóa đơn đang xử lý: " + maCacHd);
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
            .idNhanVien(nv.getId())
            .tenNhanVien(nv.getTenTaiKhoan())
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

    private CaLamViecDto toDto(CaLamViec ca) {
        NhanVien nv = ca.getNhanVien();
        return CaLamViecDto.builder()
            .id(ca.getId())
            .maCa(ca.getMaCa())
            .idNhanVien(nv.getId())
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

    private static String chuanHoaGhiChu(String ghiChu) {
        return ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim();
    }
}
