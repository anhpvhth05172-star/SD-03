package com.example.datn.service;

import com.example.datn.dto.ThongKe.GuiMailTongKetRequest;
import com.example.datn.dto.ThongKe.GuiMailTongKetResponse;
import com.example.datn.dto.ThongKe.KySoSanhDTO;
import com.example.datn.dto.ThongKe.SoSanhDTO;
import com.example.datn.dto.ThongKe.TopSanPhamKyDTO;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.entity.NhanVien;
import com.example.datn.repository.NhanVienRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Bao cao tong ket doanh thu: tinh so lieu that theo ky, soan noi dung
 * plain text va gui qua EmailService.guiMailCustom (ket qua that, khong gia lap).
 */
@Service
public class BaoCaoTongKetService {

    private final ThongKeService thongKeService;
    private final EmailService emailService;
    private final NhanVienRepository nhanVienRepository;
    private final String mailUsername;

    public BaoCaoTongKetService(
        ThongKeService thongKeService,
        EmailService emailService,
        NhanVienRepository nhanVienRepository,
        @Value("${spring.mail.username:}") String mailUsername
    ) {
        this.thongKeService = thongKeService;
        this.emailService = emailService;
        this.nhanVienRepository = nhanVienRepository;
        this.mailUsername = mailUsername;
    }

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String TEN_HE_THONG = "Hệ thống quản lý bán hàng";

    /* ---------------------------------- Validate ---------------------------------- */

    private void validate(GuiMailTongKetRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Thiếu thông tin yêu cầu");
        }
        String loai = req.loai();
        if (!"THANG".equals(loai) && !"NAM".equals(loai)) {
            throw new IllegalArgumentException("loai phải là THANG hoặc NAM");
        }
        if (req.nam() == null || req.nam() < 2000 || req.nam() > 2100) {
            throw new IllegalArgumentException("Năm không hợp lệ: " + req.nam());
        }
        if ("THANG".equals(loai) && (req.thang() == null || req.thang() < 1 || req.thang() > 12)) {
            throw new IllegalArgumentException("Tháng không hợp lệ: " + req.thang());
        }
        String email = req.emailNhan();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email người nhận không được để trống");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email người nhận không hợp lệ: " + email);
        }
    }

    /* ---------------------------------- Gui mail ---------------------------------- */

    public GuiMailTongKetResponse guiMailTongKet(GuiMailTongKetRequest req) {
        validate(req);

        boolean laThang = "THANG".equals(req.loai());
        int nam = req.nam();
        int thang = laThang ? req.thang() : 0;

        // Ky hien tai + ky truoc (thang truoc / nam truoc)
        LocalDate tu, den, tuTruoc, denTruoc;
        if (laThang) {
            tu = LocalDate.of(nam, thang, 1);
            den = LocalDate.of(nam, thang, tu.lengthOfMonth());
            LocalDate thangTruoc = tu.minusMonths(1);
            tuTruoc = thangTruoc;
            denTruoc = LocalDate.of(thangTruoc.getYear(), thangTruoc.getMonth(), thangTruoc.lengthOfMonth());
        } else {
            tu = LocalDate.of(nam, 1, 1);
            den = LocalDate.of(nam, 12, 31);
            tuTruoc = LocalDate.of(nam - 1, 1, 1);
            denTruoc = LocalDate.of(nam - 1, 12, 31);
        }

        TongQuanDTO hienTai = thongKeService.tongQuan(tu, den, null);
        TongQuanDTO truoc = thongKeService.tongQuan(tuTruoc, denTruoc, null);
        long donHuy = thongKeService.demDonHuy(tu, den);

        // Bang doi chieu: 12 thang cua nam; top5: theo ky hien tai (thang) hoac ca nam (nam)
        SoSanhDTO soSanhThang = thongKeService.soSanh("thang", nam, null, null, null, null, null, null);
        SoSanhDTO topKy = laThang
            ? thongKeService.soSanh("2ky", null, tu, den, tu, den, null, null)
            : soSanhThang;

        long doanhThuHienTai = hienTai.doanhThu();
        long doanhThuTruoc = truoc.doanhThu();
        long chenhLech = doanhThuHienTai - doanhThuTruoc;
        int tangTruong = doanhThuTruoc <= 0
            ? (doanhThuHienTai > 0 ? 100 : 0)
            : (int) Math.round((doanhThuHienTai - doanhThuTruoc) * 100.0 / doanhThuTruoc);

        String tenKy = laThang ? ("Tháng " + thang + " Năm " + nam) : ("Năm " + nam);
        String tieuDe = "BÁO CÁO TỔNG KẾT & SO SÁNH DOANH THU — " + tenKy;
        String noiDung = noiDung(
            tenKy, hienTai, donHuy, tangTruong, chenhLech,
            topKy.topSanPham(), soSanhThang.ky(), nam,
            laThang ? thang : -1
        );

        boolean thanhCong = emailService.guiMailCustom(req.emailNhan().trim(), tieuDe, noiDung);
        return new GuiMailTongKetResponse(
            thanhCong,
            req.emailNhan().trim(),
            tieuDe,
            thanhCong ? null : "Nhà cung cấp mail từ chối gửi hoặc lỗi kết nối SMTP — báo cáo CHƯA được gửi"
        );
    }

    /* ---------------------------------- Noi dung ---------------------------------- */

    private String noiDung(
        String tenKy,
        TongQuanDTO hienTai,
        long donHuy,
        int tangTruong,
        long chenhLech,
        List<TopSanPhamKyDTO> top,
        List<KySoSanhDTO> cacKy,
        int nam,
        int thangHienTai
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("BÁO CÁO TỔNG KẾT & SO SÁNH DOANH THU\n");
        sb.append(tenKy).append(" — ").append(TEN_HE_THONG).append("\n\n");

        sb.append("TỔNG DOANH THU: ").append(so(hienTai.doanhThu())).append(" đ\n");
        sb.append("Tiền mặt: ").append(so(hienTai.tienMat()))
            .append(" đ | Chuyển khoản: ").append(so(hienTai.tienChuyenKhoan())).append(" đ\n\n");

        sb.append("TĂNG TRƯỞNG SO VỚI ")
            .append(thangHienTai > 0 ? "THÁNG" : "NĂM").append(" TRƯỚC: ")
            .append(dau(tangTruong)).append("%\n");
        sb.append("Chênh lệch: ").append(dau(chenhLech)).append(" đ\n\n");

        sb.append("ĐƠN HOÀN THÀNH: ").append(hienTai.soDon())
            .append(" | ĐƠN ĐÃ HỦY: ").append(donHuy)
            .append(" | AOV (TB ĐƠN): ").append(so(hienTai.aov())).append(" đ")
            .append(" | SẢN PHẨM BÁN: ").append(hienTai.sanPhamBan()).append("\n\n");

        sb.append("TOP 5 SẢN PHẨM BÁN CHẠY NHẤT ")
            .append(thangHienTai > 0 ? ("THÁNG " + thangHienTai) : ("NĂM " + nam)).append("\n");
        for (int i = 0; i < Math.min(5, top.size()); i++) {
            TopSanPhamKyDTO t = top.get(i);
            sb.append("#").append(i + 1).append(" ").append(ten(t))
                .append(" (").append(t.maSanPham()).append(")")
                .append(" — Đã bán: ").append(so(t.tongBan()))
                .append(" — Doanh thu: ").append(so(t.tongDoanhThu())).append(" đ\n");
        }
        if (top.isEmpty()) {
            sb.append("(Không có dữ liệu sản phẩm trong kỳ)\n");
        }
        sb.append("\n");

        sb.append("BẢNG ĐỐI CHIẾU DOANH THU CÁC THÁNG TRONG NĂM ").append(nam).append("\n");
        sb.append("Thời gian | Doanh thu | Số đơn | AOV | Tăng trưởng\n");
        for (int i = 0; i < cacKy.size(); i++) {
            KySoSanhDTO k = cacKy.get(i);
            boolean hienTaiKy = thangHienTai > 0 && i == thangHienTai - 1;
            sb.append(k.tenKy()).append(hienTaiKy ? " (Hiện tại)" : "")
                .append(" | ").append(so(k.doanhThu())).append(" đ")
                .append(" | ").append(k.soDon())
                .append(" | ").append(so(k.aov())).append(" đ")
                .append(" | ").append(dau(k.tangTruong())).append("%\n");
        }
        return sb.toString();
    }

    private static String ten(TopSanPhamKyDTO t) {
        return t.tenSanPham() == null || t.tenSanPham().isBlank() ? t.maSanPham() : t.tenSanPham();
    }

    private static String so(long n) {
        return String.format(new Locale("vi", "VN"), "%,d", n);
    }

    private static String dau(long n) {
        return (n > 0 ? "+" : "") + String.format(new Locale("vi", "VN"), "%,d", n);
    }

    /* ---------------------------------- Email mac dinh ---------------------------------- */

    /**
     * Email nhan bao cao mac dinh: nhan vien ADMIN dang hoat dong co email,
     * fallback sang spring.mail.username, cuoi cung la "".
     */
    public String emailMacDinh() {
        Optional<NhanVien> admin = nhanVienRepository
            .findFirstByVaiTro_MaVaiTroAndTrangThaiTrueAndEmailIsNotNullOrderByMaNhanVienAsc("ADMIN");
        if (admin.isPresent() && admin.get().getEmail() != null && !admin.get().getEmail().isBlank()) {
            return admin.get().getEmail().trim();
        }
        return mailUsername == null ? "" : mailUsername.trim();
    }
}
