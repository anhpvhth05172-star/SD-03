/**
 * Kiểm tra tính tương thích Xuất → Nhập Excel giữa FE và BE.
 *
 * - So khớp hàng tiêu đề FE xuất (EmployeeListPage.vue, ProductVariantPage.vue)
 *   với hằng số tiêu đề BE (NhanVienImportService, SanPhamServiceImpl).
 * - Mô phỏng đúng luật kiểm tra tiêu đề của BE (ExcelTieuDeUtil.kiemTraTieuDe):
 *   bỏ dấu, viết thường, so khớp theo thứ tự từng cột.
 * - Tạo file .xlsx thật bằng buildWorkbookBuffer của FE, đọc lại, kiểm tra
 *   hàng tiêu đề trong file đã tạo (round-trip) và ghi file mẫu ra thư mục tạm.
 *
 * Chạy: npm run verify:excel   (thoát mã != 0 nếu có lỗi)
 */
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import os from 'node:os'

import { buildWorkbookBuffer } from '../src/utils/export.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const FE = path.join(__dirname, '..')
const BE = path.resolve(__dirname, '..', '..', '..', 'BE', 'src', 'main', 'java', 'com', 'example', 'datn')

const read = (p) => readFileSync(p, 'utf8')

/** Bỏ dấu tiếng Việt, viết thường, gộp khoảng trắng — khớp với ExcelTieuDeUtil.chuanHoa. */
const chuanHoa = (s) =>
  String(s ?? '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .toLowerCase()
    .replace(/đ/g, 'd')
    .replace(/\s+/g, ' ')
    .trim()

/** Trích mảng chuỗi khỏi khối Java: name = { "a", "b" }; */
const javaArr = (src, name) => {
  const m = src.match(new RegExp(`\\b${name}\\s*=\\s*\\{([^}]*)\\}`))
  if (!m) throw new Error(`Không tìm thấy mảng ${name} trong Java`)
  return [...m[1].matchAll(/"([^"]*)"/g)].map((x) => x[1])
}

/** Trích mảng chuỗi khỏi Vue: const name = [ 'a', 'b', ]; */
const vueArr = (src, name) => {
  const m = src.match(new RegExp(`const\\s+${name}\\s*=\\s*\\[([^\\]]*)\\]`))
  if (!m) throw new Error(`Không tìm thấy mảng ${name} trong Vue`)
  return [...m[1].matchAll(/'([^']*)'/g)].map((x) => x[1])
}

/** Mô phỏng ExcelTieuDeUtil.kiemTraTieuDe — trả về thông báo lỗi hoặc null nếu hợp lệ. */
const kiemTraTieuDe = (headers, kyMon, mau) => {
  if (!headers || headers.length === 0) return 'thiếu hàng tiêu đề'
  for (let i = 0; i < kyMon.length; i++) {
    const text = String(headers[i] ?? '').trim()
    const c = chuanHoa(text)
    if (!c) return `thiếu cột bắt buộc thứ ${i + 1} (mong đợi "${mau[i]}")`
    if (!c.startsWith(kyMon[i])) {
      return `cột thứ ${i + 1} là "${text}" nhưng mong đợi "${mau[i]}"`
    }
  }
  return null
}

let thatBai = 0
const bai = (ok, ten, chiTiet = '') => {
  console.log(`${ok ? '[PASS]' : '[FAIL]'} ${ten}${chiTiet ? ` — ${chiTiet}` : ''}`)
  if (!ok) thatBai++
}

// ---------- Đọc nguồn ----------
const nvVue = read(path.join(FE, 'src', 'pages', 'Employee', 'EmployeeListPage.vue'))
const btVue = read(path.join(FE, 'src', 'pages', 'Product', 'ProductVariantPage.vue'))
const nvJava = read(path.join(BE, 'service', 'NhanVienImportService.java'))
const spJava = read(path.join(BE, 'service', 'impl', 'SanPhamServiceImpl.java'))

const tieuDeNvFe = vueArr(nvVue, 'TIEU_DE_IMPORT_NHAN_VIEN')
const tieuDeNvBe = javaArr(nvJava, 'TIEU_DE')
const kyMonNvBe = javaArr(nvJava, 'KY_MON_TIEU_DE')

// Mảng tiêu đề inline trong lời gọi exportExcel của trang biến thể
const viTriFile = btVue.indexOf('Danh_sach_bien_the')
if (viTriFile < 0) throw new Error('Không tìm thấy exportExcel biến thể trong ProductVariantPage.vue')
const btStart = btVue.indexOf('[', viTriFile)
const btEnd = btVue.indexOf(']', btStart)
const tieuDeBtFe = [...btVue.slice(btStart, btEnd + 1).matchAll(/'([^']*)'/g)].map((x) => x[1])

const tieuDeSpBe = javaArr(spJava, 'TIEU_DE_MAU_SANPHAM')
const kyMonSpBe = javaArr(spJava, 'KY_MON_TIEU_DE_SANPHAM')

// ---------- 1. Nhân viên: FE === BE (so khớp chính xác từng chuỗi) ----------
const trungNv =
  tieuDeNvFe.length === tieuDeNvBe.length &&
  tieuDeNvFe.every((h, i) => h === tieuDeNvBe[i])
bai(trungNv, 'NV: tiêu đề FE xuất === mẫu BE import',
  trungNv ? `${tieuDeNvFe.length} cột` : `FE[${tieuDeNvFe.length}] vs BE[${tieuDeNvBe.length}]`)

const loiNv = kiemTraTieuDe(tieuDeNvFe, kyMonNvBe, tieuDeNvBe)
bai(!loiNv, 'NV: tiêu đề FE vượt qua luật kiểm tra của BE', loiNv ?? '')

// ---------- 2. Biến thể: 14 cột đầu vượt qua luật BE ----------
const loiBt = kiemTraTieuDe(tieuDeBtFe, kyMonSpBe, tieuDeSpBe)
bai(!loiBt, 'Biến thể: 14 cột đầu vượt qua luật kiểm tra của BE', loiBt ?? '')

// ---------- 3. Hằng BE tự nhất quán (KY_MON khớp MAU) ----------
const loiKyMonNv = kiemTraTieuDe(tieuDeNvBe, kyMonNvBe, tieuDeNvBe)
bai(!loiKyMonNv, 'BE: KY_MON_TIEU_DE khớp TIEU_DE (nhân viên)', loiKyMonNv ?? '')
const loiKyMonSp = kiemTraTieuDe(tieuDeSpBe, kyMonSpBe, tieuDeSpBe)
bai(!loiKyMonSp, 'BE: KY_MON_TIEU_DE_SANPHAM khớp TIEU_DE_MAU_SANPHAM', loiKyMonSp ?? '')

// ---------- 4. Round-trip: tạo file thật bằng buildWorkbookBuffer, đọc lại, kiểm ----------
const dongNvMau = ['', 'Nguyễn Văn A', 'van.a@example.com', '0901234567', 'Nam',
  '15/03/1990', 'STAFF', 'Hà Nội', 'Cầu Giấy', 'Số 1']
const dongBtMau = [1, 'Giày Test', 'Nike', 'Sneaker', 'Da thật', 'Sport',
  'Việt Nam', 'Nam', 'Đen', '42', 5, 1500000, 'Êm khí', 'Mô tả test',
  'SP001', 'CTSP001', 'Kinh doanh']

const ExcelJS = (await import('exceljs')).default
const docTieuDe = async (buffer) => {
  const wb = new ExcelJS.Workbook()
  await wb.xlsx.load(buffer)
  return wb.worksheets[0].getRow(1).values.slice(1)
}

const bufNv = await buildWorkbookBuffer(tieuDeNvFe, [dongNvMau], 'Danh sách nhân viên')
const docLaiNv = await docTieuDe(bufNv)
const loiDocNv = kiemTraTieuDe(docLaiNv, kyMonNvBe, tieuDeNvBe)
bai(!loiDocNv, 'NV: file FE tạo ra đọc lại vượt qua luật BE', loiDocNv ?? '')

const bufBt = await buildWorkbookBuffer(tieuDeBtFe, [dongBtMau], 'Danh sách biến thể')
const docLaiBt = await docTieuDe(bufBt)
const loiDocBt = kiemTraTieuDe(docLaiBt, kyMonSpBe, tieuDeSpBe)
bai(!loiDocBt, 'Biến thể: file FE tạo ra đọc lại vượt qua luật BE', loiDocBt ?? '')

// ---------- Ghi file mẫu ra thư mục tạm (test thủ công được ngay) ----------
const thuMuc = path.join(os.tmpdir(), 'sd03-verify-excel')
mkdirSync(thuMuc, { recursive: true })
const fileNv = path.join(thuMuc, 'nhan-vien-tu-fe.xlsx')
const fileBt = path.join(thuMuc, 'bien-the-tu-fe.xlsx')
writeFileSync(fileNv, Buffer.from(bufNv))
writeFileSync(fileBt, Buffer.from(bufBt))
console.log(`\nFile mẫu (có thể tải lên form import để thử):`)
console.log(`  ${fileNv}`)
console.log(`  ${fileBt}`)

if (thatBai > 0) {
  console.error(`\n${thatBai} kiểm tra THẤT BẠI`)
  process.exit(1)
}
console.log('\nTất cả kiểm tra đều PASS')
