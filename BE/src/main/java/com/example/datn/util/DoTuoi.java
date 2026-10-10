package com.example.datn.util;

import java.time.LocalDate;

/**
 * Tính tuổi theo ngày/tháng/năm thực tế (không chỉ lấy hiệu năm).
 * Dùng chung cho validate tuổi khách hàng và nhân viên.
 */
public final class DoTuoi {

    private DoTuoi() {
    }

    /** Tuổi thực tế tại ngày {@code homNay}; trả về -1 nếu không tính được. */
    public static int tinhTuoi(LocalDate ngaySinh, LocalDate homNay) {
        if (ngaySinh == null || homNay == null) {
            return -1;
        }
        int tuoi = homNay.getYear() - ngaySinh.getYear();
        boolean chuaDenSinhNham =
            homNay.getMonthValue() < ngaySinh.getMonthValue()
                || (homNay.getMonthValue() == ngaySinh.getMonthValue()
                    && homNay.getDayOfMonth() < ngaySinh.getDayOfMonth());
        if (chuaDenSinhNham) {
            tuoi--;
        }
        return tuoi;
    }

    public static boolean duTuoi(LocalDate ngaySinh, int tuoiToiThieu, LocalDate homNay) {
        return ngaySinh != null && tinhTuoi(ngaySinh, homNay) >= tuoiToiThieu;
    }

    /**
     * Kiểm tra ngày sinh: không được ở tương lai và phải đủ {@code tuoiToiThieu}.
     * Trả về thông báo lỗi, hoặc null nếu hợp lệ.
     */
    public static String kiemTra(LocalDate ngaySinh, int tuoiToiThieu, String thongBao) {
        if (ngaySinh == null) {
            return null;
        }
        LocalDate homNay = LocalDate.now();
        if (ngaySinh.isAfter(homNay)) {
            return "Ngày sinh không được ở tương lai";
        }
        return duTuoi(ngaySinh, tuoiToiThieu, homNay) ? null : thongBao;
    }
}
