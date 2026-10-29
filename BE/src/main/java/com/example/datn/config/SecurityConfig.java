package com.example.datn.config;

import com.example.datn.repository.PhienDangNhapRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final PhienDangNhapRepository phienDangNhapRepository;
    private final long thoiGianPhienGhiNhoMs;

    public SecurityConfig(
        JwtService jwtService,
        PhienDangNhapRepository phienDangNhapRepository,
        @Value("${app.auth.session-remember-ms:2592000000}") long thoiGianPhienGhiNhoMs
    ) {
        this.jwtService = jwtService;
        this.phienDangNhapRepository = phienDangNhapRepository;
        this.thoiGianPhienGhiNhoMs = thoiGianPhienGhiNhoMs;
    }

    /** JWT xác thực danh tính; quyền truy cập được giới hạn theo nhóm endpoint và vai trò. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/dang-ky", "/api/auth/dang-nhap",
                    "/api/auth/nhan-vien/dang-nhap", "/api/auth/refresh", "/api/auth/dang-xuat",
                    "/api/auth/quen-mat-khau/**", "/api/auth/nhan-vien/gui-ma-quen-mat-khau",
                    "/api/auth/nhan-vien/dat-lai-mat-khau"
                ).permitAll()
                .requestMatchers("/api/test", "/api/chat/khach/**", "/api/chat/ai/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/**", "/api/phieu-giam-gia").permitAll()
                .requestMatchers("/api/shipping/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/hoa-don").permitAll()
                .requestMatchers("/api/auth/thong-tin").authenticated()
                .requestMatchers("/api/phieu-giam-gia/cua-toi", "/api/gio-hang/**").hasRole("KHACH_HANG")
                .requestMatchers("/api/nhan-vien/**", "/admin/**", "/api/lich-lam-viec/**",
                    "/api/ca-lam-viec-dinh-nghia/**").hasRole("ADMIN")
                .requestMatchers("/api/thong-ke/**").hasRole("ADMIN")
                .requestMatchers("/api/ca/lich-su", "/api/ca/*/doi-soat").hasRole("ADMIN")
                .requestMatchers("/api/hoa-don/**",
                    "/api/khach-hang/**", "/api/khach-hang/{idKhachHang}/dia-chi/**",
                    "/api/chat/nhan-vien/**", "/api/voucher-email/**").hasAnyRole("NHAN_VIEN", "ADMIN")
                .requestMatchers("/api/ca/**").hasAnyRole("NHAN_VIEN", "ADMIN")
                .requestMatchers("/api/**").hasAnyRole("NHAN_VIEN", "ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((request, response, ex) -> chuaDangNhap(response))
                .accessDeniedHandler((request, response, ex) -> thieuQuyen(response))
            )
            .addFilterBefore(
                new JwtAuthFilter(jwtService, phienDangNhapRepository, thoiGianPhienGhiNhoMs),
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();
    }

    private static void thieuQuyen(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Bạn không có quyền thực hiện thao tác này\"}");
    }

    private static void chuaDangNhap(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"Chưa đăng nhập hoặc phiên đăng nhập không hợp lệ\"}");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
