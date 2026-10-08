package com.example.datn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SanPhamChiTietDTO(
    Long id,
    Long idSanPham,
    String maChiTietSanPham,
    String tenMau,
    String tenKichCo,
    String tenThanGiay,
    String tenDeGiay,
    Integer soLuong,
    BigDecimal giaBan,
    Boolean trangThai,
    BigDecimal trongLuong,
    BigDecimal chieuCaoDe,
    BigDecimal chieuCaoGot,
    BigDecimal doChenhGotMui,
    String formGiay,
    LocalDateTime ngayTao
) {}
