package com.example.datn.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.datn.entity.ChatLieu;
import com.example.datn.entity.KichCo;
import com.example.datn.entity.MauSac;
import com.example.datn.entity.SanPham;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.entity.ThuongHieu;
import com.example.datn.repository.DotGiamGiaChiTietRepository;
import com.example.datn.repository.DotGiamGiaRepository;
import com.example.datn.repository.SanPhamChiTietRepository;
import com.example.datn.service.impls.DotGiamGiaServiceFinal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * RED/GREEN cho Task 6 plan SD-03: for-discount loc theo Thuong hieu/Chat lieu
 * + response day du tenThuongHieu/chatLieu.
 */
class DotGiamGiaLocTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SanPhamChiTietRepository spctRepo = mock(SanPhamChiTietRepository.class);
        DotGiamGiaChiTietRepository chiTietRepo = mock(DotGiamGiaChiTietRepository.class);
        DotGiamGiaRepository dotRepo = mock(DotGiamGiaRepository.class);

        when(spctRepo.findAllActiveForDiscount()).thenReturn(List.of(
                variant(1L, "SP-1", "TH-A", "CL-Da"),
                variant(2L, "SP-2", "TH-B", "CL-Vai"),
                variant(3L, "SP-3", "TH-A", "CL-Vai")));
        when(chiTietRepo.findApDungHienTai(any(LocalDateTime.class), any(Long.class)))
                .thenReturn(List.of());

        DotGiamGiaServiceFinal service = new DotGiamGiaServiceFinal(dotRepo, chiTietRepo, spctRepo);
        DotGiamGiaRestController controller = new DotGiamGiaRestController();
        ReflectionTestUtils.setField(controller, "serviceFinal", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void forDiscountTraVeTenThuongHieuVaChatLieu() throws Exception {
        mvc.perform(get("/admin/dot-giam-gia/san-pham-chi-tiet/for-discount"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].tenThuongHieu").value("TH-A"))
                .andExpect(jsonPath("$[0].chatLieu").value("CL-Da"))
                .andExpect(jsonPath("$[1].tenThuongHieu").value("TH-B"))
                .andExpect(jsonPath("$[1].chatLieu").value("CL-Vai"));
    }

    @Test
    void locTheoThuongHieu() throws Exception {
        mvc.perform(get("/admin/dot-giam-gia/san-pham-chi-tiet/for-discount")
                        .param("idThuongHieu", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].tenThuongHieu", org.hamcrest.Matchers.hasItems("TH-A")))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void locTheoChatLieu() throws Exception {
        mvc.perform(get("/admin/dot-giam-gia/san-pham-chi-tiet/for-discount")
                        .param("idChatLieu", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chatLieu").value("CL-Vai"))
                .andExpect(jsonPath("$[1].chatLieu").value("CL-Vai"));
    }

    @Test
    void locKetHopThuongHieuVaChatLieu() throws Exception {
        mvc.perform(get("/admin/dot-giam-gia/san-pham-chi-tiet/for-discount")
                        .param("idThuongHieu", "1")
                        .param("idChatLieu", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tenThuongHieu").value("TH-A"))
                .andExpect(jsonPath("$[0].chatLieu").value("CL-Vai"));
    }

    private static SanPhamChiTiet variant(Long id, String tenSp, String th, String cl) {
        ThuongHieu thuongHieu = new ThuongHieu();
        thuongHieu.setId(th.equals("TH-A") ? 1L : 2L);
        thuongHieu.setTenThuongHieu(th);

        ChatLieu chatLieu = new ChatLieu();
        chatLieu.setId(cl.equals("CL-Da") ? 1L : 2L);
        chatLieu.setTenChatLieu(cl);

        SanPham sp = new SanPham();
        sp.setId(id);
        sp.setMaSanPham(tenSp);
        sp.setTenSanPham(tenSp);
        sp.setThuongHieu(thuongHieu);
        sp.setChatLieu(chatLieu);

        KichCo kichCo = new KichCo();
        kichCo.setId(1L);
        kichCo.setTenKichCo("40");

        MauSac mauSac = new MauSac();
        mauSac.setId(1L);
        mauSac.setTenMau("Do");

        SanPhamChiTiet s = new SanPhamChiTiet();
        s.setId(id);
        s.setSanPham(sp);
        s.setKichCo(kichCo);
        s.setMauSac(mauSac);
        s.setMaChiTietSanPham("CT-" + id);
        s.setGiaBan(new BigDecimal("100000"));
        s.setSoLuong(5);
        s.setTrangThai(true);
        return s;
    }
}
