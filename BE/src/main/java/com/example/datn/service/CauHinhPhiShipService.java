package com.example.datn.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CauHinhPhiShipService {

    public static final String KEY_FREESHIP_THRESHOLD = "FREESHIP_THRESHOLD";
    public static final BigDecimal DEFAULT_FREESHIP_THRESHOLD = BigDecimal.valueOf(10_000_000L);
    private static final BigDecimal MAX_FREESHIP_THRESHOLD = BigDecimal.valueOf(1_000_000_000L);

    private final JdbcTemplate jdbcTemplate;

    public CauHinhPhiShipService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal getFreeshipThreshold() {
        try {
            List<String> rows = jdbcTemplate.queryForList(
                "SELECT gia_tri FROM dbo.cau_hinh_he_thong WHERE ma_cau_hinh = ?",
                String.class,
                KEY_FREESHIP_THRESHOLD
            );
            if (!rows.isEmpty() && rows.get(0) != null && !rows.get(0).isBlank()) {
                BigDecimal val = new BigDecimal(rows.get(0).trim());
                if (val.compareTo(BigDecimal.ZERO) >= 0) {
                    return val;
                }
            }
        } catch (Exception ignored) {
            // Fallback mặc định khi bảng chưa được migrate hoặc trong unit test
        }
        return DEFAULT_FREESHIP_THRESHOLD;
    }

    public BigDecimal updateFreeshipThreshold(BigDecimal threshold, String nguoiCapNhat) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mốc miễn phí vận chuyển phải lớn hơn hoặc bằng 0");
        }
        if (threshold.compareTo(MAX_FREESHIP_THRESHOLD) > 0) {
            throw new IllegalArgumentException("Mốc miễn phí vận chuyển không được vượt quá 1.000.000.000 ₫");
        }
        String giaTri = threshold.stripTrailingZeros().toPlainString();
        int updated = jdbcTemplate.update(
            "UPDATE dbo.cau_hinh_he_thong SET gia_tri = ?, ngay_cap_nhat = SYSDATETIME(), nguoi_cap_nhat = ? WHERE ma_cau_hinh = ?",
            giaTri,
            nguoiCapNhat,
            KEY_FREESHIP_THRESHOLD
        );
        if (updated == 0) {
            jdbcTemplate.update(
                "INSERT INTO dbo.cau_hinh_he_thong (ma_cau_hinh, gia_tri, nguoi_cap_nhat) VALUES (?, ?, ?)",
                KEY_FREESHIP_THRESHOLD,
                giaTri,
                nguoiCapNhat
            );
        }
        return new BigDecimal(giaTri);
    }
}
