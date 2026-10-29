# Thống kê (Email tổng kết + So sánh doanh thu) & Lịch sử hoạt động — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bổ sung 3 phần còn thiếu theo ảnh tham khảo: hộp thoại gửi email tổng kết (+job @Scheduled thật), khu so sánh & tăng trưởng doanh thu, và màn "Lịch sử hoạt động" thay "Lịch ca làm".

**Architecture:** BE thêm 3 endpoint trong `ThongKeController` (`so-sanh`, `gui-mail-tong-ket`, `email-mac-dinh`) + 1 scheduler, tái sử dụng `ThongKeRepository`/`EmailService.guiMailCustom`; sửa nhỏ `GiaoCaService.lichSu` (keyword + tên người nhận). FE: `StatisticsPage` thêm page header, dialog email và mode so sánh trong cùng route `/thong-ke`; `BarLineChart` mở rộng bằng props tùy chọn (không đổi hành vi cũ); trang mới `ActivityHistoryPage` cho route `/lich-su-ca`.

**Tech Stack:** Spring Boot (JPA/JPQL, `ApiExceptionHandler` bắt `IllegalArgumentException` → 400), JUnit5 + Mockito (unit) và `@SpringBootTest` (GiaoCa), Vue 3 + Vite, chart SVG nội bộ, SweetAlert2 notify, `exportExcel`.

**Spec:** `docs/superpowers/specs/2026-10-28-thong-ke-va-lich-su-hoat-dong-design.md` (đặc tả thị giác §3b, thiết kế §5, quyết định người dùng: "Tìm sản phẩm" chỉ lọc bảng Top).

## Global Constraints

- Môi trường Windows/PowerShell: chạy test BE bằng `.\mvnw.cmd ...` với workdir `BE/`; build FE bằng `npm.cmd run build` với workdir `FE/sd03_fe/`.
- Toàn bộ test BE hiện tại phải giữ xanh; **được phép đúng 1 failure pre-existing**: `SecurityIntegrationTest.nhanVienAdmin_duQuyenXemHoaDon_vaQuanLyNhanVien` (401, thiếu tài khoản admin local).
- **Không đổi** công thức doanh thu/AOV/tăng trưởng đang dùng (`ThongKeService.tongQuan`, `statisticsData.tinhChuKy`), không đổi màu/logo/font/styles hiện có, không sửa API `/api/ca-lam` và `/api/lich-lam-viec`.
- **Không migration/schema mới** — mọi thay đổi là query/DTO/code.
- **Không tạo dữ liệu giả**; email trả kết quả thật từ `EmailService.guiMailCustom`; thiếu dữ liệu → hiển thị 0/rỗng.
- Copy tiếng Việt đúng theo design doc §3b; UI tái dùng class/style hiện có.
- Commit sau mỗi task; **hỏi người dùng trước khi push**.
- Test BE chạy theo từng class khi TDD: `.\mvnw.cmd test -Dtest=ThongKeServiceTest` (workdir `BE/`).

## Review Focus

1. **Email "thành công giả"** — `thanhCong` phải là đúng kết quả trả về của `guiMailCustom`, không bao giờ true mặc định → test `guiMailTongKet_mailerTraFalseTraVeThanhCongFalseVaLyDo` (Task 4).
2. **Số liệu so sánh lệch khỏi `tongQuan`** (cùng khoảng ngày nhưng 2 công thức) → test `soSanh_kyCungKhoangTraVeDoanhThuGiongTongQuan` (Task 1).
3. **Chia cho 0 / mẫu số 0**: `soDon=0` → aov=0; kỳ trước = 0 → tangTruong = 100 nếu cur>0, ngược 0; DT=0 toàn bộ → không lỗi, `laCaoNhat=false` → test `soSanh_khongCoDuLieuTatCaKyBang0` (Task 1).
4. **Keyword `LIKE` làm sai phân trang/số dòng** (EXISTS không nhân bản dòng) → test `lichSu_locKeywordTheoMaHoaDonVaTenTaiKhoan` kèm assert tổng số dòng (Task 7).
5. **`BarLineChart` làm hỏng chart cũ** — props mới mặc định `null` → nhánh render cũ giữ nguyên; verify bằng build + soi chart màn Thống kê cũ trong Task 12.

---

### Task 1: BE — Tính các kỳ so sánh (`tinhKySoSanh`)

**Files:**
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/KySoSanhDTO.java`
- Modify: `BE/src/main/java/com/example/datn/service/ThongKeService.java` (thêm method, không sửa method cũ)
- Modify: `BE/src/main/java/com/example/datn/repository/ThongKeRepository.java` (thêm `cacNamCoDuLieu`)
- Test: `BE/src/test/java/com/example/datn/service/ThongKeServiceTest.java`

**Interfaces:**
- Consumes: `ThongKeRepository.findHoaDonTrongKhoang/tfindChiTietTrongKhoang(LocalDateTime, LocalDateTime)` (giữ nguyên), hằng `TRANG_THAI_HUY_HOAN_TIEN`, `MA_HINH_THUC_TIEN_MAT`.
- Produces:
  - `public record KySoSanhDTO(String maKy, String tenKy, LocalDate tu, LocalDate den, long doanhThu, long tienMat, long tienChuyenKhoan, long soDon, long aov, long sanPhamBan, long chenhLech, int tangTruong, boolean laCaoNhat)`
  - `public List<KySoSanhDTO> ThongKeService.tinhKySoSanh(String cheDo, Integer nam, LocalDate tu1, LocalDate den1, LocalDate tu2, LocalDate den2, String thuongHieu)` — ném `IllegalArgumentException` khi `cheDo` không thuộc `thang|quy|nam|2ky`, `nam` ngoài 2000–2100, hoặc `2ky` thiếu/ngày sai (`tu<=den`).
  - `List<Integer> ThongKeRepository.cacNamCoDuLieu()` — `SELECT DISTINCT YEAR(h.ngayTao) FROM HoaDon h ORDER BY YEAR(h.ngayTao) DESC`.

**Quyết định đã chốt (không cần hỏi lại):**
- `den` **bao gồm** ngày cuối kỳ; cách chuyển `LocalDate → LocalDateTime` để query làm **đúng như `tongQuan` đang làm** (đọc `ThongKeService.tongQuan` dòng ~46–49 rồi làm theo).
- Query **theo từng kỳ** (gọi `findHoaDonTrongKhoang`/`findChiTietTrongKhoang` 2 lần/ kỳ) vì `HoaDonThongKeRow` không có ngày tạo — không đổi record đang dùng.
- `cheDo=nam`: năm = `cacNamCoDuLieu()` lọc `<= nam`, lấy tối đa 5; rỗng → `[nam]`.
- Kỳ đầu: `prev = 0`; `chenhLech = doanhThu - prev`; `tangTruong = prev<=0 ? (doanhThu>0?100:0) : Math.round((doanhThu-prev)*100.0/prev)`; `aov = soDon==0 ? 0 : Math.round(doanhThu/(double)soDon)`; `laCaoNhat = doanhThu>0 && doanhThu==max`.
- Brand: nhân bản cách `tongQuanLocThuongHieu` (doanh thu theo dòng hàng khớp, tienMat pro-rate theo tỷ trọng) — viết private helper mới, **không** đụng method cũ.

- [ ] **Step 1: Viết test thất bại** — thêm vào `ThongKeServiceTest` (giữ style mock repo hiện có):
  - `soSanh_thangTraVe12KyTheoThuTuNam`: `tinhKySoSanh("thang", 2026, null…, null)` → size 12, `ky[0].maKy="T1"`, `tu=2026-01-01`, `den=2026-01-31`, `ky[11].tenKy="Tháng 12"`.
  - `soSanh_quyTraVe4Ky`: `cheDo="quy", nam=2026` → `Q1 tu=2026-01-01 den=2026-03-31`, …, `Q4 den=2026-12-31`.
  - `soSanh_tinhChenhLechVaTangTruongTheoCongThucChuan`: stub repo trả DT=0 ở T9, DT=102352150 ở T10, DT=0 ở T11 → T10 `chenhLech=102352150`, `tangTruong=100`; T11 `chenhLech=-102352150`, `tangTruong=-100`; T1 `chenhLech=0`, `tangTruong=0`.
  - `soSanh_khongCoDuLieuTatCaKyBang0`: repo trả list rỗng → 12 kỳ, mọi số = 0, `laCaoNhat=false`.
  - `soSanh_namGioiHan5NamGanNhatCoDuLieu`: `cacNamCoDuLieu` → `[2026,2025,2024,2023,2022,2021]`, `nam=2026` → 5 kỳ `2026..2022`.
  - `soSanh_2kyValidateThieuNgayNemIAE`: `tu2=null` → `assertThatThrownBy` IllegalArgumentException.
  - `soSanh_locThuongHieuTinhDungTienMatTyTrong`: sao fixture của test `tongQuan_locThuongHieuTinhTienMatTheoTyTrong` → assert `tienMat`/`doanhThu` kỳ khớp.
  - `soSanh_kyCungKhoangTraVeDoanhThuGiongTongQuan`: cùng fixture, `tongQuan(tu,den,null).doanhThu == tinhKySoSanh(...)[i].doanhThu`.
- [ ] **Step 2: Chạy xác nhận thất bại** — `.\mvnw.cmd test -Dtest=ThongKeServiceTest` → FAIL (method chưa tồn tại).
- [ ] **Step 3: Implement** — thêm record `KySoSanhDTO`, query `cacNamCoDuLieu`, và `tinhKySoSanh` + private helpers `danhSachKy(...)` (trả list ranh giới kỳ), `tinhMotKy(...)` (công thức 1 kỳ, nhân bản `tongQuan`/`tongQuanLocThuongHieu`), validate IAE thông điệp tiếng Việt.
- [ ] **Step 4: Chạy xác nhận pass** — `.\mvnw.cmd test -Dtest=ThongKeServiceTest` → PASS tất cả (test cũ không đổi).
- [ ] **Step 5: Commit** — `git add BE/... && git commit -m "feat(thong-ke): tinh ky so sanh theo che do thang/quy/nam/2ky"`

---

### Task 2: BE — Top sản phẩm theo kỳ + `SoSanhDTO`

**Files:**
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/SanPhamThongKeRow.java`, `TopSanPhamKyDTO.java`, `SoSanhDTO.java`
- Modify: `ThongKeRepository.java` (query `findSanPhamTrongKhoang`), `ThongKeService.java`
- Test: `ThongKeServiceTest.java`

**Interfaces:**
- Consumes: `tinhKySoSanh` + `danhSachKy` (Task 1).
- Produces:
  - `public record SanPhamThongKeRow(Long idHoaDon, String trangThai, boolean daXoa, LocalDateTime ngayTao, String maSanPham, String tenSanPham, String tenThuongHieu, BigDecimal thanhTien, Integer soLuong)`
  - `public record TopSanPhamKyDTO(String maSanPham, String tenSanPham, String tenThuongHieu, long tongBan, long tongDoanhThu, List<Long> banTheoKy)` — `banTheoKy[i]` = số lượng bán trong kỳ i.
  - `public record SoSanhDTO(List<KySoSanhDTO> ky, List<TopSanPhamKyDTO> topSanPham)`
  - `public SoSanhDTO ThongKeService.soSanh(String cheDo, Integer nam, LocalDate tu1, LocalDate den1, LocalDate tu2, LocalDate den2, String thuongHieu, String keyword)`
- Repo mới: `List<SanPhamThongKeRow> findSanPhamTrongKhoang(LocalDateTime tu, LocalDateTime den)` — JPQL mirror `findChiTietTrongKhoang` + `h.ngayTao`, `sp.maSanPham/sp.tenSanPham` (đọc entity `SanPham` để lấy đúng tên field, `ChiTietHoaDon.sanPhamChiTiet.sanPham`).

**Quyết định đã chốt:**
- Lọc dòng top: bỏ `daXoa`/`DA_HUY`/`DA_HOAN_TIEN`; `thuongHieu` khớp tên (không phân biệt hoa thường, trim); `keyword` khớp `maSanPham` **hoặc** `tenSanPham` (contains, lowercase) — **chỉ áp cho bảng top**, không ảnh hưởng `ky` (quyết định người dùng).
- Sắp xếp: `tongBan` giảm dần, tie-break `tongDoanhThu` giảm dần, `limit 10`.
- Bucket theo kỳ bằng đúng ranh giới từ `danhSachKy` (cùng tham số với `tinhKySoSanh`).

- [ ] **Step 1: Viết test thất bại** (ThongKeServiceTest):
  - `soSanh_topSapXepTheoTongBanGiamDanVaGioiHan10`: 12 dòng SP → lấy 10, thứ tự đúng, `banTheoKy` đúng kỳ.
  - `soSanh_topLocKeywordTheoMaVaTen`: keyword `SP00` match mã, keyword `áo` match tên (đều case-insensitive) → đúng dòng.
  - `soSanh_topLocTheoThuongHieu`: `thuongHieu="FF Shirt"` → chỉ dòng SP thương hiệu đó (kèm test `ky` vẫn tính brand theo cách pro-rate).
  - `soSanh_topRongKhiKhongCoDuLieu`: repo rỗng → `topSanPham` rỗng, `ky` vẫn đủ số kỳ.
- [ ] **Step 2: Chạy xác nhận thất bại** — `.\mvnw.cmd test -Dtest=ThongKeServiceTest` → FAIL.
- [ ] **Step 3: Implement** — 3 record + query repo + `soSanh(...)` (gọi `tinhKySoSanh`, fetch SP theo khoảng gộp, bucket, lọc, sort, limit).
- [ ] **Step 4: Chạy pass** — `.\mvnw.cmd test -Dtest=ThongKeServiceTest` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(thong-ke): so sanh tra ve top san pham theo ky"`

---

### Task 3: BE — Endpoint `GET /api/thong-ke/so-sanh`

**Files:**
- Modify: `BE/src/main/java/com/example/datn/controller/ThongKeController.java`
- Test: Create `BE/src/test/java/com/example/datn/controller/ThongKeControllerSoSanhTest.java`

**Interfaces:**
- Consumes: `ThongKeService.soSanh(...)` (Task 2), `QuyenGuard.canQuanLyKhachHang()`, `ApiExceptionHandler`.
- Produces: `GET /api/thong-ke/so-sanh?cheDo=&nam=&tu1=&den1=&tu2=&den2=&thuongHieu=&keyword=` → `SoSanhDTO` (cheDo bắt buộc; còn lại optional; validation IAE → 400 `{message}`).

- [ ] **Step 1: Viết test thất bại** — style `DotGiamGiaLocTest`: `standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler())`, mock `ThongKeService`:
  - `soSanh_hopLeTraVe200VaGuiThamSo`: service trả `SoSanhDTO` 12 kỳ → 200, verify `soSanh("thang", 2026, null…, "FF Shirt", "SP00")`.
  - `soSanh_serviceNemIAETraVe400CoMessage`: mock ném IAE("cheDo không hợp lệ") → 400, body `{"message":"cheDo không hợp lệ"}`.
- [ ] **Step 2: Chạy thất bại** — `.\mvnw.cmd test -Dtest=ThongKeControllerSoSanhTest` → FAIL.
- [ ] **Step 3: Implement** — thêm `@GetMapping("/so-sanh")` với `@RequestParam String cheDo`, optional params `@DateTimeFormat(iso=DATE)`, gọi `quyenGuard.canQuanLyKhachHang()` rồi `thongKeService.soSanh(...)`.
- [ ] **Step 4: Chạy pass** — `.\mvnw.cmd test -Dtest=ThongKeControllerSoSanhTest` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(thong-ke): api so sanh doanh thu"`

---

### Task 4: BE — `BaoCaoTongKetService` (nội dung email + gửi thật)

**Files:**
- Create: `BE/src/main/java/com/example/datn/dto/ThongKe/GuiMailTongKetRequest.java`, `GuiMailTongKetResponse.java`, `EmailMacDinhDTO.java`
- Create: `BE/src/main/java/com/example/datn/service/BaoCaoTongKetService.java`
- Modify: `ThongKeService.java` (thêm `demDonHuy`)
- Test: Create `BE/src/test/java/com/example/datn/service/BaoCaoTongKetServiceTest.java`; Modify `ThongKeServiceTest.java`

**Interfaces:**
- Consumes: `ThongKeService.tongQuan/soSanh`, `EmailService.guiMailCustom(String,String,String):boolean`, `NhanVienRepository`, `@Value("${spring.mail.username:}")`.
- Produces:
  - `public record GuiMailTongKetRequest(String loai, Integer thang, Integer nam, String emailNhan)` — `loai ∈ {THANG, NAM}`.
  - `public record GuiMailTongKetResponse(boolean thanhCong, String emailNhan, String tieuDe, String lyDo)`
  - `public record EmailMacDinhDTO(String email)`
  - `public GuiMailTongKetResponse BaoCaoTongKetService.guiMailTongKet(GuiMailTongKetRequest req)`
  - `public String BaoCaoTongKetService.emailMacDinh()`
  - `public long ThongKeService.demDonHuy(LocalDate tu, LocalDate den)` — đếm `DA_HUY/DA_HOAN_TIEN` và không `daXoa` trong khoảng (query theo cách `tongQuan` đang convert ngày).
- Validate (ném IAE, thông điệp tiếng Việt): `loai` sai; `THANG` thiếu/thang∉1..12/nam∉2000..2100; `NAM` thiếu nam ngoài khoảng; `emailNhan` rỗng hoặc sai regex `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`.
- `emailMacDinh()`: derived query mới trên `NhanVienRepository` — `findFirstByVaiTro_MaVaiTroAndTrangThaiTrueAndEmailIsNotNullOrderByMaNhanVienAsc("ADMIN")` → fallback `spring.mail.username` → `""`.
- Nội dung email (plain text — `guiMailCustom` escape HTML, `\n` → `<br/>`), đúng thứ tự mục theo design doc §3b ảnh 6–7:
  1. `BÁO CÁO TỔNG KẾT & SO SÁNH DOANH THU` + dòng kỳ (`Tháng 10 Năm 2026 — Hệ thống quản lý bán hàng` / `Năm 2026 — Hệ thống quản lý bán hàng`).
  2. `TỔNG DOANH THU: {n} đ` + `Tiền mặt: {n} đ | Chuyển khoản: {n} đ` (từ `tongQuan` theo kỳ).
  3. `TĂNG TRƯỞNG SO VỚI {THÁNG|NĂM} TRƯỚC: {+x%}` + `Chênh lệch: {+n} đ` (cùng công thức `tinhChuKy`, kỳ trước = kỳ liền trước).
  4. `ĐƠN HOÀN THÀNH: {soDon} | ĐƠN ĐÃ HỦY: {demDonHuy} | AOV (TB ĐƠN): {n} đ | SẢN PHẨM BÁN: {n}`.
  5. `TOP 5 SẢN PHẨM BÁN CHẠY NHẤT {THÁNG 10|NĂM 2026}` — `#1 {ten} ({ma}) — Đã bán: {n} — Doanh thu: {n} đ` (lấy `topSanPham` từ `soSanh`, cắt 5).
  6. `BẢNG ĐỐI CHIẾU DOANH THU CÁC THÁNG TRONG NĂM {năm}` — `Thời gian | Doanh thu | Số đơn | AOV | Tăng trưởng`, kỳ hiện tại gắn `(Hiện tại)`.
  - Định dạng số `#,##` theo `Locale("vi-VN")`, nối `đ`; `loai=NAM`: kỳ = cả năm, kỳ trước = năm trước, top5 = cả năm, bảng so sánh = 12 tháng của năm đó.
  - `tieuDe = "BÁO CÁO TỔNG KẾT & SO SÁNH DOANH THU — Tháng 10 Năm 2026"` (hoặc `— Năm 2026`).

- [ ] **Step 1: Viết test thất bại**:
  - `ThongKeServiceTest.demDonHuy_TinhDonHuyVaLoaiDaXoa` (mock repo).
  - `BaoCaoTongKetServiceTest` (JUnit + Mockito, mock `ThongKeService`, `EmailService`, `NhanVienRepository`):
    - `guiMailTongKet_thangHopLeGuiBangSoLieuThat`: `guiMailCustom` → true; verify nhận đúng `emailNhan`, subject chứa `Tháng 10 Năm 2026`; body chứa `TỔNG DOANH THU`, `TOP 5`, `BẢNG ĐỐI CHIẾU`; response `thanhCong=true`, `lyDo=null`.
    - `guiMailTongKet_mailerTraFalseTraVeThanhCongFalseVaLyDo`: → false + `lyDo` khác rỗng.
    - `guiMailTongKet_loaiSaiThangSaiEmailSaiNemIAE` (3 nhánh).
    - `emailMacDinhLayEmailAdminDauTien` / `emailMacDinhFallbackSangMailUsernameKhiKhongCoAdmin`.
- [ ] **Step 2: Chạy thất bại** — `.\mvnw.cmd test -Dtest=ThongKeServiceTest,BaoCaoTongKetServiceTest` → FAIL.
- [ ] **Step 3: Implement** — `demDonHuy` + `BaoCaoTongKetService` (validate → tính số liệu → build text → `guiMailCustom` → response trung thực).
- [ ] **Step 4: Chạy pass** — chạy lại 2 class → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(thong-ke): dich vu gui mail tong ket doanh thu"`

---

### Task 5: BE — Endpoint gửi email + email mặc định

**Files:**
- Modify: `ThongKeController.java`
- Test: Create `BE/src/test/java/com/example/datn/controller/ThongKeControllerEmailTest.java`

**Interfaces:**
- Consumes: `BaoCaoTongKetService` (Task 4).
- Produces:
  - `POST /api/thong-ke/gui-mail-tong-ket` body `GuiMailTongKetRequest` → `GuiMailTongKetResponse` (200 kể cả `thanhCong=false`; input sai → 400 `{message}`).
  - `GET /api/thong-ke/email-mac-dinh` → `EmailMacDinhDTO`.
  - `ThongKeController` thêm dependency `BaoCaoTongKetService`.

- [ ] **Step 1: Viết test thất bại** — standalone MockMvc + `ApiExceptionHandler`:
    - `guiMailTongKet_hopLeTraVe200VaKetQuaTuService` (mock service trả `thanhCong=false` → vẫn 200, đúng body).
    - `guiMailTongKet_jsonSaiDinhDangTraVe400` (body rỗng/`{}` thiếu `loai` → 400 message).
    - `emailMacDinh_traVeEmailTuService` → 200 `{"email":"..."}`.
- [ ] **Step 2: Chạy thất bại** → FAIL.
- [ ] **Step 3: Implement** — 2 endpoint, mỗi endpoint gọi `quyenGuard.canQuanLyKhachHang()` trước.
- [ ] **Step 4: Chạy pass** → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(thong-ke): api gui mail tong ket va email mac dinh"`

---

### Task 6: BE — Job `@Scheduled` gửi báo cáo tự động

**Files:**
- Modify: `BE/src/main/java/com/example/datn/DatnApplication.java` (thêm `@EnableScheduling`)
- Create: `BE/src/main/java/com/example/datn/scheduler/BaoCaoTongKetScheduler.java`
- Test: Create `BE/src/test/java/com/example/datn/scheduler/BaoCaoTongKetSchedulerTest.java`

**Interfaces:**
- Consumes: `BaoCaoTongKetService.guiMailTongKet` + `emailMacDinh`.
- Produces:
  - `public static List<String> BaoCaoTongKetScheduler.quyetDinhGui(LocalDate ngay)` → chứa `"THANG"` nếu là ngày cuối tháng; thêm `"NAM"` nếu `31/12`; ngày thường → rỗng.
  - `@Scheduled(cron = "0 59 23 * * *", zone = "Asia/Ho_Chi_Minh") public void guiBaoCaoTuDong()` — build request từ `emailMacDinh()` (rỗng → skip + log), gọi `guiMailTongKet` trong `try/catch` (lỗi chỉ log, không crash app).

- [ ] **Step 1: Viết test thất bại** — `BaoCaoTongKetSchedulerTest` (pure JUnit, không Spring):
    - `quyetDinhGui_ngayCuoiThangGuiThang` (31/01/2026 → `["THANG"]`; 30/04/2026 → `["THANG"]`; 28/02/2026 → `["THANG"]`)
    - `quyetDinhGui_dauGiuaThangKhongGui` (15/10/2026 → `[]`)
    - `quyetDinhGui_cuoiNamGuiCaThangVaNam` (31/12/2026 → `["THANG","NAM"]`)
    - `quyetDinhGui_thangNhuanDungNgay29` (29/02/2028 → `["THANG"]`)
- [ ] **Step 2: Chạy thất bại** — `.\mvnw.cmd test -Dtest=BaoCaoTongKetSchedulerTest` → FAIL.
- [ ] **Step 3: Implement** — `@EnableScheduling` + scheduler như Interfaces; `quyetDinhGui` tính ngày cuối tháng bằng `ngay.withDayOfMonth(ngay.lengthOfMonth()).equals(ngay)`.
- [ ] **Step 4: Chạy pass** → PASS; chạy nốt `.\mvnw.cmd test -Dtest=BaoCaoTongKetServiceTest` để chắc không vỡ.
- [ ] **Step 5: Commit** — `git commit -m "feat(thong-ke): job scheduled gui bao cao tong ket"`

---

### Task 7: BE — `keyword` + `tenNguoiNhan` cho `/api/ca/lich-su`

**Files:**
- Modify: `BE/src/main/java/com/example/datn/repository/CaLamViecRepository.java` (query `lichSu`)
- Modify: `BE/src/main/java/com/example/datn/service/GiaoCaService.java` (signature + map DTO)
- Modify: `BE/src/main/java/com/example/datn/controller/GiaoCaController.java` (param)
- Modify: `BE/src/main/java/com/example/datn/dto/CaLamViecDto.java` (thêm field cuối)
- Test: `BE/src/test/java/com/example/datn/GiaoCaServiceTest.java`

**Interfaces:**
- Consumes: `CaLamViec.nguoiCapNhat` (chuỗi ID nhân viên, ghi lúc kết ca), `NhanVienRepository` (đã có trong `GiaoCaService`).
- Produces:
  - `CaLamViecRepository.lichSu(Long idNhanVien, String trangThai, LocalDateTime tu, LocalDateTime den, String keyword, Pageable pageable)` — thêm `LEFT JOIN c.nhanVien nv` + `AND (:keyword IS NULL OR LOWER(c.maCa) LIKE LOWER(CONCAT('%',:keyword,'%')) OR LOWER(nv.tenTaiKhoan) LIKE … OR LOWER(nv.maNhanVien) LIKE … OR LOWER(nv.email) LIKE … OR EXISTS (SELECT 1 FROM HoaDon h WHERE h.idCaLamViec = c.id AND LOWER(h.maHoaDon) LIKE LOWER(CONCAT('%',:keyword,'%'))))`.
  - `GiaoCaService.lichSu(int page, int size, LocalDate tuNgay, LocalDate denNgay, String trangThai, Long idNhanVien, String keyword, Long callerId, boolean callerAdmin)` — chèn `keyword` **trước** `callerId`; sau khi có `Page`, batch `nhanVienRepository.findAllById` các ID parse được từ `nguoiCapNhat` rồi set `dto.setTenNguoiNhan(ten)`; ca `DANG_MO`/ID không parse được/nhân viên không còn → để `null`.
  - `CaLamViecDto`: `private String tenNguoiNhan;` (Lombok, mặc định null — **không đổi field cũ**).
  - FE hiển thị: `tenNguoiNhan || 'Chưa có'`.
- Cập nhật mọi call site cũ của `lichSu(...)` (grep trước).

- [ ] **Step 1: Viết test thất bại** (GiaoCaServiceTest, `@SpringBootTest`, dùng fixture + cleanup sẵn có):
    - `lichSu_locKeywordTheoMaHoaDonVaTenTaiKhoan`: tạo NV (`tenTaiKhoan` biết trước) + ca + hóa đơn có `maHoaDon` riêng; search keyword theo mã hóa đơn → đúng ca; theo tên tài khoản → đúng ca; keyword lạ → 0 dòng; keyword匹配 không nhân bản dòng (assert `totalElements`).
    - `lichSu_tenNguoiNhanSauKhiKetCa`: ca đang `DANG_MO` → `tenNguoiNhan=null`; sau `ketCa` → bằng tên nhân viên của người kết.
- [ ] **Step 2: Chạy thất bại** — `.\mvnw.cmd test -Dtest=GiaoCaServiceTest` → FAIL (test mới).
- [ ] **Step 3: Implement** — sửa query/service/controller/DTO như Interfaces (không đổi logic phân quyền non-admin ép xem ca của mình).
- [ ] **Step 4: Chạy pass** — `.\mvnw.cmd test -Dtest=GiaoCaServiceTest` → PASS toàn bộ (kèm test cũ).
- [ ] **Step 5: Commit** — `git commit -m "feat(giao-ca): keyword tim kiem va ten nguoi nhan cho lich su"`

---

### Task 8: FE — API email + page header + hộp thoại "Gửi Mail Tổng Kết"

**Files:**
- Modify: `FE/sd03_fe/src/api/statistics.js`
- Modify: `FE/sd03_fe/src/pages/Statistics/StatisticsPage.vue` (script state/handlers, template header + buttons + modal, style mới scoped)

**Interfaces:**
- Consumes: `http` (`api/http.js`), `notifySuccess/notifyError` (`utils/notify.js`), class hiện có `btn-action-outline`, `filter-card`, `card-title-row`, `quick-range-group`.
- Produces (dùng cho Task 10):
  - `fetchEmailMacDinh(): Promise<{email:string}>` → `GET /thong-ke/email-mac-dinh`
  - `guiMailTongKet({loai:'THANG'|'NAM', thang, nam, emailNhan}): Promise<{thanhCong:boolean,emailNhan:string,tieuDe:string,lyDo:string}>` → `POST /thong-ke/gui-mail-tong-ket`
  - `fetchSoSanh(params): Promise<{ky:Array,topSanPham:Array}>` → `GET /thong-ke/so-sanh` (thêm ở Task 10, nhưng viết luôn ở đây cho gọn)
  - State FE: `showEmailModal, emailLoai('THANG'), emailThang(tháng hiện tại), emailNam(năm hiện tại), emailNhan, emailDangGui, emailErr`.
  - 2 hàm mở dialog (`moGuiMail()` — gọi `fetchEmailMacDinh()` điền `emailNhan` nếu rỗng) và `guiBaoCao()`.

**Quyết định đã chốt:**
- Page header (chưa có sẵn) chèn ngay sau `<div class="statistics-page-container">` (dòng ~482), trước `<!-- CARD 1 -->`: title `Thống kê doanh thu`, phụ đề `Tổng hợp kinh doanh, phân tích sản phẩm và theo dõi tăng trưởng bán hàng`, 2 nút góc phải: `Gửi mail tổng kết` (btn viền xanh, icon `bi-envelope`) và `So sánh doanh thu` (btn màu nâu/neutral hiện có — xem class nút sẵn có trong trang, chọn class gần nhất, **không** tạo màu mới lạ mắt).
- 2 nút cũng thêm vào `.card-title-row` của card "Bộ lọc tổng quan", **bên phải** `.quick-range-group` (như ảnh 1).
- Hộp thoại (modal, bấm nền/`Hủy bỏ` để đóng): đúng cấu trúc ảnh 5 — toggle `Báo cáo tổng kết Tháng | Báo cáo tổng kết Năm`; select `Chọn Tháng` (Tháng 1..12), `Chọn Năm` (2020..năm hiện tại); input `Email Admin nhận báo cáo` + chú thích `Mặc định lấy địa chỉ email Admin hệ thống. Bạn có thể thay đổi để kiểm thử gửi vào email bất kỳ.`; box xanh `Trạng thái tự động của hệ thống (@Scheduled): Hệ thống tự động gửi báo cáo vào 23:59 ngày cuối mỗi tháng và 31/12 cuối năm về hộp thư Quản trị viên.`; nút `Hủy bỏ` + `Gửi báo cáo ngay`.
- Validate client: email regex; `THANG` cần thang/nam; disable nút + loading khi gửi; `thanhCong=true` → `notifySuccess` + đóng modal; `false` → `notifyError(lyDo)`; lỗi mạng → `notifyError` chung.

- [ ] **Step 1: Implement script** — thêm 3 API fn + state + `moGuiMail()` + `guiBaoCao()` (dùng `notifySuccess/notifyError`, try/finally `emailDangGui`).
- [ ] **Step 2: Implement template + style** — header block, 2 nút filter card, modal + `<style scoped>` mới (theo phong cách modal/`filter-card` sẵn có).
- [ ] **Step 3: Build** — `npm.cmd run build` (workdir `FE/sd03_fe`) → PASS.
- [ ] **Step 4: Smoke BE** — nếu BE chưa restart, bỏ qua smoke (Task 12 làm集中); commit.
- [ ] **Step 5: Commit** — `git commit -m "feat(fe): nut va hop thoai gui mail tong ket"`

---

### Task 9: FE — Mở rộng `BarLineChart` (cột nhóm + đường %)

**Files:**
- Modify: `FE/sd03_fe/src/components/charts/BarLineChart.vue`

**Interfaces:**
- Consumes: props hiện có `labels/values/color/mode/series/height`.
- Produces (dùng Task 10):
  - Props mới (mặc định `null` → **không đổi hành vi caller cũ**): `barSeries: Array|null` (mỗi phần `{label, color, values}` — cột nhóm trên trục tiền), `lineSeries: Array|null` (đường trên trục riêng), `lineFormat: 'so'|'percent'` (mặc định `'so'`), `showLineLabels: Boolean` (mặc định `false`).
  - Khi `barSeries` có giá trị: vẽ nhóm cột/bộ (`mode='cot'`), `yMax` tính từ `barSeries`; khi `lineSeries` có giá trị: vẽ polyline với thang riêng `lineMin..lineMax` (luôn chứa 0), label điểm theo `lineFormat` (`percent` → `+100%`).

- [ ] **Step 1: Implement** — thêm computed `lineScale`, nhánh render `barSeries`/`lineSeries` trong template; giữ nguyên nhánh cũ khi props mới null.
- [ ] **Step 2: Build** — `npm.cmd run build` → PASS.
- [ ] **Step 3: Xem nhanh** — chạy dev server nếu đang tắt, mở `/thong-ke` xác nhận biểu đồ "Sản phẩm bán được theo ngày" và bảng chu kỳ **không đổi so với trước** (soi bằng mắt, không lỗi console).
- [ ] **Step 4: Commit** — `git commit -m "feat(fe): barlinechart ho tro cot nhom va duong phan tram"`

---

### Task 10: FE — Mode "So sánh & Tăng trưởng doanh thu"

**Files:**
- Modify: `FE/sd03_fe/src/pages/Statistics/StatisticsPage.vue`

**Interfaces:**
- Consumes: `fetchSoSanh` (Task 8), `BarLineChart` props mới (Task 9), `formatVnd`, `exportExcel`, `layTangTruongLabel`, `SearchableSelect`, class `stat-card`, `.filter-card`, `.data-table`, `notifyError`.
- Produces:
  - State: `soSanhMode(false)`, `ssCheDo('thang')`, `ssNam(năm hiện tại)`, `ssTu1/ssDen1/ssTu2/ssDen2('')`, `ssKeyword('')`, `ssData(null)`, `ssDangTai`, `ssErr`.
  - `taiSoSanh()` — gọi `fetchSoSanh({cheDo, nam, tu1, den1, tu2, den2, thuongHieu: thuongHieuChon, keyword: ssKeyword})`; `watch([ssCheDo, ssNam, thuongHieuChon])` → reload (với `2ky` chỉ reload khi đủ 4 ngày).
  - Computed KPI từ `ssData.ky`: `tongDt=Σ`, `tongTm`, `tongCk`, `tongDon=ΣsoDon`, `aovTrungBinh = tongDt/tongDon (0 nếu 0)`, `tangTruongBinhQuan = round(ΣtangTruong / số kỳ)` — đúng số trong ảnh (102.352.150 / 22 = 4.652.370; TB% = trung bình cộng).
  - Markup (đúng §3b ảnh 2–4): wrap khối hiện có trong `<template v-if="!soSanhMode">`, khối so sánh trong `<template v-else>`:
    1. Header variant: nút ← (quay lại `soSanhMode=false`), title `So sánh & Tăng trưởng doanh thu`, phụ đề `Đối chiếu doanh thu, đo lường tốc độ tăng trưởng và theo dõi biến động sức mua sản phẩm`; nút `Gửi mail tổng kết` (mở dialog Task 8) + `Xuất Excel So sánh`.
    2. Filter card "Chế độ đối chiếu": 4 tab `Theo Tháng (12 tháng) / Theo Quý (4 quý) / Theo Năm (Đối chiếu nhiều năm) / Tùy chọn 2 kỳ`; ô `Năm so sánh` (select 2020..năm hiện tại), `Thương hiệu` (dùng `thuongHieuOptions`), `Tìm sản phẩm` (input → `ssKeyword`), `Đặt lại`; khi `2ky` hiện 2 cặp `Từ ngày/Đến ngày` (yyyy-MM-dd).
    3. 6 thẻ KPI (class `stat-card` hiện có).
    4. Card biểu đồ `Doanh thu & Tốc độ tăng trưởng qua các kỳ`: `BarLineChart` với `barSeries=[Tiền mặt, Chuyển khoản]`, `lineSeries=[Tốc độ tăng trưởng (%)]`, `lineFormat="percent"`, `showLineLabels`, `labels=tenKy`.
    5. Bảng `Đối chiếu doanh thu chi tiết các kỳ` (đơn vị VND): đúng cột ảnh; badge `Cao nhất` khi `laCaoNhat`; hàng `TỔNG CỘNG` với `Tăng trưởng TB: {x}%`.
    6. Bảng `Top sản phẩm bán chạy & Đối chiếu sức mua qua các kỳ` (badge `Top 10 mặt hàng`): `STT | MÃ SP | TÊN SẢN PHẨM | THƯƠNG HIỆU | TỔNG BÁN | TỔNG DOANH THU | {tenKy...}` (cuộn ngang).
  - Trạng thái: `ssDangTai` → skeleton/`Đang tải...`; `ssErr` → `notifyError` + text + nút thử lại; `ky` rỗng → text `Không có dữ liệu trong khoảng này`.
  - Xuất Excel `Xuất Excel So sánh`: `exportExcel` với headers bảng đối chi tiết.

- [ ] **Step 1: Implement state + `taiSoSanh` + watch** (script).
- [ ] **Step 2: Implement template 6 khối + bọc `v-if/v-else`** (khối hiện có giữ nguyên 100%).
- [ ] **Step 3: Implement styles** (tab `.seg-toggle/.seg-btn` nếu đã có, ngược lại tái dùng class nút có sẵn; bảng reuse `.data-table`).
- [ ] **Step 4: Build** — `npm.cmd run build` → PASS.
- [ ] **Step 5: Commit** — `git commit -m "feat(fe): man so sanh tang truong doanh thu"`

---

### Task 11: FE — Màn "Lịch sử hoạt động" thay "Lịch ca làm"

**Files:**
- Modify: `FE/sd03_fe/src/router/index.js`
- Modify: `FE/sd03_fe/src/components/AppSidebar.vue`
- Create: `FE/sd03_fe/src/pages/Schedule/ActivityHistoryPage.vue`
- (Giữ nguyên file `ShiftSchedulePage.vue` — chỉ không còn route/menu trỏ tới; `api/schedule.js`, `scheduleData.js` không đổi.)

**Interfaces:**
- Consumes: `getLichSu(params)` từ `api/giaoCa.js` (`GET /ca/lich-su`, đã nhận params tùy ý — chỉ cần truyền `keyword`), `PaginationBar`, class `.filter-card/.data-table/.status-chip` (mượn pattern từ `ShiftSchedulePage`/`GiaoCaListPage`), `formatVnd`.
- Produces:
  - Route: `{ path: '/lich-su-ca', component: ActivityHistoryPage, meta: { ...metaBo, title: 'Lịch sử hoạt động' } }`; `{ path: '/lich-ca-lam', redirect: '/lich-su-ca' }`; **xóa import route component `ShiftSchedulePage`** nếu không còn dùng.
  - Sidebar: `{ label: 'Lịch sử hoạt động', path: '/lich-su-ca' }` thay `{ label: 'Lịch ca làm', path: '/lich-ca-lam' }` (AppSidebar dòng ~68).
  - Page state: `keyword, tuNgay, denNgay, trangThai(''), page, size=10, data(Page), dangTai`; `taiLai()` reset page=0; `watch([tuNgay, denNgay, trangThai])` → reload; nút `Tải lại`.
  - Filter label/placeholder đúng ảnh 9: `TÌM KIẾM` placeholder `Tìm theo tài khoản / mã ca / mã hóa đơn...`, `TỪ NGÀY`, `ĐẾN NGÀY`, `TRẠNG THÁI` (Tất cả / Đang mở → `DANG_MO` / Đã đóng → `DA_KET_CA`), nút `Tải lại`.
  - Bảng cột: `# | NGƯỜI GIAO (avatar tròn 2 chữ đầu + `tenNhanVien`) | NGƯỜI NHẬN (`tenNguoiNhan || 'Chưa có'`) | CA (`maCa` chip) | MỞ (`thoiGianMo` dd/MM HH:mm) | ĐÓNG (`thoiGianKet` hoặc '—') | TIỀN MẶT (`Đ đầu: {tienDauCa} / Cuối: {tienThucTe}`) | CHÊNH LỆCH (`chenhLech`) | TRẠNG THÁI (chip Đang mở/Đã đóng) | HÀNH ĐỘNG (icon tài liệu → `$router.push('/ca/'+id)`)`.
  - Phân trang `PaginationBar` (server `page/size`); `data` rỗng → `Chưa có lịch sử hoạt động`.

- [ ] **Step 1: Implement route + sidebar** (2 file nhỏ, đổi label/path/title).
- [ ] **Step 2: Implement `ActivityHistoryPage.vue`** (load, filter, table, pagination, empty state; style mượn từ `ShiftSchedulePage`).
- [ ] **Step 3: Build** — `npm.cmd run build` → PASS.
- [ ] **Step 4: Commit** — `git commit -m "feat(fe): man lich su hoat dong thay lich ca lam"`

---

### Task 12: Final verification

**Files:** không sửa (trừ fix phát sinh nếu có).

- [ ] **Step 1: Test BE toàn bộ** — `.\mvnw.cmd test` (workdir `BE/`) → đúng **370+ tests**, chỉ 1 failure pre-existing `SecurityIntegrationTest.nhanVienAdmin_...`; nếu có failure mới → sửa tới khi chỉ còn pre-existing.
- [ ] **Step 2: Build FE** — `npm.cmd run build` (workdir `FE/sd03_fe`) → PASS.
- [ ] **Step 3: Restart BE** — tắt process 8080, chạy `.\mvnw.cmd spring-boot:run` (background, workdir `BE/`), chờ `Started DatnApplication`.
- [ ] **Step 4: Smoke curl** (`curl.exe`):
  - `GET /api/thong-ke/so-sanh?cheDo=thang&nam=2026` → 200, JSON có `ky` length 12.
  - `GET /api/thong-ke/so-sanh?cheDo=2ky&tu1=2026-10-01&den1=2026-10-27&tu2=2026-09-01&den2=2026-09-30` → 200, 2 kỳ.
  - `GET /api/thong-ke/so-sanh?cheDo=abc` → 400 có `message`.
  - `GET /api/thong-ke/email-mac-dinh` → 200.
  - `POST /api/thong-ke/gui-mail-tong-ket` body thiếu `loai` → 400; body hợp lệ → 200 với `thanhCong` **theo kết quả SMTP thật** (false cũng OK — đó là kết quả thật, báo cáo nguyên văn trong summary).
  - `GET /api/ca/lich-su?page=0&size=10&keyword=abc` → 200.
- [ ] **Step 5: Duyệt FE bằng trình duyệt** — mở `/thong-ke` (header, dialog gửi mail, mode so sánh: 4 tab, KPI, biểu đồ, 2 bảng), mở `/lich-su-ca` (sidebar "Lịch sử hoạt động", breadcrumb, filter, bảng, phân trang, bấm icon → `/ca/{id}`), **console không có lỗi mới**; kiểm tra nút/bộ lọc/biểu đồ chạy.
- [ ] **Step 6: Trạng thái git** — `git status` sạch sau khi commit mọi phát sinh; `git log --oneline` liệt kê các commit mới.
- [ ] **Step 7: Báo cáo** — tóm tắt những gì đã làm, kết quả test/build/smoke **nguyên văn**, phần SMTP/email nếu gửi thất bại, và **hỏi trước khi push**.
