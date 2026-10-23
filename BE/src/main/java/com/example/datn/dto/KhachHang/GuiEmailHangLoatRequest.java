package com.example.datn.dto.KhachHang;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GuiEmailHangLoatRequest {

    private List<Long> ids;
    private String tieuDe;
    private String noiDung;
}