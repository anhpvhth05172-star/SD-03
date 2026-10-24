import { ref } from 'vue'

const khoaVaiTro = 'vaiTroHienTai'
const khoaNguoiDung = 'nguoiDungHienTai'

const doc = (key) => {
  try {
    return localStorage.getItem(key) || ''
  } catch {
    return ''
  }
}

const ghi = (key, value) => {
  try {
    localStorage.setItem(key, value)
  } catch {
    /* localStorage không khả dụng */
  }
}

export const vaiTroHienTai = ref((doc(khoaVaiTro) || '').toUpperCase())

export const laAdmin = () => vaiTroHienTai.value === 'ADMIN'

export const datVaiTro = (vaiTro) => {
  const v = String(vaiTro || '').toUpperCase()
  vaiTroHienTai.value = v
  ghi(khoaVaiTro, v)
}

export const tenVaiTro = () => (laAdmin() ? 'Admin' : 'Nhân viên')

export const datNguoiDung = (ten) => ghi(khoaNguoiDung, ten || '')
export const layNguoiDung = () => doc(khoaNguoiDung) || 'admin'

if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => {
    if (e.key === khoaVaiTro) {
      vaiTroHienTai.value = String(e.newValue || '').toUpperCase()
    } else if (e.key === 'polyshoes_user' && e.newValue) {
      try {
        const parsed = JSON.parse(e.newValue)
        if (parsed?.vaiTro) {
          vaiTroHienTai.value = String(parsed.vaiTro).toUpperCase()
        }
      } catch {
        // ignore parse error
      }
    }
  })
}
