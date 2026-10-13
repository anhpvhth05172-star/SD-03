<script setup>
import { reactive, computed, watch, inject, ref } from "vue";
import axios from "axios";
import Swal from "sweetalert2";
import { useRouter } from "vue-router";

const router = useRouter();
const baseAPI = inject("baseAPI");
const saving = ref(false);

const LOAI_GIAM = { percent: "PERCENT", cash: "AMOUNT" };
const PHAM_VI = { public: "CONG_KHAI", private: "CA_NHAN" };

/* Mã phiếu: ảnh không có ô nhập nên tự sinh, backend vẫn nhận maPhieuGiamGia */
const randomCode = () => {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
  let code = "VC";
  for (let i = 0; i < 8; i++) {
    code += chars[Math.floor(Math.random() * chars.length)];
  }
  return code;
};

const form = reactive({
  code: randomCode(),
  name: "",
  quantity: null,
  minOrder: null,
  discountType: "percent",
  discountValue: null,
  maxDiscount: null,
  startDate: "",
  endDate: "",
  description: "",
  scope: "public",
  customerIds: [],
});

const isPercent = computed(() => form.discountType === "percent");
const isPrivate = computed(() => form.scope === "private");

/* ---------------- Định dạng tiền: 100.000 ---------------- */
const fmtMoney = (v) =>
  v === null || v === undefined || v === ""
    ? ""
    : Number(v).toLocaleString("vi-VN");

const onMoneyInput = (key, e) => {
  const digits = e.target.value.replace(/\D/g, "");
  form[key] = digits ? Number(digits) : null;
  e.target.value = fmtMoney(form[key]); // hiển thị lại có dấu chấm
};

/* ---------------- Validate (hiện lỗi sau khi chạm vào ô hoặc bấm Lưu) ---------------- */
const submitted = ref(false);
const touched = reactive({});
const touch = (key) => (touched[key] = true);

const errors = computed(() => {
  const e = {};
  if (!form.name) e.name = "Vui lòng nhập tên phiếu giảm giá.";

  if (!form.quantity || form.quantity < 1)
    e.quantity = "Số lượng phải lớn hơn hoặc bằng 1.";

  if (form.minOrder === null || form.minOrder < 0)
    e.minOrder = "Vui lòng nhập đơn hàng tối thiểu.";

  if (isPercent.value) {
    if (
      !form.discountValue ||
      form.discountValue < 1 ||
      form.discountValue > 100
    )
      e.discountValue = "Mức giảm (%) phải nằm trong khoảng từ 1% đến 100%.";
  } else if (!form.discountValue || form.discountValue <= 0) {
    e.discountValue = "Số tiền giảm phải lớn hơn 0.";
  }

  if (!form.startDate) e.startDate = "Vui lòng chọn ngày bắt đầu.";
  if (!form.endDate) e.endDate = "Vui lòng chọn ngày kết thúc.";
  else if (form.startDate && form.endDate <= form.startDate)
    e.endDate = "Ngày kết thúc phải sau ngày bắt đầu.";

  if (isPrivate.value && form.customerIds.length === 0)
    e.customerIds = "Vui lòng chọn ít nhất 1 khách hàng.";

  return e;
});

const err = (key) => (submitted.value || touched[key]) && errors.value[key];

/* ---------------- Khách hàng ---------------- */
const customers = ref([]);
const loadingCustomers = ref(false);
const customerKeyword = ref("");
let customersLoaded = false;

const formatDate = (s) => (s ? new Date(s).toLocaleDateString("en-GB") : "—");

const loadCustomers = async () => {
  if (customersLoaded) return;
  loadingCustomers.value = true;
  try {
    // ⚠️ Đổi endpoint + tên field cho khớp backend của bạn
    const res = await axios.get(baseAPI + "api/khach-hang/all");
    const list = res.data?.data ?? res.data ?? [];
    customers.value = list.map((k) => ({
      id: k.id,
      code: k.maKhachHang,
      name: k.tenKhachHang,
      dob: k.ngaySinh,
      phone: k.soDienThoai,
      email: k.email,
      orders: k.soDonHang ?? 0,
    }));
    customersLoaded = true;
  } catch (e) {
    Swal.fire({
      icon: "error",
      title: "Không tải được danh sách khách hàng",
      text: e.response?.data?.message || "Không kết nối được máy chủ",
      confirmButtonColor: "#cc0000",
    });
  } finally {
    loadingCustomers.value = false;
  }
};

const filteredCustomers = computed(() => {
  const kw = customerKeyword.value.trim().toLowerCase();
  if (!kw) return customers.value;
  return customers.value.filter((c) =>
    [c.code, c.name, c.phone].some((v) => (v || "").toLowerCase().includes(kw)),
  );
});

const allFilteredSelected = computed(
  () =>
    filteredCustomers.value.length > 0 &&
    filteredCustomers.value.every((c) => form.customerIds.includes(c.id)),
);

const toggleAll = (checked) => {
  const ids = filteredCustomers.value.map((c) => c.id);
  form.customerIds = checked
    ? [...new Set([...form.customerIds, ...ids])]
    : form.customerIds.filter((id) => !ids.includes(id));
};

/* ---------------- Watchers ---------------- */
watch(
  () => form.scope,
  (value) => {
    if (value === "private") {
      loadCustomers();
      form.quantity = form.customerIds.length || null;
    } else {
      form.customerIds = [];
      customerKeyword.value = "";
      form.quantity = null; // về công khai thì cho nhập lại
    }
  },
  { immediate: true },
);

/* Cá nhân: số lượng = số khách hàng đã chọn */
watch(
  () => form.customerIds.length,
  (len) => {
    if (isPrivate.value) form.quantity = len || null;
  },
);

/* ---------------- Submit ---------------- */
const toLocalDateTime = (v) => (v && v.length === 16 ? v + ":00" : v);

const submit = async () => {
  submitted.value = true;
  if (Object.keys(errors.value).length) return;

  const data = {
    maPhieuGiamGia: form.code,
    tenPhieuGiamGia: form.name,
    loaiGiamGia: LOAI_GIAM[form.discountType],
    giaTriGiam: form.discountValue,
    giamToiDa:
      isPercent.value && form.maxDiscount > 0 ? form.maxDiscount : null,
    hoaDonToiThieu: form.minOrder || 0,
    ngayBatDau: toLocalDateTime(form.startDate),
    ngayKetThuc: toLocalDateTime(form.endDate),
    soLuong: form.quantity,
    trangThai: true,
    phamVi: PHAM_VI[form.scope],
    danhSachKhachHangId: isPrivate.value ? form.customerIds : [],
    moTa: form.description || null,
  };

  saving.value = true;
  try {
    await axios.post(baseAPI + "admin/phieu-giam-gia/add", data);
    await Swal.fire({
      icon: "success",
      title: "Tạo phiếu giảm giá thành công",
      timer: 1500,
      showConfirmButton: false,
    });
    router.push("/admin/phieu-giam-gia");
  } catch (e) {
    Swal.fire({
      icon: "error",
      title: "Tạo thất bại",
      text: e.response?.data?.message || "Không kết nối được máy chủ",
      confirmButtonColor: "#cc0000",
    });
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div class="screen">
    <!-- Đầu trang -->
    <header class="page-head">
      <h1 class="page-title">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linejoin="round"
        >
          <path
            d="M3 8a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v2a2 2 0 0 0 0 4v2a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-2a2 2 0 0 0 0-4V8z"
          />
          <path d="M9 9h6M9 15h6" stroke-linecap="round" />
        </svg>
        Thêm mới phiếu giảm giá
      </h1>
      <RouterLink to="/admin/phieu-giam-gia" class="btn-back">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M19 12H5M11 6l-6 6 6 6" />
        </svg>
        Quay lại danh sách
      </RouterLink>
    </header>

    <form novalidate @submit.prevent="submit">
      <!-- ============ Thông tin chung ============ -->
      <section class="card">
        <h2 class="card-title">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <rect x="3" y="4" width="18" height="16" rx="2" />
            <path d="M7 9h10M7 13h10M7 17h6" />
          </svg>
          Thông tin chung
        </h2>

        <div class="grid">
          <!-- Tên phiếu -->
          <div class="field span-2">
            <label>Tên phiếu giảm giá <b class="req">*</b></label>
            <input
              v-model.trim="form.name"
              type="text"
              maxlength="200"
              :class="{ invalid: err('name') }"
              @blur="touch('name')"
            />
            <p v-if="err('name')" class="err">
              <i class="err-ico">!</i>{{ errors.name }}
            </p>
          </div>

          <!-- Số lượng -->
          <div class="field">
            <label>
              Số lượng <b class="req">*</b>
              <small v-if="isPrivate"
                >(Tự động theo số khách hàng đã chọn)</small
              >
            </label>
            <input
              v-model.number="form.quantity"
              type="number"
              min="1"
              :readonly="isPrivate"
              :tabindex="isPrivate ? -1 : 0"
              :class="{ invalid: err('quantity'), locked: isPrivate }"
              @blur="touch('quantity')"
            />
            <p v-if="err('quantity')" class="err">
              <i class="err-ico">!</i>{{ errors.quantity }}
            </p>
          </div>

          <!-- Đơn hàng tối thiểu -->
          <div class="field">
            <label>Đơn hàng tối thiểu <b class="req">*</b></label>
            <input
              :value="fmtMoney(form.minOrder)"
              type="text"
              inputmode="numeric"
              :class="{ invalid: err('minOrder') }"
              @input="onMoneyInput('minOrder', $event)"
              @blur="touch('minOrder')"
            />
            <p v-if="err('minOrder')" class="err">
              <i class="err-ico">!</i>{{ errors.minOrder }}
            </p>
          </div>

          <!-- Loại giảm -->
          <div class="field">
            <label>Loại giảm <b class="req">*</b></label>
            <select v-model="form.discountType">
              <option value="percent">Giảm theo %</option>
              <option value="cash">Giảm theo số tiền</option>
            </select>
          </div>

          <!-- Giá trị giảm -->
          <div class="field">
            <label>Giá trị giảm <b class="req">*</b></label>
            <div class="affix" :class="{ invalid: err('discountValue') }">
              <input
                v-if="isPercent"
                v-model.number="form.discountValue"
                type="number"
                min="1"
                max="100"
                @blur="touch('discountValue')"
              />
              <input
                v-else
                :value="fmtMoney(form.discountValue)"
                type="text"
                inputmode="numeric"
                @input="onMoneyInput('discountValue', $event)"
                @blur="touch('discountValue')"
              />
              <span class="affix-text">{{ isPercent ? "%" : "₫" }}</span>
            </div>
            <p v-if="err('discountValue')" class="err">
              <i class="err-ico">!</i>{{ errors.discountValue }}
            </p>
          </div>

          <!-- Giảm tối đa (chỉ khi giảm theo %) -->
          <div v-if="isPercent" class="field span-2">
            <label>Giảm tối đa (VNĐ)</label>
            <input
              :value="fmtMoney(form.maxDiscount)"
              type="text"
              inputmode="numeric"
              placeholder="Để trống nếu không giới hạn"
              @input="onMoneyInput('maxDiscount', $event)"
            />
          </div>

          <!-- Ngày bắt đầu -->
          <div class="field">
            <label>Ngày bắt đầu <b class="req">*</b></label>
            <input
              v-model="form.startDate"
              type="datetime-local"
              :class="{ invalid: err('startDate') }"
              @blur="touch('startDate')"
            />
            <p v-if="err('startDate')" class="err">
              <i class="err-ico">!</i>{{ errors.startDate }}
            </p>
          </div>

          <!-- Ngày kết thúc -->
          <div class="field">
            <label>Ngày kết thúc <b class="req">*</b></label>
            <input
              v-model="form.endDate"
              type="datetime-local"
              :min="form.startDate"
              :class="{ invalid: err('endDate') }"
              @blur="touch('endDate')"
            />
            <p v-if="err('endDate')" class="err">
              <i class="err-ico">!</i>{{ errors.endDate }}
            </p>
          </div>

          <!-- Mô tả -->
          <div class="field span-2">
            <label>Mô tả phiếu giảm giá <small>(Không bắt buộc)</small></label>
            <textarea
              v-model.trim="form.description"
              rows="3"
              maxlength="500"
              placeholder="Nhập ghi chú hoặc mô tả"
            ></textarea>
          </div>
        </div>
      </section>

      <!-- ============ Đối tượng áp dụng ============ -->
      <section class="card">
        <h2 class="card-title">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <circle cx="9" cy="8" r="3.2" />
            <path
              d="M2.5 19c0-3.3 2.9-5.5 6.5-5.5s6.5 2.2 6.5 5.5v.5h-13V19z"
            />
            <circle cx="17" cy="9" r="2.5" />
            <path
              d="M16.5 13.6c3 0 5 1.8 5 4.4v.5h-4.4c0-1.9-.8-3.6-2.1-4.6.4-.2.9-.3 1.5-.3z"
            />
          </svg>
          Đối tượng áp dụng
        </h2>

        <div class="radio-row">
          <label class="radio">
            <input v-model="form.scope" type="radio" value="public" />
            <span>Tất cả (Công khai)</span>
          </label>
          <label class="radio">
            <input v-model="form.scope" type="radio" value="private" />
            <span>Nhóm chỉ định (Cá nhân)</span>
          </label>
        </div>

        <div v-if="isPrivate" class="picker">
          <div class="picker-head">
            <h3>Danh sách khách hàng nhận phiếu</h3>
            <span class="picked">Đã chọn: {{ form.customerIds.length }}</span>
          </div>

          <div class="search">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            >
              <circle cx="11" cy="11" r="6.5" />
              <path d="M16 16l4.5 4.5" />
            </svg>
            <input
              v-model="customerKeyword"
              type="text"
              placeholder="Tìm kiếm theo mã, tên, SDT..."
            />
          </div>
          <p v-if="err('customerIds')" class="err">
            <i class="err-ico">!</i>{{ errors.customerIds }}
          </p>

          <div class="table-wrap">
            <table class="cus-table">
              <thead>
                <tr>
                  <th class="w-check">
                    <input
                      type="checkbox"
                      :checked="allFilteredSelected"
                      :disabled="!filteredCustomers.length"
                      aria-label="Chọn tất cả"
                      @change="toggleAll($event.target.checked)"
                    />
                  </th>
                  <th>Mã KH</th>
                  <th>Tên Khách Hàng</th>
                  <th>Ngày sinh</th>
                  <th>Số điện thoại</th>
                  <th>Email</th>
                  <th class="right">Đã mua</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loadingCustomers">
                  <td colspan="7" class="empty">Đang tải khách hàng...</td>
                </tr>
                <tr v-else-if="!filteredCustomers.length">
                  <td colspan="7" class="empty">Không có khách hàng phù hợp</td>
                </tr>
                <tr
                  v-for="c in filteredCustomers"
                  v-else
                  :key="c.id"
                  :class="{ selected: form.customerIds.includes(c.id) }"
                >
                  <td class="w-check">
                    <input
                      v-model="form.customerIds"
                      type="checkbox"
                      :value="c.id"
                    />
                  </td>
                  <td class="muted">{{ c.code }}</td>
                  <td class="strong">{{ c.name }}</td>
                  <td>{{ formatDate(c.dob) }}</td>
                  <td>{{ c.phone || "—" }}</td>
                  <td>{{ c.email || "—" }}</td>
                  <td class="right strong">{{ c.orders }} đơn</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- Nút thao tác -->
      <div class="actions">
        <RouterLink to="/admin/phieu-giam-gia" class="btn-cancel"
          >Hủy</RouterLink
        >
        <button type="submit" class="btn-save" :disabled="saving">
          {{ saving ? "Đang lưu..." : "Lưu phiếu giảm giá" }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.screen {
  --red: #cc0000;
  --red-soft: #fdecec;
  --line: #e5e7eb;
  --ink: #111827;
  --muted: #6b7280;
  padding: 0.5rem 0.75rem 2rem;
  color: var(--ink);
}

/* ---- Đầu trang ---- */
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0 0.9rem;
  margin-bottom: 1.1rem;
  border-bottom: 1px solid var(--line);
}
.page-title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
}
.page-title svg {
  width: 24px;
  height: 24px;
}
.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.55rem 1.1rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: #374151;
  font-weight: 600;
  text-decoration: none;
}
.btn-back svg {
  width: 16px;
  height: 16px;
}
.btn-back:hover {
  border-color: var(--red);
  color: var(--red);
}

/* ---- Card ---- */
.card {
  background: #fff;
  border-radius: 14px;
  padding: 1.4rem 1.6rem 1.6rem;
  margin-bottom: 1.25rem;
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.06);
}
.card-title {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin: 0 0 1.25rem;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid var(--line);
  font-size: 1rem;
  font-weight: 700;
}
.card-title svg {
  width: 18px;
  height: 18px;
}

/* ---- Lưới form ---- */
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.1rem 1.5rem;
}
.span-2 {
  grid-column: 1 / -1;
}
.field label {
  display: block;
  margin-bottom: 0.4rem;
  font-size: 0.88rem;
  font-weight: 600;
}
.field label small {
  font-weight: 400;
  color: var(--muted);
}
.req {
  color: var(--red);
}

.field input,
.field select,
.field textarea {
  width: 100%;
  padding: 0.7rem 0.9rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  font: inherit;
  color: var(--ink);
  outline: none;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}
.field textarea {
  resize: vertical;
}
.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}
.field input.invalid {
  border-color: var(--red);
}

/* Ô có hậu tố (%, ₫) */
.affix {
  display: flex;
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.affix:focus-within {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}
.affix.invalid {
  border-color: var(--red);
}
.affix input {
  border: 0;
  border-radius: 0;
  box-shadow: none !important;
}
.affix-text {
  display: grid;
  place-items: center;
  min-width: 48px;
  background: #f3f4f6;
  border-left: 1px solid var(--line);
  color: var(--muted);
  font-weight: 600;
}

/* Thông báo lỗi */
.err {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin: 0.4rem 0 0;
  font-size: 0.82rem;
  color: var(--red);
}
.err-ico {
  display: inline-grid;
  place-items: center;
  width: 14px;
  height: 14px;
  border: 1.5px solid var(--red);
  border-radius: 50%;
  font-size: 0.6rem;
  font-style: normal;
  font-weight: 800;
  flex: none;
}

/* ---- Đối tượng áp dụng ---- */
.radio-row {
  display: flex;
  gap: 3rem;
  margin-bottom: 1.1rem;
}
.radio {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
  cursor: pointer;
  font-weight: 600;
}
.radio input {
  width: 18px;
  height: 18px;
  accent-color: var(--red);
}

.picker {
  padding: 1.2rem;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fafafa;
}
.picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.9rem;
}
.picker-head h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
}
.picked {
  padding: 0.2rem 0.7rem;
  border-radius: 999px;
  background: var(--red-soft);
  color: var(--red);
  font-size: 0.82rem;
  font-weight: 600;
}

.search {
  position: relative;
  max-width: 460px;
  margin-bottom: 1rem;
}
.search svg {
  position: absolute;
  left: 14px;
  top: 50%;
  width: 18px;
  height: 18px;
  transform: translateY(-50%);
  color: var(--muted);
}
.search input {
  width: 100%;
  padding: 0.65rem 1rem 0.65rem 2.6rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  font: inherit;
  outline: none;
}
.search input:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

.table-wrap {
  max-height: 340px;
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fff;
}
.cus-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.92rem;
}
.cus-table th {
  position: sticky;
  top: 0;
  padding: 0.75rem 1rem;
  background: #f9fafb;
  border-bottom: 1px solid var(--line);
  text-align: left;
  font-weight: 600;
  color: #4b5563;
  white-space: nowrap;
}
.cus-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f2f4;
  white-space: nowrap;
}
.cus-table tbody tr:last-child td {
  border-bottom: 0;
}
.cus-table tbody tr:hover {
  background: #fff7f7;
}
.cus-table tr.selected {
  background: var(--red-soft);
}
.cus-table input[type="checkbox"] {
  width: 17px;
  height: 17px;
  accent-color: var(--red);
  cursor: pointer;
}
.w-check {
  width: 44px;
}
.right {
  text-align: right !important;
}
.muted {
  color: var(--muted);
}
.strong {
  font-weight: 700;
}
.empty {
  padding: 2rem 1rem !important;
  text-align: center;
  color: var(--muted);
}

/* ---- Nút ---- */
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
.btn-cancel,
.btn-save {
  padding: 0.7rem 1.6rem;
  border-radius: 8px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
}
.btn-cancel {
  border: 1px solid var(--line);
  background: #fff;
  color: #374151;
}
.btn-cancel:hover {
  border-color: #9ca3af;
}
.btn-save {
  border: 1px solid var(--red);
  background: var(--red);
  color: #fff;
}
.btn-save:hover {
  background: #a80000;
}
.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 720px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .radio-row {
    flex-direction: column;
    gap: 0.75rem;
  }
}

.field input.locked,
.field input[readonly] {
  background: #f3f4f6;
  color: var(--muted);
  cursor: not-allowed;
}
.field input.locked:focus {
  border-color: var(--line);
  box-shadow: none;
}
</style>
