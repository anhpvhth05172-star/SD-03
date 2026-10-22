import http from './http'

// GET /api/ca/dang-mo — 200 + body rỗng khi nhân viên chưa có ca DANG_MO
export const getDangMo = () =>
  http.get('/ca/dang-mo').then((r) => {
    const d = r.data
    return d && d.id ? d : null
  })

// POST /api/ca/mo-ca — 201 CaLamViecDto; 400 CA_DA_MO nếu đã có ca đang mở
export const moCa = (payload) => http.post('/ca/mo-ca', payload).then((r) => r.data)

// GET /api/ca/{id}/tong-ket — TongKetDto (đối soát trước khi kết ca)
export const getTongKet = (id) => http.get(`/ca/${id}/tong-ket`).then((r) => r.data)

// POST /api/ca/{id}/ket-ca — lỗi: 400 LY_DO_LECH_THIEU / CON_HD_DANG_XU_LY, 409 CA_DANG_KET
export const ketCa = (id, payload) => http.post(`/ca/${id}/ket-ca`, payload).then((r) => r.data)
