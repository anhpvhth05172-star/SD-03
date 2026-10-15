package com.example.datn.dto.Auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String refreshToken;
    private Long idKhachHang;
    private Long idNhanVien;
    private String loaiTaiKhoan;
    private String tenTaiKhoan;
    private String email;
    private String tenKhachHang;
    private String vaiTro;

    public AuthResponse(
        String token,
        String refreshToken,
        Long idKhachHang,
        String tenTaiKhoan,
        String email,
        String tenKhachHang,
        String vaiTro
    ) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.idKhachHang = idKhachHang;
        this.loaiTaiKhoan = "KHACH_HANG";
        this.tenTaiKhoan = tenTaiKhoan;
        this.email = email;
        this.tenKhachHang = tenKhachHang;
        this.vaiTro = vaiTro;
    }
}
