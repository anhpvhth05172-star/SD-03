package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiaChiKhachHangRequest {

    private String tenDiaChi;
    private String tinhThanhPho;
    private String phuong;
    private String diaChiCuThe;
    private Boolean macDinh;
    private Boolean trangThai;
}
