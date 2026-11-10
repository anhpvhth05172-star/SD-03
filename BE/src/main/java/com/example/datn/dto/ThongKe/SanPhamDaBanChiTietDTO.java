package com.example.datn.dto.ThongKe;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SanPhamDaBanChiTietDTO(
    Long idHoaDon,
    String maHoaDon,
    LocalDateTime ngayTao,
    String maSanPham,
    String tenSanPham,
    String bienThe,
    String tenThuongHieu,
    int soLuong,
    BigDecimal donGia,
    BigDecimal thanhTien
) {}
