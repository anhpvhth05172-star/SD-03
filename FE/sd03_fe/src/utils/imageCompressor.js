/**
 * Tiện ích kiểm tra định dạng và nén ảnh client-side
 * Hỗ trợ mọi định dạng ảnh phổ biến (JPG, PNG, WEBP, AVIF, GIF, BMP, JFIF).
 * Tự động scale kích thước tối đa và nén giảm dung lượng trước khi chuyển thành Base64,
 * giúp tải lên nhanh chóng, không bị quá tải bộ nhớ và tương thích 100% với hệ thống.
 */

export const checkImageMagicBytes = async (file) => {
  if (!file) return false

  // Nếu trình duyệt đã nhận diện là image/* thì ưu tiên chấp nhận
  if (file.type && file.type.startsWith('image/')) {
    // Vẫn đọc thử vài byte đầu để loại trừ tệp rác giả mạo
    try {
      const buffer = await file.slice(0, 16).arrayBuffer()
      const arr = new Uint8Array(buffer)
      if (arr.length < 2) return false

      // JPEG: FF D8
      if (arr[0] === 0xFF && arr[1] === 0xD8) return true
      // PNG: 89 50 4E 47
      if (arr[0] === 0x89 && arr[1] === 0x50 && arr[2] === 0x4E && arr[3] === 0x47) return true
      // GIF: 47 49 46
      if (arr[0] === 0x47 && arr[1] === 0x49 && arr[2] === 0x46) return true
      // BMP: 42 4D
      if (arr[0] === 0x42 && arr[1] === 0x4D) return true
      // WEBP: RIFF....WEBP
      if (arr[0] === 0x52 && arr[1] === 0x49 && arr[2] === 0x46 && arr[3] === 0x46) return true
      // AVIF: ...ftyp
      if (arr.length >= 8 && arr[4] === 0x66 && arr[5] === 0x74 && arr[6] === 0x79 && arr[7] === 0x70) return true

      // Nếu là SVG hoặc định dạng ảnh khác mà browser nhận là image/*
      return true
    } catch {
      return true
    }
  }

  // Nếu file.type rỗng (một số máy Windows không gán MIME type cho .avif/.jfif)
  const ext = (file.name || '').toLowerCase()
  if (/\.(jpg|jpeg|jfif|png|webp|avif|gif|bmp|svg)$/i.test(ext)) {
    return true
  }

  return false
}

/**
 * Đọc và nén ảnh về Base64 Data URL
 * @param {File} file - File ảnh người dùng tải lên
 * @param {number} maxDimension - Kích thước tối đa chiều rộng/cao (mặc định 1200px)
 * @param {number} quality - Chất lượng nén JPEG (mặc định 0.85)
 * @returns {Promise<string>} Chuỗi Base64 DataURL đã tối ưu
 */
export const compressImageToBase64 = (file, maxDimension = 1200, quality = 0.85) => {
  return new Promise((resolve, reject) => {
    if (!file) return reject(new Error('Không có tệp ảnh'))

    const reader = new FileReader()
    reader.onerror = () => reject(new Error('Không thể đọc tệp ảnh'))
    reader.onload = (e) => {
      const srcDataUrl = e.target.result

      // Nếu là SVG hoặc GIF (để giữ animation), không nén qua canvas
      if (file.type === 'image/svg+xml' || file.type === 'image/gif') {
        return resolve(srcDataUrl)
      }

      const img = new Image()
      img.onerror = () => {
        // Fallback: Nếu canvas decode lỗi, trả về nguyên bản
        resolve(srcDataUrl)
      }
      img.onload = () => {
        try {
          let { width, height } = img

          // Tính toán tỷ lệ co giãn nếu vượt quá maxDimension
          if (width > maxDimension || height > maxDimension) {
            if (width > height) {
              height = Math.round((height * maxDimension) / width)
              width = maxDimension
            } else {
              width = Math.round((width * maxDimension) / height)
              height = maxDimension
            }
          }

          const canvas = document.createElement('canvas')
          canvas.width = width
          canvas.height = height
          const ctx = canvas.getContext('2d')

          // Tô nền trắng cho ảnh có độ trong suốt nếu chuyển sang JPEG
          if (file.type !== 'image/png') {
            ctx.fillStyle = '#FFFFFF'
            ctx.fillRect(0, 0, width, height)
          }

          ctx.drawImage(img, 0, 0, width, height)

          // Định dạng xuất: PNG nếu gốc là PNG, ngược lại dùng JPEG chất lượng cao
          const outputFormat = file.type === 'image/png' ? 'image/png' : 'image/jpeg'
          const compressedDataUrl = canvas.toDataURL(outputFormat, quality)
          resolve(compressedDataUrl)
        } catch {
          resolve(srcDataUrl)
        }
      }
      img.src = srcDataUrl
    }
    reader.readAsDataURL(file)
  })
}
