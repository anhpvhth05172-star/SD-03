package com.example.datn.dto;

import java.util.List;

public record SanPhamRequest(
    String maSanPham,
    String tenSanPham,
    String doiTuong,
    String tinhNang,
    String moTa,
    Long idLoaiGiay,
    Long idThuongHieu,
    Long idChatLieu,
    Long idXuatXu,
    Long idKieuDang,
    Boolean trangThai,
    List<SanPhamChiTietRequest> bienThe
) {}
