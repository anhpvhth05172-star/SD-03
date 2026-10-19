package com.example.datn.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

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
     * Gửi email nội dung tùy chỉnh (tiêu đề + nội dung do quản trị viên soạn) tới một địa chỉ.
     *
     * @return true nếu nhà cung cấp mail xác nhận đã tiếp nhận email; false nếu gửi thất bại.
     */
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
}
