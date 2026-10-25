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
@Table(name = "lich_su_ca")
public class LichSuCa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ca_lam_viec", nullable = false)
    private CaLamViec caLamViec;

    @Column(name = "hanh_dong", nullable = false, length = 30)
    private String hanhDong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nhan_vien", nullable = false)
    private NhanVien nhanVien;

    @Column(name = "thoi_gian", insertable = false, updatable = false)
    private LocalDateTime thoiGian;

    @Column(name = "truoc_sau", columnDefinition = "NVARCHAR(MAX)")
    private String truocSau;

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
