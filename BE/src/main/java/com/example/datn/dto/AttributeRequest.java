package com.example.datn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class AttributeRequest {

    private String ma;

    @NotBlank(message = "Tên thuộc tính không được để trống")
    @Size(max = 150, message = "Tên thuộc tính không được vượt quá 150 ký tự")
    private String ten;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String moTa;

    private Boolean trangThai;
}
