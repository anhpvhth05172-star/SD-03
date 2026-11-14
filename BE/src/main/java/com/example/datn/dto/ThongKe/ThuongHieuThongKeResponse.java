package com.example.datn.dto.ThongKe;

import java.util.List;

public record ThuongHieuThongKeResponse(
    long tongDoanhThu,
    long tongSoLuong,
    List<ThuongHieuItemDTO> danhSach
) {
    public record ThuongHieuItemDTO(
        String ten,
        long doanhThu,
        long soLuong,
        double tyLe,
        String mau
    ) {}
}
