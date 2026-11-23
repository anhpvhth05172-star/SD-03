package com.example.datn.dto.ThongKe;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SanPhamDaBanChiTietRow(
    Long idHoaDon,
    String maHoaDon,
    LocalDateTime ngayTao,
    String maSanPham,
    String tenSanPham,
    String tenKichCo,
    String tenMau,
    String tenThuongHieu,
    int soLuong,
    BigDecimal donGia,
    BigDecimal thanhTien,
    String trangThai,
    boolean daXoa
) {
    public String bienThe() {
        String kc = tenKichCo != null ? tenKichCo.trim() : "";
        String ms = tenMau != null ? tenMau.trim() : "";
        if (!kc.isEmpty() && !ms.isEmpty()) return "Size: " + kc + ", Màu: " + ms;
        if (!kc.isEmpty()) return "Size: " + kc;
        if (!ms.isEmpty()) return "Màu: " + ms;
        return "Tiêu chuẩn";
    }
}
