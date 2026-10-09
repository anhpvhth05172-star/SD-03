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

export const vaiTroHienTai = ref((doc(khoaVaiTro) || 'ADMIN').toUpperCase())

export const laAdmin = () => vaiTroHienTai.value === 'ADMIN'

export const datVaiTro = (vaiTro) => {
  const v = String(vaiTro || 'ADMIN').toUpperCase()
  vaiTroHienTai.value = v
  ghi(khoaVaiTro, v)
}

export const tenVaiTro = () => (laAdmin() ? 'Admin' : 'Nhân viên')

export const datNguoiDung = (ten) => ghi(khoaNguoiDung, ten || '')
export const layNguoiDung = () => doc(khoaNguoiDung) || 'admin'
