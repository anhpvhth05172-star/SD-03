package com.example.datn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TongKetDto {

    private Long id;
    private String maCa;
    private Long idNhanVien;
    private String tenNhanVien;
    private String tenQuy;
    private LocalDateTime thoiGianMo;
    private String trangThai;
    private BigDecimal tienDauCa;
    private Integer tongHoaDon;
    private BigDecimal tongDoanhThu;
    private BigDecimal tienMatThu;
    private BigDecimal tienKhongMat;
    private BigDecimal tienHoanMat;
    private BigDecimal tienDuKien;
    private List<HoaDonTongHopItem> hoaDonGanDay;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoaDonTongHopItem {

        private String maHoaDon;
        private LocalDateTime ngayTao;
        private String tenPhuongThuc;
        private BigDecimal tienSauGiamGia;
        private String trangThai;
    }
}
