package com.example.datn.dto.ThongKe;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DonHangThongKeDTO(
    Long id,
    String maHoaDon,
    LocalDateTime ngayTao,
    String tenKhachHang,
    String soDienThoaiKhachHang,
    String tenNhanVien,
    String loaiDon,
    String hinhThucThanhToan,
    String trangThai,
    String trangThaiThanhToan,
    BigDecimal thanhTien
) {}
