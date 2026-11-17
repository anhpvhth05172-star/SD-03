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
    long soDonTaiQuay,
    long doanhThuDuKien
) {
    public TongQuanDTO(long doanhThu, long tienMat, long tienChuyenKhoan, long soDon, long aov, long sanPhamBan, long khachMoi, long soDonOnline, long soDonTaiQuay) {
        this(doanhThu, tienMat, tienChuyenKhoan, soDon, aov, sanPhamBan, khachMoi, soDonOnline, soDonTaiQuay, 0);
    }

    public TongQuanDTO(long doanhThu, long tienMat, long tienChuyenKhoan, long soDon, long aov, long sanPhamBan, long khachMoi) {
        this(doanhThu, tienMat, tienChuyenKhoan, soDon, aov, sanPhamBan, khachMoi, 0, 0, 0);
    }
}
