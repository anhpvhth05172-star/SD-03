package com.example.datn.dto.LichLamViec;

import java.time.LocalDate;

public record LichLamViecRequest(
    String maNhanVien,
    Long idCa,
    LocalDate ngay
) {}
