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
public class CaLamViecDto {

    private Long id;
    private String maCa;
    private Long idNhanVien;
    private String maNhanVien;
    private String tenNhanVien;
    private String tenQuy;
    private LocalDateTime thoiGianMo;
    private LocalDateTime thoiGianKet;
    private BigDecimal tienDauCa;
    private String trangThai;
    private Integer tongHoaDon;
    private BigDecimal tongDoanhThu;
    private BigDecimal tienMatThu;
    private BigDecimal tienKhongMat;
    private BigDecimal tienHoanMat;
    private BigDecimal tienDuKien;
    private BigDecimal tienThucTe;
    private BigDecimal chenhLech;
    private String lyDoLech;
    private String ghiChuMo;
    private List<LichSuCaDto> lichSu;
    private String tenNguoiNhan;
}
