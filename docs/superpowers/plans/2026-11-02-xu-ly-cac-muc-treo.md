# Plan: Xử lý các mục treo — 2026-11-02

Nguồn: bảng "Chưa làm / còn treo" ngày 2026-11-01.

## Mục 1 — Bug FE pre-existing (no-undef) ✅ DONE
- [x] `EmployeeListPage.vue`: import `notifyDeleteConfirm`, `deleteEmployee`
- [x] `EmployeeCreatePage.vue`: import `datVaiTro`
- Verify: eslint 0 lỗi no-undef ở 2 file

## Mục 2 — 98 test fail/error BE (53F + 45E)
Phân loại (đã phân định qua worktree origin/duc):
- [ ] 70 fail có sẵn bên duc — hành vi mới của duc (403 "Chỉ nhân viên mới được tạo hóa đơn tại quầy", đổi trangThai/validation, chặn thong-ke) → test cũ chưa cập nhật
- [ ] 6 fail do `phanCongNhanVienDonOnline` (commit 158ad02 của user) — 400 "Không có nhân viên hoạt động để nhận đơn online"
- [ ] 21 error do `SqlMigrationRunner` chạy migration trên DB thật lúc test start
- [ ] 1 error test Mockito (`BaoCaoHangNgayServiceTest`)

Cách làm: chạy test lấy danh sách hiện tại → điều tra từng nhóm nguyên nhân → sửa test theo hành vi đã chốt (không sửa business rule ngoài phạm vi).

## Mục 3 — 13 yêu cầu mới của "cô"
Làm theo nhóm:
- [ ] A. Thống kê & biểu đồ: #2 filter nhiều tháng/quý/năm, #3 biểu đồ cột+đường + search 2 bảng
- [ ] B. Email & chuông: #1 mail báo cáo tự động, #12 chuông admin đơn mới
- [ ] C. Checkout & voucher: #4 địa chỉ+phí ship, #5 voucher gợi ý, #7 xác nhận đặt hàng, #11 trừ tồn kho + voucher
- [ ] D. POS & thanh toán: #6 voucher POS, #9 VietQR/Mono/VnPay, #13 thanh toán online
- [ ] E. Giao ca: #8 tự hiện ca hiện tại, #10 nhân viên nhận giao tự động

## Ràng buộc
- Không push/commit tự ý (user tự push)
- Không SQL ghi/DDL lên DB thật ngoài luồng test đã được phê duyệt
- Không sửa SecurityConfig, không thêm dependency
- Báo cáo minh bạch cuối mỗi mục
