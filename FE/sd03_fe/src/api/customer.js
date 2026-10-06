import http from './http'

export const listCustomers = () => http.get('/khach-hang').then((r) => r.data)
