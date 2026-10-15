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
@Table(name = "tin_nhan")
public class TinNhan {

    public static final String GUI_KHACH = "KHACH";
    public static final String GUI_NHAN_VIEN = "NHAN_VIEN";
    public static final String GUI_AI = "AI";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_hoi_thoai", nullable = false)
    private HoiThoai hoiThoai;

    @Column(name = "loai_nguoi_gui", nullable = false, length = 20)
    private String loaiNguoiGui;

    @Column(name = "id_nguoi_gui")
    private Long idNguoiGui;

    @Column(name = "noi_dung", nullable = false, length = 2000, columnDefinition = "NVARCHAR(2000)")
    private String noiDung;

    @Column(name = "da_doc", nullable = false)
    private boolean daDoc;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;
}
