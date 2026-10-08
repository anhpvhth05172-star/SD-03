import http from './http'

export const listInvoices = (params) =>
  http.get('/hoa-don', { params }).then((r) => r.data)

export const getInvoice = (id) => http.get(`/hoa-don/${id}`).then((r) => r.data)

export const createInvoice = (data) =>
  http.post('/hoa-don', data).then((r) => r.data)

export const getInvoiceFormData = () =>
  http.get('/hoa-don/form-data').then((r) => r.data)
