import { formatVnd, formatDateTime } from './format'

/**
 * In Biên lai Đối soát ca (Khổ nhiệt K80 - 80mm) qua iframe ẩn
 */
export const inBienLaiDoiSoatK80 = (caData, tongKetData) => {
  const ca = caData || {}
  const tk = tongKetData || {}

  const maCa = ca.maCa || tk.maCa || 'GC-CHUA-LUU'
  const tenNhanVien = ca.tenNhanVien || tk.tenNhanVien || 'Thu ngân'
  const thoiGianMo = ca.thoiGianMo || tk.thoiGianMo
  const thoiGianKet = ca.thoiGianKet || new Date()
  const tienDauCa = Number(ca.tienDauCa ?? tk.tienDauCa ?? 0)
  const tienMatThu = Number(ca.tienMatThu ?? tk.tienMatThu ?? 0)
  const tienKhongMat = Number(ca.tienKhongMat ?? tk.tienKhongMat ?? 0)
  const tienHoanMat = Number(ca.tienHoanMat ?? tk.tienHoanMat ?? 0)
  const tienDuKien = Number(ca.tienDuKien ?? tk.tienDuKien ?? (tienDauCa + tienMatThu - tienHoanMat))
  const tienThucTe = Number(ca.tienThucTe ?? tk.tienThucTe ?? tienDuKien)
  const chenhLech = Number(ca.chenhLech ?? (tienThucTe - tienDuKien))
  const lyDoLech = ca.lyDoLech || ''
  const tongDoanhThu = Number(ca.tongDoanhThu ?? tk.tongDoanhThu ?? (tienMatThu + tienKhongMat))
  const tongHoaDon = Number(ca.tongHoaDon ?? tk.tongHoaDon ?? 0)
  const hoaDonGanDay = tk.hoaDonGanDay || []

  let statusDiffText = 'Khớp 100% (0 ₫)'
  if (chenhLech > 0) {
    statusDiffText = `Thừa (+${formatVnd(chenhLech)})`
  } else if (chenhLech < 0) {
    statusDiffText = `Thiếu (${formatVnd(chenhLech)})`
  }

  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.right = '0'
  iframe.style.bottom = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = '0'
  iframe.style.opacity = '0'
  iframe.style.pointerEvents = 'none'
  document.body.appendChild(iframe)

  const doc = iframe.contentWindow.document
  doc.open()
  doc.write(`<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>Bien_lai_doi_soat_${maCa}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
    <style>
      @page {
        size: 80mm auto;
        margin: 0;
      }
      * {
        box-sizing: border-box;
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
        color-adjust: exact !important;
      }
      html, body {
        margin: 0;
        padding: 0;
        width: 100%;
        background: #ffffff;
        font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
        color: #0f172a;
        font-size: 11.5px;
        line-height: 1.35;
      }
      .thermal-wrap {
        width: 100%;
        display: flex;
        justify-content: center;
        background: #ffffff;
      }
      .receipt-box {
        width: 76mm !important;
        max-width: 76mm !important;
        min-width: 76mm !important;
        margin: 0 auto !important;
        padding: 10px 6px !important;
        background: #ffffff !important;
      }
      .center {
        text-align: center;
      }
      .brand-title {
        font-size: 17px;
        font-weight: 800;
        letter-spacing: 0.5px;
        color: #0f172a;
        margin: 0 0 2px 0;
      }
      .brand-sub {
        font-size: 10px;
        color: #64748b;
        margin: 1px 0;
      }
      .divider {
        border-top: 1px dashed #94a3b8;
        margin: 8px 0;
      }
      .double-divider {
        border-top: 2px dashed #475569;
        margin: 8px 0;
      }
      .receipt-title {
        font-size: 13.5px;
        font-weight: 800;
        text-transform: uppercase;
        color: #0f172a;
        margin: 4px 0 2px 0;
        letter-spacing: 0.5px;
      }
      .receipt-code {
        font-family: 'JetBrains Mono', monospace;
        font-size: 11px;
        font-weight: 700;
        color: #1e293b;
      }
      .row {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        padding: 2px 0;
        font-size: 11px;
      }
      .row.bold {
        font-weight: 700;
      }
      .row.highlight {
        font-size: 12px;
        font-weight: 800;
        padding: 3px 0;
      }
      .diff-box {
        background: #f8fafc;
        border: 1px solid #cbd5e1;
        border-radius: 4px;
        padding: 5px 6px;
        margin: 6px 0;
      }
      .diff-box.exact {
        background: #f0fdf4;
        border-color: #86efac;
      }
      .diff-box.warn {
        background: #fef2f2;
        border-color: #fca5a5;
      }
      .table-mini {
        width: 100%;
        border-collapse: collapse;
        font-size: 10px;
        margin-top: 4px;
      }
      .table-mini th {
        text-align: left;
        border-bottom: 1px solid #94a3b8;
        padding: 3px 0;
        font-weight: 700;
        color: #475569;
      }
      .table-mini td {
        padding: 3px 0;
        border-bottom: 1px dotted #e2e8f0;
      }
      .text-right {
        text-align: right;
      }
      .signature-area {
        display: flex;
        justify-content: space-between;
        margin-top: 14px;
        padding-top: 4px;
        font-size: 10.5px;
      }
      .sign-col {
        width: 48%;
        text-align: center;
      }
      .sign-title {
        font-weight: 700;
      }
      .sign-note {
        font-size: 9px;
        color: #64748b;
        font-style: italic;
      }
      .sign-space {
        height: 40px;
      }
      .sign-name {
        font-weight: 700;
        font-size: 10.5px;
      }
      .footer-note {
        text-align: center;
        font-size: 9.5px;
        color: #94a3b8;
        margin-top: 10px;
        font-style: italic;
      }
    </style>
  </head>
  <body>
    <div class="thermal-wrap">
      <div class="receipt-box">
        
        <div class="center">
          <h1 class="brand-title">POLYSHOES STORE</h1>
          <p class="brand-sub">Hệ thống giày dép thời trang chính hãng</p>
          <p class="brand-sub">Quầy POS #01 • Hotline: 1900 6868</p>
        </div>

        <div class="divider"></div>

        <div class="center">
          <div class="receipt-title">BIÊN LAI ĐỐI SOÁT & KẾT CA</div>
          <div class="receipt-code">${maCa}</div>
        </div>

        <div class="divider"></div>

        <!-- THÔNG TIN PHIÊN LÀM VIỆC -->
        <div class="row">
          <span>Thu ngân trực:</span>
          <strong>${tenNhanVien}</strong>
        </div>
        <div class="row">
          <span>Mở ca:</span>
          <span>${formatDateTime(thoiGianMo)}</span>
        </div>
        <div class="row">
          <span>Đóng ca:</span>
          <span>${formatDateTime(thoiGianKet)}</span>
        </div>
        <div class="row">
          <span>Trạng thái:</span>
          <strong>${ca.thoiGianKet ? 'ĐÃ KẾT CA' : 'ĐANG MỞ'}</strong>
        </div>

        <div class="double-divider"></div>

        <!-- CHI TIẾT ĐỐI SOÁT KÉT TIỀN MẶT -->
        <div class="row">
          <span>1. Tiền mặt ban đầu:</span>
          <span>${formatVnd(tienDauCa)}</span>
        </div>
        <div class="row">
          <span>2. Tiền mặt bán hàng (+):</span>
          <span>${formatVnd(tienMatThu)}</span>
        </div>
        <div class="row">
          <span>3. Tiền hoàn trả khách (-):</span>
          <span>${formatVnd(tienHoanMat)}</span>
        </div>

        <div class="divider"></div>

        <div class="row bold">
          <span>TIỀN DỰ KIẾN KÉT (1+2-3):</span>
          <span>${formatVnd(tienDuKien)}</span>
        </div>
        <div class="row highlight">
          <span>TIỀN THỰC TẾ KIỂM ĐẾM:</span>
          <span>${formatVnd(tienThucTe)}</span>
        </div>

        <div class="diff-box ${chenhLech === 0 ? 'exact' : 'warn'}">
          <div class="row bold" style="padding: 0;">
            <span>KẾT QUẢ ĐỐI SOÁT:</span>
            <span>${statusDiffText}</span>
          </div>
          ${lyDoLech ? `<div style="font-size: 10px; margin-top: 3px; color: #b91c1c;"><b>Lý do:</b> ${lyDoLech}</div>` : ''}
        </div>

        <div class="double-divider"></div>

        <!-- TỔNG KẾT DOANH SỐ CA -->
        <div class="row">
          <span>Doanh số Tiền mặt:</span>
          <span>${formatVnd(tienMatThu)}</span>
        </div>
        <div class="row">
          <span>Chuyển khoản (VietQR):</span>
          <span>${formatVnd(tienKhongMat)}</span>
        </div>
        <div class="row highlight">
          <span>TỔNG DOANH THU CA:</span>
          <span>${formatVnd(tongDoanhThu)}</span>
        </div>
        <div class="row">
          <span>Tổng số hóa đơn:</span>
          <strong>${tongHoaDon} đơn hoàn tất</strong>
        </div>

        ${hoaDonGanDay.length > 0 ? `
          <div class="divider"></div>
          <div style="font-weight: 700; font-size: 10.5px; margin-bottom: 2px;">DANH SÁCH ĐƠN TRONG CA (${hoaDonGanDay.length}):</div>
          <table class="table-mini">
            <thead>
              <tr>
                <th>MÃ HĐ</th>
                <th>PTTT</th>
                <th class="text-right">TIỀN</th>
              </tr>
            </thead>
            <tbody>
              ${hoaDonGanDay.map(h => `
                <tr>
                  <td><b>${h.maHoaDon}</b></td>
                  <td>${h.tenPhuongThuc?.includes('khoản') ? 'CK' : 'Tiền mặt'}</td>
                  <td class="text-right">${formatVnd(h.tienSauGiamGia)}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        ` : ''}

        <div class="divider"></div>

        <!-- CHỮ KÝ XÁC NHẬN BÀN GIAO -->
        <div class="signature-area">
          <div class="sign-col">
            <div class="sign-title">Người giao ca</div>
            <div class="sign-note">(Thu ngân ký)</div>
            <div class="sign-space"></div>
            <div class="sign-name">${tenNhanVien}</div>
          </div>
          <div class="sign-col">
            <div class="sign-title">Người nhận ca</div>
            <div class="sign-note">(Ký & ghi họ tên)</div>
            <div class="sign-space"></div>
            <div class="sign-name">........................</div>
          </div>
        </div>

        <div class="footer-note">
          Thời gian in: ${formatDateTime(new Date())}<br/>
          Hệ thống Quản lý Bán lẻ PolyShoes POS
        </div>

      </div>
    </div>
  </body>
</html>`)
  doc.close()

  setTimeout(() => {
    try {
      iframe.contentWindow.focus()
      iframe.contentWindow.print()
    } catch (e) {
      console.error('Print error:', e)
    } finally {
      setTimeout(() => {
        if (iframe.parentNode) {
          iframe.parentNode.removeChild(iframe)
        }
      }, 2500)
    }
  }, 350)
}

/**
 * In Biên bản Bàn giao & Đối soát ca làm việc (Khổ A4 chuẩn) qua iframe ẩn
 */
export const inBienBanDoiSoatA4 = (caData, tongKetData) => {
  const ca = caData || {}
  const tk = tongKetData || {}

  const maCa = ca.maCa || tk.maCa || 'GC-CHUA-LUU'
  const tenNhanVien = ca.tenNhanVien || tk.tenNhanVien || 'Thu ngân'
  const thoiGianMo = ca.thoiGianMo || tk.thoiGianMo
  const thoiGianKet = ca.thoiGianKet || new Date()
  const tienDauCa = Number(ca.tienDauCa ?? tk.tienDauCa ?? 0)
  const tienMatThu = Number(ca.tienMatThu ?? tk.tienMatThu ?? 0)
  const tienKhongMat = Number(ca.tienKhongMat ?? tk.tienKhongMat ?? 0)
  const tienHoanMat = Number(ca.tienHoanMat ?? tk.tienHoanMat ?? 0)
  const tienDuKien = Number(ca.tienDuKien ?? tk.tienDuKien ?? (tienDauCa + tienMatThu - tienHoanMat))
  const tienThucTe = Number(ca.tienThucTe ?? tk.tienThucTe ?? tienDuKien)
  const chenhLech = Number(ca.chenhLech ?? (tienThucTe - tienDuKien))
  const lyDoLech = ca.lyDoLech || ''
  const tongDoanhThu = Number(ca.tongDoanhThu ?? tk.tongDoanhThu ?? (tienMatThu + tienKhongMat))
  const tongHoaDon = Number(ca.tongHoaDon ?? tk.tongHoaDon ?? 0)
  const hoaDonGanDay = tk.hoaDonGanDay || []

  let statusDiffText = 'Khớp 100% (Không phát sinh chênh lệch két)'
  if (chenhLech > 0) {
    statusDiffText = `Phát hiện thừa tiền két: +${formatVnd(chenhLech)}`
  } else if (chenhLech < 0) {
    statusDiffText = `Phát hiện thiếu hụt tiền két: ${formatVnd(chenhLech)}`
  }

  const iframe = document.createElement('iframe')
  iframe.style.position = 'fixed'
  iframe.style.right = '0'
  iframe.style.bottom = '0'
  iframe.style.width = '0'
  iframe.style.height = '0'
  iframe.style.border = '0'
  iframe.style.opacity = '0'
  iframe.style.pointerEvents = 'none'
  document.body.appendChild(iframe)

  const doc = iframe.contentWindow.document
  doc.open()
  doc.write(`<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>Bien_ban_doi_soat_${maCa}</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@400;600&display=swap" rel="stylesheet">
    <style>
      @page {
        size: A4 portrait;
        margin: 15mm 18mm;
      }
      * {
        box-sizing: border-box;
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
        color-adjust: exact !important;
      }
      body {
        margin: 0;
        padding: 0;
        background: #ffffff;
        font-family: 'Times New Roman', 'Plus Jakarta Sans', serif;
        color: #111827;
        font-size: 13.5px;
        line-height: 1.5;
      }
      .header-grid {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 20px;
        border-bottom: 2px solid #111827;
        padding-bottom: 12px;
      }
      .brand-box {
        width: 50%;
      }
      .brand-name {
        font-size: 16px;
        font-weight: 800;
        text-transform: uppercase;
        margin: 0;
      }
      .brand-desc {
        font-size: 12px;
        color: #4b5563;
        margin: 2px 0;
      }
      .nation-box {
        width: 50%;
        text-align: center;
      }
      .nation-title {
        font-weight: 700;
        font-size: 13px;
        text-transform: uppercase;
      }
      .nation-motto {
        font-weight: 700;
        font-size: 12px;
      }
      .doc-title {
        text-align: center;
        margin: 24px 0 16px 0;
      }
      .doc-title h1 {
        font-size: 20px;
        font-weight: 800;
        text-transform: uppercase;
        margin: 0 0 6px 0;
      }
      .doc-title p {
        font-size: 13px;
        font-style: italic;
        color: #4b5563;
        margin: 0;
      }
      .info-box {
        background: #f9fafb;
        border: 1px solid #e5e7eb;
        border-radius: 6px;
        padding: 12px 16px;
        margin-bottom: 20px;
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px 24px;
      }
      .info-row {
        display: flex;
        gap: 8px;
      }
      .info-k {
        font-weight: 700;
        color: #374151;
        min-width: 140px;
      }
      .info-v {
        color: #111827;
      }
      .section-heading {
        font-size: 14.5px;
        font-weight: 700;
        text-transform: uppercase;
        margin: 20px 0 8px 0;
        color: #1e3a8a;
      }
      table.data-table {
        width: 100%;
        border-collapse: collapse;
        margin-bottom: 16px;
      }
      table.data-table th, table.data-table td {
        border: 1px solid #d1d5db;
        padding: 8px 12px;
        font-size: 13px;
      }
      table.data-table th {
        background: #f3f4f6;
        font-weight: 700;
        text-align: left;
      }
      table.data-table tr.total-row {
        background: #eff6ff;
        font-weight: 800;
      }
      .text-right {
        text-align: right;
      }
      .text-center {
        text-align: center;
      }
      .discrepancy-callout {
        padding: 12px 16px;
        border-radius: 6px;
        margin: 14px 0 24px 0;
      }
      .discrepancy-callout.exact {
        background: #f0fdf4;
        border: 1.5px solid #22c55e;
        color: #15803d;
      }
      .discrepancy-callout.warn {
        background: #fef2f2;
        border: 1.5px solid #ef4444;
        color: #b91c1c;
      }
      .signature-grid {
        display: flex;
        justify-content: space-between;
        margin-top: 36px;
        page-break-inside: avoid;
      }
      .signature-col {
        width: 45%;
        text-align: center;
      }
      .sign-title-a4 {
        font-weight: 700;
        font-size: 14px;
        text-transform: uppercase;
      }
      .sign-hint-a4 {
        font-size: 12px;
        font-style: italic;
        color: #6b7280;
      }
      .sign-space-a4 {
        height: 80px;
      }
      .sign-name-a4 {
        font-weight: 700;
        font-size: 14px;
      }
    </style>
  </head>
  <body>
    <div class="header-grid">
      <div class="brand-box">
        <h2 class="brand-name">POLYSHOES STORE</h2>
        <div class="brand-desc">Hệ thống Giày dép Thời trang & Thể thao</div>
        <div class="brand-desc">Quầy thu ngân POS • Hotline: 1900 6868</div>
      </div>
      <div class="nation-box">
        <div class="nation-title">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</div>
        <div class="nation-motto">Độc lập - Tự do - Hạnh phúc</div>
        <div style="font-size: 11px; margin-top: 4px;">----------o0o----------</div>
      </div>
    </div>

    <div class="doc-title">
      <h1>BIÊN BẢN BÀN GIAO & ĐỐI SOÁT CA LÀM VIỆC</h1>
      <p>Số: ${maCa} • Thời gian lập biên bản: ${formatDateTime(new Date())}</p>
    </div>

    <div class="info-box">
      <div class="info-row">
        <span class="info-k">Mã ca làm việc:</span>
        <strong class="info-v">${maCa}</strong>
      </div>
      <div class="info-row">
        <span class="info-k">Thu ngân bàn giao:</span>
        <strong class="info-v">${tenNhanVien}</strong>
      </div>
      <div class="info-row">
        <span class="info-k">Thời gian mở ca:</span>
        <span class="info-v">${formatDateTime(thoiGianMo)}</span>
      </div>
      <div class="info-row">
        <span class="info-k">Thời gian chốt ca:</span>
        <span class="info-v">${formatDateTime(thoiGianKet)}</span>
      </div>
      <div class="info-row">
        <span class="info-k">Điểm bán hàng:</span>
        <span class="info-v">Quầy POS #01 (Cửa hàng chính)</span>
      </div>
      <div class="info-row">
        <span class="info-k">Trạng thái ca:</span>
        <span class="info-v"><strong>${ca.thoiGianKet ? 'ĐÃ KẾT CA' : 'ĐANG MỞ'}</strong></span>
      </div>
    </div>

    <div class="section-heading">I. BẢNG KÊ ĐỐI SOÁT QUỸ TIỀN MẶT TRONG KÉT</div>
    <table class="data-table">
      <thead>
        <tr>
          <th width="50" class="text-center">STT</th>
          <th>HẠNG MỤC DÒNG TIỀN MẶT</th>
          <th width="180" class="text-right">SỐ TIỀN (VNĐ)</th>
          <th width="240">GHI CHÚ / GIẢI TRÌNH</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td class="text-center">1</td>
          <td><b>Tiền mặt ban đầu trong két</b> (Quỹ thối đầu ca)</td>
          <td class="text-right font-bold">${formatVnd(tienDauCa)}</td>
          <td>Khai báo lúc mở ca làm việc</td>
        </tr>
        <tr>
          <td class="text-center">2</td>
          <td><b>Doanh thu tiền mặt bán hàng tại quầy</b> (+)</td>
          <td class="text-right font-bold">${formatVnd(tienMatThu)}</td>
          <td>Tiền mặt thu trực tiếp từ khách mua hàng</td>
        </tr>
        <tr>
          <td class="text-center">3</td>
          <td><b>Tiền hoàn trả khách hàng</b> (-)</td>
          <td class="text-right font-bold">${formatVnd(tienHoanMat)}</td>
          <td>Hoàn tiền mặt hủy/trả hàng</td>
        </tr>
        <tr class="total-row">
          <td class="text-center">4</td>
          <td><b>TIỀN DỰ KIẾN TRONG KÉT (1 + 2 - 3)</b></td>
          <td class="text-right" style="color: #1e3a8a;">${formatVnd(tienDuKien)}</td>
          <td>Hệ thống phần mềm tự động tính</td>
        </tr>
        <tr class="total-row" style="background: #fdf2f8;">
          <td class="text-center">5</td>
          <td><b>TIỀN MẶT THỰC TẾ KIỂM ĐẾM</b></td>
          <td class="text-right" style="color: #9d174d;">${formatVnd(tienThucTe)}</td>
          <td>Thu ngân kiểm đếm thực tế khi chốt ca</td>
        </tr>
      </tbody>
    </table>

    <div class="discrepancy-callout ${chenhLech === 0 ? 'exact' : 'warn'}">
      <div><b>KẾT LUẬN ĐỐI SOÁT TIỀN KÉT:</b> ${statusDiffText}</div>
      ${lyDoLech ? `<div style="margin-top: 4px;"><b>Lý do giải trình chênh lệch:</b> ${lyDoLech}</div>` : ''}
    </div>

    <div class="section-heading">II. TỔNG HỢP DOANH SỐ THEO PHƯƠNG THỨC THANH TOÁN</div>
    <table class="data-table">
      <thead>
        <tr>
          <th width="50" class="text-center">STT</th>
          <th>PHƯƠNG THỨC THANH TOÁN</th>
          <th width="180" class="text-right">SỐ TIỀN (VNĐ)</th>
          <th width="240">GHI CHÚ</th>
        </tr>
      </thead>
      <tbody>
        <tr>
          <td class="text-center">1</td>
          <td>Tiền mặt tại quầy</td>
          <td class="text-right font-bold">${formatVnd(tienMatThu)}</td>
          <td>Có trong két tiền</td>
        </tr>
        <tr>
          <td class="text-center">2</td>
          <td>Chuyển khoản Ngân hàng / VietQR</td>
          <td class="text-right font-bold">${formatVnd(tienKhongMat)}</td>
          <td>Đã ghi nhận tài khoản ngân hàng</td>
        </tr>
        <tr class="total-row">
          <td class="text-center">Σ</td>
          <td><b>TỔNG DOANH SỐ TOÀN CA</b></td>
          <td class="text-right" style="color: #15803d;">${formatVnd(tongDoanhThu)}</td>
          <td>Tổng cộng ${tongHoaDon} hóa đơn thành công</td>
        </tr>
      </tbody>
    </table>

    ${hoaDonGanDay.length > 0 ? `
      <div class="section-heading">III. DANH SÁCH CÁC HÓA ĐƠN HOÀN TẤT TRONG CA (${hoaDonGanDay.length})</div>
      <table class="data-table">
        <thead>
          <tr>
            <th width="50" class="text-center">STT</th>
            <th>MÃ HÓA ĐƠN</th>
            <th>THỜI GIAN</th>
            <th>PHƯƠNG THỨC</th>
            <th class="text-right">TỔNG TIỀN</th>
            <th class="text-center">TRẠNG THÁI</th>
          </tr>
        </thead>
        <tbody>
          ${hoaDonGanDay.map((h, i) => `
            <tr>
              <td class="text-center">${i + 1}</td>
              <td><b>${h.maHoaDon}</b></td>
              <td>${formatDateTime(h.ngayTao)}</td>
              <td>${h.tenPhuongThuc || 'Tiền mặt'}</td>
              <td class="text-right font-bold">${formatVnd(h.tienSauGiamGia)}</td>
              <td class="text-center">${h.trangThai}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    ` : ''}

    <div class="signature-grid">
      <div class="signature-col">
        <div class="sign-title-a4">NGƯỜI GIAO CA</div>
        <div class="sign-hint-a4">(Thu ngân trực ca ký & ghi họ tên)</div>
        <div class="sign-space-a4"></div>
        <div class="sign-name-a4">${tenNhanVien}</div>
      </div>
      <div class="signature-col">
        <div class="sign-title-a4">NGƯỜI NHẬN CA / QUẢN LÝ</div>
        <div class="sign-hint-a4">(Ký, đóng dấu hoặc ghi rõ họ tên)</div>
        <div class="sign-space-a4"></div>
        <div class="sign-name-a4">....................................................</div>
      </div>
    </div>
  </body>
</html>`)
  doc.close()

  setTimeout(() => {
    try {
      iframe.contentWindow.focus()
      iframe.contentWindow.print()
    } catch (e) {
      console.error('Print error:', e)
    } finally {
      setTimeout(() => {
        if (iframe.parentNode) {
          iframe.parentNode.removeChild(iframe)
        }
      }, 2500)
    }
  }, 350)
}
