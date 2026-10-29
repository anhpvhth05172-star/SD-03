package com.example.datn.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.datn.config.ApiExceptionHandler;
import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.ThongKe.GuiMailTongKetResponse;
import com.example.datn.service.BaoCaoTongKetService;
import com.example.datn.service.ThongKeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ThongKeControllerEmailTest {

    private ThongKeService thongKeService;
    private BaoCaoTongKetService baoCaoTongKetService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        thongKeService = mock(ThongKeService.class);
        baoCaoTongKetService = mock(BaoCaoTongKetService.class);
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ThongKeController(thongKeService, new QuyenGuard(), baoCaoTongKetService))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
    }

    @Test
    void guiMailTongKet_hopLeTraVe200VaKetQuaTuService() throws Exception {
        when(baoCaoTongKetService.guiMailTongKet(any()))
            .thenReturn(new GuiMailTongKetResponse(false, "admin@polyshoes.vn",
                "BÁO CÁO TỔNG KẾT & SO SÁNH DOANH THU — Tháng 10 Năm 2026",
                "Nhà cung cấp mail từ chối gửi hoặc lỗi kết nối SMTP — báo cáo CHƯA được gửi"));

        mockMvc.perform(post("/api/thong-ke/gui-mail-tong-ket")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loai\":\"THANG\",\"thang\":10,\"nam\":2026,\"emailNhan\":\"admin@polyshoes.vn\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.thanhCong").value(false))
            .andExpect(jsonPath("$.emailNhan").value("admin@polyshoes.vn"))
            .andExpect(jsonPath("$.lyDo").isNotEmpty());
    }

    @Test
    void guiMailTongKet_jsonThieuLoaiTraVe400() throws Exception {
        when(baoCaoTongKetService.guiMailTongKet(any()))
            .thenThrow(new IllegalArgumentException("loai phải là THANG hoặc NAM"));

        mockMvc.perform(post("/api/thong-ke/gui-mail-tong-ket")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("loai phải là THANG hoặc NAM"));
    }

    @Test
    void guiMailTongKet_jsonRongTraVe400() throws Exception {
        mockMvc.perform(post("/api/thong-ke/gui-mail-tong-ket")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void emailMacDinh_traVeEmailTuService() throws Exception {
        when(baoCaoTongKetService.emailMacDinh()).thenReturn("admin@polyshoes.vn");

        mockMvc.perform(get("/api/thong-ke/email-mac-dinh"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@polyshoes.vn"));
    }
}
