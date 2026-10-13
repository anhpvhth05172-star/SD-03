<script setup>
import { formatDateTime } from "@/utils/format";
import axios from "axios";
import { inject, onMounted, ref } from "vue";
import Swal from "sweetalert2";

const showFilter = ref(true);
const apiDiscountEvent = inject("baseAPI");

const listEvent = ref([]);
const page = ref(0);
const pageSize = 8;
const totalPages = ref(0);
const totalElements = ref(0);

const getData = async (p = 0) => {
  try {
    const res = await axios.get(apiDiscountEvent + "admin/dot-giam-gia", {
      params: { page: p, size: pageSize },
    });
    if (res.status === 200) {
      listEvent.value = res.data.content;
      page.value = res.data.number;
      totalPages.value = res.data.totalPages;
      totalElements.value = res.data.totalElements;
    }
  } catch (error) {
    console.error(error);
  }
};

const goToPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return;
  getData(p);
};

const formatDate = (s) => (s ? new Date(s).toLocaleDateString("vi-VN") : "");

const getStatus = (d) => {
  if (!d.trangThai) return "Ngừng hoạt động";
  const now = new Date();
  if (now < new Date(d.ngayBatDau)) return "Sắp diễn ra";
  if (now > new Date(d.ngayKetThuc)) return "Đã kết thúc";
  return "Đang diễn ra";
};

const statusPill = (value) => {
  if (value === "Đang diễn ra") return "pill-green";
  if (value === "Sắp diễn ra") return "pill-amber";
  if (value === "Ngừng hoạt động") return "pill-gray";
  return "pill-gray";
};

// ========== Đổi trạng thái bằng switch ==========
const toggleStatus = async (item) => {
  const newStatus = !item.trangThai;

  const result = await Swal.fire({
    title: newStatus ? "Bật đợt giảm giá?" : "Tắt đợt giảm giá?",
    text: newStatus
      ? `Bạn muốn kích hoạt đợt "${item.tenDotGiamGia}"?`
      : `Bạn muốn ngừng đợt "${item.tenDotGiamGia}"?`,
    icon: "question",
    showCancelButton: true,
    confirmButtonColor: "#cc0000",
    cancelButtonColor: "#6b7280",
    confirmButtonText: newStatus ? "Bật" : "Tắt",
    cancelButtonText: "Hủy",
  });

  if (!result.isConfirmed) return;

  try {
    // ⚠️ Đổi endpoint cho khớp backend của bạn
    await axios.put(
      apiDiscountEvent + `admin/dot-giam-gia/change-status/${item.id}`,
      { trangThai: newStatus },
    );

    // Cập nhật ngay trên UI
    item.trangThai = newStatus;

    await Swal.fire({
      icon: "success",
      title: newStatus ? "Đã bật đợt giảm giá" : "Đã tắt đợt giảm giá",
      timer: 1200,
      showConfirmButton: false,
    });
  } catch (error) {
    Swal.fire({
      icon: "error",
      title: "Thất bại",
      text: error.response?.data?.message || "Không thể đổi trạng thái",
      confirmButtonColor: "#cc0000",
    });
  }
};

onMounted(() => getData(0));
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý đợt giảm giá</h1>
        <p class="screen-sub">
          Quản lý các sự kiện giảm giá áp dụng cho tất cả khách hàng trên toàn
          hệ thống
        </p>
      </div>
      <RouterLink to="/admin/dot-giam-gia/add" class="btn-add" type="button">
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2.2"
          stroke-linecap="round"
        >
          <path d="M12 6v12M6 12h12" />
        </svg>
        Thêm đợt giảm giá
      </RouterLink>
    </div>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Tìm kiếm &amp; bộ lọc</h3>
        <div class="panel-right">
          <span class="result-count">{{ totalElements }} kết quả</span>
          <button
            class="link-red"
            type="button"
            @click="showFilter = !showFilter"
          >
            {{ showFilter ? "Ẩn bớt" : "Hiện thêm" }}
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path :d="showFilter ? 'M6 14.5l6-6 6 6' : 'M6 9.5l6 6 6-6'" />
            </svg>
          </button>
        </div>
      </div>

      <div v-show="showFilter" class="filter-row">
        <div class="f-item">
          <label>Tên đợt giảm giá</label>
          <input type="text" placeholder="Nhập tên đợt giảm giá" />
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select>
            <option value="" label="Tất cả"></option>
            <option value="1" label="Đang hoạt động"></option>
            <option value="0" label="Ngừng hoạt động"></option>
          </select>
        </div>

        <div class="f-item">
          <label>Từ ngày</label>
          <div class="date-box">
            <input type="text" placeholder="dd/mm/yyyy" />
            <svg
              class="date-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="f-item">
          <label>Đến ngày</label>
          <div class="date-box">
            <input type="text" placeholder="dd/mm/yyyy" />
            <svg
              class="date-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="3.8" y="5.5" width="16.4" height="14.5" rx="2" />
              <path d="M8 3.2v4.2M16 3.2v4.2M3.8 10h16.4" />
            </svg>
          </div>
        </div>

        <div class="filter-actions">
          <button class="btn-reset" type="button" aria-label="Đặt lại">
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
          </button>
          <button class="btn-apply" type="button">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.2"
              stroke-linecap="round"
            >
              <path d="M4 6h16M7 12h10M10 18h4" />
            </svg>
            Áp dụng
          </button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon is-red">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"
            />
          </svg>
        </span>
        <h3 class="panel-title">Danh sách đợt giảm giá</h3>
        <span class="count-pill">{{ totalElements }} đợt</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Mã ĐGG</th>
              <th>Tên ĐGG</th>
              <th>Giá trị giảm</th>
              <th>Ngày bắt đầu</th>
              <th>Ngày kết thúc</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in listEvent" :key="i.index">
              <td>{{ i + 1 }}</td>
              <td>{{ d.maDotGiamGia }}</td>
              <td>{{ d.tenDotGiamGia }}</td>
              <td class="cell-strong">{{ d.phanTramGiam }}%</td>
              <td>{{ formatDate(d.ngayBatDau) }}</td>
              <td>{{ formatDate(d.ngayKetThuc) }}</td>
              <td>
                <span class="pill" :class="statusPill(getStatus(d))">{{
                  getStatus(d)
                }}</span>
              </td>
              <td>
                <div class="act-group">
                  <!-- SWITCH TRẠNG THÁI -->
                  <label
                    class="switch"
                    :title="d.trangThai ? 'Đang bật' : 'Đang tắt'"
                  >
                    <input
                      type="checkbox"
                      :checked="d.trangThai"
                      @change="toggleStatus(d)"
                    />
                    <span class="slider"></span>
                  </label>

                  <!-- NÚT SỬA (giữ nguyên) -->
                  <RouterLink
                    :to="`/admin/dot-giam-gia/update/${d.id}`"
                    class="act-btn is-red"
                    type="button"
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
            <tr v-if="!listEvent.length">
              <td colspan="7" style="text-align: center">Không có dữ liệu</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">
          Hiển thị {{ listEvent.length ? page * pageSize + 1 : 0 }}-{{
            page * pageSize + listEvent.length
          }}
          trong {{ totalElements }} đợt giảm giá
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
/* ===== Switch ===== */
.switch {
  position: relative;
  display: inline-block;
  width: 42px;
  height: 22px;
  flex-shrink: 0;
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
  background-color: #d1d5db;
  border-radius: 999px;
  transition: 0.25s;
}

.slider::before {
  content: "";
  position: absolute;
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  border-radius: 50%;
  transition: 0.25s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.switch input:checked + .slider {
  background-color: green;
}

.switch input:checked + .slider::before {
  transform: translateX(20px);
}

.switch input:focus-visible + .slider {
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.25);
}
</style>
