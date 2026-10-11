package com.example.datn.dto.DotGiamGia;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DotGiamGiaDTO(
    Long id,
    String ma,
    String ten,
    BigDecimal phanTramGiam,
    LocalDateTime ngayBatDau,
    LocalDateTime ngayKetThuc,
    Boolean trangThai,
    Long soPhieu
) {}
