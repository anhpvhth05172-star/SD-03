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

    /**
     * Dang public permitAll (ma tran quyen van comment duoi day).
     * JwtAuthFilter duoc bat lai de xac dinh danh tinh cho chat nhan-vien:
     * filter dung cua so lon nhat (ghi nho); /api/auth/refresh van chot han dung.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Ma tran quyen truoc do (da comment khi tat JWT):
            // .authorizeHttpRequests(auth -> auth
            //     .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            //     .requestMatchers(
            //         HttpMethod.POST,
            //         "/api/auth/dang-ky",
            //         "/api/auth/dang-nhap",
            //         "/api/auth/nhan-vien/dang-nhap",
            //         "/api/auth/refresh",
            //         "/api/auth/dang-xuat"
            //     )
            //         .permitAll()
            //     .requestMatchers("/api/test").permitAll()
            //     .requestMatchers("/api/chat/khach/**", "/api/chat/ai/**").permitAll()
            //     .requestMatchers("/api/auth/thong-tin", "/api/phieu-giam-gia/cua-toi").authenticated()
            //     .requestMatchers(
            //         "/api/hoa-don/**",
            //         "/api/v1/san-pham/**",
            //         "/api/v1/san-pham-chi-tiet/**",
            //         "/api/v1/thuong-hieu/**",
            //         "/api/v1/xuat-xu/**",
            //         "/api/v1/chat-lieu/**",
            //         "/api/v1/kieu-dang/**",
            //         "/api/v1/loai-giay/**",
            //         "/api/v1/kich-co/**",
            //         "/api/v1/mau-sac/**",
            //         "/api/v1/than-giay/**",
            //         "/api/v1/de-giay/**",
            //         "/api/v1/attributes/**",
            //         "/api/phieu-giam-gia/**",
            //         "/api/dot-giam-gia/**",
            //         "/admin/phieu-giam-gia/**",
            //         "/admin/dot-giam-gia/**",
            //         "/api/voucher-email/**",
            //         "/api/shipping/**",
            //         "/api/khach-hang/**",
            //         "/api/chat/nhan-vien/**"
            //     )
            //         .hasRole("NHAN_VIEN")
            //     .requestMatchers("/api/nhan-vien/**").hasRole("ADMIN")
            //     .anyRequest().authenticated()
            // )
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
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
