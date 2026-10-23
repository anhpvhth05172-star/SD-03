package com.example.datn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ca_lam")
public class CaLam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ca", nullable = false, unique = true, length = 50, columnDefinition = "NVARCHAR(50)")
    private String maCa;

    @Column(name = "ten_ca", nullable = false, length = 100, columnDefinition = "NVARCHAR(100)")
    private String tenCa;

    @Column(name = "bat_dau", nullable = false)
    private LocalTime batDau;

    @Column(name = "ket_thuc", nullable = false)
    private LocalTime ketThuc;

    @Column(name = "dang_bat", nullable = false)
    private Boolean dangBat = true;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "nguoi_tao", length = 100)
    private String nguoiTao;

    @Column(name = "nguoi_cap_nhat", length = 100)
    private String nguoiCapNhat;
}
