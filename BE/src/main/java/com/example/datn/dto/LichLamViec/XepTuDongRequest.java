package com.example.datn.dto.LichLamViec;

import java.time.LocalDate;
import java.util.List;

public record XepTuDongRequest(
    LocalDate ngay,
    LocalDate tuNgay,
    LocalDate denNgay,
    List<Long> idCaLams,
    List<String> maNhanViens,
    Integer soNhanVienMoiCa,
    Integer maxCaMoiNgay,
    Boolean ghiDe
) {
    public XepTuDongRequest(LocalDate ngay) {
        this(ngay, null, null, null, null, null, null, null);
    }
}
