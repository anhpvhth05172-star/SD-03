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

    private static final int SO_SAN_PHAM = 30;
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
        return ds.stream()
            .filter(sp -> sp.getTrangThai() == null || Boolean.TRUE.equals(sp.getTrangThai()))
            .map(sp -> {
                StringBuilder sb = new StringBuilder();
                sb.append("- [ID: ").append(sp.getId()).append("] ").append(sp.getTenSanPham());
                sb.append(" | Link: /cua-hang/").append(sp.getId());
                if (sp.getMaSanPham() != null) {
                    sb.append(" | Mã: ").append(sp.getMaSanPham());
                }
                if (sp.getLoaiGiay() != null) {
                    sb.append(" | Loại: ").append(sp.getLoaiGiay().getTenLoaiGiay());
                }
                if (sp.getThuongHieu() != null) {
                    sb.append(" | Thương hiệu: ").append(sp.getThuongHieu().getTenThuongHieu());
                }
                if (sp.getDoiTuong() != null) {
                    sb.append(" | Đối tượng: ").append(sp.getDoiTuong());
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

    @Transactional(readOnly = true)
    public String timGoiYFallback(String cauHoi) {
        if (cauHoi == null || cauHoi.isBlank()) {
            return null;
        }
        String keyword = cauHoi.trim().toLowerCase();
        List<SanPham> ds = sanPhamRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
            .stream()
            .filter(sp -> sp.getTrangThai() == null || Boolean.TRUE.equals(sp.getTrangThai()))
            .filter(sp -> {
                String ten = sp.getTenSanPham() != null ? sp.getTenSanPham().toLowerCase() : "";
                String th = sp.getThuongHieu() != null && sp.getThuongHieu().getTenThuongHieu() != null
                    ? sp.getThuongHieu().getTenThuongHieu().toLowerCase() : "";
                String lg = sp.getLoaiGiay() != null && sp.getLoaiGiay().getTenLoaiGiay() != null
                    ? sp.getLoaiGiay().getTenLoaiGiay().toLowerCase() : "";
                for (String word : keyword.split("\\s+")) {
                    if (word.length() >= 2 && (ten.contains(word) || th.contains(word) || lg.contains(word))) {
                        return true;
                    }
                }
                return false;
            })
            .limit(3)
            .toList();

        if (ds.isEmpty()) {
            ds = sanPhamRepository.findAll(PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id"))).getContent();
        }

        if (ds.isEmpty()) {
            return null;
        }

        String links = ds.stream()
            .map(sp -> "[" + sp.getTenSanPham() + "](/cua-hang/" + sp.getId() + ")")
            .collect(Collectors.joining(", "));

        return "PolyShoes gợi ý cho bạn một số mẫu giày phù hợp: " + links + ". Bạn có thể bấm vào sản phẩm để xem chi tiết nhé!";
    }
}
