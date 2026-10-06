package com.example.datn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KhachHangDTO(
    Long id,
    String ma,
    String tenKhachHang,
    String tenTaiKhoan,
    String email,
    String soDienThoai,
    LocalDateTime ngayTao,
    Boolean trangThai,
    Long soDon,
    BigDecimal tongChiTieu
) {}
