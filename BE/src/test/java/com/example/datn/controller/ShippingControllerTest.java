package com.example.datn.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.datn.dto.ShippingAddressDTO;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.dto.ShippingFeeResponse;
import com.example.datn.dto.ShippingServiceDTO;
import com.example.datn.service.GHNService;
import com.example.datn.service.GhnDiaChiMapper;
import com.example.datn.service.GhnDiaChiMapper.KetQuaAnhXach;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ShippingControllerTest {

    private final GHNService ghnService = mock(GHNService.class);
    private final GhnDiaChiMapper mapper = mock(GhnDiaChiMapper.class);
    private final ShippingController controller = new ShippingController(ghnService, mapper);

    @Test
    void fee_DiaChiMoi_MapSangMaLegacyRoiGoiGHN() {
        when(mapper.anhXach("Hà Nội", "Phường Đống Đa"))
            .thenReturn(new KetQuaAnhXach(201, 1486, "1A0415", "Phường Thịnh Quang"));
        when(ghnService.tinhPhiResponse(any()))
            .thenReturn(new ShippingFeeResponse(20900, "VND", "ok"));

        ShippingFeeRequest req = new ShippingFeeRequest(null, null, List.of());
        req.setToProvinceName("Hà Nội");
        req.setToWardName("Phường Đống Đa");

        ShippingFeeResponse res = controller.fee(req);

        ArgumentCaptor<ShippingFeeRequest> chup = ArgumentCaptor.forClass(ShippingFeeRequest.class);
        verify(ghnService).tinhPhiResponse(chup.capture());
        assertThat(chup.getValue().getToDistrictId()).isEqualTo(1486);
        assertThat(chup.getValue().getToWardCode()).isEqualTo("1A0415");
        assertThat(res.getShippingFee()).isEqualTo(20900);
        assertThat(res.getToProvinceId()).isEqualTo(201);
        assertThat(res.getToDistrictId()).isEqualTo(1486);
        assertThat(res.getToWardCode()).isEqualTo("1A0415");
        assertThat(res.getToWardName()).isEqualTo("Phường Đống Đa");
    }

    @Test
    void fee_CoMaLegacy_KhongGoiMapper() {
        when(ghnService.tinhPhiResponse(any()))
            .thenReturn(new ShippingFeeResponse(50000, "VND", "ok"));

        ShippingFeeRequest req = new ShippingFeeRequest(1442, "20107", List.of());
        ShippingFeeResponse res = controller.fee(req);

        verifyNoInteractions(mapper);
        assertThat(res.getToDistrictId()).isEqualTo(1442);
        assertThat(res.getToWardCode()).isEqualTo("20107");
        assertThat(res.getToProvinceId()).isNull();
    }

    @Test
    void fee_MapThatBai_LoiRoRangKhongGiaMao() {
        when(mapper.anhXach(any(), any()))
            .thenThrow(new IllegalArgumentException("Chưa tìm thấy phường/xã 'X'"));

        ShippingFeeRequest req = new ShippingFeeRequest(null, null, List.of());
        req.setToProvinceName("Hà Nội");
        req.setToWardName("Phường X");

        assertThatThrownBy(() -> controller.fee(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chưa tìm thấy phường/xã");
        verifyNoInteractions(ghnService);
    }

    @Test
    void provinces_V3_GoiGetTinhThanhMoi() {
        when(ghnService.getTinhThanhMoi())
            .thenReturn(List.of(new ShippingAddressDTO(1000000, "Hà Nội")));

        assertThat(controller.provinces()).hasSize(1);
        verify(ghnService).getTinhThanhMoi();
    }

    @Test
    void wards_GoiTheoTinhIdMoi() {
        when(ghnService.getPhuongXaMoi(1000000))
            .thenReturn(List.of(new ShippingAddressDTO(1003476, "Phường Đống Đa")));

        assertThat(controller.wards(1000000)).hasSize(1);
        verify(ghnService).getPhuongXaMoi(1000000);
    }

    @Test
    void services_TraDanhSachDichVu() {
        List<ShippingServiceDTO> ds = List.of(
            new ShippingServiceDTO(2, 53320L, "Hàng nhẹ"));
        when(ghnService.getDichVu(1442)).thenReturn(ds);

        assertThat(controller.services(1442)).isEqualTo(ds);
    }
}
