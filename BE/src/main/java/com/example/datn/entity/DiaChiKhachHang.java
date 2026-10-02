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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dia_chi_khach_hang")
public class DiaChiKhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_khach_hang", nullable = false)
    private KhachHang khachHang;

    @Column(name = "ma_dia_chi", nullable = false, length = 50)
    private String maDiaChi;

    @Column(name = "ten_chi_chi", length = 100)
    private String tenChiChi;

    @Column(name = "thanh_pho", length = 100)
    private String thanhPho;

    @Column(name = "phuong", length = 100)
    private String phuong;

    @Column(name = "dia_chi_cu_the", nullable = false, length = 255)
    private String diaChiCuThe;

    @Column(name = "mac_dinh", nullable = false)
    private Boolean macDinh = false;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
