package com.example.datn.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
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

    @Value("${spring.mail.username:lehuynhduc150303@gmail.com}")
    private String fromEmail;

    @Async
    public void guiMailDangKyThanhCong(
            String toEmail,
            String tenKhachHang,
            String maKhachHang,
            String tenTaiKhoan,
            String matKhau
    ) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không thể gửi email: Địa chỉ email của khách hàng trống");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PolyShoes Shop");
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
            log.info("Đã gửi email thông tin tài khoản thành công đến khách hàng: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đến {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void guiMailNhanVienMoi(
            String toEmail,
            String tenNhanVien,
            String maNhanVien,
            String tenTaiKhoan,
            String matKhau,
            String vaiTro
    ) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Không thể gửi email: Địa chỉ email của nhân viên trống");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PolyShoes System");
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
            log.info("Đã gửi email thông tin tài khoản thành công đến nhân viên: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email đến nhân viên {}: {}", toEmail, e.getMessage());
        }
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
                      <span class="info-value">%s</span>
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
            """.formatted(tenNhanVien, maNhanVien, vaiTro, tenTaiKhoan, matKhau);
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
                      <span class="info-value">%s</span>
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
            """.formatted(tenKhachHang, maKhachHang, tenTaiKhoan, matKhau);
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
    /**
     * Gui email xac nhan dat hang thanh cong (VNPay hoac COD).
     */
    @Async
    public void guiMailDatHangThanhCong(
            String toEmail,
            String tenKhachHang,
            String maHoaDon,
            String soTien,
            String phuongThucThanhToan,
            String diaChiNhanHang
    ) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Khong the gui email dat hang: email trong");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "PolyShoes Shop");
            helper.setTo(toEmail.trim());
            helper.setSubject("[PolyShoes] Dat hang thanh cong - " + maHoaDon);
            String html = buildOrderSuccessTemplate(
                tenKhachHang != null ? tenKhachHang : "Quy khach",
                maHoaDon != null ? maHoaDon : "",
                soTien != null ? soTien : "0",
                phuongThucThanhToan != null ? phuongThucThanhToan : "",
                diaChiNhanHang != null ? diaChiNhanHang : ""
            );
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Da gui email dat hang {} den {}", maHoaDon, toEmail);
        } catch (Exception e) {
            log.error("Loi gui email dat hang den {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildOrderSuccessTemplate(
            String tenKhachHang,
            String maHoaDon,
            String soTien,
            String phuongThucThanhToan,
            String diaChiNhanHang
    ) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f4f6f8; margin: 0; padding: 20px; color: #333; }
                .email-container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); border: 1px solid #e1e8ed; }
                .email-header { background: linear-gradient(135deg, #059669, #10b981); color: #ffffff; padding: 28px 24px; text-align: center; }
                .email-header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .email-header p { margin: 8px 0 0; opacity: 0.9; font-size: 14px; }
                .email-body { padding: 32px 28px; line-height: 1.6; }
                .greeting { font-size: 16px; margin-bottom: 16px; }
                .success-badge { display: inline-block; background: #d1fae5; color: #065f46; padding: 6px 16px; border-radius: 20px; font-weight: 700; font-size: 14px; margin-bottom: 20px; }
                .card-info { background: #f0fdf4; border-left: 4px solid #10b981; border-radius: 8px; padding: 18px 20px; margin: 24px 0; }
                .info-row { display: flex; margin-bottom: 10px; font-size: 14.5px; }
                .info-label { font-weight: 600; width: 180px; color: #475569; }
                .info-value { font-weight: 700; color: #0f172a; }
                .amount-highlight { color: #059669; font-size: 18px; }
                .email-footer { background: #f8fafc; padding: 20px 24px; text-align: center; font-size: 12.5px; color: #64748b; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="email-container">
                <div class="email-header">
                  <h1>&#x2705; Don hang da dat thanh cong!</h1>
                  <p>Cam on quy khach da tin tuong mua sam tai PolyShoes</p>
                </div>
                <div class="email-body">
                  <div class="greeting">Xin chao <b>%s</b>,</div>
                  <p>Don hang cua quy khach da duoc dat thanh cong. Chung toi se xu ly va giao hang som nhat co the. Duoi day la thong tin chi tiet don hang:</p>

                  <div class="card-info">
                    <div class="info-row">
                      <span class="info-label">Ma don hang:</span>
                      <span class="info-value">%s</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">Tong thanh toan:</span>
                      <span class="info-value amount-highlight">%s VND</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">Phuong thuc thanh toan:</span>
                      <span class="info-value">%s</span>
                    </div>
                    <div class="info-row" style="margin-bottom: 0;">
                      <span class="info-label">Dia chi nhan hang:</span>
                      <span class="info-value">%s</span>
                    </div>
                  </div>

                  <p>Neu quy khach co bat ky thac mac nao, vui long lien he hotline hoac phan hoi email nay.</p>
                  <p style="margin-top: 24px;">Tran trong,<br/><b>Doi ngu PolyShoes</b></p>
                </div>
                <div class="email-footer">
                  <p>&copy; 2026 PolyShoes. Moi quyen duoc bao luu.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(tenKhachHang, maHoaDon, soTien, phuongThucThanhToan, diaChiNhanHang);
    }
}