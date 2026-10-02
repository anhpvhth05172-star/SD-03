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
@Table(name = "phuong_thuc_thanh_toan")
public class PhuongThucThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_hinh_thuc_thanh_toan", nullable = false)
    private HinhThucThanhToan hinhThucThanhToan;

    @Column(name = "ma_phuong_thuc", nullable = false, unique = true, length = 50)
    private String maPhuongThuc;

    @Column(name = "ten_phuong_thuc", nullable = false, length = 100)
    private String tenPhuongThuc;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
