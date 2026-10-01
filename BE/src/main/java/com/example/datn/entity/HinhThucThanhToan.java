package com.example.datn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "hinh_thuc_thanh_toan")
public class HinhThucThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_hinh_thuc", nullable = false, unique = true, length = 50)
    private String maHinhThuc;

    @Column(name = "ten_hinh_thuc", nullable = false, length = 100)
    private String tenHinhThuc;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
