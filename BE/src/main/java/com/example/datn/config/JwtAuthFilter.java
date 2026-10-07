package com.example.datn.config;

import com.example.datn.entity.PhienDangNhap;
import com.example.datn.repository.PhienDangNhapRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
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
                Long idKhachHang = jwtService.layIdKhachHang(token);
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
                        List.of(new SimpleGrantedAuthority("ROLE_KHACH_HANG"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(xacThuc);
                    request.setAttribute("idKhachHang", idKhachHang);

                    // Hoat dong hop le -> gia han phien 30 phut ke tu luc nay
                    phien.setHoatDongCuoi(bayGio);
                    phienDangNhapRepository.save(phien);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
