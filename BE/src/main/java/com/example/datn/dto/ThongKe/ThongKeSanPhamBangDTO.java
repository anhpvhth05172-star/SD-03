package com.example.datn.dto.ThongKe;

public record ThongKeSanPhamBangDTO(
    Long idSanPham,
    String maSanPham,
    String tenSanPham,
    String hinhAnh,
    String bienThe,
    String tenThuongHieu,
    long daBan,
    long soLuongTra,
    long doanhThu,
    long tonKho
) {}
