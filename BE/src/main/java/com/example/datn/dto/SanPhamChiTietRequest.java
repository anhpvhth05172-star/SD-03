package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamChiTietRequest {
    private Long idSanPham;
    private Long idMauSac;
    private Long idKichCo;
    private Long idThanGiay;
    private Long idDeGiay;
    private String maChiTietSanPham;
    private Integer soLuong;
    private BigDecimal giaBan;
    private Boolean trangThai = true;
}
