package com.example.datn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "danh_muc_ca_lam_viec")
public class DanhMucCaLamViec {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_ca", nullable = false, unique = true, length = 50)
    private String ma;

    @Column(name = "ten_ca", nullable = false, unique = true, length = 100, columnDefinition = "NVARCHAR(100)")
    private String ten;

    @Column(name = "gio_bat_dau", nullable = false)
    private LocalTime batDau;

    @Column(name = "gio_ket_thuc", nullable = false)
    private LocalTime ketThuc;

    @Column(name = "dang_bat", nullable = false)
    private boolean dangBat = true;
}
