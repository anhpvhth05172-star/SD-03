package com.example.datn.util;

import java.text.Normalizer;

/**
 * Sinh mã từ họ tên (dùng chung cho nhân viên và khách hàng).
 * Quy tắc: tên gọi cuối cùng viết hoa chữ đầu + chữ cái đầu của họ và tên đệm
 * (viết thường, không dấu), cộng số thứ tự 2 chữ số.
 * Ví dụ: "Nguyễn Thị Bình" -&gt; "Binhnt01", "Phạm Hà Anh" -&gt; "Anhph01".
 */
public final class MaNhanVien {

    public static final int TOI_DA = 25;

    private MaNhanVien() {
    }

    /** Phần gốc (chưa cộng số thứ tự) của mã sinh từ họ tên. */
    public static String sinh(String ten) {
        if (ten == null || ten.isBlank()) {
            return "NV";
        }
        String[] parts = ten.trim().replaceAll("\\s+", " ").split(" ");
        String cuoi = vietHoaDau(boDau(parts[parts.length - 1]).toLowerCase());
        if (parts.length == 1) {
            return cat(cuoi, TOI_DA);
        }
        StringBuilder dau = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            String khongDau = boDau(parts[i]).toLowerCase();
            if (!khongDau.isEmpty()) {
                dau.append(khongDau.charAt(0));
            }
        }
        String goc = cuoi + dau;
        return cat(goc, TOI_DA);
    }

    /**
     * Cộng số thứ tự vào phần gốc: 01..99 (đúng 2 chữ số), từ 100 trở lên giữ nguyên
     * số chữ số. Kết quả luôn nằm trong {@link #TOI_DA} ký tự.
     */
    public static String congSo(String goc, int so) {
        int thuTu = Math.max(so, 1);
        String hauTo = thuTu < 100 ? String.format("%02d", thuTu) : String.valueOf(thuTu);
        int toiDaGoc = Math.max(1, TOI_DA - hauTo.length());
        String gocCat = goc == null || goc.isBlank() ? "NV" : cat(goc, toiDaGoc);
        return gocCat + hauTo;
    }

    public static boolean hopLe(String ma) {
        return ma != null && ma.matches("^[A-Za-z][A-Za-z0-9]{0,24}$");
    }

    public static String dayDu(String ma, int toiDa) {
        if (ma == null) {
            return null;
        }
        return ma.length() <= toiDa ? ma : ma.substring(0, toiDa);
    }

    private static String cat(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String vietHoaDau(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String boDau(String s) {
        String khongDau = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return khongDau.replace('đ', 'd').replace('Đ', 'D');
    }
}
