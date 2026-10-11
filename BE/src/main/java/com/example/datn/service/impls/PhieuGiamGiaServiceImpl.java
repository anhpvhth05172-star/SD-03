package com.example.datn.service.impls;

import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaDetailResponse;
import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.KhachHangPhieuGiamGia;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.repository.KhachHangPhieuGiamGiaRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.service.PhieuGiamGiaService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PhieuGiamGiaServiceImpl implements PhieuGiamGiaService {

    private static final String CONG_KHAI = "CONG_KHAI";
    private static final String CA_NHAN = "CA_NHAN";

    private final PhieuGiamGiaRepository phieuRepo;
    private final KhachHangRepository khachHangRepo;
    private final KhachHangPhieuGiamGiaRepository khPhieuRepo;

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

//    @Transactional
//    public List<KhachHangOptionResponse> layDanhSachKhachHang() {
//        return khachHangRepo
//                .findByVaiTroAndTrangThaiOrderByTenKhachHangAsc("USER", true)
//                .stream()
//                .map(KhachHangOptionResponse::from)
//                .toList();
//    }

    @Transactional
    public Long themPhieuGiamGia(PhieuGiamGiaRequest req) {
        validate(req);

        boolean caNhan = CA_NHAN.equals(req.getPhamVi());

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
        p.setGiamToiDa(req.getGiamToiDa());
        p.setHoaDonToiThieu(req.getHoaDonToiThieu() != null ? req.getHoaDonToiThieu() : BigDecimal.ZERO);
        p.setNgayBatDau(req.getNgayBatDau());
        p.setNgayKetThuc(req.getNgayKetThuc());
        p.setSoLuong(req.getSoLuong());
        p.setSoLuongDaSuDung(0);
        p.setTrangThai(req.getTrangThai() == null || req.getTrangThai());
        p.setPhamVi(caNhan ? CA_NHAN : CONG_KHAI);
        p.setMoTa(req.getMoTa());
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
        }

        return p.getId();
    }

    private void validate(PhieuGiamGiaRequest r) {
        if (r == null) throw bad("Thiếu thông tin phiếu giảm giá");
        if (isBlank(r.getMaPhieuGiamGia())) throw bad("Vui lòng nhập mã phiếu");
        if (isBlank(r.getTenPhieuGiamGia())) throw bad("Vui lòng nhập tên phiếu");
        if (r.getMaPhieuGiamGia().trim().length() > 50) throw bad("Mã phiếu tối đa 50 ký tự");
        if (r.getTenPhieuGiamGia().trim().length() > 200) throw bad("Tên phiếu tối đa 200 ký tự");
        if (r.getMoTa() != null && r.getMoTa().length() > 500) throw bad("Mô tả tối đa 500 ký tự");
        if (phieuRepo.existsByMaPhieuGiamGia(r.getMaPhieuGiamGia().trim()))
            throw bad("Mã phiếu đã tồn tại");

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

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    @Transactional
    public PhieuGiamGiaDetailResponse chiTiet(Long id) {
        PhieuGiamGia p = phieuRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phiếu giảm giá"));

        List<PhieuGiamGiaDetailResponse.KhachHangItem> khachHangs = List.of();
        if ("CA_NHAN".equals(p.getPhamVi())) {
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
}
