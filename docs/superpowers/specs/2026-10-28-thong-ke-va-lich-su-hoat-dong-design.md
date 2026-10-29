# Design: Bổ sung màn Thống kê (Gửi email tổng kết + So sánh doanh thu) và đổi màn "Lịch ca làm" → "Lịch sử hoạt động"

Ngày: 2026-10-28 · Nhánh: `ha-anh` · Đối chiếu: 9 ảnh tham khảo do người dùng cung cấp.

## 1. Intent / Vì sao

Màn Thống kê hiện có chưa có (a) chức năng gửi email tổng kết doanh thu, (b) khu so sánh & tăng trưởng doanh thu; mục "Lịch ca làm" ở sidebar cần thay bằng màn "Lịch sử hoạt động" (tra cứu lịch sử mở/đóng ca) theo ảnh tham khảo. Làm đúng phần còn thiếu, giữ nguyên phần đang chạy tốt.

## 2. Goals / Non-goals

**Goals**
- G1: Nút "Gửi mail tổng kết" (header + thanh bộ lọc) + hộp thoại gửi mail theo ảnh; email gửi **thật** bằng số liệu **thật** theo kỳ, hiển thị kết quả thành công/thất bại theo phản hồi BE.
- G2: Job `@Scheduled` gửi **thật** báo cáo 23:59 ngày cuối tháng và 31/12 (để box trạng thái trong hộp thoại nói đúng sự thật).
- G3: Khu "So sánh & Tăng trưởng doanh thu" với 4 chế độ đối chiếu, biểu đồ, bảng đối chi tiết 12 kỳ, bảng Top sản phẩm theo kỳ — dữ liệu thật từ BE.
- G4: Màn "Lịch sử hoạt động" thay "Lịch ca làm" (sidebar + route + tiêu đề/breadcrumb), tái sử dụng `GET /api/ca/lich-su`.
- G5: TDD cho mọi code BE mới; FE build sạch, không lỗi console.

**Non-goals / anti-goals**
- Không đổi màu/logo/font/sidebar/header/bảng hiện có; không thiết kế lại UI đang chạy tốt.
- Không đổi công thức tính doanh thu, AOV, tăng trưởng đang dùng (`ThongKeService` + `statisticsData.js`).
- Không tạo dữ liệu giả/mẫu; thiếu API/DB thì báo cáo phần thiếu thay vì giả lập.
- Không sửa API `/api/ca-lam` hay `/api/lich-lam-viec` (màn Lịch làm việc vẫn dùng); chỉ bỏ màn CRUD ca làm khỏi menu.
- Không lan sang chức năng khác; không thêm cột/bộ lọc ngoài những gì có trong ảnh.

## 3. Ràng buộc kỹ thuật (đã khảo sát)

- FE: Vue3 + Vite (5173), proxy `/api` → BE 8080; notify qua SweetAlert2 (`utils/notify.js`); export `utils/export.js#exportExcel`; chart tự dựng `components/charts/BarLineChart.vue` (SVG, hỗ trợ `series[]`); không có thư viện chart ngoài.
- BE: Spring Boot, `ddl-auto=none`, migration chạy tay/SqlMigrationRunner; email qua `EmailService.guiMailCustom(to, tieuDe, noiDung)` (L241) — **body là plain text, bị escape HTML** và bọc template "PolyShoes Shop" sẵn có; SMTP cấu trong `application.properties`.
- `@EnableAsync` đã có, **chưa có `@EnableScheduling`**.
- Không có bảng cấu hình chung; email Admin mặc định = nhân viên có `vai_tro.ma_vai_tro='ADMIN'` và email khác rỗng (fallback `spring.mail.username`).
- Thống kê: `ThongKeService.tongQuan` lọc **bỏ qua** `DA_HUY/DA_HOAN_TIEN/daXoa` trong Java; `tienChuyenKhoan = doanhThu - tienMat`; `aov = round(doanhThu/soDon)`. Công thức này là chuẩn, không đổi.
- Giao ca: `GET /api/ca/lich-su` (page,size,tuNgay,denNgay,trangThai,idNhanVien) trả `Page<CaLamViecDto>`; non-admin bị ép xem ca của mình.

## 3b. Đặc tả thị giác rút từ 9 ảnh tham khảo

Ảnh là của hệ thống tham khảo (FF T-Shirt), chỉ dùng để đối chiếu **thành phần còn thiếu**; màu/logo/font của dự án giữ nguyên.

- **Ảnh 1 — `/thong-ke`**: trang có **page header** (title "Thống kê doanh thu" + phụ đề "Tổng hợp kinh doanh, phân tích sản phẩm và theo dõi tăng trưởng bán hàng"; nút xanh viền `✉ Gửi mail tổng kết`, nút nâu `⇄ So sánh doanh thu` ở góc phải). Thanh bộ lọc có thêm 2 nút `Gửi mail tổng kết`, `So sánh` (bên trái `Xuất Excel`). Phần còn lại (thẻ KPI, biểu đồ, bảng) giữ nguyên hiện trạng. → `StatisticsPage.vue` hiện **chưa có block page header** nên cần thêm đúng block này theo style card/nút hiện có.
- **Ảnh 2 — mode so sánh**: header đổi thành title "So sánh & Tăng trưởng doanh thu" + phụ đề, nút ← quay về; nút `Gửi mail tổng kết`, `Xuất Excel So sánh`. Filter card "Chế độ đối chiếu" với 4 tab `Theo Tháng (12 tháng) / Theo Quý (4 quý) / Theo Năm (Đối chiếu nhiều năm) / Tùy chọn 2 kỳ`; ô `Năm so sánh` (select), `Thương hiệu`, `Tìm sản phẩm`, `Đặt lại`. 6 thẻ KPI: Tổng doanh thu kỳ / Tiền mặt / Chuyển khoản / Tổng đơn hoàn thành / AOV trung bình / Tăng trưởng bình quân. Biểu đồ: **cột** Tiền mặt+Chuyền khoản theo kỳ, **đường đỏ** Tốc độ tăng trưởng (%) với nhãn % trên từng điểm; legend 3 mục.
- **Ảnh 3 — bảng "Đối chiếu doanh thu chi tiết các kỳ"** (đơn vị VND), cột: `KỲ SO SÁNH | KHOẢNG THỜI GIAN | TỔNG DOANH THU | TIỀN MẶT | CHUYỂN KHOẢN | SỐ ĐƠN | AOV | ĐÃ BÁN | CHÊNH LỆCH | TĂNG TRƯỞNG`; badge "Cao nhất" ở kỳ DT cao nhất; hàng `TỔNG CỘNG` với ô "Tăng trưởng TB: 0%".
  - Số trong ảnh xác nhận công thức KPI: Tổng DT = Σ các kỳ; AOV trung bình = ΣDT / ΣSố đơn (102.352.150/22 = 4.652.370); Tăng trưởng bình quân = **trung bình cộng % tăng trưởng của từng kỳ** (100 + (−100) + 0…/12 = 0%).
  - Kỳ đầu tiên: ảnh không có ví dụ kỳ đầu có DT → **quyết định: kỳ đầu so với kỳ trước = 0** (cùng công thức `tinhChuKy`: prev=0 → 100% nếu cur>0, ngược lại 0%), không fetch kỳ ngoài range.
- **Ảnh 4 — bảng "Top sản phẩm bán chạy & Đối chiếu sức mua qua các kỳ"** (badge "Top 10 mặt hàng"): `STT | MÃ SP | TÊN SẢN PHẨM (+ảnh) | THƯƠNG HIỆU | TỔNG BÁN | TỔNG DOANH THU | THÁNG 1..12` (cột theo kỳ, cuộn ngang).
- **Ảnh 5 — hộp thoại "Gửi Mail Tổng Kết Doanh Thu"** + phụ đề "Tổng hợp số liệu kinh doanh & so sánh trực tiếp gửi về hộp thư Quản trị viên"; mục "1. Loại báo cáo tổng kết" (toggle 2 nút), `Chọn Tháng`, `Chọn Năm`; mục "2. Email Admin nhận báo cáo" (input + chú thích "Mặc định lấy địa chỉ email Admin hệ thống..."); box xanh "@Scheduled: Hệ thống tự động gửi báo cáo vào 23:59 ngày cuối mỗi tháng và 31/12 cuối năm về hộp thư Quản trị viên."; nút `Hủy bỏ` (viền) + `Gửi báo cáo ngay` (đặc, xanh).
- **Ảnh 6–7 — email mẫu**: subject "BÁO CÁO TỔNG KẾT & SO SÁNH DOANH THU"; các mục theo thứ tự: dòng kỳ "Tháng 10 Năm 2026 — Hệ thống thời trang …", Tổng doanh thu (+TM/CK), Tăng trưởng so với tháng trước (+% , chênh lệch), 4 ô: Đơn hoàn thành / Đơn đã hủy / AOV / Sản phẩm bán, "Top 5 sản phẩm bán chạy nhất Tháng 10" (STT, Sản phẩm, Đã bán, Doanh thu), "Bảng đối chiếu doanh thu các tháng trong năm" (Thời gian, Doanh thu, Số đơn, AOV, Tăng trưởng, badge "Hiện tại"). → Với `loai=NAM`: bảng so sánh = 12 tháng của năm đó; subject "… — Năm YYYY".
- **Ảnh 8 — sidebar**: group "Lịch Làm Việc" có 2 con `Lịch làm việc` + `Lịch sử hoạt động` (không còn "Lịch ca làm"); breadcrumb "Lịch làm việc / Lịch sử hoạt động" (= `route.meta.title`).
- **Ảnh 9 — `/lich-su-ca`**: filter `TÌM KIẾM` (placeholder "Tìm theo tài khoản / mã ca / mã hóa đơn..."), `TỪ NGÀY`, `ĐẾN NGÀY`, `TRẠNG THÁI` (Tất cả), nút `Tải lại`. Bảng: `# | NGƯỜI GIAO (avatar tròn lấy chữ đầu + tên đăng ký/tên) | NGƯỜI NHẬN ("Chưa có / Cuối ngày" khi chưa có) | CA (chip) | MỞ (giờ+ngày) | ĐÓNG | TIỀN MẶT ("Đầu: … đ / Cuối: … đ") | CHÊNH LỆCH (0 đ) | TRẠNG THÁI (chip "Đã đóng") | HÀNH ĐỘNG (icon tài liệu)`.

## 4. Approaches (đã chọn)

| Phần | Cách đã chọn | Cách bỏ |
|---|---|---|
| A. Email | **(A1)** Endpoint mới `POST /api/thong-ke/gui-mail-tong-ket` tái sử dụng `EmailService.guiMailCustom` + query hiện có; kèm `GET /api/thong-ke/email-mac-dinh`; job `@Scheduled` thật | (A2) Chỉ FE, tái sử dụng `/nhan-vien/gui-email-hang-loat` — không phù hợp (gửi theo danh sách NV, không tính số liệu TK) |
| B. So sánh | **(B1)** 1 endpoint mới `GET /api/thong-ke/so-sanh` trả list kỳ + top sản phẩm; BE gộp từ query range hiện có, bucketing theo kỳ | (B2) FE tự gọi `tong-quan` N lần — N+1 request, thiếu dữ liệu Top sản phẩm theo kỳ |
| C. Lịch sử | **(C1)** Dùng `GET /api/ca/lich-su` sẵn, chỉ thêm param `keyword`; đổi route/sidebar/meta.title; trang mới theo ảnh | (C2) Không có keyword — thiếu ô tìm kiếm trong ảnh |

Lý do chọn: A1/B1/C1 tái sử dụng tối đa service/query/API hiện có, mỗi phần chỉ thêm đúng phần thiếu.

## 5. Thiết kế từng phần

### A. Gửi email tổng kết

**BE**
1. `POST /api/thong-ke/gui-mail-tong-ket` — body `GuiMailTongKetRequest { loai: "THANG"|"NAM", thang: 1..12, nam: ≥2000, emailNhan: string }`.
   - Validate: loai phải đúng, thang/năm hợp lệ với loai, email đúng định dạng (dùng regex như các DTO hiện có), emailNhan bắt buộc (FE đã điền sẵn mặc định).
   - BE tự tính số liệu theo kỳ [tu, den] của kỳ được chọn: dùng lại đúng truy vấn + công thức `tongQuan` (doanh thu, tiền mặt, chuyển khoản, số đơn, AOV, sản phẩm bán), đếm riêng đơn `DA_HUY/DA_HOAN_TIEN`, top 5 sản phẩm, và chuỗi 12 tháng (hoặc 1 năm với loai=NAM) để so sánh — tất cả từ `ThongKeRepository` hiện có.
   - Nội dung email (plain text, hợp lệ với `guiMailCustom`): tiêu đề `BÁO CÁO TỔNG KẾT & SO SÁNH DOANH THU — Tháng N Năm YYYY`, các mục theo ảnh: tổng doanh thu + tiền mặt/CK, tăng trưởng so với kỳ trước (cùng công thức với `statisticsData.tinhChuKy`), đơn hoàn thành/đã hủy/AOV/sản phẩm bán, Top 5 sản phẩm, bảng đối chiếu các tháng trong năm.
   - Trả `{ thanhCong: boolean, emailNhan: string, tieuDe: string, ... }` **theo kết quả thật** của `guiMailCustom`; lỗi SMTP → 502/`thanhdConh=false` kèm thông điệp, không giả lập thành công.
2. `GET /api/thong-ke/email-mac-dinh` → trả email Admin (derived query: nhân viên ADMIN còn hoạt động có email; fallback `spring.mail.username`).
3. `@EnableScheduling` + `BaoCaoTongKetScheduler`: `@Scheduled` **kiểm tra hằng ngày 23:59**, nếu hôm nay là **ngày cuối tháng** → gửi báo cáo tháng; nếu **31/12** → gửi thêm báo cáo năm. Nhận email từ nguồn như trên; mọi lỗi chỉ `log` (không crash app).
4. Quyền: đi kèm `quyenGuard.canQuanLyKhachHang()` như 2 endpoint `thong-ke` hiện tại.

**FE** (`StatisticsPage.vue` + `api/statistics.js`)
- Nút "Gửi mail tổng kết" ở header (bên trái "So sánh doanh thu") và trong thanh bộ lọc — tái sử dụng style nút có sẵn (viền xanh, icon `bi-envelope`).
- Hộp thoại theo ảnh: toggle `Báo cáo tổng kết Tháng | Báo cáo tổng kết Năm`; select `Chọn Tháng` (Tháng 1..12), `Chọn Năm` (2020..năm hiện tại); input `Email Admin nhận báo cáo` (điền sẵn từ `GET /email-mac-dinh`, validate email phía client); box xanh "Trạng thái tự động của hệ thống (@Scheduled): Hệ thống tự động gửi báo cáo vào 23:59 ngày cuối mỗi tháng và 31/12 cuối năm về hộp thư Quản trị viên."; nút `Hủy bỏ` / `Gửi báo cáo ngay`.
- On submit: disable nút + loading, gọi API, `notifySuccess` khi `thanhCong=true`, `notifyError` kèm thông điệp khi thất bại; đóng hộp thoại chỉ khi thành công.
- **Không** thêm lịch hiển thị/`@Scheduled` giả ở FE.

### B. So sánh & Tăng trưởng doanh thu

**BE**
1. `GET /api/thong-ke/so-sanh?cheDo=thang|quy|nam|2ky&nam=2026&tu1=&den1=&tu2=&den2=&thuongHieu=&keyword=` → `SoSanhDTO`:
   - `List<KySoSanhDTO>`: `{ maKy, tenKy, tu, den, doanhThu, tienMat, tienChuyenKhoan, soDon, aov, sanPhamBan, chenhLech, tangTruong, laCaoNhat }` — chenhLech = doanh thu kỳ này − kỳ liền trước **trong dãy hiển thị** (kỳ đầu lấy kỳ trước = 0, không fetch ngoài range; ở chế độ `2ky`: kỳ 2 là kỳ hiện tại, kỳ 1 là kỳ so sánh, tăng trưởng hiển thị theo kỳ 2); tangTruong% theo **cùng công thức** với `statisticsData.tinhChuKy` (prev ≤ 0 → 100 nếu cur > 0, ngược lại 0); kỳ không có dữ liệu trả số 0, không bỏ kỳ.
   - KPI tổng hợp (theo số trong ảnh 2/3): Tổng DT/TT/TM/CK/Số đơn = Σ các kỳ; `aovTrungBinh = round(ΣdoanhThu / ΣsoDon)`; `tangTruongBinhQuan = trung bình cộng tangTruong của các kỳ` (kỳ 0% cũng tính). Footer "Tăng trưởng TB" cùng công thức.
   - `List<TopSanPhamKyDTO>`: top 10 sản phẩm (mã, tên, thương hiệu, tổng bán, tổng DT, DT theo từng kỳ) — cần **query mới** `SanPhamThongKeRow` (ChiTietThongKeRow hiện không chứa tên sản phẩm); kiểm tra lại xem BE đã có query top-sản-pham nào đang dùng không, nếu có thì tái sử dụng.
   - Range: `thang` → 12 tháng của `nam` (hoặc `nam` mặc định = năm hiện tại); `quy` → 4 quý của `nam`; `nam` → tối đa 5 năm gần nhất có dữ liệu (tính từ `MIN(ngayTao)`); `2ky` → dùng `tu1/den1`, `tu2/den2` (bắt buộc đủ 4 ngày, hợp lệ, `tu ≤ den`).
   - `keyword` ("Tìm sản phẩm") — **quyết định của người dùng: chỉ lọc bảng Top sản phẩm** (post-filter theo tên/mã SP), KHÔNG ảnh hưởng KPI/biểu đồ/bảng đối chi tiết các kỳ (giữ nguyên ngữ nghĩa như `thuongHieu`).
   - Chung công thức lọc với `tongQuan` (bỏ `DA_HUY/DA_HOAN_TIEN/daXoa`); lọc thương hiệu theo cách của `tongQuanLocThuongHieu`.
2. Chưa có endpoint nào làm việc này → viết mới trong `ThongKeController` + `ThongKeService`.

**FE** (`StatisticsPage.vue`, chế độ xem riêng trong cùng route `/thong-ke`)
- Nút "So sánh doanh thu" (header) và "So sánh" (thanh bộ lọc) → bật mode so sánh; nút mũi tên ← ở tiêu đề để quay về (đúng ảnh).
- Tiêu đề "So sánh & Tăng trưởng doanh thu" + phụ đề; nút "Gửi mail tổng kết" + "Xuất Excel So sánh" (dùng `exportExcel`).
- Filter card "Chế độ đối chiếu": 4 tab `Theo Tháng (12 tháng) / Theo Quý (4 quý) / Theo Năm (Đối chiếu nhiều năm) / Tùy chọn 2 kỳ`; ô `Năm so sánh`, `Thương hiệu`, `Tìm sản phẩm`, `Đặt lại` (tái sử dụng `SearchableSelect`/input styles hiện có).
- 6 thẻ KPI (tái dùng style thẻ hiện tại): Tổng doanh thu kỳ, Tiền mặt, Chuyển khoản, Tổng đơn hoàn thành, AOV trung bình, Tăng trưởng bình quân.
- Biểu đồ "Doanh thu & Tốc độ tăng trưởng qua các kỳ": `BarLineChart` với series Tiền mặt/Chuyển khoản (cột) + Tốc độ tăng trưởng % (đường, nhãn %) — nếu `BarLineChart` chưa hỗ trợ trục % thì **mở rộng nhỏ** component (giữ prop cũ, thêm prop tùy chọn), không đổi các chart đang dùng.
- Bảng "Đối chiếu doanh thu chi tiết các kỳ": đúng cột ảnh + badge "Cao nhất" + hàng TỔNG CỘNG ("Tăng trưởng TB: x%"); xử lý DT=0 (hiện 0 đ, % = 0/100 theo công thức, không chia cho 0).
- Bảng "Top sản phẩm bán chạy & Đối chiếu sức mua qua các kỳ": cột theo ảnh, cuộn ngang.
- Xử lý `loading | error | rỗng` cho cả 3 khối (text + nút "Đặt lại bộ lọc" như các màn khác).

### C. Màn "Lịch sử hoạt động"

**FE**
- `router/index.js`: route `/lich-ca-lam` → **`/lich-su-ca`**, component mới `pages/Schedule/ActivityHistoryPage.vue`, `meta.title: 'Lịch sử hoạt động'` (breadcrumb tự theo `route.meta.title`); route cũ `/lich-ca-lam` redirect → `/lich-su-ca`.
- `AppSidebar.vue`: child `Lịch ca làm → /lich-ca-lam` đổi thành `Lịch sử hoạt động → /lich-su-ca` (giữ nguyên group "Quản lý lịch làm việc", icon, các mục khác).
- `ShiftSchedulePage.vue` + modal CRUD ca làm **không hiển thị nữa** (giữ file, không xóa — `scheduleData.js`/`api/schedule.js` vẫn được `WorkSchedulePage` dùng).
- Trang mới theo ảnh 9:
  - Filter card: `TÌM KIẾM` (placeholder "Tìm theo tài khoản / mã ca / mã hóa đơn..."), `TỪ NGÀY`, `ĐẾN NGÀY`, `TRẠNG THÁI` (Tất cả / Đang mở / Đã đóng), nút `Tải lại`.
  - Bảng: `# | NGƯỜI GIAO (avatar tròn lấy 2 chữ đầu tên + tên) | NGƯỜI NHẬN | CA | MỞ | ĐÓNG | TIỀN MẶT (Đầu/Cuối) | CHÊNH LỆCH | TRẠNG THÁI | HÀNH ĐỘNG` — tái dùng styles bảng `.data-table` hiện có; avatar là chữ viết tắt từ tên (như ảnh 9), **không** thêm trường ảnh vào DTO.
  - Phân trang server-side (tái dùng `PaginationBar`); icon hành động → `router.push('/ca/{id}/doi-soat')` (trang biên bản/đối soát **đã tồn tại** — không xây trang mới trong phạm vi này).
  - Người nhận: entity lưu `nguoiCapNhat` là **chuỗi ID nhân viên** (ghi lúc kết ca) → service map sang tên bằng lookup `NhanVien` theo danh sách ID trong trang (tránh N+1); ca `DANG_MO` → "Chưa có"; ID không parse được/nhân viên đã xóa → "Không xác định" (không bịa tên).
  - Trạng thái rỗng → text "Chưa có lịch sử hoạt động".

**BE** (thay đổi nhỏ, tái sử dụng)
1. `GET /api/ca/lich-su` thêm param `keyword` (optional): `EXISTS` sub-query matching `maCa` OR `nhanVien.tenTaiKhoan`/`maNhanVien`/`email` OR `hoa_don.maHoaDon` (theo placeholder ảnh); thread qua controller → service → repository (kiểu `LOWER(...) LIKE ...` như `NhanVienRepository.findByFilters`).
2. `CaLamViecDto` thêm trường `tenNguoiNhan` (resolve chuỗi ID `nguoiCapNhat` → tên nhân viên, null-safe; xem §5.C) — thêm ở cuối DTO, không đổi trường cũ.
3. Không tạo bảng/cột mới; không thêm endpoint lịch sử mới.

## 6. Kiểm chứng (Definition of Done)

- TDD: viết test trước cho `so-sanh` (các cheDo, kỳ 0 dữ liệu, lọc thương hiệu, validate range), `gui-mail-tong-ket` (validate, kết quả false khi mailer lỗi — mock `EmailService`), scheduler (hàm xác định "ngày cuối tháng"), `lich-su` + keyword. Chạy `BE\mvnw.cmd test` — không phá test hiện có (chỉ 1 failure pre-existing `SecurityIntegrationTest`).
- FE: `npm.cmd run build` sạch; smoke bằng curl các endpoint mới; mở màn hình kiểm tra với ảnh (nút, hộp thoại, biểu đồ, bảng, phân trang, không lỗi console).
- `git status` sạch sau khi commit; **hỏi người dùng trước khi push**.

## 7. Để lại cho builder (quyết định trong lúc làm)

- Kiểu dữ liệu/ghi chú chính xác cho cron scheduler và timezone (Asia/Ho_Chi_Minh).
- Cách vẽ đường % tăng trưởng trong `BarLineChart` (props mới so với series thứ 3) — chọn cách ít ảnh hưởng chart cũ nhất.
- Thứ tự commit: BE trước (A+B+C), FE sau, hoặc tách theo từng feature.
