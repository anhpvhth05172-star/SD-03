package com.example.datn.scheduler;

import com.example.datn.dto.ThongKe.GuiMailTongKetRequest;
import com.example.datn.dto.ThongKe.GuiMailTongKetResponse;
import com.example.datn.service.BaoCaoTongKetService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job that tu dong gui bao cao tong ket doanh thu:
 * 23:59 ngay cuoi moi thang (gui bao cao thang) va 23:59 31/12 (gui ca bao cao nam).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaoCaoTongKetScheduler {

    private final BaoCaoTongKetService baoCaoTongKetService;

    private static final ZoneId VUNG_GIO = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * Quyet dinh gui loai bao cao nao vao ngay nay:
     * ngay cuoi thang -> ["THANG"]; 31/12 -> ["THANG","NAM"]; ngay thuong -> [].
     */
    public static List<String> quyetDinhGui(LocalDate ngay) {
        List<String> ketQua = new ArrayList<>(2);
        if (ngay.getDayOfMonth() == ngay.lengthOfMonth()) {
            ketQua.add("THANG");
            if (ngay.getMonthValue() == 12) {
                ketQua.add("NAM");
            }
        }
        return ketQua;
    }

    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Ho_Chi_Minh")
    public void guiBaoCaoTuDong() {
        LocalDate ngay = LocalDate.now(VUNG_GIO);
        for (String loai : quyetDinhGui(ngay)) {
            try {
                String email = baoCaoTongKetService.emailMacDinh();
                if (email == null || email.isBlank()) {
                    log.warn("Bỏ qua gửi báo cáo tự động {}: chưa cấu hình email nhận", loai);
                    continue;
                }
                GuiMailTongKetRequest req = new GuiMailTongKetRequest(
                    loai,
                    loai.equals("THANG") ? ngay.getMonthValue() : null,
                    ngay.getYear(),
                    email
                );
                GuiMailTongKetResponse res = baoCaoTongKetService.guiMailTongKet(req);
                if (res.thanhCong()) {
                    log.info("Đã gửi báo cáo tự động {} ({}) tới {}", loai, res.tieuDe(), res.emailNhan());
                } else {
                    log.error("Gửi báo cáo tự động {} thất bại: {}", loai, res.lyDo());
                }
            } catch (Exception e) {
                log.error("Lỗi khi gửi báo cáo tự động {}: {}", loai, e.toString(), e);
            }
        }
    }
}
