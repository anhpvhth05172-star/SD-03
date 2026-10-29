# POLYSHOES UI Review Fixes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sửa toàn bộ ý kiến review video 17 phút trên 5 module: Hóa đơn, Giảm giá, Sản phẩm/Biến thể, Khách hàng/Nhân viên, Cỡ chữ toàn app.

**Architecture:** Chủ yếu FE Vue 3 (no unit test runner → tests = e2e puppeteer scripts + `vite build`); BE chỉ đụng ở chống trùng SĐT nhân viên + bộ lọc TH/CL cho đợt giảm giá (JUnit test-first). Verification mỗi task = test RED → implement → test GREEN + build.

**Tech Stack:** Vue 3 + Vite 8, Spring Boot + JUnit, puppeteer-core + Chrome (e2e, `--no-proxy-server`).

**Spec:** Bản tóm tắt review 17 phút + Q&A đã chốt (user "mặc định hết"): Q1=A (lọc TH/CL), Q2=% của đợt (`form.percent`), Q3=h1 biến thể 24→20px, Q4=bỏ tag "+N địa chỉ" + tooltip (modal Sổ địa chỉ giữ danh sách, chỉ bỏ mã DC), Q4b=giữ 2 icon, mắt→nút "Sửa", Q5=chống trùng theo SĐT, Q6=fix route Sửa bị tráo.

## Global Constraints
- Không thêm dependency mới.
- Tiếng Việt có dấu, UTF-8 — đọc file bằng Read trước khi edit (tránh mojibake như vụ ProductListPage).
- Không đổi BE flow trạng thái hóa đơn (TRANSITION_MAP, auto-hủy lần 3, retry).
- E2e: puppeteer-core + Chrome `--no-proxy-server`, screenshot vào `C:\Users\DELL\AppData\Local\Temp\opencode\bugshots\`, script vào `...\Temp\opencode\pdfrepro\`.
- FE test command: `npm run build` + `node <e2e script>` (FE không có unit runner). BE test: `.\mvnw.cmd -q test -Dtest=<Class>`.
- Restart BE: kill java trước khi `package` (Windows lock jar). FE: không đổi vite config nên không cần restart (HMR).
- Commit per task bằng `git commit -m <msg> -- <paths>` (chỉ các file của task; không push).
- Server kỳ vọng: BE 8080, FE 5173 — kiểm tra port trước e2e, start lại nếu cần.

## Review Focus
1. Stepper: sau GHTB → retry `DANG_GIAO_HANG` phải quay lại 6 bước bình thường, fail node biến mất — test: chuyển GHTB→DANG→DA_GIAO.
2. Auto-hủy lần 3 (`GIAO_HANG_THAT_BAI`→`DA_HUY`) → terminal-box render, stepper ẩn — test: đơn đã hủy mở chi tiết không lỗi.
3. Bỏ ô "Đến ngày": `fDenNgay` vẫn prefill hôm nay — lọc "Từ ngày" > hôm nay → check `applyFilter` không treo/rỗng sai.
4. Bỏ lọc theo sản phẩm: link từ bảng SP vẫn vào `/bien-the` OK, add-modal vẫn chọn SP được.
5. Nhân viên: sửa chính nó không bị báo trùng SĐT (exclude id); import Excel không rớt valid.

---

### Task 1: Hóa đơn — Stepper "Giao hàng thất bại" + nhãn "Giao hàng thành công"

**Files:** `FE/sd03_fe/src/pages/Invoice/InvoiceDetailPage.vue` (stepsList :81-84, template :496-541, class :501-505, label :533, icon :509-531, CSS ~:1012-1155), `FE/sd03_fe/src/constants/invoiceStatus.js`

**Interfaces:** Produces `nhanBuoc(s)` export từ invoiceStatus.js; stepsList fail-branch (5 nút). Không task nào consume khác.

- [ ] Bước 1: viết e2e test RED — `Temp\opencode\pdfrepro\stepper-ghb-e2e.js`: đăng nhập NV (`duc@gmail.com`/`MatKhauMoi@123`), tìm/create 1 đơn online, PUT `/api/.../trang-thai` → `GIAO_HANG_THAT_BAI` (endpoint lấy từ `api/invoice.js updateInvoiceStatus`), mở `/hoa-don/:id`, assert: (a) tồn tại node stepper có text "Giao hàng thất bại" + class đỏ (`is-fail`); (b) "Đã giao hàng" & "Đã hoàn thành" **không** có trong stepper; (c) 4 bước đầu có `is-done`. Chạy → FAIL (RED).
- [ ] Bước 2: implement — `stepsList` nhánh `trangThai === 'Giao hàng thất bại'` → `['Chờ xác nhận','Đã xác nhận','Chờ giao hàng','Đang giao hàng','Giao hàng thất bại']`; export `nhanBuoc` (`'Đã giao hàng' → 'Giao hàng thành công'`); template label `{{ nhanBuoc(s) }}`; class `'is-next': laTrangThaiTiepTheo(s) && !laDaQua(s)`; icon `v-else-if="s === 'Giao hàng thất bại'"` (svg ✗) **trước** icon theo index; CSS `.stepper-step.is-fail` (đỏ, mirror `.is-active`).
- [ ] Bước 3: chạy e2e → GREEN; extend test case: đơn `DA_GIAO_HANG` → node label "Giao hàng thành công"; GHTB→retry → 6 bước.
- [ ] Bước 4: `npm run build` PASS. Chụp screenshots.
- [ ] Bước 5: commit (InvoiceDetailPage.vue, invoiceStatus.js, e2e script).

### Task 2: Hóa đơn — bỏ nút ẩn bộ lọc + bỏ ô "Đến ngày"

**Files:** `FE/sd03_fe/src/pages/Invoice/InvoiceListPage.vue` (ref :54, button :282-289, v-show :291, ô :342-345; GIỮ `fDenNgay` prefill :62-63/:126-127)

- [ ] RED: e2e `invoice-filter-e2e.js` — mở `/hoa-don`, assert: (a) không có button chứa "Ẩn bộ lọc"; (b) không có label "Đến ngày"; (c) label "Từ ngày" còn; (d) bấm "Áp dụng" list vẫn tải (count > 0 hoặc empty-state hiển thị). → FAIL.
- [ ] Implement: xóa button + `v-show` + ref; xóa block "Đến ngày".
- [ ] GREEN + `npm run build` PASS + screenshot.
- [ ] Commit (InvoiceListPage.vue).

### Task 3: Giảm giá — bỏ "Ẩn bớt"/"N kết quả"/chữ phụ/text chết

**Files:** `DiscountEventPage.vue` (:6, :169-172, :197-216, :219), `VoucherListPage.vue` (:30, :187-189, :219-241, :245, :372)

- [ ] RED: e2e `discount-toggle-e2e.js` — mở `/dot-giam-gia` + `/phieu-giam-gia`: assert không có text "kết quả" (trong panel đầu), không có button "Ẩn bớt"/"Hiện thêm"/"Mở rộng", không có class `screen-sub` text mô tả, không có "08:32 25/05/2026". → FAIL.
- [ ] Implement: xóa `.panel-right`/`.filter-head-right` + `v-show` + refs + 2 `screen-sub` + text chết.
- [ ] GREEN + build + screenshots.
- [ ] Commit (2 files).

### Task 4: Đợt form — bỏ "Giá hiện tại", thêm cột "% giảm", ảnh-sau-STT (update form)

**Files:** `VoucherForm.vue` (thead :613-622, cell :648-691), `VoucherFormUpdate.vue` (thead :712-721, cell :750-795)

- [ ] RED: e2e `voucher-price-e2e.js` — mở form thêm đợt (`/dot-giam-gia/add`) + form sửa (tạo 1 đợt trước hoặc dùng data sẵn): assert: (a) cột "Giá hiện tại" vắng; (b) có cột "% giảm" hiển thị `form.percent`; (c) thứ tự cột ở form sửa = STT, Ảnh, Sản phẩm… (d) cột Giá bán + Giá sau giảm + Tồn còn. → FAIL.
- [ ] Implement: cả 2 form — xóa cột `Giá hiện tại` (th + td), chèn cột `% giảm` sau `Giá bán` (nội dung `{{ form.percent }}%`); form sửa: chuyển `Ảnh` th+td lên sau STT.
- [ ] GREEN + build + screenshots.
- [ ] Commit (2 files).

### Task 5: Fix route "Sửa" bị tráo (đợt↔phiếu)

**Files:** `FE/sd03_fe/src/router/index.js` :75-82

- [ ] RED: e2e `route-update-e2e.js` — đăng nhập, mở `/dot-giam-gia/update/<id đợt>` → assert heading "Cập nhật đợt giảm giá"; `/phieu-giam-gia/update/<id phiếu>` → "Cập nhật phiếu giảm giá". → FAIL (hiện ngược).
- [ ] Implement: swap component mapping 2 route (route đợt → `VoucherFormUpdate`, route phiếu → `DiscountFormUpdate`).
- [ ] GREEN + build.
- [ ] Commit (router/index.js).

### Task 6: Đợt form — bộ lọc Thương hiệu + Chất liệu (form A) + cột TH/CL

**Files:** FE `VoucherForm.vue` + `VoucherFormUpdate.vue` (khu vực bộ lọc biến thể :559-583 / :658-681 + thead), BE `DotGiamGiaRestController.for-discount` (:68-71), `SanPhamChiTietOptionResponse.java` (:13-27), service query tương ứng

- [ ] RED (BE): test JUnit `DotGiamGiaLocTest` — `for-discount?idThuongHieu=X` trả đúng danh sách SP của TH X; response có `tenThuongHieu`/`chatLieu` đầy đủ → FAIL (chưa có param/field).
- [ ] Implement BE: thêm param `idThuongHieu`, `idChatLieu` (nullable) vào controller + service filter; thêm field `tenThuongHieu`, `chatLieu` vào `SanPhamChiTietOptionResponse` + populate. Chạy test BE → GREEN.
- [ ] RED (FE): e2e — mở form đợt: assert tồn tại select "Thương hiệu" + "Chất liệu" trong bộ lọc chọn biến thể + cột "Thương hiệu"/"Chất liệu" trong bảng → FAIL.
- [ ] Implement FE: 2 SearchableSelect (options từ API attribute/products `all-filter-options` như ProductListPage), state `filters.idThuongHieu/idChatLieu` gửi param khi load `for-discount`; thêm 2 cột th+td hiển thị `tenThuongHieu`/`chatLieu`. (Cập nhật cả create + update form.)
- [ ] GREEN: BE test pass + e2e pass + build.
- [ ] Commit (BE files + FE files).

### Task 7: Sản phẩm — combo box trạng thái + đồng bộ nhãn

**Files:** `ProductListPage.vue` (:542-555, :229-233), `ProductVariantPage.vue` (nhãn :1014-1015, :1337-1340, :1440-1443, :1137, :213-216)

- [ ] RED: e2e `product-combo-e2e.js` — mở `/san-pham`: assert (a) không còn 3 button `.status-tab`, có combobox trạng thái (SearchableSelect trigger); (b) chọn "Ngừng kinh doanh" → request có `trangThai=false`; (c) badge dòng + biến thể dùng nhãn "Đang kinh doanh"/"Ngừng kinh doanh". → FAIL.
- [ ] Implement: thay `.tabs-group` → SearchableSelect (options ALL/ACTIVE/INACTIVE, labels "Tất cả"/"Đang kinh doanh"/"Ngừng kinh doanh", `v-model=activeTab`, `@change=selectTab`, `selectTab` guard `tab || 'ALL'`); bỏ tab-pill; đổi nhãn 'Kinh doanh'→'Đang kinh doanh', 'Ngưng kinh doanh'→'Ngừng kinh doanh' ở 2 trang (badge/modal/helper).
- [ ] GREEN + build + screenshot.
- [ ] Commit (2 files).

### Task 8: Biến thể — ảnh-sau-STT + bỏ lọc theo sản phẩm + h1 20px

**Files:** `ProductVariantPage.vue` (thead :1059-1075, td :1103-1113, tag :883-894, :107-109, state :49-65), `ProductListPage.vue` (:352-361), `assets/styles/ProductVariantPage.css` (:36-44)

- [ ] RED: e2e `variant-filter-e2e.js` — vào `/bien-the?idSanPham=...` (link từ bảng SP): assert (a) KHÔNG có text "Đang lọc theo sản phẩm"; (b) bảng có cột thứ tự STT → Ảnh → Mã SP; (c) style h1 `.heading-title` font-size = 20px. (Form add-modal vẫn mở được, có select chọn sản phẩm.) → FAIL.
- [ ] Implement: chuyển cột Ảnh (th+td) lên sau STT; xóa tag block + `params.append('idSanPham')` + state chết; `ProductListPage` push không query; `.heading-title` 24→20px.
- [ ] GREEN + build + screenshots.
- [ ] Commit (3 files).

### Task 9: Khách hàng — bỏ cột Ảnh + bỏ AvatarCard ở form

**Files:** `CustomerListPage.vue` (th :352, td :371-375, colspan :363/:366), `CustomerCreatePage.vue` (:7, :69, :72-73)

- [ ] RED: e2e `customer-avatar-e2e.js` — mở `/khach-hang`: assert (a) không có th "Ảnh"; (b) empty-state colspan=7 (đếm số cột header = 7); mở form thêm KH: không có component avatar (không có input/preview ảnh đại diện). → FAIL.
- [ ] Implement: xóa th+td, colspan 8→7, xóa AvatarCard import/render/onAvatarChange (payload `anhDaiDien: ''` giữ).
- [ ] GREEN + build + screenshots.
- [ ] Commit (2 files).

### Task 10: Khách hàng — bỏ mã DC + chỉ hiện địa chỉ mặc định + nút "Sửa"

**Files:** `CustomerAddressModal.vue` (:275, :280), `CustomerListPage.vue` (tag :382-388 + CSS :897-908, tooltip :104-125, nút mắt :414-424)

- [ ] RED: e2e `customer-address-e2e.js` — mở list KH (KH có >1 địa chỉ): assert (a) KHÔNG có tag "+N địa chỉ"; (b) row có nút text "Sửa"; (c) mở Sổ địa chỉ modal: không có mã `DC…` (regex `DC\d`); modal vẫn liệt kê nhiều địa chỉ. → FAIL.
- [ ] Implement: xóa tag + tooltip list (giữ địa chỉ mặc định render sẵn); bỏ `.addr-code` + fallback mã; thay icon mắt → button "Sửa" (vẫn `openDetail`); giữ 2 icon còn lại.
- [ ] GREEN + build + screenshots.
- [ ] Commit (2 files).

### Task 11: Nhân viên — địa chỉ hiện đủ + chống trùng SĐT

**Files:** FE `EmployeeListPage.vue` (CSS :1449-1456), `EmployeeCreatePage.vue` (:467-473, mirror :155-171), `api/employee.js`; BE `NhanVienRepository`, `NhanVienService.validate` :596-604, `NhanVienController` (check-sdt), `NhanVienImportService.xacNhan` :163-188; Test: `BE/src/test/java/.../NhanVienSdtTrungTest.java`

- [ ] RED (BE): viết `NhanVienSdtTrungTest` — (1) tạo NV trùng SĐT → validate throw "Số điện thoại đã được sử dụng"; (2) update chính nó (giữ SĐT) → OK; (3) 2 NV SĐT khác nhau → OK. Chạy → FAIL (chưa có check).
- [ ] Implement BE: `existsBySoDienThoai` + `existsBySoDienThoaiAndIdNot` (repository), check trong `validate()` (create + edit exclude id), endpoint `GET /nhan-vien/check-sdt`; import service dedup SĐT (file + hệ thống). Chạy test → GREEN; chạy full `.\mvnw.cmd -q test` (6 fail duc còn nợ không tính — không new fail).
- [ ] RED (FE): e2e `employee-phone-e2e.js` — form thêm NV: nhập SĐT đã tồn tại (tạo NV trước qua API) → blur → hiện lỗi chặn submit; CSS: cột địa chỉ hiển thị full (không ellipsis 2 dòng — assert không có `-webkit-line-clamp:2` effective / text đầy đủ). → FAIL.
- [ ] Implement FE: `kiemTraSdtTrung()` onBlur + validate block (mirror email; fix `catch` im lặng của email → báo lỗi), API `checkSdt`; CSS bỏ clamp.
- [ ] GREEN + FE build.
- [ ] Commit (FE + BE + test).

### Task 12: Cỡ chữ toàn app (tham chiếu = Hóa đơn)

**Files:** `VoucherListPage.vue` (scoped :614-618, :625-628, :669-674, :677-690, :752-768), `assets/screens.css` (:296-312), `CustomerListPage.vue` (:840-856), `EmployeeListPage.vue` (:1385-1401)

- [ ] RED: e2e `typography-e2e.js` — mở 4 trang, đo computed style: VoucherList `.filter-title h3` = 14.5px; label = 11.5px; input = 13px; screens `.data-table td` = 13px + th uppercase; Customer/Employee th = 11.5px/700/uppercase, td = 13px. → FAIL (hiện: 16.8px/13.6px/16px/12.5px/13.5px…).
- [ ] Implement: đổi các giá trị trên (px explicit, chặn `font:inherit` 16px).
- [ ] GREEN + build + screenshot 7 trang.
- [ ] Commit (4 files).

---

## Ghi chú ngoài plan
- Form thêm SP **đã có** validate ảnh sẵn (magic-byte + 5MB) ✓ — không task.
- Bảng SP không có cột ảnh riêng (ảnh trong ô SẢN PHẨM); review "có/không đều được" → không đụng.
- Logo giữ nguyên ở sidebar (AppSidebar) ✓ — không task.
- "Cập nhật có gửi mail" giữ nguyên ✓ — không task.
- Ảnh sản phẩm trong form đợt dùng logo tĩnh (API không trả ảnh) — ngoài review, đánh dấu.
- Email NV chưa check vs khách hàng — ngoài review, đánh dấu.
