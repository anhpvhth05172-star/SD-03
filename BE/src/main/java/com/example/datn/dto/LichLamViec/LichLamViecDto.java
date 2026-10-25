package com.example.datn.dto.LichLamViec;

import java.time.LocalDate;

public record LichLamViecDto(
    Long id,
    String maNhanVien,
    String tenNhanVien,
    Long idCa,
    LocalDate ngay
) {}
