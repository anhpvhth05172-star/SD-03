//package com.example.datn.config;
//
//import com.example.datn.entity.HoaDon;
//import com.example.datn.entity.LichSuHoaDon;
//import com.example.datn.repository.HoaDonRepository;
//import com.example.datn.repository.LichSuHoaDonRepository;
//import com.example.datn.service.VNPayService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PendingPaymentCleanupJob {
//
//    private final HoaDonRepository hoaDonRepository;
//    private final LichSuHoaDonRepository lichSuHoaDonRepository;
//
//    @Scheduled(fixedDelay = 5 * 60 * 1000)
//    @Transactional
//    public void cancelExpiredPendingOrders() {
//        LocalDateTime limit = LocalDateTime.now().minusMinutes(15);
//        List<HoaDon> expired = hoaDonRepository
//                .findByTrangThaiAndNgayTaoBefore(VNPayService.TT_CHO_THANH_TOAN, limit);
//
//        for (HoaDon hd : expired) {
//            hd.setTrangThai(VNPayService.TT_DA_HUY);
//            hd.setGhiChu("Hết hạn thanh toán VNPay, đơn tự động hủy");
//            hd.setNgayCapNhat(LocalDateTime.now());
//            hd.setNguoiCapNhat("SYSTEM");
//            hoaDonRepository.save(hd);
//
//            LichSuHoaDon ls = new LichSuHoaDon();
//            ls.setHoaDon(hd);
//            ls.setTrangThai(hd.getTrangThai());
//            ls.setHanhDong("HET_HAN_THANH_TOAN");
//            ls.setGhiChu("Đơn chờ thanh toán quá 15 phút, tự động hủy");
//            lichSuHoaDonRepository.save(ls);
//        }
//        if (!expired.isEmpty()) {
//            log.info("Đã hủy {} đơn VNPay hết hạn", expired.size());
//        }
//    }
//}
