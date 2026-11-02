package com.example.datn.config;

import com.example.datn.exception.KhongCoQuyenException;
import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Kiểm tra quyền dựa trên JWT đã xác thực (SecurityContext).
 * Nhân viên: ROLE_NHAN_VIEN + ROLE_ADMIN/ROLE_STAFF theo vai trò.
 * Khách hàng: ROLE_KHACH_HANG (+ ROLE_ADMIN nếu vai_tro = ADMIN).
 */
@Component
public class QuyenGuard {

    public void canAdmin() {
        if (!coQuyen("ROLE_ADMIN")) {
            throw new KhongCoQuyenException("Bạn không có quyền thực hiện thao tác này (cần quyền ADMIN)");
        }
    }

    public void canQuanLyNhanVien() {
        canAdmin();
    }

    public void canQuanLyKhachHang() {
        if (!coQuyen("ROLE_ADMIN") && !coQuyen("ROLE_STAFF") && !coQuyen("ROLE_NHAN_VIEN")) {
            throw new KhongCoQuyenException("Bạn không có quyền thực hiện thao tác này");
        }
    }

    public void canXemThongKe() {
        if (!coQuyen("ROLE_ADMIN") && !coQuyen("ROLE_STAFF") && !coQuyen("ROLE_NHAN_VIEN")) {
            throw new KhongCoQuyenException("Bạn không có quyền xem thống kê");
        }
    }

    private static boolean coQuyen(String quyen) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities != null && authorities.stream()
            .anyMatch(a -> a.getAuthority() != null && a.getAuthority().equals(quyen));
    }
}
