package com.example.datn.config;

import com.example.datn.entity.PhienDangNhap;
import com.example.datn.repository.PhienDangNhapRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final PhienDangNhapRepository phienDangNhapRepository;
    private final long thoiGianPhienMs;

    public JwtAuthFilter(
        JwtService jwtService,
        PhienDangNhapRepository phienDangNhapRepository,
        long thoiGianPhienMs
    ) {
        this.jwtService = jwtService;
        this.phienDangNhapRepository = phienDangNhapRepository;
        this.thoiGianPhienMs = thoiGianPhienMs;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!token.isEmpty() && jwtService.hopLe(token)) {
                String loaiTaiKhoan = jwtService.layLoaiTaiKhoan(token);
                if (JwtService.LOAI_NHAN_VIEN.equals(loaiTaiKhoan)) {
                    xuLyNhanVien(token, request);
                } else {
                    xuLyKhachHang(token, request);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private void xuLyKhachHang(String token, HttpServletRequest request) {
        Long idKhachHang = jwtService.layIdNguoiDung(token);
        Long idPhien = jwtService.layIdPhien(token);
        PhienDangNhap phien = idPhien == null
            ? null
            : phienDangNhapRepository.findById(idPhien).orElse(null);
        LocalDateTime bayGio = LocalDateTime.now();
        if (
            phien != null
                && !phien.isRevoked()
                && !phien.hetHan(bayGio, thoiGianPhienMs)
                && phien.getIdKhachHang().equals(idKhachHang)
        ) {
            UsernamePasswordAuthenticationToken xacThuc = new UsernamePasswordAuthenticationToken(
                idKhachHang,
                null,
                taoAuthorities(jwtService.layVaiTro(token), "KHACH_HANG")
            );
            SecurityContextHolder.getContext().setAuthentication(xacThuc);
            request.setAttribute("idKhachHang", idKhachHang);

            phien.setHoatDongCuoi(bayGio);
            phienDangNhapRepository.save(phien);
        }
    }

    private void xuLyNhanVien(String token, HttpServletRequest request) {
        Long idNhanVien = jwtService.layIdNguoiDung(token);
        Long idPhien = jwtService.layIdPhien(token);
        if (idPhien == null) {
            return;
        }
        LocalDateTime bayGio = LocalDateTime.now();
        PhienDangNhap phien = phienDangNhapRepository.findById(idPhien).orElse(null);
        boolean hopLe =
            phien != null
                && !phien.isRevoked()
                && !phien.hetHan(bayGio, thoiGianPhienMs)
                && idNhanVien.equals(phien.getIdNhanVien());
        if (!hopLe) {
            return;
        }
        phien.setHoatDongCuoi(bayGio);
        phienDangNhapRepository.save(phien);
        UsernamePasswordAuthenticationToken xacThuc = new UsernamePasswordAuthenticationToken(
            idNhanVien,
            null,
            taoAuthorities(jwtService.layVaiTro(token), "NHAN_VIEN")
        );
        SecurityContextHolder.getContext().setAuthentication(xacThuc);
        request.setAttribute("idNhanVien", idNhanVien);
    }

    static List<SimpleGrantedAuthority> taoAuthorities(String vaiTro, String loaiTaiKhoan) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + loaiTaiKhoan));
        if (vaiTro != null && !vaiTro.isBlank()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + vaiTro.trim().toUpperCase()));
        }
        return authorities;
    }
}
