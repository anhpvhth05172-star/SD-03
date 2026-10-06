package com.example.datn.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PhieuGiamGiaUtilTest {

    @Test
    void tenPhanTram10() {
        assertThat(PhieuGiamGiaUtil.tenHienThi("PERCENT", new BigDecimal("10")))
            .isEqualTo("Giảm 10%");
    }

    @Test
    void tenPhanTram20() {
        assertThat(PhieuGiamGiaUtil.tenHienThi("PERCENT", new BigDecimal("20.00")))
            .isEqualTo("Giảm 20%");
    }

    @Test
    void tien60000() {
        assertThat(PhieuGiamGiaUtil.tenHienThi("AMOUNT", new BigDecimal("60000.00")))
            .isEqualTo("Giảm 60.000đ");
    }

    @Test
    void tien70000() {
        assertThat(PhieuGiamGiaUtil.tenHienThi("AMOUNT", new BigDecimal("70000")))
            .isEqualTo("Giảm 70.000đ");
    }

    @Test
    void phanTramLe() {
        assertThat(PhieuGiamGiaUtil.tenHienThi("PERCENT", new BigDecimal("10.5")))
            .isEqualTo("Giảm 10.5%");
    }
}
