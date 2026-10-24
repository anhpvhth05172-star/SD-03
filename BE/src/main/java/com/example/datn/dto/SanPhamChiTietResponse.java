package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPhamChiTietResponse {
    private Long id;
    private Long idSanPham;
    private String maSp;
    private String tenSp;
    private String maCtsp;
    private Long idMauSac;
    private String color;
    private String colorHex;
    private Long idKichCo;
    private String size;
    private Integer stock;
    private BigDecimal price;       // giá gốc (giaBan)
    private BigDecimal giaSauGiam;  // giá thực hiển thị (đã áp dụng đợt giảm giá nếu có)
    private BigDecimal phanTramGiam; // % giảm từ đợt giảm giá (null = không có đợt)
    private String discount;
    private Boolean trangThai;
    private String trangThaiTonKho;
    private String img;
    private LocalDateTime ngayTao;
}
