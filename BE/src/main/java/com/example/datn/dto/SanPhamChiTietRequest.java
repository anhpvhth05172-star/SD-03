package com.example.datn.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SanPhamChiTietRequest {

    private Long idSanPham;

    @NotNull(message = "Vui lòng chọn màu sắc cho biến thể")
    private Long idMauSac;

    @NotNull(message = "Vui lòng chọn kích cỡ cho biến thể")
    private Long idKichCo;

    private Long idThanGiay;
    private Long idDeGiay;

    @Size(max = 80, message = "Mã chi tiết sản phẩm tối đa 80 ký tự")
    private String maChiTietSanPham;

    @NotNull(message = "Số lượng tồn không được để trống")
    @Min(value = 0, message = "Số lượng tồn phải lớn hơn hoặc bằng 0")
    @Max(value = 100000, message = "Số lượng tồn tối đa không vượt quá 100,000 sản phẩm")
    private Integer soLuong;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "1000.0", message = "Giá bán phải từ 1,000 VNĐ trở lên")
    @DecimalMax(value = "1000000000.0", message = "Giá bán tối đa không vượt quá 1,000,000,000 VNĐ")
    private BigDecimal giaBan;

    @DecimalMin(value = "0.0", message = "Trọng lượng phải lớn hơn hoặc bằng 0")
    @DecimalMax(value = "10000.0", message = "Trọng lượng tối đa 10,000 gram")
    private BigDecimal trongLuong;

    @DecimalMin(value = "0.0", message = "Chiều cao đế phải lớn hơn hoặc bằng 0")
    @DecimalMax(value = "50.0", message = "Chiều cao đế tối đa 50 cm")
    private BigDecimal chieuCaoDe;

    @DecimalMin(value = "0.0", message = "Chiều cao gót phải lớn hơn hoặc bằng 0")
    @DecimalMax(value = "50.0", message = "Chiều cao gót tối đa 50 cm")
    private BigDecimal chieuCaoGot;

    @DecimalMin(value = "0.0", message = "Độ chênh gót mũi phải lớn hơn hoặc bằng 0")
    @DecimalMax(value = "50.0", message = "Độ chênh gót mũi tối đa 50 cm")
    private BigDecimal doChenhGotMui;

    @Size(max = 100, message = "Form giày tối đa 100 ký tự")
    private String formGiay;

    private String hinhAnh;

    private Boolean trangThai = true;
}

