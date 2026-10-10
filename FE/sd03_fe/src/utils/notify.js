import Swal from 'sweetalert2'
import '@/assets/styles/custom-swal.css'

/**
 * Format string quotes into clean strong tags
 */
const formatHighlight = (text) => {
  if (!text) return ''
  return String(text).replace(
    /["“]([^"”]+)["”]/g,
    '<strong>"$1"</strong>'
  )
}

/**
 * SVG Minimal Icons (Xanh lá nhạt thanh lịch)
 */
const MINIMAL_ICONS = {
  success: `
    <div class="minimal-icon-box is-success">
      <svg viewBox="0 0 24 24">
        <polyline points="20 6 9 17 4 12"></polyline>
      </svg>
    </div>
  `,
  error: `
    <div class="minimal-icon-box is-error">
      <svg viewBox="0 0 24 24">
        <line x1="18" y1="6" x2="6" y2="18"></line>
        <line x1="6" y1="6" x2="18" y2="18"></line>
      </svg>
    </div>
  `,
  warning: `
    <div class="minimal-icon-box is-warning">
      <svg viewBox="0 0 24 24">
        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
        <line x1="12" y1="9" x2="12" y2="13"></line>
        <line x1="12" y1="17" x2="12.01" y2="17"></line>
      </svg>
    </div>
  `,
  info: `
    <div class="minimal-icon-box is-info">
      <svg viewBox="0 0 24 24">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="16" x2="12" y2="12"></line>
        <line x1="12" y1="8" x2="12.01" y2="8"></line>
      </svg>
    </div>
  `
}

/**
 * Hiển thị thông báo thành công (Icon xanh lá nhạt, tự động tắt sau 1.5s)
 * @param {string} title Nội dung thông báo
 * @param {string} [text] Nội dung phụ
 * @param {number} [timer=1500] Thời gian tự tắt (ms)
 */
export const notifySuccess = (title, text = '', timer = 1500) => {
  const contentHtml = `
    ${MINIMAL_ICONS.success}
    <div class="minimal-title">${formatHighlight(title || 'Thành công')}</div>
    ${text ? `<div class="minimal-desc">${formatHighlight(text)}</div>` : ''}
  `

  return Swal.fire({
    html: contentHtml,
    showConfirmButton: false,
    timer: timer,
    timerProgressBar: false,
    allowOutsideClick: true,
    allowEscapeKey: true,
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container'
    },
    didOpen: () => {
      // Đảm bảo đóng popup tự động sau đúng thời gian timer (1.5s)
      setTimeout(() => {
        if (Swal.isVisible()) {
          Swal.close()
        }
      }, timer)
    }
  })
}

/**
 * Hiển thị thông báo lỗi
 */
export const notifyError = (title, text = '') => {
  const contentHtml = `
    ${MINIMAL_ICONS.error}
    <div class="minimal-title">${title || 'Đã có lỗi xảy ra'}</div>
    ${text ? `<div class="minimal-desc">${formatHighlight(text)}</div>` : ''}
  `

  return Swal.fire({
    html: contentHtml,
    showConfirmButton: true,
    confirmButtonText: 'Đóng',
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      actions: 'custom-swal-actions',
      confirmButton: 'custom-swal-confirm-btn'
    },
    buttonsStyling: false
  })
}

/**
 * Hiển thị thông báo cảnh báo
 */
export const notifyWarning = (title, text = '') => {
  const contentHtml = `
    ${MINIMAL_ICONS.warning}
    <div class="minimal-title">${title || 'Cảnh báo'}</div>
    ${text ? `<div class="minimal-desc">${formatHighlight(text)}</div>` : ''}
  `

  return Swal.fire({
    html: contentHtml,
    showConfirmButton: true,
    confirmButtonText: 'Đã hiểu',
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      actions: 'custom-swal-actions',
      confirmButton: 'custom-swal-confirm-btn'
    },
    buttonsStyling: false
  })
}

/**
 * Hộp thoại xác nhận (Confirm modal)
 */
export const notifyConfirm = async (
  title = 'Bạn có chắc chắn?',
  text = 'Thao tác này sẽ không thể hoàn tác!',
  confirmText = 'Đồng ý',
  cancelText = 'Hủy bỏ'
) => {
  const contentHtml = `
    ${MINIMAL_ICONS.warning}
    <div class="minimal-title">${title}</div>
    ${text ? `<div class="minimal-desc">${formatHighlight(text)}</div>` : ''}
  `

  const result = await Swal.fire({
    html: contentHtml,
    showCancelButton: true,
    confirmButtonText,
    cancelButtonText,
    reverseButtons: true,
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      actions: 'custom-swal-actions',
      confirmButton: 'custom-swal-confirm-btn',
      cancelButton: 'custom-swal-cancel-btn'
    },
    buttonsStyling: false
  })
  return result.isConfirmed
}

/**
 * Xác nhận xóa bản ghi
 */
export const notifyDeleteConfirm = async (itemName = '') => {
  const text = itemName ? `Bạn có chắc muốn xóa "${itemName}" không?` : 'Dữ liệu bị xóa sẽ không thể phục hồi.'
  return notifyConfirm('Xác nhận xóa dữ liệu', text, 'Xóa ngay', 'Hủy bỏ')
}

export default {
  success: notifySuccess,
  error: notifyError,
  warning: notifyWarning,
  confirm: notifyConfirm,
  deleteConfirm: notifyDeleteConfirm
}
