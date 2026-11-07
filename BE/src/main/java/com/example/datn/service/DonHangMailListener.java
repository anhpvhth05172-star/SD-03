package com.example.datn.service;

import com.example.datn.dto.DonHangDatThanhCongEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DonHangMailListener {

    @Autowired
    private EmailService emailService;

    public void onDatHang(DonHangDatThanhCongEvent e) {
        emailService.guiMailDatHangThanhCong(
                e.getEmail(), e.getTenKhachHang(), e.getMaHoaDon(),
                e.getSoTien(), e.getPhuongThucThanhToan(), e.getDiaChiNhanHang());
    }
}
