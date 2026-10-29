package com.example.datn.dto.LichLamViec;

import java.time.LocalDate;
import java.time.LocalTime;

public record LichLamViecDTO(
    Long id,
    String maNhanVien,
    String tenNhanVien,
    Long idCa,
    String tenCa,
    LocalTime batDau,
    LocalTime ketThuc,
    LocalDate ngay
) {}
