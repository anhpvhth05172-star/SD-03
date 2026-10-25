package com.example.datn.service;

import com.example.datn.config.JwtService;
import com.example.datn.dto.Auth.AuthResponse;
import com.example.datn.dto.Auth.DangKyRequest;
import com.example.datn.dto.Auth.DangNhapRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhienDangNhap;
import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhienDangNhapRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String EMAIL_REGEX = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$";
    private static final String PHONE_REGEX = "^0\\d{9}$";

    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PhienDangNhapRepository phienDangNhapRepository;
    private final EmailService emailService;
    private final QuenMatKhauOtpService quenMatKhauOtpService;

    private final long thoiGianPhienMs;
    private final long thoiGianPhienGhiNhoMs;

    private static final String PREFIX_REFRESH_GHI_NHO = "r_";
    private static final String KHOA_OTP_KHACH = "KH:";

    public AuthService(
        KhachHangRepository khachHangRepository,
        NhanVienRepository nhanVienRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        PhienDangNhapRepository phienDangNhapRepository,
        @Value("${app.auth.session-inactivity-ms:1800000}") long thoiGianPhienMs,
        EmailService emailService,
        QuenMatKhauOtpService quenMatKhauOtpService,
        @Value("${app.auth.session-remember-ms:2592000000}") long thoiGianPhienGhiNhoMs
    ) {
        this.khachHangRepository = khachHangRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.phienDangNhapRepository = phienDangNhapRepository;
        this.thoiGianPhienMs = thoiGianPhienMs;
        this.emailService = emailService;
        this.quenMatKhauOtpService = quenMatKhauOtpService;
        this.thoiGianPhienGhiNhoMs = thoiGianPhienGhiNhoMs;
    }

    @Transactional
    public KhachHang dangKy(DangKyRequest req) {
        if (req == null || isBlank(req.getTenTaiKhoan())) {
            throw new IllegalArgumentException("Tên tài khoản không được để trống");
        }
        String tenTaiKhoan = req.getTenTaiKhoan().trim();
        if (tenTaiKhoan.length() > 100) {
            throw new IllegalArgumentException("Tên tài khoản không được vượt quá 100 ký tự");
        }
        if (isBlank(req.getEmail())) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        String email = req.getEmail().trim();
        if (email.length() > 150) {
            throw new IllegalArgumentException("Email không được vượt quá 150 ký tự");
        }
        if (!email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        if (isBlank(req.getSoDienThoai())) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }
        String soDienThoai = req.getSoDienThoai().trim();
        if (soDienThoai.length() > 10) {
            throw new IllegalArgumentException("Số điện thoại không được vượt quá 10 ký tự");
        }
        if (!soDienThoai.matches(PHONE_REGEX)) {
            throw new IllegalArgumentException("Số điện thoại phải bắt đầu bằng số 0 và gồm đúng 10 chữ số");
        }
        if (isBlank(req.getMatKhau())) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }
        if (req.getMatKhau().length() < 6 || req.getMatKhau().length() > 127) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 6 đến 127 ký tự");
        }
        if (isBlank(req.getXacNhanMatKhau())) {
            throw new IllegalArgumentException("Vui lòng xác nhận mật khẩu");
        }
        if (!req.getMatKhau().equals(req.getXacNhanMatKhau())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        if (khachHangRepository.existsByTenTaiKhoan(tenTaiKhoan)) {
            throw new IllegalArgumentException("Tên tài khoản đã tồn tại");
        }
        if (khachHangRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        KhachHang khachHang = new KhachHang();
        khachHang.setMaKhachHang(taoMaKhachHang());
        khachHang.setTenTaiKhoan(tenTaiKhoan);
        khachHang.setTenKhachHang(tenTaiKhoan);
        khachHang.setEmail(email);
        khachHang.setSoDienThoai(soDienThoai);
        khachHang.setMatKhau(passwordEncoder.encode(req.getMatKhau()));
        khachHang.setTrangThai(true);
        khachHang.setVaiTro("USER");
        khachHang.setNguoiTao("he-thong");
        return khachHangRepository.save(khachHang);
    }

    @Transactional
    public AuthResponse dangNhap(DangNhapRequest req) {
        if (req == null || isBlank(req.getTaiKhoan()) || isBlank(req.getMatKhau())) {
            throw new DangNhapThatBaiException("Vui lòng nhập tài khoản và mật khẩu");
        }
        KhachHang khachHang = khachHangRepository
            .findByEmailOrTenTaiKhoan(req.getTaiKhoan().trim())
            .orElseThrow(() -> new DangNhapThatBaiException("Sai tài khoản hoặc mật khẩu"));
        if (!passwordEncoder.matches(req.getMatKhau(), khachHang.getMatKhau())) {
            throw new DangNhapThatBaiException("Sai tài khoản hoặc mật khẩu");
        }
        if (Boolean.FALSE.equals(khachHang.getTrangThai())) {
            throw new DangNhapThatBaiException("Tài khoản đã bị khóa");
        }

        LocalDateTime bayGio = LocalDateTime.now();
        String refreshToken = taoRefreshToken(req.getGhiNhoDangNhap());
        PhienDangNhap phien = new PhienDangNhap();
        phien.setIdKhachHang(khachHang.getId());
        phien.setRefreshTokenHash(sha256(refreshToken));
        phien.setNgayTao(bayGio);
        phien.setHoatDongCuoi(bayGio);
        phien.setRevoked(false);
        phienDangNhapRepository.save(phien);

        return new AuthResponse(
            jwtService.taoToken(khachHang, phien.getId()),
            refreshToken,
            khachHang.getId(),
            khachHang.getTenTaiKhoan(),
            khachHang.getEmail(),
            khachHang.getTenKhachHang(),
            khachHang.getVaiTro()
        );
    }

    /**
     * Đăng nhập nhân viên: mật khẩu trong DB có thể là BCrypt hoặc văn bản thuần
     * (dữ liệu cũ do NhanVienService tạo). Hỗ trợ cả hai cách so khớp.
     */
    @Transactional
    public AuthResponse dangNhapNhanVien(DangNhapRequest req) {
        if (req == null || isBlank(req.getTaiKhoan()) || isBlank(req.getMatKhau())) {
            throw new DangNhapThatBaiException("Vui lòng nhập tài khoản và mật khẩu");
        }
        NhanVien nhanVien = nhanVienRepository
            .findByEmailOrTenTaiKhoan(req.getTaiKhoan().trim())
            .orElseThrow(() -> new DangNhapThatBaiException("Sai tài khoản hoặc mật khẩu"));
        if (!kiemTraMatKhauNhanVien(req.getMatKhau(), nhanVien.getMatKhau())) {
            throw new DangNhapThatBaiException("Sai tài khoản hoặc mật khẩu");
        }
        if (Boolean.FALSE.equals(nhanVien.getTrangThai())) {
            throw new DangNhapThatBaiException("Tài khoản đã bị khóa");
        }

        LocalDateTime bayGio = LocalDateTime.now();
        String refreshToken = taoRefreshToken(req.getGhiNhoDangNhap());
        PhienDangNhap phien = new PhienDangNhap();
        phien.setIdNhanVien(nhanVien.getId());
        phien.setRefreshTokenHash(sha256(refreshToken));
        phien.setNgayTao(bayGio);
        phien.setHoatDongCuoi(bayGio);
        phien.setRevoked(false);
        phienDangNhapRepository.save(phien);

        AuthResponse resp = new AuthResponse();
        resp.setToken(jwtService.taoTokenNhanVien(nhanVien, phien.getId()));
        resp.setRefreshToken(refreshToken);
        resp.setIdNhanVien(nhanVien.getId());
        resp.setLoaiTaiKhoan(JwtService.LOAI_NHAN_VIEN);
        resp.setTenTaiKhoan(nhanVien.getTenTaiKhoan());
        resp.setEmail(nhanVien.getEmail());
        resp.setTenKhachHang(nhanVien.getTenTaiKhoan());
        resp.setVaiTro(nhanVien.getVaiTro() != null ? nhanVien.getVaiTro().getMaVaiTro() : "STAFF");
        return resp;
    }

    private boolean kiemTraMatKhauNhanVien(String nhapVao, String daLuu) {
        if (isBlank(daLuu)) {
            return false;
        }
        if (daLuu.startsWith("$2a$") || daLuu.startsWith("$2b$") || daLuu.startsWith("$2y$")) {
            return passwordEncoder.matches(nhapVao, daLuu);
        }
        return daLuu.equals(nhapVao);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (isBlank(refreshToken)) {
            throw new DangNhapThatBaiException("Phiên đăng nhập không hợp lệ");
        }
        PhienDangNhap phien = phienDangNhapRepository
            .findByRefreshTokenHash(sha256(refreshToken))
            .orElseThrow(() -> new DangNhapThatBaiException("Phiên đăng nhập không hợp lệ"));
        LocalDateTime bayGio = LocalDateTime.now();
        boolean ghiNho = refreshToken.startsWith(PREFIX_REFRESH_GHI_NHO);
        long thoiGianGioiHan = ghiNho ? thoiGianPhienGhiNhoMs : thoiGianPhienMs;
        if (phien.isRevoked() || phien.hetHan(bayGio, thoiGianGioiHan)) {
            throw new DangNhapThatBaiException("Phiên đăng nhập đã hết hạn");
        }

        if (phien.getIdNhanVien() != null) {
            NhanVien nhanVien = nhanVienRepository
                .findById(phien.getIdNhanVien())
                .orElseThrow(() -> new DangNhapThatBaiException("Phiên đăng nhập không hợp lệ"));
            if (Boolean.FALSE.equals(nhanVien.getTrangThai())) {
                throw new DangNhapThatBaiException("Tài khoản đã bị khóa");
            }
            phien.setHoatDongCuoi(bayGio);
            phienDangNhapRepository.save(phien);

            AuthResponse resp = new AuthResponse();
            resp.setToken(jwtService.taoTokenNhanVien(nhanVien, phien.getId()));
            resp.setRefreshToken(refreshToken);
            resp.setIdNhanVien(nhanVien.getId());
            resp.setLoaiTaiKhoan(JwtService.LOAI_NHAN_VIEN);
            resp.setTenTaiKhoan(nhanVien.getTenTaiKhoan());
            resp.setEmail(nhanVien.getEmail());
            resp.setTenKhachHang(nhanVien.getTenTaiKhoan());
            resp.setVaiTro(nhanVien.getVaiTro() != null ? nhanVien.getVaiTro().getMaVaiTro() : "STAFF");
            return resp;
        }

        KhachHang khachHang = khachHangRepository
            .findById(phien.getIdKhachHang())
            .orElseThrow(() -> new DangNhapThatBaiException("Phiên đăng nhập không hợp lệ"));
        if (Boolean.FALSE.equals(khachHang.getTrangThai())) {
            throw new DangNhapThatBaiException("Tài khoản đã bị khóa");
        }

        phien.setHoatDongCuoi(bayGio);
        phienDangNhapRepository.save(phien);

        return new AuthResponse(
            jwtService.taoToken(khachHang, phien.getId()),
            refreshToken,
            khachHang.getId(),
            khachHang.getTenTaiKhoan(),
            khachHang.getEmail(),
            khachHang.getTenKhachHang(),
            khachHang.getVaiTro()
        );
    }

    /**
     * Admin đặt lại mật khẩu cho nhân viên. Tài khoản vai trò ADMIN không được đổi mật khẩu.
     */
    @Transactional
    public void doiMatKhauNhanVien(Long id, String matKhauMoi, String xacNhan) {
        hopLeMatKhauMoi(matKhauMoi, xacNhan);
        NhanVien nhanVien = nhanVienRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên"));
        if (nhanVien.getVaiTro() != null && "ADMIN".equalsIgnoreCase(nhanVien.getVaiTro().getMaVaiTro())) {
            throw new IllegalArgumentException("Không được đổi mật khẩu tài khoản quản trị");
        }
        nhanVien.setMatKhau(passwordEncoder.encode(matKhauMoi));
        nhanVienRepository.save(nhanVien);
    }

    /**
     * Admin đặt lại mật khẩu cho khách hàng.
     */
    @Transactional
    public void doiMatKhauKhachHang(Long id, String matKhauMoi, String xacNhan) {
        hopLeMatKhauMoi(matKhauMoi, xacNhan);
        KhachHang khachHang = khachHangRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        khachHang.setMatKhau(passwordEncoder.encode(matKhauMoi));
        khachHangRepository.save(khachHang);
    }

    private static void hopLeMatKhauMoi(String matKhauMoi, String xacNhan) {
        if (isBlank(matKhauMoi)) {
            throw new IllegalArgumentException("Mật khẩu mới không được để trống");
        }
        if (matKhauMoi.length() < 6 || matKhauMoi.length() > 127) {
            throw new IllegalArgumentException("Mật khẩu phải có từ 6 đến 127 ký tự");
        }
        if (isBlank(xacNhan)) {
            throw new IllegalArgumentException("Vui lòng xác nhận mật khẩu mới");
        }
        if (!matKhauMoi.equals(xacNhan)) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
    }

    /**
     * Nhân viên quên mật khẩu: gửi mã OTP 6 số tới email đã đăng ký.
     */
    @Transactional
    public void guiMaQuenMatKhau(String email) {
        NhanVien nhanVien = timNhanVienTheoEmail(email);
        String maOtp = quenMatKhauOtpService.sinhMa(nhanVien.getEmail());
        if (!emailService.guiMaQuenMatKhau(nhanVien.getEmail(), maOtp)) {
            throw new IllegalStateException("Không gửi được mã xác nhận, vui lòng thử lại sau");
        }
    }

    /**
     * Nhân viên đặt lại mật khẩu bằng mã OTP hợp lệ.
     */
    @Transactional
    public void datLaiMatKhau(String email, String maOtp, String matKhauMoi, String xacNhan) {
        hopLeMatKhauMoi(matKhauMoi, xacNhan);
        quenMatKhauOtpService.xacNhan(email, maOtp);
        NhanVien nhanVien = timNhanVienTheoEmail(email);
        nhanVien.setMatKhau(passwordEncoder.encode(matKhauMoi));
        nhanVienRepository.save(nhanVien);
    }

    /**
     * Quên mật khẩu chung (nhân viên hoặc khách hàng): tự nhận diện email
     * thuộc tài khoản nào rồi gửi OTP 6 số qua email.
     */
    @Transactional
    public void guiMaQuenMatKhauChung(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        String diaChi = email.trim();
        if (nhanVienRepository.findByEmail(diaChi).isPresent()) {
            guiMaQuenMatKhau(diaChi);
            return;
        }
        KhachHang khachHang = timKhachHangTheoEmail(diaChi);
        String maOtp = quenMatKhauOtpService.sinhMa(KHOA_OTP_KHACH + khachHang.getEmail());
        if (!emailService.guiMaQuenMatKhau(khachHang.getEmail(), maOtp)) {
            throw new IllegalStateException("Không gửi được mã xác nhận, vui lòng thử lại sau");
        }
    }

    /**
     * Đặt lại mật khẩu chung: kiểm tra OTP rồi cập nhật mật khẩu của đúng tài khoản.
     */
    @Transactional
    public void datLaiMatKhauChung(String email, String maOtp, String matKhauMoi, String xacNhan) {
        hopLeMatKhauMoi(matKhauMoi, xacNhan);
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        String diaChi = email.trim();
        if (nhanVienRepository.findByEmail(diaChi).isPresent()) {
            datLaiMatKhau(diaChi, maOtp, matKhauMoi, xacNhan);
            return;
        }
        KhachHang khachHang = timKhachHangTheoEmail(diaChi);
        quenMatKhauOtpService.xacNhan(KHOA_OTP_KHACH + diaChi, maOtp);
        khachHang.setMatKhau(passwordEncoder.encode(matKhauMoi));
        khachHangRepository.save(khachHang);
    }

    private KhachHang timKhachHangTheoEmail(String email) {
        return khachHangRepository
            .findByEmail(email)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email này"));
    }

    private NhanVien timNhanVienTheoEmail(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        return nhanVienRepository
            .findByEmail(email.trim())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với email này"));
    }

    @Transactional
    public void dangXuat(String refreshToken) {
        if (isBlank(refreshToken)) {
            return;
        }
        Optional<PhienDangNhap> phien = phienDangNhapRepository.findByRefreshTokenHash(sha256(refreshToken));
        if (phien.isPresent() && !phien.get().isRevoked()) {
            phien.get().setRevoked(true);
            phien.get().setNgayDangXuat(LocalDateTime.now());
            phienDangNhapRepository.save(phien.get());
        }
    }

    private static String taoRefreshToken(Boolean ghiNhoDangNhap) {
        byte[] bytes = new byte[48];
        ThreadLocalRandom.current().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return Boolean.TRUE.equals(ghiNhoDangNhap) ? PREFIX_REFRESH_GHI_NHO + token : token;
    }

    private static String sha256(String giaTri) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(giaTri.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Khong tim duoc SHA-256", e);
        }
    }

    private String taoMaKhachHang() {
        for (int i = 0; i < 100; i++) {
            String ma = "KH" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
            if (!khachHangRepository.existsByMaKhachHang(ma)) {
                return ma;
            }
        }
        return "KH" + (System.currentTimeMillis() % 100_000_000);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
