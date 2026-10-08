import http from './http'

export const listCustomers = (params) =>
  http.get('/khach-hang', { params }).then((r) => r.data)

export const getCustomer = (id) => http.get(`/khach-hang/${id}`).then((r) => r.data)

export const createCustomer = (data) =>
  http.post('/khach-hang', data).then((r) => r.data)

export const updateCustomer = (id, data) =>
  http.put(`/khach-hang/${id}`, data).then((r) => r.data)

export const deleteCustomer = (id) => http.delete(`/khach-hang/${id}`).then((r) => r.data)
