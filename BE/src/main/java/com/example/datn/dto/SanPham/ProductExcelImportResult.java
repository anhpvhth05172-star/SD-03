package com.example.datn.dto.SanPham;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductExcelImportResult {
    private int tongSoDong;
    private int soDongThanhCong;
    private int soDongThatBai;
    private int soSanPhamMoi;
    private int soBienTheMoi;
    private int soBienTheCapNhat;

    @Builder.Default
    private List<String> danhSachLoi = new ArrayList<>();
}
