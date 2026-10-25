package com.example.datn.service.impls;

import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaDetailResponse;
import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.KhachHangPhieuGiamGia;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.repository.KhachHangPhieuGiamGiaRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.service.EmailService;
import com.example.datn.service.EmailService.LoaiThongBao;
import com.example.datn.service.PhieuGiamGiaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaServiceImpl implements PhieuGiamGiaService {

    private static final String CONG_KHAI = "CONG_KHAI";
    private static final String CA_NHAN = "CA_NHAN";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat VND = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

    private final PhieuGiamGiaRepository phieuRepo;
    private final KhachHangRepository khachHangRepo;
    private final KhachHangPhieuGiamGiaRepository khPhieuRepo;
    private final EmailService emailService;

    @Override
    public Page<PhieuGiamGia> getPageList(Pageable pageable) {
        return phieuRepo.findAll(pageable);
    }

    @Override
    public void addPhieuGiamGia(PhieuGiamGia phieuGiamGia) {
        phieuGiamGia.setTrangThai(true);
        phieuGiamGia.setSoLuongDaSuDung(0);
        phieuGiamGia.setNgayTao(LocalDateTime.now());
        phieuRepo.save(phieuGiamGia);
    }

    @Override
    public PhieuGiamGia detail(Long id) {
        return phieuRepo.findById(id).orElse(null);
    }

    @Override
    public void update(PhieuGiamGia req) {
        if (req == null || req.getId() == null) {
            throw new IllegalArgumentException("Thiếu thông tin phiếu giảm giá cần cập nhật");
        }
        PhieuGiamGia db = phieuRepo.findById(req.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá"));

        String ma = req.getMaPhieuGiamGia() == null ? "" : req.getMaPhieuGiamGia().trim();
        String ten = req.getTenPhieuGiamGia() == null ? "" : req.getTenPhieuGiamGia().trim();
        if (ma.isEmpty() || ma.length() > 50) throw bad("Mã phiếu không được để trống và tối đa 50 ký tự");
        if (phieuRepo.existsByMaPhieuGiamGiaAndIdNot(ma, req.getId())) throw bad("Mã phiếu đã tồn tại");
        if (ten.isEmpty() || ten.length() > 200) throw bad("Tên phiếu không được để trống và tối đa 200 ký tự");
        if (!"PERCENT".equals(req.getLoaiGiamGia()) && !"AMOUNT".equals(req.getLoaiGiamGia()))
            throw bad("Loại giảm giá không hợp lệ");
        if (req.getGiaTriGiam() == null || req.getGiaTriGiam().signum() <= 0
                || ("PERCENT".equals(req.getLoaiGiamGia())
                    && req.getGiaTriGiam().compareTo(BigDecimal.valueOf(100)) > 0))
            throw bad("Giá trị giảm không hợp lệ (phần trăm tối đa 100)");
        if (req.getGiamToiDa() != null && req.getGiamToiDa().signum() < 0)
            throw bad("Mức giảm tối đa không được âm");
        if (req.getHoaDonToiThieu() != null && req.getHoaDonToiThieu().signum() < 0)
            throw bad("Hóa đơn tối thiểu không được âm");
        if (req.getSoLuong() == null || req.getSoLuong() < 1
                || req.getSoLuong() < (db.getSoLuongDaSuDung() == null ? 0 : db.getSoLuongDaSuDung()))
            throw bad("Số lượng không được nhỏ hơn 1 hoặc số lượng đã sử dụng");
        if (req.getNgayBatDau() == null || req.getNgayKetThuc() == null
                || !req.getNgayKetThuc().isAfter(req.getNgayBatDau()))
            throw bad("Ngày kết thúc phải sau ngày bắt đầu");
        if (req.getTrangThai() == null) throw bad("Vui lòng chọn trạng thái");
        if (req.getMoTa() != null && req.getMoTa().length() > 500)
            throw bad("Mô tả không được vượt quá 500 ký tự");

        db.setMaPhieuGiamGia(ma);
        db.setTenPhieuGiamGia(ten);
        db.setLoaiGiamGia(req.getLoaiGiamGia());
        db.setGiaTriGiam(req.getGiaTriGiam());
        db.setGiamToiDa(req.getGiamToiDa());
        db.setHoaDonToiThieu(req.getHoaDonToiThieu());
        db.setSoLuong(req.getSoLuong());
        db.setNgayBatDau(req.getNgayBatDau());
        db.setNgayKetThuc(req.getNgayKetThuc());
        db.setTrangThai(req.getTrangThai());
        db.setMoTa(req.getMoTa());
        db.setNgayCapNhat(LocalDateTime.now());

        phieuRepo.save(db);
    }

    @Transactional
    public Long capNhatPhieuGiamGia(Long id, PhieuGiamGiaRequest req) {
        // 1. Tìm phiếu
        PhieuGiamGia db = phieuRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phiếu giảm giá"));

        // Ghi nhớ trạng thái trước khi sửa để biết có bật/tắt hay không
        boolean wasActive = Boolean.TRUE.equals(db.getTrangThai());

        // 2. Validate dữ liệu đầu vào
        validate(req);
        if (phieuRepo.existsByMaPhieuGiamGiaAndIdNot(req.getMaPhieuGiamGia().trim(), id)) {
            throw bad("Mã phiếu đã tồn tại");
        }

        // 3. Không cho phép đổi phạm vi
        if (!db.getPhamVi().equals(req.getPhamVi())) {
            throw bad("Không thể thay đổi phạm vi của phiếu đã tạo");
        }

        // 4. Số lượng không được nhỏ hơn số đã sử dụng
        int daDung = db.getSoLuongDaSuDung() == null ? 0 : db.getSoLuongDaSuDung();
        if (req.getSoLuong() < daDung) {
            throw bad("Số lượng không được nhỏ hơn số lượng đã sử dụng (" + daDung + ")");
        }

        boolean isCaNhan = CA_NHAN.equals(db.getPhamVi());
        boolean isPercent = "PERCENT".equals(req.getLoaiGiamGia());

        // ==================== Xử lý danh sách khách hàng (chỉ phiếu cá nhân) ====================
        List<KhachHangPhieuGiamGia> canXoa = List.of();
        List<KhachHang> khachMoi = List.of();
        List<KhachHang> khachCu = List.of(); // khách giữ nguyên, chưa dùng phiếu -> nhận mail cập nhật/bật/tắt

        if (isCaNhan) {
            Set<Long> idMoi = Set.copyOf(req.getDanhSachKhachHangId());

            // Số lượng phải >= số khách được chọn
            if (req.getSoLuong() < idMoi.size()) {
                throw bad("Số lượng phải ≥ số khách hàng được chọn (" + idMoi.size() + ")");
            }

            List<KhachHangPhieuGiamGia> hienTai = khPhieuRepo.findByPhieuId(id);
            Set<Long> idHienTai = hienTai.stream()
                    .map(row -> row.getKhachHang().getId())
                    .collect(Collectors.toSet());

            // Khách bị bỏ chọn
            canXoa = hienTai.stream()
                    .filter(row -> !idMoi.contains(row.getKhachHang().getId()))
                    .toList();

            // Không cho bỏ khách đã sử dụng phiếu
            if (canXoa.stream().anyMatch(row -> row.getNgaySuDung() != null)) {
                throw bad("Không thể bỏ khách hàng đã sử dụng phiếu này");
            }

            // Khách ở lại và chưa dùng phiếu
            khachCu = hienTai.stream()
                    .filter(row -> idMoi.contains(row.getKhachHang().getId()))
                    .filter(row -> row.getNgaySuDung() == null)
                    .map(KhachHangPhieuGiamGia::getKhachHang)
                    .toList();

            // Khách mới được thêm
            Set<Long> idThem = idMoi.stream()
                    .filter(x -> !idHienTai.contains(x))
                    .collect(Collectors.toSet());

            khachMoi = khachHangRepo.findAllById(idThem);
            if (khachMoi.size() != idThem.size()) {
                throw bad("Có khách hàng không tồn tại");
            }
        }

        // ==================== Cập nhật thông tin phiếu ====================
        db.setMaPhieuGiamGia(req.getMaPhieuGiamGia().trim());
        db.setTenPhieuGiamGia(req.getTenPhieuGiamGia().trim());
        db.setLoaiGiamGia(req.getLoaiGiamGia());
        db.setGiaTriGiam(req.getGiaTriGiam());
        db.setGiamToiDa(isPercent ? req.getGiamToiDa() : null);
        db.setHoaDonToiThieu(req.getHoaDonToiThieu() != null ? req.getHoaDonToiThieu() : BigDecimal.ZERO);
        db.setNgayBatDau(req.getNgayBatDau());
        db.setNgayKetThuc(req.getNgayKetThuc());
        db.setSoLuong(req.getSoLuong());
        db.setTrangThai(req.getTrangThai());
        db.setMoTa(req.getMoTa());
        db.setNgayCapNhat(LocalDateTime.now());
        // Giữ nguyên: soLuongDaSuDung, ngayTao, phamVi

        phieuRepo.save(db);

        // ==================== Đồng bộ bảng trung gian ====================
        if (!canXoa.isEmpty()) {
            khPhieuRepo.deleteAll(canXoa);
        }

        if (!khachMoi.isEmpty()) {
            List<KhachHangPhieuGiamGia> rows = khachMoi.stream()
                    .map(kh -> {
                        KhachHangPhieuGiamGia row = new KhachHangPhieuGiamGia();
                        row.setKhachHang(kh);
                        row.setPhieuGiamGia(db);
                        row.setTrangThai(true);
                        return row;
                    })
                    .toList();
            khPhieuRepo.saveAll(rows);

            // Khách mới được thêm: gửi mail nhận phiếu
            guiMailPhieuCaNhan(db, khachMoi, LoaiThongBao.MOI);
        }

        // ==================== Mail cho khách cũ ====================
        // Trạng thái đổi -> báo bật/tắt; không đổi -> báo cập nhật (chỉ khi phiếu đang bật)
        if (isCaNhan && !khachCu.isEmpty()) {
            boolean nowActive = Boolean.TRUE.equals(db.getTrangThai());
            LoaiThongBao loai;
            if (wasActive != nowActive) {
                loai = nowActive ? LoaiThongBao.BAT : LoaiThongBao.TAT;
            } else {
                loai = nowActive ? LoaiThongBao.CAP_NHAT : null;
            }
            if (loai != null) {
                guiMailPhieuCaNhan(db, khachCu, loai);
            }
        }

        return db.getId();
    }

    @Transactional
    public Long themPhieuGiamGia(PhieuGiamGiaRequest req) {
        validate(req);
        if (phieuRepo.existsByMaPhieuGiamGia(req.getMaPhieuGiamGia().trim())) {
            throw bad("Mã phiếu đã tồn tại");
        }

        boolean caNhan = CA_NHAN.equals(req.getPhamVi());
        boolean isPercent = "PERCENT".equals(req.getLoaiGiamGia());

        // Với phiếu cá nhân: kiểm tra toàn bộ khách hàng tồn tại TRƯỚC khi lưu phiếu
        List<KhachHang> khachHangs = List.of();
        if (caNhan) {
            Set<Long> ids = Set.copyOf(req.getDanhSachKhachHangId());
            khachHangs = khachHangRepo.findAllById(ids);
            if (khachHangs.size() != ids.size()) {
                throw bad("Có khách hàng không tồn tại");
            }
        }

        PhieuGiamGia p = new PhieuGiamGia();
        p.setMaPhieuGiamGia(req.getMaPhieuGiamGia().trim());
        p.setTenPhieuGiamGia(req.getTenPhieuGiamGia().trim());
        p.setLoaiGiamGia(req.getLoaiGiamGia());
        p.setGiaTriGiam(req.getGiaTriGiam());
        p.setGiamToiDa(isPercent ? req.getGiamToiDa() : null);
        p.setHoaDonToiThieu(req.getHoaDonToiThieu() != null ? req.getHoaDonToiThieu() : BigDecimal.ZERO);
        p.setNgayBatDau(req.getNgayBatDau());
        p.setNgayKetThuc(req.getNgayKetThuc());
        p.setSoLuong(req.getSoLuong());
        p.setSoLuongDaSuDung(0);
        p.setTrangThai(req.getTrangThai() == null || req.getTrangThai());
        p.setPhamVi(caNhan ? CA_NHAN : CONG_KHAI);
        p.setMoTa(req.getMoTa());
        p.setNgayTao(LocalDateTime.now());
        p.setNgayCapNhat(LocalDateTime.now());
        p = phieuRepo.save(p);

        // Lưu từng cặp (id_khach_hang, id_phieu_giam_gia)
        if (caNhan) {
            final PhieuGiamGia phieu = p;
            List<KhachHangPhieuGiamGia> rows = khachHangs.stream().map(kh -> {
                KhachHangPhieuGiamGia row = new KhachHangPhieuGiamGia();
                row.setKhachHang(kh);
                row.setPhieuGiamGia(phieu);
                row.setTrangThai(true);
                return row;
            }).collect(Collectors.toList());
            khPhieuRepo.saveAll(rows);

            // Gửi mail cho toàn bộ khách được chọn
            guiMailPhieuCaNhan(phieu, khachHangs, LoaiThongBao.MOI);
        }

        return p.getId();
    }

    /**
     * Gửi mail phiếu giảm giá cá nhân cho danh sách khách hàng.
     * Chỉ gửi SAU KHI transaction commit thành công, tránh trường hợp
     * mail đã gửi mà dữ liệu bị rollback.
     *
     * Quy tắc: loại TAT chỉ gửi khi phiếu đang tắt;
     * các loại còn lại (MOI, CAP_NHAT, BAT) chỉ gửi khi phiếu đang bật.
     */
    private void guiMailPhieuCaNhan(PhieuGiamGia p, List<KhachHang> dsKhach, LoaiThongBao loai) {
        if (dsKhach == null || dsKhach.isEmpty()) return;

        boolean active = Boolean.TRUE.equals(p.getTrangThai());
        boolean duocGui = (loai == LoaiThongBao.TAT) ? !active : active;
        if (!duocGui) return;

        String giaTri = "PERCENT".equals(p.getLoaiGiamGia())
                ? p.getGiaTriGiam().stripTrailingZeros().toPlainString() + "%"
                : VND.format(p.getGiaTriGiam()) + "đ";

        StringBuilder dk = new StringBuilder();
        if (p.getHoaDonToiThieu() != null && p.getHoaDonToiThieu().signum() > 0) {
            dk.append("Đơn hàng từ ").append(VND.format(p.getHoaDonToiThieu())).append("đ");
        }
        if (p.getGiamToiDa() != null) {
            if (dk.length() > 0) dk.append(", ");
            dk.append("giảm tối đa ").append(VND.format(p.getGiamToiDa())).append("đ");
        }
        String dieuKien = dk.length() > 0 ? dk.toString() : "Áp dụng cho mọi đơn hàng";

        // Lấy dữ liệu ra biến thường trước, tránh phụ thuộc entity sau commit
        String ma = p.getMaPhieuGiamGia();
        String ten = p.getTenPhieuGiamGia();
        String batDau = p.getNgayBatDau().format(DATE_FMT);
        String ketThuc = p.getNgayKetThuc().format(DATE_FMT);

        List<String[]> nguoiNhan = dsKhach.stream()
                .map(kh -> new String[]{kh.getEmail(), kh.getTenKhachHang()})
                .toList();

        Runnable guiMail = () -> nguoiNhan.forEach(n ->
                emailService.guiMailPhieuGiamGiaCaNhan(
                        n[0], n[1], ma, ten, giaTri, dieuKien, batDau, ketThuc, loai));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    guiMail.run();
                }
            });
        } else {
            guiMail.run();
        }
    }

    private void validate(PhieuGiamGiaRequest r) {
        if (r == null) throw bad("Thiếu thông tin phiếu giảm giá");
        if (isBlank(r.getMaPhieuGiamGia())) throw bad("Vui lòng nhập mã phiếu");
        if (isBlank(r.getTenPhieuGiamGia())) throw bad("Vui lòng nhập tên phiếu");
        if (r.getMaPhieuGiamGia().trim().length() > 50) throw bad("Mã phiếu tối đa 50 ký tự");
        if (r.getTenPhieuGiamGia().trim().length() > 200) throw bad("Tên phiếu tối đa 200 ký tự");
        if (r.getMoTa() != null && r.getMoTa().length() > 500) throw bad("Mô tả tối đa 500 ký tự");

        if (!"PERCENT".equals(r.getLoaiGiamGia()) && !"AMOUNT".equals(r.getLoaiGiamGia()))
            throw bad("Loại giảm giá không hợp lệ");
        if (r.getGiaTriGiam() == null || r.getGiaTriGiam().signum() <= 0)
            throw bad("Giá trị giảm phải lớn hơn 0");
        if (r.getGiamToiDa() != null && r.getGiamToiDa().signum() < 0)
            throw bad("Mức giảm tối đa không được âm");
        if (r.getHoaDonToiThieu() != null && r.getHoaDonToiThieu().signum() < 0)
            throw bad("Hóa đơn tối thiểu không được âm");
        if ("PERCENT".equals(r.getLoaiGiamGia())
                && r.getGiaTriGiam().compareTo(BigDecimal.valueOf(100)) > 0)
            throw bad("Phần trăm giảm tối đa là 100");

        if (r.getSoLuong() == null || r.getSoLuong() < 1) throw bad("Số lượng phải từ 1 trở lên");

        if (r.getNgayBatDau() == null || r.getNgayKetThuc() == null)
            throw bad("Vui lòng chọn ngày bắt đầu và ngày kết thúc");
        if (!r.getNgayKetThuc().isAfter(r.getNgayBatDau()))
            throw bad("Ngày kết thúc phải sau ngày bắt đầu");

        String pv = r.getPhamVi();
        if (!CONG_KHAI.equals(pv) && !CA_NHAN.equals(pv)) throw bad("Phạm vi không hợp lệ");
        if (CA_NHAN.equals(pv)
                && (r.getDanhSachKhachHangId() == null || r.getDanhSachKhachHangId().isEmpty()))
            throw bad("Phiếu cá nhân phải chọn ít nhất 1 khách hàng");
        if (CA_NHAN.equals(pv) && r.getDanhSachKhachHangId().stream().anyMatch(id -> id == null || id <= 0))
            throw bad("Danh sách khách hàng chứa mã không hợp lệ");
    }

    @Transactional
    public Boolean toggleTrangThai(Long id) {
        PhieuGiamGia phieu = phieuRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy phiếu giảm giá"));

        Boolean newStatus = !Boolean.TRUE.equals(phieu.getTrangThai());
        phieu.setTrangThai(newStatus);
        phieu.setNgayCapNhat(LocalDateTime.now());
        phieuRepo.save(phieu);

        // Phiếu cá nhân: báo cho khách chưa dùng phiếu
        if (CA_NHAN.equals(phieu.getPhamVi())) {
            List<KhachHang> dsKhach = khPhieuRepo.findByPhieuId(id).stream()
                    .filter(r -> r.getNgaySuDung() == null)
                    .map(KhachHangPhieuGiamGia::getKhachHang)
                    .toList();
            guiMailPhieuCaNhan(phieu, dsKhach,
                    newStatus ? LoaiThongBao.BAT : LoaiThongBao.TAT);
        }

        return newStatus;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    @Transactional
    public PhieuGiamGiaDetailResponse chiTiet(Long id) {
        PhieuGiamGia p = phieuRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu giảm giá"));

        List<PhieuGiamGiaDetailResponse.KhachHangItem> khachHangs = List.of();
        if (CA_NHAN.equals(p.getPhamVi())) {
            khachHangs = khPhieuRepo.findByPhieuId(id).stream()
                    .map(r -> PhieuGiamGiaDetailResponse.KhachHangItem.builder()
                            .idKhachHang(r.getKhachHang().getId())
                            .maKhachHang(r.getKhachHang().getMaKhachHang())
                            .tenKhachHang(r.getKhachHang().getTenKhachHang())
                            .email(r.getKhachHang().getEmail())
                            .soDienThoai(r.getKhachHang().getSoDienThoai())
                            .ngaySuDung(r.getNgaySuDung())
                            .trangThai(r.getTrangThai())
                            .build())
                    .toList();
        }

        return PhieuGiamGiaDetailResponse.builder()
                .id(p.getId())
                .maPhieuGiamGia(p.getMaPhieuGiamGia())
                .tenPhieuGiamGia(p.getTenPhieuGiamGia())
                .loaiGiamGia(p.getLoaiGiamGia())
                .giaTriGiam(p.getGiaTriGiam())
                .giamToiDa(p.getGiamToiDa())
                .hoaDonToiThieu(p.getHoaDonToiThieu())
                .ngayBatDau(p.getNgayBatDau())
                .ngayKetThuc(p.getNgayKetThuc())
                .soLuong(p.getSoLuong())
                .soLuongDaSuDung(p.getSoLuongDaSuDung())
                .gioiHanMoiTaiKhoan(p.getGioiHanMoiTaiKhoan())
                .trangThai(p.getTrangThai())
                .phamVi(p.getPhamVi())
                .ngayTao(p.getNgayTao())
                .moTa(p.getMoTa())
                .khachHangs(khachHangs)
                .build();
    }

    public Page<PhieuGiamGia> search(
            String keyword, String loaiGiamGia, String trangThai, String phamVi,
            LocalDate tuNgay, LocalDate denNgay, int page, int size) {

        String kw = keyword == null ? "" : keyword.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

        LocalDateTime from = tuNgay != null
                ? tuNgay.atStartOfDay()
                : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime to = denNgay != null
                ? denNgay.plusDays(1).atStartOfDay()
                : LocalDateTime.of(2999, 12, 31, 0, 0);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        return phieuRepo.search(
                "%" + kw + "%",
                loaiGiamGia == null ? "" : loaiGiamGia.trim(),
                phamVi == null ? "" : phamVi.trim(),
                trangThai == null ? "" : trangThai.trim(),
                LocalDateTime.now(),
                from,
                to,
                pageable);
    }
}