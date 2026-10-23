package com.example.datn.dto;

import java.time.LocalDateTime;
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
public class LichSuCaDto {

    private String hanhDong;
    private String tenNhanVien;
    private LocalDateTime thoiGian;
    private String ghiChu;
}
