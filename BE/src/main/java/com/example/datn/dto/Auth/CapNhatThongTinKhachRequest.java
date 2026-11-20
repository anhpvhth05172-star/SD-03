package com.example.datn.dto.Auth;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapNhatThongTinKhachRequest {
    private String tenKhachHang;
    private String email;
    private String soDienThoai;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private Long idDiaChi;
    private String tenDiaChi;
    private String tinhThanhPho;
    private String phuong;
    private String diaChiCuThe;
    private Boolean macDinh;
}
