import http from './http'

export const listScheduleShifts = () =>
  http.get('/ca-lam-viec-dinh-nghia').then((r) => r.data)
export const createScheduleShift = (data) =>
  http.post('/ca-lam-viec-dinh-nghia', data).then((r) => r.data)
export const updateScheduleShift = (id, data) =>
  http.put(`/ca-lam-viec-dinh-nghia/${id}`, data).then((r) => r.data)
export const toggleScheduleShift = (id) =>
  http.patch(`/ca-lam-viec-dinh-nghia/${id}/trang-thai`).then((r) => r.data)
export const deleteScheduleShift = (id) => http.delete(`/ca-lam-viec-dinh-nghia/${id}`)

export const listWorkSchedules = (params = {}) =>
  http.get('/lich-lam-viec', { params }).then((r) => r.data)
export const createWorkSchedule = (data) =>
  http.post('/lich-lam-viec', data).then((r) => r.data)
export const updateWorkSchedule = (id, data) =>
  http.put(`/lich-lam-viec/${id}`, data).then((r) => r.data)
export const deleteWorkSchedule = (id) => http.delete(`/lich-lam-viec/${id}`)
export const deleteWorkSchedulesForDate = (ngay) =>
  http.delete('/lich-lam-viec', { params: { ngay } })
