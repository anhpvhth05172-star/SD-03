package com.example.datn.config;

import com.example.datn.entity.KhachHang;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey khoa;
    private final long thoiGianSongMs;

    public JwtService(
        @Value("${app.jwt.secret}") String biMat,
        @Value("${app.jwt.expiration-ms:86400000}") long thoiGianSongMs
    ) {
        this.khoa = Keys.hmacShaKeyFor(Decoders.BASE64.decode(biMat));
        this.thoiGianSongMs = thoiGianSongMs;
    }

    public String taoToken(KhachHang khachHang, Long idPhien) {
        Date hienTai = new Date();
        var builder = Jwts.builder()
            .subject(String.valueOf(khachHang.getId()))
            .claim("tenTaiKhoan", khachHang.getTenTaiKhoan())
            .claim("email", khachHang.getEmail());
        if (idPhien != null) {
            builder.claim("phienId", idPhien);
        }
        return builder
            .issuedAt(hienTai)
            .expiration(new Date(hienTai.getTime() + thoiGianSongMs))
            .signWith(khoa)
            .compact();
    }

    public boolean hopLe(String token) {
        try {
            docClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long layIdKhachHang(String token) {
        return Long.valueOf(docClaims(token).getSubject());
    }

    public Long layIdPhien(String token) {
        Object giaTri = docClaims(token).get("phienId");
        if (giaTri == null) {
            return null;
        }
        return Long.valueOf(giaTri.toString());
    }

    private Claims docClaims(String token) {
        return Jwts.parser()
            .verifyWith(khoa)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
