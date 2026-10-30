package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.datn.config.GHNConfig;
import com.example.datn.dto.ShippingAddressDTO;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.dto.ShippingServiceDTO;
import com.example.datn.dto.ShippingWardDTO;
import com.example.datn.entity.SanPham;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.SanPhamChiTietRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GHNServiceTest {

    private static final Long SPCT_ID = 9L;

    private final GHNConfig cauHinh = new GHNConfig();
    private final GHNApiClient ghnApiClient = mock(GHNApiClient.class);
    private final SanPhamChiTietRepository sanPhamChiTietRepository =
        mock(SanPhamChiTietRepository.class);
    private final OpenApiClient openApiClient = mock(OpenApiClient.class);
    private final GHNService service = new GHNService(
        cauHinh, ghnApiClient, sanPhamChiTietRepository, openApiClient
    );

    @BeforeEach
    void datCauHinh() {
        cauHinh.setApiUrl("https://dev-online-gateway.ghn.vn");
        cauHinh.setToken("token-test");
        cauHinh.setShopId("885");
        cauHinh.setPackageLengthCm(30);
        cauHinh.setPackageWidthCm(20);
        cauHinh.setPackageHeightCm(15);
        cauHinh.setDefaultItemWeightG(500);
        cauHinh.setMaxWeightG(30000);
        cauHinh.setInsuranceMaxVnd(5000000);
        cauHinh.setFromDistrictId(null);
        cauHinh.setFromWardCode(null);
        cauHinh.setTimeoutMs(5000);
    }

    private SanPhamChiTiet spct(BigDecimal trongLuong, BigDecimal giaBan) {
        SanPhamChiTiet spct = new SanPhamChiTiet();
        spct.setId(SPCT_ID);
        spct.setMaChiTietSanPham("SPCT01");
        spct.setTrongLuong(trongLuong);
        spct.setGiaBan(giaBan);
        spct.setTrangThai(true);
        SanPham sp = new SanPham();
        sp.setId(1L);
        sp.setTenSanPham("Giay test");
        sp.setTrangThai(true);
        spct.setSanPham(sp);
        return spct;
    }

    private ShippingFeeRequest yeuCau(int soLuong) {
        return new ShippingFeeRequest(
            1442,
            "20107",
            List.of(new ShippingFeeRequest.DongHang(SPCT_ID, soLuong))
        );
    }

    @Test
    void thieuToken_KhongCauHinh_TraPhiMacDinh() {
        cauHinh.setToken("");

        assertThat(service.tinhPhi(yeuCau(1))).isEqualTo(30000);

        verifyNoInteractions(ghnApiClient);
    }

    @Test
    void thieuShopId_KhongCauHinh_TraPhiMacDinh() {
        cauHinh.setShopId("  ");

        assertThat(service.tinhPhi(yeuCau(1))).isEqualTo(30000);

        verifyNoInteractions(ghnApiClient);
    }

    @Test
    void thieuQuanHuyen_BiTuChoi() {
        assertThatThrownBy(() -> service.tinhPhi(
            new ShippingFeeRequest(null, "20107", List.of(new ShippingFeeRequest.DongHang(SPCT_ID, 1)))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thiếu mã quận/huyện");
    }

    @Test
    void thieuPhuongXa_BiTuChoi() {
        assertThatThrownBy(() -> service.tinhPhi(
            new ShippingFeeRequest(1442, " ", List.of(new ShippingFeeRequest.DongHang(SPCT_ID, 1)))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thiếu mã phường/xã");
    }

    @Test
    void khongCoSanPham_BiTuChoi() {
        assertThatThrownBy(() -> service.tinhPhi(
            new ShippingFeeRequest(1442, "20107", List.of())))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chưa có sản phẩm");
    }

    @Test
    void soLuongAm_BiTuChoi() {
        assertThatThrownBy(() -> service.tinhPhi(yeuCau(0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số lượng phải lớn hơn 0");
    }

    @Test
    void tinhPhi_TruongTrongLuongTuSanPhamVaGiaTriHang() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(new BigDecimal("800"), new BigDecimal("500000"))));
        when(ghnApiClient.post(contains("shipping-order/fee"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":{\"total\":36300}}");

        int phi = service.tinhPhi(yeuCau(2));

        assertThat(phi).isEqualTo(36300);
        ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
        verify(ghnApiClient).post(contains("shipping-order/fee"), bodyCap.capture());
        String body = bodyCap.getValue();
        assertThat(body).contains("\"weight\":1600");
        assertThat(body).contains("\"to_district_id\":1442");
        assertThat(body).contains("\"to_ward_code\":\"20107\"");
        assertThat(body).contains("\"service_type_id\":2");
        assertThat(body).contains("\"length\":30");
        assertThat(body).contains("\"insurance_value\":1000000");
    }

    @Test
    void trongLuongThieu_DungKhoiLuongMacDinh() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(null, new BigDecimal("300000"))));
        when(ghnApiClient.post(contains("shipping-order/fee"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":{\"total\":22000}}");

        int phi = service.tinhPhi(yeuCau(3));

        assertThat(phi).isEqualTo(22000);
        ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
        verify(ghnApiClient).post(contains("shipping-order/fee"), bodyCap.capture());
        assertThat(bodyCap.getValue()).contains("\"weight\":1500");
    }

    @Test
    void giaTriHangVuotGioiHanBaoHiem_GioiHanTheoCauHinh() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(new BigDecimal("700"), new BigDecimal("10000000"))));
        when(ghnApiClient.post(contains("shipping-order/fee"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":{\"total\":45000}}");

        service.tinhPhi(yeuCau(1));

        ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
        verify(ghnApiClient).post(contains("shipping-order/fee"), bodyCap.capture());
        assertThat(bodyCap.getValue()).contains("\"insurance_value\":5000000");
    }

    @Test
    void vuotKhoiLuongToiDa_BiTuChoi() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(new BigDecimal("20000"), new BigDecimal("500000"))));

        assertThatThrownBy(() -> service.tinhPhi(yeuCau(2)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vượt quá giới hạn");
    }

    @Test
    void ghnTraMaLoi_BaoLoiCoNghia() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(new BigDecimal("800"), new BigDecimal("500000"))));
        when(ghnApiClient.post(contains("shipping-order/fee"), anyString()))
            .thenReturn("{\"code\":400,\"message\":\"ward code not found\",\"data\":null}");

        assertThatThrownBy(() -> service.tinhPhi(yeuCau(1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ward code not found");
    }

    @Test
    void ghnTraDataRong_BaoLoi() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(new BigDecimal("800"), new BigDecimal("500000"))));
        when(ghnApiClient.post(contains("shipping-order/fee"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":null}");

        assertThatThrownBy(() -> service.tinhPhi(yeuCau(1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không trả về dữ liệu phí vận chuyển");
    }

    @Test
    void ghnTraPhiZero_BaoLoi() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(new BigDecimal("800"), new BigDecimal("500000"))));
        when(ghnApiClient.post(contains("shipping-order/fee"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":{\"total\":0}}");

        assertThatThrownBy(() -> service.tinhPhi(yeuCau(1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("phí vận chuyển không hợp lệ");
    }

    @Test
    void khongKetNoiDuocGHN_BaoLoiThanhThien() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(new BigDecimal("800"), new BigDecimal("500000"))));
        when(ghnApiClient.post(contains("shipping-order/fee"), anyString()))
            .thenThrow(new IllegalArgumentException("Không kết nối được máy chủ GHN. Vui lòng thử lại"));

        assertThatThrownBy(() -> service.tinhPhi(yeuCau(1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không kết nối được máy chủ GHN");
    }

    @Test
    void tinhThanh_MapDungVaSapXepTheoTiengViet() {
        when(ghnApiClient.get(contains("master-data/province")))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":["
                + "{\"ProvinceID\":202,\"ProvinceName\":\"Hồ Chí Minh\"},"
                + "{\"ProvinceID\":201,\"ProvinceName\":\"Hà Nội\"}]}");

        List<ShippingAddressDTO> ds = service.getTinhThanh();

        assertThat(ds).hasSize(2);
        assertThat(ds.get(0).getName()).isEqualTo("Hà Nội");
        assertThat(ds.get(1).getName()).isEqualTo("Hồ Chí Minh");
    }

    @Test
    void quanHuyen_MapDung() {
        when(ghnApiClient.post(contains("master-data/district"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":["
                + "{\"DistrictID\":1442,\"ProvinceID\":202,\"DistrictName\":\"Quận 1\"}]}");

        List<ShippingAddressDTO> ds = service.getQuanHuyen(202);

        assertThat(ds).hasSize(1);
        assertThat(ds.get(0).getId()).isEqualTo(1442);
        assertThat(ds.get(0).getName()).isEqualTo("Quận 1");
    }

    @Test
    void quanHuyen_ThieuProvinceId_BiTuChoi() {
        assertThatThrownBy(() -> service.getQuanHuyen(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thiếu mã tỉnh/thành");

        verifyNoInteractions(ghnApiClient);
    }

    @Test
    void phuongXa_MapDungCodeLaChuoi() {
        when(ghnApiClient.post(contains("master-data/ward"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":["
                + "{\"WardCode\":21012,\"DistrictID\":1442,\"WardName\":\"Phường Bến Nghé\"}]}");

        List<ShippingWardDTO> ds = service.getPhuongXa(1442);

        assertThat(ds).hasSize(1);
        assertThat(ds.get(0).getCode()).isEqualTo("21012");
        assertThat(ds.get(0).getName()).isEqualTo("Phường Bến Nghé");
    }

    @Test
    void phuongXa_ThieuDistrictId_BiTuChoi() {
        assertThatThrownBy(() -> service.getPhuongXa(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thiếu mã quận/huyện");
    }

    @Test
    void tinhThanhMoi_MapDungIdVaTenTheoBoChuViet() {
        when(ghnApiClient.get(contains("v3/master-data/province/all")))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":["
                + "{\"_id\":1000001,\"name\":\"Hồ Chí Minh\",\"type\":\"province\"},"
                + "{\"_id\":1000000,\"name\":\"Hà Nội\",\"type\":\"province\"}]}");

        List<ShippingAddressDTO> ds = service.getTinhThanhMoi();

        assertThat(ds).hasSize(2);
        assertThat(ds.get(0).getId()).isEqualTo(1000000);
        assertThat(ds.get(0).getName()).isEqualTo("Hà Nội");
        assertThat(ds.get(1).getName()).isEqualTo("Hồ Chí Minh");
    }

    @Test
    void phuongXaMoi_MapTheoTinhIdVaSapXep() {
        when(ghnApiClient.get(contains("v3/master-data/ward/all-by-province-id?province_id=1000000")))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":["
                + "{\"_id\":1003472,\"name\":\"Phường Kim Liên\",\"parent_id\":1000000},"
                + "{\"_id\":1003476,\"name\":\"Phường Đống Đa\",\"parent_id\":1000000}]}");

        List<ShippingAddressDTO> ds = service.getPhuongXaMoi(1000000);

        assertThat(ds).hasSize(2);
        assertThat(ds.get(0).getId()).isEqualTo(1003476);
        assertThat(ds.get(0).getName()).isEqualTo("Phường Đống Đa");
        assertThat(ds.get(1).getName()).isEqualTo("Phường Kim Liên");
    }

    @Test
    void phuongXaMoi_ThieuTinhId_BiTuChoi() {
        assertThatThrownBy(() -> service.getPhuongXaMoi(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thiếu mã tỉnh/thành");

        verifyNoInteractions(ghnApiClient);
    }

    @Test
    void phuongXaMoi_GhnTraMaLoi_BaoLoiCoNghia() {
        when(ghnApiClient.get(contains("ward/all-by-province-id")))
            .thenReturn("{\"code\":400,\"message\":\"Province not found\",\"data\":null}");

        assertThatThrownBy(() -> service.getPhuongXaMoi(1000000))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Province not found");
    }

    @Test
    void dichVu_GuiDiaChiGuiTuCauHinhVaParseDanhSach() {
        cauHinh.setFromDistrictId(1486);
        cauHinh.setFromWardCode("910012");
        when(ghnApiClient.post(contains("available-services"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":["
                + "{\"service_id\":53320,\"short_name\":\"Hàng nhẹ\",\"service_type_id\":2},"
                + "{\"service_id\":100039,\"short_name\":\"Hàng nặng\",\"service_type_id\":5}]}");

        List<ShippingServiceDTO> ds = service.getDichVu(1442);

        assertThat(ds).hasSize(2);
        assertThat(ds.get(0).getServiceTypeId()).isEqualTo(2);
        assertThat(ds.get(0).getShortName()).isEqualTo("Hàng nhẹ");
        assertThat(ds.get(1).getServiceTypeId()).isEqualTo(5);
        ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
        verify(ghnApiClient).post(contains("available-services"), bodyCap.capture());
        String body = bodyCap.getValue();
        assertThat(body).contains("\"shop_id\":885");
        assertThat(body).contains("\"from_district\":1486");
        assertThat(body).contains("\"to_district\":1442");
    }

    @Test
    void dichVu_ThieuToDistrict_BiTuChoi() {
        assertThatThrownBy(() -> service.getDichVu(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thiếu mã quận/huyện đích");

        verifyNoInteractions(ghnApiClient);
    }

    @Test
    void dichVu_ThieuDiaChiGui_BaoLoiCauHinh() {
        cauHinh.setFromDistrictId(null);

        assertThatThrownBy(() -> service.getDichVu(1442))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("GHN_FROM_DISTRICT_ID");

        verifyNoInteractions(ghnApiClient);
    }

    @Test
    void dichVu_ThieuToken_KhongCauHinh_TraDichVuMacDinh() {
        cauHinh.setToken("");

        List<ShippingServiceDTO> ds = service.getDichVu(1442);

        assertThat(ds).hasSize(1);
        assertThat(ds.get(0).getServiceTypeId()).isEqualTo(2);
        assertThat(ds.get(0).getShortName()).isEqualTo("Giao hàng tiêu chuẩn");
        verifyNoInteractions(ghnApiClient);
    }

    @Test
    void tinhPhi_DungServiceTypeIdTuYeuCau() {
        when(sanPhamChiTietRepository.findById(SPCT_ID))
            .thenReturn(Optional.of(spct(new BigDecimal("800"), new BigDecimal("500000"))));
        when(ghnApiClient.post(contains("shipping-order/fee"), anyString()))
            .thenReturn("{\"code\":200,\"message\":\"Success\",\"data\":{\"total\":41000}}");

        ShippingFeeRequest req = yeuCau(1);
        req.setServiceTypeId(5);
        int phi = service.tinhPhi(req);

        assertThat(phi).isEqualTo(41000);
        ArgumentCaptor<String> bodyCap = ArgumentCaptor.forClass(String.class);
        verify(ghnApiClient).post(contains("shipping-order/fee"), bodyCap.capture());
        assertThat(bodyCap.getValue()).contains("\"service_type_id\":5");
    }
}
