package com.example.datn.dto.ThongKe;

import java.util.List;

/** Ket qua so sanh doanh thu: cac ky doi chieu + top san pham theo ky. */
public record SoSanhDTO(
    List<KySoSanhDTO> ky,
    List<TopSanPhamKyDTO> topSanPham
) {}
