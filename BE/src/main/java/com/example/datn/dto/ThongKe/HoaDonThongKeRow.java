package com.example.datn.dto.ThongKe;

import java.math.BigDecimal;

public record HoaDonThongKeRow(
    Long id,
    String trangThai,
    BigDecimal tienSauGiamGia,
    boolean daXoa,
    String maHinhThuc,
    String loaiDon
) {
    public HoaDonThongKeRow(Long id, String trangThai, BigDecimal tienSauGiamGia, boolean daXoa, String maHinhThuc) {
        this(id, trangThai, tienSauGiamGia, daXoa, maHinhThuc, null);
    }
}
