<script setup>
import axios from "axios";
import Swal from "sweetalert2";
import { computed, inject, onMounted, reactive, ref } from "vue";

const apiDiscount = inject("baseAPI");

const listDiscount = ref([]);
const page = ref(0);
const pageSize = 8;
const totalPages = ref(0);
const totalElements = ref(0);

/* ---------------- Bộ lọc ---------------- */
const emptyFilter = () => ({
  keyword: "",
  discountType: "",
  status: "",
  scope: "",
  fromDate: "",
  toDate: "",
});

// Giá trị đang nhập trên form
const filter = reactive(emptyFilter());
// Giá trị đã áp dụng (dùng để gọi API / phân trang)
const applied = reactive(emptyFilter());

const hasFilter = computed(() => Object.values(applied).some((v) => v));

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
        keyword: applied.keyword || undefined,
        loaiGiamGia: applied.discountType || undefined,
        trangThai: applied.status || undefined,
        phamVi: applied.scope || undefined,
        tuNgay: applied.fromDate || undefined,
        denNgay: applied.toDate || undefined,
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

/* Nhấn "Áp dụng" (hoặc Enter trong ô tìm kiếm) */
const applyFilter = () => {
  Object.assign(applied, filter);
  getData(0);
};

const resetFilter = () => {
  Object.assign(filter, emptyFilter());
  Object.assign(applied, emptyFilter());
  getData(0);
};

/* ---------------- Bật / tắt trạng thái ---------------- */
const toggleTrangThai = async (item) => {
  // Không cho bật lại nếu đã hết hạn
  if (new Date(item.ngayKetThuc) < new Date() && !item.trangThai) {
    Swal.fire({
      icon: "warning",
      title: "Không thể kích hoạt",
      text: "Phiếu đã hết hạn, không thể kích hoạt lại",
      confirmButtonColor: "#cc0000",
    });
    return;
  }

  const willActivate = !item.trangThai;

  const { isConfirmed } = await Swal.fire({
    icon: "question",
    title: willActivate ? "Kích hoạt phiếu?" : "Ngừng hoạt động phiếu?",
    text: `Bạn có chắc muốn ${
      willActivate ? "kích hoạt" : "ngừng hoạt động"
    } phiếu "${item.tenPhieuGiamGia}" không?`,
    showCancelButton: true,
    confirmButtonText: "Xác nhận",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#cc0000",
    cancelButtonColor: "#6b7280",
    reverseButtons: true,
  });

  if (!isConfirmed) return; // Hủy -> giữ nguyên trạng thái cũ

  const oldValue = item.trangThai;
  item.trangThai = willActivate; // optimistic

  try {
    const res = await axios.put(
      apiDiscount + `admin/phieu-giam-gia/toggle-trang-thai/${item.id}`,
    );
    item.trangThai = res.data.trangThai; // đồng bộ lại từ server
    Swal.fire({
      icon: "success",
      title: "Cập nhật thành công",
      timer: 1200,
      showConfirmButton: false,
    });
  } catch (error) {
    item.trangThai = oldValue; // rollback
    Swal.fire({
      icon: "error",
      title: "Cập nhật thất bại",
      text: error.response?.data?.message || "Vui lòng thử lại",
      confirmButtonColor: "#cc0000",
    });
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

const formatDate = (s) => {
  if (!s) return "";
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return String(s);
  const p = (x) => String(x).padStart(2, "0");
  return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

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

    <!-- ============ Tìm kiếm & bộ lọc ============ -->
    <section class="panel filter-panel">
      <div class="filter-head">
        <div class="filter-title">
          <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
          <h3>Tìm kiếm &amp; bộ lọc</h3>
        </div>
      </div>

      <form class="filter-row" @submit.prevent="applyFilter">
        <div class="f-item f-keyword">
          <label for="f-keyword">Mã / tên phiếu giảm giá</label>
          <input
            id="f-keyword"
            v-model.trim="filter.keyword"
            type="text"
            class="f-control"
            placeholder="Nhập mã hoặc tên phiếu"
          />
        </div>

        <div class="f-item">
          <label for="f-type">Loại giảm</label>
          <select
            id="f-type"
            v-model="filter.discountType"
            class="f-control f-select"
          >
            <option value="">Tất cả</option>
            <option value="PERCENT">Giảm theo %</option>
            <option value="AMOUNT">Giảm tiền (VNĐ)</option>
          </select>
        </div>

        <div class="f-item">
          <label for="f-status">Trạng thái</label>
          <select
            id="f-status"
            v-model="filter.status"
            class="f-control f-select"
          >
            <option value="">Tất cả</option>
            <option value="active">Đang hoạt động</option>
            <option value="upcoming">Sắp diễn ra</option>
            <option value="expired">Hết hạn</option>
            <option value="inactive">Ngừng hoạt động</option>
          </select>
        </div>

        <div class="f-item">
          <label for="f-scope">Loại phiếu</label>
          <select
            id="f-scope"
            v-model="filter.scope"
            class="f-control f-select"
          >
            <option value="">Tất cả</option>
            <option value="CONG_KHAI">Công khai</option>
            <option value="CA_NHAN">Cá nhân</option>
          </select>
        </div>

        <div class="f-item">
          <label for="f-from">Từ ngày</label>
          <input
            id="f-from"
            v-model="filter.fromDate"
            type="date"
            class="f-control"
            :max="filter.toDate || undefined"
          />
        </div>

        <div class="f-item">
          <label for="f-to">Đến ngày</label>
          <input
            id="f-to"
            v-model="filter.toDate"
            type="date"
            class="f-control"
            :min="filter.fromDate || undefined"
          />
        </div>

        <div class="f-actions">
          <button
            type="button"
            class="btn-icon-reset"
            aria-label="Đặt lại bộ lọc"
            title="Đặt lại bộ lọc"
            @click="resetFilter"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M20 12a8 8 0 1 1-2.6-5.9" />
              <path d="M20 4v5h-5" />
            </svg>
          </button>
          <button type="submit" class="btn-apply">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
            >
              <path d="M4 7h16M7 12h10M10 17h4" />
            </svg>
            Áp dụng
          </button>
        </div>
      </form>
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
              <td style="font-weight: bold">
                {{ formatDiscount(d.giaTriGiam) }}
              </td>
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
                  <!-- Switch bật/tắt trạng thái (có xác nhận SweetAlert) -->
                  <label class="switch">
                    <input
                      type="checkbox"
                      :checked="
                        d.trangThai && new Date(d.ngayKetThuc) >= new Date()
                      "
                      :disabled="new Date(d.ngayKetThuc) < new Date()"
                      @click.prevent="toggleTrangThai(d)"
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

/* ---- Tìm kiếm & bộ lọc (cùng layout với đợt giảm giá) ---- */
.filter-panel {
  --red: #cc0000;
  --red-dark: #b01e1e;
  --line: #e5e7eb;
  --ink: #111827;
  --muted: #6b7280;
}

.filter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.1rem;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  color: var(--ink);
}
.filter-title svg {
  width: 22px;
  height: 22px;
  color: #374151;
}
.filter-title h3 {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
}

.filter-head-right {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.filter-count {
  font-size: 11.5px;
  color: var(--muted);
}
.filter-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0;
  border: 0;
  background: none;
  font: inherit;
  font-weight: 600;
  color: var(--red);
  cursor: pointer;
}
.filter-toggle svg {
  width: 16px;
  height: 16px;
  transition: transform 0.2s;
}
.filter-toggle svg.is-flipped {
  transform: rotate(180deg);
}
.filter-toggle:focus-visible {
  outline: 2px solid var(--red);
  outline-offset: 3px;
  border-radius: 4px;
}

/* Lưới 12 cột, các ô rộng đều nhau (mỗi ô 1/4 hàng):
   Hàng 1: Tìm kiếm | Loại giảm | Trạng thái | Loại phiếu
   Hàng 2: Từ ngày | Đến ngày | (cụm nút căn phải) */
.filter-row {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 1.1rem 1.25rem;
  align-items: end;
}
.filter-row > .f-item {
  grid-column: span 3;
  min-width: 0;
}

.f-item label {
  display: block;
  margin-bottom: 0.4rem;
  font-size: 11.5px;
  color: var(--muted);
}

/* Ô nhập dùng chung, bo góc 8px */
.f-control {
  width: 100%;
  height: 44px;
  padding: 0 0.9rem;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  font: inherit;
  font-size: 13px;
  color: var(--ink);
  outline: none;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}
.f-control::placeholder {
  color: #9ca3af;
}
.f-control:focus {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.12);
}

/* Select có mũi tên riêng */
.f-select {
  appearance: none;
  padding-right: 2.3rem;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23374151' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9.5l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.8rem center;
  background-size: 16px;
}

/* Cụm nút: reset (chỉ icon) + Áp dụng */
.f-actions {
  grid-column: span 6;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.6rem;
}
.f-actions .btn-apply {
  min-width: 150px;
}

.btn-icon-reset {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  color: #374151;
  cursor: pointer;
  transition:
    background-color 0.15s,
    border-color 0.15s;
}
.btn-icon-reset svg {
  width: 18px;
  height: 18px;
}
.btn-icon-reset:hover {
  background: #f5f5f5;
  border-color: #9ca3af;
}
.btn-icon-reset:focus-visible,
.btn-apply:focus-visible {
  outline: 2px solid var(--red);
  outline-offset: 2px;
}

.btn-apply {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  height: 44px;
  padding: 0 1.4rem;
  border: 0;
  border-radius: 8px;
  background: var(--red);
  color: #fff;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.15s;
}
.btn-apply svg {
  width: 18px;
  height: 18px;
}
.btn-apply:hover {
  background: var(--red-dark);
}

/* Tablet: 2 cột, cụm nút chiếm cả hàng */
@media (max-width: 1100px) {
  .filter-row > .f-item {
    grid-column: span 6;
  }
  .filter-row > .f-actions {
    grid-column: 1 / -1;
  }
}

/* Điện thoại: 1 cột */
@media (max-width: 640px) {
  .filter-row > .f-item {
    grid-column: 1 / -1;
  }
  .filter-head {
    flex-wrap: wrap;
  }
  .f-actions .btn-apply {
    flex: 1;
  }
}
</style>
