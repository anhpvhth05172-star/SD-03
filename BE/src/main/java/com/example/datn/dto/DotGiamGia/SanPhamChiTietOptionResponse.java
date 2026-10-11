package com.example.datn.dto.DotGiamGia;

import com.example.datn.entity.SanPhamChiTiet;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SanPhamChiTietOptionResponse {
    private Long id;
    private Long sanPhamId;
    private String maSanPham;
    private String tenSanPham;
    private String maChiTiet;
    private String kichCo;
    private String mauSac;
    private BigDecimal giaBan;
    private Integer soLuong;

    public static SanPhamChiTietOptionResponse from(SanPhamChiTiet s) {
        return new SanPhamChiTietOptionResponse(
                s.getId(),
                s.getSanPham().getId(),
                s.getSanPham().getMaSanPham(),
                s.getSanPham().getTenSanPham(),
                s.getMaChiTietSanPham(),
                s.getKichCo().getTenKichCo(),   // ⚠️ chỉnh theo entity KichCo
                s.getMauSac().getTenMau(),   // ⚠️ chỉnh theo entity MauSac
                s.getGiaBan(),
                s.getSoLuong());
    }
}
