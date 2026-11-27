package com.example.datn.dto.PhieuGiamGia;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PhieuSuDungCuaToiDTO(
    Long idPhieuGiamGia,
    String ma,
    String ten,
    Integer gioiHanMoiTaiKhoan,
    Long soLanDaDung,
    String loaiGiamGia,
    BigDecimal giaTriGiam,
    BigDecimal giaTriDonHangToiThieu,
    LocalDateTime ngayKetThuc
) {}
