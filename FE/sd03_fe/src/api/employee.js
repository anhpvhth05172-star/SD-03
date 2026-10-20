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

export const suggestEmployeeCode = (ten) =>
  http.get('/nhan-vien/ma-tu-dong', { params: { ten } }).then((r) => r.data)

export const updateEmployeeStatus = (id, trangThai) =>
  http.put(`/nhan-vien/${id}/trang-thai`, null, { params: { trangThai } }).then((r) => r.data)

export const changeEmployeePassword = (id, matKhauMoi, xacNhan) =>
  http.put(`/nhan-vien/${id}/doi-mat-khau`, { matKhauMoi, xacNhan }).then((r) => r.data)

export const checkEmployeeEmail = (email, excludeId) =>
  http.get('/nhan-vien/check-email', { params: { email, excludeId } }).then((r) => r.data)

export const sendEmployeesBulkEmail = (data) =>
  http.post('/nhan-vien/gui-email-hang-loat', data).then((r) => r.data)

export const downloadEmployeeImportTemplate = () =>
  http.get('/nhan-vien/import/mau', { responseType: 'blob' }).then((r) => r.data)

const buildImportFormData = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return fd
}

// Override Content-Type JSON mặc định của instance: nếu để application/json,
// axios 1.x sẽ serialize FormData thành JSON (formDataToJSON) và backend báo
// "Current request is not a multipart request". Để undefined thì axios bỏ hẳn
// header này, browser tự gắn multipart/form-data kèm boundary.
const multipartConfig = { headers: { 'Content-Type': undefined } }

export const importEmployeesPreview = (file) =>
  http.post('/nhan-vien/import/xem-truoc', buildImportFormData(file), multipartConfig).then((r) => r.data)

export const importEmployeesConfirm = (file) =>
  http.post('/nhan-vien/import/xac-nhan', buildImportFormData(file), multipartConfig).then((r) => r.data)
