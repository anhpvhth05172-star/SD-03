package com.example.datn.dto.ThongKe;

public record TongQuanDTO(
    long doanhThu,
    long tienMat,
    long tienChuyenKhoan,
    long soDon,
    long aov,
    long sanPhamBan,
    long khachMoi,
    long soDonOnline,
    long soDonTaiQuay
) {
    public TongQuanDTO(long doanhThu, long tienMat, long tienChuyenKhoan, long soDon, long aov, long sanPhamBan, long khachMoi) {
        this(doanhThu, tienMat, tienChuyenKhoan, soDon, aov, sanPhamBan, khachMoi, 0, 0);
    }
}
