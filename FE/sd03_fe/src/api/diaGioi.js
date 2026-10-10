const URL_API = 'https://provinces.open-api.vn/api/v2/?depth=2'

let cache = null
let pending = null

/**
 * Tải 1 lần duy nhất danh sách tỉnh/thành + phường/xã (depth=2).
 * Kết quả được cache trong phiên, các lần gọi sau trả về ngay.
 */
export const layDiaGioi = () => {
  if (cache) return Promise.resolve(cache)
  if (pending) return pending
  pending = fetch(URL_API)
    .then((r) => {
      if (!r.ok) throw new Error('Không tải được danh sách tỉnh/thành')
      return r.json()
    })
    .then((ds) => {
      cache = (ds || []).map((t) => ({
        code: t.code,
        ten: t.name,
        phuongXa: (t.wards || []).map((w) => ({ code: w.code, ten: w.name })),
      }))
      pending = null
      return cache
    })
    .catch((e) => {
      pending = null
      throw e
    })
  return pending
}

export const timTinh = (ds, tenHayCode) => {
  if (!tenHayCode) return null
  const k = String(tenHayCode).trim().toLowerCase()
  return (ds || []).find((t) => String(t.ten).toLowerCase() === k || String(t.code) === k) || null
}

export const layPhuongXa = (ds, tinh) => (tinh ? tinh.phuongXa || [] : [])
