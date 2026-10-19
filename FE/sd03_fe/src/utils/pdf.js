const trichPrintRules = (doc) => {
  const rules = []
  for (const sheet of Array.from(doc.styleSheets || [])) {
    try {
      const css = Array.from(sheet.cssRules || [])
      // Chỉ nhúng @media print của style khối hóa đơn; không trộn rule in
      // của thư viện bên thứ 3 (vd sweetalert2 in ẩn body > [aria-hidden=true]
      // khi modal đang mở → toàn bộ #app biến mất trong clone → ảnh chụp rỗng).
      const laStyleHoaDon = css.some((r) => r.cssText && r.cssText.includes('invoice-print-sheet'))
      if (!laStyleHoaDon) continue
      for (const rule of css) {
        if (rule.media && /print/.test(rule.media.mediaText || '')) {
          for (const inner of Array.from(rule.cssRules || [])) {
            rules.push(inner.cssText)
          }
        }
      }
    } catch {
      // stylesheet không đọc được (cross-origin) → bỏ qua
    }
  }
  return rules
}

export const taoVaTaiPdf = async (element, tenFile) => {
  if (!element) {
    throw new Error('Không tìm thấy vùng hóa đơn để tạo PDF')
  }
  const [{ jsPDF }, { default: html2canvas }] = await Promise.all([
    import('jspdf'),
    import('html2canvas'),
  ])
  let cloneHeight = 0
  const canvas = await html2canvas(element, {
    scale: 2,
    backgroundColor: '#ffffff',
    useCORS: true,
    logging: false,
    windowWidth: 1200,
    onclone: (doc, cloned) => {
      const rules = trichPrintRules(doc)
      if (rules.length) {
        const style = doc.createElement('style')
        style.textContent = rules.join('\n')
        doc.head.appendChild(style)
      }
      cloned.style.zoom = '1'
      cloneHeight = cloned.getBoundingClientRect().height
    },
  })
  if (!cloneHeight || !canvas.width || !canvas.height) {
    throw new Error('Không chụp được nội dung hóa đơn')
  }
  const imageData = canvas.toDataURL('image/jpeg', 0.95)
  const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' })
  const pageWidth = 210
  const pageHeight = 297
  const marginTop = 8
  const contentHeight = pageHeight - marginTop * 2
  const imageWidth = 180
  const imageHeight = (canvas.height * imageWidth) / canvas.width
  const leftX = (pageWidth - imageWidth) / 2
  pdf.addImage(imageData, 'JPEG', leftX, marginTop, imageWidth, imageHeight)
  const totalPages = Math.max(1, Math.ceil(imageHeight / contentHeight))
  for (let p = 1; p < totalPages; p++) {
    pdf.addPage()
    pdf.addImage(imageData, 'JPEG', leftX, marginTop - p * contentHeight, imageWidth, imageHeight)
  }
  const blob = pdf.output('blob')
  const url = URL.createObjectURL(blob)
  try {
    const a = document.createElement('a')
    a.href = url
    a.download = tenFile
    document.body.appendChild(a)
    a.click()
    a.remove()
  } finally {
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
}
