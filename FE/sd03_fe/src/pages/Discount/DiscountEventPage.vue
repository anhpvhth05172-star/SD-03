<script setup>
import axios from "axios";
import { inject, onMounted, ref } from "vue";
import Swal from "sweetalert2";
import PaginationBar from "@/components/PaginationBar.vue";

const apiDiscountEvent = inject("baseAPI");

const listEvent = ref([]);
const page = ref(0);
const pageSize = ref(10);
const totalPages = ref(0);
const totalElements = ref(0);
const togglingId = ref(null); // id đợt đang đổi trạng thái (khóa switch)
const lastUpdated = ref(null); // thời điểm tải dữ liệu gần nhất

// ========== Bộ lọc ==========
const emptyFilter = () => ({
  ten: "",
  trangThai: null,
  tuNgay: "",
  denNgay: "",
});
// Giá trị đang nhập trên form
const filter = ref(emptyFilter());
// Giá trị đã bấm "Áp dụng" (dùng cho phân trang, đổi trạng thái, v.v.)
const applied = ref(emptyFilter());

const getData = async (p = 0) => {
  try {
    const params = { page: p, size: pageSize.value };
    const f = applied.value;
    if (f.ten.trim()) params.ten = f.ten.trim();
    if (f.trangThai !== null) params.trangThai = f.trangThai;
    if (f.tuNgay) params.tuNgay = f.tuNgay; // yyyy-MM-dd
    if (f.denNgay) params.denNgay = f.denNgay;

    const res = await axios.get(apiDiscountEvent + "admin/dot-giam-gia", {
      params,
    });
    if (res.status === 200) {
      listEvent.value = res.data.content;
      page.value = res.data.number;
      totalPages.value = res.data.totalPages;
      totalElements.value = res.data.totalElements;
      lastUpdated.value = new Date();
    }
  } catch (error) {
    console.error(error);
  }
};

const applyFilter = () => {
  const { tuNgay, denNgay } = filter.value;
  if (tuNgay && denNgay && tuNgay > denNgay) {
    Swal.fire({
      icon: "warning",
      title: "Khoảng ngày không hợp lệ",
      text: "'Từ ngày' không được lớn hơn 'Đến ngày'",
      confirmButtonColor: "#cc0000",
    });
    return;
  }
  applied.value = { ...filter.value };
  getData(0); // luôn về trang đầu khi lọc mới
};

const resetFilter = () => {
  filter.value = emptyFilter();
  applied.value = emptyFilter();
  getData(0);
};

const goToPage = (p) => {
  if (p < 0 || p >= totalPages.value || p === page.value) return;
  getData(p);
};

const onSizeChange = (s) => {
  pageSize.value = s;
  getData(0);
};

// ========== Hiển thị ==========
const formatDate = (s) => {
  if (!s) return "";
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return String(s);
  const p = (x) => String(x).padStart(2, "0");
  return `${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

const formatUpdated = (d) =>
  d
    ? `${d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" })} ${d.toLocaleDateString("vi-VN")}`
    : "";

const isEnded = (d) => !!d.ngayKetThuc && new Date() > new Date(d.ngayKetThuc);
const isOn = (d) => !!d.trangThai && !isEnded(d);

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
  return "pill-gray";
};

// ========== Đổi trạng thái bằng switch (đồng bộ với back-end) ==========
const toggleStatus = async (item) => {
  if (togglingId.value !== null) return;

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

  // Bấm Hủy: không làm gì, switch vẫn theo dữ liệu cũ
  if (!result.isConfirmed) return;

  togglingId.value = item.id;
  try {
    const res = await axios.put(
      apiDiscountEvent + `admin/dot-giam-gia/change-status/${item.id}`,
      { trangThai: newStatus },
    );

    // Lấy trạng thái thật từ back-end
    if (typeof res.data?.trangThai === "boolean") {
      item.trangThai = res.data.trangThai;
    }

    // Tải lại trang hiện tại (giữ nguyên bộ lọc đã áp dụng)
    await getData(page.value);

    await Swal.fire({
      icon: "success",
      title: newStatus ? "Đã bật đợt giảm giá" : "Đã tắt đợt giảm giá",
      timer: 1200,
      showConfirmButton: false,
    });
  } catch (error) {
    // Lỗi: tải lại để switch quay về đúng trạng thái trong DB
    await getData(page.value);
    Swal.fire({
      icon: "error",
      title: "Thất bại",
      text: error.response?.data?.message || "Không thể đổi trạng thái",
      confirmButtonColor: "#cc0000",
    });
  } finally {
    togglingId.value = null;
  }
};

onMounted(() => getData(0));
</script>

<template>
  <div class="screen">
      <div class="screen-head">
        <div>
          <h1 class="screen-title">Quản lý đợt giảm giá</h1>
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

    <!-- ===== TÌM KIẾM & BỘ LỌC ===== -->
    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Tìm kiếm &amp; bộ lọc</h3>
      </div>

      <div class="filter-row">
        <div class="f-item">
          <label>Tên đợt giảm giá</label>
          <input
            v-model="filter.ten"
            type="text"
            placeholder="Nhập tên đợt giảm giá"
            @keyup.enter="applyFilter"
          />
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select v-model="filter.trangThai">
            <option :value="null">Tất cả</option>
            <option :value="1">Đang hoạt động</option>
            <option :value="0">Ngừng hoạt động</option>
          </select>
        </div>

        <div class="f-item">
          <label>Từ ngày</label>
          <div class="date-box">
            <input
              v-model="filter.tuNgay"
              type="date"
              :max="filter.denNgay || undefined"
            />
          </div>
        </div>

        <div class="f-item">
          <label>Đến ngày</label>
          <div class="date-box">
            <input
              v-model="filter.denNgay"
              type="date"
              :min="filter.tuNgay || undefined"
            />
          </div>
        </div>

        <div class="filter-actions">
          <button
            class="btn-reset"
            type="button"
            aria-label="Đặt lại"
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
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
          </button>
          <button class="btn-apply" type="button" @click="applyFilter">
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

    <!-- ===== DANH SÁCH ===== -->
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
          <span v-if="lastUpdated" class="panel-meta"
            >Cập nhật lúc {{ formatUpdated(lastUpdated) }}</span
          >
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
              <th>Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(d, i) in listEvent" :key="d.id">
              <td>{{ page * pageSize + i + 1 }}</td>
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
                  <!-- SWITCH TRẠNG THÁI: hiển thị theo dữ liệu back-end -->
                  <label
                    class="switch"
                    :class="{
                      'is-disabled': isEnded(d) || togglingId === d.id,
                    }"
                    :title="
                      isEnded(d)
                        ? 'Đợt đã kết thúc'
                        : d.trangThai
                          ? 'Đang bật'
                          : 'Đang tắt'
                    "
                  >
                    <input
                      type="checkbox"
                      :checked="isOn(d)"
                      :disabled="isEnded(d) || togglingId === d.id"
                      @click.prevent="toggleStatus(d)"
                    />
                    <span class="slider"></span>
                  </label>

                  <!-- NÚT SỬA -->
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
              <td colspan="8" style="text-align: center">Không có dữ liệu</td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar
        :page="page"
        :total-pages="totalPages"
        :total-elements="totalElements"
        :page-size="pageSize"
        :from="listEvent.length ? page * pageSize + 1 : 0"
        :to="page * pageSize + listEvent.length"
        label="đợt giảm giá"
        @change="goToPage"
        @size-change="onSizeChange"
      />
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

/* Khóa switch khi đợt đã kết thúc hoặc đang gọi API */
.switch.is-disabled .slider {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
