# Thiết kế chức năng Giao/Kết ca — PolyShoes POS

Ngày: 2026-10-21 · Nhánh: `hoang-anh` · Trạng thái: draft chờ duyệt

## 1. Mục tiêu & phạm vi

Quy trình khép kín: **mở ca → bán hàng → kết ca → đối soát tiền mặt → (bàn giao) → lưu lịch sử**.

Phân 3 phase để giảm rủi ro (theo kết luận trong phân tích nghiệp vụ):

| Phase | Nội dung | Trạng thái ca |
|---|---|---|
| **P1 (làm trước)** | Mở ca, liên kết HĐ↔ca, tổng hợp số liệu, kết ca + đối soát tiền mặt | `DANG_MO` → `DA_KET_CA` |
| **P2** | Bàn giao ca (người giao/người nhận, chênh lệch bàn giao) | + `CHO_BAN_GIAO`, `DA_BAN_GIAO` |
| **P3 (tùy chọn)** | Thu/chi khác trong ca, chính sách quản lý duyệt | + `CHO_XAC_NHAN` |

Cửa hàng 1 quầy, không yêu cầu quản lý duyệt kết ca → P1 bỏ `CHO_XAC_NHAN` (ruling R5).

## 2. Quyết định chốt (ruling) — chỉnh phần này nếu bạn không đồng ý

| # | Quyết định | Lý do |
|---|---|---|
| **R1** | Gộp **đối soát** vào bảng ca (1:1): cột `tien_du_kien, tien_thuc_te, chenh_lech, ly_do_lech` nằm trong `ca_lam_viec`, không tách bảng `doi_soat_ca` | Mỗi ca chỉ có 1 kết quả đối soát chính thức; đơn giản, chống bấm nút nhiều lần |
| **R2** | Tạo bảng `lich_su_ca` (ca, hành động, người thực hiện, thời điểm, dữ liệu trước/sau JSON, ghi chú) | Yêu cầu lưu vết mọi thao tác mở/kết/bàn giao |
| **R3** | Liên kết HĐ↔ca bằng cột `id_ca_lam_viec` (nullable) trên **`hoa_don`**; BE tự resolve ca `DANG_MO` của NV đang đăng nhập khi tạo HĐ tại quầy, **không tin FE gửi idCa** | Đúng nguyên tắc "backend tự tổng hợp, không suy luận theo ngày tạo"; HĐ cũ để NULL |
| **R4** | Quy đổi tiền mặt: `id_phuong_thuc_thanh_toan ∈ (1, 19)` (Tiền mặt tại quầy / Tiền mặt) là tiền mặt; còn lại (CK, QR, VNPay, MoMo…) là không tiền mặt | Dùng bảng `phuong_thuc_thanh_toan` sẵn có |
| **R5** | Trạng thái P1 chỉ `DANG_MO` và `DA_KET_CA` (bỏ `CHO_XAC_NHAN`); P2 thêm `CHO_BAN_GIAO`/`DA_BAN_GIAO` | Cửa hàng không duyệt kết ca; bàn giao là bước riêng, không bắt buộc |
| **R6** | **1 quầy duy nhất** (`ten_quy` default `Quầy tổng`); ràng buộc: 1 NV tối đa 1 ca mở **và** toàn hệ thống tối đa 1 ca `DANG_MO` | Đúng thực tế cửa hàng 1 quầy; đa quầy để sau |
| **R7** | Điều kiện kết ca: mọi HĐ thuộc ca phải ở trạng thái cuối (`Hoàn thành` / `Đã hủy` / `Đã hoàn tiền`) — không có HĐ dở dang | POS chỉ tạo HĐ khi checkout xong nên mặc định thỏa; vẫn kiểm trên BE vì HĐ có thể tạo từ admin |
| **R8** | Tiền mặt dự kiến = `tien_dau_ca + tien_mat_thu − tien_hoan_mat` (P1 chưa có thu/chi khác). Hoàn tiền: HĐ trạng thái `Đã hoàn tiền` + PT mặt → trừ vào tiền mặt | Công thức chuẩn; `hoàn` ≠ `hủy` |
| **R9** | Thu/chi khác trong ca → **P3** (bảng `thu_chi_trong_ca` + API + nút trong POS) | Không đưa vào P1 để không phình scope |
| **R10** | Quyền: NV thao tác ca của mình; vai trò Admin (id 1) xem tất cả +结 ca thay | Không tin vai trò do FE gửi — check trên BE từ session |
| **R11** | Màn "Ca làm việc"/"Lịch ca làm" hiện tại là **mock FE (localStorage)** — không liên kết, không sửa; `Giao ca` là module độc lập, bảng mới自己的 | Tránh đụng 2 màn đang có |
| **R12** | Tiền đầu ca **nhập tay** khi mở ca, không tự copy số dư ca trước (phải có bước xác nhận ở P2 bàn giao) | Theo yêu cầu: không tự lấy số dư cuối ca làm đầu ca mới |
| **R13** | **Chặn thanh toán khi chưa mở ca**: checkout `TAI_QUAY` khi không có ca `DANG_MO` → BE trả 400 `CA_CHUA_MO`; FE hiện modal yêu cầu mở ca. Online không chặn | Đã chốt với chủ shop: đối soát phải đủ sổ, không chấp nhận HĐ lọt ngoài ca |
| **R14** | Nhận diện tiền mặt theo **tên** PT (`ten_phuong_thuc` chứa N'Tiền mặt'), **không hardcode id** 1/19 | DB seed PT khác nhau giữa môi trường; FE fallback đang hardcode id 1/2 |
| **R15** | Quyền dùng **authority** `ROLE_ADMIN`/`ROLE_NHAN_VIEN` từ JWT (pattern ChatController), không hardcode `vai_tro id=1` | Codebase đã có pattern auth; không tin id vai trò FE gửi |
| **R16** | Endpoint xem lịch sử thao tác: **embed** danh sách `lich_su_ca` vào response `GET /api/ca/{id}/doi-soat`, không thêm endpoint riêng | FE modal chi tiết cần 1 lần gọi là đủ |
| **R17** | R7 (không có HĐ dở dang) chỉ áp cho HĐ **`loai_don = TAI_QUAY`**; HĐ online/giao hàng bỏ qua | Checkout tại POS tạo HĐ online dính vào ca → kẹt kết ca nhiều ngày |
| **R18** | Nguồn tính doanh thu/thu: cột **`tien_sau_giam_gia`** (không phải `tong_tien`) | Đã trừ giảm giá = số tiền thực thu |
| **R19** | Migration thêm **filtered unique index**: 1 ca `DANG_MO` per NV và 1 ca `DANG_MO` toàn hệ thống (bên cạnh check trong service) | Chống race ở tầng DB |
| **R20** | BE **override `id_nhan_vien` từ session** cho đơn `TAI_QUAY` thay vì nhận FE hardcode (`allEmployees[0].id \|\| 2`) | Đã chốt: trang web mặc định là trang khách hàng, phải đăng nhập mới vào màn quản lý → lấy NV từ session |
| **R21** | **Hoàn tiền tại quầy: CÓ trong P1.** Mở `TRANSITION_MAP_POS`: `DA_HOAN_THANH → [DA_HOAN_TIEN]` (hiện đang rỗng); dùng endpoint chuyển trạng thái HĐ có sẵn (không API mới). `tien_hoan_mat` = sum HĐ `DA_HOAN_TIEN` + PT mặt **thuộc ca đó**; hoàn sau khi ca đã kết không tính lại sổ ca cũ | Đã chốt với chủ shop; làm POS refund = 1 dòng transition + test |

## 3. Thiết kế dữ liệu

```sql
-- P1
CREATE TABLE ca_lam_viec (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  ma_ca VARCHAR(30) NOT NULL UNIQUE,          -- GC-yyyymmdd-HHmmss-nv
  id_nhan_vien BIGINT NOT NULL REFERENCES nhan_vien(id),
  ten_quy NVARCHAR(50) NOT NULL DEFAULT N'Quầy tổng',
  thoi_gian_mo DATETIME2 NOT NULL,
  thoi_gian_ket DATETIME2 NULL,
  tien_dau_ca DECIMAL(18,2) NOT NULL CHECK (tien_dau_ca >= 0),
  trang_thai VARCHAR(20) NOT NULL,            -- DANG_MO | DA_KET_CA
  -- đối soát (1:1, đi kèm kết ca)
  tong_hoa_don INT NULL,
  tong_doanh_thu DECIMAL(18,2) NULL,          -- HĐ hiệu lực (không tính hủy/hoàn)
  tien_mat_thu DECIMAL(18,2) NULL,
  tien_khong_mat DECIMAL(18,2) NULL,
  tien_hoan_mat DECIMAL(18,2) NULL,
  tien_du_kien DECIMAL(18,2) NULL,            -- dau_ca + thu - hoan
  tien_thuc_te DECIMAL(18,2) NULL,            -- NV kiểm đếm
  chenh_lech DECIMAL(18,2) NULL,              -- thuc_te - du_kien
  ly_do_lech NVARCHAR(255) NULL,              -- bắt buộc khi chenh_lech != 0
  ghi_chu_mo NVARCHAR(255) NULL,
  da_xoa BIT NOT NULL DEFAULT 0,
  ngay_tao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  ngay_cap_nhat DATETIME2 NULL,
  nguoi_cap_nhat NVARCHAR(100) NULL
);

ALTER TABLE hoa_don ADD id_ca_lam_viec BIGINT NULL REFERENCES ca_lam_viec(id);

CREATE TABLE lich_su_ca (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  id_ca_lam_viec BIGINT NOT NULL REFERENCES ca_lam_viec(id),
  hanh_dong VARCHAR(30) NOT NULL,             -- MO_CA | KET_CA | BAN_GIAO | ...
  id_nhan_vien BIGINT NOT NULL REFERENCES nhan_vien(id),
  thoi_gian DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  truoc_sau NVARCHAR(MAX) NULL,               -- JSON snapshot
  ghi_chu NVARCHAR(255) NULL
);

-- P2
CREATE TABLE ban_giao_ca (
  id BIGINT IDENTITY(1,1) PRIMARY KEY,
  id_ca_lam_viec BIGINT NOT NULL UNIQUE REFERENCES ca_lam_viec(id),
  id_nguoi_giao BIGINT NOT NULL REFERENCES nhan_vien(id),
  id_nguoi_nhan BIGINT NOT NULL REFERENCES nhan_vien(id),
  tien_giao DECIMAL(18,2) NOT NULL,           -- người giao khai
  tien_nhan DECIMAL(18,2) NULL,               -- người nhận kiểm đếm
  chenh_lech_ban_giao DECIMAL(18,2) NULL,
  trang_thai VARCHAR(20) NOT NULL,            -- CHO_BAN_GIAO | DA_BAN_GIAO
  ghi_chu NVARCHAR(255) NULL,
  thoi_gian_giao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  thoi_gian_nhan DATETIME2 NULL,
  ngay_tao DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);

-- P3 (không làm trong đợt này)
-- CREATE TABLE thu_chi_trong_ca (...)
```

## 4. API (P1)

| Method + Path | Chức năng | Validate chính |
|---|---|---|
| `POST /api/ca/mo-ca` | Mở ca (body: `tienDauCa`, `ghiChu`) | NV chưa có ca `DANG_MO`; hệ thống chưa có ca `DANG_MO` (R6); `tienDauCa >= 0` |
| `GET /api/ca/dang-mo` | Ca đang mở của NV đăng nhập | Trả `null` nếu chưa mở |
| `GET /api/ca/{id}/tong-ket` | Tổng hợp HĐ + tiền dự kiến (chưa chốt) | Chỉ xem được ca của mình hoặc Admin |
| `POST /api/ca/{id}/ket-ca` | Kết ca (body: `tienThucTe`, `lyDoLech`, `ghiChu`) | Ca `DANG_MO`; R7 không có HĐ dở; `tienThucTe >= 0`; `lyDoLech` bắt buộc khi lệch ≠ 0; **cập nhật có điều kiện** `WHERE trang_thai='DANG_MO'` (chống 2 request đồng thời); BE tự tính `tienDuKien/chenhLech`, không nhận từ FE |
| `GET /api/ca/{id}/doi-soat` | Xem đối soát đã chốt | |
| `GET /api/ca/lich-su` | Danh sách ca (filter: nhân viên, từ ngày, đến ngày, trạng thái; phân trang) | NV xem của mình; Admin xem tất cả |

P2 (tách task riêng): `POST /api/ca/{id}/ban-giao`, `POST /api/ca/{id}/xac-nhan-ban-giao`.

Nguồn tính tổng hợp: truy vấn `hoa_don` theo `id_ca_lam_viec` (đã stamp) + `phuong_thuc_thanh_toan` (R4) + trạng thái HĐ (R8). **FE không bao giờ gửi tổng doanh thu.**

## 5. Frontend (P1)

1. **POS (`/ban-hang`)** — `PosSalesPage.vue`:
   - On mount: `GET /api/ca/dang-mo`. Chưa có → banner đỏ "Chưa mở ca — Mở ca" + modal **Mở ca** (tiền đầu ca + ghi chú).
   - Header nút **Kết ca** (chỉ khi có ca mở) → modal **Kết ca**: bảng tổng hợp (tổng HĐ, doanh thu, tiền mặt thu, hoàn, tiền đầu ca, tiền dự kiến), ô nhập **tiền thực tế**, dòng chênh lệch tự tính (xanh nếu 0, đỏ nếu ≠0), ô lý do (bắt buộc khi lệch) → xác nhận → biên bản kết ca (modal thành công, nút in như phiếu HĐ).
2. **Màn lịch sử (`/ca`)** — `GiaoCaListPage.vue` (metaBo, sidebar "Giao ca"): bảng ca (mã, NV, mở/kết, tiền đầu, dự kiến, thực tế, chênh lệch, trạng thái), filter NV/từ-ngày/đen-ngày/trạng thái, PaginationBar (dùng component sẵn), click row → modal chi tiết đối soát + lịch sử thao tác (`lich_su_ca`).
3. **Service**: `FE/sd03_fe/src/api/giaoCa.js` (axios, theo pattern `fetch-auth.js`/baseAPI hiện có).

## 6. Edge cases (P1 đã xử lý)

- 2 request kết ca đồng thời → cập nhật có điều kiện, loser nhận 409 + trạng thái ca hiện tại.
- Kết ca khi còn HĐ dở → 400 với lý do cụ thể (liệt kê mã HĐ).
- Chênh lệch ≠ 0 bắt buộc lý do; không tự sửa số dự kiến từ FE.
- HĐ tạo sau khi ca đóng → không gắn vào ca cũ (BE chỉ stamp khi ca `DANG_MO`; HĐ admin ngoài ca để NULL).
- Mở lại ca / sửa kết quả → **không làm** (P1); lịch sử không xóa.
- Kết ca thay bởi Admin → ghi `lich_su_ca` với người thực hiện thật.

## 7. Không làm (non-goals P1)

- Thu/chi khác trong ca (P3), quản lý duyệt (P3), bàn giao (P2).
- Đa quầy, đa ca đồng thời, đếm tiền tự động, in PDF biên bản.
- Chặn bán khi chưa mở ca (R13), liên kết màn ca-làm-mock.
- Sửa schema `hoa_don` ngoài cột `id_ca_lam_viec`.
- **Hoàn tiền sau khi ca đã kết**: không reopen/recount sổ ca cũ (R21); hoàn tiền chỉ ý nghĩa khi ca còn mở hoặc trước khi kết.
- **Hoàn tiền một phần** (hoàn nửa hóa đơn) — P1 chỉ hoàn toàn phần.

## 7b. Câu hỏi mở — ĐÃ CHỐT (2026-10-21)

1. **Hoàn tiền tại quầy**: CÓ — R21 (mở transition POS + tính `tien_hoan_mat`).
2. **Bán khi chưa mở ca**: CHẶN checkout `TAI_QUAY` — R13 mới.
3. **id_nhan_vien POS**: lấy từ session đăng nhập — R20.

## 8. Thứ tự triển khai (plan Task)

1. Migration SQL + verify.
2. BE: entity/repo/service/controller `ca_lam_viec` (mo, dang-mo, tong-ket, ket-ca, doi-soat, lich-su) + stamp `id_ca_lam_viec` khi tạo HĐ POS + test.
3. FE POS: service + banner/mở ca + modal kết ca + e2e.
4. FE màn lịch sử `/ca` + sidebar + e2e.
5. (P2/P3 — plan riêng khi bạn duyệt làm tiếp)
