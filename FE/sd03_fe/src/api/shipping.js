import http from './http'

export const getProvinces = () =>
  http.get('/shipping/provinces').then((r) => r.data)

export const getDistricts = (provinceId) =>
  http.get('/shipping/districts', { params: { provinceId } }).then((r) => r.data)

export const getWards = (provinceId) =>
  http.get('/shipping/wards', { params: { provinceId } }).then((r) => r.data)

export const getServices = (toDistrictId) =>
  http.get('/shipping/services', { params: { toDistrictId } }).then((r) => r.data)

export const tinhPhi = (data) =>
  http.post('/shipping/fee', data).then((r) => r.data)
