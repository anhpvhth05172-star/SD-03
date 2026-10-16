package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.service.GhnDiaChiMapper.KetQuaAnhXach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GhnDiaChiMapperTest {

    private final GHNApiClient ghnApiClient = mock(GHNApiClient.class);
    private final OpenApiClient openApiClient = mock(OpenApiClient.class);
    private final GhnDiaChiMapper mapper = new GhnDiaChiMapper(ghnApiClient, openApiClient);

    @BeforeEach
    void stubGhnLegacy() {
        when(ghnApiClient.get(contains("master-data/province"))).thenReturn("""
            {"code":200,"data":[
              {"ProvinceID":201,"ProvinceName":"Hà Nội"},
              {"ProvinceID":202,"ProvinceName":"Hải Phòng"}
            ]}
            """);
        when(ghnApiClient.post(contains("master-data/district"), contains("\"province_id\":201"))).thenReturn("""
            {"code":200,"data":[
              {"DistrictID":1442,"ProvinceID":201,"DistrictName":"Quận Ba Đình"},
              {"DistrictID":1486,"ProvinceID":201,"DistrictName":"Quận Đống Đa"}
            ]}
            """);
        when(ghnApiClient.post(contains("master-data/ward"), contains("\"district_id\":1486"))).thenReturn("""
            {"code":200,"data":[
              {"WardCode":"1A0405","DistrictID":1486,"WardName":"Phường Kim Liên"},
              {"WardCode":"1A0415","DistrictID":1486,"WardName":"Phường Thịnh Quang"},
              {"WardCode":"1A0416","DistrictID":1486,"WardName":"Phường Nam Đồng"}
            ]}
            """);
        when(ghnApiClient.post(contains("master-data/ward"), contains("\"district_id\":1442"))).thenReturn("""
            {"code":200,"data":[
              {"WardCode":"1A0101","DistrictID":1442,"WardName":"Phường Phúc Xá"},
              {"WardCode":"1A0105","DistrictID":1442,"WardName":"Phường Trúc Bạch"}
            ]}
            """);
    }

    private void stubOpenApiCoBaiDongDa() {
        when(openApiClient.get(contains("/api/v2/p/"))).thenReturn("""
            [{"code":1,"name":"Thành phố Hà Nội"},
             {"code":26,"name":"Thành phố Hải Phòng"}]
            """);
        when(openApiClient.get(contains("/api/v2/w/?province=1&search="))).thenReturn("""
            [{"code":235,"name":"Phường Đống Đa","province_code":1,"division_type":"phường"}]
            """);
        when(openApiClient.get(contains("/api/v2/w/235/to-legacies/"))).thenReturn("""
            [{"code":190,"name":"Phường Ô Chợ Dừa","district_code":6,"province_code":1},
             {"code":235,"name":"Phường Thịnh Quang","district_code":6,"province_code":1}]
            """);
        when(openApiClient.get(contains("/api/v1/"))).thenReturn("""
            [{"code":1,"name":"Thành phố Hà Nội","districts":[
               {"code":3,"name":"Quận Ba Đình","province_code":1},
               {"code":6,"name":"Quận Đống Đa","province_code":1}]}]
            """);
    }

    @Test
    void anhXach_TenPhuongCoTrongGhnLegacy_TraMaLegacyNhanhKhongGoiOpenApi() {
        KetQuaAnhXach kq = mapper.anhXach("Thành phố Hà Nội", "PHƯỜNG KIM LIÊN");

        assertThat(kq.tinhId()).isEqualTo(201);
        assertThat(kq.quanId()).isEqualTo(1486);
        assertThat(kq.maPhuong()).isEqualTo("1A0405");
        assertThat(kq.tenPhuong()).isEqualTo("Phường Kim Liên");
        verify(openApiClient, never()).get(contains(anyPath()));
    }

    @Test
    void anhXach_PhuongChiCoTrongDuLieuMoi_TraMaLegacySauKhiGhep() {
        stubOpenApiCoBaiDongDa();

        KetQuaAnhXach kq = mapper.anhXach("Thành phố Hà Nội", "Phường Đống Đa");

        assertThat(kq.tinhId()).isEqualTo(201);
        assertThat(kq.quanId()).isEqualTo(1486);
        assertThat(kq.maPhuong()).isEqualTo("1A0415");
        assertThat(kq.tenPhuong()).isEqualTo("Phường Thịnh Quang");
    }

    @Test
    void anhXach_KhongTimThay_BatLoiRoRang() {
        when(openApiClient.get(contains("/api/v2/p/"))).thenReturn("""
            [{"code":1,"name":"Thành phố Hà Nội"}]
            """);
        when(openApiClient.get(contains("/api/v2/w/?province=1&search="))).thenReturn("[]");

        assertThatThrownBy(() -> mapper.anhXach("Thành phố Hà Nội", "Phường Không Tồn Tại"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chưa tìm thấy phường/xã")
            .hasMessageContaining("Phường Không Tồn Tại");
    }

    @Test
    void anhXach_OpenApiLoi_BatLoiKhongGiaMaoMa() {
        when(openApiClient.get(contains("/api/v2/p/")))
            .thenThrow(new IllegalArgumentException(
                "Không tra cứu được dữ liệu địa chỉ hành chính. Vui lòng thử lại sau"));

        assertThatThrownBy(() -> mapper.anhXach("Thành phố Hà Nội", "Phường Đống Đa"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không tra cứu được dữ liệu địa chỉ");
    }

    @Test
    void anhXach_GhepDuocNhungGhnChuaHoTro_BatLoiCoNghia() {
        when(openApiClient.get(contains("/api/v2/p/"))).thenReturn("""
            [{"code":1,"name":"Thành phố Hà Nội"}]
            """);
        when(openApiClient.get(contains("/api/v2/w/?province=1&search="))).thenReturn("""
            [{"code":500,"name":"Phường Đơn Vị Mới","province_code":1,"division_type":"phường"}]
            """);
        when(openApiClient.get(contains("/api/v2/w/500/to-legacies/"))).thenReturn("""
            [{"code":700,"name":"Phường Đơn Vị Mới","district_code":6,"province_code":1}]
            """);
        when(openApiClient.get(contains("/api/v1/"))).thenReturn("""
            [{"code":1,"name":"Thành phố Hà Nội","districts":[
               {"code":6,"name":"Quận Đống Đa","province_code":1}]}]
            """);

        assertThatThrownBy(() -> mapper.anhXach("Thành phố Hà Nội", "Phường Đơn Vị Mới"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("GHN chưa hỗ trợ");
    }

    @Test
    void anhXach_DungKetQuaGiuaHaiQuan_BatLoiChuaXacDinhDuoc() {
        when(openApiClient.get(contains("/api/v2/p/"))).thenReturn("""
            [{"code":1,"name":"Thành phố Hà Nội"}]
            """);
        when(openApiClient.get(contains("/api/v2/w/?province=1&search="))).thenReturn("""
            [{"code":500,"name":"Phường Tie Test","province_code":1,"division_type":"phường"}]
            """);
        when(openApiClient.get(contains("/api/v2/w/500/to-legacies/"))).thenReturn("""
            [{"code":1,"name":"Phường Thịnh Quang","district_code":6,"province_code":1},
             {"code":2,"name":"Phường Kim Liên","district_code":6,"province_code":1},
             {"code":3,"name":"Phường Phúc Xá","district_code":3,"province_code":1},
             {"code":4,"name":"Phường Trúc Bạch","district_code":3,"province_code":1}]
            """);
        when(openApiClient.get(contains("/api/v1/"))).thenReturn("""
            [{"code":1,"name":"Thành phố Hà Nội","districts":[
               {"code":3,"name":"Quận Ba Đình","province_code":1},
               {"code":6,"name":"Quận Đống Đa","province_code":1}]}]
            """);

        assertThatThrownBy(() -> mapper.anhXach("Thành phố Hà Nội", "Phường Tie Test"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Chưa xác định được quận/huyện");
    }

    @Test
    void anhXach_ThieuTen_BiTuChoi() {
        assertThatThrownBy(() -> mapper.anhXach(null, "Phường Kim Liên"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thiếu");
        assertThatThrownBy(() -> mapper.anhXach("Hà Nội", "  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Thiếu");
    }

    @Test
    void anhXach_TimThayNhieuKetQuaTrongMoiCham_BatLoiChonCuThe() {
        when(openApiClient.get(contains("/api/v2/p/"))).thenReturn("""
            [{"code":1,"name":"Thành phố Hà Nội"}]
            """);
        when(openApiClient.get(contains("/api/v2/w/?province=1&search="))).thenReturn("""
            [{"code":111,"name":"Phường Đống Đa","province_code":1},
             {"code":222,"name":"Phường Đống Đa","province_code":1}]
            """);

        assertThatThrownBy(() -> mapper.anhXach("Thành phố Hà Nội", "Phường Đống Đa"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nhiều kết quả");
    }

    private static String anyPath() {
        return "khong-phai-open-api";
    }
}
