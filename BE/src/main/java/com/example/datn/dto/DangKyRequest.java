package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DangKyRequest {

    private String tenTaiKhoan;
    private String email;
    private String soDienThoai;
    private String matKhau;
    private String xacNhanMatKhau;
}
