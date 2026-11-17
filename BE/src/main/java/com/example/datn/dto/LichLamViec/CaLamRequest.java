package com.example.datn.dto.LichLamViec;

import java.time.LocalTime;

public record CaLamRequest(
    String ten,
    LocalTime batDau,
    LocalTime ketThuc
) {}
