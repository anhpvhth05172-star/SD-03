<template>
  <div class="card dot-card border-0 shadow-sm">
    <div class="card-body p-4">
      <div class="d-flex align-items-center gap-3 mb-4">
        <div class="dot-icon">
          <i class="bi bi-percent"></i>
        </div>
        <h5 class="mb-0 fw-semibold">Thêm đợt giảm giá</h5>
      </div>

      <form @submit.prevent="submit()" novalidate>
        <div class="row g-4">
          <!-- Mã đợt -->
          <div class="col-md-6">
            <label class="form-label">
              Mã đợt <span class="text-danger">*</span>
            </label>
            <div class="input-group has-validation">
              <input
                v-model.trim="form.code"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.code }"
                maxlength="50"
                placeholder="Nhập mã đợt giảm giá"
              />
              <button
                type="button"
                class="btn btn-light border"
                title="Tạo mã ngẫu nhiên"
                @click="generateCode"
              >
                <i class="bi bi-arrow-repeat"></i>
              </button>
              <div class="invalid-feedback">{{ errors.code }}</div>
            </div>
          </div>

          <!-- Tên đợt -->
          <div class="col-md-6">
            <label class="form-label">
              Tên đợt <span class="text-danger">*</span>
            </label>
            <input
              v-model.trim="form.name"
              type="text"
              class="form-control"
              :class="{ 'is-invalid': errors.name }"
              maxlength="200"
              placeholder="VD: Summer Sale 2026"
            />
            <div class="invalid-feedback">{{ errors.name }}</div>
          </div>

          <!-- Phần trăm giảm -->
          <div class="col-md-4">
            <label class="form-label">
              Phần trăm giảm (%) <span class="text-danger">*</span>
            </label>
            <div class="input-group has-validation">
              <input
                v-model.number="form.percent"
                type="number"
                min="1"
                max="100"
                class="form-control"
                :class="{ 'is-invalid': errors.percent }"
                placeholder="VD: 15"
              />
              <span class="input-group-text">%</span>
              <div class="invalid-feedback">{{ errors.percent }}</div>
            </div>
          </div>

          <!-- Ngày bắt đầu -->
          <div class="col-md-4">
            <label class="form-label">
              Ngày bắt đầu <span class="text-danger">*</span>
            </label>
            <input
              v-model="form.startDate"
              type="datetime-local"
              class="form-control"
              :class="{ 'is-invalid': errors.startDate }"
            />
            <div class="invalid-feedback">{{ errors.startDate }}</div>
          </div>

          <!-- Ngày kết thúc -->
          <div class="col-md-4">
            <label class="form-label">
              Ngày kết thúc <span class="text-danger">*</span>
            </label>
            <input
              v-model="form.endDate"
              type="datetime-local"
              :min="form.startDate"
              class="form-control"
              :class="{ 'is-invalid': errors.endDate }"
            />
            <div class="invalid-feedback">{{ errors.endDate }}</div>
          </div>

          <!-- Mô tả -->
          <div class="col-12">
            <label class="form-label">Mô tả</label>
            <textarea
              v-model.trim="form.description"
              class="form-control"
              rows="2"
              maxlength="500"
              placeholder="Mô tả ngắn về đợt giảm giá"
            ></textarea>
          </div>

          <!-- Chọn sản phẩm -->
          <div class="col-12">
            <div
              class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2"
            >
              <label class="form-label mb-0">
                Sản phẩm áp dụng <span class="text-danger">*</span>
                <span class="badge bg-dot-soft ms-2">
                  Đã chọn: {{ form.productIds.length }}
                </span>
              </label>
              <div class="form-check mb-0">
                <input
                  id="select-all"
                  class="form-check-input"
                  type="checkbox"
                  :checked="allFilteredSelected"
                  :disabled="!filteredProducts.length"
                  @change="toggleAll($event.target.checked)"
                />
                <label class="form-check-label small" for="select-all">
                  Chọn tất cả (kết quả đang lọc)
                </label>
              </div>
            </div>

            <input
              v-model.trim="keyword"
              type="text"
              class="form-control mb-2"
              :class="{ 'is-invalid': errors.productIds }"
              placeholder="Tìm theo tên sản phẩm, mã sản phẩm, mã chi tiết, màu, size..."
            />
            <div class="invalid-feedback d-block mb-2" v-if="errors.productIds">
              {{ errors.productIds }}
            </div>

            <div class="product-list border rounded-3">
              <div v-if="loadingProducts" class="text-center text-muted py-4">
                <span class="spinner-border spinner-border-sm me-2"></span>
                Đang tải sản phẩm...
              </div>
              <div
                v-else-if="!groups.length"
                class="text-center text-muted py-4"
              >
                Không có sản phẩm phù hợp
              </div>

              <table v-else class="table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th style="width: 44px"></th>
                    <th>Sản phẩm / Mã chi tiết</th>
                    <th>Size</th>
                    <th>Màu</th>
                    <th class="text-end">Giá bán</th>
                    <th class="text-end">Giá sau giảm</th>
                    <th class="text-end">Tồn</th>
                  </tr>
                </thead>
                <tbody>
                  <template v-for="g in groups" :key="g.sanPhamId">
                    <!-- Dòng nhóm: chọn cả sản phẩm -->
                    <tr class="group-row">
                      <td>
                        <input
                          class="form-check-input"
                          type="checkbox"
                          :checked="groupState(g) === 'all'"
                          :indeterminate.prop="groupState(g) === 'some'"
                          @change="toggleGroup(g, $event.target.checked)"
                        />
                      </td>
                      <td colspan="6">
                        <span class="fw-semibold">{{ g.tenSanPham }}</span>
                        <span class="text-muted small ms-2">
                          {{ g.maSanPham }} · {{ g.items.length }} biến thể
                        </span>
                      </td>
                    </tr>
                    <!-- Các biến thể -->
                    <tr v-for="p in g.items" :key="p.id" class="variant-row">
                      <td>
                        <input
                          v-model="form.productIds"
                          :value="p.id"
                          class="form-check-input"
                          type="checkbox"
                        />
                      </td>
                      <td class="ps-4 small">{{ p.maChiTiet }}</td>
                      <td>{{ p.kichCo }}</td>
                      <td>{{ p.mauSac }}</td>
                      <td class="text-end">{{ money(p.giaBan) }}</td>
                      <td class="text-end">
                        <template v-if="validPercent">
                          <span class="text-dot fw-semibold">
                            {{ money(afterDiscount(p.giaBan)) }}
                          </span>
                        </template>
                        <span v-else class="text-muted">—</span>
                      </td>
                      <td class="text-end">{{ p.soLuong }}</td>
                    </tr>
                  </template>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <hr class="my-4" />

        <div class="d-flex gap-2">
          <button type="submit" class="btn btn-dot px-4" :disabled="saving">
            <span
              v-if="saving"
              class="spinner-border spinner-border-sm me-2"
            ></span>
            <i v-else class="bi bi-floppy me-2"></i>
            {{ saving ? "Đang lưu..." : "Tạo đợt giảm giá" }}
          </button>
          <button
            type="button"
            class="btn btn-light border px-4"
            :disabled="saving"
            @click="router.back()"
          >
            Hủy
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, inject, ref, onMounted } from "vue";
import axios from "axios";
import Swal from "sweetalert2";
import { useRouter } from "vue-router";

const router = useRouter();
const baseAPI = inject("baseAPI");
const saving = ref(false);

const randomCode = () => {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789";
  let code = "DGG";
  for (let i = 0; i < 6; i++) {
    code += chars[Math.floor(Math.random() * chars.length)];
  }
  return code;
};

const form = reactive({
  code: randomCode(),
  name: "",
  percent: null,
  startDate: "",
  endDate: "",
  description: "",
  productIds: [], // id của san_pham_chi_tiet
});

const errors = reactive({});

const generateCode = () => {
  form.code = randomCode();
};

/* ---------------- Sản phẩm ---------------- */
const products = ref([]);
const loadingProducts = ref(true);
const keyword = ref("");

const loadProducts = async () => {
  try {
    // ⚠️ Đổi endpoint cho khớp backend (xem controller bên dưới)
    const res = await axios.get(
      baseAPI + "admin/dot-giam-gia/san-pham-chi-tiet/for-discount",
    );
    products.value = res.data?.data ?? res.data ?? [];
  } catch (e) {
    Swal.fire({
      icon: "error",
      title: "Không tải được danh sách sản phẩm",
      text: e.response?.data?.message || "Không kết nối được máy chủ",
      confirmButtonColor: "#e8284a",
    });
  } finally {
    loadingProducts.value = false;
  }
};

onMounted(loadProducts);

const filteredProducts = computed(() => {
  const kw = keyword.value.toLowerCase();
  if (!kw) return products.value;
  return products.value.filter((p) =>
    [p.tenSanPham, p.maSanPham, p.maChiTiet, p.kichCo, p.mauSac].some((v) =>
      String(v ?? "")
        .toLowerCase()
        .includes(kw),
    ),
  );
});

// Gom biến thể theo sản phẩm (giữ thứ tự từ server)
const groups = computed(() => {
  const map = new Map();
  for (const p of filteredProducts.value) {
    if (!map.has(p.sanPhamId)) {
      map.set(p.sanPhamId, {
        sanPhamId: p.sanPhamId,
        maSanPham: p.maSanPham,
        tenSanPham: p.tenSanPham,
        items: [],
      });
    }
    map.get(p.sanPhamId).items.push(p);
  }
  return [...map.values()];
});

const groupState = (g) => {
  const selected = g.items.filter((p) => form.productIds.includes(p.id)).length;
  if (selected === 0) return "none";
  return selected === g.items.length ? "all" : "some";
};

const toggleGroup = (g, checked) => {
  const ids = g.items.map((p) => p.id);
  form.productIds = checked
    ? [...new Set([...form.productIds, ...ids])]
    : form.productIds.filter((id) => !ids.includes(id));
};

const allFilteredSelected = computed(
  () =>
    filteredProducts.value.length > 0 &&
    filteredProducts.value.every((p) => form.productIds.includes(p.id)),
);

const toggleAll = (checked) => {
  const ids = filteredProducts.value.map((p) => p.id);
  form.productIds = checked
    ? [...new Set([...form.productIds, ...ids])]
    : form.productIds.filter((id) => !ids.includes(id));
};

/* ---------------- Giá ---------------- */
const validPercent = computed(() => form.percent > 0 && form.percent <= 100);

// Phải giống công thức backend: làm tròn về đồng (HALF_UP)
const afterDiscount = (giaBan) =>
  Math.round((Number(giaBan) * (100 - form.percent)) / 100);

const money = (v) => Number(v ?? 0).toLocaleString("vi-VN") + "đ";

/* ---------------- Validate + Submit ---------------- */
const validate = () => {
  Object.keys(errors).forEach((key) => delete errors[key]);

  if (!form.code) errors.code = "Vui lòng nhập mã đợt";
  if (!form.name) errors.name = "Vui lòng nhập tên đợt";

  if (!validPercent.value) {
    errors.percent = "Phần trăm giảm phải từ 1 đến 100";
  }

  if (!form.startDate) errors.startDate = "Vui lòng chọn ngày bắt đầu";

  if (!form.endDate) {
    errors.endDate = "Vui lòng chọn ngày kết thúc";
  } else if (form.startDate && form.endDate <= form.startDate) {
    errors.endDate = "Ngày kết thúc phải sau ngày bắt đầu";
  }

  if (form.productIds.length === 0) {
    errors.productIds = "Vui lòng chọn ít nhất 1 sản phẩm";
  }

  return Object.keys(errors).length === 0;
};

const toLocalDateTime = (v) => (v && v.length === 16 ? v + ":00" : v);

const submit = async () => {
  if (!validate()) return;

  const data = {
    maDotGiamGia: form.code,
    tenDotGiamGia: form.name,
    phanTramGiam: form.percent,
    ngayBatDau: toLocalDateTime(form.startDate),
    ngayKetThuc: toLocalDateTime(form.endDate),
    trangThai: true,
    moTa: form.description || null,
    danhSachSanPhamChiTietId: form.productIds,
  };

  saving.value = true;
  try {
    await axios.post(baseAPI + "admin/dot-giam-gia/add", data);

    await Swal.fire({
      icon: "success",
      title: "Tạo đợt giảm giá thành công",
      timer: 1500,
      showConfirmButton: false,
    });
    router.back();
  } catch (e) {
    Swal.fire({
      icon: "error",
      title: "Tạo thất bại",
      text: e.response?.data?.message || "Không kết nối được máy chủ",
      confirmButtonColor: "#e8284a",
    });
  } finally {
    saving.value = false;
  }
};
</script>

<style scoped>
.dot-card {
  border-radius: 1.25rem;
}
.dot-icon {
  width: 44px;
  height: 44px;
  border-radius: 0.75rem;
  background: #fde8ec;
  color: #e8284a;
  display: grid;
  place-items: center;
  font-size: 1.15rem;
}
.form-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: #4b5563;
}
.form-control,
.input-group-text {
  background-color: #f8f9fb;
  padding-top: 0.7rem;
  padding-bottom: 0.7rem;
}
.form-control:focus {
  background-color: #fff;
  border-color: #e8284a;
  box-shadow: 0 0 0 0.2rem rgba(232, 40, 74, 0.15);
}
.form-check-input:checked,
.form-check-input:indeterminate {
  background-color: #e8284a;
  border-color: #e8284a;
}
.form-check-input:focus {
  border-color: #e8284a;
  box-shadow: 0 0 0 0.2rem rgba(232, 40, 74, 0.15);
}
.btn-dot {
  background-color: #e8284a;
  border-color: #e8284a;
  color: #fff;
  font-weight: 600;
}
.btn-dot:hover,
.btn-dot:focus {
  background-color: #cf1f3f;
  border-color: #cf1f3f;
  color: #fff;
}
.bg-dot-soft {
  background: #fde8ec;
  color: #e8284a;
}
.text-dot {
  color: #e8284a;
}

/* Bảng sản phẩm */
.product-list {
  max-height: 360px;
  overflow-y: auto;
  background: #fff;
}
.product-list thead th {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #f8f9fb;
  font-size: 0.78rem;
  font-weight: 600;
  color: #6b7280;
}
.group-row td {
  background: #fff5f7;
  border-top: 1px solid #f3d3da;
}
.variant-row:hover td {
  background: #fafafa;
}
</style>
