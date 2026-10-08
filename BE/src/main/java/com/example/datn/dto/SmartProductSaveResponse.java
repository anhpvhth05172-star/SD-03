package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartProductSaveResponse {

    // "SUCCESS" or "NEED_CONFIRMATION"
    private String status;
    private String message;

    private Long idSanPham;
    private String maSanPham;
    private String tenSanPham;

    private List<MergePreviewItem> previewItems;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MergePreviewItem {
        private Long idMauSac;
        private String tenMau;
        private String maMau;

        private Long idKichCo;
        private String tenKichCo;

        private Boolean isExisting; // true: đã có trong DB, false: thêm mới hoàn toàn
        private Integer currentStock; // Tồn hiện tại trong DB
        private Integer addedStock;   // Số lượng nhập thêm
        private Integer totalStock;   // Tổng tồn sau cập nhật
        
        private BigDecimal oldPrice;  // Giá cũ trong DB
        private BigDecimal newPrice;  // Giá mới vừa nhập
    }
}
