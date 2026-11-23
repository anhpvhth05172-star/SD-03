package com.example.datn.dto.LichLamViec;

import java.time.LocalTime;

public record CaLamViecLichDto(
    Long id,
    String ma,
    String ten,
    LocalTime batDau,
    LocalTime ketThuc,
    boolean dangBat
) {}
