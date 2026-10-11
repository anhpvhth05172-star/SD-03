package com.example.datn.service;

import com.example.datn.config.GHNConfig;
import com.example.datn.dto.ShippingAddressDTO;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.dto.ShippingFeeResponse;
import com.example.datn.dto.ShippingWardDTO;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.SanPhamChiTietRepository;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class GHNService {

    private static final String PATH_PROVINCE = "/shiip/public-api/master-data/province";
    private static final String PATH_DISTRICT = "/shiip/public-api/master-data/district";
    private static final String PATH_WARD = "/shiip/public-api/master-data/ward";
    private static final String PATH_FEE = "/shiip/public-api/v2/shipping-order/fee";

    private final GHNConfig cauHinh;
    private final GHNApiClient ghnApiClient;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ShippingAddressDTO> getTinhThanh() {
        JsonNode root = doc(ghnApiClient.get(PATH_PROVINCE));
        kiemTraCode(root, "tỉnh/thành");
        List<ShippingAddressDTO> ketQua = new ArrayList<>();
        for (JsonNode n : root.path("data")) {
            int id = n.path("ProvinceID").asInt(0);
            String ten = n.path("ProvinceName").asText("");
            if (id > 0 && !ten.isBlank()) {
                ketQua.add(new ShippingAddressDTO(id, ten));
            }
        }
        sapXepTheoTen(ketQua);
        return ketQua;
    }

    public List<ShippingAddressDTO> getQuanHuyen(Integer provinceId) {
        if (provinceId == null) {
            throw new IllegalArgumentException("Thiếu mã tỉnh/thành để lấy danh sách quận/huyện");
        }
        JsonNode root = doc(ghnApiClient.post(PATH_DISTRICT, "{\"province_id\":" + provinceId + "}"));
        kiemTraCode(root, "quận/huyện");
        List<ShippingAddressDTO> ketQua = new ArrayList<>();
        for (JsonNode n : root.path("data")) {
            int id = n.path("DistrictID").asInt(0);
            String ten = n.path("DistrictName").asText("");
            if (id > 0 && !ten.isBlank()) {
                ketQua.add(new ShippingAddressDTO(id, ten));
            }
        }
        sapXepTheoTen(ketQua);
        return ketQua;
    }

    public List<ShippingWardDTO> getPhuongXa(Integer districtId) {
        if (districtId == null) {
            throw new IllegalArgumentException("Thiếu mã quận/huyện để lấy danh sách phường/xã");
        }
        JsonNode root = doc(ghnApiClient.post(PATH_WARD, "{\"district_id\":" + districtId + "}"));
        kiemTraCode(root, "phường/xã");
        List<ShippingWardDTO> ketQua = new ArrayList<>();
        for (JsonNode n : root.path("data")) {
            String code = n.path("WardCode").asText("");
            String ten = n.path("WardName").asText("");
            if (!code.isBlank() && !ten.isBlank()) {
                ketQua.add(new ShippingWardDTO(code, ten));
            }
        }
        Collator collator = Collator.getInstance(new Locale("vi", "VN"));
        ketQua.sort(Comparator.comparing(ShippingWardDTO::getName, collator));
        return ketQua;
    }

    public ShippingFeeResponse tinhPhiResponse(ShippingFeeRequest req) {
        int phi = tinhPhi(req);
        return new ShippingFeeResponse(phi, "VND", "Phí vận chuyển được tính bởi GHN");
    }

    public int tinhPhi(ShippingFeeRequest req) {
        cauHinh.kiemTraCauHinh();
        if (req == null || req.getToDistrictId() == null) {
            throw new IllegalArgumentException("Thiếu mã quận/huyện để tính phí vận chuyển");
        }
        if (req.getToWardCode() == null || req.getToWardCode().isBlank()) {
            throw new IllegalArgumentException("Thiếu mã phường/xã để tính phí vận chuyển");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Chưa có sản phẩm để tính phí vận chuyển");
        }

        long tongTrongLuong = 0;
        long giaTriHang = 0;
        for (ShippingFeeRequest.DongHang dong : req.getItems()) {
            if (dong == null || dong.idSanPhamChiTiet() == null) {
                throw new IllegalArgumentException("Từng dòng sản phẩm phải chọn sản phẩm");
            }
            if (dong.soLuong() == null || dong.soLuong() < 1) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            }
            SanPhamChiTiet spct = sanPhamChiTietRepository.findById(dong.idSanPhamChiTiet())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm chi tiết không tồn tại"));
            long trongLuongDon = spct.getTrongLuong() != null && spct.getTrongLuong().longValue() > 0
                ? spct.getTrongLuong().longValue()
                : cauHinh.getDefaultItemWeightG();
            try {
                tongTrongLuong = Math.addExact(
                    tongTrongLuong,
                    Math.multiplyExact(trongLuongDon, dong.soLuong().longValue())
                );
                if (spct.getGiaBan() != null) {
                    giaTriHang = Math.addExact(
                        giaTriHang,
                        Math.multiplyExact(spct.getGiaBan().longValue(), dong.soLuong().longValue())
                    );
                }
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException(
                    "Trọng lượng hoặc giá trị đơn hàng vượt quá giới hạn cho phép");
            }
        }
        if (tongTrongLuong <= 0) {
            tongTrongLuong = cauHinh.getDefaultItemWeightG();
        }
        if (tongTrongLuong > cauHinh.getMaxWeightG()) {
            throw new IllegalArgumentException(
                "Tổng trọng lượng đơn hàng " + tongTrongLuong + "g vượt quá giới hạn "
                    + cauHinh.getMaxWeightG() + "g của GHN");
        }
        long baoHiem = Math.min(giaTriHang, cauHinh.getInsuranceMaxVnd());

        ObjectNode body = objectMapper.createObjectNode();
        body.put("service_type_id", cauHinh.getServiceTypeId());
        body.put("to_district_id", req.getToDistrictId());
        body.put("to_ward_code", req.getToWardCode().trim());
        body.put("weight", tongTrongLuong);
        body.put("length", cauHinh.getPackageLengthCm());
        body.put("width", cauHinh.getPackageWidthCm());
        body.put("height", cauHinh.getPackageHeightCm());
        body.put("insurance_value", baoHiem);
        if (cauHinh.getFromDistrictId() != null) {
            body.put("from_district_id", cauHinh.getFromDistrictId());
        }
        if (cauHinh.getFromWardCode() != null && !cauHinh.getFromWardCode().isBlank()) {
            body.put("from_ward_code", cauHinh.getFromWardCode().trim());
        }

        JsonNode root = doc(ghnApiClient.post(PATH_FEE, body.toString()));
        int code = root.path("code").asInt(0);
        if (code != 200 && code != 1) {
            throw new IllegalArgumentException(
                "GHN không tính được phí vận chuyển: " + root.path("message").asText("lỗi không xác định"));
        }
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull() || !data.isObject() || data.isEmpty()) {
            throw new IllegalArgumentException("GHN không trả về dữ liệu phí vận chuyển cho địa chỉ này");
        }
        long tong = data.path("total").asLong(-1);
        if (tong <= 0) {
            throw new IllegalArgumentException("GHN trả về phí vận chuyển không hợp lệ");
        }
        if (tong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Phí vận chuyển vượt quá giới hạn cho phép");
        }
        return (int) tong;
    }

    private JsonNode doc(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("GHN không trả về dữ liệu");
        }
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Phản hồi từ GHN không hợp lệ");
        }
    }

    private void kiemTraCode(JsonNode root, String loai) {
        int code = root.path("code").asInt(0);
        if (code != 200 && code != 1) {
            throw new IllegalArgumentException(
                "GHN không lấy được danh sách " + loai + ": " + root.path("message").asText("lỗi không xác định"));
        }
        JsonNode data = root.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new IllegalArgumentException("GHN không trả về dữ liệu " + loai);
        }
    }

    private void sapXepTheoTen(List<ShippingAddressDTO> ds) {
        Collator collator = Collator.getInstance(new Locale("vi", "VN"));
        ds.sort(Comparator.comparing(ShippingAddressDTO::getName, collator));
    }
}
