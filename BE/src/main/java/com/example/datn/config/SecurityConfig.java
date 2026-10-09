package com.example.datn.config;

import com.example.datn.repository.PhienDangNhapRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final PhienDangNhapRepository phienDangNhapRepository;
    private final long thoiGianPhienMs;

    public SecurityConfig(
        JwtService jwtService,
        PhienDangNhapRepository phienDangNhapRepository,
        @Value("${app.auth.session-inactivity-ms:1800000}") long thoiGianPhienMs
    ) {
        this.jwtService = jwtService;
        this.phienDangNhapRepository = phienDangNhapRepository;
        this.thoiGianPhienMs = thoiGianPhienMs;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/dang-ky",
                    "/api/auth/dang-nhap",
                    "/api/auth/refresh",
                    "/api/auth/dang-xuat"
                )
                    .permitAll()
                .requestMatchers("/api/auth/thong-tin", "/api/phieu-giam-gia/cua-toi").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(eh -> eh.authenticationEntryPoint((request, response, ex) -> chuaDangNhap(response)))
            .addFilterBefore(
                new JwtAuthFilter(jwtService, phienDangNhapRepository, thoiGianPhienMs),
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();
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
