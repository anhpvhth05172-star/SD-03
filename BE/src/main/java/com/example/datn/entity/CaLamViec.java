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
import java.math.BigDecimal;
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
@Table(name = "ca_lam_viec")
public class CaLamViec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ca", nullable = false, unique = true, length = 30)
    private String maCa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "ten_quy", length = 50)
    private String tenQuy = "Quầy tổng";

    @Column(name = "thoi_gian_mo", nullable = false)
    private LocalDateTime thoiGianMo;

    @Column(name = "thoi_gian_ket")
    private LocalDateTime thoiGianKet;

    @Column(name = "tien_dau_ca", nullable = false)
    private BigDecimal tienDauCa = BigDecimal.ZERO;

    @Column(name = "trang_thai", nullable = false, length = 20)
    private String trangThai;

    @Column(name = "tong_hoa_don")
    private Integer tongHoaDon;

    @Column(name = "tong_doanh_thu")
    private BigDecimal tongDoanhThu;

    @Column(name = "tien_mat_thu")
    private BigDecimal tienMatThu;

    @Column(name = "tien_khong_mat")
    private BigDecimal tienKhongMat;

    @Column(name = "tien_hoan_mat")
    private BigDecimal tienHoanMat;

    @Column(name = "tien_du_kien")
    private BigDecimal tienDuKien;

    @Column(name = "tien_thuc_te")
    private BigDecimal tienThucTe;

    @Column(name = "chenh_lech")
    private BigDecimal chenhLech;

    @Column(name = "ly_do_lech", length = 255)
    private String lyDoLech;

    @Column(name = "ghi_chu_mo", length = 255)
    private String ghiChuMo;

    @Column(name = "da_xoa", nullable = false)
    private boolean daXoa;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;
}
