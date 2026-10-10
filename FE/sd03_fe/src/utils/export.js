const escapeCell = (v) => `"${String(v === null || v === undefined ? '' : v).replace(/"/g, '""')}"`

/**
 * Xuất dữ liệu ra file CSV mở được bằng Excel (có BOM + dấu phân cách ';').
 */
export const exportCsv = (filename, headers, rows) => {
  const lines = [headers.map(escapeCell).join(';')]
  for (const row of rows) {
    lines.push(row.map(escapeCell).join(';'))
  }
  const blob = new Blob(['\ufeff' + lines.join('\r\n')], {
    type: 'text/csv;charset=utf-8;',
  })
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
