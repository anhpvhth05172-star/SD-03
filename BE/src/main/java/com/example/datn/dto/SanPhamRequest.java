package com.example.datn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự")
    private String tenSanPham;

    @NotNull(message = "Xuất xứ không được để trống")
    private Long idXuatXu;

    @NotNull(message = "Thương hiệu không được để trống")
    private Long idThuongHieu;

    @NotNull(message = "Chất liệu không được để trống")
    private Long idChatLieu;

    @NotNull(message = "Kiểu dáng không được để trống")
    private Long idKieuDang;

    @NotNull(message = "Loại giày không được để trống")
    private Long idLoaiGiay;

    private String doiTuong;
    private String tinhNang;
    private String moTa;
    private Boolean trangThai = true;
}
