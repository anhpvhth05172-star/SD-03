package com.example.datn.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
public class NhanVienDTO {

    private Long id;
    private String maNhanVien;
    private String tenTaiKhoan;
    private String email;
    private String soDienThoai;
    private String anhNhanVien;
    private String gioiTinh;
    private String queQuan;
    private String phuong;
    private String diaChiCuThe;
    private LocalDate ngaySinh;
    private Boolean trangThai;
    private String trangThaiLabel;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private String nguoiCapNhat;

    private Long idVaiTro;
    private String maVaiTro;
    private String tenVaiTro;
}