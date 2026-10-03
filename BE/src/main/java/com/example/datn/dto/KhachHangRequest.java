package com.example.datn.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KhachHangRequest {

    private String maKhachHang;
    private String tenTaiKhoan;
    private String matKhau;
    private String tenKhachHang;
    private String email;
    private String soDienThoai;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private Boolean trangThai;
    private String tinhThanhPho;
    private String phuong;
    private String diaChiCuThe;
    private String nguoiCapNhat;
}