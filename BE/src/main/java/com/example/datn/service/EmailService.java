package com.example.datn.service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    /** Loại thông báo phiếu giảm giá cá nhân */
    public enum LoaiThongBao { MOI, CAP_NHAT, BAT, TAT }

    private final JavaMailSender mailSender;

    /** Địa chỉ người gửi lấy từ cấu hình (không ghi cứng tài khoản trong source). */
    @Value("${spring.mail.from:${spring.mail.username:}}")
    private String fromEmail;

    /**
     * Địa chỉ người gửi: ưu tiên {@code spring.mail.from}, nếu trống thì dùng tài khoản SMTP
     * đã cấu hình. Trả về {@code null} khi không có cấu hình nào (không đặt "From" rỗng vì
     * nhà cung cấp sẽ từ chối và người dùng tưởng đã gửi thành công).
     */
    private String diaChiNguoiGui() {
        if (fromEmail != null && !fromEmail.isBlank()) {
            return fromEmail.trim();
        }
        if (mailSender instanceof JavaMailSenderImpl impl
            && impl.getUsername() != null && !impl.getUsername().isBlank()) {
            return impl.getUsername().trim();
        }
        return null;
    }

    /**
     * Gửi email chào mừng khách hàng mới.
     *
     * @return true nếu nhà cung cấp mail xác nhận đã tiếp nhận email; false nếu gửi thất bại
     * (lưu ý: xác nhận của nhà cung cấp không đảm bảo email đã vào hộp thư đến).
     */
    public boolean guiMailDangKyThanhCong(
        String toEmail,
        String tenKhachHang,
        String maKhachHang,
        String tenTaiKhoan,
        String matKhau
    ) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không thể gửi email: Địa chỉ email của khách hàng trống");
            return false;
        }

        String tu = diaChiNguoiGui();
        if (tu == null) {
            log.error(
                "Không gửi email thông tin tài khoản tới {}: chưa cấu hình người gửi (spring.mail.from / MAIL_USERNAME)",
                toEmail
            );
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(tu, "PolyShoes Shop");
            helper.setTo(toEmail.trim());
            helper.setSubject("[PolyShoes] Chúc mừng đăng ký tài khoản thành công");

            String htmlContent = buildCustomerWelcomeTemplate(
                    tenKhachHang != null ? tenKhachHang.trim() : "Quý khách",
                    maKhachHang != null ? maKhachHang.trim() : "",
                    tenTaiKhoan != null ? tenTaiKhoan.trim() : toEmail.trim(),
                    matKhau != null ? matKhau : "123456"
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Nhà cung cấp mail đã chấp nhận email thông tin tài khoản gửi tới {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi email thông tin tài khoản tới {} thất bại: {}", toEmail, e.toString(), e);
            return false;
        }
    }

    /**
     * Gửi email cấp tài khoản cho nhân viên mới.
     *
     * @return true nếu nhà cung cấp mail xác nhận đã tiếp nhận email; false nếu gửi thất bại.
     */
    public boolean guiMailNhanVienMoi(
        String toEmail,
        String tenNhanVien,
        String maNhanVien,
        String tenTaiKhoan,
        String matKhau,
        String vaiTro
    ) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không thể gửi email: Địa chỉ email của nhân viên trống");
            return false;
        }

        String tu = diaChiNguoiGui();
        if (tu == null) {
            log.error(
                "Không gửi email cấp tài khoản nhân viên tới {}: chưa cấu hình người gửi (spring.mail.from / MAIL_USERNAME)",
                toEmail
            );
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(tu, "PolyShoes System");
            helper.setTo(toEmail.trim());
            helper.setSubject("[PolyShoes] Thông tin tài khoản nhân viên mới");

            String htmlContent = buildEmployeeWelcomeTemplate(
                    tenNhanVien != null ? tenNhanVien.trim() : "Nhân viên",
                    maNhanVien != null ? maNhanVien.trim() : "",
                    tenTaiKhoan != null ? tenTaiKhoan.trim() : toEmail.trim(),
                    matKhau != null ? matKhau : "123456",
                    vaiTro != null ? vaiTro : "Nhân viên"
            );

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Nhà cung cấp mail đã chấp nhận email cấp tài khoản gửi tới {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi email cấp tài khoản nhân viên tới {} thất bại: {}", toEmail, e.toString(), e);
            return false;
        }
    }

    /**
     * Gửi email mã OTP đặt lại mật khẩu cho nhân viên.
     *
     * @return true nếu nhà cung cấp mail xác nhận đã tiếp nhận email; false nếu gửi thất bại.
     */
    public boolean guiMaQuenMatKhau(String toEmail, String maOtp) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không thể gửi email: Địa chỉ email trống");
            return false;
        }

        String tu = diaChiNguoiGui();
        if (tu == null) {
            log.error(
                "Không gửi email mã OTP tới {}: chưa cấu hình người gửi (spring.mail.from / MAIL_USERNAME)",
                toEmail
            );
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(tu, "PolyShoes System");
            helper.setTo(toEmail.trim());
            helper.setSubject("[PolyShoes] Mã xác nhận đặt lại mật khẩu");

            helper.setText(buildOtpTemplate(maOtp), true);

            mailSender.send(message);
            log.info("Nhà cung cấp mail đã chấp nhận email mã OTP gửi tới {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi email mã OTP tới {} thất bại: {}", toEmail, e.toString(), e);
            return false;
        }
    }

    private String buildOtpTemplate(String maOtp) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #333; }
                .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e1e8ed; }
                .email-header { background: linear-gradient(135deg, #e11d48, #f43f5e); color: #ffffff; padding: 28px 24px; text-align: center; }
                .email-header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .email-header p { margin: 8px 0 0; opacity: 0.9; font-size: 14px; }
                .email-body { padding: 32px 28px; line-height: 1.6; }
                .otp-box { background: #fff1f2; border-left: 4px solid #e11d48; border-radius: 8px; padding: 22px 24px; margin: 24px 0; text-align: center; }
                .otp-code { font-family: monospace; font-size: 34px; letter-spacing: 8px; font-weight: 700; color: #be123c; }
                .warning-box { background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 14px 16px; margin: 20px 0; font-size: 13.5px; color: #92400e; }
                .email-footer { background: #f8fafc; padding: 20px 24px; text-align: center; font-size: 12.5px; color: #64748b; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <h1>PolyShoes Management System</h1>
                  <p>Đặt lại mật khẩu tài khoản nhân viên</p>
                </div>
                <div class="email-body">
                  <p>Xin chào,</p>
                  <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản nhân viên của bạn. Mã xác nhận của bạn là:</p>
                  <div class="otp-box">
                    <div class="otp-code">%s</div>
                  </div>
                  <div class="warning-box">
                    ⏳ Mã có hiệu lực trong <b>10 phút</b>. Bạn có thể gửi lại mã sau 60 giây.
                  </div>
                  <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
                  <p style="margin-top: 24px;">Trân trọng,<br/><b>Ban Quản trị PolyShoes</b></p>
                </div>
                <div class="email-footer">
                  <p>© 2026 PolyShoes. Mọi quyền được bảo lưu.</p>
                  <p>Email này được gửi tự động, vui lòng không chia sẻ mã xác nhận với bất kỳ ai.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(maOtp);
    }

    /**
     * Gửi email nội dung tùy chỉnh (tiêu đề + nội dung do quản trị viên soạn) tới một địa chỉ.
     *
     * @return true nếu nhà cung cấp mail xác nhận đã tiếp nhận email; false nếu gửi thất bại.
     */
    public record ChiTietDongMail(
        String tenSanPham,
        String bienThe,
        int soLuong,
        String donGia,
        String thanhTien
    ) {}

    /**
     * Gửi lại email thông tin hóa đơn / đơn hàng cho khách hàng.
     */
    public boolean guiMailHoaDon(
        String toEmail,
        String tenKhachHang,
        String maHoaDon,
        String trangThai,
        String loaiDon,
        String diaChiNhanHang,
        String tongTienHang,
        String tienGiam,
        String phiVanChuyen,
        String tongThanhToan,
        String ngayTao,
        List<ChiTietDongMail> chiTietList
    ) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không thể gửi email hóa đơn: Địa chỉ email người nhận trống");
            return false;
        }

        String tu = diaChiNguoiGui();
        if (tu == null) {
            log.error(
                "Không gửi email hóa đơn tới {}: chưa cấu hình người gửi (spring.mail.from / MAIL_USERNAME)",
                toEmail
            );
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(tu, "PolyShoes Shop");
            helper.setTo(toEmail.trim());
            helper.setSubject("[PolyShoes] Thông tin chi tiết đơn hàng " + (maHoaDon != null ? maHoaDon : ""));

            String htmlContent = buildInvoiceEmailTemplate(
                tenKhachHang != null ? tenKhachHang.trim() : "Quý khách",
                maHoaDon != null ? maHoaDon.trim() : "—",
                trangThai != null ? trangThai : "Chờ xác nhận",
                loaiDon != null ? loaiDon : "Tại quầy",
                diaChiNhanHang != null ? diaChiNhanHang : "Nhận tại quầy",
                tongTienHang != null ? tongTienHang : "0 đ",
                tienGiam != null ? tienGiam : "0 đ",
                phiVanChuyen != null ? phiVanChuyen : "0 đ",
                tongThanhToan != null ? tongThanhToan : "0 đ",
                ngayTao != null ? ngayTao : "",
                chiTietList != null ? chiTietList : List.of()
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Đã gửi email hóa đơn {} tới {}", maHoaDon, toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi email hóa đơn tới {} thất bại: {}", toEmail, e.toString(), e);
            throw new RuntimeException("Gửi email thất bại: " + e.getMessage(), e);
        }
    }

    private String buildInvoiceEmailTemplate(
        String tenKhachHang,
        String maHoaDon,
        String trangThai,
        String loaiDon,
        String diaChiNhanHang,
        String tongTienHang,
        String tienGiam,
        String phiVanChuyen,
        String tongThanhToan,
        String ngayTao,
        List<ChiTietDongMail> chiTietList
    ) {
        StringBuilder itemsHtml = new StringBuilder();
        for (ChiTietDongMail item : chiTietList) {
            itemsHtml.append("""
                <tr>
                  <td style="padding: 10px 12px; border-bottom: 1px solid #f1f5f9; font-size: 13.5px;">
                    <div style="font-weight: 700; color: #0f172a;">%s</div>
                    <div style="font-size: 12px; color: #64748b;">%s</div>
                  </td>
                  <td style="padding: 10px 8px; border-bottom: 1px solid #f1f5f9; font-size: 13.5px; text-align: center; font-weight: 600;">%d</td>
                  <td style="padding: 10px 12px; border-bottom: 1px solid #f1f5f9; font-size: 13.5px; text-align: right; color: #334155;">%s</td>
                  <td style="padding: 10px 12px; border-bottom: 1px solid #f1f5f9; font-size: 13.5px; text-align: right; font-weight: 700; color: #0f172a;">%s</td>
                </tr>
            """.formatted(
                escapeHtml(item.tenSanPham()),
                escapeHtml(item.bienThe()),
                item.soLuong(),
                escapeHtml(item.donGia()),
                escapeHtml(item.thanhTien())
            ));
        }

        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8">
          <style>
            body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #333; }
            .email-container { max-width: 620px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e1e8ed; }
            .email-header { background: linear-gradient(135deg, #1e3a8a, #2563eb); color: #ffffff; padding: 26px 24px; text-align: center; }
            .email-header h1 { margin: 0; font-size: 24px; font-weight: 700; }
            .email-header p { margin: 6px 0 0; opacity: 0.9; font-size: 14px; }
            .email-body { padding: 28px 24px; line-height: 1.6; }
            .info-box { background: #f8fafc; border-radius: 8px; padding: 14px 18px; margin-bottom: 20px; border: 1px solid #e2e8f0; font-size: 13.5px; }
            .info-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
            .info-row:last-child { margin-bottom: 0; }
            .table-products { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
            .table-products th { background: #f1f5f9; padding: 8px 12px; font-size: 12px; text-transform: uppercase; color: #475569; font-weight: 700; border-bottom: 2px solid #cbd5e1; }
            .totals-box { border-top: 2px dashed #e2e8f0; padding-top: 14px; font-size: 14px; }
            .total-row { display: flex; justify-content: space-between; margin-bottom: 6px; color: #475569; }
            .grand-total { font-size: 16px; font-weight: 800; color: #d92d20; margin-top: 10px; padding-top: 8px; border-top: 1px solid #e2e8f0; }
            .email-footer { background: #f8fafc; padding: 18px 24px; text-align: center; font-size: 12.5px; color: #64748b; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="email-container">
            <div class="email-header">
              <h1>POLYSHOES STORE</h1>
              <p>Thông tin xác nhận đơn hàng <strong>#%s</strong></p>
            </div>
            <div class="email-body">
              <p>Kính gửi <strong>%s</strong>,</p>
              <p>PolyShoes xin gửi thông tin chi tiết đơn hàng của Quý khách:</p>

              <div class="info-box">
                <div class="info-row"><span>Mã đơn hàng:</span><strong style="color: #1e3a8a;">#%s</strong></div>
                <div class="info-row"><span>Thời gian đặt:</span><span>%s</span></div>
                <div class="info-row"><span>Trạng thái:</span><strong style="color: #2563eb;">%s</strong></div>
                <div class="info-row"><span>Hình thức:</span><span>%s</span></div>
                <div class="info-row"><span>Địa chỉ nhận:</span><span>%s</span></div>
              </div>

              <table class="table-products">
                <thead>
                  <tr>
                    <th style="text-align: left;">Sản phẩm</th>
                    <th style="text-align: center; width: 40px;">SL</th>
                    <th style="text-align: right; width: 100px;">Đơn giá</th>
                    <th style="text-align: right; width: 110px;">Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  %s
                </tbody>
              </table>

              <div class="totals-box">
                <div class="total-row"><span>Tiền hàng:</span><strong>%s</strong></div>
                <div class="total-row"><span>Giảm giá:</span><strong style="color: #16a34a;">- %s</strong></div>
                <div class="total-row"><span>Phí vận chuyển:</span><strong>+ %s</strong></div>
                <div class="total-row grand-total"><span>TỔNG THANH TOÁN:</span><span>%s</span></div>
              </div>

              <p style="margin-top: 24px; font-size: 13.5px; color: #475569;">
                Mọi thắc mắc hoặc cần hỗ trợ, Quý khách vui lòng liên hệ hotline <strong>0987.654.321</strong>.
              </p>
            </div>
            <div class="email-footer">
              <p>&copy; 2026 PolyShoes Store. Cảm ơn Quý khách đã mua sắm cùng chúng tôi!</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(
            escapeHtml(maHoaDon),
            escapeHtml(tenKhachHang),
            escapeHtml(maHoaDon),
            escapeHtml(ngayTao),
            escapeHtml(trangThai),
            escapeHtml(loaiDon),
            escapeHtml(diaChiNhanHang),
            itemsHtml.toString(),
            escapeHtml(tongTienHang),
            escapeHtml(tienGiam),
            escapeHtml(phiVanChuyen),
            escapeHtml(tongThanhToan)
        );
    }

    public boolean guiMailCustom(String toEmail, String tieuDe, String noiDung) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không thể gửi email: địa chỉ người nhận trống");
            return false;
        }
        if (tieuDe == null || tieuDe.trim().isEmpty() || noiDung == null || noiDung.trim().isEmpty()) {
            log.warn("Không thể gửi email: tiêu đề hoặc nội dung trống");
            return false;
        }

        String tu = diaChiNguoiGui();
        if (tu == null) {
            log.error(
                "Không gửi email tới {}: chưa cấu hình người gửi (spring.mail.from / MAIL_USERNAME)",
                toEmail
            );
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(tu, "PolyShoes Shop");
            helper.setTo(toEmail.trim());
            helper.setSubject(tieuDe.trim());
            helper.setText(buildCustomTemplate(noiDung.trim()), true);

            mailSender.send(message);
            log.info("Nhà cung cấp mail đã chấp nhận email hàng loạt gửi tới {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Gửi email hàng loạt tới {} thất bại: {}", toEmail, e.toString(), e);
            return false;
        }
    }

    private static String escapeHtml(String s) {
        return s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String buildCustomTemplate(String noiDung) {
        String noiDungHtml = escapeHtml(noiDung).replace("\r\n", "\n").replace("\n", "<br/>");
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #333; }
                .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e1e8ed; }
                .email-header { background: linear-gradient(135deg, #e11d48, #f43f5e); color: #ffffff; padding: 24px; text-align: center; }
                .email-header h1 { margin: 0; font-size: 22px; font-weight: 700; }
                .email-body { padding: 28px 26px; line-height: 1.65; font-size: 15px; }
                .email-footer { background: #f8fafc; padding: 16px 24px; text-align: center; font-size: 12.5px; color: #64748b; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <h1>PolyShoes Shop</h1>
                </div>
                <div class="email-body">%s</div>
                <div class="email-footer">
                  <p>&copy; 2026 PolyShoes. Mọi quyền được bảo lưu.</p>
                  <p>Email này được gửi tự động từ hệ thống quản trị PolyShoes.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(noiDungHtml);
    }


    private String buildEmployeeWelcomeTemplate(
            String tenNhanVien,
            String maNhanVien,
            String tenTaiKhoan,
            String matKhau,
            String vaiTro
    ) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #333; }
                .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e1e8ed; }
                .email-header { background: linear-gradient(135deg, #e11d48, #f43f5e); color: #ffffff; padding: 28px 24px; text-align: center; }
                .email-header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; }
                .email-header p { margin: 8px 0 0; opacity: 0.9; font-size: 14px; }
                .email-body { padding: 32px 28px; line-height: 1.6; }
                .greeting { font-size: 16px; margin-bottom: 16px; }
                .card-info { background: #fff1f2; border-left: 4px solid #e11d48; border-radius: 8px; padding: 18px 20px; margin: 24px 0; }
                .info-row { display: flex; margin-bottom: 10px; font-size: 14.5px; }
                .info-label { font-weight: 600; width: 150px; color: #475569; }
                .info-value { font-weight: 700; color: #0f172a; }
                .password-badge { display: inline-block; background: #ffe4e6; color: #be123c; padding: 4px 10px; border-radius: 6px; font-family: monospace; font-size: 15px; letter-spacing: 1px; }
                .warning-box { background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 14px 16px; margin: 20px 0; font-size: 13.5px; color: #92400e; }
                .email-footer { background: #f8fafc; padding: 20px 24px; text-align: center; font-size: 12.5px; color: #64748b; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <h1>PolyShoes Management System</h1>
                  <p>Hệ thống Quản trị & Bán hàng PolyShoes</p>
                </div>
                <div class="email-body">
                  <div class="greeting">Xin chào <b>%s</b>,</div>
                  <p>Chào mừng bạn đã gia nhập đội ngũ <b>PolyShoes</b>. Tài khoản nhân viên của bạn đã được khởi tạo thành công với thông tin dưới đây:</p>
                  
                  <div class="card-info">
                    <div class="info-row">
                      <span class="info-label">Mã nhân viên:</span>
                      <span class="info-value">%s</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">Vai trò:</span>
                      <span class="info-value">%s</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">Tài khoản / Email:</span>
                      <span class="info-value">
                        <a href="mailto:%s" style="color:#2563eb; text-decoration:none; font-weight:700;">%s</a>
                      </span>
                    </div>
                    <div class="info-row" style="margin-bottom: 0;">
                      <span class="info-label">Mật khẩu khởi tạo:</span>
                      <span class="password-badge">%s</span>
                    </div>
                  </div>

                  <div class="warning-box">
                    💡 <b>Lưu ý bảo mật:</b> Vui lòng đăng nhập và đổi mật khẩu cá nhân ngay trong lần đầu tiên để đảm bảo tính an toàn cho hệ thống.
                  </div>

                  <p>Chúc bạn có trải nghiệm làm việc hiệu quả và thành công cùng PolyShoes!</p>
                  <p style="margin-top: 24px;">Trân trọng,<br/><b>Ban Quản trị PolyShoes</b></p>
                </div>
                <div class="email-footer">
                  <p>© 2026 PolyShoes. Mọi quyền được bảo lưu.</p>
                  <p>Email này được gửi tự động, vui lòng không chia sẻ thông tin mật khẩu với bất kỳ ai.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(tenNhanVien, maNhanVien, vaiTro, tenTaiKhoan, tenTaiKhoan, matKhau);
    }

    private String buildCustomerWelcomeTemplate(
            String tenKhachHang,
            String maKhachHang,
            String tenTaiKhoan,
            String matKhau
    ) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #333; }
                .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e1e8ed; }
                .email-header { background: linear-gradient(135deg, #1e3a8a, #3b82f6); color: #ffffff; padding: 28px 24px; text-align: center; }
                .email-header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px; }
                .email-header p { margin: 8px 0 0; opacity: 0.9; font-size: 14px; }
                .email-body { padding: 32px 28px; line-height: 1.6; }
                .greeting { font-size: 16px; margin-bottom: 16px; }
                .card-info { background: #f8fafc; border-left: 4px solid #3b82f6; border-radius: 8px; padding: 18px 20px; margin: 24px 0; }
                .info-row { display: flex; margin-bottom: 10px; font-size: 14.5px; }
                .info-label { font-weight: 600; width: 140px; color: #475569; }
                .info-value { font-weight: 700; color: #0f172a; }
                .password-badge { display: inline-block; background: #dbeafe; color: #1e40af; padding: 4px 10px; border-radius: 6px; font-family: monospace; font-size: 15px; letter-spacing: 1px; }
                .warning-box { background: #fffbeb; border: 1px solid #fde68a; border-radius: 8px; padding: 14px 16px; margin: 20px 0; font-size: 13.5px; color: #92400e; }
                .email-footer { background: #f8fafc; padding: 20px 24px; text-align: center; font-size: 12.5px; color: #64748b; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <h1>PolyShoes Store</h1>
                  <p>Hệ thống cửa hàng giày thể thao chính hãng</p>
                </div>
                <div class="email-body">
                  <div class="greeting">Xin chào <b>%s</b>,</div>
                  <p>Chúc mừng quý khách đã đăng ký tài khoản thành công tại <b>PolyShoes</b>. Dưới đây là thông tin tài khoản đăng nhập của quý khách:</p>
                  
                  <div class="card-info">
                    <div class="info-row">
                      <span class="info-label">Mã khách hàng:</span>
                      <span class="info-value">%s</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">Tên tài khoản / Email:</span>
                      <span class="info-value">
                        <a href="mailto:%s" style="color:#1d4ed8; text-decoration:none; font-weight:700;">%s</a>
                      </span>
                    </div>
                    <div class="info-row" style="margin-bottom: 0;">
                      <span class="info-label">Mật khẩu đăng nhập:</span>
                      <span class="password-badge">%s</span>
                    </div>
                  </div>

                  <div class="warning-box">
                    💡 <b>Lưu ý bảo mật:</b> Vì lý do an toàn, vui lòng đổi mật khẩu sau khi đăng nhập lần đầu tiên để bảo vệ thông tin tài khoản của quý khách.
                  </div>

                  <p>Nếu quý khách có bất kỳ câu hỏi nào hoặc cần hỗ trợ, xin vui lòng phản hồi email này hoặc liên hệ hotline chăm sóc khách hàng của chúng tôi.</p>
                  <p style="margin-top: 24px;">Trân trọng,<br/><b>Đội ngũ PolyShoes</b></p>
                </div>
                <div class="email-footer">
                  <p>© 2026 PolyShoes. Mọi quyền được bảo lưu.</p>
                  <p>Email này được gửi tự động, vui lòng không chia sẻ thông tin mật khẩu với bất kỳ ai.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(tenKhachHang, maKhachHang, tenTaiKhoan, tenTaiKhoan, matKhau);
    }

    // ======================= PHIẾU GIẢM GIÁ CÁ NHÂN =======================

    @Async
    public void guiMailPhieuGiamGiaCaNhan(
            String toEmail,
            String tenKhachHang,
            String maPhieu,
            String tenPhieu,
            String giaTriGiam,      // ví dụ: "10%" hoặc "50.000đ"
            String dieuKienApDung,  // ví dụ: "Đơn hàng từ 500.000đ, giảm tối đa 100.000đ"
            String ngayBatDau,      // ví dụ: "13/10/2026 08:00"
            String ngayKetThuc,
            LoaiThongBao loai
    ) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không thể gửi email phiếu giảm giá: Địa chỉ email của khách hàng trống");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = switch (loai) {
                case MOI      -> "[PolyShoes] Bạn nhận được phiếu giảm giá dành riêng cho bạn";
                case CAP_NHAT -> "[PolyShoes] Phiếu giảm giá của bạn đã được cập nhật";
                case BAT      -> "[PolyShoes] Phiếu giảm giá của bạn đã được kích hoạt";
                case TAT      -> "[PolyShoes] Phiếu giảm giá của bạn tạm ngưng áp dụng";
            };

            helper.setFrom(fromEmail, "PolyShoes Shop");
            helper.setTo(toEmail.trim());
            helper.setSubject(subject);

            String htmlContent = buildCouponTemplate(
                    loai,
                    tenKhachHang != null ? tenKhachHang.trim() : "Quý khách",
                    maPhieu != null ? maPhieu.trim() : "",
                    tenPhieu != null ? tenPhieu.trim() : "",
                    giaTriGiam != null ? giaTriGiam : "",
                    dieuKienApDung != null ? dieuKienApDung : "Không có",
                    ngayBatDau != null ? ngayBatDau : "",
                    ngayKetThuc != null ? ngayKetThuc : ""
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Đã gửi email [{}] phiếu giảm giá {} đến khách hàng: {}", loai, maPhieu, toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email phiếu giảm giá đến {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildCouponTemplate(
            LoaiThongBao loai,
            String tenKhachHang,
            String maPhieu,
            String tenPhieu,
            String giaTriGiam,
            String dieuKienApDung,
            String ngayBatDau,
            String ngayKetThuc
    ) {
        String c1, c2, title, sub, intro;
        switch (loai) {
            case MOI -> {
                c1 = "#059669"; c2 = "#10b981";
                title = "🎁 Quà tặng từ PolyShoes";
                sub = "Phiếu giảm giá dành riêng cho bạn";
                intro = "PolyShoes gửi tặng bạn một phiếu giảm giá cá nhân. Hãy dùng mã dưới đây khi thanh toán nhé:";
            }
            case CAP_NHAT -> {
                c1 = "#2563eb"; c2 = "#3b82f6";
                title = "🔔 Phiếu giảm giá đã được cập nhật";
                sub = "Thông tin mới nhất về phiếu của bạn";
                intro = "Phiếu giảm giá của bạn vừa được thay đổi thông tin. Vui lòng xem chi tiết mới bên dưới:";
            }
            case BAT -> {
                c1 = "#059669"; c2 = "#10b981";
                title = "✅ Phiếu giảm giá đã được kích hoạt";
                sub = "Bạn có thể sử dụng ngay";
                intro = "Phiếu giảm giá của bạn đã được bật trở lại và sẵn sàng sử dụng:";
            }
            default -> {
                c1 = "#dc2626"; c2 = "#ef4444";
                title = "⏸️ Phiếu giảm giá tạm ngưng";
                sub = "Phiếu hiện không thể sử dụng";
                intro = "Phiếu giảm giá dưới đây tạm thời bị tắt, bạn chưa thể dùng cho đến khi được kích hoạt lại:";
            }
        }

        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
          <meta charset="UTF-8">
          <style>
            body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #333; }
            .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e1e8ed; }
            .email-header { background: linear-gradient(135deg, %s, %s); color: #ffffff; padding: 28px 24px; text-align: center; }
            .email-header h1 { margin: 0; font-size: 24px; font-weight: 700; }
            .email-header p { margin: 8px 0 0; opacity: 0.9; font-size: 14px; }
            .email-body { padding: 32px 28px; line-height: 1.6; }
            .greeting { font-size: 16px; margin-bottom: 16px; }
            .coupon-box { border: 2px dashed #10b981; background: #ecfdf5; border-radius: 12px; padding: 20px; margin: 24px 0; text-align: center; }
            .coupon-value { font-size: 32px; font-weight: 800; color: #047857; margin: 4px 0; }
            .coupon-code { display: inline-block; background: #d1fae5; color: #065f46; padding: 8px 18px; border-radius: 8px; font-family: monospace; font-size: 20px; font-weight: 700; letter-spacing: 2px; margin-top: 8px; }
            .card-info { background: #f8fafc; border-left: 4px solid #10b981; border-radius: 8px; padding: 16px 20px; margin: 20px 0; }
            .info-row { display: flex; margin-bottom: 8px; font-size: 14.5px; }
            .info-label { font-weight: 600; width: 130px; color: #475569; }
            .info-value { font-weight: 700; color: #0f172a; }
            .email-footer { background: #f8fafc; padding: 20px 24px; text-align: center; font-size: 12.5px; color: #64748b; border-top: 1px solid #e2e8f0; }
          </style>
        </head>
        <body>
          <div class="email-container">
            <div class="email-header">
              <h1>%s</h1>
              <p>%s</p>
            </div>
            <div class="email-body">
              <div class="greeting">Xin chào <b>%s</b>,</div>
              <p>%s</p>

              <div class="coupon-box">
                <div>%s</div>
                <div class="coupon-value">Giảm %s</div>
                <div class="coupon-code">%s</div>
              </div>

              <div class="card-info">
                <div class="info-row">
                  <span class="info-label">Điều kiện:</span>
                  <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Hiệu lực từ:</span>
                  <span class="info-value">%s</span>
                </div>
                <div class="info-row" style="margin-bottom: 0;">
                  <span class="info-label">Hết hạn:</span>
                  <span class="info-value">%s</span>
                </div>
              </div>

              <p>Phiếu giảm giá chỉ áp dụng cho tài khoản của bạn. Chúc bạn mua sắm vui vẻ!</p>
              <p style="margin-top: 24px;">Trân trọng,<br/><b>Đội ngũ PolyShoes</b></p>
            </div>
            <div class="email-footer">
              <p>© 2026 PolyShoes. Mọi quyền được bảo lưu.</p>
              <p>Email này được gửi tự động, vui lòng không chia sẻ mã giảm giá cho người khác.</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(
                c1, c2,
                title, sub,
                tenKhachHang, intro,
                tenPhieu, giaTriGiam, maPhieu,
                dieuKienApDung, ngayBatDau, ngayKetThuc);
    }
}
