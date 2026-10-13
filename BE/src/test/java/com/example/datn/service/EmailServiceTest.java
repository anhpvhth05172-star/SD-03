package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class EmailServiceTest {

    private static final String EMAIL = "kh@example.com";

    private static EmailService service(JavaMailSender sender) {
        EmailService emailService = new EmailService(sender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@polyshoes.example");
        return emailService;
    }

    private static JavaMailSender mailSender() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        return sender;
    }

    @Test
    void traVeTrueChiKhiNhaCungCapChapNhan() {
        JavaMailSender sender = mailSender();
        EmailService service = service(sender);

        assertTrue(service.guiMailDangKyThanhCong(
            EMAIL, "Nguyễn Thị Bình", "Binhnt", "binh@example.com", "123456"));
        verify(sender).send(any(MimeMessage.class));
    }

    @Test
    void traVeFalseKhiSmtpTuChoiXacThuc() {
        JavaMailSender sender = mailSender();
        doThrow(new MailSendException("535 5.7.8 Error: authentication failed"))
            .when(sender).send(any(MimeMessage.class));
        EmailService service = service(sender);

        assertFalse(service.guiMailDangKyThanhCong(
            EMAIL, "Nguyễn Thị Bình", "Binhnt", "binh@example.com", "123456"));
    }

    @Test
    void traVeFalseKhiKhongKetNoiDuocMayChuSmtp() {
        JavaMailSender sender = mailSender();
        doThrow(new MailSendException("Mail server connection failed"))
            .when(sender).send(any(MimeMessage.class));
        EmailService service = service(sender);

        assertFalse(service.guiMailNhanVienMoi(
            EMAIL, "Nguyễn Thị Bình", "Binhnt", "binh@example.com", "123456", "Nhân viên"));
    }

    @Test
    void traVeFalseVaKhongGuiGiKhiThieuDiaChiNhan() {
        JavaMailSender sender = mock(JavaMailSender.class);
        EmailService service = service(sender);

        assertFalse(service.guiMailDangKyThanhCong(
            "   ", "Nguyễn Thị Bình", "Binhnt", "binh@example.com", "123456"));
        assertFalse(service.guiMailDangKyThanhCong(
            null, "Nguyễn Thị Bình", "Binhnt", "binh@example.com", "123456"));
        assertFalse(service.guiMailNhanVienMoi(
            null, "Nguyễn Thị Bình", "Binhnt", "binh@example.com", "123456", "Nhân viên"));

        verifyNoInteractions(sender);
    }

    @Test
    void traVeFalseVaKhongGuiGiKhiKhongCauHinhNguoiGui() {
        JavaMailSender sender = mailSender();
        EmailService emailService = new EmailService(sender);
        // Không đặt spring.mail.from và sender không phải JavaMailSenderImpl -> không có người gửi.
        ReflectionTestUtils.setField(emailService, "fromEmail", "  ");

        assertFalse(emailService.guiMailNhanVienMoi(
            EMAIL, "Nguyễn Thị Bình", "NV001", "binh@example.com", "123456", "Quản trị viên"));

        verifyNoInteractions(sender);
    }
}
