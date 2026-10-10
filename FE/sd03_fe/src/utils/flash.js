let thongDiep = ''

export const setFlash = (message) => {
  thongDiep = message || ''
}

export const takeFlash = () => {
  const m = thongDiep
  thongDiep = ''
  return m
}
