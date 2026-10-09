package com.example.datn.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class PhieuGiamGiaUtil {

    private PhieuGiamGiaUtil() {
    }

    public static String tenHienThi(String loaiGiamGia, BigDecimal giaTriGiam) {
        if (giaTriGiam == null) {
            return null;
        }
        if ("PERCENT".equalsIgnoreCase(loaiGiamGia)) {
            return "Giảm " + giaTriGiam.stripTrailingZeros().toPlainString() + "%";
        }
        DecimalFormat tienTe = new DecimalFormat(
            "#,##0.########",
            DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"))
        );
        return "Giảm " + tienTe.format(giaTriGiam) + "đ";
    }
}
