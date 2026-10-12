let thongDiep = ''
let loaiThongDiep = 'success'

export const setFlash = (message, loai = 'success') => {
  thongDiep = message || ''
  loaiThongDiep = loai === 'warning' || loai === 'error' ? loai : 'success'
}

export const takeFlash = () => takeFlashWithMeta().message

export const takeFlashWithMeta = () => {
  const message = thongDiep
  const type = loaiThongDiep
  thongDiep = ''
  loaiThongDiep = 'success'
  return { message, type }
}
