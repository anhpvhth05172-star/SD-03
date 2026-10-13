package com.example.datn.dto.KhachHang;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
public class KhachHangDTO {

    private Long id;
    private String maKhachHang;
    private String tenTaiKhoan;
    private String tenKhachHang;
    private String email;
    private String soDienThoai;
    private String soCccd;
    /**
     * Trạng thái gửi email thông tin tài khoản khi tạo mới:
     * true = nhà cung cấp mail đã chấp nhận, false = gửi thất bại, null = không áp dụng.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean emailGuiThanhCong;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private Boolean trangThai;
    private String trangThaiLabel;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private String nguoiCapNhat;

    private String maHangThanhVien;
    private String hangThanhVien;
    private Long soDon;
    private BigDecimal tongChiTieu;

    private DiaChiKhachHangDTO diaChiMacDinh;
    private List<DiaChiKhachHangDTO> danhSachDiaChi;
}