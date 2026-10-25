import http from "./http";

export const themVaoGioHangApi = (data) =>
  http.post("/gio-hang/them", data).then((r) => r.data);

export const layGioHangApi = (idKhachHang) =>
  http.get("/gio-hang", { params: { idKhachHang } }).then((r) => r.data);

export const demSoLuongGioHangApi = (idKhachHang) =>
  http.get("/gio-hang/count", { params: { idKhachHang } }).then((r) => r.data);

export const capNhatSoLuongApi = (idGioHangChiTiet, data) =>
  http.put(`/gio-hang/items/${idGioHangChiTiet}`, data).then((r) => r.data);

export const xoaItemGioHangApi = (idGioHangChiTiet, idKhachHang) =>
  http
    .delete(`/gio-hang/items/${idGioHangChiTiet}`, { params: { idKhachHang } })
    .then((r) => r.data);

export const xoaTatCaGioHangApi = (idKhachHang) =>
  http.delete("/gio-hang", { params: { idKhachHang } }).then((r) => r.data);
