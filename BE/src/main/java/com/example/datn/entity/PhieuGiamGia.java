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
@Table(name = "phieu_giam_gia")
public class PhieuGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_phieu_giam_gia", nullable = false, unique = true, length = 50)
    private String maPhieuGiamGia;

    @Column(name = "ten_phieu_giam_gia", nullable = false, length = 200)
    private String tenPhieuGiamGia;

    @Column(name = "loai_giam_gia", nullable = false, length = 30)
    private String loaiGiamGia;

    @Column(name = "gia_tri_giam", nullable = false)
    private BigDecimal giaTriGiam;

    @Column(name = "giam_toi_da")
    private BigDecimal giamToiDa;

    @Column(name = "hoa_don_toi_thieu")
    private BigDecimal hoaDonToiThieu;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong = 0;

    @Column(name = "so_luong_da_su_dung", nullable = false)
    private Integer soLuongDaSuDung = 0;

    @Column(name = "gioi_han_moi_tai_khoan", nullable = false)
    private Integer gioiHanMoiTaiKhoan = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dot_giam_gia")
    private DotGiamGia dotGiamGia;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "mo_ta", length = 500)
    private String moTa;
}
