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
        {"1", "Thành phố Hà Nội"}, {"4", "Tỉnh Cao Bằng"}, {"8", "Tỉnh Tuyên Quang"}, {"11", "Tỉnh Điện Biên"},
        {"12", "Tỉnh Lai Châu"}, {"14", "Tỉnh Sơn La"}, {"15", "Tỉnh Lào Cai"}, {"19", "Tỉnh Thái Nguyên"},
        {"20", "Tỉnh Lạng Sơn"}, {"22", "Tỉnh Quảng Ninh"}, {"24", "Tỉnh Bắc Ninh"}, {"25", "Tỉnh Phú Thọ"},
        {"31", "Thành phố Hải Phòng"}, {"33", "Tỉnh Hưng Yên"}, {"37", "Tỉnh Ninh Bình"}, {"38", "Tỉnh Thanh Hóa"},
        {"40", "Tỉnh Nghệ An"}, {"42", "Tỉnh Hà Tĩnh"}, {"44", "Tỉnh Quảng Trị"}, {"46", "Thành phố Huế"},
        {"48", "Thành phố Đà Nẵng"}, {"51", "Tỉnh Quảng Ngãi"}, {"52", "Tỉnh Gia Lai"}, {"56", "Tỉnh Khánh Hòa"},
        {"66", "Tỉnh Đắk Lắk"}, {"68", "Tỉnh Lâm Đồng"}, {"75", "Tỉnh Đồng Nai"}, {"79", "Thành phố Hồ Chí Minh"},
        {"80", "Tỉnh Tây Ninh"}, {"82", "Tỉnh Đồng Tháp"}, {"86", "Tỉnh Vĩnh Long"}, {"91", "Tỉnh An Giang"},
        {"92", "Thành phố Cần Thơ"}, {"96", "Tỉnh Cà Mau"}







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
        if (cauHinh.daCauHinh()) {
            try {
                int phi = tinhPhi(req);
                return new ShippingFeeResponse(phi, "VND", "Phí vận chuyển được tính bởi GHN");
            } catch (Exception ex) {
                // GHN API timeout, lỗi token hoặc địa chỉ không khớp -> tự động chuyển sang biểu phí khu vực
            }
        }
        int phiKhuVuc = tinhPhiChuanTheoKhuVuc(req);
        return new ShippingFeeResponse(phiKhuVuc, "VND", "Phí giao hàng GHN Express (Tính theo cự ly khu vực)");
    }

    public int tinhPhiChuanTheoKhuVuc(ShippingFeeRequest req) {
        String prov = req != null ? chuanHoaDiaChi(req.getToProvinceName()) : "";
        String ward = req != null ? chuanHoaDiaChi(req.getToWardName()) : "";

        // 1. Xác định cước cơ bản theo khoảng cách từ kho (Bắc Từ Liêm, Hà Nội) đến tỉnh/thành đích
        int cuocCoBan = 35000;

        if (prov.contains("ha noi")) {
            // Hà Nội: Kiểm tra quận/huyện nội hay ngoại thành
            boolean laNoiThanh = ward.contains("ba dinh") || ward.contains("hoan kiem")
                || ward.contains("dong da") || ward.contains("hai ba trung")
                || ward.contains("cau giay") || ward.contains("thanh xuan")
                || ward.contains("tay ho") || ward.contains("bac tu liem")
                || ward.contains("nam tu liem") || ward.contains("long bien")
                || ward.contains("bo de") || ward.contains("gia thuy") || ward.contains("ngoc lam")
                || ward.contains("hoang mai") || ward.contains("ha dong");

            boolean laNgoaiThanhXa = ward.contains("ba vi") || ward.contains("son tay")
                || ward.contains("ung hoa") || ward.contains("my duc")
                || ward.contains("chuong my") || ward.contains("thuong tin")
                || ward.contains("phu xuyen") || ward.contains("quoc oai")
                || ward.contains("thach that") || ward.contains("phuc tho");

            if (laNoiThanh) {
                cuocCoBan = 22000; // Nội thành Hà Nội chuẩn GHN
            } else if (laNgoaiThanhXa) {
                cuocCoBan = 28000; // Ngoại thành xa Hà Nội
            } else {
                cuocCoBan = 25000; // Ngoại thành 1 (Gia Lâm, Đông Anh, Sóc Sơn, Mê Linh, Hoài Đức...)
            }
        } else if (prov.contains("bac ninh") || prov.contains("hung yen")
            || prov.contains("ha nam") || prov.contains("vinh phuc")) {
            cuocCoBan = 28000; // Các tỉnh giáp ranh Hà Nội
        } else if (prov.contains("hai phong") || prov.contains("hai duong")
            || prov.contains("quang ninh") || prov.contains("nam dinh")
            || prov.contains("ninh binh") || prov.contains("thai binh")
            || prov.contains("phu tho") || prov.contains("thai nguyen")) {
            cuocCoBan = 32000; // Đồng bằng sông Hồng & Duyên hải Bắc Bộ
        } else if (prov.contains("hoa binh") || prov.contains("bac kan") || prov.contains("bac can")
            || prov.contains("lang son") || prov.contains("tuyen quang")
            || prov.contains("yen bai") || prov.contains("lao cai")
            || prov.contains("cao bang") || prov.contains("ha giang")) {
            cuocCoBan = 35000; // Miền núi phía Bắc
        } else if (prov.contains("son la") || prov.contains("dien bien") || prov.contains("lai chau")) {
            cuocCoBan = 38000; // Vùng cao Tây Bắc
        } else if (prov.contains("thanh hoa") || prov.contains("nghe an") || prov.contains("ha tinh")) {
            cuocCoBan = 35000; // Bắc Trung Bộ
        } else if (prov.contains("da nang")) {
            cuocCoBan = 36000; // TP. Đà Nẵng
        } else if (prov.contains("quang binh") || prov.contains("quang tri")
            || prov.contains("hue") || prov.contains("thua thien")
            || prov.contains("quang nam") || prov.contains("quang ngai")
            || prov.contains("binh dinh") || prov.contains("phu yen")
            || prov.contains("khanh hoa") || prov.contains("nha trang")
            || prov.contains("ninh thuan") || prov.contains("binh thuan")) {
            cuocCoBan = 38000; // Duyên hải Nam Trung Bộ
        } else if (prov.contains("kon tum") || prov.contains("gia lai")
            || prov.contains("dak lak") || prov.contains("dac lac")
            || prov.contains("dak nong") || prov.contains("dac nong")
            || prov.contains("lam dong") || prov.contains("da lat")) {
            cuocCoBan = 42000; // Tây Nguyên
        } else if (prov.contains("ho chi minh") || prov.contains("sai gon") || prov.contains("tphcm")) {
            boolean laNgoaiThanhHcm = ward.contains("hoc mon") || ward.contains("cu chi")
                || ward.contains("binh chanh") || ward.contains("nha be") || ward.contains("can gio");
            cuocCoBan = laNgoaiThanhHcm ? 42000 : 38000; // TP. Hồ Chí Minh
        } else if (prov.contains("binh duong") || prov.contains("dong nai")
            || prov.contains("ba ria") || prov.contains("vung tau")) {
            cuocCoBan = 40000; // Đông Nam Bộ phát triển
        } else if (prov.contains("tay ninh") || prov.contains("binh phuoc")) {
            cuocCoBan = 42000; // Đông Nam Bộ biên giới
        } else if (prov.contains("long an") || prov.contains("tien giang")
            || prov.contains("ben tre") || prov.contains("vinh long")
            || prov.contains("tra vinh") || prov.contains("can tho")) {
            cuocCoBan = 45000; // Đồng bằng sông Cửu Long (Tiền Giang - Hậu Giang)
        } else if (prov.contains("dong thap") || prov.contains("an giang")
            || prov.contains("hau giang") || prov.contains("soc trang") || prov.contains("bac lieu")) {
            cuocCoBan = 48000; // Miền Tây sông nước
        } else if (prov.contains("kien giang") || prov.contains("phu quoc") || prov.contains("ca mau")) {
            cuocCoBan = 52000; // Cực Nam & Đảo xa
        }

        // 2. Tính trọng lượng đơn hàng (nếu có items)
        long tongTrongLuong = 500; // mặc định 1 đôi giày 500g
        if (req != null && req.getItems() != null && !req.getItems().isEmpty()) {
            tongTrongLuong = 0;
            for (ShippingFeeRequest.DongHang d : req.getItems()) {
                if (d != null && d.soLuong() != null && d.soLuong() > 0) {
                    long tlItem = 500;
                    if (d.idSanPhamChiTiet() != null) {
                        try {
                            SanPhamChiTiet sp = sanPhamChiTietRepository.findById(d.idSanPhamChiTiet()).orElse(null);
                            if (sp != null && sp.getTrongLuong() != null && sp.getTrongLuong().longValue() > 0) {
                                tlItem = sp.getTrongLuong().longValue();
                            }
                        } catch (Exception ignored) {}
                    }
                    tongTrongLuong += tlItem * d.soLuong();
                }
            }
            if (tongTrongLuong <= 0) tongTrongLuong = 500;
        }

        // Cứ mỗi 500g trên 1kg (1000g), cộng thêm 5.000đ theo cước bậc lũy tiến GHN
        if (tongTrongLuong > 1000) {
            long vuot = tongTrongLuong - 1000;
            long soBac = (vuot + 499) / 500;
            cuocCoBan += (int) (soBac * 5000);
        }

        // 3. Xử lý loại dịch vụ vận chuyển
        if (req != null && req.getServiceTypeId() != null) {
            if (req.getServiceTypeId() == 1) {
                // Hỏa tốc / Express
                cuocCoBan = (int) Math.round(cuocCoBan * 1.35);
            } else if (req.getServiceTypeId() == 5) {
                // Tiết kiệm
                cuocCoBan = (int) Math.round(cuocCoBan * 0.9);
            }
        }

        // Làm tròn tới hàng nghìn
        return (cuocCoBan / 1000) * 1000;
    }

    private String chuanHoaDiaChi(String input) {
        if (input == null) return "";
        String s = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .replace('đ', 'd').replace('Đ', 'D')
            .trim().toLowerCase();
        return s.replaceAll("^(thanh pho|tp\\.?|tinh|quan|huyen|thi xa|tx\\.?|phuong|xa|thi tran)\\s+", "").trim();
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
            String json = openApiClient.get("/api/v2/p/");
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

        RuntimeException loiV3 = null;
        if (cauHinh.daCauHinh()) {
            try {
                JsonNode root = doc(ghnApiClient.get(
                    PATH_WARD_V3 + "?province_id=" + provinceId + "&offset=0&limit=200"));
                kiemTraCode(root, "phường/xã (mô hình 2 tầng)");
                List<ShippingAddressDTO> ketQua = new ArrayList<>();
                for (JsonNode n : root.path("data")) {
                    int id = n.path("_id").asInt(0);
                    String ten = n.path("name").asText("");
                    if (id > 0 && !ten.isBlank()) {
                        ketQua.add(new ShippingAddressDTO(id, ten));
                    }
                }
                if (!ketQua.isEmpty()) {
                    sapXepTheoTen(ketQua);
                    cachePhuongXaMoi.put(provinceId, new CapPhongXaMoi(ketQua, now));
                    return ketQua;
                }
            } catch (RuntimeException ex) {
                loiV3 = ex;
            }
        }

        // Fallback: Tra cứu phường/xã qua OpenApiClient (https://provinces.open-api.vn/api/v1/p/)
        try {
            String json = openApiClient.get("/api/v2/p/" + provinceId + "?depth=2");
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

        if (loiV3 != null && provinceId >= 1000000) {
            throw loiV3;
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
            : tongTrongLuong >= 20_000 ? 5 : 2;
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
