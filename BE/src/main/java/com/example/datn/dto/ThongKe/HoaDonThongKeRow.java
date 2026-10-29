package com.example.datn.dto.ThongKe;

import java.math.BigDecimal;

public record HoaDonThongKeRow(
    Long id,
    String trangThai,
    BigDecimal tienSauGiamGia,
    boolean daXoa,
    String maHinhThuc
) {}
