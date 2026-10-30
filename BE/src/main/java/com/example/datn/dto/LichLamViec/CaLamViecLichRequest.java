package com.example.datn.dto.LichLamViec;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CaLamViecLichRequest(
    @NotBlank String ma,
    @NotBlank String ten,
    @NotNull LocalTime batDau,
    @NotNull LocalTime ketThuc
) {}
