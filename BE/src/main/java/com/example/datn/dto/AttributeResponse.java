package com.example.datn.dto;

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
public class AttributeResponse {

    private Long id;
    private String ma;
    private String ten;
    private String moTa;
    private String trangThai; // "Hoạt động" hoặc "Ngừng hoạt động"
    private Boolean status;   // true hoặc false
}
