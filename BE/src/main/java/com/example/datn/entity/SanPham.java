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
@Table(name = "san_pham")
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_xuat_xu", nullable = false)
    private XuatXu xuatXu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_thuong_hieu", nullable = false)
    private ThuongHieu thuongHieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chat_lieu", nullable = false)
    private ChatLieu chatLieu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kieu_dang", nullable = false)
    private KieuDang kieuDang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_loai_giay", nullable = false)
    private LoaiGiay loaiGiay;

    @Column(name = "ma_san_pham", nullable = false, unique = true, length = 50)
    private String maSanPham;

    @Column(name = "ten_san_pham", nullable = false, length = 200)
    private String tenSanPham;

    @Column(name = "doi_tuong", length = 100)
    private String doiTuong;

    @Column(name = "tinh_nang", length = 1000)
    private String tinhNang;

    @Column(name = "mo_ta")
    private String moTa;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_tao", length = 100)
    private String nguoiTao;

    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
