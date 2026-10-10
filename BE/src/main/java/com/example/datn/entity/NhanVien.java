package com.example.datn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "nhan_vien")
public class NhanVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vai_tro", nullable = false)
    private VaiTro vaiTro;

    @Column(name = "ma_nhan_vien", nullable = false, unique = true, length = 50)
    private String maNhanVien;

    @Column(name = "ten_tai_khoan", nullable = false, unique = true, length = 100, columnDefinition = "NVARCHAR(100)")
    private String tenTaiKhoan;

    @Column(name = "mat_khau", nullable = false, length = 255)
    private String matKhau;

    @Column(name = "email", length = 150, columnDefinition = "NVARCHAR(150)")
    private String email;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "anh_nhan_vien", columnDefinition = "NVARCHAR(MAX)")
    private String anhNhanVien;

    @Column(name = "gioi_tinh", length = 20, columnDefinition = "NVARCHAR(20)")
    private String gioiTinh;

    @Column(name = "que_quan", length = 150, columnDefinition = "NVARCHAR(150)")
    private String queQuan;

    @Column(name = "phuong", length = 100, columnDefinition = "NVARCHAR(100)")
    private String phuong;

    @Column(name = "dia_chi_cu_the", length = 255, columnDefinition = "NVARCHAR(255)")
    private String diaChiCuThe;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "nguoi_tao", length = 100)
    private String nguoiTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;
}
