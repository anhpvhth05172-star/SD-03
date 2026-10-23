package com.example.datn.service;

import com.example.datn.config.GHNConfig;
import com.example.datn.dto.ShippingAddressDTO;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.dto.ShippingFeeResponse;
import com.example.datn.dto.ShippingServiceDTO;
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
public class GHNService {

    private static final String PATH_PROVINCE = "/shiip/public-api/master-data/province";
    private static final String PATH_DISTRICT = "/shiip/public-api/master-data/district";
    private static final String PATH_WARD = "/shiip/public-api/master-data/ward";
    private static final String PATH_FEE = "/shiip/public-api/v2/shipping-order/fee";
    private static final String PATH_PROVINCE_V3 = "/shiip/public-api/v3/master-data/province/all";
    private static final String PATH_WARD_V3 = "/shiip/public-api/v3/master-data/ward/all-by-province-id";
    private static final String PATH_SERVICES = "/shiip/public-api/v2/shipping-order/available-services";

    private static final long CACHE_TTL_MS = 5 * 60_000L;

    private static final String[][] DS_63_TINH = {
        {"1", "Thành phố Hà Nội"}, {"2", "Tỉnh Hà Giang"}, {"4", "Tỉnh Cao Bằng"}, {"6", "Tỉnh Bắc Kạn"},
        {"8", "Tỉnh Tuyên Quang"}, {"10", "Tỉnh Lào Cai"}, {"11", "Tỉnh Điện Biên"}, {"12", "Tỉnh Lai Châu"},
        {"14", "Tỉnh Sơn La"}, {"15", "Tỉnh Yên Bái"}, {"17", "Tỉnh Hoà Bình"}, {"19", "Tỉnh Thái Nguyên"},
        {"20", "Tỉnh Lạng Sơn"}, {"22", "Tỉnh Quảng Ninh"}, {"24", "Tỉnh Bắc Giang"}, {"25", "Tỉnh Phú Thọ"},
        {"26", "Tỉnh Vĩnh Phúc"}, {"27", "Tỉnh Bắc Ninh"}, {"30", "Tỉnh Hải Dương"}, {"31", "Thành phố Hải Phòng"},
        {"33", "Tỉnh Hưng Yên"}, {"34", "Tỉnh Thái Bình"}, {"35", "Tỉnh Hà Nam"}, {"36", "Tỉnh Nam Định"},
        {"37", "Tỉnh Ninh Bình"}, {"38", "Tỉnh Thanh Hóa"}, {"40", "Tỉnh Nghệ An"}, {"42", "Tỉnh Hà Tĩnh"},
        {"44", "Tỉnh Quảng Bình"}, {"45", "Tỉnh Quảng Trị"}, {"46", "Thành phố Huế"}, {"48", "Thành phố Đà Nẵng"},
        {"49", "Tỉnh Quảng Nam"}, {"51", "Tỉnh Quảng Ngãi"}, {"52", "Tỉnh Bình Định"}, {"54", "Tỉnh Phú Yên"},
        {"56", "Tỉnh Khánh Hòa"}, {"58", "Tỉnh Ninh Thuận"}, {"60", "Tỉnh Bình Thuận"}, {"62", "Tỉnh Kon Tum"},
        {"64", "Tỉnh Gia Lai"}, {"66", "Tỉnh Đắk Lắk"}, {"67", "Tỉnh Đắk Nông"}, {"68", "Tỉnh Lâm Đồng"},
        {"70", "Tỉnh Bình Phước"}, {"72", "Tỉnh Tây Ninh"}, {"74", "Tỉnh Bình Dương"}, {"75", "Tỉnh Đồng Nai"},
        {"77", "Tỉnh Bà Rịa - Vũng Tàu"}, {"79", "Thành phố Hồ Chí Minh"}, {"80", "Tỉnh Long An"}, {"82", "Tỉnh Tiền Giang"},
        {"83", "Tỉnh Bến Tre"}, {"84", "Tỉnh Trà Vinh"}, {"86", "Tỉnh Vĩnh Long"}, {"87", "Tỉnh Đồng Tháp"},
        {"89", "Tỉnh An Giang"}, {"91", "Tỉnh Kiên Giang"}, {"92", "Thành phố Cần Thơ"}, {"93", "Tỉnh Hậu Giang"},
        {"94", "Tỉnh Sóc Trăng"}, {"95", "Tỉnh Bạc Liêu"}, {"96", "Tỉnh Cà Mau"}
    };

    private final GHNConfig cauHinh;
    private final GHNApiClient ghnApiClient;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final OpenApiClient openApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile List<ShippingAddressDTO> cacheTinhMoi;
    private volatile long cacheTinhMoiLuc;
    private final java.util.Map<Integer, CapPhongXaMoi> cachePhuongXaMoi = new java.util.concurrent.ConcurrentHashMap<>();

    private record CapPhongXaMoi(List<ShippingAddressDTO> ds, long luc) {}

    public GHNService(
        GHNConfig cauHinh,
        GHNApiClient ghnApiClient,
        SanPhamChiTietRepository sanPhamChiTietRepository,
        OpenApiClient openApiClient
    ) {
        this.cauHinh = cauHinh;
        this.ghnApiClient = ghnApiClient;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.openApiClient = openApiClient;
    }

    public boolean daCauHinhGhn() {
        return cauHinh.daCauHinh();
    }


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
        if (!cauHinh.daCauHinh()) {
            return new ShippingFeeResponse(30000, "VND", "Phí vận chuyển tiêu chuẩn");
        }
        try {
            int phi = tinhPhi(req);
            return new ShippingFeeResponse(phi, "VND", "Phí vận chuyển được tính bởi GHN");
        } catch (Exception e) {
            return new ShippingFeeResponse(30000, "VND", "Phí vận chuyển tiêu chuẩn");
        }
    }

    public List<ShippingAddressDTO> getTinhThanhMoi() {
        long now = System.currentTimeMillis();
        List<ShippingAddressDTO> daCache = cacheTinhMoi;
        if (daCache != null && now - cacheTinhMoiLuc < CACHE_TTL_MS) {
            return daCache;
        }

        if (cauHinh.daCauHinh()) {
            try {
                JsonNode root = doc(ghnApiClient.get(PATH_PROVINCE_V3));
                kiemTraCode(root, "tỉnh/thành (mô hình 2 tầng)");
                List<ShippingAddressDTO> ketQua = new ArrayList<>();
                for (JsonNode n : root.path("data")) {
                    int id = n.path("_id").asInt(0);
                    String ten = n.path("name").asText("");
                    if (id > 0 && !ten.isBlank()) {
                        ketQua.add(new ShippingAddressDTO(id, ten));
                    }
                }
                sapXepTheoTen(ketQua);
                cacheTinhMoi = ketQua;
                cacheTinhMoiLuc = now;
                return ketQua;
            } catch (Exception ignored) {
            }
        }

        // Fallback: Tra cứu qua OpenApiClient (https://provinces.open-api.vn/api/v1/p/)
        try {
            String json = openApiClient.get("/api/v1/p/");
            JsonNode root = objectMapper.readTree(json);
            List<ShippingAddressDTO> ketQua = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode n : root) {
                    int id = n.path("code").asInt(0);
                    String ten = n.path("name").asText("");
                    if (id > 0 && !ten.isBlank()) {
                        ketQua.add(new ShippingAddressDTO(id, ten));
                    }
                }
            }
            if (!ketQua.isEmpty()) {
                sapXepTheoTen(ketQua);
                cacheTinhMoi = ketQua;
                cacheTinhMoiLuc = now;
                return ketQua;
            }
        } catch (Exception ignored) {
        }

        // Fallback danh sách 63 tỉnh thành Việt Nam trong bộ nhớ
        List<ShippingAddressDTO> ketQua = new ArrayList<>();
        for (String[] p : DS_63_TINH) {
            ketQua.add(new ShippingAddressDTO(Integer.parseInt(p[0]), p[1]));
        }
        sapXepTheoTen(ketQua);
        cacheTinhMoi = ketQua;
        cacheTinhMoiLuc = now;
        return ketQua;
    }

    public List<ShippingAddressDTO> getPhuongXaMoi(Integer provinceId) {
        if (provinceId == null) {
            throw new IllegalArgumentException("Thiếu mã tỉnh/thành để lấy danh sách phường/xã");
        }
        long now = System.currentTimeMillis();
        CapPhongXaMoi daCache = cachePhuongXaMoi.get(provinceId);
        if (daCache != null && now - daCache.luc() < CACHE_TTL_MS) {
            return daCache.ds();
        }

        if (cauHinh.daCauHinh()) {
            JsonNode root = null;
            try {
                root = doc(ghnApiClient.get(
                    PATH_WARD_V3 + "?province_id=" + provinceId + "&offset=0&limit=200"));
            } catch (RuntimeException loiKetNoi) {
                root = null;
            }
            if (root != null) {
                kiemTraCode(root, "phường/xã (mô hình 2 tầng)");
                List<ShippingAddressDTO> ketQua = new ArrayList<>();
                for (JsonNode n : root.path("data")) {
                    int id = n.path("_id").asInt(0);
                    String ten = n.path("name").asText("");
                    if (id > 0 && !ten.isBlank()) {
                        ketQua.add(new ShippingAddressDTO(id, ten));
                    }
                }
                sapXepTheoTen(ketQua);
                cachePhuongXaMoi.put(provinceId, new CapPhongXaMoi(ketQua, now));
                return ketQua;
            }
        }

        // Fallback: Tra cứu phường/xã qua OpenApiClient (https://provinces.open-api.vn/api/v1/p/)
        try {
            String json = openApiClient.get("/api/v1/p/" + provinceId + "?depth=3");
            JsonNode root = objectMapper.readTree(json);
            List<ShippingAddressDTO> ketQua = new ArrayList<>();
            JsonNode districts = root.path("districts");
            if (districts.isArray() && districts.size() > 0) {
                for (JsonNode d : districts) {
                    String tenQuan = d.path("name").asText("");
                    JsonNode wards = d.path("wards");
                    if (wards.isArray()) {
                        for (JsonNode w : wards) {
                            int code = w.path("code").asInt(0);
                            String ten = w.path("name").asText("");
                            if (code > 0 && !ten.isBlank()) {
                                ketQua.add(new ShippingAddressDTO(code, ten + (!tenQuan.isBlank() ? " (" + tenQuan + ")" : "")));
                            }
                        }
                    }
                }
            } else if (root.path("wards").isArray()) {
                for (JsonNode w : root.path("wards")) {
                    int code = w.path("code").asInt(0);
                    String ten = w.path("name").asText("");
                    if (code > 0 && !ten.isBlank()) {
                        ketQua.add(new ShippingAddressDTO(code, ten));
                    }
                }
            }
            if (!ketQua.isEmpty()) {
                sapXepTheoTen(ketQua);
                cachePhuongXaMoi.put(provinceId, new CapPhongXaMoi(ketQua, now));
                return ketQua;
            }
        } catch (Exception ignored) {
        }

        List<ShippingAddressDTO> ketQua = List.of(
            new ShippingAddressDTO(1, "Phường/Xã trung tâm"),
            new ShippingAddressDTO(2, "Khu vực nội thành")
        );
        cachePhuongXaMoi.put(provinceId, new CapPhongXaMoi(ketQua, now));
        return ketQua;
    }

    public List<ShippingServiceDTO> getDichVu(Integer toDistrictId) {
        if (!cauHinh.daCauHinh()) {
            return List.of(new ShippingServiceDTO(2, 2L, "Giao hàng tiêu chuẩn"));
        }
        if (toDistrictId == null) {
            throw new IllegalArgumentException("Thiếu mã quận/huyện đích để lấy dịch vụ vận chuyển");
        }
        if (cauHinh.getFromDistrictId() == null) {
            throw new IllegalArgumentException(
                "Chưa cấu hình địa chỉ gửi: thiếu biến môi trường GHN_FROM_DISTRICT_ID");
        }
        int shopId;
        try {
            shopId = Integer.parseInt(cauHinh.getShopId().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("GHN_SHOP_ID phải là mã số shop hợp lệ của GHN");
        }
        ObjectNode body = objectMapper.createObjectNode();
        body.put("shop_id", shopId);
        body.put("from_district", cauHinh.getFromDistrictId());
        body.put("to_district", toDistrictId);

        JsonNode root = doc(ghnApiClient.post(PATH_SERVICES, body.toString()));
        kiemTraCode(root, "dịch vụ vận chuyển");
        List<ShippingServiceDTO> ketQua = new ArrayList<>();
        for (JsonNode n : root.path("data")) {
            int serviceTypeId = n.path("service_type_id").asInt(0);
            if (serviceTypeId <= 0) {
                continue;
            }
            long serviceId = n.path("service_id").asLong(0);
            String ten = n.path("short_name").asText("");
            ketQua.add(new ShippingServiceDTO(serviceTypeId, serviceId, ten));
        }
        ketQua.sort(Comparator.comparing(ShippingServiceDTO::getServiceTypeId));
        return ketQua;
    }


    public int tinhPhi(ShippingFeeRequest req) {
        if (!cauHinh.daCauHinh()) {
            return 30000;
        }
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
        int serviceTypeId = req.getServiceTypeId() != null && req.getServiceTypeId() > 0
            ? req.getServiceTypeId()
            : cauHinh.getServiceTypeId();
        body.put("service_type_id", serviceTypeId);
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
