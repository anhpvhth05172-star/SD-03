import http from './http'

export const listDiscountEvents = () => http.get('/dot-giam-gia').then((r) => r.data)
