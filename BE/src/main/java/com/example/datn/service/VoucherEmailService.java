package com.example.datn.service;

import com.example.datn.entity.PhieuGiamGia;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VoucherEmailService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MONEY = NumberFormat.getInstance(new Locale("vi", "VN"));

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendVoucher(String to, String tenKhachHang, PhieuGiamGia p) throws Exception {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
        h.setFrom(from, "PolyShoes");
        h.setTo(to);
        h.setSubject("Poly Shoes gửi tặng bạn phiếu giảm giá " + p.getMaPhieuGiamGia());
        h.setText(buildHtml(tenKhachHang, p), true);
        mailSender.send(msg);
    }

    private String buildHtml(String tenKh, PhieuGiamGia p) {
        boolean percent = "PERCENT".equals(p.getLoaiGiamGia());

        String giaTri = percent
                ? p.getGiaTriGiam() + "%"
                : money(p.getGiaTriGiam());

        String giamToiDa = (percent && p.getGiamToiDa() != null)
                ? row("Giảm tối đa", money(p.getGiamToiDa()), false)
                : "";

        return TEMPLATE
                .replace("{{ten}}", esc(tenKh))
                .replace("{{tenPhieu}}", esc(p.getTenPhieuGiamGia()))
                .replace("{{ma}}", esc(p.getMaPhieuGiamGia()))
                .replace("{{giaTri}}", giaTri)
                .replace("{{giamToiDa}}", giamToiDa)
                .replace("{{donToiThieu}}", money(p.getHoaDonToiThieu()))
                .replace("{{batDau}}", p.getNgayBatDau().format(DT))
                .replace("{{ketThuc}}", p.getNgayKetThuc().format(DT));
    }

    private static String row(String label, String value, boolean highlight) {
        String color = highlight ? "#1e9bc4" : "#111827";
        return "<p style=\"margin:0 0 14px;\"><b>" + label + ":</b> "
                + "<span style=\"color:" + color + ";\">" + value + "</span></p>";
    }

    private static String money(Number n) {
        return n == null ? "0đ" : MONEY.format(n) + "đ";
    }

    private static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }

    private static final String TEMPLATE = """
        <div style="background:#f3f5f9;padding:32px 16px;font-family:Arial,Helvetica,sans-serif;">
          <div style="max-width:620px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;">

            <div style="background:#0f1d36;padding:36px 24px;text-align:center;">
              <div style="font-size:36px;font-weight:800;letter-spacing:1px;color:#ffffff;">
                SMASH<span style="color:#4cc3ee;">STEP</span>
              </div>
              <div style="margin-top:14px;font-size:13px;letter-spacing:4px;color:#ffffff;">
                POLY SHOES
              </div>
            </div>

            <div style="padding:32px 28px;color:#374151;font-size:15px;line-height:1.7;">
              <h2 style="margin:0 0 20px;text-align:center;font-size:24px;color:#0f1d36;">
                Chào {{ten}},
              </h2>
              <p style="margin:0 0 24px;">
                Cảm ơn bạn đã đồng hành cùng <b style="color:#1e9bc4;">SmashStep</b>.
                Chúng tôi gửi tặng bạn một ưu đãi đặc biệt!
              </p>

              <div style="background:#f3faff;border:1px solid #dbeefa;border-left:5px solid #4cc3ee;
                          border-radius:10px;padding:24px 28px;">
                <p style="margin:0 0 14px;">Voucher: <b>{{tenPhieu}}</b></p>
                <p style="margin:0 0 14px;"><b>Mã sử dụng:</b>
                  <span style="display:inline-block;margin-left:6px;padding:8px 14px;background:#dff2fb;
                               border-radius:6px;font-weight:700;font-size:17px;letter-spacing:1px;color:#0f3d55;">
                    {{ma}}
                  </span>
                </p>
                <p style="margin:0 0 14px;"><b>Giá trị giảm:</b>
                  <b style="color:#1e9bc4;">{{giaTri}}</b>
                </p>
                {{giamToiDa}}
                <p style="margin:0 0 14px;"><b>Áp dụng cho đơn từ:</b> {{donToiThieu}}</p>
                <p style="margin:0;"><b>Thời gian áp dụng:</b> {{batDau}} đến {{ketThuc}}</p>
              </div>
            </div>

          </div>
        </div>
        """;
}