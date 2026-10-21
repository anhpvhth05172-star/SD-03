package com.example.datn.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GhnDiaChiMapper {

    private static final String PATH_PROVINCE = "/shiip/public-api/master-data/province";
    private static final String PATH_DISTRICT = "/shiip/public-api/master-data/district";
    private static final String PATH_WARD = "/shiip/public-api/master-data/ward";
    private static final String PATH_V2_PROVINCES = "/api/v2/p/";
    private static final String PATH_V2_WARD_SEARCH = "/api/v2/w/?province=%d&search=%s";
    private static final String PATH_V2_TO_LEGACIES = "/api/v2/w/%d/to-legacies/";
    private static final String PATH_V1_DEPTH2 = "/api/v1/?depth=2";

    private static final Pattern TIEN_TO = Pattern.compile(
        "^(tỉnh|thành phố|tp\\.?|thị xã|tx\\.?|thị trấn|tt\\.?|quận|huyện|phường|p\\.?|xã)\\s+");

    private final GHNApiClient ghnApiClient;
    private final OpenApiClient openApiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile List<TinhGhn> tinhGhn;
    private final Map<Integer, List<QuanGhn>> cacheQuan = new ConcurrentHashMap<>();
    private final Map<Integer, List<PhuongGhn>> cachePhuong = new ConcurrentHashMap<>();
    private volatile List<DongV1> dongV1;

    public GhnDiaChiMapper(GHNApiClient ghnApiClient, OpenApiClient openApiClient) {
        this.ghnApiClient = ghnApiClient;
        this.openApiClient = openApiClient;
    }

    public record KetQuaAnhXach(int tinhId, int quanId, String maPhuong, String tenPhuong) {}

    private record TinhGhn(int id, String ten) {}

    private record QuanGhn(int id, int tinhId, String ten) {}

    private record PhuongGhn(String ma, int quanId, String ten) {}

    private record DongV1(int maQuan, String tenQuan, int maTinh, String tenTinh) {}

    private record UngVien(int tinhId, int quanId, String maPhuong, String tenPhuong) {}

    private static class ChuaTimThayException extends IllegalArgumentException {
        ChuaTimThayException(String message) {
            super(message);
        }
    }

    public KetQuaAnhXach anhXach(String tenTinh, String tenPhuong) {
        if (isBlank(tenTinh) || isBlank(tenPhuong)) {
            throw new IllegalArgumentException(
                "Thiếu tên tỉnh/thành hoặc phường/xã để ánh xạ địa chỉ");
        }
        String khoaPhuong = chuanHoa(tenPhuong);
        List<UngVien> nhanh = timNhanh(chuanHoa(tenTinh), khoaPhuong);
        if (nhanh.size() == 1) {
            UngVien u = nhanh.get(0);
            return new KetQuaAnhXach(u.tinhId(), u.quanId(), u.maPhuong(), u.tenPhuong());
        }
        try {
            return anhXachQuaOpenApi(tenTinh, tenPhuong, khoaPhuong);
        } catch (ChuaTimThayException e) {
            if (nhanh.size() > 1) {
                throw new IllegalArgumentException(
                    "Phường/xã '" + tenPhuong + "' mơ hồ trong dữ liệu GHN"
                        + " (nhiều quận/huyện cùng tên). Vui lòng chọn địa chỉ cụ thể hơn");
            }
            throw e;
        }
    }

    private List<UngVien> timNhanh(String khoaTinh, String khoaPhuong) {
        List<UngVien> ketQua = new ArrayList<>();
        for (TinhGhn t : layTinhGhn()) {
            if (!chuanHoa(t.ten()).equals(khoaTinh)) {
                continue;
            }
            for (QuanGhn q : layQuan(t.id())) {
                for (PhuongGhn p : layPhuong(q.id())) {
                    if (chuanHoa(p.ten()).equals(khoaPhuong)) {
                        ketQua.add(new UngVien(t.id(), q.id(), p.ma(), p.ten()));
                        if (ketQua.size() > 50) {
                            return ketQua;
                        }
                    }
                }
            }
        }
        return ketQua;
    }

    private KetQuaAnhXach anhXachQuaOpenApi(String tenTinh, String tenPhuong, String khoaPhuong) {
        Integer maTinhMoi = null;
        for (JsonNode p : mangHoacData(doc(openApiClient.get(PATH_V2_PROVINCES)))) {
            if (chuanHoa(p.path("name").asText("")).equals(chuanHoa(tenTinh))) {
                maTinhMoi = p.path("code").asInt();
                break;
            }
        }
        if (maTinhMoi == null) {
            throw new ChuaTimThayException(
                "Chưa tìm thấy tỉnh/thành '" + tenTinh + "' trong dữ liệu địa chỉ hành chính");
        }

        String duongDanTim = String.format(PATH_V2_WARD_SEARCH, maTinhMoi,
            URLEncoder.encode(tenPhuong, StandardCharsets.UTF_8));
        List<JsonNode> trungTen = new ArrayList<>();
        for (JsonNode w : mangHoacData(doc(openApiClient.get(duongDanTim)))) {
            if (chuanHoa(w.path("name").asText("")).equals(khoaPhuong)) {
                trungTen.add(w);
            }
        }
        if (trungTen.isEmpty()) {
            throw new ChuaTimThayException(
                "Chưa tìm thấy phường/xã '" + tenPhuong + "' trong tỉnh/thành '" + tenTinh + "'");
        }
        if (trungTen.size() > 1) {
            throw new IllegalArgumentException(
                "Phường/xã '" + tenPhuong + "' có nhiều kết quả trong dữ liệu địa chỉ hành chính."
                    + " Vui lòng chọn địa chỉ cụ thể hơn");
        }

        int maPhuongMoi = trungTen.get(0).path("code").asInt();
        JsonNode legacies = doc(openApiClient.get(
            String.format(PATH_V2_TO_LEGACIES, maPhuongMoi)));
        if (!legacies.isArray() || legacies.isEmpty()) {
            throw new ChuaTimThayException(
                "Dữ liệu địa chỉ chưa có thông tin đơn vị hành chính cũ cho '" + tenPhuong
                    + "'. Vui lòng thử lại sau");
        }

        Map<Integer, DongV1> theoMaQuan = new HashMap<>();
        for (DongV1 d : layV1()) {
            theoMaQuan.put(d.maQuan(), d);
        }

        Map<Integer, UngVien> ungVienTheoQuan = new LinkedHashMap<>();
        Map<Integer, Integer> demTheoQuan = new LinkedHashMap<>();
        for (JsonNode row : legacies) {
            DongV1 d = theoMaQuan.get(row.path("district_code").asInt());
            if (d == null) {
                continue;
            }
            UngVien u = khopLegacy(d, chuanHoa(row.path("name").asText("")));
            if (u == null) {
                continue;
            }
            demTheoQuan.merge(u.quanId(), 1, Integer::sum);
            ungVienTheoQuan.putIfAbsent(u.quanId(), u);
        }
        if (demTheoQuan.isEmpty()) {
            throw new ChuaTimThayException(
                "GHN chưa hỗ trợ phường/xã '" + tenPhuong
                    + "' cho mô hình địa chỉ mới. Vui lòng báo quản trị viên");
        }

        int quanTotNhat = -1;
        int demToiDa = 0;
        boolean hoa = false;
        for (Map.Entry<Integer, Integer> e : demTheoQuan.entrySet()) {
            if (e.getValue() > demToiDa) {
                demToiDa = e.getValue();
                quanTotNhat = e.getKey();
                hoa = false;
            } else if (e.getValue() == demToiDa) {
                hoa = true;
            }
        }
        if (hoa) {
            throw new IllegalArgumentException(
                "Chưa xác định được quận/huyện cũ cho phường/xã '" + tenPhuong
                    + "'. Vui lòng báo quản trị viên");
        }
        UngVien chon = ungVienTheoQuan.get(quanTotNhat);
        return new KetQuaAnhXach(chon.tinhId(), chon.quanId(), chon.maPhuong(), chon.tenPhuong());
    }

    private UngVien khopLegacy(DongV1 d, String khoaPhuong) {
        for (TinhGhn t : layTinhGhn()) {
            if (!chuanHoa(t.ten()).equals(chuanHoa(d.tenTinh()))) {
                continue;
            }
            for (QuanGhn q : layQuan(t.id())) {
                if (!chuanHoa(q.ten()).equals(chuanHoa(d.tenQuan()))) {
                    continue;
                }
                for (PhuongGhn p : layPhuong(q.id())) {
                    if (chuanHoa(p.ten()).equals(khoaPhuong)) {
                        return new UngVien(t.id(), q.id(), p.ma(), p.ten());
                    }
                }
            }
        }
        return null;
    }

    private List<TinhGhn> layTinhGhn() {
        List<TinhGhn> hienTai = tinhGhn;
        if (hienTai != null) {
            return hienTai;
        }
        synchronized (this) {
            if (tinhGhn == null) {
                JsonNode root = doc(ghnApiClient.get(PATH_PROVINCE));
                kiemTraCode(root, "tỉnh/thành (GHN)");
                List<TinhGhn> ds = new ArrayList<>();
                for (JsonNode n : root.path("data")) {
                    int id = n.path("ProvinceID").asInt(0);
                    String ten = n.path("ProvinceName").asText("");
                    if (id > 0 && !ten.isBlank()) {
                        ds.add(new TinhGhn(id, ten));
                    }
                }
                tinhGhn = Collections.unmodifiableList(ds);
            }
            return tinhGhn;
        }
    }

    private List<QuanGhn> layQuan(int tinhId) {
        return cacheQuan.computeIfAbsent(tinhId, id -> {
            JsonNode root = doc(ghnApiClient.post(
                PATH_DISTRICT, "{\"province_id\":" + id + "}"));
            kiemTraCode(root, "quận/huyện (GHN)");
            List<QuanGhn> ds = new ArrayList<>();
            for (JsonNode n : root.path("data")) {
                int quanId = n.path("DistrictID").asInt(0);
                String ten = n.path("DistrictName").asText("");
                if (quanId > 0 && !ten.isBlank()) {
                    ds.add(new QuanGhn(quanId, id, ten));
                }
            }
            ds.sort(java.util.Comparator.comparingInt(QuanGhn::id));
            return Collections.unmodifiableList(ds);
        });
    }

    private List<PhuongGhn> layPhuong(int quanId) {
        return cachePhuong.computeIfAbsent(quanId, id -> {
            JsonNode root = doc(ghnApiClient.post(
                PATH_WARD, "{\"district_id\":" + id + "}"));
            kiemTraCode(root, "phường/xã (GHN)");
            List<PhuongGhn> ds = new ArrayList<>();
            for (JsonNode n : root.path("data")) {
                String ma = n.path("WardCode").asText("");
                String ten = n.path("WardName").asText("");
                if (!ma.isBlank() && !ten.isBlank()) {
                    ds.add(new PhuongGhn(ma, id, ten));
                }
            }
            ds.sort(java.util.Comparator.comparing(PhuongGhn::ma));
            return Collections.unmodifiableList(ds);
        });
    }

    private List<DongV1> layV1() {
        List<DongV1> hienTai = dongV1;
        if (hienTai != null) {
            return hienTai;
        }
        synchronized (this) {
            if (dongV1 == null) {
                JsonNode root = doc(openApiClient.get(PATH_V1_DEPTH2));
                List<DongV1> ds = new ArrayList<>();
                for (JsonNode t : mangHoacData(root)) {
                    int maTinh = t.path("code").asInt();
                    String tenTinh = t.path("name").asText("");
                    for (JsonNode q : t.path("districts")) {
                        ds.add(new DongV1(
                            q.path("code").asInt(),
                            q.path("name").asText(""),
                            maTinh,
                            tenTinh));
                    }
                }
                dongV1 = Collections.unmodifiableList(ds);
            }
            return dongV1;
        }
    }

    private static String chuanHoa(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        v = v.replaceAll("[–—]", "-");
        v = v.replaceAll("\\s*-\\s*", "-");
        v = v.replaceAll("\\s+", " ");
        for (int i = 0; i < 2; i++) {
            Matcher m = TIEN_TO.matcher(v);
            if (m.lookingAt()) {
                v = v.substring(m.end());
            }
        }
        return v.trim();
    }

    private JsonNode doc(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Dữ liệu địa chỉ hành chính trả về không hợp lệ");
        }
    }

    private static List<JsonNode> mangHoacData(JsonNode root) {
        List<JsonNode> ds = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(ds::add);
        } else {
            root.path("data").forEach(ds::add);
        }
        return ds;
    }

    private static void kiemTraCode(JsonNode root, String phan) {
        int code = root.path("code").asInt(0);
        if (code != 200) {
            throw new IllegalArgumentException(
                "Không tra cứu được " + phan + " từ GHN");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
