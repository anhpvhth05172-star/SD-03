package com.example.datn.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietHoaDonDTO {

    private Long id;
    private Long idSanPhamChiTiet;
    private String maSanPhamChiTiet;
    private String tenSanPham;
    private String tenKichCo;
    private String tenMau;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private String ghiChu;
    private Boolean trangThai;
}
