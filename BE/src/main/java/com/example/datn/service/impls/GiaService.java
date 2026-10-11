package com.example.datn.service.impls;

import com.example.datn.dto.DotGiamGia.GiaKhuyenMai;
import com.example.datn.entity.DotGiamGiaChiTiet;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.DotGiamGiaChiTietRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GiaService {

    private static final BigDecimal TRAM = BigDecimal.valueOf(100);

    private final DotGiamGiaChiTietRepository chiTietRepo;

    public static BigDecimal giaSauGiam(BigDecimal giaBan, BigDecimal phanTram) {
        if (phanTram == null) return giaBan;
        return giaBan.multiply(TRAM.subtract(phanTram))
                .divide(TRAM, 0, RoundingMode.HALF_UP);
    }

    // 1 truy vấn cho cả danh sách, tránh N+1
    @Transactional(readOnly = true)
    public Map<Long, GiaKhuyenMai> tinhGia(Collection<SanPhamChiTiet> spcts) {
        if (spcts.isEmpty()) return Map.of();

        List<Long> ids = spcts.stream().map(SanPhamChiTiet::getId).toList();
        Map<Long, BigDecimal> phanTramTheoId = new HashMap<>();
        for (DotGiamGiaChiTiet ct : chiTietRepo.findActiveByVariantIds(ids, LocalDateTime.now())) {
            // Nếu dữ liệu cũ có nhiều đợt cùng lúc thì lấy mức giảm cao nhất
            phanTramTheoId.merge(ct.getSanPhamChiTiet().getId(),
                    ct.getDotGiamGia().getPhanTramGiam(), BigDecimal::max);
        }

        Map<Long, GiaKhuyenMai> result = new HashMap<>();
        for (SanPhamChiTiet s : spcts) {
            BigDecimal pt = phanTramTheoId.get(s.getId());
            result.put(s.getId(), new GiaKhuyenMai(s.getGiaBan(), pt, giaSauGiam(s.getGiaBan(), pt)));
        }
        return result;
    }
}
