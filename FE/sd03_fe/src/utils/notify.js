import Swal from 'sweetalert2'
import '@/assets/styles/custom-swal.css'

/**
 * Dọn dẹp triệt để các lớp phủ hoặc thuộc tính khóa giao diện của SweetAlert2
 */
export const cleanupSwalResidualDom = () => {
  if (typeof document === 'undefined') return
  try {
    if (!Swal.isVisible()) {
      document.querySelectorAll('.swal2-container').forEach((el) => el.remove())
      document.body.classList.remove('swal2-shown', 'swal2-height-auto', 'swal2-no-backdrop', 'swal2-toast-shown', 'modal-open')
      document.documentElement.classList.remove('swal2-shown', 'swal2-height-auto', 'swal2-no-backdrop', 'swal2-toast-shown')
      document.body.style.removeProperty('padding-right')
      document.body.style.removeProperty('overflow')
      const appEl = document.getElementById('app')
      if (appEl && appEl.getAttribute('aria-hidden') === 'true') {
        appEl.removeAttribute('aria-hidden')
      }
      document.querySelectorAll('[aria-hidden="true"]').forEach((el) => {
        if (el.id === 'app' || el.classList.contains('app-layout') || el.classList.contains('main-content-wrapper')) {
          el.removeAttribute('aria-hidden')
        }
      })
    }
  } catch {}
}

/**
 * Format string quotes into clean strong tags
 */
const formatHighlight = (text) => {
  if (!text) return ''
  return String(text)
    .replace(/["“]([^"”]+)["”]/g, '<strong>"$1"</strong>')
    .replace(/\n/g, '<br/>')
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

const NO_HIDE_ANIMATION = {
  showClass: {
    popup: 'swal2-show',
    backdrop: 'swal2-backdrop-show',
    icon: ''
  },
  hideClass: {
    popup: '',
    backdrop: '',
    icon: ''
  },
  returnFocus: false
}

/**
 * Hiển thị thông báo thành công (Không chặn click nền để thu ngân thao tác liên tục)
 * @param {string} title Nội dung thông báo
 * @param {string} [text] Nội dung phụ
 * @param {number} [timer=1400] Thời gian tự tắt (ms)
 */
export const notifySuccess = (title, text = '', timer = 1400) => {
  const contentHtml = `
    ${MINIMAL_ICONS.success}
    <div class="minimal-title">${formatHighlight(title || 'Thành công')}</div>
    ${text ? `<div class="minimal-desc">${formatHighlight(text)}</div>` : ''}
  `

  return Swal.fire({
    ...NO_HIDE_ANIMATION,
    html: contentHtml,
    showConfirmButton: false,
    timer: timer,
    timerProgressBar: false,
    backdrop: false,
    allowOutsideClick: true,
    allowEscapeKey: true,
    customClass: {
      popup: 'custom-swal-popup is-non-blocking-popup',
      container: 'custom-swal-container is-non-blocking-container'
    },
    didOpen: (popup) => {
      if (popup) {
        popup.style.cursor = 'pointer'
        popup.addEventListener('click', () => {
          Swal.close()
        })
      }
    },
    didClose: () => {
      setTimeout(cleanupSwalResidualDom, 10)
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
    ...NO_HIDE_ANIMATION,
    html: contentHtml,
    showConfirmButton: true,
    confirmButtonText: 'Đóng',
    allowOutsideClick: true,
    allowEscapeKey: true,
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      actions: 'custom-swal-actions',
      confirmButton: 'custom-swal-confirm-btn'
    },
    buttonsStyling: false,
    didClose: () => {
      setTimeout(cleanupSwalResidualDom, 10)
    }
  })
}

/**
 * Hiển thị thông báo cảnh báo
 */
export const notifyWarning = (title, text = '', timer = undefined) => {
  const contentHtml = `
    ${MINIMAL_ICONS.warning}
    <div class="minimal-title">${title || 'Cảnh báo'}</div>
    ${text ? `<div class="minimal-desc">${formatHighlight(text)}</div>` : ''}
  `

  return Swal.fire({
    ...NO_HIDE_ANIMATION,
    html: contentHtml,
    showConfirmButton: true,
    confirmButtonText: 'Đã hiểu',
    timer: timer,
    allowOutsideClick: true,
    allowEscapeKey: true,
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      actions: 'custom-swal-actions',
      confirmButton: 'custom-swal-confirm-btn'
    },
    buttonsStyling: false,
    didClose: () => {
      setTimeout(cleanupSwalResidualDom, 10)
    }
  })
}

/**
 * Hộp thoại xác nhận (Confirm modal)
 */
export const notifyConfirm = async (
  title = 'Bạn có chắc chắn?',
  text = 'Thao tác này sẽ không thể hoàn tác!',
  confirmButtonText = 'Đồng ý',
  cancelButtonText = 'Hủy bỏ'
) => {
  const contentHtml = `
    ${MINIMAL_ICONS.warning}
    <div class="minimal-title">${title}</div>
    ${text ? `<div class="minimal-desc">${formatHighlight(text)}</div>` : ''}
  `

  try {
    const result = await Swal.fire({
      ...NO_HIDE_ANIMATION,
      html: contentHtml,
      showCancelButton: true,
      confirmButtonText,
      cancelButtonText,
      reverseButtons: true,
      allowOutsideClick: true,
      allowEscapeKey: true,
      customClass: {
        popup: 'custom-swal-popup',
        container: 'custom-swal-container',
        actions: 'custom-swal-actions',
        confirmButton: 'custom-swal-confirm-btn',
        cancelButton: 'custom-swal-cancel-btn'
      },
      buttonsStyling: false,
      didClose: () => {
        setTimeout(cleanupSwalResidualDom, 10)
      }
    })
    return Boolean(result && result.isConfirmed)
  } finally {
    setTimeout(cleanupSwalResidualDom, 20)
  }
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
  deleteConfirm: notifyDeleteConfirm,
  cleanup: cleanupSwalResidualDom
}
