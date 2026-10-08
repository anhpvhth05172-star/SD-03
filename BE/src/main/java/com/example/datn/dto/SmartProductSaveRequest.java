package com.example.datn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmartProductSaveRequest {

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

    private Boolean confirmMerge = false;

    @Valid
    private List<BienTheItem> bienThes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BienTheItem {
        @NotNull(message = "Màu sắc không được để trống")
        private Long idMauSac;

        @NotNull(message = "Kích cỡ không được để trống")
        private Long idKichCo;

        private Integer soLuong;
        private BigDecimal giaBan;
        private String hinhAnh;
        private Boolean trangThai = true;
    }
}
