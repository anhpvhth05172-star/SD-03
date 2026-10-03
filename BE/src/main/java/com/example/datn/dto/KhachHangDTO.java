package com.example.datn.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class KhachHangDTO {

    private Long id;
    private String maKhachHang;
    private String tenTaiKhoan;
    private String tenKhachHang;
    private String email;
    private String soDienThoai;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private Boolean trangThai;
    private String trangThaiLabel;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private String nguoiCapNhat;

    private String maHangThanhVien;
    private String hangThanhVien;
    private Long soDon;
    private BigDecimal tongChiTieu;

    private DiaChiKhachHangDTO diaChiMacDinh;
    private List<DiaChiKhachHangDTO> danhSachDiaChi;
}