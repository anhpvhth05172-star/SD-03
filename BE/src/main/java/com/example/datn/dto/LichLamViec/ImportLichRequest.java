package com.example.datn.dto.LichLamViec;

import java.time.LocalDate;
import java.util.List;

public record ImportLichRequest(List<ImportLichRow> rows) {}
