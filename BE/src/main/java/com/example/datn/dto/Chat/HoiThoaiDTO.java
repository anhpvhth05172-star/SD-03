package com.example.datn.dto.Chat;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HoiThoaiDTO {

    private Long id;
    private String loai;
    private String trangThai;
    private Long idKhachHang;
    private Long idNhanVienPhuTrach;
    private String tenKhach;
    private int tongTinKhach;
    private int tongTinNhanVien;
    private long tinChuaDoc;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private List<TinNhanDTO> tinNhanList;
}
