package com.example.datn.dto.ThongKe;

/**
 * Yeu cau gui mail tong ket doanh thu.
 * loai: THANG (can thang 1..12 + nam) hoac NAM (can nam).
 */
public record GuiMailTongKetRequest(
    String loai,
    Integer thang,
    Integer nam,
    String emailNhan
) {}
