package com.example.datn.config;

import com.example.datn.entity.KhachHang;
import com.example.datn.entity.NhanVien;
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

    public static final String LOAI_KHACH_HANG = "KHACH_HANG";
    public static final String LOAI_NHAN_VIEN = "NHAN_VIEN";

    private final SecretKey khoa;
    private final long thoiGianSongMs;
    private final long thoiGianSongNhanVienMs;

    public JwtService(
        @Value("${app.jwt.secret}") String biMat,
        @Value("${app.jwt.expiration-ms:86400000}") long thoiGianSongMs,
        @Value("${app.jwt.nhan-vien-expiration-ms:28800000}") long thoiGianSongNhanVienMs
    ) {
        this.khoa = Keys.hmacShaKeyFor(Decoders.BASE64.decode(biMat));
        this.thoiGianSongMs = thoiGianSongMs;
        this.thoiGianSongNhanVienMs = thoiGianSongNhanVienMs;
    }

    public String taoToken(KhachHang khachHang, Long idPhien) {
        Date hienTai = new Date();
        var builder = Jwts.builder()
            .subject(String.valueOf(khachHang.getId()))
            .claim("loaiTaiKhoan", LOAI_KHACH_HANG)
            .claim("vaiTro", khachHang.getVaiTro())
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

    public String taoTokenNhanVien(NhanVien nhanVien, Long idPhien) {
        Date hienTai = new Date();
        String vaiTro = nhanVien.getVaiTro() != null ? nhanVien.getVaiTro().getMaVaiTro() : "STAFF";
        var builder = Jwts.builder()
            .subject(String.valueOf(nhanVien.getId()))
            .claim("loaiTaiKhoan", LOAI_NHAN_VIEN)
            .claim("vaiTro", vaiTro)
            .claim("tenTaiKhoan", nhanVien.getTenTaiKhoan())
            .claim("email", nhanVien.getEmail());
        if (idPhien != null) {
            builder.claim("phienId", idPhien);
        }
        return builder
            .issuedAt(hienTai)
            .expiration(new Date(hienTai.getTime() + thoiGianSongNhanVienMs))
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

    public Long layIdNguoiDung(String token) {
        return Long.valueOf(docClaims(token).getSubject());
    }

    /** Tên cũ — giữ tương thích, subject giờ là id khách hàng hoặc nhân viên. */
    public Long layIdKhachHang(String token) {
        return layIdNguoiDung(token);
    }

    public String layLoaiTaiKhoan(String token) {
        Object giaTri = docClaims(token).get("loaiTaiKhoan");
        return giaTri == null ? LOAI_KHACH_HANG : giaTri.toString();
    }

    public String layVaiTro(String token) {
        Object giaTri = docClaims(token).get("vaiTro");
        return giaTri == null ? null : giaTri.toString();
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
