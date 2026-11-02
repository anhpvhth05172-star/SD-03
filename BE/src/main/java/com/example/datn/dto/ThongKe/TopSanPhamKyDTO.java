package com.example.datn.dto.ThongKe;

import java.util.List;

/**
 * Top san pham ban chay theo cac ky so sanh.
 * banTheoKy[i] = so luong ban duoc trong ky thu i (dong ky voi SoSanhDTO.ky).
 */
public record TopSanPhamKyDTO(
    String maSanPham,
    String tenSanPham,
    String tenThuongHieu,
    long tongBan,
    long tongDoanhThu,
    List<Long> banTheoKy
) {}
