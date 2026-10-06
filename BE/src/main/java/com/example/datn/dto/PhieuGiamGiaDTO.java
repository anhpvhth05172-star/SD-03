package com.example.datn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PhieuGiamGiaDTO {

    private Long id;

    private String maPhieuGiamGia;

    private String tenPhieuGiamGia;

    private BigDecimal giaTriGiam;

    private String moTa;

    private String loaiGiamGia;

    private Integer soLuongDaSuDung;

    private LocalDateTime ngayKetThuc;

    private Boolean trangThai;

}
