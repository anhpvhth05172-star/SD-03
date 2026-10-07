package com.example.datn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SanPhamDTO(
    Long id,
    String maSanPham,
    String tenSanPham,
    String doiTuong,
    String moTa,
    String tenLoaiGiay,
    String tenThuongHieu,
    String tenChatLieu,
    String tenXuatXu,
    String tenKieuDang,
    int soLuongBienThe,
    long tongTonKho,
    BigDecimal giaNhoNhat,
    Boolean trangThai,
    LocalDateTime ngayTao
) {}
