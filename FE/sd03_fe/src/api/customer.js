import http from './http'

export const listCustomers = (params) =>
  http.get('/khach-hang', { params }).then((r) => r.data)

export const getCustomer = (id) => http.get(`/khach-hang/${id}`).then((r) => r.data)

export const createCustomer = (data) =>
  http.post('/khach-hang', data).then((r) => r.data)

export const updateCustomer = (id, data) =>
  http.put(`/khach-hang/${id}`, data).then((r) => r.data)

export const deleteCustomer = (id) => http.delete(`/khach-hang/${id}`).then((r) => r.data)

export const suggestCustomerCode = (ten) =>
  http.get('/khach-hang/ma-tu-dong', { params: { ten } }).then((r) => r.data)

export const updateCustomerStatus = (id, trangThai) =>
  http.put(`/khach-hang/${id}/trang-thai`, null, { params: { trangThai } }).then((r) => r.data)

export const listAddresses = (idKhachHang) =>
  http.get(`/khach-hang/${idKhachHang}/dia-chi`).then((r) => r.data)

export const createAddress = (idKhachHang, data) =>
  http.post(`/khach-hang/${idKhachHang}/dia-chi`, data).then((r) => r.data)

export const updateAddress = (idKhachHang, id, data) =>
  http.put(`/khach-hang/${idKhachHang}/dia-chi/${id}`, data).then((r) => r.data)

export const deleteAddress = (idKhachHang, id) =>
  http.delete(`/khach-hang/${idKhachHang}/dia-chi/${id}`).then((r) => r.data)

export const setDefaultAddress = (idKhachHang, id) =>
  http.put(`/khach-hang/${idKhachHang}/dia-chi/${id}/mac-dinh`).then((r) => r.data)
