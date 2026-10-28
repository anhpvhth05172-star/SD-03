package com.example.datn.dto.ThongKe;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1 dong chi tiet hoa don kem thong tin san pham + ngay tao hoa don
 * (dung de tinh top san pham theo ky).
 */
public record SanPhamThongKeRow(
    Long idHoaDon,
    String trangThai,
    boolean daXoa,
    LocalDateTime ngayTao,
    String maSanPham,
    String tenSanPham,
    String tenThuongHieu,
    BigDecimal thanhTien,
    Integer soLuong
) {}
