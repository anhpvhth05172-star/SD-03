package com.example.datn.dto.SanPham;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SanPhamChiTietDTO(
    Long id,
    Long idSanPham,
    String maChiTietSanPham,
    String tenMau,
    String tenKichCo,
    Integer soLuong,
    BigDecimal giaBan,
    Boolean trangThai,
    BigDecimal trongLuong,
    LocalDateTime ngayTao
) {}
