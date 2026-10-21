# Chỉnh trạng thái đơn hàng PolyShoes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm nút "Chỉnh đơn hàng" cho từng hóa đơn với modal chọn trạng thái hợp lệ; Backend kiểm soát quy tắc chuyển trạng thái riêng cho đơn online và đơn tại quầy (POS), ghi lịch sử thay đổi.

**Architecture:** Tái sử dụng endpoint sẵn có `PUT /api/hoa-don/{id}/trang-thai` và service `HoaDonService.doiTrangThai`. Thêm map chuyển trạng thái riêng cho POS, khóa chống ghi đè bằng `findByIdForUpdate` (PESSIMISTIC_WRITE đã có), DTO trả về danh sách trạng thái tiếp theo được phép để FE hiển thị — FE không tự quyết định tính hợp lệ. FE thêm modal component dùng chung cho trang danh sách và trang chi tiết.

**Tech Stack:** Spring Boot + JPA + SQL Server (JUnit5 + Mockito), Vue 3 + Vite (không có FE test framework — verify bằng `npm run build`).

**Spec:** Prompt "CHỈNH TRẠNG THÁI ĐƠN HÀNG POLYSHOES" (user cung cấp trong phiên).

## Global Constraints

- Không đổi schema SQL Server, không thêm dependency, không sửa/xóa dữ liệu thật.
- Không commit/push nếu chưa được yêu cầu (spec mục 9).
- Không sửa module không liên quan (chỉ hóa đơn FE + BE liên quan hóa đơn).
- Tông đỏ chủ đạo `#dc2626`; không phá bố cục/responsive hiện có.
- Giữ nguyên bộ lọc, phân trang, tìm kiếm, xuất Excel, nút Xem chi tiết (hạn chế hiện có: tab thùng rác đã bị gỡ theo yêu cầu trước đó — không tái thêm).
- Thông báo lỗi trả về tiếng Việt.
- Trạng thái lưu trong DB là **mã** (`CHO_XAC_NHAN`, ..., `loai_don` = `TAI_QUAY`/`ONLINE`); DTO xuất ra nhãn tiếng Việt.
- Code style: không thêm comment; tuân thủ style file hiện có.

## Facts từ khảo sát (không cần đọc lại code)

- `HoaDonService.java:63-72` `TRANG_THAI_MAP` (8 mã↔nhãn); `:74-84` `TRANSITION_MAP` (online).
- `HoaDonService.doiTrangThai(Long, String)` tại `:348-370` — đã validate + ghi `LichSuHoaDon` trong cùng `@Transactional`; **đang dùng `findOrThrow` (không khóa)**.
- `HoaDonRepository.findByIdForUpdate` (PESSIMISTIC_WRITE) đã có, `HoaDonService.findForUpdateOrThrow` đã có.
- Endpoint `PUT /api/hoa-don/{id}/trang-thai` đã có (`HoaDonController:93-96`), request `TrangThaiHoaDonRequest{trangThai}`.
- `LichSuHoaDon`: id, id_hoa_don, trang_thai(30), thoi_gian, ghi_chu(500), hanh_dong(100) — **không có cột người thực hiện** → không ghi người vào lịch sử (spec: không ghi dữ liệu giả).
- `HoaDonService.nguoiCapNhat` hiện hardcode `"admin"` ở mọi nơi — giữ nguyên (ngoài phạm vi).
- POS tạo hóa đơn với `trangThai: 'Đã hoàn thành'`, `loaiDon` = `Online` (nếu isDelivery) hoặc `Tại quầy` (`PosSalesPage.vue:819-828`) — **phân loại phải theo `loaiDon`**, không theo trang.
- Test hiện có: `HoaDonValidationTest.java` (Mockito thuần, `new HoaDonService(10 arg)`), đã có 2 test `doiTrangThai` mock `findById` (L606-624) — **phải đổi mock sang `findByIdForUpdate`**.
- FE: `api/invoice.js` chưa có hàm đổi trạng thái; list actions chỉ còn "Xem chi tiết"; detail có `STEPS`(6 nhãn)+`TERMINAL` hardcode và step-track; modal pattern = `.modal-mask @click.self`, `position: fixed; z-index: 60` (`CustomerAddressModal.vue`).
- Không có FE test framework → verify FE bằng `npm run build` + Vite transform check.

## Review Focus

1. **Dữ liệu POS legacy sai trạng thái** (ví dụ `TAI_QUAY` đang `DA_HUY` hoặc `CHO_GIAO_HANG`): modal vẫn mở được, `trangThaiTiepTheo` rỗng → FE phải hiện "không thể chuyển tiếp", không crash, không gửi rỗng. → Test BE `posTrangThaiLaMa_TiepTheoRong`; FE kiểm tra bằng build + đọc code.
2. **`loaiDon` lưu dạng nhãn tiếng Việt** (dữ liệu cũ `'Tại quầy'` thay vì `TAI_QUAY`): nếu không normalize, POS bị xử lý theo map online (lọt sang trạng thái giao hàng = vi phạm spec). → Test BE `loaiDonLaTenTiengViet_VanDungQuyTacPos`.
3. **Ghi chú > 500 ký tự / ký tự điều khiển** làm hỏng ghi lịch sử (DB từ chối, rollback oan): phải validate trước. → Test BE `ghiChuQua500_BiTuChoi`, `ghiChuKyTuDieuKhien_BiTuChoi`.
4. **Đọc dữ liệu cũ từ FE** (2 request đồng thời / FE giữ DTO cũ): request 2 ghi đè mất thay đổi của request 1. → Khóa `findByIdForUpdate` + test `dongThoi_DungKhoaChongDong` (verify gọi `findByIdForUpdate`, không gọi `findById`).
5. **Cập nhật thất bại không được để lại lịch sử**: reject xảy ra trước mọi `save`. → Test `chuyenTrangThaiThatBai_KhongGhiLichSu` (verify `lichSuHoaDonRepository, never()).save`).

---

### Task 1: Backend — Quy tắc chuyển trạng thái theo loại đơn + khóa + ghi chú

**Files:**
- Modify: `BE/src/main/java/com/example/datn/service/HoaDonService.java` (map `TRANSITION_MAP_POS` tại vùng `:74-84`, `doiTrangThai` tại `:348-370`, `ghiLichSuTrangThai` tại `:372-379`)
- Test tạo mới: `BE/src/test/java/com/example/datn/service/HoaDonTrangThaiTest.java`
- Modify test: `BE/src/test/java/com/example/datn/service/HoaDonValidationTest.java:606-624` (đổi mock `findById` → `findByIdForUpdate`)

**Interfaces:**
- Consumes: `HoaDonRepository.findByIdForUpdate(Long)` (PESSIMISTIC_WRITE), `LichSuHoaDonRepository.save`, `TRANG_THAI_MAP`/`toMaTrangThai`/`toTenTrangThai` có sẵn.
- Produces:
  - `public HoaDonDTO doiTrangThai(Long id, String trangThaiMoi, String ghiChu)` — 3 tham số; overload 2 tham số giữ nguyên (delegate `ghiChu=null`).
  - `List<String> trangThaiTiepTheo(String maHienTai, String maLoaiDon)` — package-private: danh sách **nhãn** được phép, chọn map theo loại đơn (helper nội bộ `chonBangChuyenTrangThai` không export) — Task 2 dùng trong `toFullDTO`.

**Quy tắc chính xác (quyết định, không phải code):**
- `TRANSITION_MAP_POS`: `CHO_XAC_NHAN → [DA_HOAN_THANH]`, `DA_HOAN_THANH → []`, mọi trạng thái khác → `[]` (getOrDefault).
- Map online giữ nguyên `TRANSITION_MAP` hiện có (8 trạng thái, terminal `DA_HUY`/`DA_HOAN_TIEN` rỗng).
- Nhận diện POS: `maLoaiDon` bằng `TAI_QUAY` **hoặc** nhãn `Tại quầy` (normalize qua logic giống `toMaLoaiDon`, không ném lỗi khi null → coi là online).
- `doiTrangThai`: đổi `findOrThrow` → `findForUpdateOrThrow` (khóa + chặn `daXoa`); validate `ghiChu` (≤500 ký tự, không chứa ký tự điều khiển, regex `KY_TU_CONTROL` có sẵn) trước khi `save`; reject cùng trạng thái & chuyển đổi không hợp lệ như hiện tại (thông báo đã có: `"Không được chuyển từ X sang Y"`).
- `ghiLichSuTrangThai(hoaDon, tuMa, denMa, ghiChu)`: `hanhDong="CHUYEN_TRANG_THAI"`, `ghiChu` = `"Chuyển từ X sang Y"` + (ghiChu người dùng → nối `" — " + ghiChu`), cắt còn 500.
- Không ghi người thực hiện (không có cột).

- [ ] **Step 1: Tạo `HoaDonTrangThaiTest.java` với các test thất bại (sẽ không compile vì chưa có method 3 tham số)**

Kiểu: sao chép cách dựng test của `HoaDonValidationTest` (mock 10 repo, `new HoaDonService(...)`, helper `hoaDon(trangThai)` mở rộng thêm `setLoaiDon`). Các test (tên + assert chính):

```
posChiDuocHoanThanh_TuChoXacNhan          // TAI_QUAY+CHO_XAC_NHAN, tiepTheo==["Đã hoàn thành"], doiTrangThai(…,"Đã hoàn thành") ok
posKhongDuocChuyenSangDangGiaoHang        // TAI_QUAY+CHO_XAC_NHAN → "Đang giao hàng" ném IAE, message "Không được chuyển từ Chờ xác nhận sang Đang giao hàng"
posKhongDuocChuyenSangDaXacNhan           // TAI_QUAY+CHO_XAC_NHAN → "Đã xác nhận" ném IAE (chỉ 2 trạng thái)
posDaHoanThanhKhongQuayLai                // TAI_QUAY+DA_HOAN_THANH → "Chờ xác nhận" ném IAE; trangThaiTiepTheo rỗng
posTrangThaiLaMa_TiepTheoRong             // TAI_QUAY+DA_HUY → trangThaiTiepTheo rỗng, doiTrangThai bất kỳ đều ném IAE
loaiDonLaTenTiengViet_VanDungQuyTacPos    // loaiDon="Tại quầy" + CHO_XAC_NHAN → "Đang giao hàng" ném IAE
onlineXacNhanHopLe_GhiLichSuCoGhiChu      // ONLINE+CHO_XAC_NHAN → "Đã xác nhận", ghiChu="Khách yêu cầu"; ArgumentCaptor: lichSu.trangThai==DA_XAC_NHAN, ghiChu chứa "Chuyển từ Chờ xác nhận sang Đã xác nhận" và "Khách yêu cầu"
onlineKhongDuocBoQuaBuoc                  // ONLINE+CHO_XAC_NHAN → "Chờ giao hàng" ném IAE
onlineChuyenTungBuoc_HopLe                // ONLINE+DA_XAC_NHAN → "Chờ giao hàng" ok
onlineDaHoanThanhKhongQuayLaiGiaoHang     // ONLINE+DA_HOAN_THANH → "Đang giao hàng" ném IAE
daHuyKhongTiepTucGiaoHang                 // ONLINE+DA_HUY → "Đã xác nhận" ném IAE
trangThaiMoiTrong_KhongDoiGiu             // same status → ném IAE "Trạng thái không thay đổi"
trangThaiKhongHopLe                       // "ABC" → ném IAE "Trạng thái không hợp lệ"
hoaDonDaXoaKhongDoiDuoc                   // daXoa=true → ném IAE "Không tìm thấy hóa đơn"
chuyenTrangThaiThatBai_KhongGhiLichSu     // reject → verify(lichSu, never()).save(any())
dongThoi_DungKhoaChongDong                // ok path → verify(hoaDonRepository).findByIdForUpdate(HD_ID); verify(hoaDonRepository, never()).findById(anyLong())
ghiChuQua500KyTu_BiTuChoi                 // ghiChu 501 ký tự → ném IAE chứa "500"
ghiChuKyTuDieuKhien_BiTuChoi              // ghiChu chứa \u0007 → ném IAE
getTraVeTrangThaiTiepTheo_Online          // ONLINE+CHO_XAC_NHAN → get() trả ["Đã xác nhận","Đã hủy"] (mock chiTiet findByHoaDonId → List.of())
```

- [ ] **Step 2: Chạy test → FAIL**

Run: `& .\mvnw.cmd test -Dtest=HoaDonTrangThaiTest` (workdir `BE`, `JAVA_HOME=C:\Users\DELL\.jdks\corretto-23.0.2`)
Expected: **COMPILE ERROR** (method `doiTrangThai(Long,String,String)` chưa tồn tại).

- [ ] **Step 3: Sửa `HoaDonValidationTest.java:606-624` — đổi 2 chỗ mock `findById(HD_ID)` → `findByIdForUpdate(HD_ID)`**

- [ ] **Step 4: Chạy 2 test cũ trong `HoaDonValidationTest` → PASS (chưa cần code mới)**

Run: `& .\mvnw.cmd test -Dtest=HoaDonValidationTest`
Expected: PASS (các test khác không đổi hành vi vì chưa sửa service).

- [ ] **Step 5: Implement trong `HoaDonService.java`**

- Thêm `TRANSITION_MAP_POS` (giá trị ở trên), `chonBangChuyenTrangThai(String maLoaiDon)`, `trangThaiTiepTheo(String,String)` (trả nhãn qua `toTenTrangThai`).
- `doiTrangThai(Long, String, String)`: `findForUpdateOrThrow` → normalize → cùng trạng thái → kiểm tra qua map theo loại đơn → validate `ghiChu` → set trạng thái + `ngayCapNhat` + `nguoiCapNhat="admin"` → `save` → `ghiLichSuTrangThai(..., ghiChu)` → `toFullDTO`. Overload 2 tham số delegate.

- [ ] **Step 6: Chạy `HoaDonTrangThaiTest` → PASS**

Run: `& .\mvnw.cmd test -Dtest=HoaDonTrangThaiTest`
Expected: PASS toàn bộ.

- [ ] **Step 7: Chạy `HoaDonValidationTest` → PASS**

Run: `& .\mvnw.cmd test -Dtest=HoaDonValidationTest`
Expected: PASS — nếu `chuyenTrangThaiHopLe_ThanhCongVaGhiLichSu` fail thì do lịch sử bị cắt/ghép khác kỳ vọng → sửa phần nối ghi chú chứ không sửa assert cũ (assert cũ chỉ verify `save`).

### Task 2: Backend — DTO + API surface (trạng thái tiếp theo, ghi chú)

**Files:**
- Modify: `BE/src/main/java/com/example/datn/dto/HoaDon/HoaDonDTO.java` (thêm 2 field cuối)
- Modify: `BE/src/main/java/com/example/datn/dto/HoaDon/TrangThaiHoaDonRequest.java` (thêm field)
- Modify: `BE/src/main/java/com/example/datn/controller/HoaDonController.java:93-96`
- Modify: `BE/src/main/java/com/example/datn/service/HoaDonService.java` (`toDTO` `:779`, `toFullDTO` `:814`)
- Test: `HoaDonTrangThaiTest.java` (bổ sung)

**Interfaces:**
- Consumes: `trangThaiTiepTheo(String,String)` từ Task 1.
- Produces:
  - `HoaDonDTO.maTrangThai` (String mã, set trong `toDTO`), `HoaDonDTO.trangThaiTiepTheo` (`List<String>` nhãn, set trong `toFullDTO` — chỉ `GET /{id}`).
  - `TrangThaiHoaDonRequest.ghiChu` (String, optional).
  - FE gọi `PUT /api/hoa-don/{id}/trang-thai` body `{trangThai, ghiChu?}` → trả `HoaDonDTO` đầy đủ.

- [ ] **Step 1: Test mới trong `HoaDonTrangThaiTest` (FAIL)**

```
getCoMaTrangThaiVaTiepTheo_ONLINE    // get(CHO_XAC_NHAN, ONLINE): maTrangThai=="CHO_XAC_NHAN", trangThaiTiepTheo==["Đã xác nhận","Đã hủy"], trangThai=="Chờ xác nhận"
getTiepTheoPos_TaiQuay               // get(CHO_XAC_NHAN, TAI_QUAY): trangThaiTiepTheo==["Đã hoàn thành"]
listKhongCanTiepTheo                 // list(...) không ném lỗi, dto.trangThaiTiepTheo == null (không tính ở list)
```
(ghiChu validation đã test Task 1; controller pass-through là code mỏng — không test riêng.)

- [ ] **Step 2: Chạy → FAIL** (run `HoaDonTrangThaiTest`, expected: assert trên `getMaTrangThai()`/`getTrangThaiTiepTheo()` fail).

- [ ] **Step 3: Implement**
- `HoaDonDTO`: thêm `private String maTrangThai;` và `private java.util.List<String> trangThaiTiepTheo;`.
- `toDTO`: `.maTrangThai(h.getTrangThai())`.
- `toFullDTO`: `dto.setTrangThaiTiepTheo(trangThaiTiepTheo(h.getTrangThai(), h.getLoaiDon()))`.
- `TrangThaiHoaDonRequest`: thêm `private String ghiChu;`.
- Controller: `hoaDonService.doiTrangThai(id, request.getTrangThai(), request.getGhiChu())`.

- [ ] **Step 4: Chạy `HoaDonTrangThaiTest` → PASS**

- [ ] **Step 5: Chạy toàn bộ suite BE**

Run: `& .\mvnw.cmd test` (workdir `BE`)
Expected: **BUILD SUCCESS, 0 test fail** (bao gồm `JwtAuthFilterTest` và các test WIP auth đang có trong work tree).

### Task 3: FE — Config dùng chung + API + Modal component

**Files:**
- Create: `FE/sd03_fe/src/constants/invoiceStatus.js`
- Create: `FE/sd03_fe/src/components/InvoiceStatusModal.vue`
- Modify: `FE/sd03_fe/src/api/invoice.js`

**Interfaces:**
- Consumes: `GET /api/hoa-don/{id}` (fields `maHoaDon, loaiDon, trangThai, maTrangThai, trangThaiTiepTheo`), `PUT /api/hoa-don/{id}/trang-thai`.
- Produces (Task 4/5 dùng):
  - `constants/invoiceStatus.js`: export `TRANG_THAI_ONLINE` (6 nhãn, thứ tự tiến trình), `TRANG_THAI_POS` (`['Chờ xác nhận','Đã hoàn thành']`), `TRANG_THAI_KET_THUC` (`['Đã hủy','Đã hoàn tiền']`), `laDonTaiQuay(loaiDon)` (`loaiDon === 'Tại quầy'`), `cacBuocCua(loaiDon)` → 1 trong 2 mảng trên.
  - `api/invoice.js`: `export const updateInvoiceStatus = (id, data) => http.put(`/hoa-don/${id}/trang-thai`, data).then((r) => r.data)`.
  - Component `InvoiceStatusModal.vue`: props `isOpen: Boolean`, `invoiceId: [Number, String]`; emits `close`, `updated(dto)`.

- [ ] **Step 1: Tạo `invoiceStatus.js`** (4 export như trên; di chuyển STEPS/TERMINAL hardcode hiện ở `InvoiceDetailPage.vue:16-25` vào đây).

- [ ] **Step 2: Thêm `updateInvoiceStatus` vào `api/invoice.js`.**

- [ ] **Step 3: Tạo `InvoiceStatusModal.vue`**

Hành vi (quyết định thiết kế):
- Render khi `isOpen`; mở → reset state → `getInvoice(invoiceId)` (luôn tải mới từ BE); loading state + lỗi tải → hiển thị inline + nút đóng.
- Hiển thị: Mã hóa đơn, Loại đơn (Nhãn), Trạng thái hiện tại (pill), **tiến trình** = `cacBuocCua(loaiDon)` dạng chip đánh dấu done/active/pending (chỉ render nếu trạng thái hiện tại nằm trong list, nếu không → ẩn track, vẫn hiện pill), select "Trạng thái tiếp theo" options từ `data.trangThaiTiepTheo` (nhãn), textarea "Ghi chú (tùy chọn)" maxlength 500.
- `trangThaiTiepTheo` rỗng → disable Lưu + dòng thông báo "Đã ở trạng thái cuối — không thể chuyển đổi".
- Submit: guard `saving` (chống bấm đôi), gọi `updateInvoiceStatus(id, { trangThai, ghiChu })`; lỗi BE → `notifyError` + giữ modal mở; thành công → `notifySuccess` → emit `updated(dto)` → emit `close`.
- Styling scoped: mask `position: fixed; inset:0; background rgba(15,23,42,.45); z-index: 60; display:flex; center`, panel `width:min(480px,92vw)`, accent `#dc2626` (nút Lưu), nút Hủy xám; responsive (padding nhỏ hơn ≤560px). Theo pattern `.modal-mask @click.self="close"`.

- [ ] **Step 4: Build PASS**

Run: `npm run build` (workdir `FE/sd03_fe`)
Expected: EXIT=0.

### Task 4: FE — Nút "Chỉnh đơn hàng" ở trang danh sách

**Files:**
- Modify: `FE/sd03_fe/src/pages/Invoice/InvoiceListPage.vue` (actions column ~L460, script import/state)
- Modify: `FE/sd03_fe/src/assets/styles/InvoiceListPage.css` (thêm `.act-btn.status-btn`)

**Interfaces:**
- Consumes: `InvoiceStatusModal` (Task 3), `load()` sẵn có.
- Produces: state `editingId` + handler mở modal; sau `updated` → `load()`.

- [ ] **Step 1: Script** — import modal + state `editingId = ref(null)` + hàm `chinhTrangThai(row)` set `editingId = row.id`; `@updated="editingId = null; load()"` (với notifySuccess thực hiện trong modal).

- [ ] **Step 2: Template** — thêm nút **đầu tiên** trong `.action-btns`:

```html
<button class="act-btn status-btn" type="button" title="Chỉnh đơn hàng" @click="chinhTrangThai(row)">…svg…</button>
```
+ `<InvoiceStatusModal :is-open="editingId !== null" :invoice-id="editingId" @close="editingId = null" @updated="taiLaiSauChinh" />` (handler gọi `load()` + `editingId = null`).

- [ ] **Step 3: CSS** — `.act-btn.status-btn` (border/color `#dc2626`, hover `background:#fef2f2`) theo pattern `.act-btn.view-btn`.

- [ ] **Step 4: Build PASS + verify Vite serve**

Run: `npm run build`; rồi `Invoke-WebRequest http://localhost:5173/src/pages/Invoice/InvoiceListPage.vue` → content chứa `Chỉnh đơn hàng`.
Expected: EXIT=0 + có chuỗi.

### Task 5: FE — Nút ở trang chi tiết + tiến trình 2 bước cho POS

**Files:**
- Modify: `FE/sd03_fe/src/pages/Invoice/InvoiceDetailPage.vue` (script `:16-33`, template step-track `:230-270`, khu nút `:445-460`)

**Interfaces:**
- Consumes: `cacBuocCua`, `TRANG_THAI_KET_THUC` (Task 3), `InvoiceStatusModal`, `getInvoice` sẵn có.

- [ ] **Step 1: Script** — bỏ `STEPS`/`TERMINAL` local → import từ constants; thêm `stepsHienThi = computed(() => data.value ? cacBuocCua(data.value.loaiDon) : TRANG_THAI_ONLINE)`; `stepIndex` tính theo `stepsHienThi`; `TERMINAL` → `TRANG_THAI_KET_THUC`; thêm `editing = ref(false)` + handler.

- [ ] **Step 2: Template** — step-track `v-for` sang `stepsHienThi`; thêm nút "Chỉnh đơn hàng" (trước "In hóa đơn", class `btn btn-status`) + mount `<InvoiceStatusModal … @updated="load" />`.

- [ ] **Step 3: CSS** — `.btn-status` (nền `#dc2626` outline style, cùng cao với `btn-print`) trong scoped style của detail.

- [ ] **Step 4: Build PASS**

Run: `npm run build` → EXIT=0.

### Task 6: Kiểm thử tổng thể + smoke API (không commit)

**Files:** không sửa (chỉ chạy + 1 file test data tạo qua API).

- [ ] **Step 1: Full BE test**

Run: `& .\mvnw.cmd test` (workdir `BE`, `JAVA_HOME=corretto-23.0.2`)
Expected: BUILD SUCCESS, 0 fail.

- [ ] **Step 2: FE build** → `npm run build` EXIT=0.

- [ ] **Step 3: Smoke API online (tạo test invoice qua POST, chạy đủ 12 case khả thi, dọn sạch)**

Chạy trên BE đang sống (`localhost:8080`), dùng PowerShell `Invoke-RestMethod`:
1. `POST /api/hoa-don` tạo hóa đơn ONLINE test (mã `HDTESTTT` + timestamp, sao chép payload hợp lệ từ session trước; ghi lại id).
2. Case: bỏ bước (`CHO_XAC_NHAN` → `Chờ giao hàng`) → kỳ vọng 400/IAE tiếng Việt.
3. Case: `Đã hủy` khi đang `Chờ xác nhận` → kỳ vọng IAE (vì chọn `Đã xác nhận`… → thực tế: hủy thành công allowed — dùng case `Chờ xác nhận` → `Đã xác nhận` (200), rồi `Đã xác nhận` → `Chờ xác nhận` (bị từ chối — quay ngược), `Đã xác nhận` → `Chờ giao hàng` (200), … lần lượt về `Đã hoàn thành` (200) — mỗi bước kiểm tra `trangThaiTiepTheo` trong response.
4. Case POS trên hóa đơn test: trước khi hoàn tất online, tạo thêm 1 hóa đơn `Tại quầy` test với trạng thái `Chờ xác nhận`: `GET /{id}` kỳ vọng `trangThaiTiepTheo=["Đã hoàn thành"]`; PUT `Đang giao hàng` → bị từ chối; PUT `Đã hoàn thành` → 200. **Hóa đơn POS test này KHÔNG xóa được (chỉ xóa từ Chờ xác nhận/Đã hủy) → giữ lại với mã `HDTESTTT…` và báo cáo.** Lựa chọn thay thế đã cân nhắc: chỉ test đường từ chối rồi xóa (được xóa từ Chờ xác nhận) — nhưng tiêu chí nghiệm thu yêu cầu đường positive hoạt động thật → ưu tiên positive, chấp nhận 1 record test, báo cáo rõ.
5. Case `daXoa`: `DELETE` hóa đơn online test (từ `Đã hủy`) → 204; `PUT trang-thai` trên nó → 400 "Không tìm thấy".
6. Kiểm tra lịch sử: `GET /api/hoa-don/{id}` không trả lịch sử (không có endpoint) → kiểm gián tiếp qua DB query `SELECT TOP 5 * FROM lich_su_hoa_don ORDER BY id DESC` (nếu có quyền truy cập DB) hoặc bỏ qua, ghi rõ trong báo cáo.
7. Case chức năng cũ: `GET list` (filter/pagination), `GET export` (blob) → 200.

- [ ] **Step 4: FE runtime check (không có browser automation)**

- `Invoke-WebRequest` module `InvoiceStatusModal.vue` và `InvoiceListPage.vue` từ Vite → 200, content chứa `Chỉnh đơn hàng`.
- Grep xác nhận không còn hardcode `STEPS`/`TERMINAL` trong detail page.

- [ ] **Step 5: Báo cáo theo spec mục 10** (file đã sửa/tạo, cách phân biệt loại đơn, quy tắc, API, lịch sử, kết quả test, vấn đề tồn tại — gồm: hóa đơn POS test để lại trong DB; tab thùng rác không có (đã gỡ trước đó, spec nhắc nhưng hạn chế hiện có); POS không cho hủy qua trạng thái (spec yêu cầu chỉ 2 trạng thái); hoàn tiền giữ nguyên quy trình cũ `Đã hoàn thành → Đã hoàn tiền`; không có người thực hiện trong lịch sử (không có cột); đồng bộ đa-tab giữa list và detail không làm (SPA mỗi tab 1 route, mỗi trang tự reload sau khi cập nhật)).
