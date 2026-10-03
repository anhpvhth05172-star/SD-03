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
@Table(name = "san_pham_chi_tiet")
public class SanPhamChiTiet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_san_pham", nullable = false)
    private SanPham sanPham;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kich_co", nullable = false)
    private KichCo kichCo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mau_sac", nullable = false)
    private MauSac mauSac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_than_giay", nullable = false)
    private ThanGiay thanGiay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_de_giay", nullable = false)
    private DeGiay deGiay;

    @Column(name = "ma_chi_tiet_san_pham", nullable = false, unique = true, length = 80)
    private String maChiTietSanPham;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong = 0;

    @Column(name = "gia_ban", nullable = false)
    private BigDecimal giaBan;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "trong_luong")
    private BigDecimal trongLuong;

    @Column(name = "chieu_cao_de")
    private BigDecimal chieuCaoDe;

    @Column(name = "chieu_cao_got")
    private BigDecimal chieuCaoGot;

    @Column(name = "do_chenh_got_mui")
    private BigDecimal doChenhGotMui;

    @Column(name = "form_giay", length = 100)
    private String formGiay;

    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;
}
