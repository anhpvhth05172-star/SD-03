package com.example.datn.dto.Auth;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapNhatThongTinKhachRequest {
    private String tenKhachHang;
    private String soDienThoai;
    private LocalDate ngaySinh;
    private String gioiTinh;
}
