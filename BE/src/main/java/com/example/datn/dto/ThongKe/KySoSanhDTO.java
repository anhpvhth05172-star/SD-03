package com.example.datn.dto.ThongKe;

import java.time.LocalDate;

/**
 * 1 ky so sanh doanh thu (thang/quy/nam/2ky).
 * den bao gom ngay cuoi ky; chenhLech = doanhThu - ky truoc (ky dau lay 0);
 * tangTruong theo cung cong thuc statisticsData.tinhChuKy.
 */
public record KySoSanhDTO(
    String maKy,
    String tenKy,
    LocalDate tu,
    LocalDate den,
    long doanhThu,
    long tienMat,
    long tienChuyenKhoan,
    long soDon,
    long aov,
    long sanPhamBan,
    long chenhLech,
    int tangTruong,
    boolean laCaoNhat
) {}
