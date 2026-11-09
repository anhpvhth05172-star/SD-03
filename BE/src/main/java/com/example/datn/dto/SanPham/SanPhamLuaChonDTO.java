package com.example.datn.dto.SanPham;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanPhamLuaChonDTO {
    private Long id;
    private String maSanPham;
    private String tenSanPham;
    private Boolean trangThai;
}

