package com.example.datn.dto;

public record DiaChiKhachHangDTO(
    Long id,
    Long idKhachHang,
    String maDiaChi,
    String tenChiChi,
    String thanhPho,
    String phuong,
    String diaChiCuThe,
    Boolean macDinh,
    Boolean trangThai
) {}