# Thống kê + Quản lý lịch làm việc — kết nối dữ liệu thật: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thay mock data của 3 màn Thống kê / Lịch làm việc / Lịch ca làm bằng API thật: migration sửa schema drift, API `/api/thong-ke/*` tính từ hóa đơn thật, bảng `ca_lam`/`lich_lam_viec` + CRUD/xếp ca tự động/import, FE nối API — không đổi bố cục.

**Architecture:** BE Spring Boot thêm 1 service tổng hợp thống kê (read-only, aggregate trong Java trên ~50 hóa đơn) + 2 module CRUD (`ca_lam`, `lich_lam_viec`) theo pattern `QuyenGuard`/`PageResponse` sẵn có. FE giữ nguyên template; tầng `statisticsData.js`/`scheduleData.js` chuyển từ mock generator sang helper thuần + module API mới. Biểu đồ dùng lại SVG components nội bộ (`BarLineChart.vue`, `PieChart.vue`) — không thêm thư viện.

**Tech Stack:** Spring Boot 3 (JPA, ddl-auto=none), SQL Server 2017 (`PolyShoesDB`), JUnit5 + Mockito; Vue 3 + vue-router 5 + axios (`src/api/http.js`), exceljs (export/import sẵn có).

**Spec:** `docs/superpowers/specs/2026-10-19-thong-ke-lich-lam-viec-design.md`

## Global Constraints

- Windows/PowerShell: FE build = `npm.cmd run build` (trong `FE/sd03_fe`); BE test = `BE\mvnw.cmd test`; java `C:\Program Files\Java\jdk-17\bin\java.exe`.
- Migration chạy tay: `sqlcmd -S 127.0.0.1,1433 -U sa -P 12131415 -d PolyShoesDB -i <file>` — KHÔNG dùng `-S localhost` (login fail). `spring.jpa.hibernate.ddl-auto=none`.
- Migration pattern: `IF COL_LENGTH('tbl.col') IS NULL ALTER TABLE ...` (như file trong `BE/src/main/resources/db/migration/`). Không DROP/DELETE/sửa dữ liệu cũ.
- FE: không thêm thư viện mới; không đổi layout/màu brand/sidebar; text UI tiếng Việt.
- BE: endpoint mới đi pattern `QuyenGuard` (header `X-Vai-Tro`, FE tự gắn) + `PageResponse` như `NhanVienController`. Không sửa `SecurityConfig`.
- Số liệu thật: 50 hóa đơn (ngay_tao 2025-04-10→2026-09-21), 64 KH, 69 NV, `trang_thai` ∈ {CHO_XAC_NHAN, DA_XAC_NHAN, CHO_GIAO_HANG, DANG_GIAO_HANG, DA_GIAO_HANG, DA_HOAN_THANH, DA_HUY, DA_HOAN_TIEN}. Trống thì hiển thị 0 — không giả số.
- **Quyết định role (chốt theo FE router hiện tại, user chưa phản đối):** `GET /api/thong-ke/*` = ADMIN+STAFF (router `/thong-ke` chỉ `requiresAuth`); `ca-lam`/`lich-lam-viec` = ADMIN (router chặn `!laAdmin()`).
- **Quyết định DTO field names:** BE trả đúng tên field mà template đang bind để FE đổi minimal (ví dụ `tong-quan` trả `doanhThu/soDon/sanPhamBan` chứ không phải `tongDoanhThu/tongDonHang` — xem Task 3).
- **Quyết định tính toán:** aggregate trong Java trên entity/DTO rows (mock repository trong unit test); data tiny (50 đơn, 69 NV) nên không cần SQL-side aggregation. Paging NV/SP: fetch all + sort + `PageImpl` trong service.
- Nhãn VI trạng thái đơn (đúng thứ tự FE): Chờ xác nhận, Đã xác nhận, Chờ giao hàng, Đang giao hàng, Đã giao hàng, Đã hoàn thành, Đã hủy, Đã hoàn tiền — map từ code DB ở trên.
- Tiền mặt = join `hoa_don.id_phuong_thuc_thanh_toan → phuong_thuc_thanh_toan.id_hinh_thuc_thanh_toan → hinh_thuc_thanh_toan.ma_hinh_thuc IN ('TM','CASH')`.
- Commit per task trên nhánh `ha-anh`. Không push trừ khi user yêu cầu.

## Review Focus

5 failure mode dễ bite nhất (mỗi dòng có test/smoke sở hữu trong task):

1. Hóa đơn `DA_HUY`/`DA_HOAN_TIEN` hoặc `da_xoa=1` vẫn bị cộng vào doanh thu → `ThongKeServiceTest.tongQuan_loaiDonHuyHoanTienVaDaXoa` (Task 3).
2. `tienMat` với lọc thương hiệu phải pro-rata theo tỷ trọng dòng TH trong đơn tiền mặt nhiều TH → `ThongKeServiceTest.tongQuan_locThuongHieuTinhTienMatTheoTyTrong` (Task 3).
3. Tăng trưởng chu-ky phải so "cửa sổ bằng độ dài ngay trước kỳ" (không phải tháng dương lịch) → `ThongKeServiceTest.chuKy_tangTruongCuaSoBangDoDai` (Task 4).
4. Xóa ca còn lịch gán phải 409 + message thân thiện (không 500 FK) → `CaLamServiceTest.xoaCaConLichGanBiChan` (Task 6).
5. Import lịch hàng loạt phải bắt trùng lịch ngay trong chính batch (ngoài trùng DB) → `LichLamViecServiceTest.import_batTrungTrongBatch` (Task 7).

---

### Task 1: Đổi nút Import Excel trên màn Lịch làm việc sang style đỏ/nền trắng

User request trực tiếp: button đen (`.btn-action-dark`) trên `WorkSchedulePage.vue` đổi sang style như Import Excel màn Nhân viên (chữ `#f43f5e`, nền trắng, border `#ffe4e6` = `.btn-action-outline`).

**Files:**
- Modify: `FE/sd03_fe/src/pages/Schedule/WorkSchedulePage.vue:507` (dùng class)
- Modify: `FE/sd03_fe/src/pages/Schedule/WorkSchedulePage.vue:1177-1204` (xóa 3 rule CSS `.btn-action-dark`)

**Interfaces:**
- Consumes: class `.btn-action-outline` đã có sẵn (được nút "Cập nhật doanh thu" màn Thống kê + nút Import Excel màn Nhân viên dùng).
- Produces: không đổi interface. Grep xác nhận `.btn-action-dark` chỉ xuất hiện ở file này (4 chỗ: 1 usage + 3 CSS).

- [ ] **Step 1: Đổi class + xóa CSS cũ**

Tại dòng 507 đổi `class="btn-action-dark"` → `class="btn-action-outline"`. Xóa hẳn 3 block CSS `.btn-action-dark` / `.btn-action-dark svg` / `.btn-action-dark:hover` (dòng 1177-1204).

- [ ] **Step 2: Verify build**

Run: `npm.cmd run build` (workdir `FE/sd03_fe`)
Expected: build thành công, không lỗi.

- [ ] **Step 3: Commit**

```bash
git add FE/sd03_fe/src/pages/Schedule/WorkSchedulePage.vue
git commit -m "style: doi nut Import Excel man lich lam viec sang style do nen trang"
```

---

### Task 2: Migration sửa schema drift (8 cột thiếu)

Sửa `GET /api/hoa-don` đang 500 (`Invalid column name 'da_xoa'` + 7 cột khác). Entity đã map đủ; DB thiếu.

**Files:**
- Create: `BE/src/main/resources/db/migration/migration_sua_schema_drift.sql`

**Interfaces:**
- Consumes: pattern migration hiện có (`IF COL_LENGTH(...) IS NULL`).
- Produces: DB có đủ 8 cột → entity `HoaDon`/`KichCo`/`MauSac`/`KieuDang`/`DotGiamGia`/`XuatXu`/`DeGiay`/`ThanGiay` map khớp; Task 3+ dùng `da_xoa` trong mọi công thức.

- [ ] **Step 1: Viết file migration**

```sql
IF COL_LENGTH('dbo.hoa_don.da_xoa') IS NULL
    ALTER TABLE dbo.hoa_don ADD da_xoa BIT NOT NULL CONSTRAINT DF_hoa_don_da_xoa DEFAULT 0;
IF COL_LENGTH('dbo.kich_co.mo_ta') IS NULL
    ALTER TABLE dbo.kich_co ADD mo_ta NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.mau_sac.mo_ta') IS NULL
    ALTER TABLE dbo.mau_sac ADD mo_ta NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.kieu_dang.mo_ta') IS NULL
    ALTER TABLE dbo.kieu_dang ADD mo_ta NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.dot_giam_gia.mo_ta') IS NULL
    ALTER TABLE dbo.dot_giam_gia ADD mo_ta NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.xuat_xu.mo_ta') IS NULL
    ALTER TABLE dbo.xuat_xu ADD mo_ta NVARCHAR(255) NULL;
IF COL_LENGTH('dbo.de_giay.ma_de_giay') IS NULL
    ALTER TABLE dbo.de_giay ADD ma_de_giay NVARCHAR(50) NULL;
IF COL_LENGTH('dbo.than_giay.ma_than_giay') IS NULL
    ALTER TABLE dbo.than_giay ADD ma_than_giay NVARCHAR(50) NULL;
```

(Đối chiếu `columnDefinition` trong 8 entity trước khi chạy — length/type phải khớp exactly.)

- [ ] **Step 2: Chạy sqlcmd**

Run: `sqlcmd -S 127.0.0.1,1433 -U sa -P 12131415 -d PolyShoesDB -i BE/src/main/resources/db/migration/migration_sua_schema_drift.sql`
Expected: không lỗi.

- [ ] **Step 3: Verify 0 cột thiếu**

Run sqlcmd query đếm `sys.columns` cho 8 cột (loop hoặc UNION) → mong đợi `0`.
Expected: `0`.

- [ ] **Step 4: Smoke `GET /api/hoa-don`**

Run: `curl -s -o NUL -w "%{http_code}" http://localhost:8080/api/hoa-don` (BE đang chạy trên 8080)
Expected: `200` (trước khi sửa là 500). Nếu BE chưa chạy thì khởi động lại trước.

- [ ] **Step 5: Commit**

```bash
git add BE/src/main/resources/db/migration/migration_sua_schema_drift.sql
git commit -m "fix: migration sua schema drift them 8 cot thieu sua loi 500 hoa don"
```

---

### Task 3: BE — API Thống kê phần 1: tong-quan + trang-thai-don (TDD)

**Files:**
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/TongQuanDTO.java` (record)
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/TrangThaiDonDTO.java` (record)
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/DongHoaDonThongKeRow.java` (record trung gian nội bộ: `id, trangThai, tienSauGiamGia, daXoa, ngayTao, idNhanVien, maHinhThuc, chiTiet` — hoặc service tự join 2 query; builder tự quyết cấu trúc tối ưu)
- Create: `BE/src/main/java/com/example/datn/repository/ThongKeRepository.java`
- Create: `BE/src/main/java/com/example/datn/service/ThongKeService.java`
- Create: `BE/src/main/java/com/example/datn/controller/ThongKeController.java`
- Modify: `BE/src/main/java/com/example/datn/config/QuyenGuard.java` — thêm `canXemThongKe(String vaiTroHeader)` = ADMIN|STAFF (copy logic `canQuanLyKhachHang`)
- Test: `BE/src/test/java/com/example/datn/service/ThongKeServiceTest.java`

**Interfaces:**
- Consumes: entities `HoaDon` (`daXoa`, `trangThai`, `tienSauGiamGia`, `ngayTao`, `nhanVien`, `phuongThucThanhToan`), `ChiTietHoaDon` (`thanhTien`, `soLuong`, `trangThai`), `SanPhamChiTiet`→`SanPham`→`ThuongHieu`, `PhuongThucThanhToan`→`HinhThucThanhToan.maHinhThuc`, `KhachHang.ngayTao`. `PageResponse` không dùng ở task này.
- Produces (Task 4-8, FE Task 8):
  - `ThongKeService.tongQuan(LocalDate tu, LocalDate den, String thuongHieuOrNull) → TongQuanDTO`
  - `ThongKeService.trangThaiDon(LocalDate tu, LocalDate den, String thuongHieuOrNull) → List<TrangThaiDonDTO>`
  - `GET /api/thong-ke/tong-quan?tu=&den=&thuongHieu=` → `TongQuanDTO` JSON:
    `{ "doanhThu": long, "tienMat": long, "tienChuyenKhoan": long, "soDon": long, "aov": long, "sanPhamBan": long, "khachMoi": long }`
    (Field names TRÙNG binding template `khoang.*` — xem `StatisticsPage.vue:533-604`.)
  - `GET /api/thong-ke/trang-thai-don?tu=&den=&thuongHieu=` → `[{"ten": string, "soLuong": long}]` — LUÔN đủ 8 dòng theo thứ tự VI ở Global Constraints (thiếu = 0).
  - Guard: `quyenGuard.canXemThongKe(vaiTro)` cho cả 2 endpoint (header optional, thiếu → ADMIN như `QuyenGuard` hiện tại).

**Công thức (từ spec §G2 — không đổi):**
- Lọc TH: không có → `doanhThu = Σ tien_sau_giam_gia` đơn `trang_thai NOT IN ('DA_HUY','DA_HOAN_TIEN') AND da_xoa=0 AND ngay_tao ∈ [tu,den]`; `soDon` = COUNT đơn đó; `tienMat` = Σ `tien_sau_giam_gia` của đơn có `ma_hinh_thuc IN ('TM','CASH')`; `tienChuyenKhoan = doanhThu − tienMat`; `sanPhamBan` = Σ `so_luong` dòng chi tiết của các đơn trên; `aov = soDon>0 ? round(doanhThu/soDon) : 0`; `khachMoi` = COUNT `khach_hang.ngay_tao ∈ [tu,den]` (không phụ thuộc TH).
- Có `thuongHieu` (line-level): `doanhThu` = Σ `thanh_tien` dòng TH (đơn hợp lệ trên); `soDon` = COUNT đơn chứa ≥1 dòng TH; `tienMat` = Σ theo từng đơn tiền mặt: `tien_sau_giam_gia × (tỷ trọng dòng TH / tổng thanh_tien hợp lệ của đơn)`; `sanPhamBan` = Σ `so_luong` dòng TH.
- `trang-thai-don` không TH: COUNT đơn `da_xoa=0 AND ngay_tao ∈ [tu,den]` nhóm theo `trang_thai`. Có TH: COUNT **đơn khác nhau** chứa ≥1 dòng TH (đơn hợp lệ) nhóm theo `trang_thai`.

- [ ] **Step 1: Viết test FAILING**

`ThongKeServiceTest.java` — Mockito mock `ThongKeRepository` (+ repo cần khác nếu tách), construct entity fixtures. Test cases (tên + assertion chính):

- `tongQuan_tinhDungCongThucCoBan`: 2 đơn hợp lệ (1 TM 1 triệu, 1 BANK 2 triệu) + 1 đơn DA_HUY 9 triệu → `doanhThu=3_000_000, soDon=2, aov=1_500_000, tienMat=1_000_000, tienChuyenKhoan=2_000_000`.
- `tongQuan_loaiDonHuyHoanTienVaDaXoa`: đơn DA_HUY, DA_HOAN_TIEN, và đơn `da_xoa=true` (kể cả DA_HOAN_THANH) → không vào `doanhThu/soDon/sanPhamBan`.
- `tongQuan_locThuongHieuTinhTienMatTheoTyTrong`: đơn tiền mặt 1 triệu có 2 dòng (TH A 750k, TH B 250k), filter TH A → `doanhThu=750_000, tienMat=750_000`; thêm đơn BANK 2 triệu 1 dòng TH A → `soDon=2`.
- `tongQuan_khongCoDonTraVe0`: range trống → tất cả field = 0, không chia 0.
- `trangThaiDon_du8DongTheoThuTuVi`: fixture 3 đơn (CHO_XAC_NHAN ×2, DA_HUY ×1) → 8 dòng, `soLuong` đúng dòng, các dòng kia 0; thứ tự ten đúng.
- `trangThaiDon_khiLocTHDemDonKhacNhau`: 1 đơn chứa 2 dòng TH A → đếm 1 (không 2).

- [ ] **Step 2: Chạy test FAIL**

Run: `BE\mvnw.cmd test -Dtest=ThongKeServiceTest` (workdir repo root)
Expected: FAIL (compile error — class chưa tồn tại / method chưa có).

- [ ] **Step 3: Implement**

`ThongKeRepository`: JPQL/derived methods nạp đơn + chi tiết trong khoảng ngày (sau Task 2 entity map đủ — JPQL bình thường). `ThongKeService`: aggregate công thức trên; tách method private `locDonHopLe(tu, den)`, `tienMatCuaDon(hd)`. `ThongKeController`: 2 GET endpoint như Interfaces. `QuyenGuard.canXemThongKe`.

- [ ] **Step 4: Chạy test PASS**

Run: `BE\mvnw.cmd test -Dtest=ThongKeServiceTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add BE/src/main/java/com/example/datn/dto/ThongKe BE/src/main/java/com/example/datn/repository/ThongKeRepository.java BE/src/main/java/com/example/datn/service/ThongKeService.java BE/src/main/java/com/example/datn/controller/ThongKeController.java BE/src/main/java/com/example/datn/config/QuyenGuard.java BE/src/test/java/com/example/datn/service/ThongKeServiceTest.java
git commit -m "feat: API thong ke tong-quan va trang-thai-don tinh tu hoa don that"
```

---

### Task 4: BE — API Thống kê phần 2: chuoi / chu-ky / thuong-hieu / nhan-vien / san-pham (TDD)

**Files:**
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/ChuoiDTO.java` (record: `nhan, giaTri`)
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/ChuoiThuongHieuDTO.java` (record: `ten, values: List<Long>`)
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/ThuongHieuDTO.java` (record: `ten, doanhThu, soDon`)
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/ChuKyDTO.java` (record: `nhan, doanhThu, soDon, aov, tangTruong`)
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/NhanVienThongKeDTO.java` (record: `id, maNhanVien, ten, vaiTro, anh, don, spBan, doanhThu`)
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/SanPhamThongKeDTO.java` (record: `maSanPham, tenSanPham, tenThuongHieu, daBan, soTra, doanhThu, tonKho`)
- Modify: `BE/src/main/java/com/example/datn/service/ThongKeService.java` (thêm 5 method)
- Modify: `BE/src/main/java/com/example/datn/controller/ThongKeController.java` (thêm 6 GET)
- Modify: `BE/src/test/java/com/example/datn/service/ThongKeServiceTest.java`

**Interfaces:**
- Consumes: `ThongKeService`/`ThongKeRepository` Task 3; `NhanVienRepository`, `SanPhamRepository`, `SanPhamChiTietRepository`, `KhachHangRepository` sẵn có; `PageResponse.from(Page)` (`dto/Common/PageResponse.java`).
- Produces (FE Task 8):
  - `GET /api/thong-ke/chuoi?tu=&den=&kieu=ngay|tuan|thang&thuongHieu=` → `[{"nhan": "dd/MM"|"MM/yyyy", "giaTri": long}]` — `giaTri` = Σ `sanPhamBan` (số SP bán) theo kỳ; `kieu=ngay` nhan `dd/MM`, `tuan` nhan `dd/MM` đầu tuần (Thứ 2), `thang` nhan `MM/yyyy`. Chỉ trả kỳ có dữ liệu (giống mock `tinhChuoiBanHang`).
  - `GET /api/thong-ke/chuoi-thuong-hieu?tu=&den=&kieu=&theo=doanhThu|soDon` → `[{"ten": string, "values": [long,...]}]` — top 5 TH theo `doanhThu`, `values` align index với `chuoi` cùng params (chuỗi cột "Đường" panel TH).
  - `GET /api/thong-ke/thuong-hieu?tu=&den=` → `[{"ten", "doanhThu", "soDon"}]` sort giảm dần; 5 dòng đầu + dòng `ten="Thương hiệu khác"` (tổng phần còn lại; không có thì 0).
  - `GET /api/thong-ke/chu-ky` (KHÔNG nhận param) → `[{"nhan":"Hôm nay"|"Tuần này"|"Tháng này"|"Năm nay", "doanhThu", "soDon", "aov", "tangTruong"}]` — kỳ hiện tại tính từ `LocalDate.now()`; **kỳ trước = cửa sổ độ dài bằng nhau ngay trước** (Hôm nay→hôm qua; 7 ngày→7 ngày trước; mùng 1→hôm nay vs cửa sổ bằng độ dài trước đó; 1/1→hôm nay vs cửa sổ trước). `prev.doanhThu==0 && cur>0 → tangTruong=100`; cả hai 0 → 0; else `round((cur-prev)/prev*100)`. Doanh thu các kỳ = công thức Task 3 (loại hủy/hoàn/da_xoa).
  - `GET /api/thong-ke/nhan-vien?tim=&sort=doanhThuCao|doanhThuThap|donNhieu&page=&size=&tu=&den=` → `PageResponse<NhanVienThongKeDTO>` — **tất cả nhân viên** (chưa có đơn → don/spBan/doanhThu=0); `tim` LIKE mã/tên; `don` = COUNT đơn hợp lệ trong khoảng của NV; `spBan` = Σ so_luong; `doanhThu` = Σ tien_sau_giam_gia; `ten` = `tenTaiKhoan`; `vaiTro` = `tenVaiTro`; `anh` = `anhNhanVien`. Sort: `doanhThuCao` DESC (dòng 0 tự chèn cuối theo sort DESC chuẩn — OK), `doanhThuThap` ASC, `donNhieu` DESC.
  - `GET /api/thong-ke/san-pham?tim=&thuongHieu=&tonKho=con|sapHet|het|''&sort=banChay|doanhThu|tonKho&page=&size=&tu=&den=` → `PageResponse<SanPhamThongKeDTO>` — **tất cả SP**; `daBan/doanhThu` từ dòng chi tiết hợp lệ (loại hủy/hoàn/da_xoa) trong khoảng; `soTra` = Σ so_luong dòng của đơn `DA_HOAN_TIEN` (da_xoa=0); `tonKho` = Σ `san_pham_chi_tiet.so_luong`; `tonKho` filter: `con` >50, `sapHet` 1..50, `het` =0 (FE bỏ filter client `sanPhamLoc`); `thuongHieu` filter theo tên server-side; sort theo field tương ứng.
  - Guard: `canXemThongKe` cho tất cả.

- [ ] **Step 1: Viết test FAILING** (thêm vào `ThongKeServiceTest`)

- `chuoi_kieuNgayNhanDdMm`: 2 ngày có dữ liệu (01/03: 3 SP, 02/03: 5 SP) → 2 phần tử `{nhan:"01/03",giaTri:3}`, `{nhan:"02/03",giaTri:5}`.
- `chuoi_kieuThangNhanMmYyyy`: 2 ngày cùng tháng 03/2026 → 1 phần tử `{nhan:"03/2026", giaTri: sum}`.
- `chuKy_tangTruongCuaSoBangDoDai`: với `now` cố định (inject `Clock` hoặc truyền `LocalDate hienTai` vào service — builder tự quyết, test phải deterministic): tuần hiện tại doanh thu 200k, cửa sổ 7 ngày trước 100k → `tangTruong=100`; thêm case prev=0 cur>0 → 100; prev=0 cur=0 → 0.
- `thuongHieu_top5VaKhac`: 7 TH doanh thu 700k..100k → 6 phần tử; phần tử 6 = `{ten:"Thương hiệu khác", doanhThu: 100k+...}`; sort DESC.
- `nhanVien_nvChuaBanVanXuatHienZero`: 2 NV, 1 NV có 2 đơn hợp lệ 3 triệu, 1 NV không đơn → 2 dòng; dòng không đơn `don=spBan=doanhThu=0`; `sort=doanhThuThap` → NV 0 lên đầu.
- `nhanVien_timTheoMaVaTen`: `tim="NV0"` → chỉ NV mã bắt đầu NV0.
- `sanPham_tonKhoVaSoTra`: SP A: 2 đơn hợp lệ (5 SP, 3 SP), 1 đơn DA_HOAN_TIEN trả 2 → `daBan=8, soTra=2`; SP B chưa bán → `daBan=0`; `tonKho` từ fixture `SanPhamChiTiet`; filter `tonKho=het` → chỉ SP `tonKho=0`.
- `sanPham_locThuongHieuServerSide`: fixture 2 TH → filter `thuongHieu="Nike"` chỉ trả SP Nike.

- [ ] **Step 2: Chạy test FAIL**

Run: `BE\mvnw.cmd test -Dtest=ThongKeServiceTest`
Expected: FAIL (method chưa tồn tại).

- [ ] **Step 3: Implement**

Service thêm 5 public method + helper gom nhóm theo kỳ (`ngay`→`tuan` (Thứ 2)→`thang`). `chu-ky` dùng `Clock` (inject vào constructor, default `Clock.systemDefaultZone()`) để test deterministic. Controller thêm 6 GET. Paging: fetch toàn bộ rows đã filter → sort Java → slice `PageImpl` → `PageResponse.from`.

- [ ] **Step 4: Chạy test PASS**

Run: `BE\mvnw.cmd test -Dtest=ThongKeServiceTest`
Expected: PASS (cả test Task 3 + mới).

- [ ] **Step 5: Smoke curl tất cả 6 endpoint mới** (BE đang chạy, range có dữ liệu: `tu=2026-01-01&den=2026-10-19`)

Run lần lượt curl, kiểm tra JSON đúng shape (vd `chu-ky` đủ 4 dòng, `nhan-vien.totalElements` ≥ 69, `san-pham` có row).
Expected: HTTP 200, field names đúng như Interfaces.

- [ ] **Step 6: Commit**

```bash
git add -A BE/src/main/java/com/example/datn/dto/ThongKe BE/src/main/java/com/example/datn/service/ThongKeService.java BE/src/main/java/com/example/datn/controller/ThongKeController.java BE/src/test/java/com/example/datn/service/ThongKeServiceTest.java
git commit -m "feat: API thong ke chuoi chu-ky thuong-hieu nhan-vien san-pham"
```

---

### Task 5: Migration + entities ca_lam / lich_lam_viec

**Files:**
- Create: `BE/src/main/resources/db/migration/migration_lich_lam_viec.sql`
- Create: `BE/src/main/java/com/example/datn/entity/CaLam.java`
- Create: `BE/src/main/java/com/example/datn/entity/LichLamViec.java`
- Create: `BE/src/main/java/com/example/datn/repository/CaLamRepository.java`
- Create: `BE/src/main/java/com/example/datn/repository/LichLamViecRepository.java`

**Interfaces:**
- Consumes: pattern entity/repository hiện có (Lombok `@Getter/@Setter`, `@Column(name=...)` explicit, `columnDefinition = "NVARCHAR..."` khi cần).
- Produces (Task 6-7, FE Task 9):
  - Table `ca_lam(id BIGINT IDENTITY PK, ma_ca NVARCHAR(50) UNIQUE NOT NULL, ten_ca NVARCHAR(100) NOT NULL, bat_dau TIME NOT NULL, ket_thuc TIME NOT NULL, dang_bat BIT NOT NULL DEFAULT 1, ngay_tao DATETIME2, ngay_cap_nhat DATETIME2, nguoi_tao NVARCHAR(100), nguoi_cap_nhat NVARCHAR(100))`
  - Table `lich_lam_viec(id BIGINT IDENTITY PK, id_nhan_vien BIGINT NOT NULL FK→nhan_vien, id_ca_lam BIGINT NOT NULL FK→ca_lam, ngay_lam DATE NOT NULL, ngay_tao/ngay_cap_nhat DATETIME2, nguoi_tao/nguoi_cap_nhat NVARCHAR(100), CONSTRAINT UQ_lich UNIQUE(id_nhan_vien, id_ca_lam, ngay_lam))`
  - Seed 3 ca (INSERT nếu chưa có ma_ca tương ứng): `sang`/Ca sáng/08:00/12:00, `chieu`/Ca chiều/13:00/17:00, `toi`/Ca tối/17:30/21:30, `dang_bat=1`.
  - Entity `CaLam`: fields `id, maCa, tenCa, batDau(LocalTime), ketThuc(LocalTime), dangBat(Boolean), ngayTao, ngayCapNhat, nguoiTao, nguoiCapNhat`. `LichLamViec`: `id, nhanVien(@ManyToOne), caLam(@ManyToOne), ngayLam(LocalDate), ngayTao...`.
  - Repository: `CaLamRepository` (StandardRepository + `existsByTenCaIgnoreCaseAndIdNot`, `existsByTenCaIgnoreCase`, `findByDangBatTrueOrderByBatDauAsc`, query đếm `lich_lam_viec` theo `id_ca_lam`); `LichLamViecRepository` (Standard + `existsByIdNhanVienAndNgayLamAndIdCaLam...` bỏ qua id, `findByIdCaLam`, `deleteByNgayLam`, list lọc theo khoảng/ngày/CA/NV).

- [ ] **Step 1: Viết migration + chạy**

Tạo file SQL đúng schema + seed 3 ca (dùng `IF NOT EXISTS (SELECT 1 FROM ca_lam WHERE ma_ca='sang')` cho từng dòng seed). Chạy sqlcmd như Task 2.
Expected: không lỗi; query `SELECT COUNT(*) FROM ca_lam` ≥ 3.

- [ ] **Step 2: Implement entities + repositories**

Theo pattern entity hiện có. Repositories interfaces (logic Task 6-7).

- [ ] **Step 3: Verify app boot**

Run: `BE\mvnw.cmd spring-boot:run` (hoặc restart BE đang chạy) — hoặc simply để BE đang chạy tự reload nếu devtools; cách chắc nhất: dừng BE cũ, chạy `BE\mvnw.cmd spring-boot:run`.
Expected: startup OK, không lỗi mapping entity (check log).
Dừng lại khi xong verify.

- [ ] **Step 4: Commit**

```bash
git add BE/src/main/resources/db/migration/migration_lich_lam_viec.sql BE/src/main/java/com/example/datn/entity/CaLam.java BE/src/main/java/com/example/datn/entity/LichLamViec.java BE/src/main/java/com/example/datn/repository/CaLamRepository.java BE/src/main/java/com/example/datn/repository/LichLamViecRepository.java
git commit -m "feat: bang ca_lam lich_lam_viec migration entities repositories"
```

---

### Task 6: BE — API Ca làm (TDD)

**Files:**
- Create: `BE/src/main/java/com/example/datn/dto/LichLamViec/CaLamDTO.java` (record: `id, ma, ten, batDau, ketThuc, dangBat` — **field `ma`/`ten` khớp binding FE hiện tại** `ca.ma`, `ca.ten` trong `ShiftSchedulePage.vue`)
- Create: `BE/src/main/java/com/example/datn/dto/LichLamViec/CaLamRequest.java` (`ten, batDau, ketThuc` — String "HH:mm")
- Create: `BE/src/main/java/com/example/datn/service/CaLamService.java`
- Create: `BE/src/main/java/com/example/datn/controller/CaLamController.java`
- Create: `BE/src/main/java/com/example/datn/exception/BiChanException.java` (message; handler → HTTP 409)
- Modify: `BE/src/main/java/com/example/datn/exception/GlobalExceptionHandler.java` (thêm `@ExceptionHandler(BiChanException.class)` → 409)
- Test: `BE/src/test/java/com/example/datn/service/CaLamServiceTest.java`

**Interfaces:**
- Consumes: `CaLamRepository`, `LichLamViecRepository` (Task 5), `DuplicateRecordException` (đã map 400), `BiChanException` (409 mới).
- Produces (FE Task 9):
  - `GET /api/ca-lam` → `[CaLamDTO]` (tất cả, order theo `batDau`)
  - `POST /api/ca-lam` body `{ten, batDau, ketThuc}` → `CaLamDTO` (ma tự sinh slug từ ten, nếu trùng append số)
  - `PUT /api/ca-lam/{id}` → `CaLamDTO`
  - `PUT /api/ca-lam/{id}/trang-thai` → `CaLamDTO` (toggle `dangBat`)
  - `DELETE /api/ca-lam/{id}` → 204; còn lịch gán → 409 `BiChanException` message `"Còn N lịch làm việc đang gắn với ca \"ten\" — hãy xóa hoặc đổi các lịch đó trước"` (mirror FE hiện tại).
  - Validate POST/PUT: ten blank → 400; trùng ten (ignore id khi update, case-insensitive) → `DuplicateRecordException` `"Tên ca làm đã tồn tại — vui lòng chọn tên khác"`; `batDau >= ketThuc` → 400; **overlap** khung giờ với ca khác (kể cả ca tắt — ca tắt vẫn giữ khung giờ) → `DuplicateRecordException` message nêu ca trùng.
  - Guard: `canAdmin` cho mọi method.
  - Slug: `slugMaCa` FE (`scheduleData.js`) là pattern tham khảo — BE tự viết: lower, bỏ dấu, space→`-`.

- [ ] **Step 1: Viết test FAILING**

`CaLamServiceTest`:
- `create_sinhMaVaTraDTO`: ten "Ca sáng mới" → DTO `ma` slug chứa "ca-sang-moi", `dangBat=true`.
- `create_trungTenBi400`: đã có "Ca sáng" → `DuplicateRecordException`.
- `create_ketThucTruocBatDauBi400`: batDau "12:00" ketThuc "08:00` → exception.
- `create_trungKhungGioBi400`: ca 08:00-12:00 đã có, tạo 09:00-13:00 → exception; tạo 12:00-14:00 → OK (không overlap).
- `update_boQuaIdHienTai`: sửa ca id=1 (08:00-12:00) giữ nguyên giờ → OK.
- `xoaCaConLichGanBiChan`: repo đếm lịch = 2 → `BiChanException`, message chứa "2".
- `xoaCaKhongLich_OK`: đếm = 0 → không exception.
- `toggle_doiTrangThai`: `dangBat` true→false.

- [ ] **Step 2: Chạy FAIL** → `BE\mvnw.cmd test -Dtest=CaLamServiceTest` → FAIL.

- [ ] **Step 3: Implement** service + controller + `BiChanException` + handler. `ma` sinh slug + suffix nếu unique violation.

- [ ] **Step 4: Chạy PASS** → `BE\mvnw.cmd test -Dtest=CaLamServiceTest` PASS.

- [ ] **Step 5: Smoke curl** `GET /api/ca-lam` → 200, đủ 3 ca seed; `POST` ca hợp lệ → 201/200; `DELETE` ca sáng (đã seed, có thể chưa có lịch) → 204.
Expected: đúng.

- [ ] **Step 6: Commit**

```bash
git add BE/src/main/java/com/example/datn/dto/LichLamViec BE/src/main/java/com/example/datn/service/CaLamService.java BE/src/main/java/com/example/datn/controller/CaLamController.java BE/src/main/java/com/example/datn/exception/BiChanException.java BE/src/main/java/com/example/datn/exception/GlobalExceptionHandler.java BE/src/test/java/com/example/datn/service/CaLamServiceTest.java
git commit -m "feat: API ca-lam CRUD validate trung ten overlap chan xoa 409"
```

---

### Task 7: BE — API Lịch làm việc (TDD)

**Files:**
- Create: `BE/src/main/java/com/example/datn/dto/LichLamViec/LichLamViecDTO.java` (record: `id, maNhanVien, tenNhanVien, idCa, tenCa, batDau, ketThuc, ngay` — `ngay` String "yyyy-MM-dd"; `tenCa/batDau/ketThuc` embed để FE bỏ `timCa` lookup)
- Create: `BE/src/main/java/com/example/datn/dto/LichLamViec/LichLamViecRequest.java` (`maNhanVien, idCa, ngay`)
- Create: `BE/src/main/java/com/example/datn/dto/LichLamViec/XepTuDongRequest.java` (`ngay`), `XepTuDongResponse.java` (record: `lyDo` nullable String, `themMoi` int)
- Create: `BE/src/main/java/com/example/datn/dto/LichLamViec/ImportLichRequest.java` (`rows: List<ImportLichRow>`; row = `maNhanVien, maCa, ngay`), `ImportLichResponse.java` (record: `themMoi` int, `loi: List<ImportLoiRow>`; row = `dong, lyDo`)
- Create: `BE/src/main/java/com/example/datn/service/LichLamViecService.java`
- Create: `BE/src/main/java/com/example/datn/controller/LichLamViecController.java`
- Test: `BE/src/test/java/com/example/datn/service/LichLamViecServiceTest.java`

**Interfaces:**
- Consumes: Task 5-6 repos/entities; `DuplicateRecordException`, `ResourceNotFoundException`.
- Produces (FE Task 9):
  - `GET /api/lich-lam-viec?tu=&den=&tim=&idCa=&ngay=` → `[LichLamViecDTO]` (FE tự paginate client như hiện tại); `tim` LIKE mã/tên NV; `tu/den` theo `ngay_lam`; `ngay` override `tu=den=ngay`.
  - `POST /api/lich-lam-viec` body `LichLamViecRequest` → `LichLamViecDTO`; validate: NV tồn tại, CA tồn tại & `dangBat=true`, `(nv, ngay, ca)` chưa trùng → `DuplicateRecordException` `"Trùng lịch làm việc"`; ca tắt → 400 message `"Ca \"ten\" đang tắt — hãy bật ca trước"`; thiếu NV/ca → 400/`ResourceNotFoundException`.
  - `PUT /api/lich-lam-viec/{id}` — validate như trên, bỏ qua id hiện tại.
  - `DELETE /api/lich-lam-viec/{id}` → 204.
  - `DELETE /api/lich-lam-viec?ngay=yyyy-MM-dd` → xóa toàn bộ lịch ngày (đặt lại lịch).
  - `POST /api/lich-lam-viec/xep-tu-dong` body `{ngay}` → `XepTuDongResponse` — port `xepCaTuDong` (`FE/.../scheduleData.js:154`): ca đang bật sort `batDau` asc; NV `trangThai=true` chưa có lịch ngày đó; round-robin `ca[i % caCount]`; không có ca bật → `{lyDo:"khongCa", themMoi:0}`; không ứng viên → `{lyDo:"khongUngVien", themMoi:0}`; else `{lyDo:null, themMoi:n}`.
  - `POST /api/lich-lam-viec/import` body `{rows:[{maNhanVien, maCa, ngay}]}` → `ImportLichResponse` — validate từng dòng: NV tồn tại, ca tồn tại theo `maCa` + đang bật, ngày hợp lệ, trùng `(nv,ngay,ca)` trong DB **hoặc trong chính batch** → `loi` row `{dong (1-based index trong rows), lyDo}`; dòng hợp lệ → insert (1 transaction); `{themMoi, loi}`.
  - Guard: `canAdmin` mọi method.

- [ ] **Step 1: Viết test FAILING**

`LichLamViecServiceTest`:
- `create_hopLeTraDTO`: NV+CA fixture → DTO có `tenCa/batDau/ketThuc/ngay` đúng.
- `create_trungLichBi400`: đã có (nv=1, ca=1, 2026-10-20) → `DuplicateRecordException`.
- `create_updateBoQuaId`: update id=2 về đúng (nv,ngay,ca) của chính nó → OK.
- `create_caTatBi400`: ca `dangBat=false` → exception message chứa tên ca.
- `xepTuDong_TuDongPhanCa`: 2 ca bật (sang/chieu), 5 NV chưa lịch → `{lyDo:null, themMoi:5}`; verify DB mock: 5 insert, NV1→sang, NV2→chieu, NV3→sang (round-robin).
- `xepTuDong_KhongCa`: 0 ca bật → `{lyDo:"khongCa", themMoi:0}`.
- `xepTuDong_KhongUngVien`: có ca bật, 0 NV trống → `{lyDo:"khongUngVien", themMoi:0}`.
- `import_batTrungTrongBatch`: 2 dòng cùng (nv,ngay,ca) → 1 insert, 1 `loi` dòng 2 chứa "trùng".
- `import_batCaTatVaKhongTonTai`: dòng ca tắt + dòng NV sai mã → `loi` đúng 2 dòng, `themMoi` = dòng hợp lệ còn lại.
- `deleteNgay_xoaHet`: 3 lịch cùng ngày → `deleteByNgayLam` gọi 1 lần, không exception.

- [ ] **Step 2: Chạy FAIL** → `BE\mvnw.cmd test -Dtest=LichLamViecServiceTest` FAIL.

- [ ] **Step 3: Implement** service + controller. Import: validate hết trước, insert dòng hợp lệ trong `@Transactional`.

- [ ] **Step 4: Chạy PASS** → PASS. Chạy thêm toàn bộ: `BE\mvnw.cmd test` — tất cả PASS.

- [ ] **Step 5: Smoke curl**: `GET /api/lich-lam-viec` → `[]`; `POST /xep-tu-dong {ngay:"2026-10-20"}` → themMoi ≥1 (69 NV, 3 ca); `GET` lại → có dữ liệu; `DELETE ?ngay=2026-10-20` → rỗng.
Expected: đúng flow.

- [ ] **Step 6: Commit**

```bash
git add BE/src/main/java/com/example/datn/dto/LichLamViec BE/src/main/java/com/example/datn/service/LichLamViecService.java BE/src/main/java/com/example/datn/controller/LichLamViecController.java BE/src/test/java/com/example/datn/service/LichLamViecServiceTest.java
git commit -m "feat: API lich-lam-viec CRUD xep-tu-dong import"
```

---

### Task 8: FE — StatisticsPage nối API thật, bỏ mock

**Files:**
- Create: `FE/sd03_fe/src/api/statistics.js`
- Modify: `FE/sd03_fe/src/pages/Statistics/statisticsData.js` (xóa mock, giữ helper)
- Modify: `FE/sd03_fe/src/pages/Statistics/StatisticsPage.vue` (script section — template giữ nguyên gần như toàn bộ)

**Interfaces:**
- Consumes: Task 3-4 endpoints + field names; `http` (`src/api/http.js`); helpers giữ lại: `khoaNgay, docKhoa, dauNgay, cuoiNgay, khoangNhanh, layTangTruongLabel, TRANG_THAI_DON`.
- Produces: module `statistics.js` export:
  ```js
  getTongQuan({ tu, den, thuongHieu })          // → TongQuanDTO
  getChuoi({ tu, den, kieu, thuongHieu })       // → [{nhan, giaTri}]
  getChuoiThuongHieu({ tu, den, kieu, theo })   // → [{ten, values}]
  getThuongHieu({ tu, den })                    // → [{ten, doanhThu, soDon}]
  getChuKy()                                    // → 4 dòng
  getTrangThaiDon({ tu, den, thuongHieu })      // → [{ten, soLuong}]
  getNhanVienThongKe({ tim, sort, page, size, tu, den })   // → PageResponse
  getSanPhamThongKe({ tim, thuongHieu, tonKho, sort, page, size, tu, den }) // → PageResponse
  ```
  (mỗi function `http.get('/thong-ke/...', { params }).then(r => r.data)` — pattern `employee.js`).

- [ ] **Step 1: Xóa mock trong `statisticsData.js`**

Xóa: `SEED_GOC, SO_NGAY_LUU, MAU_TRANG_THAI, MAU_THUONG_HIEU, SAN_PHAM_MAU, mulberry32, hashStr, congNgay(private), taoBangNgay, tinhKhoang, tinhChuoiBanHang, tinhChuKy, tinhTrangThaiDon, tinhSanPham, tinhBangThuongHieu, tinhNhanVien`, và mọi import/use tương ứng. GIỮ: `TRANG_THAI_DON, khoaNgay, docKhoa, dauNgay, cuoiNgay, khoangNhanh, layTangTruongLabel` (+ helper private còn dùng: `pad2, danhSachKhoa` nếu `khoangNhanh` cần).

- [ ] **Step 2: Tạo `src/api/statistics.js`**

Như Interfaces trên.

- [ ] **Step 3: Đổi `StatisticsPage.vue` script sang API**

- Bỏ imports hàm tính mock; import from `@/api/statistics`.
- Refs thay computed mock: `khoang, chuoiBanHang, chuKy, trangThai, topThuongHieu, seriesThuongHieu, nhanVienRows, nhanVienTotal, sanPhamRows, sanPhamTotal` (ref + reactive state).
- `watch [tuNgay, denNgay, thuongHieuChon, kieuTongQuan]` → `Promise.all([getTongQuan, getChuoi, getChuoiThuongHieu, getThuongHieu, getTrangThaiDon])` với params `{tu: tuDate, den: denDate, thuongHieu: thuongHieuChon || undefined, kieu: kieuTongQuan, theo: thTheo}`. (thTheo watch riêng → refetch `getChuoiThuongHieu`.)
- `watch [kwNv, sortNv, pageNv, sizeNv]` + khoảng ngày → `getNhanVienThongKe` → `nhanVienRows = r.content`, `nhanVienTotal = r.totalElements`; hiển thị footer dùng total (giữ markup, đổi binding sang total).
- `watch [kwSp, tonKhoSp, sortSp, pageSp, sizeSp, thuongHieuChon]` + khoảng ngày → `getSanPhamThongKe` → rows/total. **Bỏ computed `sanPhamLoc`, `nhanVienLoc`, `dongNv/dongSp` slice client** — FE nhận page server-side (template vẫn `v-for` rows; footer dùng total).
- Màu: giữ client-side — `mauThuongHieu(i)` palette `['#d92d20','#f59e0b','#eab308','#22c55e','#3b82f6','#94a3b8']` (top 5 + khác) và `mauTrangThai(ten)` map sẵn trong page; áp cho `slicesThuongHieu`, `danhSachTH`, `slicesTrangThai` (gán `mau` khi nhận API rows).
- `chuKy`: fetch lúc `onMounted` + trong `capNhatDoanhThu()` (nút "Cập nhật doanh thu" — refetch toàn bộ watch block trên + `getChuKy`, cập `capLuc` label giờ, **bỏ text "Tự động lúc 22:00"** trong template); KHÔNG phụ thuộc bộ lọc.
- `tyLeChon`/`boiSo`: xóa (API trả số thật).
- Export Excel NV/SP: trước khi export fetch `size=1000` (1 trang lớn) rồi export rows.
- `khoangNhanh` quick buttons giữ nguyên (chỉ đổi ref ngày).

- [ ] **Step 4: Build + verify**

Run: `npm.cmd run build`
Expected: PASS, không import symbol nào đã xóa.

- [ ] **Step 5: Chạy tay màn Thống kê** (FE 5173 + BE 8080 đang chạy)

Mở `/thong-ke`: cards có số thật (doanh thu range mặc định = tháng hiện tại); đổi khoảng/quicks → số đổi; chọn TH → cards/pie đổi; bảng NV/SP phân trang + sort + filter hoạt động; "Cập nhật doanh thu" refetch; "Đường" panel TH có chuỗi thật; console không lỗi network/response shape.

- [ ] **Step 6: Commit**

```bash
git add FE/sd03_fe/src/api/statistics.js FE/sd03_fe/src/pages/Statistics/statisticsData.js FE/sd03_fe/src/pages/Statistics/StatisticsPage.vue
git commit -m "feat: man thong ke goi API that bo mock data"
```

---

### Task 9: FE — Lịch làm việc + Lịch ca làm nối API thật

**Files:**
- Create: `FE/sd03_fe/src/api/schedule.js`
- Modify: `FE/sd03_fe/src/pages/Schedule/scheduleData.js` (xóa store/mock, giữ helper + import parse)
- Modify: `FE/sd03_fe/src/pages/Schedule/WorkSchedulePage.vue`
- Modify: `FE/sd03_fe/src/pages/Schedule/ShiftSchedulePage.vue`

**Interfaces:**
- Consumes: Task 5-7 endpoints. GIỮ trong `scheduleData.js`: `khoaNgay, docNgay, hienThiNgay, slugMaCa, phanTichCsvLich, phanTichDongLich, xuatMauLichLamViec, tachCot/docFileDong` (helpers thuần). XÓA: `danhSachLich, danhSachCa, nhanVienMau, caDangBat, lichCuaNgay, lichCuaCaNgay, timCa, kiemTraTrung, themLich, capNhatLich, xoaLich, xoaLichTheoNgay, xepCaTuDong, themCa, capNhatCa, doiTrangThaiCa, caTrungTen, xoaCa, soLichCuaCa, nhapRowsVaoStore`.
- Produces: module `schedule.js`:
  ```js
  listCaLam() / createCaLam(data) / updateCaLam(id, data) / toggleCaLam(id) / deleteCaLam(id)
  listLich(params) / createLich(data) / updateLich(id, data) / deleteLich(id)
  deleteLichTheoNgay(ngay) / xepTuDong(ngay) / importLich(rows)
  ```
  (`deleteLichTheoNgay`: `http.delete('/lich-lam-viec', { params: { ngay } })`; `importLich`: `http.post('/lich-lam-viec/import', { rows })`.)

- [ ] **Step 1: Tạo `src/api/schedule.js`**

Như trên.

- [ ] **Step 2: `ShiftSchedulePage.vue` → API**

- Refs `cas = ref([])`; `onMounted` + sau mọi mutation → `cas.value = await listCaLam()`.
- `caLoc` computed filter trên `cas` (giữ logic keyword/trạng thái).
- `luuForm`: POST/PUT với `{ten, batDau, ketThuc}` (BE map `ten`→`tenCa` trong request DTO — xem Task 6: request field là `ten`); reload list; catch error → `notifyWarning(msg)`.
- `doiTrangThaiBtn`: `toggleCaLam(id)` → reload → notify.
- `xoaCaBtn`: bỏ check `soLichCuaCa` client (BE chặn 409) → `deleteCaLam(id)` catch 409 → `notifyWarning(message)`; thành công → reload + notify.
- Bỏ import store.

- [ ] **Step 3: `WorkSchedulePage.vue` → API**

- Refs: `lichs = ref([])`, `cas = ref([])`, `dsNhanVien` giữ (fetch `listEmployees({page:0,size:1000})` — bỏ fallback `nhanVienMau`, trống thì bảng trống).
- `onMounted` + `watch [fCa, fNgay]` (keyword giữ client filter trên `lichs` như hiện tại hoặc gửi `tim` lên BE — đơn giản: keyword vẫn client, `fCa/fNgay` gửi params `idCa/ngay`) → `lichs.value = await listLich(params)`.
- `lichLoc` computed: filter keyword trên `lichs` (giữ), sort client (giữ).
- `tenCaCua/gioCua`: dùng `tenCa/batDau/ketThuc` embed trong `LichLamViecDTO` (bỏ `timCa` import); fallback từ `cas`.
- Form thêm/sửa lịch: POST/PUT `{maNhanVien, idCa, ngay}` → reload → notify; validate client giữ (trùng lịch → backend 400 → notifyWarning message).
- `xoaLich`/`xoaLichTheoNgay`: API + confirm giữ nguyên.
- `xepCaTuDongBtn`: `xepTuDong(khoa)` → `{lyDo}`: `khongCa` → notifyWarning "Chưa có ca đang bật..." (copy text hiện tại); `khongUngVien` → notifyWarning tương ứng; else reload + notifySuccess.
- Import Excel: giữ nguyên parse/preview client (`phanTichDongLich` cần `cas` + `dsNhanVien` truyền vào thay store); `xacNhanNhap` → `importLich(rows.map(r => ({maNhanVien: r.maNhanVien, maCa: cas.id→ma, ngay: r.ngay})))` → hiển thị kết quả `{themMoi, loi}` (catch + notify theo format hiện tại) → reload.
- `lichCuaNgay/lichCuaCaNgay` (calendar views): tính từ `lichs` local (1-2 dòng helper inline trong page).
- Bỏ tất cả import store.

- [ ] **Step 4: Build**

Run: `npm.cmd run build`
Expected: PASS.

- [ ] **Step 5: Chạy tay 2 màn**

`/lich-ca-lam`: list 3 ca seed; toggle on/off; thêm/sửa ca (trùng tên/khung giờ → báo lỗi); `/lich-lam-viec`: "Xếp ca tự động" ngày mai → NV vào ca; form thêm/sửa/xóa lịch; "Đặt lại lịch" ngày → trống; import Excel template → preview → confirm → DB có dòng; console không lỗi.

- [ ] **Step 6: Commit**

```bash
git add FE/sd03_fe/src/api/schedule.js FE/sd03_fe/src/pages/Schedule/scheduleData.js FE/sd03_fe/src/pages/Schedule/WorkSchedulePage.vue FE/sd03_fe/src/pages/Schedule/ShiftSchedulePage.vue
git commit -m "feat: man lich lam viec lich ca lam goi API that bo mock store"
```

---

### Task 10: Final verification toàn nhánh

- [ ] **Step 1:** `BE\mvnw.cmd test` → all PASS.
- [ ] **Step 2:** `npm.cmd run build` → PASS.
- [ ] **Step 3:** Smoke curl lại 8 endpoint `/api/thong-ke/*` + `/api/ca-lam` + `/api/lich-lam-viec/*` với range thật → 200, JSON shape đúng.
- [ ] **Step 4:** Chạy tay 3 màn trên `localhost:5173` đối chiếu ảnh mẫu (bố cục giữ nguyên, số liệu thật, console sạch). Kiểm tra thêm `GET /api/hoa-don` = 200 (G1).
- [ ] **Step 5:** `git status` + `git log --oneline -10` — working tree sạch, 9 commit Task 1-9. **Hỏi user trước khi push.**
