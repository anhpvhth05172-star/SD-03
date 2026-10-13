<script setup>
import axios from "axios";
import { computed, inject, onMounted, reactive, ref, watch } from "vue";

const apiDiscount = inject("baseAPI");

const listDiscount = ref([]);
const page = ref(0);
const pageSize = 8;
const totalPages = ref(0);
const totalElements = ref(0);

/* ---------------- Bộ lọc ---------------- */
const filter = reactive({
  keyword: "",
  discountType: "",
  status: "",
  scope: "",
  fromDate: "",
  toDate: "",
});

const hasFilter = computed(() => Object.values(filter).some((v) => v));

/* ---------------- Cache danh sách ban đầu ---------------- */
try {
  const cachedVouchers = localStorage.getItem("voucher_initial_list_cache");
  if (cachedVouchers) {
    const parsed = JSON.parse(cachedVouchers);
    if (parsed && Array.isArray(parsed.content)) {
      listDiscount.value = parsed.content;
      page.value = parsed.number || 0;
      totalPages.value = parsed.totalPages || 1;
      totalElements.value = parsed.totalElements || parsed.content.length;
    }
  }
} catch (e) {}

/* ---------------- Tải dữ liệu ---------------- */
const getData = async (p = 0) => {
  try {
    const res = await axios.get(apiDiscount + "admin/phieu-giam-gia", {
      params: {
        page: p,
        size: pageSize,
        // ⚠️ đổi tên param cho khớp backend
        keyword: filter.keyword || undefined,
        loaiGiamGia: filter.discountType || undefined,
        trangThai: filter.status || undefined,
        phamVi: filter.scope || undefined,
        tuNgay: filter.fromDate || undefined,
        denNgay: filter.toDate || undefined,
      },
    });
    if (res.status === 200) {
      listDiscount.value = res.data.content;
      page.value = res.data.number;
      totalPages.value = res.data.totalPages;
      totalElements.value = res.data.totalElements;

      // chỉ cache khi không có bộ lọc
      if (p === 0 && !hasFilter.value) {
        localStorage.setItem(
          "voucher_initial_list_cache",
          JSON.stringify(res.data),
        );
      }
    }
  } catch (error) {
    console.error(error);
  }
};

/* Tự tải lại khi đổi bộ lọc (debounce 400ms) */
let timer;
watch(
  filter,
  () => {
    clearTimeout(timer);
    timer = setTimeout(() => getData(0), 400);
  },
  { deep: true },
);

/* ---------------- Bật / tắt trạng thái ---------------- */
const toggleTrangThai = async (item) => {
  // Không cho bật lại nếu đã hết hạn
  if (new Date(item.ngayKetThuc) < new Date() && !item.trangThai) {
    alert("Phiếu đã hết hạn, không thể kích hoạt lại");
    return;
  }

  const oldValue = item.trangThai;
  item.trangThai = !item.trangThai; // optimistic

  try {
    const res = await axios.put(
      apiDiscount + `admin/phieu-giam-gia/toggle-trang-thai/${item.id}`,
    );
    item.trangThai = res.data.trangThai; // đồng bộ lại từ server
  } catch (error) {
    item.trangThai = oldValue; // rollback
    alert(error.response?.data?.message || "Cập nhật thất bại");
  }
};

/* ---------------- Phân trang ---------------- */
const goToPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return;
  getData(p);
};

/* ---------------- Định dạng ---------------- */
const formatDiscount = (v) =>
  v <= 100 ? v + "%" : v.toLocaleString("vi-VN") + "đ";

const formatDate = (s) => (s ? new Date(s).toLocaleDateString("vi-VN") : "");

const getStatus = (d) => {
  const now = new Date();
  const start = new Date(d.ngayBatDau);
  const end = new Date(d.ngayKetThuc);

  if (!d.trangThai) return "Ngừng hoạt động";
  if (end < now) return "Hết hạn";
  if (start > now) return "Sắp diễn ra";
  return "Đang hoạt động";
};

const statusPill = (value) => {
  if (value === "Đang hoạt động") return "pill-green";
  if (value === "Sắp diễn ra") return "pill-amber";
  if (value === "Ngừng hoạt động") return "pill-red";
  return "pill-gray"; // Hết hạn
};

onMounted(() => getData(0));
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý phiếu giảm giá</h1>
        <p class="screen-sub">
          Tạo, theo dõi và phân phối các mã giảm giá đến khách hàng
        </p>
      </div>
      <RouterLink
        :to="`/admin/phieu-giam-gia/add`"
        class="btn-add"
        type="button"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.2"
          stroke-linecap="round"
        >
          <path d="M12 6v12M6 12h12" />
        </svg>
        Thêm phiếu giảm giá
      </RouterLink>
    </div>

    <!-- ============ Bộ lọc tìm kiếm ============ -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linejoin="round"
          >
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Bộ lọc tìm kiếm</h3>
      </div>

      <!-- Hàng 1 -->
      <div class="filter-grid">
        <div class="f-item">
          <label>Tìm kiếm</label>
          <div class="f-search">
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
              v-model.trim="filter.keyword"
              type="text"
              placeholder="Nhập mã / tên phiếu giảm giá..."
            />
          </div>
        </div>

        <div class="f-item">
          <label>Loại giảm</label>
          <select v-model="filter.discountType" class="f-select">
            <option value="">Tất cả</option>
            <option value="PERCENT">Giảm theo %</option>
            <option value="AMOUNT">Giảm tiền (VNĐ)</option>
          </select>
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select v-model="filter.status" class="f-select">
            <option value="">Tất cả</option>
            <option value="active">Đang hoạt động</option>
            <option value="upcoming">Sắp diễn ra</option>
            <option value="expired">Hết hạn</option>
            <option value="inactive">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <hr class="filter-divider" />

      <!-- Hàng 2 -->
      <div class="filter-grid row-2">
        <div class="f-item">
          <label>Loại phiếu</label>
          <div class="pill-radio">
            <label class="pill-opt">
              <input v-model="filter.scope" type="radio" value="" />
              <span>Tất cả</span>
            </label>
            <label class="pill-opt">
              <input v-model="filter.scope" type="radio" value="CONG_KHAI" />
              <span>Công khai</span>
            </label>
            <label class="pill-opt">
              <input v-model="filter.scope" type="radio" value="CA_NHAN" />
              <span>Cá nhân</span>
            </label>
          </div>
        </div>

        <div class="f-item">
          <label>Từ ngày</label>
          <input
            v-model="filter.fromDate"
            type="date"
            class="f-date"
            :max="filter.toDate || undefined"
          />
        </div>

        <div class="f-item">
          <label>Đến ngày</label>
          <input
            v-model="filter.toDate"
            type="date"
            class="f-date"
            :min="filter.fromDate || undefined"
          />
        </div>
      </div>
    </section>

    <!-- ============ Danh sách ============ -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon is-red">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"
            />
          </svg>
        </span>
        <h3 class="panel-title">Danh sách phiếu giảm giá</h3>
        <span class="count-pill">{{ totalElements }} phiếu</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Mã PGG</th>
              <th>Tên PGG</th>
              <th>Loại phiếu</th>
              <th>Số lượng</th>
              <th>Giá trị giảm</th>
              <th>Thời gian áp dụng</th>
              <th>Trạng thái</th>
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in listDiscount" :key="d.id || i">
              <td>{{ page * pageSize + i + 1 }}</td>
              <td>{{ d.maPhieuGiamGia }}</td>
              <td>{{ d.tenPhieuGiamGia }}</td>
              <td>
                <span
                  class="badge"
                  :class="
                    d.phamVi === 'CONG_KHAI' ? 'bg-success' : 'bg-secondary'
                  "
                >
                  {{ d.phamVi === "CONG_KHAI" ? "Công khai" : "Cá nhân" }}
                </span>
              </td>
              <td>{{ d.soLuong }}</td>
              <td>{{ formatDiscount(d.giaTriGiam) }}</td>
              <td>
                {{ formatDate(d.ngayBatDau) }} - {{ formatDate(d.ngayKetThuc) }}
              </td>

              <td>
                <span class="pill" :class="statusPill(getStatus(d))">
                  {{ getStatus(d) }}
                </span>
              </td>
              <td>
                <div class="act-group">
                  <!-- Switch bật/tắt trạng thái voucher -->
                  <label class="switch">
                    <input
                      type="checkbox"
                      :checked="
                        d.trangThai && new Date(d.ngayKetThuc) >= new Date()
                      "
                      :disabled="new Date(d.ngayKetThuc) < new Date()"
                      @change="toggleTrangThai(d)"
                    />
                    <span class="slider"></span>
                  </label>

                  <!-- Nút sửa -->
                  <RouterLink
                    :to="`/admin/phieu-giam-gia/update/${d.id}`"
                    class="act-btn is-red"
                    aria-label="Sửa"
                  >
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.9"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    >
                      <path
                        d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3zM14.5 6.5l3 3"
                      />
                    </svg>
                  </RouterLink>
                </div>
              </td>
            </tr>
            <tr v-if="!listDiscount.length">
              <td colspan="9" style="text-align: center">Không có dữ liệu</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">
          Hiển thị {{ listDiscount.length ? page * pageSize + 1 : 0 }}-{{
            page * pageSize + listDiscount.length
          }}
          trong {{ totalElements }} phiếu giảm giá
        </span>
        <div class="pager">
          <button
            class="page-btn"
            type="button"
            aria-label="Trang trước"
            :disabled="page === 0"
            @click="goToPage(page - 1)"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M14.5 6l-6 6 6 6" />
            </svg>
          </button>
          <button
            v-for="p in totalPages"
            :key="p"
            class="page-btn"
            :class="{ 'is-active': p - 1 === page }"
            type="button"
            @click="goToPage(p - 1)"
          >
            {{ p }}
          </button>
          <button
            class="page-btn"
            type="button"
            aria-label="Trang sau"
            :disabled="page >= totalPages - 1"
            @click="goToPage(page + 1)"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M9.5 6l6 6-6 6" />
            </svg>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ---- Switch bật/tắt ---- */
.switch {
  position: relative;
  display: inline-block;
  width: 38px;
  height: 21px;
  flex-shrink: 0;
  margin: 0;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background-color: #cbd5e1;
  border-radius: 999px;
  transition: 0.25s;
}

.slider::before {
  position: absolute;
  content: "";
  width: 15px;
  height: 15px;
  left: 3px;
  top: 3px;
  background-color: white;
  border-radius: 50%;
  transition: 0.25s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.switch input:checked + .slider {
  background-color: #16a34a;
}

.switch input:checked + .slider::before {
  transform: translateX(17px);
}

.switch input:disabled + .slider {
  opacity: 0.5;
  cursor: not-allowed;
}

.switch input:focus-visible + .slider {
  outline: 2px solid #2563eb;
  outline-offset: 2px;
}

.act-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ---- Bộ lọc tìm kiếm ---- */
.filter-grid {
  --red: #cc0000;
  --line: #e5e7eb;
  --ink: #111827;
  --muted: #6b7280;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem 1.5rem;
  align-items: end;
}
.filter-grid.row-2 {
  grid-template-columns: 1.4fr 1fr 1fr;
}
.filter-divider {
  margin: 1.25rem 0;
  border: 0;
  border-top: 1px solid #e5e7eb;
}

.f-item label {
  display: block;
  margin-bottom: 0.4rem;
  font-size: 0.85rem;
  color: var(--muted);
}

/* Ô bo tròn dùng chung */
.f-search input,
.f-select,
.f-date {
  width: 100%;
  height: 44px;
  padding: 0 1rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  font: inherit;
  color: var(--ink);
  outline: none;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}
.f-search input:focus,
.f-select:focus,
.f-date:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

/* Ô tìm kiếm có icon kính lúp */
.f-search {
  position: relative;
}
.f-search svg {
  position: absolute;
  left: 16px;
  top: 50%;
  width: 18px;
  height: 18px;
  transform: translateY(-50%);
  color: var(--muted);
  pointer-events: none;
}
.f-search input {
  padding-left: 2.8rem;
}

/* Select có mũi tên riêng */
.f-select {
  appearance: none;
  padding-right: 2.5rem;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23374151' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9.5l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 1rem center;
  background-size: 16px;
}

/* Radio dạng viên thuốc */
.pill-radio {
  display: inline-flex;
  align-items: center;
  gap: 1.4rem;
  height: 44px;
  padding: 0 1.2rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
}
.pill-opt {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  margin: 0 !important;
  font-size: 0.92rem !important;
  font-weight: 600;
  color: var(--ink) !important;
  cursor: pointer;
}
.pill-opt input {
  width: 17px;
  height: 17px;
  margin: 0;
  accent-color: var(--red);
  cursor: pointer;
}

@media (max-width: 900px) {
  .filter-grid,
  .filter-grid.row-2 {
    grid-template-columns: 1fr;
  }
}
</style>
