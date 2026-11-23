package com.example.datn.dto.ThongKe;

import java.time.LocalDate;

public record SanPhamTheoNgayDTO(
    LocalDate ngay,
    String nhanNgay,
    long soLuongBan,
    long doanhThu,
    long soDon
) {}
