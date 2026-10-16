package com.example.datn.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Kiểm chứng SecurityFilterChain thật trên Spring context:
 * công khai / 401 khi thiếu token / 403 khi thiếu quyền / 200 khi đủ quyền.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper om = new ObjectMapper();

    private String tokenNhanVienAdmin() throws Exception {
        MvcResult result = mockMvc
            .perform(
                post("/api/auth/nhan-vien/dang-nhap")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"taiKhoan\":\"admin\",\"matKhau\":\"admin123\"}")
            )
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = om.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private String dangKyVaLoginKhachHang() throws Exception {
        String taiKhoan = "khtest" + System.currentTimeMillis() % 100_000_000L;
        String email = taiKhoan + "@test.local";
        mockMvc
            .perform(
                post("/api/auth/dang-ky")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"tenTaiKhoan\":\"" +
                            taiKhoan +
                            "\",\"email\":\"" +
                            email +
                            "\",\"soDienThoai\":\"0912345678\",\"matKhau\":\"matkhau123\"," +
                            "\"xacNhanMatKhau\":\"matkhau123\"}"
                    )
            )
            .andExpect(status().isCreated());
        MvcResult result = mockMvc
            .perform(
                post("/api/auth/dang-nhap")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"taiKhoan\":\"" + taiKhoan + "\",\"matKhau\":\"matkhau123\"}")
            )
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = om.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void endpointCongKhai_vanHoatDong() throws Exception {
        mockMvc.perform(get("/api/test")).andExpect(status().isOk());
    }

    // ==== JWT DA TAT theo yeu cau: cac test nay kiem tra hanh vi JWT cu, bo comment khi bat lai ====
    // @Test
    // void apiBaoVe_khongToken_tra401() throws Exception {
    //     mockMvc.perform(get("/api/hoa-don")).andExpect(status().isUnauthorized());
    //     mockMvc.perform(get("/api/nhan-vien")).andExpect(status().isUnauthorized());
    //     mockMvc.perform(get("/api/auth/thong-tin")).andExpect(status().isUnauthorized());
    //     mockMvc.perform(get("/api/v1/san-pham")).andExpect(status().isUnauthorized());
    // }
    //
    // @Test
    // void apiBaoVe_tokenSaiKyTra401() throws Exception {
    //     JwtService saiKy = new JwtService(
    //         "SaiKyTrenKetQuaKhongHopLe0123456789ABCDEFGHIJKL=",
    //         900_000L,
    //         900_000L
    //     );
    //     String tokenSaiKy = saiKy.taoTokenNhanVien(taoNhanVien(), 1L);
    //     mockMvc
    //         .perform(get("/api/hoa-don").header("Authorization", "Bearer " + tokenSaiKy))
    //         .andExpect(status().isUnauthorized());
    // }
    //
    // @Test
    // void apiBaoVe_tokenHetHan_tra401() throws Exception {
    //     JwtService hetHanService = new JwtService(
    //         "TGIqllBw2vKZsRHOmSm0a39M4Y7a0kipFX/aZASwkh4=",
    //         -1_000L,
    //         -1_000L
    //     );
    //     String tokenHetHan = hetHanService.taoTokenNhanVien(taoNhanVien(), 1L);
    //     mockMvc
    //         .perform(get("/api/hoa-don").header("Authorization", "Bearer " + tokenHetHan))
    //         .andExpect(status().isUnauthorized());
    // }

    @Test
    void dangNhapSaiMatKhau_tra401() throws Exception {
        mockMvc
            .perform(
                post("/api/auth/nhan-vien/dang-nhap")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"taiKhoan\":\"admin\",\"matKhau\":\"sai-mat-khau\"}")
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    void nhanVienAdmin_duQuyenXemHoaDon_vaQuanLyNhanVien() throws Exception {
        String token = tokenNhanVienAdmin();
        mockMvc
            .perform(get("/api/hoa-don").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
        mockMvc
            .perform(get("/api/nhan-vien").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    // @Test
    // void khachHang_thieuQuyen_tra403_khongBiLogout() throws Exception {
    //     String token = dangKyVaLoginKhachHang();
    //     mockMvc
    //         .perform(get("/api/hoa-don").header("Authorization", "Bearer " + token))
    //         .andExpect(status().isForbidden())
    //         .andExpect(jsonPath("$.message").exists());
    //     mockMvc
    //         .perform(get("/api/nhan-vien").header("Authorization", "Bearer " + token))
    //         .andExpect(status().isForbidden());
    // }
    //
    // @Test
    // void khachHang_duQuyenThongTinVaPhieuCuaToi() throws Exception {
    //     String token = dangKyVaLoginKhachHang();
    //     mockMvc
    //         .perform(get("/api/auth/thong-tin").header("Authorization", "Bearer " + token))
    //         .andExpect(status().isOk());
    //     mockMvc
    //         .perform(get("/api/phieu-giam-gia/cua-toi").header("Authorization", "Bearer " + token))
    //         .andExpect(status().isOk());
    // }

    @Test
    void chatKhachAnDanh_vanHoatDong() throws Exception {
        mockMvc
            .perform(
                post("/api/chat/khach/hoi-thoai")
                    .header("X-Ma-An-Danh", "integration-test-" + System.currentTimeMillis())
            )
            .andExpect(status().isOk());
    }

    private static com.example.datn.entity.NhanVien taoNhanVien() {
        com.example.datn.entity.NhanVien nv = new com.example.datn.entity.NhanVien();
        nv.setId(9L);
        nv.setTenTaiKhoan("admin");
        com.example.datn.entity.VaiTro vt = new com.example.datn.entity.VaiTro();
        vt.setMaVaiTro("ADMIN");
        nv.setVaiTro(vt);
        return nv;
    }
}
