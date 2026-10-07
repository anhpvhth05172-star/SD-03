package com.example.datn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "phien_dang_nhap")
public class PhienDangNhap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_khach_hang", nullable = false)
    private Long idKhachHang;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    @Column(name = "hoat_dong_cuoi", nullable = false)
    private LocalDateTime hoatDongCuoi;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "ngay_dang_xuat")
    private LocalDateTime ngayDangXuat;

    public boolean hetHan(LocalDateTime bayGio, long thoiGianPhienMs) {
        return hoatDongCuoi == null
            || bayGio.isAfter(hoatDongCuoi.plusNanos(thoiGianPhienMs * 1_000_000L));
    }
}
