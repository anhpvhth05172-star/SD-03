# Design: Thống kê + Quản lý lịch làm việc — kết nối dữ liệu thật (BE + FE)

- Ngày: 2026-10-19
- Repo: SD-03 (Spring Boot `BE/` + Vue 3 `FE/sd03_fe/`), nhánh `ha-anh`
- Trạng thái hiện tại: 3 màn hình (Thống kê `/thong-ke`, Lịch làm việc `/lich-lam-viec`, Lịch ca làm `/lich-ca-lam`) **đã tồn tại** trong FE (commit `fb9a767`), route + sidebar + bố cục theo ảnh mẫu đã dựng, biểu đồ dùng component SVG nội bộ (`BarLineChart.vue`, `PieChart.vue`). **Toàn bộ số liệu đang là mock seed trên client** (`statisticsData.js`, `scheduleData.js`).

## 1. Mục tiêu (intent)

Thay tầng dữ liệu giả của 3 màn hình bằng **dữ liệu thật từ backend**: viết API tổng hợp thống kê tính từ dữ liệu hóa đơn/sản phẩm/khách hàng hiện có, tạo bảng + API cho ca làm/lịch làm việc, rồi nối FE gọi API. Giữ nguyên bố cục giao diện hiện tại (đã bám ảnh mẫu), màu sắc/brand hiện tại của web.

## 2. Mục tiêu phụ / không làm

**Goals:**
- G1. Migration sửa lỗi schema drift đã có (8 cột thiếu) — sửa màn hóa đơn đang 500.
- G2. API thống kê tổng hợp (`/api/thong-ke/*`) — read-only, không bảng mới.
- G3. Bảng `ca_lam`, `lich_lam_viec` + API CRUD + xếp ca tự động + đặt lại lịch + import hàng loạt.
- G4. FE: thay mock trong `statisticsData.js` / `scheduleData.js` bằng API calls; **không đổi bố cục**.
- G5. Unit test cho service BE mới (theo pattern test sẵn có trong `BE/src/test`).

**Non-goals / anti-goals:**
- Không sửa design/layout của bất kỳ màn hiện có nào; không đổi màu/brand/sidebar/header.
- Không thêm thư viện FE mới (biểu đồ SVG nội bộ sẵn, xuất Excel dùng `exceljs` + `utils/export` sẵn).
- Không sửa SecurityConfig / luồng auth; endpoint mới đi theo pattern `QuyenGuard` (header `X-Vai-Tro`) như `NhanVienController`.
- Không sửa cấu trúc bảng có sẵn ngoài G1; không sửa API/nghiệp vụ của các màn khác.
- Không cài scheduler refresh lúc 22:00 như ảnh mẫu (nút "Cập nhật doanh thu" = refetch thủ công — xử lý thật, không trang trí).
- Không giả lập/đoán số: mọi số hiển thị phải tính từ DB thật; nếu mảng dữ liệu trống thì hiển thị 0.

## 3. Ràng buộc kỹ thuật

- `ddl-auto=none` → schema thay đổi = file SQL trong `BE/src/main/resources/db/migration/`, **chạy tay bằng sqlcmd** (pattern `IF COL_LENGTH(...) IS NULL ALTER TABLE ...` như file sẵn có). Kết nối: `sqlcmd -S 127.0.0.1,1433 -U sa -P 12131415 -d PolyShoesDB -i <file>`.
- FE: Vue 3 + vue-router 5 + axios qua `src/api/http.js`; số liệu thật hiện có: 50 hóa đơn (2025-04-10 → 2026-09-21), 64 KH, 69 NV, trạng thái `hoa_don.trang_thai` là code UPPERCASE (`CHO_XAC_NHAN`, `DA_HOAN_THANH`, `DA_HUY`, `DA_HOAN_TIEN`, …) — FE hiển thị nhãn tiếng Việt, mapping nhãn phía BE.
- Pattern test hiện có: JUnit5 + Mockito (ví dụ `NhanVienImportServiceTest`).

## 4. Các phần "how" đã quyết

### G1 — Migration sửa drift (đã được duyệt)

File `migration_sua_schema_drift.sql`, thêm 8 cột thiếu (đối chiếu entity ↔ DB):
`hoa_don.da_xoa BIT NOT NULL DEFAULT 0` · `kich_co.mo_ta` · `mau_sac.mo_ta` · `kieu_dang.mo_ta` · `dot_giam_gia.mo_ta` · `xuat_xu.mo_ta` (NVARCHAR NULL) · `de_giay.ma_de_giay` · `than_giay.ma_than_giay` (NVARCHAR NULL, theo definition trong entity). Không DROP, không DELETE, không sửa dữ liệu cũ.

### G2 — API Thống kê (bảng mới: không; entity sẵn có: HoaDon, ChiTietHoaDon, KhachHang, NhanVien, SanPham, ThuongHieu, PhuongThucThanhToan/HinhThucThanhToan)

Controller `ThongKeController` + service `ThongKeService`. Dùng **native query liệt kê cột tường minh / constructor DTO** (không load entity HaDon qua JPQL trừ khi G1 đã chạy). Tham số chung: `tu`, `den` (yyyy-MM-dd), `thuongHieu` (tên, optional — FE đang chọn theo tên).

Công thức (quy ước, trừ khi có lý do đổi khi implement):
- **Không lọc thương hiệu**: `doanhThu = Σ tien_sau_giam_gia` của đơn `trang_thai NOT IN ('DA_HUY','DA_HOAN_TIEN')` có `ngay_tao` ∈ [tu, den]; `soDon` = COUNT đơn đó; `aov = doanhThu/soDon` (0 nếu soDon=0); `tienMat` =那些 đơn join `hinh_thuc_thanh_toan.ma_hinh_thuc IN ('TM','CASH')` → Σ; `tienChuyenKhoan = doanhThu − tienMat`; `sanPhamBan = Σ chi_tiet_hoa_don.so_luong` của các đơn trên; `khachMoi` = COUNT `khach_hang.ngay_tao` ∈ [tu,den].
- **Có lọc thương hiệu** (line-level): `doanhThu` = Σ `thanh_tien` dòng chi tiết thuộc TH (đơn không hủy/hoàn tiền); `soDon` = COUNT đơn chứa ≥1 dòng TH; `aov` = doanhThu/soDon; `sanPhamBan` = Σ `so_luong` dòng TH; `tienMat` = Σ theo từng đơn: `tien_sau_giam_gia × (tỷ trọng dòng TH trong đơn)` nếu đơn là tiền mặt; `tienChuyenKhoan` = còn lại; `khachMoi` không đổi.
- Nhãn tiếng Việt cho pie trạng thái, đúng thứ tự FE: Chờ xác nhận, Đã xác nhận, Chờ giao hàng, Đang giao hàng, Đã giao hàng, Đã hoàn thành, Đã hủy, Đã hoàn tiền (map từ 8 code DB).

Endpoints:
| Method + Path | Trả về |
|---|---|
| `GET /api/thong-ke/tong-quan` | 7 card: tongDoanhThu, tienMat, tienChuyenKhoan, tongDonHang, aov, sanPhamDaBan, khachMoi |
| `GET /api/thong-ke/chuoi?kieu=ngay\|tuan\|thang` | `[{nhan, giaTri}]` — **số sản phẩm bán** theo kỳ (chuỗi biểu đồ cột/đường) |
| `GET /api/thong-ke/chuoi-thuong-hieu?kieu=&theo=doanhThu\|soDon` | `[{ten, values}]` — chuỗi theo kỳ của **top 5 thương hiệu** (nạp biểu đồ "Đường" của panel Thương hiệu; thay luôn chuỗi nhiễu ngẫu nhiên `chaoMauNgauNhien` trong mock). FE tự gán màu theo thứ tự như hiện tại |
| `GET /api/thong-ke/thuong-hieu` | `[{ten, doanhThu, soDon}]` đã sort giảm dần, top 5 + "Thương hiệu khác" (FE đã có màu/legend) |
| `GET /api/thong-ke/chu-ky` | 4 dòng Hôm nay / Tuần này / Tháng này / Năm nay: doanhThu, soDon, aov, tangTruong (%) — **"kỳ trước" = cửa sổ độ dài bằng nhau ngay trước kỳ hiện tại** (đúng ngữ nghĩa mock FE hiện tại: hôm nay vs hôm qua; 7 ngày vs 7 ngày trước; từ mùng 1→hôm nay vs cửa sổ có độ dài bằng nhau ngay trước đó). Kỳ trước = 0 && kỳ này > 0 → +100% |
| `GET /api/thong-ke/trang-thai-don` | `[{ten, soLuong}]` (nhãn VI, 8 trạng thái) |
| `GET /api/thong-ke/nhan-vien?tim=&sort=&page=&size=` | Page DTO: `[{id, maNhanVien, ten, vaiTro, anh, don, spBan, doanhThu}]` — **tất cả nhân viên** (LEFT JOIN đơn, nhân viên chưa có đơn vẫn hiện, `don/spBan/doanhThu = 0` — đúng hành vi pill "{{n}} nhân viên" hiện tại); `tim` = LIKE mã/tên; `sort` ∈ `doanhThuCao\|doanhThuThap\|donNhieu` (dòng 0 tự nhiên chèn cuối); page server-side |
| `GET /api/thong-ke/san-pham?tim=&tonKho=&sort=&page=&size=` | Page DTO: `[{maSanPham, tenSanPham, tenThuongHieu, daBan, soTra, doanhThu, tonKho}]` — **tất cả sản phẩm** (LEFT JOIN, SP chưa bán vẫn hiện với `daBan=0` — pill "{{n}} sản phẩm" hiện tại = tổng SP); `daBan/doanhThu` từ chi tiết đơn hợp lệ trong khoảng; `soTra` = Σ so_luong dòng của đơn `DA_HOAN_TIEN`; `tonKho` = Σ `san_pham_chi_tiet.so_luong`; `tonKho` ∈ `''\|con(>50)\|sapHet(1..50)\|het(=0)`; `sort` ∈ `banChay\|doanhThu\|tonKho` (đúng values FE đang gửi) |

Xuất Excel (3 bảng): giữ **client-side** bằng `utils/export` trên dữ liệu API đã tải (bảng NV/SP export tất cả page: fetch size lớn rồi export).

**Quy tắc lọc thương hiệu `thuongHieu`** (bám đúng hành vi màn hiện tại — mock đang scale cards/chuoi/trang-thai theo tỉ trọng, KHÔNG lọc chu-ky/NV, và lọc SP client-side; bản API thay tỉ trọng xấp xỉ bằng tính toán line-level thật):

| Endpoint | Nhận `thuongHieu`? | Quy tắc |
|---|---|---|
| `tong-quan`, `chuoi`, `trang-thai-don` | Có (optional) | công thức line-level §G2; riêng `trang-thai-don` khi có TH = COUNT **đơn khác nhau chứa ≥1 dòng của TH** nhóm theo `trang_thai` (đếm thật, thay quy mô tỉ trọng xấp xỉ của mock) |
| `chu-ky`, `nhan-vien` | **Không** | giữ nguyên như màn hiện tại (không ảnh hưởng bởi chọn TH) |
| `san-pham` | Có (optional) | lọc server-side theo tên TH; FE bỏ đoạn filter client `sanPhamLoc` |
| `thuong-hieu`, `chuoi-thuong-hieu` | Không | panel hiển thị bảng phân bổ tất cả TH như hiện tại |

**Quy tắc `da_xoa`:** sau migration G1, mọi công thức thống kê chỉ tính đơn với `da_xoa = 0` (đơn đã xóa mềm không vào số liệu) — kết hợp với loại `DA_HUY`/`DA_HOAN_TIEN` ở các công thức doanh thu ở trên.

### G3 — Bảng + API Lịch làm việc / Ca làm

Migration `migration_lich_lam_viec.sql`:
```sql
ca_lam(id BIGINT IDENTITY PK, ma_ca NVARCHAR(50) UNIQUE NOT NULL, ten_ca NVARCHAR(100) NOT NULL,
       bat_dau TIME NOT NULL, ket_thuc TIME NOT NULL, dang_bat BIT NOT NULL DEFAULT 1,
       ngay_tao DATETIME2, ngay_cap_nhat DATETIME2, nguoi_tao NVARCHAR(100), nguoi_cap_nhat NVARCHAR(100))
lich_lam_viec(id BIGINT IDENTITY PK, id_nhan_vien BIGINT NOT NULL FK→nhan_vien,
       id_ca_lam BIGINT NOT NULL FK→ca_lam, ngay_lam DATE NOT NULL,
       ngay_tao/ngay_cap_nhat DATETIME2, nguoi_tao/nguoi_cap_nhat NVARCHAR(100),
       CONSTRAINT UQ_lich UNIQUE(id_nhan_vien, id_ca_lam, ngay_lam))
```
Seed 3 ca mặc định (INSERT nếu chưa có): `sang` Ca sáng 08:00–12:00, `chieu` Ca chiều 13:00–17:00, `toi` Ca tối 17:30–21:30, `dang_bat=1` (đúng ảnh mẫu).

**Ca làm** — `CaLamController` `/api/ca-lam`: `GET` (list), `POST` (validate trùng tên, overlap khung giờ), `PUT /{id}`, `PUT /{id}/trang-thai` (toggle), `DELETE /{id}` (**chặn 409 nếu còn lịch gán** — FK + thông báo thân thiện thay vì lỗi thô).

**Lịch làm việc** — `LichLamViecController` `/api/lich-lam-viec`:
| Endpoint | Hành vi |
|---|---|
| `GET ?tu=&den=&tim=&idCa=&ngay=` | list lọc server-side (tim = LIKE mã/tên NV); FE giữ phân trang client như hiện tại |
| `POST` | tạo 1 — validate NV tồn tại, ca tồn tại, `(nv, ngày, ca)` chưa trùng (mirror `kiemTraTrung`) |
| `PUT /{id}` | sửa — validate như trên, bỏ qua id hiện tại |
| `DELETE /{id}` | xóa 1 |
| `DELETE ?ngay=yyyy-MM-dd` | **Đặt lại lịch**: xóa toàn bộ lịch của ngày |
| `POST /xep-tu-dong` body `{ngay}` | port thuật toán `xepCaTuDong` hiện có (sang→chiều→toi, NV đang hoạt động chưa có lịch ngày đó, round-robin); trả `{lyDo?: 'khongCa'\|'khongUngVien', themMoi}` — FE giữ nguyên nhánh notify |
| `POST /import` body `{rows:[{maNhanVien, maCa, ngay}]}` | bulk insert 1 transaction; BE validate lại từng dòng (NV/ca không tồn tại, ca tắt, trùng lịch có sẵn hoặc trong batch) → trả `{themMoi, loi:[{dong, lyDo}]}` |

**Import/Tải template**: FE đã parse Excel + preview client bằng `exceljs` và sinh template (`xuatMauLichLamViec`) — **giữ nguyên phía FE** (template tĩnh, không phải dữ liệu); chỉ phần confirm gọi `POST /import`.

### G4 — FE nối API

- Mới: `src/api/statistics.js`, `src/api/schedule.js` (pattern `src/api/employee.js`).
- `statisticsData.js`: **xóa** generator mock (`taoBangNgay`, `tinhKhoang`, `tinhChuoiBanHang`, `tinhChuKy`, `tinhTrangThaiDon`, `tinhSanPham`, `tinhBangThuongHieu`, `tinhNhanVien`, `SAN_PHAM_MAU`, `MAU_*`); **giữ** helper thuần + không phụ thuộc dữ liệu: `khoaNgay`, `docKhoa`, `khoangNhanh`, `dauNgay`, `cuoiNgay`, `layTangTruongLabel`, `TRANG_THAI_DON`.
- `StatisticsPage.vue`: bộ lọc/quicks đổi → gọi API; `watch` [tuNgay, denNgay, thuongHieuChon, kieuTongQuan] → fetch `tong-quan` + `chuoi` + `chuoi-thuong-hieu` (kèm `the=thTheo`) + `thuong-hieu` + `trang-thai-don`; **`chu-ky` independent khỏi bộ lọc** (luôn tính theo "hôm nay" như mock hiện tại) → fetch lúc mount + nút "Cập nhật doanh thu"; bảng NV/SP: `watch` [kw, sort, page, size, khoảng ngày, (thuongHieu cho SP)] → fetch paged; "Cập nhật doanh thu" = refetch toàn bộ + cập label giờ; bỏ text "Tự động lúc 22:00".
- `scheduleData.js`: xóa `nhanVienMau` + store in-memory; giữ helper ngày/giờ thuần (`khoaNgay`, `docNgay`, `hienThiNgay`, `slugMaCa`…). Trang tự fetch NV thật (`listEmployees` — đã có sẵn).
- `WorkSchedulePage.vue`: load list khi mount + khi đổi bộ lọc/ngày; form thêm/sửa → POST/PUT; xóa → DELETE; đặt lại lịch → DELETE ?ngay (có confirm như hiện tại); xếp ca tự động → POST /xep-tu-dong; import confirm → POST /import; render Bảng/Lịch (ngày/tuần/tháng) như cũ trên dữ liệu API (fetch theo khoảng ngày đang xem).
- `ShiftSchedulePage.vue`: list/toggle/sửa/xóa ca qua API.
- Nếu `listEmployees` trả trang nhỏ → fetch trang lớn (size=1000) cho combobox NV.

### G5 — Kiểm thử / verify

- Unit test mới (Mockito): `ThongKeServiceTest` (công thức doanh thu/tiền mặt/brand filter line-level/trang-thai theo TH/tăng trưởng cửa sổ bằng độ dài/loại `da_xoa=0`), `LichLamViecServiceTest` (trùng lịch, validate import), `xepTuDongTest`, `CaLamServiceTest` (trùng tên/overlap/chặn xóa) — RED → code → GREEN theo TDD.
- Migration: chạy sqlcmd, verify `sys.columns` còn 0 column thiếu.
- Smoke curl toàn bộ endpoint mới; `GET /api/hoa-don` trả 200 sau G1.
- FE: `npm.cmd run build` + chạy tay 3 màn trên `localhost:5173` đối chiếu ảnh mẫu; console không lỗi.

## 5. Những gì để builder tự quyết

- Cấu trúc DTO/Exception/response theo pattern hiện có trong repo (`PageResponse`, `GlobalExceptionHandler`, `QuyenGuard`).
- Công thức override nếu thực tế SQL bất tiện (phải giữ nguyên ý nghĩa quy ước §G2 và ghi chú trong code).
- thứ tự triển khai, tên file, cách chia method.
- Các option FE khác phát hiện khi nối API (mismatch giá trị) → ưu tiên **giữ FE đổi minimal**, điều chỉnh param phía BE cho khớp FE đang gửi.
