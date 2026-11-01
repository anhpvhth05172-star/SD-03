package com.example.datn.service;

import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.repository.NhanVienRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BaoCaoHangNgayService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NhanVienRepository nhanVienRepository;
    private final ThongKeService thongKeService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Bangkok")
    public void guiBaoCaoHomQua() {
        guiBaoCao(LocalDate.now(ZONE).minusDays(1));
    }

    void guiBaoCao(LocalDate ngay) {
        var admin = nhanVienRepository
            .findFirstByVaiTro_MaVaiTroAndTrangThaiTrueAndEmailIsNotNullOrderByIdAsc("ADMIN");
        if (admin.isEmpty() || admin.get().getEmail().isBlank()) {
            log.warn("Bỏ qua báo cáo ngày {}: chưa có email quản trị viên", ngay);
            return;
        }

        TongQuanDTO tongQuan = thongKeService.tongQuan(ngay, ngay, null);
        String trangThaiDon = thongKeService.trangThaiDon(ngay, ngay, null).stream()
            .filter(trangThai -> trangThai.soLuong() > 0)
            .map(trangThai -> trangThai.ten() + ": " + trangThai.soLuong())
            .collect(Collectors.joining("\n"));
        if (trangThaiDon.isBlank()) trangThaiDon = "Không có đơn hàng";

        String noiDung = "Báo cáo kinh doanh ngày " + ngay.format(DATE_FORMAT) + "\n\n" +
            "Doanh thu: " + NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
                .format(tongQuan.doanhThu()) + " đ\n" +
            "Số đơn: " + tongQuan.soDon() + "\n\n" +
            "Đơn hàng theo trạng thái:\n" + trangThaiDon;
        boolean accepted = emailService.guiMailCustom(
            admin.get().getEmail(),
            "Báo cáo kinh doanh PolyShoes - " + ngay.format(DATE_FORMAT),
            noiDung
        );
        if (!accepted) log.warn("Gửi báo cáo ngày {} không thành công", ngay);
    }
}
