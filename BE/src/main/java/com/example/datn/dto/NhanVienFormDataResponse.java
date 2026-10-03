package com.example.datn.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NhanVienFormDataResponse {

    private List<VaiTroOption> vaiTros;

    public record VaiTroOption(Long id, String ma, String ten) {}
}