package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DonHangDatThanhCongEvent {

    private final String email;
    private final String tenKhachHang;
    private final String maHoaDon;
    private final String soTien;
    private final String phuongThucThanhToan;
    private final String diaChiNhanHang;
}
