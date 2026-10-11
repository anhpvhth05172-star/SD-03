package com.example.datn.dto.DotGiamGia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DotGiamGiaDetailResponse {
    private Long id;
    private String maDotGiamGia;
    private String tenDotGiamGia;
    private BigDecimal phanTramGiam;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Boolean trangThai;
    private String moTa;
    private LocalDateTime ngayTao;
    private List<SanPhamItem> sanPhams;

    @Getter
    @Builder
    public static class SanPhamItem {
        private Long id;              // id san_pham_chi_tiet
        private Long sanPhamId;
        private String maSanPham;
        private String tenSanPham;
        private String maChiTiet;
        private String kichCo;
        private String mauSac;
        private BigDecimal giaBan;
        private BigDecimal giaSauGiam;
        private Integer soLuong;
        private Boolean trangThai;    // trạng thái dòng gắn vào đợt
    }
}
