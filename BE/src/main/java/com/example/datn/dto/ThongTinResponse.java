package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThongTinResponse {

    private Long id;
    private String tenTaiKhoan;
    private String tenKhachHang;
    private String email;
    private String soDienThoai;
    private String vaiTro;
}
