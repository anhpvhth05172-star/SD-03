/**
 * Xuất Excel dùng chung cho toàn dự án — tạo file .xlsx thật (Office Open XML),
 * mỗi trường một ô, mỗi bản ghi một hàng, hàng tiêu đề nổi bật, cột tự giãn
 * theo nội dung. Dữ liệu chuỗi (mã NV, số điện thoại, email…) giữ nguyên dạng
 * text nên không mất số 0 ở đầu; số giữ nguyên dạng số.
 */

const isNumber = (v) => typeof v === 'number' && Number.isFinite(v)

const chieuDaiHienThi = (v) => {
  if (v === null || v === undefined) return 0
  if (v instanceof Date) return 10 // dd/MM/yyyy
  return String(v).length
}

const dinhDangNgay = (v) => {
  const d = v instanceof Date ? v : new Date(v)
  if (Number.isNaN(d.getTime())) return String(v)
  const p = (n) => String(n).padStart(2, '0')
  return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()}`
}

const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/**
 * Tạo buffer .xlsx (chạy được cả trên trình duyệt lẫn Node để kiểm thử).
 * @param {string[]} headers  hàng tiêu đề (tên cột)
 * @param {any[][]}   rows    dữ liệu các hàng
 * @param {string}    sheetName tên sheet
 * @returns {Promise<ArrayBuffer>}
 */
export const buildWorkbookBuffer = async (headers, rows, sheetName = 'Danh sách') => {
  const ExcelJS = (await import('exceljs')).default
  const wb = new ExcelJS.Workbook()
  wb.creator = 'Hệ thống quản lý SD-03'
  wb.created = new Date()

  const tenSheet = String(sheetName).replace(/[\\/?*[\]:]/g, ' ').slice(0, 31) || 'Danh sách'
  const ws = wb.addWorksheet(tenSheet, {
    views: [{ state: 'frozen', ySplit: 1 }],
    properties: { defaultRowHeight: 20 },
  })

  // Độ rộng cột = max tiêu đề + dữ liệu, kính cỡ 8..55
  const widths = headers.map((h, i) => {
    let max = chieuDaiHienThi(h)
    for (const row of rows) {
      const len = chieuDaiHienThi(row[i])
      if (len > max) max = len
    }
    return Math.min(Math.max(max + 3, 8), 55)
  })
  ws.columns = headers.map((h, i) => ({ header: String(h ?? ''), key: `c${i}`, width: widths[i] }))

  for (const row of rows) {
    ws.addRow(headers.map((_, i) => {
      const v = row[i]
      return v === null || v === undefined ? '' : v
    }))
  }

  const border = { style: 'thin', color: { argb: 'FFE2E8F0' } }
  const headerBorder = { style: 'thin', color: { argb: 'FFCBD5E1' } }

  const headerRow = ws.getRow(1)
  headerRow.height = 26
  headerRow.eachCell((cell) => {
    cell.font = { name: 'Arial', size: 11, bold: true, color: { argb: 'FFFFFFFF' } }
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF166534' } }
    cell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true }
    cell.border = { top: headerBorder, left: headerBorder, right: headerBorder, bottom: headerBorder }
  })

  const laCotStt = headers.map((h) => String(h).trim().toUpperCase() === 'STT')
  ws.eachRow((row, rowNumber) => {
    if (rowNumber === 1) return
    const banded = rowNumber % 2 === 0
    row.eachCell({ includeEmpty: true }, (cell, colNumber) => {
      cell.font = { name: 'Arial', size: 11, color: { argb: 'FF1E293B' } }
      cell.alignment = { vertical: 'middle' }
      cell.border = { top: border, left: border, right: border, bottom: border }
      if (banded) {
        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF8FAFC' } }
      }
      const v = cell.value
      if (isNumber(v)) {
        cell.numFmt = laCotStt[colNumber - 1] ? '0' : '#,##0.##'
      } else if (v instanceof Date) {
        cell.numFmt = 'dd/MM/yyyy'
      }
    })
  })

  ws.autoFilter = {
    from: { row: 1, column: 1 },
    to: { row: 1, column: headers.length },
  }

  return await wb.xlsx.writeBuffer()
}

/**
 * Xuất dữ liệu ra file Excel .xlsx và tải về.
 * Tên file tự chuẩn hóa về đuôi .xlsx.
 */
export const exportExcel = async (filename, headers, rows, options = {}) => {
  const { sheetName = 'Danh sách' } = options
  const buffer = await buildWorkbookBuffer(headers, rows, sheetName)
  let ten = String(filename).replace(/\.(csv|xls|txt)$/i, '.xlsx')
  if (!/\.xlsx$/i.test(ten)) ten += '.xlsx'
  downloadBlob(
    new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }),
    ten
  )
  return ten
}

/**
 * Tải toàn bộ dữ liệu theo bộ lọc (mỗi trang 50 bản ghi, tối đa maxTrang trang).
 */
export const fetchAllPages = async (fetchPage, params, maxTrang = 20) => {
  const all = []
  let page = 0
  let totalPages = 1
  do {
    const data = await fetchPage({ ...params, page, size: 50 })
    all.push(...(data.content || []))
    totalPages = data.totalPages || 1
    page += 1
    if (page >= maxTrang) break
  } while (page < totalPages)
  return all
}
