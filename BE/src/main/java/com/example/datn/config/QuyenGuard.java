package com.example.datn.config;

import com.example.datn.exception.KhongCoQuyenException;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra quyền dựa trên header vai trò do FE gửi lên.
 * Header mặc định: X-Vai-Tro (ADMIN | STAFF).
 * Không gửi header -> mặc định là ADMIN để không chặn các client cũ.
 */
@Component
public class QuyenGuard {

    public static final String HEADER_VAI_TRO = "X-Vai-Tro";

    public void canAdmin(String vaiTroHeader) {
        String vaiTro = chuanHoa(vaiTroHeader);
        if (!"ADMIN".equals(vaiTro)) {
            throw new KhongCoQuyenException("Bạn không có quyền thực hiện thao tác này (cần quyền ADMIN)");
        }
    }

    public void canQuanLyNhanVien(String vaiTroHeader) {
        canAdmin(vaiTroHeader);
    }

    public void canQuanLyKhachHang(String vaiTroHeader) {
        String vaiTro = chuanHoa(vaiTroHeader);
        if (!"ADMIN".equals(vaiTro) && !"STAFF".equals(vaiTro)) {
            throw new KhongCoQuyenException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    private static String chuanHoa(String header) {
        if (header == null || header.isBlank()) {
            return "ADMIN";
        }
        return header.trim().toUpperCase();
    }
}
