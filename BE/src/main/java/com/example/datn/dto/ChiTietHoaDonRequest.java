package com.example.datn.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietHoaDonRequest {

    private Long idSanPhamChiTiet;
    private Integer soLuong;
    private BigDecimal donGia;
    private String ghiChu;
    private Boolean trangThai;
}
