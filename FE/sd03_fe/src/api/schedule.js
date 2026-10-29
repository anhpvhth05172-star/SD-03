import http from './http'

/* ---------------------------------- Ca làm ---------------------------------- */
export const listCaLam = () => http.get('/ca-lam').then((r) => r.data)

export const createCaLam = (data) => http.post('/ca-lam', data).then((r) => r.data)

export const updateCaLam = (id, data) => http.put(`/ca-lam/${id}`, data).then((r) => r.data)

export const toggleCaLam = (id) => http.put(`/ca-lam/${id}/trang-thai`).then((r) => r.data)

export const deleteCaLam = (id) => http.delete(`/ca-lam/${id}`).then((r) => r.data)

/* ---------------------------------- Lịch làm việc ---------------------------------- */
export const listLich = (params) => http.get('/lich-lam-viec', { params }).then((r) => r.data)

export const createLich = (data) => http.post('/lich-lam-viec', data).then((r) => r.data)

export const updateLich = (id, data) => http.put(`/lich-lam-viec/${id}`, data).then((r) => r.data)

export const deleteLich = (id) => http.delete(`/lich-lam-viec/${id}`).then((r) => r.data)

export const deleteLichTheoNgay = (ngay) =>
  http.delete('/lich-lam-viec', { params: { ngay } }).then((r) => r.data)

export const xepTuDong = (payload) => {
  const data = typeof payload === 'string' ? { ngay: payload } : payload
  return http.post('/lich-lam-viec/xep-tu-dong', data).then((r) => r.data)
}

export const importLich = (rows) =>
  http.post('/lich-lam-viec/import', { rows }).then((r) => r.data)
