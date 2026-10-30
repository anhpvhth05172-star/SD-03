package com.example.datn.dto.LichLamViec;

import java.util.Map;

public record XepTuDongResponse(
    String lyDo,
    int themMoi,
    int soNgay,
    Map<String, Integer> phanBoNhanVien,
    String thongBao
) {
    public XepTuDongResponse(String lyDo, int themMoi) {
        this(lyDo, themMoi, lyDo == null && themMoi > 0 ? 1 : 0, Map.of(), null);
    }
}
