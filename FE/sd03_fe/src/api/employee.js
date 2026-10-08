import http from './http'

export const listEmployees = (params) =>
  http.get('/nhan-vien', { params }).then((r) => r.data)

export const getEmployee = (id) => http.get(`/nhan-vien/${id}`).then((r) => r.data)

export const createEmployee = (data) =>
  http.post('/nhan-vien', data).then((r) => r.data)

export const updateEmployee = (id, data) =>
  http.put(`/nhan-vien/${id}`, data).then((r) => r.data)

export const deleteEmployee = (id) => http.delete(`/nhan-vien/${id}`).then((r) => r.data)

export const getEmployeeFormData = () =>
  http.get('/nhan-vien/form-data').then((r) => r.data)
