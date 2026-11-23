package com.example.datn.dto.ThongKe;

import java.math.BigDecimal;

public record ChiTietThongKeRow(
    Long idHoaDon,
    String trangThai,
    boolean daXoa,
    String tenThuongHieu,
    BigDecimal thanhTien,
    Integer soLuong
) {}
