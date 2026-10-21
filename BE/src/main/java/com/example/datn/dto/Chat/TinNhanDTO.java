package com.example.datn.dto.Chat;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TinNhanDTO {

    private Long id;
    private Long idHoiThoai;
    private String loaiNguoiGui;
    private Long idNguoiGui;
    private String noiDung;
    private boolean daDoc;
    private LocalDateTime ngayTao;
    private String tenNguoiGui;
}
