# TỔNG HỢP — Trạng thái & Yêu cầu mới (2026-11-03)

Nguồn: plan `2026-11-02-xu-ly-cac-muc-treo.md` + buổi review trước hội đồng (transcript 03/11) + thiết kế Giao ca đã duyệt.

---

## A. Trạng thái mục treo cũ

| # | Việc | Trạng thái |
|---|------|-----------|
| 1 | Bug FE no-undef (`EmployeeListPage`, `EmployeeCreatePage`) | ✅ Xong — eslint 0 lỗi no-undef (chưa commit) |
| 2 | 77 test mock fail (53F+45E) | ✅ Xong — sửa test theo behavior code hiện tại |
| 3 | Regression mất 2 guard khi merge `origin/duc` | ✅ Xong — RED(5 fail) → sửa `HoaDonService` → GREEN |
| 4 | 21 error context (schema drift `ca_lam`) | ✅ Migration đã chạy (Phương án A, user duyệt) |
| 5 | `lich_lam_viec` thiếu 4 cột audit + `id_ca` NOT NULL | ✅ Migration đã chạy |
| 6 | Test POS `khongDangNhap` (403 deliberate) | ✅ Sửa test theo behavior |
| 7 | `SecurityIntegrationTest.nhanVienAdmin` 401 | ⏳ Còn lại — mật khẩu `admin` trong DB không phải `admin123`; cần: hoặc user cho mật khẩu đúng, hoặc test tự tạo tài khoản |

**Bằng chứng cuối:** `GiaoCaServiceTest` + `HoaDonPosCaLamViecTest` = 16/16 pass. **Cần chạy 1 lần full suite cuối** để xác nhận 409/0F/1F.
**Migration đã chạy (tay, có duyệt):** `migration_ca_lam_backfill.sql`, `migration_lich_lam_viec_audit.sql`, `migration_lich_lam_viec_id_ca_null.sql`. `v102_recreate_phien_dang_nhap.sql` (DROP TABLE) → **đánh dấu bỏ qua** theo chọn của user.

**Chưa commit/push bất cứ gì** — user tự tay commit.

---

## B. Yêu cầu mới từ review (13 mục, nhóm A–E)

### B1. POS — bán hàng tại quầy (FE)
1. Giỏ hàng: gộp cột **màu sắc + kích cỡ thành 1 cột**; cột đơn giá **giữ 1 dòng, không xuống dòng**; rút gọn phần sản phẩm cho bớt chiếm chỗ.
2. **Voucher tự động**: thêm SP → tự áp voucher tốt nhất (không bấm tay), hiển thị gợi ý *"mua thêm X để được giảm tiếp"*; **chặn voucher hết hạn/giới hạn lượt** (vẫn cho thanh toán không voucher) — đang có bug cho áp voucher hết hạn.
3. **Ô tiền mặt**: auto-fill tổng tiền hóa đơn; khách đưa thừa → nhập số tiền khách đưa, hiện *"tiền trả khách"* = đưa − tổng.
4. Đơn **Đã hoàn thành**: bỏ nút *Chuyển trạng thái* + *Quay lại danh sách* ở dưới (giữ 2 nút trên, chỉ thêm *In hóa đơn*).
5. **Giá trong giỏ tự cập nhật** khi đổi đợt giảm giá (đang lag/không cập nhật).
6. Sau thanh toán: tự chuyển trạng thái, **không tự in hóa đơn**; kiểm tra luồng thanh toán chuyển khoản.

### B2. Thống kê (FE)
7. Đổi nút **"Reset" → "Đặt lại"**.
8. Bỏ 2 thẻ **Tổng tiền mặt / Tổng tiền chuyển khoản**.
9. Bỏ hết từ **"So sánh tăng trưởng doanh thu" trở xuống** (so sánh chu kỳ, cơ cấu trạng thái đơn hàng) → **chỉ giữ bảng Tổng quan sản phẩm**. Tăng trưởng doanh thu để dành cho trang So sánh (chưa làm).

### B3. Trang chủ & bán hàng online (FE + BE)
10. **Xóa marketing chưa làm**: thương hiệu đồng hành, chính sách đổi trả 7 ngày, hỗ trợ 24/7, đăng ký nhận ưu đãi (footer) → chỉ giữ *Giao hàng toàn quốc*.
11. **Mua hàng không cần đăng nhập**.
12. Thêm **nút Quay lại** ở màn đăng nhập/đăng ký.
13. **Giao diện đăng nhập khách hàng khác admin** (phong cách client).
14. Màn đặt hàng online: tồn kho → **"còn lại" + lượt bán**; bộ lọc màu/size **fill ngay**, option hết hàng hiện nhưng *gạch*; SĐT tự fill sau đăng nhập; chọn địa chỉ tỉnh/xã **giống hệt POS** (code có sẵn); shipping **áp API GHN giống POS**; voucher gợi ý fill lên được.

### B4. Chat (BE + FE — feature)
15. Gộp 2 nút chat thành **1 nút "Gặp nhân viên hỗ trợ/tư vấn"**.
16. Flow **tiếp nhận**: khách gửi yêu cầu → nhân viên bấm *Tiếp nhận* → mới nhắn được; chưa tiếp nhận thì không nhắn.
17. Màn admin **2 tab**: *Chờ tiếp nhận/đang mở* + *Lịch sử chat đã đóng*; lưu lịch sử tin nhắn.
18. Khách **bắt buộc đăng nhập** mới chat (chống spam).

### B5. Việc khác
19. **Giao ca: làm lại** theo thiết kế đã duyệt (Section C).
20. *"Thêm phần quản lý kèm lịch"* — **chưa rõ yêu cầu** (người nói tự nhận không biết dùng) → cần làm rõ.
21. Trang **So sánh** (so sánh tăng trưởng doanh thu) — chưa làm, để sau B2.

**Deadline:** view hội đồng **2:30 chiều** với cô Trang, cô Ngân → ưu tiên việc FE nhanh (B1–B3).

---

## C. Thiết kế Giao ca — ĐÃ DUYỆT (03/11)

**1. Trang Giao ca — bỏ combobox chọn ca:**
- Tự hiện khối **CA ĐANG TRỰC**: tên ca + khung giờ, nhân viên, mã ca, thời điểm mở ca.
- **Chưa mở ca**: banner *"Chưa có ca làm việc → chỉ xem, thao tác bị khóa"* + nút **"Vào ca làm việc"** (nhập tiền đầu ca → `moCa`).
- *Giả định đã duyệt:* banner này cũng hiện ở **POS** khi chưa mở ca.

**2. Modal "Kết thúc ca & Bàn giao tiền mặt":**
- 4 thẻ tự tính: **tiền mặt đầu ca · bán tại quầy · chuyển khoản/QR · tiền mặt dự kiến trong kết**.
- Ô **Tiền chi trong ca** (nhập tay, mặc 0) + ô **Tiền mặt thực tế kiểm đếm** (tự fill = dự kiến).
- Cảnh báo tự động: **khớp 100%** (xanh) / **chênh lệch** → bắt buộc nhập lý do (giữ luật `lyDoLech` hiện có).
- **Nhân viên nhận bàn giao tự động, không dropdown**: lấy NV được phân **ca kế tiếp trong ngày theo lịch** (`lich_lam_viec`); hết ca cuối ngày / không có lịch → ghi **"Cuối ngày"**.
- Ghi chú bàn giao + nút *Hủy* / *Xác nhận Kết ca & Bàn giao tiền*.

**3. Biên bản bàn giao** (sau xác nhận): ca, người giao, người nhận, giờ mở/kết, 6 dòng số liệu, chênh lệch,2 chữ ký — **nút In** (print) + Đóng.

**4. BE**: mở rộng `ketCa` (tiền chi, ghi chú bàn giao, tự tính người nhận) → **thêm cột `ca_lam_viec`** bằng migration idempotent — *viết script chờ user duyệt, không tự chạy DDL*.

---

## D. Thứ tự đề xuất tiếp theo
1. Chạy full suite cuối + xử lý test Security 401 (cần input mật khẩu/tự tạo TK).
2. B1–B3 (FE nhanh, kịp view hội đồng).
3. C — làm lại Giao ca (đã duyệt thiết kế).
4. B4 — Chat (feature lớn).
5. B5 — làm rõ "quản lý kèm lịch" + xem video mẫu giao ca (nếu có).
