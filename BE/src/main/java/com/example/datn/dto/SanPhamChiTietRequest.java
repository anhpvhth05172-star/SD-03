package com.example.datn.dto;

import java.math.BigDecimal;

public record SanPhamChiTietRequest(
    String maChiTietSanPham,
    Long idMauSac,
    Long idKichCo,
    Long idThanGiay,
    Long idDeGiay,
    Integer soLuong,
    BigDecimal giaBan,
    Boolean trangThai,
    BigDecimal trongLuong,
    BigDecimal chieuCaoDe,
    BigDecimal chieuCaoGot,
    BigDecimal doChenhGotMui,
    String formGiay
) {}
