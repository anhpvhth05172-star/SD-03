package com.example.datn.util;

import java.text.Normalizer;

/**
 * Sinh mã từ họ tên (dùng chung cho nhân viên và khách hàng).
 * Quy tắc: tên gọi cuối cùng viết hoa chữ đầu + chữ cái đầu của họ và tên đệm
 * viết hoa. Ví dụ: "Nguyễn Thị Bình" -&gt; "BinhNT", "Phạm Vũ Hoàng Anh" -&gt; "AnhPVH".
 * Khi trùng mã: cộng số thứ tự trực tiếp đằng sau (2, 3, ...), ví dụ: "AnhPVH2", "AnhPVH3".
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
            return cat(damBaoDauLaChuCai(cuoi), TOI_DA);
        }
        StringBuilder dau = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            String khongDau = boDau(parts[i]);
            if (!khongDau.isEmpty()) {
                dau.append(Character.toUpperCase(khongDau.charAt(0)));
            }
        }
        String goc = cuoi + dau;
        return cat(damBaoDauLaChuCai(goc), TOI_DA);
    }

    /**
     * Mã phải bắt đầu bằng chữ cái theo {@link #hopLe(String)}. Tên gọi cuối cùng
     * là chuỗi số (vd: "1234567") sẽ sinh ra mã bắt đầu bằng số — khi đó thêm
     * tiền tố "NV" để mã sinh ra luôn hợp lệ và import lại (cập nhật) không bị từ chối.
     */
    private static String damBaoDauLaChuCai(String ma) {
        if (ma.isEmpty()) {
            return "NV";
        }
        char c = ma.charAt(0);
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
            return ma;
        }
        return "NV" + ma;
    }

    /**
     * Cộng số thứ tự vào phần gốc: từ 2 trở lên cộng trực tiếp số (2, 3, ...).
     * Nếu số &lt;= 1, giữ nguyên phần gốc. Kết quả luôn nằm trong {@link #TOI_DA} ký tự.
     */
    public static String congSo(String goc, int so) {
        if (so <= 1) {
            return goc == null || goc.isBlank() ? "NV" : cat(goc, TOI_DA);
        }
        String hauTo = String.valueOf(so);
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
