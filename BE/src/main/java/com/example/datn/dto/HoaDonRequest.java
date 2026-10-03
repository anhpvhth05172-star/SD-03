package com.example.datn.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoaDonRequest {

    private String maHoaDon;
    private String loaiDon;
    private BigDecimal phiVanChuyen;
    private String tenKhachHang;
    private String soDienThoaiKhachHang;
    private String diaChiNhanHang;
    private String trangThai;
    private String ghiChu;
    private String nguoiCapNhat;

    private Long idKhachHang;
    private Long idNhanVien;
    private Long idPhuongThucThanhToan;
    private Long idPhieuGiamGia;

    private List<ChiTietHoaDonRequest> chiTiet;
}
