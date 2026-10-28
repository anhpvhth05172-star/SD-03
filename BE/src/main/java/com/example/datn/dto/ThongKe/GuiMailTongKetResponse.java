package com.example.datn.dto.ThongKe;

/**
 * Ket qua gui mail tong ket — trung ket qua that cua guiMailCustom.
 * thanhCong=false khi mailer loi; lyDo giai thich nguyen nhan.
 */
public record GuiMailTongKetResponse(
    boolean thanhCong,
    String emailNhan,
    String tieuDe,
    String lyDo
) {}
