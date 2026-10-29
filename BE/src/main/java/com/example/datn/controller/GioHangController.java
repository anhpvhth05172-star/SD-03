package com.example.datn.controller;

import com.example.datn.dto.GioHang.AddGioHangResponse;
import com.example.datn.dto.GioHang.GioHangResponse;
import com.example.datn.dto.GioHang.ThemGioHangRequest;
import com.example.datn.dto.GioHang.UpdateGioHangRequest;
import com.example.datn.service.GioHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/gio-hang")
@RequiredArgsConstructor
public class GioHangController {

    private final GioHangService gioHangService;

    @PostMapping("/them")
    public ResponseEntity<AddGioHangResponse> them(@Valid @RequestBody ThemGioHangRequest request, Authentication auth) {
        request.setIdKhachHang(kiemTraChuSoHuu(auth, request.getIdKhachHang()));
        return ResponseEntity.ok(gioHangService.themVaoGioHang(request));
    }

    @GetMapping
    public ResponseEntity<GioHangResponse> xem(@RequestParam Long idKhachHang, Authentication auth) {
        kiemTraChuSoHuu(auth, idKhachHang);
        return ResponseEntity.ok(gioHangService.xemGioHang(idKhachHang));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> dem(@RequestParam(required = false) Long idKhachHang, Authentication auth) {
        idKhachHang = kiemTraChuSoHuu(auth, idKhachHang);
        return ResponseEntity.ok(Map.of("tongSoLuong", gioHangService.demSoLuong(idKhachHang)));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<GioHangResponse> capNhat(@PathVariable Integer id,
                                                   @Valid @RequestBody UpdateGioHangRequest request,
                                                   Authentication auth) {
        request.setIdKhachHang(kiemTraChuSoHuu(auth, request.getIdKhachHang()));
        return ResponseEntity.ok(gioHangService.capNhatSoLuong(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<GioHangResponse> xoaMot(@PathVariable Integer id,
                                                  @RequestParam Long idKhachHang,
                                                  Authentication auth) {
        kiemTraChuSoHuu(auth, idKhachHang);
        return ResponseEntity.ok(gioHangService.xoaSanPham(id, idKhachHang));
    }

    @DeleteMapping
    public ResponseEntity<GioHangResponse> xoaHet(@RequestParam Long idKhachHang, Authentication auth) {
        kiemTraChuSoHuu(auth, idKhachHang);
        return ResponseEntity.ok(gioHangService.xoaTatCa(idKhachHang));
    }

    private static Long kiemTraChuSoHuu(Authentication auth, Long requestedId) {
        if (auth == null || !(auth.getPrincipal() instanceof Long id)
            || auth.getAuthorities().stream().noneMatch(a -> "ROLE_KHACH_HANG".equals(a.getAuthority()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập tài khoản khách hàng");
        }
        if (requestedId != null && !requestedId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể truy cập giỏ hàng của tài khoản khác");
        }
        return id;
    }
}
