package com.example.datn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PhieuGiamGiaDTO(
    Long id,
    String ma,
    String ten,
    String loaiGiamGia,
    BigDecimal giaTriGiam,
    BigDecimal giamToiDa,
    BigDecimal hoaDonToiThieu,
    LocalDateTime ngayBatDau,
    LocalDateTime ngayKetThuc,
    Integer soLuong,
    Integer soLuongDaSuDung,
    Integer gioiHanMoiTaiKhoan,
    Boolean trangThai,
    DotGiamGiaDTO dotGiamGia
) {

    public record DotGiamGiaDTO(Long id, String ma, String ten) {}
}
