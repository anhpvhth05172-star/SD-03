package com.example.datn.controller;

import com.example.datn.dto.Chat.GuiTinNhanRequest;
import com.example.datn.dto.Chat.HoiThoaiDTO;
import com.example.datn.dto.Chat.TinNhanDTO;
import com.example.datn.service.ChatService;
import com.example.datn.service.SanPhamChatContextService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Chat nhân viên (REST + polling) và chat AI (Gemini).
 * Khách chưa đăng nhập: gửi maAnDanh (UUID ẩn danh) qua header/body.
 * Khách đã đăng nhập: token JWT xác định idKhachHang.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final com.example.datn.service.GeminiService geminiService;
    private final SanPhamChatContextService sanPhamChatContextService;

    // ---- Khách: hội thoại nhân viên ----

    @PostMapping("/khach/hoi-thoai")
    public HoiThoaiDTO layHoiThoaiKhach(
        @RequestHeader(value = "X-Ma-An-Danh", required = false) String maAnDanh,
        Authentication auth
    ) {
        Long idKhachHang = idKhachHangIfDangNhap(auth);
        return chatService.layHoiThoaiNhanVien(idKhachHang, maAnDanh);
    }

    @PostMapping("/khach/hoi-thoai/{id}/tin-nhan")
    public TinNhanDTO guiTinKhach(
        @PathVariable Long id,
        @RequestBody GuiTinNhanRequest req,
        @RequestHeader(value = "X-Ma-An-Danh", required = false) String maAnDanh,
        Authentication auth
    ) {
        Long idKhachHang = idKhachHangIfDangNhap(auth);
        return chatService.guiTinKhach(id, idKhachHang, maAnDanh, req.getNoiDung());
    }

    @GetMapping("/khach/hoi-thoai/{id}/tin-nhan")
    public List<TinNhanDTO> layTinKhach(
        @PathVariable Long id,
        @RequestParam(required = false) Long tuId,
        @RequestHeader(value = "X-Ma-An-Danh", required = false) String maAnDanh,
        Authentication auth
    ) {
        Long idKhachHang = idKhachHangIfDangNhap(auth);
        return chatService.layTinKhach(id, idKhachHang, maAnDanh, tuId);
    }

    @PutMapping("/khach/hoi-thoai/{id}/da-doc")
    public ResponseEntity<Void> danhDaDocKhach(
        @PathVariable Long id,
        @RequestHeader(value = "X-Ma-An-Danh", required = false) String maAnDanh,
        Authentication auth
    ) {
        Long idKhachHang = idKhachHangIfDangNhap(auth);
        chatService.danhDaDocTinKhach(id, idKhachHang, maAnDanh);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/khach/lien-ket")
    public HoiThoaiDTO lienKetSauDangNhap(
        @RequestBody(required = false) Map<String, String> body,
        Authentication auth
    ) {
        Long idKhachHang = idKhachHangBatBuoc(auth);
        String maAnDanh = body == null ? null : body.get("maAnDanh");
        return chatService.lienKetVaDangNhap(idKhachHang, maAnDanh);
    }

    // ---- Nhân viên ----

    @GetMapping("/nhan-vien/hoi-thoai")
    public List<HoiThoaiDTO> dsHoiThoaiNhanVien(Authentication auth) {
        return chatService.dsHoiThoaiNhanVien(idNhanVien(auth));
    }

    @PostMapping("/nhan-vien/hoi-thoai/{id}/tin-nhan")
    public TinNhanDTO guiTinNhanVien(
        @PathVariable Long id,
        @RequestBody GuiTinNhanRequest req,
        Authentication auth
    ) {
        return chatService.guiTinNhanVien(id, idNhanVien(auth), req.getNoiDung());
    }

    @GetMapping("/nhan-vien/hoi-thoai/{id}/tin-nhan")
    public List<TinNhanDTO> layTinNhanVien(
        @PathVariable Long id,
        @RequestParam(required = false) Long tuId,
        Authentication auth
    ) {
        return chatService.layTinNhanVien(id, idNhanVien(auth), tuId);
    }

    @PutMapping("/nhan-vien/hoi-thoai/{id}/da-doc")
    public ResponseEntity<Void> danhDaDocNhanVien(@PathVariable Long id, Authentication auth) {
        chatService.danhDaDocTinNhanVien(id, idNhanVien(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/nhan-vien/hoi-thoai/{id}/dong")
    public HoiThoaiDTO dongHoiThoai(@PathVariable Long id, Authentication auth) {
        return chatService.dongHoiThoai(id, idNhanVien(auth));
    }

    // ---- AI ----

    @PostMapping("/ai/hoi-thoai")
    public HoiThoaiDTO layHoiThoaiAI(
        @RequestHeader(value = "X-Ma-An-Danh", required = false) String maAnDanh,
        Authentication auth
    ) {
        Long idKhachHang = idKhachHangIfDangNhap(auth);
        return chatService.layHoiThoaiAI(idKhachHang, maAnDanh);
    }

    @PostMapping("/ai/tin-nhan")
    public Map<String, Object> hoiAI(
        @RequestBody GuiTinNhanRequest req,
        @RequestHeader(value = "X-Ma-An-Danh", required = false) String maAnDanh,
        Authentication auth
    ) {
        Long idKhachHang = idKhachHangIfDangNhap(auth);
        HoiThoaiDTO hoiThoai = chatService.layHoiThoaiAI(idKhachHang, maAnDanh);
        String noiDungKhach = req.getNoiDung();
        String noiDungAI;
        try {
            String boSung = sanPhamChatContextService.taoBoSung();
            noiDungAI = geminiService.hoi(noiDungKhach, boSung);
        } catch (Exception e) {
            noiDungAI =
                "Xin lỗi, trợ lý AI đang gặp sự cố kết nối. "
                    + "Bạn hãy thử lại sau hoặc liên hệ nhân viên để được hỗ trợ trực tiếp.";
        }
        TinNhanDTO tinKQ = chatService.guiTinAIvaLuu(
            hoiThoai.getId(), idKhachHang, maAnDanh, noiDungKhach, noiDungAI
        );
        return Map.of(
            "idHoiThoai", hoiThoai.getId(),
            "tinKhach", tinKQ,
            "noiDungAI", noiDungAI
        );
    }

    @GetMapping("/ai/hoi-thoai/{id}/tin-nhan")
    public List<TinNhanDTO> layTinAI(
        @PathVariable Long id,
        @RequestParam(required = false) Long tuId,
        @RequestHeader(value = "X-Ma-An-Danh", required = false) String maAnDanh,
        Authentication auth
    ) {
        Long idKhachHang = idKhachHangIfDangNhap(auth);
        return chatService.layTinAI(id, idKhachHang, maAnDanh, tuId);
    }

    // ---- helpers ----

    private static Long idKhachHangIfDangNhap(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Long id)) {
            return null;
        }
        boolean laNV = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_NHAN_VIEN".equals(a.getAuthority()));
        return laNV ? null : id;
    }

    private static Long idKhachHangBatBuoc(Authentication auth) {
        Long id = idKhachHangIfDangNhap(auth);
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return id;
    }

    private static Long idNhanVien(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        boolean laNV = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_NHAN_VIEN".equals(a.getAuthority()));
        if (!laNV) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cần đăng nhập nhân viên");
        }
        return id;
    }
}
