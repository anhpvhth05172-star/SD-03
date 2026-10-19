package com.example.datn.util;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Kiểm tra hàng tiêu đề của tệp Excel import: phải đủ cột và đúng thứ tự
 * theo mẫu, tránh đọc nhầm dữ liệu khi tệp sai cấu trúc (thiếu cột,
 * sai thứ tự, hoặc dùng file xuất từ màn hình khác).
 */
public final class ExcelTieuDeUtil {

    private ExcelTieuDeUtil() {
    }

    /** Bỏ dấu tiếng Việt, viết thường, gộp khoảng trắng để so khớp tiêu đề. */
    public static String chuanHoa(String s) {
        if (s == null) {
            return "";
        }
        String x = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .toLowerCase()
            .replace('đ', 'd');
        return x.replaceAll("\\s+", " ").trim();
    }

    /**
     * @param sheet  sheet đọc được từ tệp (hàng 0 là tiêu đề)
     * @param formatter formatter của POI để lấy text ô
     * @param kyMon  chuỗi chuẩn hóa bắt buộc bắt đầu mỗi cột (theo thứ tự)
     * @param mau    chuỗi tiêu đề mẫu tương ứng (dùng trong thông báo lỗi)
     * @throws IllegalArgumentException nếu thiếu hàng tiêu đề, thiếu cột hoặc sai thứ tự
     */
    public static void kiemTraTieuDe(Sheet sheet, DataFormatter formatter, String[] kyMon, String[] mau) {
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new IllegalArgumentException(
                "Sai cấu trúc tệp Excel: thiếu hàng tiêu đề. Hãy dùng đúng mẫu tệp Excel của hệ thống.");
        }
        for (int i = 0; i < kyMon.length; i++) {
            String text = formatter.formatCellValue(header.getCell(i)).trim();
            String chuanHoa = chuanHoa(text);
            if (chuanHoa.isEmpty()) {
                throw new IllegalArgumentException(
                    "Sai cấu trúc tệp Excel: thiếu cột bắt buộc thứ " + (i + 1)
                        + " (mong đợi \"" + mau[i] + "\"). Hãy dùng đúng mẫu tệp Excel của hệ thống.");
            }
            if (!chuanHoa.startsWith(kyMon[i])) {
                throw new IllegalArgumentException(
                    "Sai cấu trúc tệp Excel: cột thứ " + (i + 1) + " là \"" + text
                        + "\" nhưng mong đợi \"" + mau[i] + "\". Hãy dùng đúng mẫu tệp Excel của hệ thống.");
            }
        }
    }
}
