package com.example.datn.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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
                UsernamePasswordAuthenticationToken xacThuc = new UsernamePasswordAuthenticationToken(
                    idKhachHang,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_KHACH_HANG"))
                );
                SecurityContextHolder.getContext().setAuthentication(xacThuc);
                request.setAttribute("idKhachHang", idKhachHang);
            }
        }
        filterChain.doFilter(request, response);
    }
}
