package com.example.datn.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PhieuGiamGiaDTO {

    private Long id;

    private String maPhieuGiamGia;

    private String tenPhieuGiamGia;

    private String loaiGiamGia;

    private BigDecimal giaTriGiam;

    private BigDecimal giamToiDa;

    private BigDecimal hoaDonToiThieu;

    private LocalDateTime ngayBatDau;

    private LocalDateTime ngayKetThuc;

    private Integer soLuong;

    private Integer soLuongDaSuDung;

    private Integer gioiHanMoiTaiKhoan;

    private String moTa;

    private Boolean trangThai;

    private DotGiamGiaDTO dotGiamGia;

    public PhieuGiamGiaDTO(
        Long id,
        String maPhieuGiamGia,
        String tenPhieuGiamGia,
        BigDecimal giaTriGiam,
        String moTa,
        Integer soLuongDaSuDung,
        LocalDateTime ngayKetThuc,
        Boolean trangThai
    ) {
        this.id = id;
        this.maPhieuGiamGia = maPhieuGiamGia;
        this.tenPhieuGiamGia = tenPhieuGiamGia;
        this.giaTriGiam = giaTriGiam;
        this.moTa = moTa;
        this.soLuongDaSuDung = soLuongDaSuDung;
        this.ngayKetThuc = ngayKetThuc;
        this.trangThai = trangThai;
    }

    public PhieuGiamGiaDTO(
        Long id,
        String maPhieuGiamGia,
        String tenPhieuGiamGia,
        String loaiGiamGia,
        BigDecimal giaTriGiam,
        BigDecimal giamToiDa,
        BigDecimal hoaDonToiThieu,
        LocalDateTime ngayBatDau,
        LocalDateTime ngayKetThuc,
        Integer soLuong,
        Integer soLuongDaSuDung,
        Integer gioiHanMoiTaiKhoan,
        Boolean trangThai,
        DotGiamGiaDTO dotGiamGia
    ) {
        this.id = id;
        this.maPhieuGiamGia = maPhieuGiamGia;
        this.tenPhieuGiamGia = tenPhieuGiamGia;
        this.loaiGiamGia = loaiGiamGia;
        this.giaTriGiam = giaTriGiam;
        this.giamToiDa = giamToiDa;
        this.hoaDonToiThieu = hoaDonToiThieu;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
        this.soLuong = soLuong;
        this.soLuongDaSuDung = soLuongDaSuDung;
        this.gioiHanMoiTaiKhoan = gioiHanMoiTaiKhoan;
        this.trangThai = trangThai;
        this.dotGiamGia = dotGiamGia;
    }

    public record DotGiamGiaDTO(Long id, String ma, String ten) {}
}
