package com.example.datn.dto.GioHang;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ThemGioHangRequest {

    private Long idKhachHang;

    @NotNull(message = "Vui lòng chọn biến thể sản phẩm (màu, kích cỡ)")
    private Long idSanPhamChiTiet;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    @Max(value = 100, message = "Số lượng mỗi lần thêm tối đa là 100")
    private Integer soLuong;
}
