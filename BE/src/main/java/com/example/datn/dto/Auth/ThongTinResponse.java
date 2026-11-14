package com.example.datn.dto.Auth;

import com.example.datn.dto.KhachHang.DiaChiKhachHangDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class ThongTinResponse {

    private Long id;
    private String tenTaiKhoan;
    private String tenKhachHang;
    private String email;
    private String soDienThoai;
    private String vaiTro;

    private String maKhachHang;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String soCccd;
    private String hangThanhVien;
    private String maHangThanhVien;
    private Long soDon;
    private BigDecimal tongChiTieu;
    private DiaChiKhachHangDTO diaChiMacDinh;
    private List<DiaChiKhachHangDTO> danhSachDiaChi;
    private LocalDateTime ngayTao;

    public ThongTinResponse(Long id, String tenTaiKhoan, String tenKhachHang, String email, String soDienThoai, String vaiTro) {
        this.id = id;
        this.tenTaiKhoan = tenTaiKhoan;
        this.tenKhachHang = tenKhachHang;
        this.email = email;
        this.soDienThoai = soDienThoai;
        this.vaiTro = vaiTro;
    }
}
