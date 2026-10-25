package com.example.datn.dto.NhanVien;

import java.time.LocalDate;
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
public class NhanVienImportDong {

    private int dong;
    private String maNhanVien;
    private String tenTaiKhoan;
    private String email;
    private String soDienThoai;
    private String gioiTinh;
    private LocalDate ngaySinh;
    private String vaiTro;
    private String queQuan;
    private String phuong;
    private String diaChiCuThe;

    /** THEM = thêm mới, CAP_NHAT = cập nhật nhân viên đã có theo mã, null = dòng lỗi. */
    private String hanhDong;
    private String lyDoLoi;
}
