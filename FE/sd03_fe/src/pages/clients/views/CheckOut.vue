<script setup>
import { reactive, ref, computed, watch, onMounted, nextTick } from "vue";
import * as store from "../store";
import { createInvoice } from "@/api/invoice";
import { listVouchers, suDungCuaToi } from "@/api/voucher";
import { getProvinces, getWards, getServices, tinhPhi, getFreeshipConfig } from "@/api/shipping";
import { notifyConfirm, notifySuccess, notifyWarning, notifyError } from "@/utils/notify";
import { getUser, isKhachHangDangNhap } from "@/utils/auth";

const { cart, cartTotal, money } = store;

// Phí vận chuyển đơn online cơ bản (chuẩn nội thành 22.000đ).
const SHIP_FEE = 22000;
const shippingFee = ref(SHIP_FEE);
const provinces = ref([]);
const wards = ref([]);
const provinceId = ref("");
const wardId = ref("");
const toProvinceId = ref(null);
const toDistrictId = ref(null);
const toWardCode = ref("");
const serviceTypeId = ref(null);
const shippingLoading = ref(false);
const shippingError = ref("");
const savedAddressToRestore = ref(null);
let shippingRequestId = 0;

const getToken = () => {
  for (const area of [localStorage, sessionStorage]) {
    const token = area.getItem("polyshoes_token") || area.getItem("token") || area.getItem("accessToken");
    if (token) return token;
  }
  return "";
};

const authHeaders = () => {
  const t = getToken();
  return t ? { Authorization: `Bearer ${t}` } : {};
};

async function getJson(url) {
  const res = await fetch(url, { headers: authHeaders() });
  if (!res.ok) throw new Error(res.status);
  return res.json();
}

function getLoggedInUser() {
  const user = getUser();
  if (!user || !isKhachHangDangNhap()) return null;
  return { ...user, id: user.idKhachHang ?? user.id };
}

async function fetchCustomer() {
  const u = getLoggedInUser();
  console.log("[Checkout] user đăng nhập:", u);
  if (!u) {
    console.warn(
      "[Checkout] Không tìm thấy user đăng nhập trong store/localStorage",
    );
    return null;
  }

  try {
    const profile = await getJson("/api/auth/thong-tin");
    return { ...u, ...profile, id: profile.id ?? u.idKhachHang ?? u.id };
  } catch (e) {
    console.warn("[Checkout] Không tải được thông tin phiên:", e.message);
    return u;
  }
}

const customer = ref(null);
onMounted(async () => {
  const [, shippingResult] = await Promise.allSettled([
    fetchCustomer().then((result) => { customer.value = result; }),
    loadProvinces(),
    getFreeshipConfig().then((cfg) => {
      if (cfg?.freeShipThreshold != null) freeShipThreshold.value = Number(cfg.freeShipThreshold);
    }).catch(() => {}),
  ]);
  if (shippingResult.status === "rejected") shippingError.value = "Không tải được danh sách tỉnh/thành phố.";
  await loadVouchers();
});

/* ============== ADAPTER theo JSON thực tế ============== */
const currentUser = computed(() => customer.value);
const isLoggedIn = computed(() => !!customer.value);

const userInfo = (u) => ({
  name: u.tenKhachHang || "",
  phone: u.soDienThoai || "",
  email: u.email || "",
});

const fullAddress = (a) =>
  [a.diaChiCuThe, a.phuong, a.thanhPho].filter(Boolean).join(", ");

const savedAddresses = computed(() => {
  const c = customer.value;
  if (!c) return [];
  const list = c.danhSachDiaChi?.length
    ? c.danhSachDiaChi
    : c.diaChiMacDinh
      ? [c.diaChiMacDinh]
      : [];
  return list
    .filter((a) => a.trangThai !== false)
    .map((a) => ({
      id: a.id,
      name: a.tenChiChi || c.tenKhachHang || "",
      phone: c.soDienThoai || "",
      address: fullAddress(a),
      street: a.diaChiCuThe || "",
      ward: a.phuong || "",
      province: a.thanhPho || a.tinhThanhPho || "",
      isDefault: !!a.macDinh,
    }))
    .sort((x, y) => Number(y.isDefault) - Number(x.isDefault)); // mặc định lên đầu
});

const itemImage = (i) => i.hinhAnh || i.img || i.image || null;

// Ảnh lỗi -> hiện emoji thay thế
const imgFailed = ref({});
const itemKey = (i) => `${i.id}-${i.size}`;

// Tổng số lượng sản phẩm khách đã chọn (cộng dồn qty của từng dòng)
const totalQty = computed(() =>
  cart.items.reduce((sum, i) => sum + (Number(i.qty) || 0), 0),
);

/* ================= PHIẾU GIẢM GIÁ (gọi API thật) ================= */
const availableVouchers = ref([]);
const selectedVoucherId = ref("");
const voucherCode = ref("");

/* >>> ADAPTER: đổi tên field cho khớp entity/DTO PhieuGiamGia của bạn <<< */
const VF = {
  id: (v) => v.id,
  code: (v) => v.maPhieuGiamGia,
  loai: (v) => String(v.loaiGiamGia ?? "").toUpperCase(), // "PERCENT" => theo %, còn lại => số tiền
  value: (v) => Number(v.giaTriGiam ?? 0),
  max: (v) => Number(v.giamToiDa ?? 0),
  min: (v) => Number(v.hoaDonToiThieu ?? 0),
  start: (v) => v.ngayBatDau,
  end: (v) => v.ngayKetThuc,
  // số lượt còn lại (undefined nếu phiếu không giới hạn)
  qty: (v) => {
    const conLai = v.soLuongConLai ?? v.soLuongCon;
    if (conLai != null) return Number(conLai);
    const total = v.soLuong ?? v.soLuongToiDa ?? v.soLuongPhieu;
    if (total == null) return undefined;
    const used =
      v.soLuongDaSuDung ??
      v.soLuongDaDung ??
      v.daSuDung ??
      v.soLuongSuDung ??
      0;
    return Number(total) - Number(used);
  },
  active: (v) => v.trangThai,
  scope: (v) => String(v.phamVi ?? "").toUpperCase(), // chứa CA_NHAN/KHACH_HANG => riêng từng khách
  // danh sách id khách được áp dụng (lấy từ API detail hoặc DTO)
  customerIds: (d) =>
    (d.idKhachHangsChuaDung || d.danhSachKhachHang || d.khachHangIds || d.khachHang || []).map((k) =>
      typeof k === "object" ? (k.id ?? k.idKhachHang) : Number(k),
    ),
  usedCustomerIds: (d) =>
    (d.idKhachHangsDaDung || []).map((k) =>
      typeof k === "object" ? (k.id ?? k.idKhachHang) : Number(k),
    ),
};

const myVoucherUsage = ref({});
const isPercent = (loai) => /PHAN_TRAM|PERCENT|%/.test(loai);
const isPersonal = (scope) => /CA_NHAN|KHACH_HANG|RIENG|PRIVATE/.test(scope);

// Phiếu đã hết lượt sử dụng (hết số lượng hoặc trạng thái báo hết lượt)
function isExhausted(detail) {
  const qty = VF.qty(detail);
  if (qty !== undefined && qty !== null && Number(qty) <= 0) return true;
  return /HET_LUOT|HET_SO_LUONG|DA_HET|HET_SL/i.test(String(VF.active(detail)));
}

function hasCustomerReachedLimit(detail, cid) {
  if (!detail || !cid) return false;
  const numCid = Number(cid);
  if (VF.usedCustomerIds(detail).includes(numCid)) return true;
  const usage = myVoucherUsage.value[VF.id(detail)];
  if (usage) {
    const limit = Number(detail.gioiHanMoiTaiKhoan || usage.gioiHanMoiTaiKhoan || 1);
    if (Number(usage.daSuDung || 0) >= limit) return true;
  }
  return false;
}

// Kiểm tra điều kiện của 1 phiếu. Trả về chuỗi lỗi, hoặc "" nếu hợp lệ (dùng được).
// ignoreMin = true: bỏ qua điều kiện "đơn tối thiểu" (dùng khi kiểm tra gợi ý mua thêm)
function checkVoucher(detail, ignoreMin = false) {
  if (!detail) return "Mã giảm giá không tồn tại.";
  if (isExhausted(detail)) return "Đã hết lượt sử dụng.";

  const act = VF.active(detail);
  if (act === false || /NGUNG|TAT|INACTIVE|HET_HAN/i.test(String(act)))
    return "Mã giảm giá đang bị tắt hoặc đã hết hạn.";

  const now = new Date();
  const st = VF.start(detail);
  const en = VF.end(detail);
  if (st && now < new Date(st))
    return "Chưa đến thời gian áp dụng.";
  if (en && now > new Date(en)) return "Mã giảm giá đã hết hạn.";

  const isPrivate = isPersonal(VF.scope(detail));
  const cid = customer.value?.id ? Number(customer.value.id) : null;
  if (isPrivate && !cid) {
    return "Cần đăng nhập tài khoản để dùng mã giảm giá cá nhân.";
  }

  if (cid && hasCustomerReachedLimit(detail, cid)) {
    const usage = myVoucherUsage.value[VF.id(detail)];
    const limit = Number(detail.gioiHanMoiTaiKhoan || usage?.gioiHanMoiTaiKhoan || 1);
    const used = Number(usage?.daSuDung || limit);
    return `Bạn đã dùng hết lượt mã này (${used}/${limit} lần).`;
  }

  if (isPrivate && cid) {
    const allowedIds = VF.customerIds(detail);
    if (!allowedIds.includes(cid)) {
      return "Mã cá nhân không thuộc tài khoản của bạn.";
    }
  }

  const min = VF.min(detail);
  if (!ignoreMin && orderTotal.value < min)
    return `Mua thêm ${money(min - orderTotal.value)} để dùng mã này (Đơn từ ${money(min)}).`;

  return "";
}

// Chuẩn hóa dữ liệu phiếu thành { id, code, type, value, max, min }
function toVoucher(detail) {
  return {
    id: VF.id(detail),
    code: String(VF.code(detail)).toUpperCase(),
    type: isPercent(VF.loai(detail)) ? "percent" : "fixed",
    value: VF.value(detail),
    max: VF.max(detail),
    min: VF.min(detail),
  };
}

// Số tiền giảm của 1 phiếu với tổng đơn `total`
function calcDiscount(v, total) {
  if (!v) return 0;
  if (total < (v.min || 0)) return 0;
  let d = v.type === "percent" ? (total * v.value) / 100 : v.value;
  if (v.max > 0) d = Math.min(d, v.max);
  return Math.max(0, Math.min(Math.floor(d), total));
}

// Trả về { ok: true, voucher } hoặc { ok: false, message }  (nhập mã tay)
async function fetchVoucher(code) {
  const found = availableVouchers.value.find((v) => String(VF.code(v)).toUpperCase() === code);
  if (!found) return { ok: false, message: "Mã giảm giá không tồn tại." };

  // 2) Lấy chi tiết + 3) Kiểm tra điều kiện
  const msg = checkVoucher(found);
  if (msg) return { ok: false, message: msg };

  return { ok: true, voucher: toVoucher(found) };
}
/* ======================================================= */

const f = reactive({ name: "", phone: "", email: "", address: "", note: "", pay: "cod" });
const errors = reactive({});
const done = ref(false);
const orderId = ref("");

/* ---------- Tự điền khi đã đăng nhập ---------- */
const selectedAddr = ref("new");
const isAddressModalOpen = ref(false);
const isEditingAddressInModal = ref(false);
const addrModalSaving = ref(false);
const addrModalError = ref("");
const addrModalWards = ref([]);
const addrModalForm = reactive({
  id: null,
  name: "",
  phone: "",
  provinceId: "",
  wardId: "",
  street: "",
  isDefault: true,
});

const currentSelectedSavedAddr = computed(() => {
  if (selectedAddr.value === "new") return null;
  return savedAddresses.value.find((a) => a.id === selectedAddr.value) || savedAddresses.value[0] || null;
});

function selectAddrAndClose(id) {
  chooseAddress(id);
  isEditingAddressInModal.value = false;
  isAddressModalOpen.value = false;
}

function openSelectAddressListModal() {
  isEditingAddressInModal.value = false;
  addrModalError.value = "";
  isAddressModalOpen.value = true;
}

async function loadAddrModalWards(provId, targetWardName = "") {
  addrModalWards.value = [];
  addrModalForm.wardId = "";
  if (!provId) return;
  try {
    const provObj = provinces.value.find((p) => String(p.id) === String(provId));
    const list = await getWards(Number(provId), provObj?.name || "");
    addrModalWards.value = Array.isArray(list) ? list : [];
    const target = normalizePlace(targetWardName);
    if (target) {
      let match = addrModalWards.value.find(
        (w) =>
          normalizePlace(w.name) === target ||
          normalizePlace(w.name).startsWith(`${target} `) ||
          cleanPlaceName(w.name) === cleanPlaceName(targetWardName),
      );
      if (!match) {
        match = { id: targetWardName, name: targetWardName };
        addrModalWards.value.unshift(match);
      }
      addrModalForm.wardId = String(match.id);
    }
  } catch {
    if (targetWardName) {
      const fallback = { id: targetWardName, name: targetWardName };
      addrModalWards.value = [fallback];
      addrModalForm.wardId = String(fallback.id);
    }
  }
}

async function onAddrModalProvinceChange() {
  await loadAddrModalWards(addrModalForm.provinceId, "");
}

async function openEditAddressModal(addr = null) {
  if (!provinces.value.length) {
    try {
      await loadProvinces();
    } catch {
      /* ignore */
    }
  }
  const target = addr || currentSelectedSavedAddr.value || savedAddresses.value[0] || null;
  const base = currentUser.value ? userInfo(currentUser.value) : { name: "", phone: "" };
  addrModalError.value = "";
  addrModalForm.id = target?.id || null;
  addrModalForm.name = target?.name || f.name || base.name || "";
  addrModalForm.phone = target?.phone || f.phone || base.phone || "";
  addrModalForm.street = target?.street || f.address || "";
  addrModalForm.isDefault = target ? !!target.isDefault : true;

  const targetProv = cleanPlaceName(target?.province);
  const matchedProv = targetProv
    ? provinces.value.find(
        (p) => cleanPlaceName(p.name) === targetProv || normalizePlace(p.name).includes(targetProv),
      )
    : provinces.value.find((p) => String(p.id) === String(provinceId.value));
  addrModalForm.provinceId = matchedProv ? String(matchedProv.id) : "";

  isEditingAddressInModal.value = true;
  isAddressModalOpen.value = true;

  if (addrModalForm.provinceId) {
    await loadAddrModalWards(addrModalForm.provinceId, target?.ward || selectedWard.value?.name || "");
  } else {
    addrModalWards.value = [];
    addrModalForm.wardId = "";
  }
}

async function openAddAddressModal() {
  if (!provinces.value.length) {
    try {
      await loadProvinces();
    } catch {
      /* ignore */
    }
  }
  const base = currentUser.value ? userInfo(currentUser.value) : { name: "", phone: "" };
  addrModalError.value = "";
  addrModalForm.id = null;
  addrModalForm.name = f.name || base.name || "";
  addrModalForm.phone = f.phone || base.phone || "";
  addrModalForm.provinceId = "";
  addrModalForm.wardId = "";
  addrModalForm.street = "";
  addrModalForm.isDefault = !savedAddresses.value.length;
  addrModalWards.value = [];
  isEditingAddressInModal.value = true;
  isAddressModalOpen.value = true;
}

async function saveAddrModalForm() {
  addrModalError.value = "";
  const cleanName = addrModalForm.name.trim();
  const cleanPhone = addrModalForm.phone.trim();
  const cleanStreet = addrModalForm.street.trim();

  if (!cleanName) {
    addrModalError.value = "Vui lòng nhập họ và tên người nhận.";
    return;
  }
  if (!/^0\d{9}$/.test(cleanPhone)) {
    addrModalError.value = "Số điện thoại phải gồm 10 chữ số, bắt đầu bằng số 0.";
    return;
  }
  if (!addrModalForm.provinceId) {
    addrModalError.value = "Vui lòng chọn Tỉnh / Thành phố.";
    return;
  }
  if (!addrModalForm.wardId) {
    addrModalError.value = "Vui lòng chọn Phường / Xã.";
    return;
  }
  if (!cleanStreet) {
    addrModalError.value = "Vui lòng nhập địa chỉ cụ thể (số nhà, tên đường...).";
    return;
  }

  const provObj = provinces.value.find((p) => String(p.id) === String(addrModalForm.provinceId));
  const wardObj = addrModalWards.value.find((w) => String(w.id) === String(addrModalForm.wardId));

  addrModalSaving.value = true;
  try {
    const res = await fetch("/api/auth/thong-tin", {
      method: "PUT",
      headers: { "Content-Type": "application/json", ...authHeaders() },
      body: JSON.stringify({
        idDiaChi: addrModalForm.id,
        tenDiaChi: cleanName,
        tenKhachHang: cleanName,
        soDienThoai: cleanPhone,
        tinhThanhPho: provObj?.name || "",
        phuong: wardObj?.name || "",
        diaChiCuThe: cleanStreet,
        macDinh: !!addrModalForm.isDefault,
      }),
    });
    if (!res.ok) {
      let errJson = {};
      try {
        errJson = await res.json();
      } catch {
        /* ignore non-JSON error body */
      }
      throw new Error(errJson.message || `Không thể lưu địa chỉ (mã lỗi ${res.status}).`);
    }
    const updatedProfile = await res.json();
    const editedId = addrModalForm.id;
    customer.value = { ...customer.value, ...updatedProfile, id: updatedProfile.id ?? customer.value?.id };

    await nextTick();
    let targetId = editedId;
    if (!targetId) {
      const matched = savedAddresses.value.find(
        (a) =>
          a.street.trim() === cleanStreet &&
          (!provObj?.name || normalizePlace(a.province) === normalizePlace(provObj.name)),
      );
      targetId = matched?.id || savedAddresses.value[0]?.id || "new";
    }
    if (targetId && targetId !== "new") {
      chooseAddress(targetId);
    }

    isEditingAddressInModal.value = false;
    isAddressModalOpen.value = false;
    notifySuccess(
      "Thành công",
      editedId ? "Đã cập nhật địa chỉ nhận hàng!" : "Đã thêm địa chỉ nhận hàng mới!",
    );
  } catch (e) {
    addrModalError.value = e.message || "Không thể lưu địa chỉ, vui lòng thử lại.";
  } finally {
    addrModalSaving.value = false;
  }
}

function chooseAddress(id) {
  selectedAddr.value = id;
  const u = currentUser.value;
  if (!u) return;
  const base = userInfo(u);
  if (id === "new") {
    f.name = base.name;
    f.phone = base.phone;
    f.email = base.email;
    f.address = "";
    provinceId.value = "";
    wardId.value = "";
    savedAddressToRestore.value = null;
    return;
  }
  const a = savedAddresses.value.find((x) => x.id === id);
  if (!a) return;
  f.name = a.name || base.name;
  f.phone = a.phone || base.phone;
  f.email = base.email;
  f.address = a.street;
  savedAddressToRestore.value = a;
  restoreSavedAddressRegion();
}

const selectedProvince = computed(() => provinces.value.find((p) => String(p.id) === String(provinceId.value)) || null);
const selectedWard = computed(() => wards.value.find((w) => String(w.id) === String(wardId.value)) || null);
const fullDeliveryAddress = computed(() => {
  if (currentSelectedSavedAddr.value?.address) {
    return currentSelectedSavedAddr.value.address;
  }
  return [f.address.trim(), selectedWard.value?.name, selectedProvince.value?.name].filter(Boolean).join(", ");
});

function normalizePlace(value) {
  return String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/đ/g, "d").trim().toLowerCase();
}

function cleanPlaceName(v) {
  return normalizePlace(v).replace(/^(thanh pho|tp\.?|tinh|quan|huyen|thi xa|phuong|xa|thi tran)\s+/i, "").trim();
}

function getEstimatedShippingFee(provinceName, wardName) {
  const p = normalizePlace(provinceName);
  const w = normalizePlace(wardName);
  if (p.includes('ha noi')) {
    const isInner = ['ba dinh', 'hoan kiem', 'dong da', 'hai ba trung', 'cau giay', 'thanh xuan', 'tay ho', 'bac tu liem', 'nam tu liem', 'long bien', 'bo de', 'hoang mai', 'ha dong'].some((q) => w.includes(q));
    const isOuterFar = ['ba vi', 'son tay', 'ung hoa', 'my duc', 'chuong my', 'thuong tin', 'phu xuyen', 'quoc oai', 'thach that', 'phuc tho'].some((q) => w.includes(q));
    if (isInner) return 22000;
    if (isOuterFar) return 28000;
    return 25000;
  }
  if (['bac ninh', 'hung yen', 'ha nam', 'vinh phuc'].some((k) => p.includes(k))) return 28000;
  if (['hai phong', 'hai duong', 'quang ninh', 'nam dinh', 'ninh binh', 'thai binh', 'phu tho', 'thai nguyen'].some((k) => p.includes(k))) return 32000;
  if (['hoa binh', 'bac kan', 'lang son', 'tuyen quang', 'yen bai', 'lao cai', 'cao bang', 'ha giang'].some((k) => p.includes(k))) return 35000;
  if (['son la', 'dien bien', 'lai chau'].some((k) => p.includes(k))) return 38000;
  if (['thanh hoa', 'nghe an', 'ha tinh'].some((k) => p.includes(k))) return 35000;
  if (p.includes('da nang')) return 36000;
  if (['quang binh', 'quang tri', 'hue', 'thua thien', 'quang nam', 'quang ngai', 'binh dinh', 'phu yen', 'khanh hoa', 'ninh thuan', 'binh thuan'].some((k) => p.includes(k))) return 38000;
  if (['kon tum', 'gia lai', 'dak lak', 'dak nong', 'lam dong'].some((k) => p.includes(k))) return 42000;
  if (['ho chi minh', 'sai gon', 'tphcm'].some((k) => p.includes(k))) return 38000;
  if (['binh duong', 'dong nai', 'ba ria', 'vung tau', 'tay ninh', 'binh phuoc'].some((k) => p.includes(k))) return 40000;
  if (['long an', 'tien giang', 'ben tre', 'vinh long', 'tra vinh', 'can tho'].some((k) => p.includes(k))) return 45000;
  if (['dong thap', 'an giang', 'hau giang', 'soc trang', 'bac lieu'].some((k) => p.includes(k))) return 48000;
  if (['kien giang', 'ca mau', 'phu quoc'].some((k) => p.includes(k))) return 52000;
  return 35000;
}

function restoreSavedAddressRegion() {
  const address = savedAddressToRestore.value;
  if (!address || !provinces.value.length) return;
  const targetProv = cleanPlaceName(address.province);
  const province = targetProv
    ? provinces.value.find((p) => cleanPlaceName(p.name) === targetProv || normalizePlace(p.name).includes(targetProv))
    : null;
  if (province) provinceId.value = String(province.id);
  savedAddressToRestore.value = null;
  if (province && address.ward) savedWardToRestore.value = address.ward;
}

const savedWardToRestore = ref("");

async function loadProvinces() {
  provinces.value = await getProvinces();
  restoreSavedAddressRegion();
}

watch(provinceId, async (id) => {
  wards.value = [];
  wardId.value = "";
  shippingError.value = "";
  if (!id) return;
  try {
    const provObj = provinces.value.find((p) => String(p.id) === String(id));
    wards.value = await getWards(Number(id), provObj?.name || "");
    if (savedWardToRestore.value) {
      const savedWard = savedWardToRestore.value;
      const savedNorm = normalizePlace(savedWard);
      const savedClean = cleanPlaceName(savedWard);
      const match = wards.value.find(
        (w) =>
          normalizePlace(w.name) === savedNorm ||
          normalizePlace(w.name).startsWith(`${savedNorm} `) ||
          cleanPlaceName(w.name) === savedClean,
      );
      if (match) wardId.value = String(match.id);
      savedWardToRestore.value = "";
    }
  } catch {
    shippingError.value = "Không tải được danh sách phường/xã.";
  }
});

watch([provinceId, wardId, serviceTypeId, currentSelectedSavedAddr], async ([province, ward, , savedAddr]) => {
  const requestId = ++shippingRequestId;
  const provName = selectedProvince.value?.name || savedAddr?.province || "";
  const wardName = selectedWard.value?.name || savedAddr?.ward || "";
  if (!provName || !wardName) {
    shippingFee.value = provName
      ? getEstimatedShippingFee(provName, '')
      : SHIP_FEE;
    toProvinceId.value = selectedProvince.value ? (Number(selectedProvince.value.id) || 201) : null;
    toDistrictId.value = null;
    toWardCode.value = "";
    shippingLoading.value = false;
    return;
  }
  shippingLoading.value = true;
  shippingError.value = "";
  try {
    const result = await tinhPhi({
      toProvinceName: provName,
      toWardName: wardName,
      serviceTypeId: serviceTypeId.value || undefined,
      items: cart.items.map((i) => ({
        idSanPhamChiTiet: i.idSanPhamChiTiet ?? i.variantId ?? i.spctId ?? i.id,
        soLuong: i.qty || 1,
      })),
    });
    if (requestId !== shippingRequestId) return;
    shippingFee.value = Number(result.shippingFee || getEstimatedShippingFee(provName, wardName));
    toProvinceId.value = Number(result.toProvinceId || selectedProvince.value?.id) || 201;
    toDistrictId.value = Number(result.toDistrictId || 0) || 1;
    toWardCode.value = String(result.toWardCode || selectedWard.value?.id || "001");
    if (result.toDistrictId) {
      try {
        const services = await getServices(result.toDistrictId);
        if (requestId !== shippingRequestId) return;
        if (!serviceTypeId.value && services.length) serviceTypeId.value = services[0].serviceTypeId;
      } catch {
        /* ignore service fetch error */
      }
    }
  } catch (error) {
    if (requestId !== shippingRequestId) return;
    console.warn("[Checkout] Không tính được phí GHN tự động, dùng phí dự phòng:", error.message);
    shippingError.value = "";
    shippingFee.value = getEstimatedShippingFee(provName, wardName);
    toProvinceId.value = Number(selectedProvince.value?.id) || 201;
    toDistrictId.value = 1;
    toWardCode.value = "001";
  } finally {
    if (requestId === shippingRequestId) shippingLoading.value = false;
  }
});

/* ---------- Khóa ô nhập khi đã tự điền ---------- */
// Họ tên, SĐT: khóa nếu thông tin lấy được từ tài khoản (SĐT trống thì vẫn cho nhập)
const lockName = computed(
  () => isLoggedIn.value && !!userInfo(currentUser.value).name,
);
const lockPhone = computed(
  () => isLoggedIn.value && !!userInfo(currentUser.value).phone,
);
// Địa chỉ: khóa khi đang dùng địa chỉ đã lưu, chỉ nhập được khi chọn "Nhập địa chỉ mới"
const lockAddress = computed(
  () => isLoggedIn.value && selectedAddr.value !== "new"
    && !!savedAddresses.value.find((a) => a.id === selectedAddr.value)?.street,
);

// Chạy khi dữ liệu khách hàng tải xong
watch(
  currentUser,
  (u) => {
    if (!u) return;
    const def = savedAddresses.value[0]; // đã sort: mặc định đứng đầu
    chooseAddress(def ? def.id : "new");
  },
  { immediate: true },
);

/* ---------- Voucher ---------- */
const voucher = ref(null); // { code, ...rule }
const voucherMsg = reactive({ text: "", ok: false });
const manualRemoved = ref(false); // khách chủ động gỡ mã -> không tự áp lại

// Tổng trước giảm = tiền hàng + phí vận chuyển (khớp cách tính của back-end)
const freeShipThreshold = ref(10000000);
const isFreeShip = computed(() => freeShipThreshold.value > 0 && Number(cartTotal.value || 0) >= freeShipThreshold.value);
const actualShippingFee = computed(() => (isFreeShip.value ? 0 : Number(shippingFee.value || 0)));
const orderTotal = computed(() => cartTotal.value + actualShippingFee.value);
const voucherIsAvailable = (v) => {
  const now = new Date();
  const end = v.ngayKetThuc ? new Date(v.ngayKetThuc) : null;
  return v.trangThai !== false
    && (!v.soLuong || Number(v.soLuongDaSuDung || 0) < Number(v.soLuong))
    && (!v.ngayBatDau || now >= new Date(v.ngayBatDau))
    && (!end || now <= end);
};
const voucherSuggestion = computed(() => availableVouchers.value
  .filter((v) => !checkVoucher(v, true) && Number(v.hoaDonToiThieu || 0) > orderTotal.value)
  .sort((a, b) => Number(a.hoaDonToiThieu) - Number(b.hoaDonToiThieu))[0] || null);

function formatVoucherValue(v) {
  return /PERCENT|PHAN_TRAM|%/i.test(String(v.loaiGiamGia || ""))
    ? `Giảm ${v.giaTriGiam}%`
    : `Giảm ${money(v.giaTriGiam || 0)}`;
}

const discount = computed(() => calcDiscount(voucher.value, orderTotal.value));
const finalTotal = computed(() => orderTotal.value - discount.value);

const applying = ref(false);
async function applyVoucher() {
  const selected = availableVouchers.value.find((v) => String(v.id) === String(selectedVoucherId.value));
  const code = String(voucherCode.value || VF.code(selected) || "").trim().toUpperCase();
  if (!code) {
    voucherMsg.text = "Vui lòng chọn voucher.";
    voucherMsg.ok = false;
    return;
  }
  applying.value = true;
  const r = await fetchVoucher(code);
  applying.value = false;
  if (!r.ok) {
    voucher.value = null;
    voucherMsg.text = r.message;
    voucherMsg.ok = false;
    return;
  }
  manualRemoved.value = false;
  voucher.value = r.voucher;
  sessionStorage.setItem("ps_selected_voucher_code", r.voucher.code);
  voucherMsg.text = "Áp dụng mã thành công!";
  voucherMsg.ok = true;
}
function removeVoucher() {
  voucher.value = null;
  selectedVoucherId.value = "";
  voucherMsg.text = "";
}
// Khách bấm gỡ mã
function onRemoveClick() {
  manualRemoved.value = true;
  sessionStorage.removeItem("ps_selected_voucher_code");
  removeVoucher();
}
// Giỏ thay đổi làm không còn đủ điều kiện -> tự động tìm mã tốt nhất
watch(orderTotal, () => {
  if (voucher.value && orderTotal.value < (voucher.value.min || 0)) {
    const oldCode = voucher.value.code;
    removeVoucher();

    // Tự động tìm voucher tốt nhất còn đủ điều kiện
    const eligible = availableVouchers.value
      .filter((v) => !checkVoucher(v))
      .map((v) => ({ v, d: calcDiscount(toVoucher(v), orderTotal.value) }))
      .filter((x) => x.d > 0)
      .sort((a, b) => b.d - a.d);

    if (eligible.length > 0) {
      const best = eligible[0].v;
      voucher.value = toVoucher(best);
      selectedVoucherId.value = VF.id(best);
      voucherMsg.text = `Mã "${oldCode}" không đủ điều kiện, đã tự động chuyển sang mã tốt nhất "${VF.code(best)}".`;
      voucherMsg.ok = true;
    } else {
      voucherMsg.text = `Mã "${oldCode}" đã được gỡ vì đơn hàng không đủ điều kiện tối thiểu.`;
      voucherMsg.ok = false;
    }
  }
});

/* ---------- Danh sách phiếu gợi ý theo giá trị đơn hàng ---------- */
const allVouchers = ref([]); // voucher công khai đang còn hiệu lực
const loadingVouchers = ref(false);

async function loadVouchers() {
  loadingVouchers.value = true;
  try {
    const [listRes, usageRes] = await Promise.allSettled([
      listVouchers(),
      customer.value?.id ? suDungCuaToi() : Promise.resolve([]),
    ]);
    const list = listRes.status === "fulfilled" && Array.isArray(listRes.value) ? listRes.value : [];
    const usageList = usageRes.status === "fulfilled" && Array.isArray(usageRes.value) ? usageRes.value : [];
    const map = {};
    for (const u of usageList) {
      if (u && u.id != null) {
        map[u.id] = {
          daSuDung: Number(u.daSuDung || 0),
          gioiHanMoiTaiKhoan: Number(u.gioiHanMoiTaiKhoan || 1),
        };
      }
    }
    myVoucherUsage.value = map;
    availableVouchers.value = list;
    allVouchers.value = list;
  } catch (e) {
    console.warn("[Checkout] Không tải được danh sách phiếu:", e.message);
    availableVouchers.value = [];
    allVouchers.value = [];
  } finally {
    loadingVouchers.value = false;
  }
}

// Danh sách tất cả phiếu giảm giá kèm kiểm tra cái nào dùng được / không dùng được
const voucherOptions = computed(() => {
  const total = orderTotal.value;
  const cid = customer.value?.id ? Number(customer.value.id) : null;
  const list = allVouchers.value
    .filter((d) => {
      // Với phiếu cá nhân, chỉ hiển thị nếu được cấp cho khách hàng hiện tại
      if (isPersonal(VF.scope(d))) {
        if (!cid) return false;
        const assigned = VF.customerIds(d).includes(cid) || VF.usedCustomerIds(d).includes(cid);
        if (!assigned) return false;
      }
      return true;
    })
    .map((d) => {
      const v = toVoucher(d);
      const reason = checkVoucher(d, false);
      const disc = calcDiscount(v, total);
      const eligible = !reason && disc > 0;
      const end = VF.end(d);
      const left = VF.qty(d);
      return {
        v,
        raw: d,
        eligible,
        reason: reason || (disc <= 0 ? "Không đủ điều kiện giảm giá." : ""),
        discount: eligible ? disc : 0,
        missing: Math.max(0, (v.min || 0) - total),
        expire: end ? new Date(end).toLocaleDateString("vi-VN") : "",
        left: left !== undefined && left !== null ? Number(left) : null,
        best: false,
      };
    });
  // Phiếu dùng được lên trước (giảm nhiều nhất lên đầu), phiếu không dùng được xuống dưới
  list.sort(
    (x, y) =>
      Number(y.eligible) - Number(x.eligible) ||
      y.discount - x.discount ||
      (x.v.min || 0) - (y.v.min || 0),
  );
  const best = list.find((o) => o.eligible && o.discount > 0);
  if (best) best.best = true;
  return list;
});

const usableVoucherCount = computed(() => voucherOptions.value.filter((o) => o.eligible).length);
const bestVoucher = computed(() => voucherOptions.value.find((o) => o.best && o.eligible) || null);

// Tự động áp dụng duy nhất voucher tốt nhất VÀ dùng được; nếu không dùng được thì tuyệt đối không tự áp dụng.
watch([bestVoucher, orderTotal, customer, myVoucherUsage], ([best]) => {
  if (voucher.value) {
    const raw = allVouchers.value.find((x) => VF.id(x) === voucher.value.id);
    const err = raw ? checkVoucher(raw, false) : "Mã giảm giá không còn khả dụng.";
    if (err || calcDiscount(voucher.value, orderTotal.value) <= 0) {
      voucher.value = null;
      selectedVoucherId.value = "";
    }
  }
  if (manualRemoved.value) return;

  // Nếu khách hàng đã chọn mã từ giỏ hàng hoặc phiên trước (sessionStorage)
  const savedCode = sessionStorage.getItem("ps_selected_voucher_code");
  if (savedCode && !manualRemoved.value) {
    const found = allVouchers.value.find((v) => String(VF.code(v)).toUpperCase() === savedCode.toUpperCase());
    if (found && !checkVoucher(found, false) && calcDiscount(toVoucher(found), orderTotal.value) > 0) {
      const v = toVoucher(found);
      voucher.value = v;
      selectedVoucherId.value = String(v.id);
      voucherMsg.text = `Đang áp dụng mã: ${v.code}.`;
      voucherMsg.ok = true;
      return;
    }
  }

  const current = voucher.value;
  const next = best && best.eligible ? best.v : null;
  if (current?.id === next?.id) return;
  voucher.value = next;
  if (next) {
    voucherCode.value = "";
    selectedVoucherId.value = String(next.id);
    voucherMsg.text = `Tự động áp dụng mã tốt nhất dùng được: ${next.code}.`;
    voucherMsg.ok = true;
  } else {
    selectedVoucherId.value = "";
    if (current) {
      voucherMsg.text = "Không có mã giảm giá nào dùng được cho đơn hàng này.";
      voucherMsg.ok = false;
    } else {
      voucherMsg.text = "";
    }
  }
}, { immediate: true });

// Voucher đang áp dụng mà bị hết lượt (sau khi tải lại danh sách) -> gỡ
watch(allVouchers, () => {
  if (!voucher.value) return;
  const d = allVouchers.value.find((x) => VF.id(x) === voucher.value.id);
  if (d && isExhausted(d)) {
    removeVoucher();
    voucherMsg.text = "Mã giảm giá đã hết lượt sử dụng nên đã được gỡ.";
    voucherMsg.ok = false;
  }
});

const voucherLabel = (v) =>
  v.type === "percent"
    ? `Giảm ${v.value}%${v.max > 0 ? ` (tối đa ${money(v.max)})` : ""}`
    : `Giảm ${money(v.value)}`;

// Bấm vào phiếu: chọn / bỏ chọn
function toggleVoucher(o) {
  if (!o.eligible) return;
  if (voucher.value?.id === o.v.id) {
    onRemoveClick();
    return;
  }
  manualRemoved.value = false;
  voucher.value = o.v;
  voucherCode.value = "";
  selectedVoucherId.value = String(o.v.id);
  sessionStorage.setItem("ps_selected_voucher_code", o.v.code);
  voucherMsg.text = "Áp dụng mã thành công!";
  voucherMsg.ok = true;
}

/* ---------- Đặt hàng: lưu vào hóa đơn (POST /api/hoa-don) ---------- */
const ID_NHAN_VIEN_ONLINE = null; // null = không gán nhân viên (đơn online)
const orderCode = ref("");
const submitting = ref(false);
const submitError = ref("");

// Sinh mã dạng HD + yyMMddHHmmss + 3 số ngẫu nhiên, ví dụ HD261024153045123
function genMaHoaDon() {
  const d = new Date();
  const p = (n, l = 2) => String(n).padStart(l, "0");
  const stamp =
    String(d.getFullYear()).slice(2) +
    p(d.getMonth() + 1) +
    p(d.getDate()) +
    p(d.getHours()) +
    p(d.getMinutes()) +
    p(d.getSeconds());
  return `HD${stamp}${p(Math.floor(Math.random() * 1000), 3)}`;
}

/* ---------- Phương thức thanh toán: id cố định theo bảng phuong_thuc_thanh_toan ---------- */
// 11 = COD (Thanh toán khi nhận hàng), 2 = Chuyển khoản ngân hàng, 4 = VNPay QR (id 4 trong hinh_thuc_thanh_toan)
const PAY_METHOD_ID = { cod: 11, bank: 2, vnpay: 4 };

const phuongThucFields = (kind) => ({
  idPhuongThucThanhToan: PAY_METHOD_ID[kind],
});

// Các field khớp HoaDonRequest của back-end. Tiền hàng, giảm giá, tổng tiền
// do back-end tự tính lại từ chi tiết + phiếu giảm giá nên không gửi lên.
function buildHoaDonRequest() {
  return {
    maHoaDon: genMaHoaDon(),
    loaiDon: "ONLINE",
    trangThai: "CHO_XAC_NHAN",

    // Khách hàng / thông tin nhận hàng
    idKhachHang: customer.value?.id ?? null,
    tenKhachHang: f.name.trim(),
    soDienThoaiKhachHang: f.phone.trim(),
    email: customer.value?.email || f.email?.trim() || null,
    diaChiNhanHang: fullDeliveryAddress.value,
    ghiChu: f.note.trim(),

    // Đơn online không có nhân viên (cần back-end cho phép null)
    idNhanVien: ID_NHAN_VIEN_ONLINE,

    ...phuongThucFields(f.pay),
    idPhieuGiamGia: voucher.value?.id ?? null,
    phiVanChuyen: actualShippingFee.value,
    toProvinceId: toProvinceId.value,
    toDistrictId: toDistrictId.value,
    toWardCode: toWardCode.value,

    chiTiet: cart.items.map((i) => ({
      idSanPhamChiTiet: i.idSanPhamChiTiet ?? i.variantId ?? i.spctId ?? i.id,
      soLuong: i.qty,
      donGia: i.price,
    })),
  };
}

async function clearCart() {
  if (typeof store.clearCart === "function") {
    try {
      await store.clearCart();
    } catch (e) {
      console.warn("[Checkout] clearCart server warn:", e);
    }
  }
  if (Array.isArray(cart.items)) cart.items.splice(0);
  if (typeof store.resetCartLocal === "function") {
    store.resetCartLocal();
  }
}

async function submit() {
  submitError.value = "";
  errors.name = f.name.trim() ? "" : "Vui lòng nhập họ và tên.";
  errors.phone = /^0\d{9}$/.test(f.phone)
    ? ""
    : "Số điện thoại gồm 10 số, bắt đầu bằng 0.";
  if (!f.email || !f.email.trim()) {
    errors.email = "Vui lòng nhập địa chỉ email nhận thông báo đơn hàng.";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email.trim())) {
    errors.email = "Địa chỉ email không đúng định dạng (VD: nguyenvan@gmail.com).";
  } else {
    errors.email = "";
  }
  errors.address = currentSelectedSavedAddr.value?.address || (f.address.trim() && provinceId.value && wardId.value)
    ? ""
    : "Vui lòng nhập địa chỉ cụ thể và chọn tỉnh/thành phố, phường/xã.";
  if (errors.name || errors.phone || errors.email || errors.address) {
    submitError.value = errors.name || errors.phone || errors.email || errors.address;
    return;
  }
  if (shippingLoading.value) {
    submitError.value = "Đang tính phí vận chuyển, vui lòng đợi trong giây lát.";
    return;
  }
  if (!toProvinceId.value) toProvinceId.value = Number(selectedProvince.value?.id) || 201;
  if (!toDistrictId.value || toDistrictId.value === 1) {
    toDistrictId.value = 1;
    toWardCode.value = "001";
  }

  submitting.value = true;

  // ====== VNPAY: Tạo đơn CHO_THANH_TOAN + mở QR quét thanh toán ======
  if (f.pay === "vnpay") {
    const payload = buildHoaDonRequest();
    // Ghi đè trangThai, BE sẽ set CHO_THANH_TOAN
    payload.trangThai = "CHO_THANH_TOAN";
    console.log("[Checkout] VNPAY payload:", payload);
    try {
      const res = await fetch(
        "/api/vnpay/create-payment",
        {
          method: "POST",
          headers: { "Content-Type": "application/json", ...authHeaders() },
          body: JSON.stringify(payload),
        },
      );

      if (!res.ok) {
        let errJson = {};
        try {
          errJson = await res.json();
        } catch { /* Response có thể không phải JSON. */ }
        throw new Error(
          errJson.message || `Lỗi tạo thanh toán VNPAY (mã lỗi ${res.status}).`,
        );
      }

      const payData = await res.json();
      if (payData && (payData.qrUrl || payData.paymentUrl)) {
        submitting.value = false;
        openQrModal(payData);
        return;
      } else {
        throw new Error("Không nhận được liên kết thanh toán từ VNPAY.");
      }
    } catch (e) {
      console.error("[Checkout] Lỗi VNPAY:", e);
      submitError.value =
        e.message === "Failed to fetch"
          ? "Không kết nối được máy chủ, vui lòng thử lại."
          : e.message;
      submitting.value = false;
      return;
    }
  }

  // ====== COD / Bank: flow bình thường ======
  try {
    const confirmed = await notifyConfirm(
      "Xác nhận đặt hàng",
      `Xác nhận đơn hàng với tổng thanh toán ${money(finalTotal.value)}?`,
      "Xác nhận đặt hàng",
    );
    if (!confirmed) return;
    const payload = buildHoaDonRequest();
    console.log("[Checkout] payload gửi hóa đơn:", payload);
    const hd = await createInvoice(payload); // HoaDonDTO
    orderId.value = hd.id;
    orderCode.value = hd.maHoaDon ?? hd.ma ?? hd.id;

    // Lưu vào danh sách đơn gần đây để tra cứu
    try {
      const orders = JSON.parse(localStorage.getItem("ps_orders") || "[]");
      orders.unshift({
        id: orderCode.value,
        orderId: hd.id,
        info: {
          name: f.name,
          phone: f.phone,
          address: fullDeliveryAddress.value,
          note: f.note,
          pay: f.pay,
        },
        items: JSON.parse(JSON.stringify(cart.items)),
        total: finalTotal.value,
        date: new Date().toLocaleString("vi-VN"),
      });
      localStorage.setItem("ps_orders", JSON.stringify(orders.slice(0, 20)));
    } catch { /* Lưu lịch sử đơn hàng ở localStorage là tùy chọn. */ }

    done.value = true;
    await clearCart();
  } catch (e) {
    console.error("[Checkout] Lỗi tạo hóa đơn:", e);
    submitError.value = e.response?.data?.message
      || (e.message === "Network Error" ? "Không kết nối được máy chủ, vui lòng thử lại." : e.message);
  } finally {
    submitting.value = false;
  }
}

// Sao chép mã đơn
const copied = ref(false);
async function copyOrderCode() {
  try {
    await navigator.clipboard.writeText(String(orderCode.value));
    copied.value = true;
    setTimeout(() => (copied.value = false), 1800);
  } catch (e) {
    /* bỏ qua */
  }
}

// ====== Quét mã QR thanh toán (VietQR / VNPAY) ======
const isQrModalOpen = ref(false);
const qrModalData = ref({
  orderId: null,
  maHoaDon: '',
  amount: 0,
  qrUrl: '',
  paymentUrl: '',
  accountNo: '0922010408',
  accountName: 'POLYSHOES STORE',
  bankName: 'MB Bank (Ngân hàng Quân Đội)'
});
const qrConfirming = ref(false);
const autoCountdown = ref(10);
let autoCountdownTimer = null;
const isAutoConfirmEnabled = ref(false);
const copyToast = ref('');

function showCopyToast(msg) {
  copyToast.value = msg;
  setTimeout(() => (copyToast.value = ''), 2000);
}

function copyText(val, label) {
  if (!val) return;
  navigator.clipboard.writeText(String(val)).then(() => {
    showCopyToast(`Đã sao chép ${label}!`);
  }).catch(() => { });
}

const playSuccessChime = () => {
  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const playNote = (freq, start, dur) => {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(freq, start);
      gain.gain.setValueAtTime(0, start);
      gain.gain.linearRampToValueAtTime(0.3, start + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + dur);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(start);
      osc.stop(start + dur);
    };
    const now = ctx.currentTime;
    playNote(523.25, now, 0.15); // C5
    playNote(659.25, now + 0.12, 0.15); // E5
    playNote(783.99, now + 0.24, 0.3); // G5
  } catch { }
};

const startAutoCountdown = () => {
  stopAutoCountdown();
  if (!isAutoConfirmEnabled.value) return;
  autoCountdown.value = 10;
  autoCountdownTimer = setInterval(() => {
    if (autoCountdown.value > 1) {
      autoCountdown.value--;
    } else {
      stopAutoCountdown();
      confirmQrPayment();
    }
  }, 1000);
};

const stopAutoCountdown = () => {
  if (autoCountdownTimer) {
    clearInterval(autoCountdownTimer);
    autoCountdownTimer = null;
  }
};

const toggleAutoConfirm = () => {
  isAutoConfirmEnabled.value = !isAutoConfirmEnabled.value;
  if (isAutoConfirmEnabled.value) {
    startAutoCountdown();
  } else {
    stopAutoCountdown();
  }
};

let qrPollTimer = null;

const stopQrPaymentPolling = () => {
  if (qrPollTimer) {
    clearInterval(qrPollTimer);
    qrPollTimer = null;
  }
};

const finishQrOrderSuccess = async (result) => {
  stopQrPaymentPolling();
  stopAutoCountdown();
  playSuccessChime();
  isQrModalOpen.value = false;

  orderId.value = qrModalData.value.orderId;
  orderCode.value = qrModalData.value.maHoaDon;

  try {
    const orders = JSON.parse(localStorage.getItem('ps_orders') || '[]');
    orders.unshift({
      id: orderCode.value,
      orderId: orderId.value,
      info: {
        name: f.name,
        phone: f.phone,
        address: fullDeliveryAddress.value,
        note: f.note,
        pay: f.pay,
      },
      items: JSON.parse(JSON.stringify(cart.items)),
      total: qrModalData.value.amount || finalTotal.value,
      date: new Date().toLocaleString('vi-VN'),
    });
    localStorage.setItem('ps_orders', JSON.stringify(orders.slice(0, 20)));
  } catch { }

  done.value = true;
  await clearCart();

  if (result?.outOfStock) {
    notifyWarning('Sự cố tồn kho', result.message || 'Sản phẩm đã hết hàng trong kho. Đơn chuyển sang Chờ hoàn tiền!');
  } else {
    notifySuccess('Thanh toán thành công', 'Đơn hàng của bạn đã được thanh toán và chuyển sang Chờ xác nhận!');
  }
};

const startQrPaymentPolling = () => {
  stopQrPaymentPolling();
  if (!qrModalData.value.orderId) return;
  qrPollTimer = setInterval(async () => {
    if (!isQrModalOpen.value || qrConfirming.value || !qrModalData.value.orderId) return;
    try {
      const res = await fetch(`/api/vnpay/trang-thai/${qrModalData.value.orderId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...authHeaders() }
      });
      if (!res.ok) return;
      const data = await res.json();
      if (data?.paid) {
        await finishQrOrderSuccess(data);
      }
    } catch {
      // Bỏ qua lỗi mạng tạm thời khi polling
    }
  }, 2500);
};

const openQrModal = (payData) => {
  qrModalData.value = {
    orderId: payData.orderId,
    maHoaDon: payData.maHoaDon,
    amount: payData.amount,
    qrUrl: payData.qrUrl,
    paymentUrl: payData.paymentUrl,
    accountNo: '0922010408',
    accountName: 'POLYSHOES STORE',
    bankName: 'MB Bank (Ngân hàng Quân Đội)'
  };
  isQrModalOpen.value = true;
  startQrPaymentPolling();
};

const closeQrModal = () => {
  stopQrPaymentPolling();
  stopAutoCountdown();
  isQrModalOpen.value = false;
};

const confirmQrPayment = async () => {
  if (qrConfirming.value || !qrModalData.value.orderId) return;
  stopQrPaymentPolling();
  stopAutoCountdown();
  qrConfirming.value = true;

  try {
    const res = await fetch(`/api/vnpay/xac-nhan-qr/${qrModalData.value.orderId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() }
    });
    const result = await res.json();
    await finishQrOrderSuccess(result);
  } catch (err) {
    console.error('Lỗi xác nhận thanh toán QR:', err);
    notifyError('Lỗi xác nhận', err.message || 'Không thể xác nhận thanh toán!');
  } finally {
    qrConfirming.value = false;
  }
};
</script>

<template>
  <!-- ===== Đặt hàng thành công ===== -->
  <div v-if="done" class="co-success">
    <div class="co-success-ico"><i class="bi bi-check-lg"></i></div>
    <h3 class="fw-bold mb-2">Đặt hàng thành công!</h3>
    <p class="text-muted-ps mb-3">Cảm ơn bạn đã mua sắm tại Poly Shoes.</p>

    <div class="co-success-code">
      <span class="small text-muted-ps">Mã đơn hàng</span>
      <strong>#{{ orderCode }}</strong>
      <button type="button" class="co-copy" @click="copyOrderCode">
        <i class="bi" :class="copied ? 'bi-check2' : 'bi-copy'"></i>
      </button>
    </div>

    <p class="text-muted-ps mb-4">
      Poly Shoes sẽ gọi xác nhận đơn qua số <strong>{{ f.phone }}</strong> trong
      ít phút.
    </p>
    <div class="d-flex justify-content-center flex-wrap gap-2">
      <RouterLink :to="`/don-hang/${orderId}`" class="btn btn-outline-primary rounded-pill px-4">
        Chi tiết đơn hàng
      </RouterLink>
      <RouterLink to="/cua-hang" class="btn btn-primary rounded-pill px-4">
        Tiếp tục mua sắm
      </RouterLink>
    </div>
  </div>

  <!-- ===== Giỏ trống ===== -->
  <div v-else-if="!cart.items.length" class="co-success">
    <div class="co-success-ico empty"><i class="bi bi-bag-x"></i></div>
    <h5 class="fw-bold">Giỏ hàng đang trống</h5>
    <p class="text-muted-ps">
      Hãy thêm vài đôi giày bạn thích trước khi thanh toán nhé.
    </p>
    <RouterLink to="/cua-hang" class="btn btn-primary rounded-pill px-4 mt-1">Xem sản phẩm</RouterLink>
  </div>

  <!-- ===== Trang thanh toán ===== -->
  <div v-else class="co-page">
    <!-- Breadcrumb -->
    <nav class="small mb-3 text-muted-ps">
      <RouterLink to="/trang-chu" class="text-muted-ps text-decoration-none">Trang chủ</RouterLink>
      <span class="mx-1">/</span>
      <RouterLink to="/gio-hang" class="text-muted-ps text-decoration-none">Giỏ hàng</RouterLink>
      <span class="mx-1">/</span>
      <span class="text-dark fw-semibold">Thanh toán</span>
    </nav>

    <!-- Tiến trình -->
    <div class="co-steps mb-4">
      <div class="co-step done">
        <span class="co-step-dot"><i class="bi bi-check-lg"></i></span>
        <span class="co-step-name">Giỏ hàng</span>
      </div>
      <span class="co-step-line done"></span>
      <div class="co-step current">
        <span class="co-step-dot">2</span>
        <span class="co-step-name">Thanh toán</span>
      </div>
      <span class="co-step-line"></span>
      <div class="co-step">
        <span class="co-step-dot">3</span>
        <span class="co-step-name">Hoàn tất</span>
      </div>
    </div>

    <div class="row g-4">
      <!-- ================= CỘT TRÁI ================= -->
      <div class="col-lg-7">
        <!-- Gợi ý đăng nhập -->
        <div v-if="!isLoggedIn" class="co-login mb-3">
          <div class="d-flex align-items-center gap-2 small">
            <i class="bi bi-person-circle fs-5"></i>
            <span>Đã có tài khoản? Đăng nhập để tự điền thông tin và nhận mã giảm
              giá.</span>
          </div>
          <RouterLink :to="{ path: '/khach-hang', query: { redirect: '/thanh-toan' } }" class="btn btn-sm btn-dark rounded-pill px-3 fw-bold">
            Đăng nhập
          </RouterLink>
        </div>

        <!-- Thông tin giao hàng -->
        <section class="co-card mb-3">
          <h5 class="co-title">
            <span class="co-title-ico"><i class="bi bi-geo-alt-fill"></i></span>Thông tin giao hàng
          </h5>

          <!-- Địa chỉ đã lưu (Mặc định 1 địa chỉ + Popup chọn địa chỉ khác) -->
          <div v-if="isLoggedIn && savedAddresses.length" class="mb-4">
            <div class="d-flex align-items-center justify-content-between mb-2 flex-wrap gap-2">
              <label class="co-label mb-0">
                <i class="bi bi-geo-alt-fill text-primary me-1"></i>Địa chỉ nhận hàng mặc định
              </label>
              <div class="d-flex align-items-center gap-2">
                <button v-if="currentSelectedSavedAddr" type="button"
                  class="btn btn-sm btn-outline-primary rounded-pill px-3 py-1 fw-bold"
                  @click="openEditAddressModal(currentSelectedSavedAddr)">
                  <i class="bi bi-pencil-square me-1"></i>Sửa địa chỉ hiện tại
                </button>
                <button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-3 py-1 fw-bold"
                  @click="openSelectAddressListModal">
                  <i class="bi bi-arrow-repeat me-1"></i>Thay đổi
                </button>
              </div>
            </div>

            <div class="co-selected-addr-box p-3 border rounded-3 bg-light d-flex align-items-center justify-content-between cursor-pointer"
              style="cursor: pointer; transition: all 0.2s ease;"
              @click="openSelectAddressListModal"
              title="Bấm để chọn địa chỉ khác">
              <div class="min-w-0 me-3">
                <div class="fw-bold text-dark small mb-1 d-flex align-items-center gap-2">
                  <span>{{ currentSelectedSavedAddr?.name || userInfo(currentUser).name }}</span>
                  <span>·</span>
                  <span>{{ currentSelectedSavedAddr?.phone || userInfo(currentUser).phone }}</span>
                  <span v-if="currentSelectedSavedAddr?.isDefault" class="badge bg-primary">Mặc định</span>
                  <span v-else-if="selectedAddr === 'new'" class="badge bg-secondary">Địa chỉ mới</span>
                  <span v-else class="badge bg-info text-dark">Đã lưu</span>
                </div>
                <div class="text-muted small text-truncate">
                  {{ currentSelectedSavedAddr ? currentSelectedSavedAddr.address : 'Đang sử dụng địa chỉ mới tự nhập bên dưới' }}
                </div>
              </div>
              <span class="text-primary small fw-semibold flex-shrink-0">
                Chọn địa chỉ khác <i class="bi bi-chevron-right ms-1"></i>
              </span>
            </div>
          </div>

          <div class="row g-3">
            <template v-if="!currentSelectedSavedAddr">
              <div class="col-md-6">
                <label class="co-label">Họ và tên <span class="text-danger">*</span></label>
                <input v-model="f.name" class="form-control co-input" :readonly="lockName"
                  :class="{ 'is-invalid': errors.name, 'bg-light': lockName }" placeholder="Nhập họ tên của bạn" />
                <div class="invalid-feedback">{{ errors.name }}</div>
              </div>
              <div class="col-md-6">
                <label class="co-label">Số điện thoại <span class="text-danger">*</span></label>
                <input v-model="f.phone" class="form-control co-input" :readonly="lockPhone"
                  :class="{ 'is-invalid': errors.phone, 'bg-light': lockPhone }" placeholder="Nhập số điện thoại" />
                <div class="invalid-feedback">{{ errors.phone }}</div>
              </div>
            </template>
            <div class="col-12">
              <label class="co-label">Email nhận thông báo đơn hàng <span class="text-danger">*</span></label>
              <input v-model="f.email" type="email" class="form-control co-input"
                :class="{ 'is-invalid': errors.email }"
                placeholder="Nhập địa chỉ email để nhận hóa đơn (ví dụ: nguyenvan@gmail.com)" />
              <div class="invalid-feedback">{{ errors.email }}</div>
            </div>
            <template v-if="!currentSelectedSavedAddr">
              <div class="col-md-6">
                <label class="co-label">Tỉnh / Thành phố <span class="text-danger">*</span></label>
                <select v-model="provinceId" class="form-select co-input" :disabled="!provinces.length">
                  <option value="">Chọn tỉnh / thành phố</option>
                  <option v-for="p in provinces" :key="p.id" :value="String(p.id)">{{ p.name }}</option>
                </select>
              </div>
              <div class="col-md-6">
                <label class="co-label">Phường / Xã <span class="text-danger">*</span></label>
                <select v-model="wardId" class="form-select co-input" :disabled="!provinceId || !wards.length">
                  <option value="">Chọn phường / xã</option>
                  <option v-for="w in wards" :key="w.id" :value="String(w.id)">{{ w.name }}</option>
                </select>
              </div>
              <div class="col-12">
                <label class="co-label">Địa chỉ cụ thể (số nhà, tên đường) <span class="text-danger">*</span></label>
                <input v-model="f.address" class="form-control co-input" :readonly="lockAddress" :class="{
                  'is-invalid': errors.address,
                  'bg-light': lockAddress,
                }" placeholder="Ví dụ: 123 Nguyễn Trãi" />
                <div class="invalid-feedback">{{ errors.address }}</div>
              </div>
            </template>
            <div v-if="shippingError" class="col-12 small text-danger">{{ shippingError }}</div>
            <div class="col-12">
              <label class="co-label">Ghi chú (không bắt buộc)</label>
              <textarea v-model="f.note" rows="2" class="form-control co-input"
                placeholder="Ghi chú về đơn hàng, thời gian nhận hàng..."></textarea>
            </div>
          </div>
        </section>

        <!-- Vận chuyển -->
        <section class="co-card mb-3">
          <h5 class="co-title">
            <span class="co-title-ico"><i class="bi bi-truck"></i></span>Hình
            thức vận chuyển
          </h5>
          <label class="co-option active mb-0">
            <input name="ship" class="form-check-input co-radio" type="radio" checked disabled />
            <span class="co-ico-box"><i class="bi bi-truck"></i></span>
            <span class="flex-grow-1">
              <span class="d-block small fw-semibold">Giao hàng tiêu chuẩn</span>
              <span class="d-block small text-muted">Nhận hàng trong 1-3 ngày làm việc</span>
            </span>
            <span class="small fw-bold" :class="{ 'text-success': isFreeShip }">{{ shippingLoading ? 'Đang tính...' :
              (isFreeShip ? '0 ₫ (Miễn phí ship)' : money(shippingFee)) }}</span>
          </label>
        </section>

        <!-- Thanh toán -->
        <section class="co-card">
          <h5 class="co-title">
            <span class="co-title-ico"><i class="bi bi-credit-card-2-front-fill"></i></span>Phương thức thanh toán
          </h5>

          <label for="pay-cod" class="co-option" :class="{ active: f.pay === 'cod' }">
            <input id="pay-cod" v-model="f.pay" name="pay" value="cod" type="radio" class="form-check-input co-radio" />
            <span class="co-ico-box"><i class="bi bi-cash-coin"></i></span>
            <span class="flex-grow-1">
              <span class="d-block fw-semibold">Thanh toán khi nhận hàng (COD)</span>
              <!-- <span class="d-block small text-muted">
                Thanh toán tiền mặt trực tiếp cho nhân viên giao hàng khi nhận
                hàng.
              </span> -->
            </span>
          </label>

          <label for="pay-vnpay" class="co-option mb-0" :class="{ active: f.pay === 'vnpay' }">
            <input id="pay-vnpay" v-model="f.pay" name="pay" value="vnpay" type="radio"
              class="form-check-input co-radio" />
            <span class="co-ico-box vnpay-ico-box">
              <span class="fw-bold" style="font-size: 0.85rem; letter-spacing: -0.5px">
                <span style="color: #005baa">VN</span><span style="color: #e11b22">PAY</span>
              </span>
            </span>
            <span class="flex-grow-1">
              <span class="d-flex align-items-center gap-2 flex-wrap">
                <span class="fw-semibold">Thanh toán qua VNPAY</span>
                <!-- <span
                  class="badge bg-danger-subtle text-danger"
                  style="font-size: 0.7rem"
                  >Khuyên dùng</span
                > -->
              </span>
              <!-- <span class="d-block small text-muted">
                Thanh toán bảo mật qua VNPAY QR, 40+ ngân hàng nội địa hoặc thẻ
                Visa/Mastercard.
              </span> -->
            </span>
          </label>
        </section>
      </div>

      <!-- ================= CỘT PHẢI ================= -->
      <div class="col-lg-5">
        <div class="co-sticky">
          <!-- Sản phẩm -->
          <section class="co-card mb-3">
            <div class="co-cart-head">
              <span><i class="bi bi-bag-check-fill text-danger me-2"></i>Sản phẩm
                trong đơn</span>
              <span class="co-count">{{ totalQty }}</span>
            </div>

            <div class="co-items">
              <div v-for="i in cart.items" :key="itemKey(i)" class="co-item">
                <div class="co-thumb">
                  <img v-if="itemImage(i) && !imgFailed[itemKey(i)]" :src="itemImage(i)" :alt="i.name"
                    @error="imgFailed[itemKey(i)] = true" />
                  <span v-else class="co-thumb-fb">👟</span>
                  <span class="co-thumb-qty">{{ i.qty }}</span>
                </div>
                <div class="flex-grow-1 min-w-0">
                  <div class="co-item-name">{{ i.name }}</div>
                  <div class="d-flex flex-wrap gap-1 mt-1">
                    <span v-if="i.color" class="co-chip">{{ i.color }}</span>
                    <span v-if="i.size" class="co-chip">Size {{ i.size }}</span>
                    <span class="co-chip">{{ money(i.price) }} × {{ i.qty }}</span>
                  </div>
                </div>
                <div class="small fw-bold text-nowrap">
                  {{ money(i.price * i.qty) }}
                </div>
              </div>
            </div>
          </section>

          <!-- Mã giảm giá -->
          <section class="co-card mb-3">
            <h6 class="co-title mb-3">
              <span class="co-title-ico"><i class="bi bi-ticket-perforated-fill"></i></span>Mã giảm giá
            </h6>

            <div v-if="!voucher" class="d-flex gap-2">
              <input v-model="voucherCode" class="form-control co-input text-uppercase" placeholder="Nhập mã giảm giá"
                @keyup.enter="applyVoucher" />
              <button class="btn btn-primary co-apply" type="button"
                :disabled="applying || (!voucherCode && !selectedVoucherId)" @click="applyVoucher">
                {{ applying ? "Đang kiểm tra..." : "Áp dụng" }}
              </button>
            </div>
            <div v-if="!voucher && voucherSuggestion" class="small text-primary mt-2">
              Mua thêm {{ money(Number(voucherSuggestion.hoaDonToiThieu) - orderTotal) }} để dùng
              <strong>{{ voucherSuggestion.maPhieuGiamGia }}</strong>
              ({{ formatVoucherValue(voucherSuggestion) }}).
            </div>

            <div v-if="voucher" class="co-voucher-ok">
              <i class="bi bi-ticket-perforated-fill"></i>
              <div class="flex-grow-1 small">
                <div>
                  Mã: <strong>{{ voucher.code }}</strong>
                  <span class="badge bg-success ms-1">Đang áp dụng</span>
                </div>
                <div class="text-success fw-semibold">
                  Đã giảm -{{ money(discount) }}
                </div>
                <div class="d-flex align-items-center gap-2 mt-1">
                  <span class="text-muted small">Muốn đổi mã? Bấm "Áp dụng" vào mã khác ở danh sách bên dưới</span>
                  <span class="text-muted small">·</span>
                  <button type="button" class="btn btn-sm btn-link text-muted p-0 text-decoration-none small"
                    @click="onRemoveClick">
                    Gỡ mã
                  </button>
                </div>
              </div>
            </div>

            <div v-if="voucherMsg.text" class="small mt-2" :class="voucherMsg.ok ? 'text-success' : 'text-danger'">
              {{ voucherMsg.text }}
            </div>

            <!-- Danh sách phiếu -->
            <div v-if="loadingVouchers" class="small text-muted mt-3">
              <span class="spinner-border spinner-border-sm me-1"></span>Đang
              tải phiếu giảm giá...
            </div>
            <div v-else-if="voucherOptions.length" class="mt-3">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <div class="co-label mb-0">Tất cả phiếu giảm giá</div>
                <span class="badge bg-light text-dark border">
                  {{ usableVoucherCount }}/{{ voucherOptions.length }} dùng được
                </span>
              </div>
              <div class="co-voucher-list">
                <div v-for="o in voucherOptions" :key="o.v.id" class="co-vcard" :class="{
                  active: voucher && voucher.id === o.v.id,
                  disabled: !o.eligible,
                }">
                  <div class="co-vleft"><i class="bi bi-percent"></i></div>
                  <div class="flex-grow-1 min-w-0">
                    <div class="fw-bold small d-flex align-items-center flex-wrap gap-1">
                      <span>{{ o.v.code }}</span>
                      <span v-if="o.eligible" class="badge bg-success-subtle text-success border border-success-subtle">
                        Dùng được
                      </span>
                      <span v-else class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">
                        Không dùng được
                      </span>
                      <span v-if="o.best && o.eligible" class="badge bg-success">
                        Tốt nhất
                      </span>
                    </div>
                    <div class="small">
                      {{ voucherLabel(o.v) }} • Đơn từ {{ money(o.v.min) }}
                    </div>
                    <div class="co-vexp">
                      <span v-if="o.expire">HSD: {{ o.expire }}</span>
                      <span v-if="o.left !== null">
                        · Còn {{ Math.max(0, o.left) }} lượt</span>
                    </div>
                    <div v-if="!o.eligible" class="co-vexp text-danger">
                      {{ o.reason || `Mua thêm ${money(o.missing)} để dùng mã này` }}
                    </div>
                    <div v-else class="co-vexp text-success">
                      Giảm được {{ money(o.discount) }}
                    </div>
                  </div>
                  <button type="button" class="btn btn-sm co-vbtn" :class="voucher && voucher.id === o.v.id
                      ? 'btn-success'
                      : o.eligible
                        ? 'btn-outline-primary'
                        : 'btn-outline-secondary'
                    " :disabled="!o.eligible" @click="toggleVoucher(o)">
                    {{
                      voucher && voucher.id === o.v.id
                        ? "✓ Đang dùng"
                        : o.eligible
                          ? "Áp dụng"
                          : "Không khả dụng"
                    }}
                  </button>
                </div>
              </div>
            </div>
            <div v-else class="small text-muted mt-3">
              Hiện chưa có phiếu giảm giá nào khả dụng.
            </div>
          </section>

          <!-- Tóm tắt -->
          <section class="co-card">
            <h6 class="co-title mb-3">
              <span class="co-title-ico"><i class="bi bi-receipt-cutoff"></i></span>Tóm tắt đơn hàng
            </h6>

            <div class="co-row">
              <span>Tạm tính ({{ totalQty }} sản phẩm)</span>
              <span class="fw-semibold">{{ money(cartTotal) }}</span>
            </div>
            <div class="co-row">
              <span>Phí vận chuyển</span>
              <span class="fw-semibold" :class="{ 'text-success': isFreeShip }">{{ isFreeShip ? '0 ₫ (Miễn phí ship)' :
                money(shippingFee) }}</span>
            </div>
            <div v-if="discount" class="co-row text-success">
              <span>Giảm giá<span v-if="voucher"> [{{ voucher.code }}]</span></span>
              <span class="fw-semibold">-{{ money(discount) }}</span>
            </div>

            <div class="co-total">
              <span class="fw-bold">Tổng cộng</span>
              <span class="price fs-4">{{ money(finalTotal) }}</span>
            </div>
            <div v-if="discount" class="co-saved">
              <i class="bi bi-piggy-bank-fill me-1"></i>Bạn đã tiết kiệm
              <strong>{{ money(discount) }}</strong>
            </div>

            <div v-if="submitError" class="alert alert-danger small py-2 mt-3 mb-0">
              {{ submitError }}
            </div>

            <button class="btn btn-primary w-100 co-submit" :disabled="submitting" @click="submit">
              <span v-if="submitting" class="spinner-border spinner-border-sm me-2"></span>
              {{
                submitting
                  ? "Đang xử lý..."
                  : f.pay === "vnpay"
                    ? "Thanh toán qua VNPAY"
                    : "Đặt hàng"
              }}
              <i v-if="!submitting" class="bi bi-arrow-right ms-1"></i>
            </button>

          </section>
        </div>
      </div>
    </div>

    <!-- ================= MODAL QUÉT MÃ QR THANH TOÁN (VIETQR / VNPAY) ================= -->
    <transition name="fade">
      <div v-if="isQrModalOpen" class="qr-modal-backdrop" @click.self="closeQrModal">
        <div class="qr-modal-card">
          <!-- Header -->
          <div class="qr-modal-header">
            <div class="qr-header-left">
              <div class="qr-header-badge">
                <span class="viet-txt">Viet</span><span class="qr-txt">QR</span>
                <span class="napas-badge">Napas 24/7</span>
              </div>
              <h5 class="qr-header-title">Quét mã QR để thanh toán đơn hàng</h5>
            </div>
            <button type="button" class="btn-close-qr" @click="closeQrModal" title="Đóng">✕</button>
          </div>

          <!-- Body -->
          <div class="qr-modal-body">
            <div class="qr-body-grid">
              <!-- Cột trái: Ảnh mã QR -->
              <div class="qr-img-column">
                <div class="qr-box">
                  <img :src="qrModalData.qrUrl" class="qr-image" alt="Mã QR thanh toán" />
                </div>
                <div class="qr-instruction">
                  <i class="bi bi-qr-code-scan me-1 text-primary"></i>
                  <span>Quét bằng app ngân hàng hoặc ví điện tử bất kỳ</span>
                </div>
              </div>

              <!-- Cột phải: Thông tin chuyển khoản -->
              <div class="qr-info-column">
                <div class="qr-amount-card">
                  <span class="qr-amount-lbl">Số tiền cần thanh toán</span>
                  <div class="qr-amount-val">{{ money(qrModalData.amount) }}</div>
                </div>

                <div class="qr-details-table">
                  <div class="qr-detail-row">
                    <span class="detail-label">Ngân hàng:</span>
                    <span class="detail-value fw-bold text-dark">{{ qrModalData.bankName }}</span>
                  </div>
                  <div class="qr-detail-row">
                    <span class="detail-label">Chủ tài khoản:</span>
                    <span class="detail-value fw-bold text-dark">{{ qrModalData.accountName }}</span>
                  </div>
                  <div class="qr-detail-row">
                    <span class="detail-label">Số tài khoản:</span>
                    <div class="detail-value-copy">
                      <span class="fw-bold font-monospace text-primary fs-6">{{ qrModalData.accountNo }}</span>
                      <button type="button" class="btn-copy-mini"
                        @click="copyText(qrModalData.accountNo, 'Số tài khoản')">
                        Sao chép
                      </button>
                    </div>
                  </div>
                  <div class="qr-detail-row">
                    <span class="detail-label">Nội dung CK:</span>
                    <div class="detail-value-copy">
                      <span class="badge bg-danger-subtle text-danger fw-bold font-monospace fs-6 px-2 py-1">
                        {{ qrModalData.maHoaDon }}
                      </span>
                      <button type="button" class="btn-copy-mini"
                        @click="copyText(qrModalData.maHoaDon, 'Nội dung chuyển khoản')">
                        Sao chép
                      </button>
                    </div>
                  </div>
                </div>

                <div class="qr-notice-card">
                  <i class="bi bi-info-circle-fill text-warning me-1"></i>
                  <span>Hệ thống sẽ tự động cập nhật đơn hàng ngay khi bạn quét mã thanh toán.</span>
                </div>

                <!-- Toast sao chép -->
                <transition name="fade">
                  <div v-if="copyToast" class="qr-copy-toast">
                    <i class="bi bi-check-circle-fill text-success me-1"></i>
                    <span>{{ copyToast }}</span>
                  </div>
                </transition>
              </div>
            </div>
          </div>

          <!-- Footer -->
          <div class="qr-modal-footer">
            <a v-if="qrModalData.paymentUrl" :href="qrModalData.paymentUrl" target="_blank"
              class="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-1">
              <i class="bi bi-box-arrow-up-right"></i>
              <span>Mở cổng VNPAY (Thẻ/Ví)</span>
            </a>
            <div class="d-flex align-items-center gap-2 ms-auto">
              <button type="button" class="btn btn-light" @click="closeQrModal">
                Đóng
              </button>
              <button type="button" class="btn btn-success d-inline-flex align-items-center gap-2 fw-semibold px-4"
                :disabled="qrConfirming" @click="confirmQrPayment">
                <span v-if="qrConfirming" class="spinner-border spinner-border-sm"></span>
                <i v-else class="bi bi-check-circle-fill"></i>
                <span>{{ qrConfirming ? 'Đang hoàn tất...' : 'Tôi đã quét mã xong' }}</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </transition>
    <!-- POPUP CHỌN / SỬA ĐỊA CHỈ KHÁCH HÀNG (CHECKOUT ONLINE) -->
    <div v-if="isAddressModalOpen" class="modal fade show d-block" style="background: rgba(0,0,0,0.5); z-index: 1055;"
      tabindex="-1" @click.self="isAddressModalOpen = false">
      <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable" style="max-width: 560px;">
        <div class="modal-content rounded-4 border-0 shadow">
          <div class="modal-header border-bottom py-3 px-4">
            <h6 class="modal-title fw-bold text-dark mb-0">
              <i class="bi bi-geo-alt-fill text-primary me-2"></i>
              {{ isEditingAddressInModal ? (addrModalForm.id ? 'Sửa địa chỉ hiện tại' : 'Thêm địa chỉ nhận hàng mới') : 'Chọn địa chỉ nhận hàng' }}
            </h6>
            <button type="button" class="btn-close" @click="isAddressModalOpen = false"></button>
          </div>
          <div class="modal-body p-3">
            <!-- Chế độ danh sách chọn địa chỉ -->
            <div v-if="!isEditingAddressInModal" class="d-flex flex-column gap-2">
              <div
                v-for="a in savedAddresses"
                :key="a.id"
                class="p-3 rounded-3 border d-flex align-items-center justify-content-between gap-2"
                :style="selectedAddr === a.id ? 'border-color: #dc2626 !important; background: #fef2f2;' : 'cursor: pointer; background: #fff;'"
                @click="selectAddrAndClose(a.id)"
              >
                <div class="min-w-0 me-2">
                  <div class="fw-semibold small text-dark mb-1 d-flex align-items-center flex-wrap gap-2">
                    <span>{{ a.name || userInfo(currentUser).name }}</span>
                    <span>·</span>
                    <span>{{ a.phone || userInfo(currentUser).phone }}</span>
                    <span v-if="a.isDefault" class="badge bg-primary">Mặc định</span>
                    <span v-if="selectedAddr === a.id" class="badge bg-success">Đang dùng</span>
                  </div>
                  <div class="small text-muted">{{ a.address }}</div>
                </div>
                <div class="d-flex align-items-center gap-2 flex-shrink-0">
                  <button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-3 py-1 fw-bold"
                    @click.stop="openEditAddressModal(a)">
                    Sửa
                  </button>
                  <button type="button" class="btn btn-sm rounded-pill px-3 py-1 fw-bold"
                    :class="selectedAddr === a.id ? 'btn-primary' : 'btn-outline-secondary'"
                    @click.stop="selectAddrAndClose(a.id)">
                    {{ selectedAddr === a.id ? 'Đang dùng' : 'Chọn' }}
                  </button>
                </div>
              </div>

              <div
                class="p-3 rounded-3 border d-flex align-items-center justify-content-between"
                :style="selectedAddr === 'new' ? 'border-color: #dc2626 !important; background: #fef2f2;' : 'cursor: pointer; background: #fff;'"
                @click="selectAddrAndClose('new')"
              >
                <div class="d-flex align-items-center gap-2">
                  <i class="bi bi-plus-circle text-primary fs-5"></i>
                  <div>
                    <div class="fw-semibold small text-dark">Nhập địa chỉ mới</div>
                    <div class="small text-muted">Tự điền thông tin và địa chỉ giao hàng bên dưới</div>
                  </div>
                </div>
                <button type="button" class="btn btn-sm rounded-pill px-3 py-1 fw-bold"
                  :class="selectedAddr === 'new' ? 'btn-primary' : 'btn-outline-secondary'"
                  @click.stop="selectAddrAndClose('new')">
                  {{ selectedAddr === 'new' ? 'Đang dùng' : 'Chọn' }}
                </button>
              </div>
            </div>

            <!-- Chế độ sửa / thêm địa chỉ -->
            <div v-else class="row g-3 px-1 py-1">
              <div class="col-md-6">
                <label class="co-label">Họ và tên <span class="text-danger">*</span></label>
                <input v-model="addrModalForm.name" class="form-control co-input" placeholder="Nhập họ và tên người nhận" />
              </div>
              <div class="col-md-6">
                <label class="co-label">Số điện thoại <span class="text-danger">*</span></label>
                <input v-model="addrModalForm.phone" class="form-control co-input" maxlength="11" placeholder="Nhập số điện thoại (VD: 0987...)" />
              </div>
              <div class="col-md-6">
                <label class="co-label">Tỉnh / Thành phố <span class="text-danger">*</span></label>
                <select v-model="addrModalForm.provinceId" class="form-select co-input" @change="onAddrModalProvinceChange">
                  <option value="">Chọn tỉnh / thành phố</option>
                  <option v-for="p in provinces" :key="p.id" :value="String(p.id)">{{ p.name }}</option>
                </select>
              </div>
              <div class="col-md-6">
                <label class="co-label">Phường / Xã <span class="text-danger">*</span></label>
                <select v-model="addrModalForm.wardId" class="form-select co-input" :disabled="!addrModalForm.provinceId || !addrModalWards.length">
                  <option value="">Chọn phường / xã</option>
                  <option v-for="w in addrModalWards" :key="w.id" :value="String(w.id)">{{ w.name }}</option>
                </select>
              </div>
              <div class="col-12">
                <label class="co-label">Địa chỉ cụ thể (số nhà, tên đường) <span class="text-danger">*</span></label>
                <input v-model="addrModalForm.street" class="form-control co-input" placeholder="Ví dụ: Số 123 Nguyễn Trãi" @keyup.enter="saveAddrModalForm" />
              </div>
              <div class="col-12">
                <div class="form-check">
                  <input id="chkAddrModalDefault" v-model="addrModalForm.isDefault" class="form-check-input" type="checkbox" />
                  <label class="form-check-label small fw-semibold" for="chkAddrModalDefault">
                    Đặt làm địa chỉ nhận hàng mặc định
                  </label>
                </div>
              </div>
              <div v-if="addrModalError" class="col-12">
                <div class="alert alert-danger small py-2 mb-0">{{ addrModalError }}</div>
              </div>
            </div>
          </div>
          <div class="modal-footer border-top py-2 px-4 d-flex align-items-center justify-content-between">
            <button v-if="!isEditingAddressInModal" type="button" class="btn btn-outline-primary rounded-pill px-3 btn-sm fw-bold" @click="openAddAddressModal">
              <i class="bi bi-plus-circle me-1"></i>Thêm địa chỉ mới
            </button>
            <button v-else type="button" class="btn btn-outline-secondary rounded-pill px-3 btn-sm fw-bold" :disabled="addrModalSaving" @click="isEditingAddressInModal = false">
              <i class="bi bi-arrow-left me-1"></i>Quay lại
            </button>
            <div class="d-flex align-items-center gap-2 ms-auto">
              <button v-if="isEditingAddressInModal" type="button" class="btn btn-primary rounded-pill px-4 btn-sm fw-bold" :disabled="addrModalSaving" @click="saveAddrModalForm">
                <span v-if="addrModalSaving" class="spinner-border spinner-border-sm me-1"></span>
                {{ addrModalSaving ? 'Đang lưu...' : 'Lưu địa chỉ' }}
              </button>
              <button v-else type="button" class="btn btn-secondary rounded-pill px-4 btn-sm" @click="isAddressModalOpen = false">
                Đóng
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.min-w-0 {
  min-width: 0;
}

/* ===== Steps ===== */
.co-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 16px;
  box-shadow: 0 4px 18px rgba(15, 23, 42, 0.05);
}

.co-step {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #94a3b8;
  font-weight: 600;
  font-size: 0.88rem;
}

.co-step-dot {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-size: 0.82rem;
  font-weight: 700;
  background: #f1f5f9;
  color: #94a3b8;
}

.co-step.done .co-step-dot {
  background: #16a34a;
  color: #fff;
}

.co-step.done {
  color: #16a34a;
}

.co-step.current .co-step-dot {
  background: var(--bs-primary, #dc2626);
  color: #fff;
  box-shadow: 0 0 0 4px rgba(220, 38, 38, 0.15);
}

.co-step.current {
  color: #111827;
}

.co-step-line {
  flex: 0 1 70px;
  height: 3px;
  border-radius: 3px;
  background: #e5e7eb;
}

.co-step-line.done {
  background: #16a34a;
}

/* ===== Card ===== */
.co-card {
  padding: 1.35rem 1.5rem;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 18px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.05);
}

.co-login {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px;
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
  border-radius: 14px;
}

.co-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  margin-bottom: 1.1rem;
  color: #111827;
}

.co-title-ico {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 1rem;
  color: var(--bs-primary, #dc2626);
  background: rgba(var(--bs-primary-rgb, 214, 40, 40), 0.1);
}

.co-label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
  color: #334155;
}

.co-input {
  border-radius: 12px;
  padding: 0.65rem 0.9rem;
  font-size: 0.92rem;
}

.co-input:focus {
  border-color: var(--bs-primary);
  box-shadow: 0 0 0 0.2rem rgba(var(--bs-primary-rgb), 0.15);
}

/* ===== Option ===== */
.co-option {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.9rem 1rem;
  margin-bottom: 0.7rem;
  border: 1.5px solid #e5e7eb;
  border-radius: 14px;
  background: #fff;
  cursor: pointer;
  transition:
    border-color 0.15s,
    background-color 0.15s,
    box-shadow 0.15s;
}

.co-option:hover {
  border-color: var(--bs-primary);
}

.co-option.active {
  border-color: var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), 0.05);
  box-shadow: 0 4px 14px rgba(var(--bs-primary-rgb), 0.12);
}

.co-radio {
  margin: 0;
  flex-shrink: 0;
  cursor: pointer;
}

.co-radio:focus {
  box-shadow: none;
}

.co-ico-box {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  font-size: 1.3rem;
  color: var(--bs-primary);
  background: #f8f9fa;
  border-radius: 12px;
}

.vnpay-ico-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

/* ===== Right column ===== */
.co-sticky {
  position: sticky;
  top: 1rem;
}

.co-cart-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 0.9rem;
  margin-bottom: 0.9rem;
  border-bottom: 1px solid #eef0f3;
  font-weight: 800;
}

.co-count {
  min-width: 28px;
  text-align: center;
  padding: 2px 10px;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), 0.1);
  border-radius: 999px;
}

.co-items {
  max-height: 340px;
  overflow-y: auto;
  padding-right: 2px;
}

.co-item {
  display: flex;
  align-items: center;
  gap: 0.9rem;
  margin-bottom: 0.9rem;
}

.co-item:last-child {
  margin-bottom: 0;
}

.co-thumb {
  position: relative;
  flex: 0 0 66px;
  width: 66px;
  height: 66px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(145deg, #fdf2f2, #fbe9ee);
}

.co-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 14px;
}

.co-thumb-fb {
  font-size: 1.8rem;
}

.co-thumb-qty {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  display: grid;
  place-items: center;
  font-size: 0.7rem;
  font-weight: 700;
  color: #fff;
  background: #475569;
  border: 2px solid #fff;
  border-radius: 999px;
}

.co-item-name {
  font-size: 0.88rem;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.co-chip {
  display: inline-block;
  padding: 0.1rem 0.55rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 999px;
}

/* ===== Voucher ===== */
.co-apply {
  white-space: nowrap;
  border-radius: 12px;
  padding-inline: 1.1rem;
  font-weight: 700;
}

.co-voucher-ok {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 0.9rem;
  background: linear-gradient(120deg, #f0fdf4, #dcfce7);
  border: 1px dashed #86efac;
  border-radius: 12px;
}

.co-voucher-ok>.bi-ticket-perforated-fill {
  font-size: 1.4rem;
  color: #16a34a;
}

.co-x {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 50%;
  background: rgba(220, 38, 38, 0.1);
  color: #dc2626;
  font-size: 0.7rem;
  cursor: pointer;
}

.co-x:hover {
  background: rgba(220, 38, 38, 0.2);
}

.co-voucher-list {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 2px;
}

.co-vcard {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0;
  margin-bottom: 0.6rem;
  overflow: hidden;
  border: 1px dashed #e5c7ca;
  border-radius: 12px;
  background: #fffafa;
  padding-right: 0.8rem;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}

.co-vcard:hover:not(.disabled) {
  box-shadow: 0 4px 14px rgba(15, 23, 42, 0.08);
}

.co-vcard>.flex-grow-1 {
  padding: 0.6rem 0;
}

.co-vleft {
  align-self: stretch;
  flex: 0 0 46px;
  display: grid;
  place-items: center;
  font-size: 1.2rem;
  color: #fff;
  background: linear-gradient(160deg, #ef4444, #b91c1c);
}

.co-vcard.active {
  border: 1px solid var(--bs-primary);
  background: rgba(var(--bs-primary-rgb), 0.06);
}

.co-vcard.disabled {
  opacity: 0.6;
  background: #f8f9fa;
  border-color: #dee2e6;
}

.co-vcard.disabled .co-vleft {
  background: #94a3b8;
}

.co-vexp {
  font-size: 0.74rem;
  color: #6b7280;
}

.co-vbtn {
  white-space: nowrap;
  font-weight: 600;
  border-radius: 8px;
}

/* ===== Summary ===== */
.co-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
  font-size: 0.92rem;
  color: #475569;
}

.co-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 1rem 0 0.5rem;
  padding-top: 1rem;
  border-top: 2px dashed #e5e7eb;
}

.co-saved {
  text-align: right;
  font-size: 0.82rem;
  color: #15803d;
}

.co-submit {
  margin-top: 1.1rem;
  padding: 0.85rem;
  border-radius: 999px;
  font-weight: 800;
  box-shadow: 0 8px 20px rgba(var(--bs-primary-rgb), 0.28);
  transition:
    transform 0.15s,
    box-shadow 0.15s;
}

.co-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(var(--bs-primary-rgb), 0.36);
}

.co-perks {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 16px;
  margin-top: 1rem;
  padding-top: 0.9rem;
  border-top: 1px solid #f1f5f9;
  font-size: 0.78rem;
  color: #64748b;
}

.co-perks i {
  margin-right: 5px;
}

/* ===== Success / Empty ===== */
.co-success {
  max-width: 560px;
  margin: 2rem auto;
  text-align: center;
  padding: 3rem 1.5rem;
  background: #fff;
  border: 1px solid #eef0f3;
  border-radius: 24px;
  box-shadow: 0 10px 34px rgba(15, 23, 42, 0.08);
}

.co-success-ico {
  width: 84px;
  height: 84px;
  margin: 0 auto 1.1rem;
  display: grid;
  place-items: center;
  font-size: 2.4rem;
  color: #fff;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  border-radius: 50%;
  box-shadow: 0 10px 24px rgba(22, 163, 74, 0.35);
}

.co-success-ico.empty {
  background: #f1f5f9;
  color: #94a3b8;
  box-shadow: none;
}

.co-success-code {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 1rem;
  padding: 10px 18px;
  background: #fef2f2;
  border: 1px dashed #fca5a5;
  border-radius: 14px;
}

.co-success-code strong {
  color: #dc2626;
  font-size: 1.15rem;
}

.co-copy {
  width: 30px;
  height: 30px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  color: #64748b;
  cursor: pointer;
}

.co-copy:hover {
  color: #dc2626;
  border-color: #dc2626;
}

@media (max-width: 991.98px) {
  .co-sticky {
    position: static;
  }
}

@media (max-width: 575.98px) {
  .co-step-name {
    display: none;
  }

  .co-step.current .co-step-name {
    display: inline;
  }

  .co-card {
    padding: 1.1rem;
  }
}

/* ===== QR Payment Modal (VietQR / VNPAY) ===== */
.qr-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(4px);
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.qr-modal-card {
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 680px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: qrPopIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes qrPopIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }

  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.qr-modal-header {
  padding: 1.1rem 1.4rem;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #f8fafc;
}

.qr-header-left {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.qr-header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.viet-txt {
  font-weight: 900;
  color: #005baa;
  font-size: 1.15rem;
}

.qr-txt {
  font-weight: 900;
  color: #e11b22;
  font-size: 1.15rem;
}

.napas-badge {
  background: #0284c7;
  color: #fff;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
}

.qr-header-title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #0f172a;
}

.btn-close-qr {
  border: none;
  background: #e2e8f0;
  color: #475569;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.btn-close-qr:hover {
  background: #cbd5e1;
  color: #0f172a;
}

.qr-modal-body {
  padding: 1.4rem;
}

.qr-body-grid {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 640px) {
  .qr-body-grid {
    grid-template-columns: 1fr;
  }
}

.qr-img-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.qr-box {
  width: 220px;
  height: 220px;
  padding: 8px;
  border-radius: 12px;
  background: #fff;
  border: 1.5px solid #e2e8f0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.6rem;
}

.qr-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 6px;
}

.qr-instruction {
  font-size: 0.8rem;
  color: #64748b;
  line-height: 1.3;
  margin-bottom: 0.8rem;
}

.qr-auto-box {
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 0.5rem 0.75rem;
  width: 100%;
  transition: all 0.2s;
}

.qr-auto-box.disabled {
  opacity: 0.6;
}

.qr-timer-text {
  font-size: 0.8rem;
  color: #475569;
}

.qr-info-column {
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.qr-amount-card {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 10px;
  padding: 0.8rem 1rem;
}

.qr-amount-lbl {
  font-size: 0.8rem;
  color: #166534;
  font-weight: 500;
  display: block;
}

.qr-amount-val {
  font-size: 1.5rem;
  font-weight: 800;
  color: #15803d;
  letter-spacing: -0.5px;
}

.qr-details-table {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 0.6rem 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.qr-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.85rem;
}

.detail-label {
  color: #64748b;
  font-weight: 500;
}

.detail-value-copy {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-copy-mini {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #334155;
  padding: 1px 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-copy-mini:hover {
  background: #f1f5f9;
  border-color: #94a3b8;
  color: #0f172a;
}

.qr-notice-card {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 0.6rem 0.8rem;
  font-size: 0.8rem;
  color: #92400e;
  line-height: 1.35;
}

.qr-copy-toast {
  background: #1e293b;
  color: #fff;
  border-radius: 6px;
  padding: 0.4rem 0.75rem;
  font-size: 0.8rem;
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
}

.qr-modal-footer {
  padding: 0.9rem 1.4rem;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
  display: flex;
  align-items: center;
}

.cursor-pointer {
  cursor: pointer;
}
</style>
