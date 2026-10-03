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
public class NhanVienRequest {

    private String maNhanVien;
    private String tenTaiKhoan;
    private String matKhau;
    private String email;
    private String soDienThoai;
    private String anhNhanVien;
    private String gioiTinh;
    private LocalDate ngaySinh;
    private String queQuan;
    private String phuong;
    private String diaChiCuThe;
    private Boolean trangThai;
    private Long idVaiTro;
    private String nguoiCapNhat;
}