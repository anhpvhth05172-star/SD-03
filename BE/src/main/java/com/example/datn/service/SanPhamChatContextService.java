package com.example.datn.service;

import com.example.datn.entity.SanPham;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.SanPhamChiTietRepository;
import com.example.datn.repository.SanPhamRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghép dữ liệu sản phẩm thật làm ngữ cảnh cho chat AI (Gemini).
 */
@Service
@RequiredArgsConstructor
public class SanPhamChatContextService {

    private static final int SO_SAN_PHAM = 15;
    private static final int SO_CHI_TIET_MOT_SP = 5;

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;

    @Transactional(readOnly = true)
    public String taoBoSung() {
        List<SanPham> ds = sanPhamRepository.findAll(
            PageRequest.of(0, SO_SAN_PHAM, Sort.by(Sort.Direction.DESC, "id"))
        ).getContent();
        if (ds.isEmpty()) {
            return "(chưa có sản phẩm)";
        }
        return ds.stream().map(sp -> {
            StringBuilder sb = new StringBuilder();
            sb.append("- ").append(sp.getTenSanPham());
            if (sp.getLoaiGiay() != null) {
                sb.append(" | Loại: ").append(sp.getLoaiGiay().getTenLoaiGiay());
            }
            if (sp.getThuongHieu() != null) {
                sb.append(" | Thương hiệu: ").append(sp.getThuongHieu().getTenThuongHieu());
            }
            if (sp.getMoTa() != null && !sp.getMoTa().isBlank()) {
                String moTa = sp.getMoTa().length() > 150
                    ? sp.getMoTa().substring(0, 150) + "..."
                    : sp.getMoTa();
                sb.append(" | Mô tả: ").append(moTa);
            }
            List<SanPhamChiTiet> chiTiets = sanPhamChiTietRepository.findBySanPhamId(sp.getId())
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getTrangThai()))
                .limit(SO_CHI_TIET_MOT_SP)
                .collect(Collectors.toList());
            if (!chiTiets.isEmpty()) {
                sb.append(" | Giá/tùy chọn: ");
                sb.append(
                    chiTiets.stream()
                        .map(c -> {
                            String kichCo = c.getKichCo() != null ? c.getKichCo().getTenKichCo() : "?";
                            String mauSac = c.getMauSac() != null ? c.getMauSac().getTenMau() : "?";
                            return mauSac + "/" + kichCo + " = " + c.getGiaBan() + "đ (SL " + c.getSoLuong() + ")";
                        })
                        .collect(Collectors.joining("; "))
                );
            }
            return sb.toString();
        }).collect(Collectors.joining("\n"));
    }
}
