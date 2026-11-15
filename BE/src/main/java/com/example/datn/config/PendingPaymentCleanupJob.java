package com.example.datn.config;

import com.example.datn.entity.HoaDon;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.service.HoaDonService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentCleanupJob {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonService hoaDonService;

    @Scheduled(fixedDelay = 60 * 1000)
    public void cancelExpiredPendingOrders() {
        // Treo đơn 5 phút sau khi thanh toán không thành công, quá 5 phút sẽ tự động hủy
        LocalDateTime limit = LocalDateTime.now().minusMinutes(5);
        List<HoaDon> expired = hoaDonRepository
                .findByTrangThaiAndNgayTaoBefore("CHO_THANH_TOAN", limit);

        for (HoaDon hd : expired) {
            try {
                hoaDonService.huyDonChoThanhToan(hd.getId());
                log.info("Đã tự động hủy đơn chờ thanh toán quá 5 phút: {}", hd.getMaHoaDon());
            } catch (Exception ex) {
                log.warn("Lỗi khi tự động hủy đơn chờ {}: {}", hd.getMaHoaDon(), ex.getMessage());
            }
        }
        if (!expired.isEmpty()) {
            log.info("PendingPaymentCleanupJob: Đã dọn dẹp {} đơn treo quá 5 phút", expired.size());
        }
    }
}
