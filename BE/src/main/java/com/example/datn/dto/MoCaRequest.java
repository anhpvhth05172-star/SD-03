package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoCaRequest {

    private Long idCaLich;
    private Long tienDauCa;
    private String ghiChu;

    public MoCaRequest(Long tienDauCa, String ghiChu) {
        this.tienDauCa = tienDauCa;
        this.ghiChu = ghiChu;
    }
}
