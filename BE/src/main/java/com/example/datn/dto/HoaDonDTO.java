package com.example.datn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
public class HoaDonDTO {

    private Long id;
    private String maHoaDon;
    private String loaiDon;
    private BigDecimal phiVanChuyen;
    private BigDecimal tongTien;
    private BigDecimal tienSauGiamGia;
    private String tenKhachHang;
    private String soDienThoaiKhachHang;
    private String diaChiNhanHang;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private String nguoiCapNhat;
    private String trangThai;
    private String ghiChu;

    private Long idKhachHang;
    private Long idNhanVien;
    private String tenNhanVien;
    private Long idPhuongThucThanhToan;
    private String tenPhuongThucThanhToan;
    private Long idPhieuGiamGia;
    private String tenPhieuGiamGia;

    private List<ChiTietHoaDonDTO> chiTiet;
}
