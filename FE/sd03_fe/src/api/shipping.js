import http from './http'
import { DS_34_TINH_THANH, layPhuongXa, timTinh } from './diaGioi'

const isDummyWardList = (list) =>
  Array.isArray(list) &&
  list.every(
    (w) =>
      w?.name === 'Phường/Xã trung tâm' ||
      w?.name === 'Khu vực nội thành',
  )

export const getProvinces = async () => {
  try {
    const data = await http.get('/shipping/provinces').then((r) => r.data)
    if (Array.isArray(data) && data.length > 0) return data
  } catch {
    // Dùng danh mục 34 tỉnh/thành chuẩn nếu endpoint backend lỗi
  }
  return DS_34_TINH_THANH.map((p, idx) => ({
    id: p.subCodes?.[0] || idx + 1,
    name: p.ten,
  }))
}

export const getDistricts = (provinceId) =>
  http.get('/shipping/districts', { params: { provinceId } }).then((r) => r.data)

export const getWards = async (provinceId, provinceName = '') => {
  const numId = Number(provinceId)
  const matched =
    (provinceName ? timTinh(DS_34_TINH_THANH, provinceName) : null) ||
    DS_34_TINH_THANH.find(
      (t) =>
        String(t.code).toLowerCase() === String(provinceId || '').toLowerCase() ||
        (Array.isArray(t.subCodes) && t.subCodes.includes(numId)),
    )

  const targetTinh = matched
    ? !isNaN(numId) && numId > 0 && matched.subCodes?.[0] !== numId
      ? { ...matched, id: numId, subCodes: [numId] }
      : { ...matched, id: numId || matched.subCodes?.[0] }
    : !isNaN(numId) && numId > 0
      ? { code: numId, id: numId, ten: provinceName || '', subCodes: [numId] }
      : null

  if (targetTinh) {
    const list = await layPhuongXa(DS_34_TINH_THANH, targetTinh)
    if (Array.isArray(list) && list.length > 0) {
      return list.map((w) => ({
        id: w.code,
        name: String(w.ten || '').replace(/\s*\([^)]*\)\s*$/, '').trim() || w.ten,
      }))
    }
  }

  try {
    const data = await http.get('/shipping/wards', { params: { provinceId } }).then((r) => r.data)
    if (Array.isArray(data) && data.length > 0 && !isDummyWardList(data)) {
      return data.map((w) => ({
        id: w.id,
        name: String(w.name || '').replace(/\s*\([^)]*\)\s*$/, '').trim() || w.name,
      }))
    }
  } catch {
    // Bỏ qua nếu backend lỗi
  }

  return []
}

export const getServices = (toDistrictId) =>
  http.get('/shipping/services', { params: { toDistrictId } }).then((r) => r.data)

export const tinhPhi = (data) =>
  http.post('/shipping/fee', data).then((r) => r.data)

export const getFreeshipConfig = () =>
  http.get('/shipping/freeship-config').then((r) => r.data)

export const updateFreeshipConfig = (freeShipThreshold) =>
  http.put('/shipping/freeship-config', { freeShipThreshold }).then((r) => r.data)
