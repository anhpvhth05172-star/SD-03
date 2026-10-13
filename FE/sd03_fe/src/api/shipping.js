import http from './http'

export const getProvinces = () =>
  http.get('/shipping/provinces').then((r) => r.data)

export const getDistricts = (provinceId) =>
  http.get('/shipping/districts', { params: { provinceId } }).then((r) => r.data)

export const getWards = (districtId) =>
  http.get('/shipping/wards', { params: { districtId } }).then((r) => r.data)

export const tinhPhi = (data) =>
  http.post('/shipping/fee', data).then((r) => r.data)
