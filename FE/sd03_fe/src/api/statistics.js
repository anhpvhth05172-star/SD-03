import http from './http'

export const listStatisticEmployees = (params = {}) =>
  http.get('/thong-ke/nhan-vien', { params }).then((r) => r.data)

export const getRevenueSummary = (params) =>
  http.get('/thong-ke/doanh-thu', { params }).then((r) => r.data)
