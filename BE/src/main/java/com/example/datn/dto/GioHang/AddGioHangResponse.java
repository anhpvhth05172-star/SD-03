package com.example.datn.dto.GioHang;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
@Getter
@Builder
public class AddGioHangResponse {

    private Integer idGioHang;
    private Integer idGioHangChiTiet;
    private Long idSanPhamChiTiet;
    private String tenSanPham;
    private String mauSac;
    private String kichCo;
    private Integer soLuong;
    private Integer donGia;
    private BigDecimal thanhTien;
    private Integer tongSoLuongGioHang;
    private String message;
}
