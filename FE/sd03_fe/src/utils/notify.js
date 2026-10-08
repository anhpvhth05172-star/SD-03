import Swal from 'sweetalert2'
import '@/assets/styles/custom-swal.css'

/**
 * Hiển thị thông báo thành công dạng popup ở giữa màn hình (giống ảnh mẫu)
 * @param {string} title Nội dung thông báo chính (hoặc tiêu đề)
 * @param {string} [text] Nội dung phụ (tùy chọn)
 * @param {number} [timer=1800] Thời gian tự động đóng (ms)
 */
export const notifySuccess = (title, text = '', timer = 1800) => {
  return Swal.fire({
    icon: 'success',
    title: title || 'Thành công',
    text: text,
    showConfirmButton: false,
    timer: timer,
    timerProgressBar: false,
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      icon: 'custom-swal-icon-success',
      title: 'custom-swal-title',
      htmlContainer: 'custom-swal-html'
    }
  })
}

/**
 * Hiển thị thông báo thất bại / lỗi dạng popup ở giữa màn hình
 * @param {string} title Tiêu đề lỗi
 * @param {string} [text] Chi tiết lỗi
 */
export const notifyError = (title, text = '') => {
  return Swal.fire({
    icon: 'error',
    title: title || 'Thất bại',
    text: text,
    showConfirmButton: true,
    confirmButtonText: 'Đóng',
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      icon: 'custom-swal-icon-error',
      title: 'custom-swal-title',
      htmlContainer: 'custom-swal-html',
      confirmButton: 'custom-swal-confirm-btn'
    },
    buttonsStyling: false
  })
}

/**
 * Hiển thị thông báo cảnh báo dạng popup
 * @param {string} title Tiêu đề cảnh báo
 * @param {string} [text] Chi tiết
 */
export const notifyWarning = (title, text = '') => {
  return Swal.fire({
    icon: 'warning',
    title: title || 'Cảnh báo',
    text: text,
    showConfirmButton: true,
    confirmButtonText: 'Đã hiểu',
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      icon: 'custom-swal-icon-warning',
      title: 'custom-swal-title',
      htmlContainer: 'custom-swal-html',
      confirmButton: 'custom-swal-confirm-btn'
    },
    buttonsStyling: false
  })
}

/**
 * Hiển thị hộp thoại xác nhận (Confirm modal)
 * @param {string} title Tiêu đề xác nhận
 * @param {string} text Nội dung giải thích
 * @param {string} confirmText Nhãn nút xác nhận
 * @param {string} cancelText Nhãn nút hủy
 * @returns {Promise<boolean>} Trả về true nếu người dùng bấm xác nhận
 */
export const notifyConfirm = async (
  title = 'Bạn có chắc chắn?',
  text = 'Thao tác này sẽ không thể hoàn tác!',
  confirmText = 'Đồng ý',
  cancelText = 'Hủy'
) => {
  const result = await Swal.fire({
    title,
    text,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText,
    cancelButtonText,
    reverseButtons: true,
    customClass: {
      popup: 'custom-swal-popup',
      container: 'custom-swal-container',
      icon: 'custom-swal-icon-warning',
      title: 'custom-swal-title',
      htmlContainer: 'custom-swal-html',
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
 * @param {string} itemName Tên hoặc mã của đối tượng cần xóa
 * @returns {Promise<boolean>}
 */
export const notifyDeleteConfirm = async (itemName = '') => {
  const text = itemName ? `Bạn có chắc muốn xóa "${itemName}" không?` : 'Bạn có chắc chắn muốn xóa bản ghi này?'
  return notifyConfirm('Xác nhận xóa dữ liệu', text, 'Xóa ngay', 'Hủy bỏ')
}

export default {
  success: notifySuccess,
  error: notifyError,
  warning: notifyWarning,
  confirm: notifyConfirm,
  deleteConfirm: notifyDeleteConfirm
}
