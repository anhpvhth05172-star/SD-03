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
@Table(name = "loai_giay")
public class LoaiGiay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_loai_giay", nullable = false, unique = true, length = 50)
    private String maLoaiGiay;

    @Column(name = "ten_loai_giay", nullable = false, length = 100)
    private String tenLoaiGiay;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Column(name = "trang_thai", nullable = false)
    private Boolean trangThai = true;
}
