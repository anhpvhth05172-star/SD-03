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
    private BigDecimal price;
    private String discount;
    private Boolean trangThai;
    private String img;
    private LocalDateTime ngayTao;
}
