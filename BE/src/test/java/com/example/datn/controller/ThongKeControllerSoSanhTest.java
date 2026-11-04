package com.example.datn.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.datn.config.ApiExceptionHandler;
import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.ThongKe.KySoSanhDTO;
import com.example.datn.dto.ThongKe.SoSanhDTO;
import com.example.datn.dto.ThongKe.TopSanPhamKyDTO;
import com.example.datn.service.BaoCaoTongKetService;
import com.example.datn.service.ThongKeService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ThongKeControllerSoSanhTest {

    private ThongKeService service;
    private QuyenGuard quyenGuard;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(ThongKeService.class);
        quyenGuard = new QuyenGuard();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ThongKeController(service, quyenGuard, mock(BaoCaoTongKetService.class)))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
        // canQuanLyKhachHang doc quyen tu SecurityContext (khong tin header X-Vai-Tro)
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            1L, "n/a", List.of(new SimpleGrantedAuthority("ROLE_NHAN_VIEN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static KySoSanhDTO ky(String ma) {
        return new KySoSanhDTO(ma, ma, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
            0, 0, 0, 0, 0, 0, 0, 0, false);
    }

    @Test
    void soSanh_hopLeTraVe200VaGuiThamSo() throws Exception {
        when(service.soSanh(eq("thang"), eq(2026), any(), any(), any(), any(), eq("FF Shirt"), eq("SP00")))
            .thenReturn(new SoSanhDTO(List.of(ky("T1"), ky("T2")), List.of()));

        mockMvc.perform(get("/api/thong-ke/so-sanh")
                .param("cheDo", "thang")
                .param("nam", "2026")
                .param("thuongHieu", "FF Shirt")
                .param("keyword", "SP00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ky.length()").value(2));

        verify(service).soSanh("thang", 2026, null, null, null, null, "FF Shirt", "SP00");
    }

    @Test
    void soSanh_serviceNemIAETraVe400CoMessage() throws Exception {
        when(service.soSanh(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new IllegalArgumentException("cheDo không hợp lệ"));

        mockMvc.perform(get("/api/thong-ke/so-sanh").param("cheDo", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("cheDo không hợp lệ"));
    }
}
