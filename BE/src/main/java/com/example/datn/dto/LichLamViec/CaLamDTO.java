package com.example.datn.dto.LichLamViec;

import java.time.LocalTime;

public record CaLamDTO(
    Long id,
    String ma,
    String ten,
    LocalTime batDau,
    LocalTime ketThuc,
    Boolean dangBat
) {}
