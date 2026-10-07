package com.example.datn.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPhamResponse {

    private Long id;
    private String maSanPham;
    private String tenSanPham;

    private Long idXuatXu;
    private String tenXuatXu;

    private Long idThuongHieu;
    private String tenThuongHieu;

    private Long idChatLieu;
    private String tenChatLieu;

    private Long idKieuDang;
    private String tenKieuDang;

    private Long idLoaiGiay;
    private String tenLoaiGiay;

    private String doiTuong;
    private String tinhNang;
    private String moTa;
    private Boolean trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
}
