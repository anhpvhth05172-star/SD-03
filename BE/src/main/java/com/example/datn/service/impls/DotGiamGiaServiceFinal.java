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
import java.util.List;
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

    @Transactional(readOnly = true)
    public List<SanPhamChiTietOptionResponse> layDanhSachSanPham() {
        return spctRepo.findAllActiveForDiscount().stream()
                .map(SanPhamChiTietOptionResponse::from)
                .toList();
    }

    @Transactional
    public Long them(DotGiamGiaRequest r) {
        validate(r);

        Set<Long> ids = Set.copyOf(r.getDanhSachSanPhamChiTietId());
        List<SanPhamChiTiet> spcts = spctRepo.findAllById(ids);
        if (spcts.size() != ids.size()) {
            throw bad("Có sản phẩm không tồn tại");
        }

        // Một biến thể không được thuộc 2 đợt chồng thời gian
        List<DotGiamGiaChiTiet> conflicts =
                chiTietRepo.findOverlapping(ids, r.getNgayBatDau(), r.getNgayKetThuc());
        if (!conflicts.isEmpty()) {
            String detail = conflicts.stream().limit(5)
                    .map(c -> c.getSanPhamChiTiet().getMaChiTietSanPham()
                            + " (đợt " + c.getDotGiamGia().getMaDotGiamGia() + ")")
                    .collect(Collectors.joining(", "));
            throw bad("Sản phẩm đã thuộc đợt giảm giá khác trùng thời gian: " + detail
                    + (conflicts.size() > 5 ? "..." : ""));
        }

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

    private void validate(DotGiamGiaRequest r) {
        if (r.getMaDotGiamGia() == null || r.getMaDotGiamGia().isBlank())
            throw bad("Vui lòng nhập mã đợt");
        if (r.getTenDotGiamGia() == null || r.getTenDotGiamGia().isBlank())
            throw bad("Vui lòng nhập tên đợt");
        if (dotRepo.existsByMaDotGiamGia(r.getMaDotGiamGia().trim()))
            throw bad("Mã đợt đã tồn tại");

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
                            .mauSac(s.getMauSac().getTenMau())   // ⚠️ chỉnh theo entity MauSac
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
}
