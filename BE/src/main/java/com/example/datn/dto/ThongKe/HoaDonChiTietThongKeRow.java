package com.example.datn.dto.ThongKe;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HoaDonChiTietThongKeRow(
    Long id,
    String maHoaDon,
    LocalDateTime ngayTao,
    String tenKhachHang,
    String soDienThoaiKhachHang,
    String tenNhanVien,
    String loaiDon,
    String tenHinhThuc,
    String maHinhThuc,
    String trangThai,
    BigDecimal tienSauGiamGia,
    boolean daXoa
) {}
