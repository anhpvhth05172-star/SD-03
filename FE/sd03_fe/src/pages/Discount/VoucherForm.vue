<script setup>
import {
  reactive,
  computed,
  inject,
  ref,
  watch,
  onMounted,
  onUnmounted,
} from "vue";
import axios from "axios";
import Swal from "sweetalert2";
import { useRouter } from "vue-router";
import logo from "@/assets/logo.png";
import SearchableSelect from "@/components/SearchableSelect.vue";
import { API_BASE } from "@/api/http";

const router = useRouter();
const baseAPI = inject("baseAPI");
const saving = ref(false);

// ⚠️ Đổi cho đúng đường dẫn trang danh sách đợt giảm giá của bạn
const LIST_PATH = "/admin/dot-giam-gia";

/* Mã đợt: ảnh không có ô nhập nên tự sinh, backend vẫn nhận maDotGiamGia */
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
  productIds: [], // id của san_pham_chi_tiet (biến thể) được chọn
});

/* ---------------- Thời điểm hiện tại (định dạng giống datetime-local) ---------------- */
const pad = (n) => String(n).padStart(2, "0");
const nowLocal = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}`;
};

// Cập nhật định kỳ để việc kiểm tra "quá khứ" luôn đúng khi form mở lâu
const nowStr = ref(nowLocal());
let nowTimer = null;
onMounted(() => {
  nowTimer = setInterval(() => (nowStr.value = nowLocal()), 30 * 1000);
});
onUnmounted(() => clearInterval(nowTimer));

/* ---------------- Giá ---------------- */
const validPercent = computed(() => form.percent >= 1 && form.percent <= 100);

// Mức giảm thực tế = max(% đợt mới, % đợt khác đang áp dụng cho biến thể)
const effectivePercent = (v) =>
  Math.max(Number(form.percent) || 0, Number(v.phanTramGiamKhac) || 0);

// Phải giống công thức backend: làm tròn về đồng
const afterDiscount = (v) =>
  Math.round((Number(v.giaBan) * (100 - effectivePercent(v))) / 100);

// Giá hiện tại: giá đang áp dụng theo đợt khác (nếu có), tính trên giá gốc
const currentPrice = (v) =>
  Math.round(
    (Number(v.giaBan) * (100 - (Number(v.phanTramGiamKhac) || 0))) / 100,
  );

const money = (v) => Number(v ?? 0).toLocaleString("vi-VN") + "đ";

/* ---------------- Ảnh biến thể ---------------- */
const PLACEHOLDER =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(
    `<svg xmlns='http://www.w3.org/2000/svg' width='56' height='56' viewBox='0 0 24 24' fill='none' stroke='#9ca3af' stroke-width='1.5'><rect width='24' height='24' fill='#f3f4f6' stroke='none'/><rect x='4' y='5' width='16' height='14' rx='2'/><circle cx='9' cy='10' r='1.5'/><path d='M4 17l5-4 4 3 3-2 4 3'/></svg>`,
  );

const onImgError = (e) => {
  e.target.onerror = null;
  e.target.src = PLACEHOLDER;
};

/* ---------------- Validate (hiện lỗi ngay khi người dùng thao tác) ---------------- */
const submitted = ref(false);
const touched = reactive({});
const touch = (key) => (touched[key] = true);

const selectedProductIds = ref([]); // id sản phẩm (cha) đang tick

// Giá trị thay đổi là hiện lỗi ngay, không cần blur hay submit
watch(
  () => form.name,
  () => touch("name"),
);
watch(
  () => form.percent,
  () => touch("percent"),
);
watch(
  () => form.startDate,
  () => touch("startDate"),
);
watch(
  () => form.endDate,
  () => touch("endDate"),
);
watch([selectedProductIds, () => form.productIds], () => touch("productIds"), {
  deep: true,
});

const errors = computed(() => {
  const e = {};

  if (!form.name) e.name = "Vui lòng nhập tên đợt giảm giá.";

  if (form.percent === null || form.percent === "")
    e.percent = "Vui lòng nhập mức giảm (%).";
  else if (!validPercent.value)
    e.percent = "Mức giảm (%) phải nằm trong khoảng từ 1% đến 100%.";

  // Ngày bắt đầu
  if (!form.startDate) e.startDate = "Vui lòng chọn ngày bắt đầu.";
  else if (form.startDate < nowStr.value)
    e.startDate = "Ngày bắt đầu không được ở trong quá khứ.";

  // Ngày kết thúc
  if (!form.endDate) e.endDate = "Vui lòng chọn ngày kết thúc.";
  else if (form.endDate < nowStr.value)
    e.endDate = "Ngày kết thúc không được ở trong quá khứ.";
  else if (form.startDate && form.endDate < form.startDate)
    e.endDate = "Ngày kết thúc không được nhỏ hơn ngày bắt đầu.";

  if (selectedProductIds.value.length === 0)
    e.productIds = "Vui lòng chọn ít nhất 1 sản phẩm.";
  else if (form.productIds.length === 0)
    e.productIds = "Vui lòng chọn ít nhất 1 biến thể để áp dụng.";
  return e;
});

const err = (key) => (submitted.value || touched[key]) && errors.value[key];

/* ---------------- Dữ liệu sản phẩm ---------------- */
const products = ref([]); // danh sách biến thể (phẳng) từ server
const loadingProducts = ref(true);
const thuongHieuOptions = ref([]);
const chatLieuOptions = ref([]);
const filterThuongHieu = ref("");
const filterChatLieu = ref("");

const loadFilterOptions = async () => {
  try {
    const r = await fetch(`${API_BASE}/v1/attributes/all-filter-options`);
    if (!r.ok) return;
    const d = await r.json();
    const norm = (x) => (Array.isArray(x) ? x : Array.isArray(x?.content) ? x.content : Array.isArray(x?.data) ? x.data : []);
    thuongHieuOptions.value = norm(d?.thuongHieu);
    chatLieuOptions.value = norm(d?.chatLieu);
  } catch {
    /* bỏ qua, không lọc được vẫn chọn bộ lọc khác */
  }
};

const loadProducts = async () => {
  try {
    loadingProducts.value = true;
    // Backend trả thêm phanTramGiamKhac + tenDotGiamGiaKhac (mức CAO NHẤT trong
    // các đợt đang hiệu lực) cho biến thể đang thuộc đợt giảm giá khác
    const params = {};
    if (filterThuongHieu.value) params.idThuongHieu = filterThuongHieu.value;
    if (filterChatLieu.value) params.idChatLieu = filterChatLieu.value;
    const res = await axios.get(
      baseAPI + "admin/dot-giam-gia/san-pham-chi-tiet/for-discount",
      { params },
    );
    const raw = res.data?.data ?? res.data ?? [];

    // Gộp theo id biến thể, giữ dòng có % đợt khác cao nhất
    const byId = new Map();
    for (const p of raw) {
      const old = byId.get(p.id);
      if (
        !old ||
        (Number(p.phanTramGiamKhac) || 0) > (Number(old.phanTramGiamKhac) || 0)
      ) {
        byId.set(p.id, p);
      }
    }
    products.value = [...byId.values()];
  } catch (e) {
    Swal.fire({
      icon: "error",
      title: "Không tải được danh sách sản phẩm",
      text: e.response?.data?.message || "Không kết nối được máy chủ",
      confirmButtonColor: "#cc0000",
    });
  } finally {
    loadingProducts.value = false;
  }
};
onMounted(() => {
  loadProducts();
  loadFilterOptions();
});

/* ---------------- Bảng "Chọn sản phẩm" (gom biến thể theo sản phẩm) ---------------- */
const productKeyword = ref("");

const productList = computed(() => {
  const map = new Map();
  for (const p of products.value) {
    if (!map.has(p.sanPhamId)) {
      map.set(p.sanPhamId, {
        id: p.sanPhamId,
        code: p.maSanPham,
        name: p.tenSanPham,
        variantIds: [],
      });
    }
    map.get(p.sanPhamId).variantIds.push(p.id);
  }
  return [...map.values()];
});

const filteredProductList = computed(() => {
  const kw = productKeyword.value.trim().toLowerCase();
  if (!kw) return productList.value;
  return productList.value.filter((g) =>
    [g.name, g.code].some((v) =>
      String(v ?? "")
        .toLowerCase()
        .includes(kw),
    ),
  );
});

const allProductsSelected = computed(
  () =>
    filteredProductList.value.length > 0 &&
    filteredProductList.value.every((g) =>
      selectedProductIds.value.includes(g.id),
    ),
);

// Tick sản phẩm => tự chọn tất cả biến thể của nó; bỏ tick => bỏ hết biến thể
const setProductChecked = (list, checked) => {
  const pids = list.map((g) => g.id);
  const vids = list.flatMap((g) => g.variantIds);
  if (checked) {
    selectedProductIds.value = [
      ...new Set([...selectedProductIds.value, ...pids]),
    ];
    form.productIds = [...new Set([...form.productIds, ...vids])];
  } else {
    selectedProductIds.value = selectedProductIds.value.filter(
      (id) => !pids.includes(id),
    );
    form.productIds = form.productIds.filter((id) => !vids.includes(id));
  }
};

/* ---------------- Bảng "Chi tiết biến thể" ---------------- */
const variants = computed(() =>
  products.value.filter((p) => selectedProductIds.value.includes(p.sanPhamId)),
);

const variantKeyword = ref("");
const sizeFilter = ref("");
const colorFilter = ref("");

const sizeOptions = computed(() => [
  ...new Set(variants.value.map((v) => v.kichCo).filter(Boolean)),
]);
const colorOptions = computed(() => [
  ...new Set(variants.value.map((v) => v.mauSac).filter(Boolean)),
]);

const filteredVariants = computed(() => {
  const kw = variantKeyword.value.trim().toLowerCase();
  return variants.value.filter((v) => {
    if (sizeFilter.value && v.kichCo !== sizeFilter.value) return false;
    if (colorFilter.value && v.mauSac !== colorFilter.value) return false;
    if (!kw) return true;
    return [v.tenSanPham, v.maChiTiet, v.kichCo, v.mauSac].some((x) =>
      String(x ?? "")
        .toLowerCase()
        .includes(kw),
    );
  });
});

const allVariantsSelected = computed(
  () =>
    filteredVariants.value.length > 0 &&
    filteredVariants.value.every((v) => form.productIds.includes(v.id)),
);

const toggleAllVariants = (checked) => {
  const ids = filteredVariants.value.map((v) => v.id);
  form.productIds = checked
    ? [...new Set([...form.productIds, ...ids])]
    : form.productIds.filter((id) => !ids.includes(id));
};

const resetVariantFilters = () => {
  variantKeyword.value = "";
  sizeFilter.value = "";
  colorFilter.value = "";
  const hadServerFilter = !!(filterThuongHieu.value || filterChatLieu.value);
  filterThuongHieu.value = "";
  filterChatLieu.value = "";
  if (hadServerFilter) loadProducts();
};

/* ---------------- Submit ---------------- */
const toLocalDateTime = (v) => (v && v.length === 16 ? v + ":00" : v);

const submit = async () => {
  submitted.value = true;
  nowStr.value = nowLocal(); // lấy lại thời điểm hiện tại để kiểm tra chính xác
  if (Object.keys(errors.value).length) return;

  const data = {
    maDotGiamGia: form.code,
    tenDotGiamGia: form.name,
    phanTramGiam: form.percent,
    ngayBatDau: toLocalDateTime(form.startDate),
    ngayKetThuc: toLocalDateTime(form.endDate),
    trangThai: true,
    moTa: null,
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
    router.push(LIST_PATH);
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
  <div class="dg-screen">
    <!-- Đầu trang -->
    <header class="dg-head">
      <h1 class="dg-title">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path
            d="M21.4 11.6l-9-9A2 2 0 0 0 11 2H4a2 2 0 0 0-2 2v7c0 .55.22 1.05.59 1.42l9 9a2 2 0 0 0 2.82 0l7-7a2 2 0 0 0 0-2.84zM5.5 7a1.5 1.5 0 1 1 0-3 1.5 1.5 0 0 1 0 3z"
          />
        </svg>
        Thêm mới đợt giảm giá
      </h1>
      <RouterLink :to="LIST_PATH" class="dg-back">
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
      <!-- ============ Hàng trên: Thông tin chung | Chọn sản phẩm ============ -->
      <div class="dg-top">
        <!-- Thông tin chung -->
        <section class="dg-card">
          <h2 class="dg-card-title">
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

          <div class="dg-field">
            <label>Tên đợt giảm giá <b class="dg-req">*</b></label>
            <input
              v-model.trim="form.name"
              type="text"
              maxlength="200"
              placeholder="Nhập tên đợt giảm giá..."
              :class="{ 'dg-invalid': err('name') }"
              @blur="touch('name')"
            />
            <p v-if="err('name')" class="dg-err">
              <i class="dg-err-ico">!</i>{{ errors.name }}
            </p>
          </div>

          <div class="dg-field">
            <label>Giá trị giảm (%) <b class="dg-req">*</b></label>
            <div class="dg-affix" :class="{ 'dg-invalid': err('percent') }">
              <input
                v-model.number="form.percent"
                type="number"
                min="1"
                max="100"
                placeholder="Nhập %..."
                @blur="touch('percent')"
              />
              <span class="dg-affix-text">%</span>
            </div>
            <p v-if="err('percent')" class="dg-err">
              <i class="dg-err-ico">!</i>{{ errors.percent }}
            </p>
          </div>

          <div class="dg-field">
            <label>Ngày bắt đầu <b class="dg-req">*</b></label>
            <input
              v-model="form.startDate"
              type="datetime-local"
              :min="nowStr"
              :class="{ 'dg-invalid': err('startDate') }"
              @blur="touch('startDate')"
            />
            <p v-if="err('startDate')" class="dg-err">
              <i class="dg-err-ico">!</i>{{ errors.startDate }}
            </p>
          </div>

          <div class="dg-field">
            <label>Ngày kết thúc <b class="dg-req">*</b></label>
            <input
              v-model="form.endDate"
              type="datetime-local"
              :min="
                form.startDate && form.startDate > nowStr
                  ? form.startDate
                  : nowStr
              "
              :class="{ 'dg-invalid': err('endDate') }"
              @blur="touch('endDate')"
            />
            <p v-if="err('endDate')" class="dg-err">
              <i class="dg-err-ico">!</i>{{ errors.endDate }}
            </p>
          </div>
        </section>

        <!-- Chọn sản phẩm -->
        <section class="dg-card">
          <h2 class="dg-card-title">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3z" />
              <path d="M4 7.5l8 4.5 8-4.5M12 12v9" />
            </svg>
            Chọn Sản Phẩm
          </h2>

          <div class="dg-search">
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
              v-model="productKeyword"
              type="text"
              placeholder="Tìm kiếm sản phẩm theo tên..."
            />
          </div>
          <p v-if="err('productIds')" class="dg-err dg-err-block">
            <i class="dg-err-ico">!</i>{{ errors.productIds }}
          </p>

          <div class="dg-table-wrap dg-table-products">
            <table class="dg-table dg-table-fixed">
              <thead>
                <tr>
                  <th class="dg-w-check">
                    <input
                      type="checkbox"
                      aria-label="Chọn tất cả sản phẩm"
                      :checked="allProductsSelected"
                      :disabled="!filteredProductList.length"
                      @change="
                        setProductChecked(
                          filteredProductList,
                          $event.target.checked,
                        )
                      "
                    />
                  </th>
                  <th class="dg-w-stt">STT</th>
                  <th>Tên sản phẩm</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loadingProducts">
                  <td colspan="3" class="dg-empty">Đang tải sản phẩm...</td>
                </tr>
                <tr v-else-if="!filteredProductList.length">
                  <td colspan="3" class="dg-empty">
                    Không có sản phẩm phù hợp
                  </td>
                </tr>
                <tr
                  v-for="(g, i) in filteredProductList"
                  v-else
                  :key="g.id"
                  :class="{ 'dg-selected': selectedProductIds.includes(g.id) }"
                >
                  <td class="dg-w-check">
                    <input
                      type="checkbox"
                      :checked="selectedProductIds.includes(g.id)"
                      @change="setProductChecked([g], $event.target.checked)"
                    />
                  </td>
                  <td>{{ i + 1 }}</td>
                  <td class="dg-strong">{{ g.name }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <!-- ============ Chi tiết biến thể ============ -->
      <section class="dg-card">
        <h2 class="dg-card-title dg-no-line">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
          >
            <path d="M8 6h12M8 12h12M8 18h12M3.5 6h.01M3.5 12h.01M3.5 18h.01" />
          </svg>
          Chi Tiết Biến Thể
          <span class="dg-count"
            >Đã chọn: {{ form.productIds.length }}/{{ variants.length }}</span
          >
        </h2>

        <div class="dg-filter-row">
          <div class="dg-field">
            <label>Tìm kiếm chi tiết</label>
            <input
              v-model="variantKeyword"
              type="text"
              class="dg-pill"
              placeholder="Nhập tên biến thể..."
            />
          </div>
          <div class="dg-field">
            <label>Kích cỡ</label>
            <select v-model="sizeFilter" class="dg-round">
              <option value="">Tất cả</option>
              <option v-for="s in sizeOptions" :key="s" :value="s">
                {{ s }}
              </option>
            </select>
          </div>
          <div class="dg-field">
            <label>Màu sắc</label>
            <select v-model="colorFilter" class="dg-round">
              <option value="">Tất cả</option>
              <option v-for="c in colorOptions" :key="c" :value="c">
                {{ c }}
              </option>
            </select>
          </div>
          <div class="dg-field">
            <label>Thương hiệu</label>
            <SearchableSelect
              v-model="filterThuongHieu"
              :options="thuongHieuOptions"
              label-key="ten"
              value-key="id"
              placeholder="Tất cả thương hiệu"
              search-placeholder="Tìm thương hiệu..."
              @change="loadProducts"
            />
          </div>
          <div class="dg-field">
            <label>Chất liệu</label>
            <SearchableSelect
              v-model="filterChatLieu"
              :options="chatLieuOptions"
              label-key="ten"
              value-key="id"
              placeholder="Tất cả chất liệu"
              search-placeholder="Tìm chất liệu..."
              @change="loadProducts"
            />
          </div>
          <button type="button" class="dg-reset" @click="resetVariantFilters">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
            Đặt lại
          </button>
        </div>

        <div class="dg-table-wrap dg-table-variants">
          <table class="dg-table">
            <thead>
              <tr>
                <th class="dg-w-check">
                  <input
                    type="checkbox"
                    aria-label="Chọn tất cả biến thể"
                    :checked="allVariantsSelected"
                    :disabled="!filteredVariants.length"
                    @change="toggleAllVariants($event.target.checked)"
                  />
                </th>
                <th class="dg-center dg-w-stt">STT</th>
                <th class="dg-center dg-w-img">Ảnh</th>
                <th>Sản phẩm</th>
                <th>Mã chi tiết</th>
                <th class="dg-center">Kích cỡ</th>
                <th class="dg-center">Màu sắc</th>
                <th>Thương hiệu</th>
                <th>Chất liệu</th>
                <th class="dg-right">Giá bán</th>
                <th class="dg-right">Giá sau giảm</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="!variants.length">
                <td colspan="11" class="dg-empty">
                  Chọn sản phẩm ở trên để xem các biến thể.
                </td>
              </tr>
              <tr v-else-if="!filteredVariants.length">
                <td colspan="11" class="dg-empty">Không có biến thể phù hợp</td>
              </tr>
              <tr
                v-for="(v, i) in filteredVariants"
                v-else
                :key="v.id"
                :class="{ 'dg-selected': form.productIds.includes(v.id) }"
              >
                <td class="dg-w-check">
                  <input
                    v-model="form.productIds"
                    type="checkbox"
                    :value="v.id"
                  />
                </td>
                <td class="dg-center">{{ i + 1 }}</td>
                <td class="dg-center">
                  <div class="dg-img-wrap">
                    <img
                      :src="logo"
                      :alt="v.tenSanPham"
                      class="dg-thumb"
                      loading="lazy"
                      @error="onImgError"
                    />
                    <!-- Biến thể đã thuộc đợt giảm giá khác đang hiệu lực -->
                    <span
                      v-if="v.phanTramGiamKhac"
                      class="dg-badge"
                      :title="`Đang thuộc đợt: ${v.tenDotGiamGiaKhac} (${v.phanTramGiamKhac}%). Khi trùng sẽ lấy mức cao hơn.`"
                    >
                      -{{ v.phanTramGiamKhac }}%
                    </span>
                  </div>
                </td>
                <td class="dg-strong">{{ v.tenSanPham }}</td>
                <td class="dg-muted">{{ v.maChiTiet }}</td>
                <td class="dg-center">{{ v.kichCo }}</td>
                <td class="dg-center">{{ v.mauSac }}</td>
                <td>{{ v.tenThuongHieu }}</td>
                <td>{{ v.chatLieu }}</td>
                <td class="dg-right">{{ money(v.giaBan) }}</td>
                <td class="dg-right">
                  <template v-if="validPercent">
                    <span class="dg-sale">{{ money(afterDiscount(v)) }}</span>
                    <small v-if="v.phanTramGiamKhac" class="dg-note">
                      Áp dụng {{ effectivePercent(v) }}%
                      {{
                        v.phanTramGiamKhac > form.percent
                          ? "(giữ mức đợt khác)"
                          : "(cao hơn đợt khác)"
                      }}
                    </small>
                  </template>
                  <span v-else class="dg-muted">—</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Nút thao tác -->
      <div class="dg-actions">
        <RouterLink :to="LIST_PATH" class="dg-btn-cancel">Hủy</RouterLink>
        <button type="submit" class="dg-btn-save" :disabled="saving">
          {{ saving ? "Đang lưu..." : "Lưu đợt giảm giá" }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
/* Dùng tiền tố dg- để không bị trùng class với Bootstrap (card, col-*, badge...) */
.dg-screen {
  --red: #cc0000;
  --red-soft: #fdecec;
  --line: #e5e7eb;
  --ink: #111827;
  --muted: #6b7280;
  padding: 0.5rem 0.75rem 2rem;
  color: var(--ink);
}

/* ---- Đầu trang ---- */
.dg-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0 0.9rem;
  margin-bottom: 1.1rem;
  border-bottom: 1px solid var(--line);
}
.dg-title {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin: 0;
  font-size: 1.35rem;
  font-weight: 700;
}
.dg-title svg {
  width: 24px;
  height: 24px;
}
.dg-back {
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
.dg-back svg {
  width: 16px;
  height: 16px;
}
.dg-back:hover {
  border-color: var(--red);
  color: var(--red);
}

/* ---- Bố cục ---- */
.dg-top {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) 2fr;
  gap: 1.25rem;
  align-items: start;
  margin-bottom: 1.25rem;
}
.dg-card {
  background: #fff;
  border-radius: 14px;
  padding: 1.3rem 1.5rem 1.5rem;
  box-shadow: 0 1px 3px rgba(17, 24, 39, 0.06);
}
.dg-card-title {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin: 0 0 1.1rem;
  padding-bottom: 0.8rem;
  border-bottom: 1px solid var(--line);
  font-size: 1rem;
  font-weight: 700;
}
.dg-card-title.dg-no-line {
  border-bottom: 0;
  padding-bottom: 0;
}
.dg-card-title svg {
  width: 18px;
  height: 18px;
}
.dg-count {
  padding: 0.15rem 0.65rem;
  border-radius: 999px;
  background: #4b5563;
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
}

/* ---- Ô nhập ---- */
.dg-field {
  margin-bottom: 1rem;
}
.dg-field label {
  display: block;
  margin-bottom: 0.4rem;
  font-size: 0.88rem;
  font-weight: 600;
}
.dg-req {
  color: var(--red);
}
.dg-field input,
.dg-field select {
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
.dg-field input:focus,
.dg-field select:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}
.dg-field input.dg-invalid {
  border-color: var(--red);
}
.dg-field input.dg-pill {
  border-radius: 999px;
  padding-left: 1.1rem;
}
.dg-field select.dg-round {
  border-radius: 12px;
}

.dg-affix {
  display: flex;
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.dg-affix:focus-within {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}
.dg-affix.dg-invalid {
  border-color: var(--red);
}
.dg-affix input {
  border: 0 !important;
  border-radius: 0 !important;
  box-shadow: none !important;
}
.dg-affix-text {
  display: grid;
  place-items: center;
  min-width: 48px;
  background: #f3f4f6;
  border-left: 1px solid var(--line);
  color: var(--muted);
  font-weight: 600;
}

.dg-err {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin: 0.4rem 0 0;
  font-size: 0.82rem;
  color: var(--red);
}
.dg-err-block {
  margin: -0.4rem 0 0.8rem;
}
.dg-err-ico {
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

/* ---- Tìm kiếm ---- */
.dg-search {
  position: relative;
  max-width: 520px;
  margin-bottom: 1rem;
}
.dg-search svg {
  position: absolute;
  left: 14px;
  top: 50%;
  width: 18px;
  height: 18px;
  transform: translateY(-50%);
  color: var(--muted);
}
.dg-search input {
  width: 100%;
  padding: 0.65rem 1rem 0.65rem 2.7rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  font: inherit;
  outline: none;
}
.dg-search input:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

/* ---- Bảng ---- */
.dg-table-wrap {
  overflow: auto;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fff;
}
.dg-table-products {
  max-height: 440px;
}
.dg-table-variants {
  max-height: 420px;
}
.dg-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.92rem;
}
.dg-table th {
  position: sticky;
  top: 0;
  z-index: 1;
  padding: 0.8rem 1rem;
  background: #fff;
  border-bottom: 1px solid var(--line);
  text-align: left;
  font-weight: 700;
  white-space: nowrap;
}
.dg-table td {
  padding: 0.8rem 1rem;
  border-bottom: 1px solid #f1f2f4;
  white-space: nowrap;
}
.dg-table tbody tr:last-child td {
  border-bottom: 0;
}
.dg-table tbody tr:hover {
  background: #fff7f7;
}
.dg-table tr.dg-selected {
  background: var(--red-soft);
}
.dg-table input[type="checkbox"] {
  width: 17px;
  height: 17px;
  accent-color: var(--red);
  cursor: pointer;
}
.dg-w-check {
  width: 48px;
}
.dg-w-stt {
  width: 64px;
}
.dg-center {
  text-align: center !important;
}
.dg-right {
  text-align: right !important;
}
.dg-muted {
  color: var(--muted);
}
.dg-strong {
  font-weight: 700;
}
.dg-sale {
  color: var(--red);
  font-weight: 700;
}
.dg-note {
  display: block;
  margin-top: 0.15rem;
  font-size: 0.72rem;
  color: var(--muted);
}
.dg-empty {
  padding: 2rem 1rem !important;
  text-align: center;
  color: var(--muted);
}

/* ---- Bảng chọn sản phẩm: căn giữa, cột đều, to hơn ---- */
.dg-table-fixed {
  table-layout: fixed;
  font-size: 1rem;
}
.dg-table-fixed th,
.dg-table-fixed td {
  padding: 1rem 1rem;
  text-align: center;
  vertical-align: middle;
}
.dg-table-fixed .dg-w-check {
  width: 80px;
}
.dg-table-fixed .dg-w-stt {
  width: 100px;
}
.dg-table-fixed td.dg-strong {
  white-space: normal; /* tên dài tự xuống dòng, không tràn */
}
.dg-table-fixed input[type="checkbox"] {
  width: 20px;
  height: 20px;
}

/* ---- Ảnh biến thể + nhãn % đợt giảm giá khác ---- */
.dg-w-img {
  width: 84px;
}
.dg-img-wrap {
  position: relative;
  width: 56px;
  height: 56px;
  margin: 0 auto;
}
.dg-thumb {
  display: block;
  width: 56px;
  height: 56px;
  margin: 0 auto;
  object-fit: cover;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #f3f4f6;
}
.dg-badge {
  position: absolute;
  top: -4px;
  left: -6px;
  padding: 1px 6px;
  border-radius: 6px;
  background: var(--red);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  line-height: 1.4;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.25);
  white-space: nowrap;
  cursor: help;
}

/* ---- Bộ lọc biến thể ---- */
.dg-filter-row {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr auto;
  gap: 1.25rem;
  align-items: end;
  margin-bottom: 0.25rem;
}
.dg-reset {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  height: 44px;
  margin-bottom: 1rem;
  padding: 0 1.8rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  font: inherit;
  font-weight: 600;
  color: #374151;
  cursor: pointer;
}
.dg-reset svg {
  width: 16px;
  height: 16px;
}
.dg-reset:hover {
  border-color: var(--red);
  color: var(--red);
}

/* ---- Nút lưu ---- */
.dg-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}
.dg-btn-cancel,
.dg-btn-save {
  padding: 0.7rem 1.6rem;
  border-radius: 8px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
}
.dg-btn-cancel {
  border: 1px solid var(--line);
  background: #fff;
  color: #374151;
}
.dg-btn-cancel:hover {
  border-color: #9ca3af;
}
.dg-btn-save {
  border: 1px solid var(--red);
  background: var(--red);
  color: #fff;
}
.dg-btn-save:hover {
  background: #a80000;
}
.dg-btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 992px) {
  .dg-top {
    grid-template-columns: 1fr;
  }
  .dg-filter-row {
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 560px) {
  .dg-filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
