package com.example.datn.config;

import com.example.datn.entity.HoaDon;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.service.HoaDonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingPaymentCleanupJob {

    private final HoaDonRepository hoaDonRepository;
    private final HoaDonService hoaDonService;

    /**
     * Chạy định kỳ mỗi 1 phút: quét các đơn hàng CHO_THANH_TOAN (VNPAY) đã tạo quá 15 phút
     * mà khách hàng không hoàn tất thanh toán -> tự động hủy đơn và hoàn trả tồn kho giữ chỗ.
     * Mỗi đơn hàng được hủy trong một transaction độc lập (REQUIRES_NEW).
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void cancelExpiredPendingOrders() {
        try {
            LocalDateTime limit = LocalDateTime.now().minusMinutes(15);
            List<HoaDon> expired = hoaDonRepository
                    .findByTrangThaiAndNgayTaoBefore("CHO_THANH_TOAN", limit);

            for (HoaDon hd : expired) {
                try {
                    hoaDonService.huyDonChoThanhToan(hd.getId());
                    log.info("Đã tự động hủy đơn VNPay hết hạn #{} ({}) và hoàn tồn kho thành công", hd.getId(), hd.getMaHoaDon());
                } catch (Exception e) {
                    log.error("Lỗi khi hủy đơn VNPay hết hạn #{}: {}", hd.getId(), e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("Lỗi khi quét đơn VNPay hết hạn: {}", ex.getMessage());
        }
    }
}
