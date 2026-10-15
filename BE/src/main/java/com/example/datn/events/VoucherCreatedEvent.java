package com.example.datn.events;

import com.example.datn.entity.KhachHang;
import com.example.datn.entity.PhieuGiamGia;

import java.util.List;

public record VoucherCreatedEvent(
        PhieuGiamGia phieu, List<KhachHang> khachHangs
) {
}
