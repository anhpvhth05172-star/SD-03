package com.example.datn.dto.HoaDon;

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
public class LichSuHoaDonDTO {

    private Long id;
    private Long idHoaDon;
    private String maHoaDon;
    private LocalDateTime thoiGian;
    private String trangThaiTruoc;
    private String trangThaiSau;
    private String maTrangThaiSau;
    private String ghiChu;
    private String hanhDong;
}
