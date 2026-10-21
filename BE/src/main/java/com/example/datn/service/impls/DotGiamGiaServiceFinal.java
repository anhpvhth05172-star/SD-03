package com.example.datn.service.impls;

import com.example.datn.dto.DotGiamGia.DotGiamGiaDetailResponse;
import com.example.datn.dto.DotGiamGia.DotGiamGiaRequest;
import com.example.datn.dto.DotGiamGia.SanPhamChiTietOptionResponse;
import com.example.datn.entity.DotGiamGia;
import com.example.datn.entity.DotGiamGiaChiTiet;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.DotGiamGiaChiTietRepository;
import com.example.datn.repository.DotGiamGiaRepository;
import com.example.datn.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DotGiamGiaServiceFinal {

    private final DotGiamGiaRepository dotRepo;
    private final DotGiamGiaChiTietRepository chiTietRepo;
    private final SanPhamChiTietRepository spctRepo;

    /**
     * Gom các đợt đang hiệu lực theo biến thể, giữ lại đợt có % CAO NHẤT.
     * Trả về: variantId -> {variantId, tenDot, phanTram}
     */
    private Map<Long, Object[]> maxDotTheoBienThe(Long excludeId) {
        Long ex = (excludeId == null) ? -1L : excludeId;
        Map<Long, Object[]> result = new HashMap<>();
        for (Object[] r : chiTietRepo.findApDungHienTai(LocalDateTime.now(), ex)) {
            if (r[0] == null || r[2] == null) continue;
            Long variantId = ((Number) r[0]).longValue();
            Object[] old = result.get(variantId);
            if (old == null || toPercent(r[2]).compareTo(toPercent(old[2])) > 0) {
                result.put(variantId, r);
            }
        }
        return result;
    }

    /**
     * Danh sách biến thể để chọn vào đợt giảm giá.
     * Biến thể thuộc nhiều đợt đang hiệu lực -> trả về đợt có % cao nhất
     * (mức giảm áp dụng = max các đợt, không cộng dồn, không nhân chồng).
     */
    @Transactional(readOnly = true)
    public List<SanPhamChiTietOptionResponse> layDanhSachSanPham(Long excludeId) {
        List<SanPhamChiTietOptionResponse> list = spctRepo.findAllActiveForDiscount().stream()
                .map(SanPhamChiTietOptionResponse::from)
                .toList();

        Map<Long, Object[]> dangApDung = maxDotTheoBienThe(excludeId);

        for (SanPhamChiTietOptionResponse v : list) {
            Object[] r = dangApDung.get(v.getId());
            if (r != null) {
                v.setTenDotGiamGiaKhac((String) r[1]);
                v.setPhanTramGiamKhac(toPercent(r[2]).intValue());
            }
        }
        return list;
    }

    /**
     * % giảm thực tế của 1 biến thể tại thời điểm hiện tại = MAX các đợt đang hiệu lực.
     * Dùng khi tính giá cho giỏ hàng / hóa đơn. Trả về 0 nếu không thuộc đợt nào.
     */
    @Transactional(readOnly = true)
    public BigDecimal phanTramGiamHieuLuc(Long variantId) {
        Object[] r = maxDotTheoBienThe(null).get(variantId);
        return r == null ? BigDecimal.ZERO : toPercent(r[2]);
    }

    private BigDecimal toPercent(Object value) {
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }

    @Transactional
    public Long them(DotGiamGiaRequest r) {
        validate(r, null);

        Set<Long> ids = Set.copyOf(r.getDanhSachSanPhamChiTietId());
        List<SanPhamChiTiet> spcts = spctRepo.findAllById(ids);
        if (spcts.size() != ids.size()) {
            throw bad("Có sản phẩm không tồn tại");
        }

        // Cho phép 1 biến thể thuộc nhiều đợt cùng lúc: khi tính giá sẽ lấy max %.

        DotGiamGia d = new DotGiamGia();
        d.setMaDotGiamGia(r.getMaDotGiamGia().trim());
        d.setTenDotGiamGia(r.getTenDotGiamGia().trim());
        d.setPhanTramGiam(r.getPhanTramGiam());
        d.setNgayBatDau(r.getNgayBatDau());
        d.setNgayKetThuc(r.getNgayKetThuc());
        d.setTrangThai(r.getTrangThai() == null || r.getTrangThai());
        d.setMoTa(r.getMoTa());
        d = dotRepo.save(d);

        final DotGiamGia dot = d;
        List<DotGiamGiaChiTiet> rows = spcts.stream().map(s -> {
            DotGiamGiaChiTiet ct = new DotGiamGiaChiTiet();
            ct.setDotGiamGia(dot);
            ct.setSanPhamChiTiet(s);
            ct.setTrangThai(true);
            return ct;
        }).toList();
        chiTietRepo.saveAll(rows);

        return d.getId();
    }

    private void validate(DotGiamGiaRequest r, Long excludeId) {
        if (r.getMaDotGiamGia() == null || r.getMaDotGiamGia().isBlank())
            throw bad("Vui lòng nhập mã đợt");
        if (r.getTenDotGiamGia() == null || r.getTenDotGiamGia().isBlank())
            throw bad("Vui lòng nhập tên đợt");

        String ma = r.getMaDotGiamGia().trim();
        boolean trung = (excludeId == null)
                ? dotRepo.existsByMaDotGiamGia(ma)
                : dotRepo.existsByMaDotGiamGiaAndIdNot(ma, excludeId);
        if (trung) throw bad("Mã đợt đã tồn tại");

        if (r.getPhanTramGiam() == null
                || r.getPhanTramGiam().signum() <= 0
                || r.getPhanTramGiam().compareTo(BigDecimal.valueOf(100)) > 0)
            throw bad("Phần trăm giảm phải từ 1 đến 100");

        if (r.getNgayBatDau() == null || r.getNgayKetThuc() == null)
            throw bad("Vui lòng chọn ngày bắt đầu và ngày kết thúc");
        if (!r.getNgayKetThuc().isAfter(r.getNgayBatDau()))
            throw bad("Ngày kết thúc phải sau ngày bắt đầu");

        if (r.getDanhSachSanPhamChiTietId() == null || r.getDanhSachSanPhamChiTietId().isEmpty())
            throw bad("Vui lòng chọn ít nhất 1 sản phẩm");
    }

    private ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    @Transactional(readOnly = true)
    public DotGiamGiaDetailResponse chiTiet(Long id) {
        DotGiamGia d = dotRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đợt giảm giá"));

        List<DotGiamGiaDetailResponse.SanPhamItem> sanPhams = chiTietRepo.findByDotId(id).stream()
                .map(ct -> {
                    SanPhamChiTiet s = ct.getSanPhamChiTiet();
                    return DotGiamGiaDetailResponse.SanPhamItem.builder()
                            .id(s.getId())
                            .sanPhamId(s.getSanPham().getId())
                            .maSanPham(s.getSanPham().getMaSanPham())
                            .tenSanPham(s.getSanPham().getTenSanPham())
                            .maChiTiet(s.getMaChiTietSanPham())
                            .kichCo(s.getKichCo().getTenKichCo())   // ⚠️ chỉnh theo entity KichCo
                            .mauSac(s.getMauSac().getTenMau())      // ⚠️ chỉnh theo entity MauSac
                            .giaBan(s.getGiaBan())
                            .giaSauGiam(GiaService.giaSauGiam(s.getGiaBan(), d.getPhanTramGiam()))
                            .soLuong(s.getSoLuong())
                            .trangThai(ct.getTrangThai())
                            .build();
                })
                .toList();

        return DotGiamGiaDetailResponse.builder()
                .id(d.getId())
                .maDotGiamGia(d.getMaDotGiamGia())
                .tenDotGiamGia(d.getTenDotGiamGia())
                .phanTramGiam(d.getPhanTramGiam())
                .ngayBatDau(d.getNgayBatDau())
                .ngayKetThuc(d.getNgayKetThuc())
                .trangThai(d.getTrangThai())
                .moTa(d.getMoTa())
                .ngayTao(d.getNgayTao())
                .sanPhams(sanPhams)
                .build();
    }

    /**
     * Đổi trạng thái bật/tắt của đợt giảm giá.
     * Trả về trạng thái thực tế đã lưu để frontend đồng bộ.
     */
    @Transactional
    public Boolean doiTrangThai(Long id, Boolean trangThai) {
        if (trangThai == null) {
            throw bad("Trạng thái không được để trống");
        }

        DotGiamGia d = dotRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đợt giảm giá"));

        if (trangThai.equals(d.getTrangThai())) {
            return d.getTrangThai();
        }

        // Đợt đã kết thúc thì không bật lại được
        // (không kiểm tra trùng đợt: nhiều đợt cùng lúc sẽ lấy max %)
        if (trangThai && d.getNgayKetThuc().isBefore(LocalDateTime.now())) {
            throw bad("Đợt giảm giá đã kết thúc, không thể bật lại");
        }

        d.setTrangThai(trangThai);
        return dotRepo.save(d).getTrangThai();
    }

    @Transactional
    public Long capNhat(Long id, DotGiamGiaRequest r) {
        DotGiamGia d = dotRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy đợt giảm giá"));

        validate(r, id);

        Set<Long> newIds = Set.copyOf(r.getDanhSachSanPhamChiTietId());
        List<SanPhamChiTiet> spcts = spctRepo.findAllById(newIds);
        if (spcts.size() != newIds.size()) {
            throw bad("Có sản phẩm không tồn tại");
        }

        d.setMaDotGiamGia(r.getMaDotGiamGia().trim());
        d.setTenDotGiamGia(r.getTenDotGiamGia().trim());
        d.setPhanTramGiam(r.getPhanTramGiam());
        d.setNgayBatDau(r.getNgayBatDau());
        d.setNgayKetThuc(r.getNgayKetThuc());
        d.setMoTa(r.getMoTa());
        if (r.getTrangThai() != null) d.setTrangThai(r.getTrangThai());
        dotRepo.save(d);

        // Đồng bộ danh sách sản phẩm: xoá cái bị bỏ, thêm cái mới, bật lại cái đã có
        List<DotGiamGiaChiTiet> hienTai = chiTietRepo.findByDotId(id);
        Set<Long> idsHienTai = hienTai.stream()
                .map(ct -> ct.getSanPhamChiTiet().getId())
                .collect(Collectors.toSet());

        List<DotGiamGiaChiTiet> canXoa = hienTai.stream()
                .filter(ct -> !newIds.contains(ct.getSanPhamChiTiet().getId()))
                .toList();
        chiTietRepo.deleteAll(canXoa);

        hienTai.stream()
                .filter(ct -> newIds.contains(ct.getSanPhamChiTiet().getId()))
                .forEach(ct -> ct.setTrangThai(true));

        List<DotGiamGiaChiTiet> canThem = spcts.stream()
                .filter(s -> !idsHienTai.contains(s.getId()))
                .map(s -> {
                    DotGiamGiaChiTiet ct = new DotGiamGiaChiTiet();
                    ct.setDotGiamGia(d);
                    ct.setSanPhamChiTiet(s);
                    ct.setTrangThai(true);
                    return ct;
                }).toList();
        chiTietRepo.saveAll(canThem);

        return d.getId();
    }
}