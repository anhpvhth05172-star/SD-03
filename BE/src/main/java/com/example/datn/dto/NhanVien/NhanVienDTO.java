package com.example.datn.dto.NhanVien;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    /**
     * Trạng thái gửi email cấp tài khoản khi tạo mới:
     * true = nhà cung cấp mail đã chấp nhận, false = gửi thất bại, null = không áp dụng.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean emailGuiThanhCong;
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