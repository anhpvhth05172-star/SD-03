# Fix Quản lý Đơn hàng / Hóa đơn — PolyShoes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Đồng bộ combobox Loại đơn, bỏ bộ lọc khoảng thời gian, thêm trạng thái "Giao hàng thất bại", chặn hủy đơn từ Đã xác nhận trở đi, và tự mở in PDF cho đơn online tạo ở trạng thái Chờ xác nhận.

**Architecture:** Mọi quy tắc trạng thái hóa đơn nằm trong `HoaDonService` (TRANG_THAI_MAP mã↔nhãn, TRANSITION_MAP online, TRANSITION_MAP_POS) — thêm trạng thái = sửa map + test unit mock (không migration, cột `trang_thai` là String(30), không CHECK). FE đọc nhãn/mã từ `constants/invoiceStatus.js`, tabs/pill của `InvoiceListPage`, dropdown trạng thái của `InvoiceDetailPage` sinh từ `trangThaiTiepTheo` do BE trả → cập nhật map BE là nguồn sự thật, FE chỉ thêm hiển thị.

**Tech Stack:** Spring Boot/Java 17 + Mockito unit tests (pattern HoaDonTrangThaiTest), Vue 3 SPA + SearchableSelect component, in PDF = `window.print()` + CSS A4 sẵn có trong InvoiceDetailPage (không thêm thư viện).

**Spec:** yêu cầu trong phiên (4 yêu cầu + kiểm thử + báo cáo) — plan này lập từ spec đó.

## Global Constraints
- KHÔNG commit, KHÔNG push, KHÔNG tạo commit.
- Bảo toàn toàn bộ thay đổi chưa commit (6 file import Excel nhân viên: `NhanVienController/Service/...`, `EmployeeCreatePage/ListPage`, DTO + test mới) — không đụng các file này.
- Không đổi schema/migration DB trừ khi thật cần (plan này không cần: không có CHECK constraint).
- Giữ nguyên option + param API của Loại đơn: `loaiDon` = `"" | "Tại quầy" | "Online"`.
- Giữ nguyên filter khác: `ma`, `trangThai` (nhãn), `page`, `size`; cột thời gian trong bảng/lịch sử giữ nguyên.
- Mã trạng thái dùng mã kỹ thuật in hoa (`GIAO_HANG_THAT_BAI`), nhãn tiếng Việt `Giao hàng thất bại`.
- Không thêm luồng giao lại sau thất bại (failed = terminal, không chuyển tiếp).
- Không thêm thư viện PDF mới.
- Test BE: `.\mvnw.cmd test` (workdir `BE`); FE: `npm run build` (workdir `FE/sd03_fe`).

## Review Focus
1. **POS tại quầy bị ảnh hưởng oan** — đơn `Tại quầy` vẫn tạo `Đã hoàn thành`, không thấy trạng thái thất bại, không auto-print. → Test BE `posKhongDuocChuyenSangGiaoHangThatBai`, điều kiện FE `loaiDon/maTrangThai`.
2. **In tự động lặp (F5 / mở chi tiết)** — flag in phải bị tiêu thụ một lần. → `router.replace` xóa `?in=1` ngay khi đọc; chỉ in khi `maTrangThai === 'CHO_XAC_NHAN'`.
3. **Hủy đơn lách qua dropdown "Chuyển trạng thái"** — BE map gỡ `DA_HUY` khỏi `DA_XAC_NHAN`/`CHO_GIAO_HANG`; FE lọc `Đã hủy` khỏi dropdown, nút Hủy riêng chỉ hiện ở `CHO_XAC_NHAN`. → Test BE từ chối đúng message + không ghi lịch sử.
4. **Nhãn FE ≠ nhãn BE** (dropdown/tabs chết im lặng) — FE dùng nguyên label BE trả về; test BE assert nhãn `Giao hàng thất bại` trong `toTenTrangThai`/`trangThaiTiepTheo`.
5. **Allowlist trạng thái tạo đơn phá test cũ** — create chỉ nhận `CHO_XAC_NHAN`/`DA_HOAN_THANH`; chạy toàn bộ suite, nếu test GHN/validation tạo trạng thái khác thì nới allowlist cho khớp thực tế và ghi rõ trong báo cáo.

---

### Task 1: BE — trạng thái `GIAO_HANG_THAT_BAI`

**Files:**
- Modify: `BE/src/main/java/com/example/datn/service/HoaDonService.java` (TRANG_THAI_MAP:66-75, TRANSITION_MAP:77-87, tinhTrangThaiThanhToan:265-273 không đổi — để rơi nhánh "Chưa thanh toán")
- Modify: `BE/src/main/java/com/example/datn/repository/KhachHangRepository.java:49,52,72` (thêm `GIAO_HANG_THAT_BAI` vào danh sách loại trừ tổng chi tiêu)
- Test: `BE/src/test/java/com/example/datn/service/HoaDonGiaoHangThatBaiTest.java` (mới, cùng pattern HoaDonTrangThaiTest: mock repos, `new HoaDonService(...)`)

**Interfaces:**
- Produces: mã `GIAO_HANG_THAT_BAI`, nhãn `Giao hàng thất bại`; transition `DANG_GIAO_HANG → [DA_GIAO_HANG, GIAO_HANG_THAT_BAI]`; `GIAO_HANG_THAT_BAI → []` (terminal); `trangThaiTiepTheo` tự chứa "Giao hàng thất bại" khi đơn Đang giao hàng (FE Task 4 dùng).

- [ ] **Step 1: Viết test đỏ** — các case: `onlineDangGiaoHangSangGiaoHangThatBai_HopLe` (đổi OK + lịch sử ghi `CHUYEN_TRANG_THAI`, noiDung chứa "Chuyển từ Đang giao hàng sang Giao hàng thất bại"), `onlineChoXacNhanSangGiaoHangThatBai_BiTuChoi`, `onlineGiaoHangThatBaiLaTrangThaiKetThuc` (không chuyển tiếp), `posKhongDuocChuyenSangGiaoHangThatBai`, `getTiepTheoOnline_DangGiaoHang_ChuaCoGiaoHangThatBai` (DTO trả label mới).
- [ ] **Step 2: Chạy xác nhận FAIL đúng lý do** (thiếu mã/nhãn → "Trạng thái không hợp lệ" / transition không chứa).
- [ ] **Step 3: Implement**: thêm `{"GIAO_HANG_THAT_BAI", "Giao hàng thất bại"}` vào TRANG_THAI_MAP; `TRANSITION_MAP.put("DANG_GIAO_HANG", List.of("DA_GIAO_HANG", "GIAO_HANG_THAT_BAI"))`; `TRANSITION_MAP.put("GIAO_HANG_THAT_BAI", List.of())`; KhachHangRepository thêm mã vào 3 query loại trừ.
- [ ] **Step 4: Chạy test PASS** + chạy `.\mvnw.cmd test -Dtest=HoaDon*` không phá test cũ.

### Task 2: BE — cấm hủy từ Đã xác nhận trở đi

**Files:**
- Modify: `BE/src/main/java/com/example/datn/service/HoaDonService.java` (TRANSITION_MAP:79-81, `doiTrangThai`:365-400)
- Test: `BE/src/test/java/com/example/datn/service/HoaDonHuyDonTest.java` (mới)

**Interfaces:**
- Consumes: `toMaTrangThai`, `findForUpdateOrThrow`, `ghiLichSuTrangThai` (có sẵn).
- Produces: message chính xác `Đơn hàng đã được xác nhận, không thể hủy đơn.` khi request `DA_HUY` từ trạng thái ≥ `DA_XAC_NHAN`; `CHO_XAC_NHAN → DA_HUY` vẫn hợp lệ.

- [ ] **Step 1: Test đỏ**: `onlineDaXacNhanHuy_BiTuChoiCoThongBaoRieng` (DA_XAC_NHAN→DA_HUY throw IllegalArgumentException message đúng, verify `lichSuHoaDonRepository.save` never + `hoaDonRepository.save` never = không side effect), `onlineChoGiaoHangHuy_BiTuChoi`, `onlineChoXacNhanHuy_VanHopLe` (vẫn OK + có lịch sử), `onlineDangGiaoHangHuy_BiTuChoi` (message riêng).
- [ ] **Step 2: Chạy FAIL đúng message/behavior.**
- [ ] **Step 3: Implement**: TRANSITION_MAP gỡ `DA_HUY` khỏi `DA_XAC_NHAN` và `CHO_GIAO_HANG` (giữ ở `CHO_XAC_NHAN`); trong `doiTrangThai`, trước check map: nếu `maMoi=DA_HUY` và `maHienTai != CHO_XAC_NHAN` → throw `IllegalArgumentException("Đơn hàng đã được xác nhận, không thể hủy đơn.")`.
- [ ] **Step 4: PASS** + `.\mvnw.cmd test` toàn bộ.

### Task 3: BE — chặn tạo đơn với trạng thái bỏ qua quy trình

**Files:**
- Modify: `BE/src/main/java/com/example/datn/service/HoaDonService.java` (`validateHeader`:547-550 hoặc `create`:280)
- Test: thêm vào `HoaDonHuyDonTest` hoặc `HoaDonValidationTest` pattern

**Interfaces:**
- Produces: `create` chỉ nhận trạng thái khởi tạo ∈ {`CHO_XAC_NHAN`, `DA_HOAN_THANH`} (online tạo ở Chờ xác nhận, POS tạo ở Đã hoàn thành); sai → `IllegalArgumentException("Trạng thái khởi tạo không hợp lệ...")`.

- [ ] **Step 1: Test đỏ**: tạo với `GIAO_HANG_THAT_BAI` (và `DA_GIAO_HANG`) bị từ chối; tạo `Chờ xác nhận` và `Đã hoàn thành` vẫn OK.
- [ ] **Step 2: FAIL đúng.**
- [ ] **Step 3: Implement allowlist** sau khi `toMaTrangThai` ở validateHeader/create.
- [ ] **Step 4: PASS + toàn bộ suite** (đây là Step soát allowlist có phá test GHN/validation cũ không — nếu có, nới allowlist tối thiểu và ghi vào báo cáo).

### Task 4: FE — hiển thị trạng thái thất bại + nút hủy đơn

**Files:**
- Modify: `FE/sd03_fe/src/constants/invoiceStatus.js` (`TRANG_THAI_KET_THUC`:12 thêm `'Giao hàng thất bại'`; `pillClass`:31-38 thêm red)
- Modify: `FE/sd03_fe/src/pages/Invoice/InvoiceListPage.vue` (tabs:17-27 + tabDot:29-39 + statusPill:213-221 thêm trạng thái)
- Modify: `FE/sd03_fe/src/pages/Invoice/InvoiceDetailPage.vue` (`TRANG_THAI_CODE`:36-45 thêm key; dropdown `tiepTheoList` lọc `'Đã hủy'`; thêm nút `Hủy đơn` ở khối nút:695-734 + handler `huyDon()`)

**Interfaces:**
- Consumes (Task 1-2): BE trả `maTrangThai`, `trangThaiTiepTheo` (labels) — dropdown sinh từ response.
- Produces: `huyConfirm → updateInvoiceStatus(id, 'DA_HUY') → load()` (dữ liệu lấy từ API, không giả lập).

- [ ] **Step 1: Sửa constants + list page** (tab `GIAO_HANG_THAT_BAI`/label, dot red, pill red; terminal list thêm label — stepper ẩn, hiện terminal-box).
- [ ] **Step 2: Sửa detail page**: thêm `TRANG_THAI_CODE['Giao hàng thất bại'] = 'GIAO_HANG_THAT_BAI'`; lọc `'Đã hủy'` khỏi menu chuyển trạng thái; nút `Hủy đơn` (`v-if` `data.maTrangThai === 'CHO_XAC_NHAN' && tiepTheoList.includes('Đã hủy')`), dialog `notifyConfirm('Xác nhận hủy đơn?', ...)` → `updateInvoiceStatus(..., 'DA_HUY')` → `notifySuccess` → `await load()`; lỗi BE (message `Đơn hàng đã được xác nhận...`) → `notifyError`.
- [ ] **Step 3: `npm run build` PASS.**

### Task 5: FE — đồng bộ combobox Loại đơn với chuẩn

**Files:**
- Modify: `FE/sd03_fe/src/pages/Invoice/InvoiceListPage.vue:334-341` (+ import `SearchableSelect` ở dòng 2)

**Interfaces:**
- Consumes: `SearchableSelect` (`modelValue`, `options[{id,ten}]`, `value-key="id"`, `label-key="ten"`, `placeholder`, `@change`) — pattern `ProductListPage.vue:830-840`.
- Produces: `fLoaiDon` giữ nguyên `""|Tại quầy|Online`, `params.loaiDon` không đổi.

- [ ] **Step 1: Thay `<select class="form-select">` bằng `<SearchableSelect>`** với `options = [{id:'',ten:'Tất cả'},{id:'Tại quầy',ten:'Tại quầy'},{id:'Online',ten:'Online'}]`, placeholder `Tất cả loại đơn`, `@change="applyFilter"` — giữ `.filter-col` wrapper.
- [ ] **Step 2: `npm run build` PASS** (check không còn tham chiếu `form-select` của combobox này).

### Task 6: FE — bỏ bộ lọc Khoảng thời gian

**Files:**
- Modify: `FE/sd03_fe/src/pages/Invoice/InvoiceListPage.vue` — xóa: state `fTuNgay/fDenNgay` (53-54), default hôm nay (59-66), `parseDate`+`todayLocal` (68-75) nếu không còn nơi dùng, điều kiện `tuNgay/denNgay` trong `buildParams` (80-84), `appliedRange` (99,108-111,367), nhánh reset ngày trong `resetFilter` (135-136), 2 cột `Từ ngày/Đến ngày` trong template (343-351)

**Interfaces:**
- Produces: `buildParams()` chỉ còn `page,size,ma,loaiDon,trangThai` — `load()` và `xuatExcel()` dùng chung nên export tự hết param ngày.

- [ ] **Step 1: Xóa UI + state + params** như trên; giữ nguyên cột ngày trong bảng.
- [ ] **Step 2: grep xác nhận** không còn `tuNgay|denNgay|fTuNgay|fDenNgay|appliedRange` trong file; `npm run build` PASS.

### Task 7: FE — tự mở in cho đơn online tạo ở Chờ xác nhận

**Files:**
- Modify: `FE/sd03_fe/src/pages/Pos/PosSalesPage.vue` (`handleCheckout`:780-902 — đọc response, trả `trangThai` thật)
- Modify: `FE/sd03_fe/src/pages/Invoice/InvoiceDetailPage.vue` (`onMounted`/`load` + `inHoaDon`:274-289)

**Interfaces:**
- Consumes: response `POST /hoa-don` → `HoaDonDTO.trangThai` (label) + `id`; `inHoaDon()` (gen QR + `nextTick` + `window.print()`); CSS A4 sẵn.
- Produces: flow mới trong POS: payload online (`isDelivery`) đổi `trangThai` → `'Chờ xác nhận'` (tại quầy giữ `'Đã hoàn thành'`); nếu response `trangThai === 'Chờ xác nhận'` → `router.push('/hoa-don/' + res.id + '?in=1')` (thay mở modal biên lai cho nhánh này); nếu response có `id` mà navigate/print lỗi → vẫn `notifySuccess` tạo đơn + `notifyWarning` lỗi in (không báo tạo thất bại).

- [ ] **Step 1: Sửa POS**: payload `trangThai: currentOrder.value.isDelivery ? 'Chờ xác nhận' : 'Đã hoàn thành'`; đọc `res.trangThai/res.id`; nhánh Chờ xác nhận → navigate `?in=1` (guard try/catch).
- [ ] **Step 2: Sửa detail**: khi `onMounted` thấy route.query.in === '1' → `router.replace` xóa query NGAY (tiêu thụ flag), sau khi `load()` xong + `data.maTrangThai === 'CHO_XAC_NHAN'` → `await nextTick` + `await genQr()` (nếu thiếu) + `inHoaDon()` đúng 1 lần (biến cờ local, không watcher lặp).
- [ ] **Step 3: `npm run build` PASS.**

### Task 8: Kiểm thử tổng & báo cáo

- [ ] `.\mvnw.cmd test` (BE, toàn bộ) — exit 0.
- [ ] `npm run build` (FE) — exit 0.
- [ ] Rà `git status` — chỉ các file trong plan + 6 file import Excel (uncommitted) còn nguyên.
- [ ] Báo cáo theo mục 9 của spec (file đổi, cách xử lý từng YC, API/DTO/rules đổi, test + kết quả, build, phần chưa làm được, xác nhận không commit).
