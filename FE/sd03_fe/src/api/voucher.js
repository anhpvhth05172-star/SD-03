import http from './http'

export const listVouchers = () => http.get('/phieu-giam-gia').then((r) => r.data)

export const suDungCuaToi = () => http.get('/phieu-giam-gia/cua-toi').then((r) => r.data)
