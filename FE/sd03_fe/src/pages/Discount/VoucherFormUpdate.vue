<template>
  <div>
    <div class="card voucher-card border-0 shadow-sm">
      <div class="title-wrap">
        <div class="title-icon"><i class="bi bi-ticket-perforated"></i></div>
        <h2 class="title">Cập nhật phiếu giảm giá</h2>
      </div>

      <form @submit.prevent="submit">
        <div class="row g-4">
          <div class="col-6">
            <label class="form-label required">Mã phiếu giảm giá</label>
            <input
              v-model.trim="form.maPhieuGiamGia"
              type="text"
              class="form-control"
              placeholder="VD: PGG001"
            />
          </div>
          <div class="col-6">
            <label class="form-label required">Tên phiếu giảm giá</label>
            <input
              v-model.trim="form.tenPhieuGiamGia"
              type="text"
              class="form-control"
              placeholder="VD: Giảm giá mùa hè"
            />
          </div>

          <div class="col-6">
            <label class="form-label required">Loại giảm giá</label>
            <select v-model="form.loaiGiamGia" class="form-select form-control">
              <option value="PERCENT">Phần trăm (%)</option>
              <option value="CASH">Tiền mặt (đ)</option>
            </select>
          </div>
          <div class="col-6">
            <label class="form-label required">Giá trị giảm</label>
            <div class="input-group">
              <input
                v-model.number="form.giaTriGiam"
                type="number"
                min="1"
                class="form-control"
                placeholder="VD: 10"
              />
              <span class="input-group-text">{{
                form.loaiGiamGia === "PERCENT" ? "%" : "đ"
              }}</span>
            </div>
          </div>

          <div class="col-6">
            <label class="form-label">Giảm tối đa</label>
            <div class="input-group">
              <input
                v-model.number="form.giamToiDa"
                type="number"
                min="0"
                class="form-control"
                placeholder="VD: 50000"
              />
              <span class="input-group-text">đ</span>
            </div>
          </div>
          <div class="col-6">
            <label class="form-label">Hóa đơn tối thiểu</label>
            <div class="input-group">
              <input
                v-model.number="form.hoaDonToiThieu"
                type="number"
                min="0"
                class="form-control"
                placeholder="VD: 200000"
              />
              <span class="input-group-text">đ</span>
            </div>
          </div>

          <div class="col-6">
            <label class="form-label required">Số lượng</label>
            <input
              v-model.number="form.soLuong"
              type="number"
              min="1"
              class="form-control"
              placeholder="VD: 100"
            />
            <div v-if="soLuongDaSuDung > 0" class="form-text">
              Đã sử dụng: {{ soLuongDaSuDung }}
            </div>
          </div>
          <div class="col-6">
            <label class="form-label required">Trạng thái</label>
            <div class="status-group">
              <div class="form-check form-check-inline">
                <input
                  id="tt-hoat-dong"
                  v-model="form.trangThai"
                  class="form-check-input"
                  type="radio"
                  name="trangThai"
                  :value="true"
                />
                <label class="form-check-label" for="tt-hoat-dong"
                  >Đang hoạt động</label
                >
              </div>
              <div class="form-check form-check-inline">
                <input
                  id="tt-ngung"
                  v-model="form.trangThai"
                  class="form-check-input"
                  type="radio"
                  name="trangThai"
                  :value="false"
                />
                <label class="form-check-label" for="tt-ngung"
                  >Ngừng hoạt động</label
                >
              </div>
            </div>
          </div>

          <div class="col-6">
            <label class="form-label required">Ngày bắt đầu</label>
            <input
              v-model="form.ngayBatDau"
              type="datetime-local"
              class="form-control"
            />
          </div>
          <div class="col-6">
            <label class="form-label required">Ngày kết thúc</label>
            <input
              v-model="form.ngayKetThuc"
              type="datetime-local"
              class="form-control"
            />
          </div>

          <div class="col-12">
            <label class="form-label">Mô tả</label>
            <textarea
              v-model="form.moTa"
              class="form-control"
              rows="3"
              maxlength="500"
              placeholder="Mô tả ngắn về phiếu giảm giá (tối đa 500 ký tự)"
            ></textarea>
          </div>
        </div>

        <hr class="my-4" />
        <div class="d-flex gap-2">
          <button type="submit" class="btn btn-add" :disabled="loading">
            <span
              v-if="loading"
              class="spinner-border spinner-border-sm me-1"
            ></span>
            Cập nhật
          </button>
          <button type="button" class="btn btn-cancel" @click="router.back()">
            Hủy
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import axios from "axios";
import Swal from "sweetalert2";
import { inject, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();
const baseAPI = inject("baseAPI");

const form = reactive({
  id: route.params.id,
  maPhieuGiamGia: "",
  tenPhieuGiamGia: "",
  loaiGiamGia: "PERCENT",
  giaTriGiam: null,
  giamToiDa: null,
  hoaDonToiThieu: null,
  soLuong: null,
  ngayBatDau: "",
  ngayKetThuc: "",
  trangThai: true, // true: Đang hoạt động, false: Ngừng hoạt động
  moTa: "",
});

const soLuongDaSuDung = ref(0);
const loading = ref(false);

// "2026-05-25T08:00:00" hoặc [2026,5,25,8,0] -> "2026-05-25T08:00"
const toInputDateTime = (v) => {
  if (!v) return "";
  if (Array.isArray(v)) {
    const [y, m, d, h = 0, mi = 0] = v;
    const p = (n) => String(n).padStart(2, "0");
    return `${y}-${p(m)}-${p(d)}T${p(h)}:${p(mi)}`;
  }
  return String(v).replace(" ", "T").substring(0, 16);
};
// "2026-05-25T08:00" -> "2026-05-25T08:00:00" (gửi lên backend)
const toLocalDateTime = (v) => (v && v.length === 16 ? v + ":00" : v);

const toBool = (v) => v === true || v === 1 || v === "true" || v === "1";
// Ô số bị xóa trống sẽ ra "" -> gửi null
const numOrNull = (v) => (v === "" || v === undefined ? null : v);

const showError = (text, title = "Có lỗi xảy ra") =>
  Swal.fire({
    icon: "error",
    title,
    text,
    confirmButtonText: "Đóng",
    confirmButtonColor: "#d63348",
  });

// Load dữ liệu cũ
const loadDetail = async () => {
  loading.value = true;
  try {
    const res = await axios.get(
      baseAPI + "admin/phieu-giam-gia/detail/" + form.id,
    );
    const data = res.data?.data ?? res.data;

    form.maPhieuGiamGia = data.maPhieuGiamGia ?? "";
    form.tenPhieuGiamGia = data.tenPhieuGiamGia ?? "";
    form.loaiGiamGia = data.loaiGiamGia ?? "PERCENT";
    form.giaTriGiam = data.giaTriGiam ?? null;
    form.giamToiDa = data.giamToiDa ?? null;
    form.hoaDonToiThieu = data.hoaDonToiThieu ?? null;
    form.soLuong = data.soLuong ?? null;
    form.ngayBatDau = toInputDateTime(data.ngayBatDau);
    form.ngayKetThuc = toInputDateTime(data.ngayKetThuc);
    form.trangThai = toBool(data.trangThai);
    form.moTa = data.moTa ?? "";
    soLuongDaSuDung.value = data.soLuongDaSuDung ?? 0;
  } catch (e) {
    await showError(
      e.response?.data?.message || "Không tải được dữ liệu phiếu giảm giá",
      "Tải dữ liệu thất bại",
    );
    router.back();
  } finally {
    loading.value = false;
  }
};

onMounted(loadDetail);

const submit = async () => {
  if (
    !form.maPhieuGiamGia ||
    !form.tenPhieuGiamGia ||
    !form.loaiGiamGia ||
    !form.giaTriGiam ||
    !form.soLuong ||
    !form.ngayBatDau ||
    !form.ngayKetThuc
  ) {
    return showError("Nhập đủ các trường bắt buộc", "Thiếu thông tin");
  }
  if (
    form.loaiGiamGia === "PERCENT" &&
    (form.giaTriGiam < 1 || form.giaTriGiam > 100)
  ) {
    return showError(
      "Phần trăm giảm phải từ 1 đến 100",
      "Dữ liệu không hợp lệ",
    );
  }
  if (form.loaiGiamGia !== "PERCENT" && form.giaTriGiam <= 0) {
    return showError("Giá trị giảm phải lớn hơn 0", "Dữ liệu không hợp lệ");
  }
  if (form.soLuong < soLuongDaSuDung.value) {
    return showError(
      `Số lượng không được nhỏ hơn số đã sử dụng (${soLuongDaSuDung.value})`,
      "Dữ liệu không hợp lệ",
    );
  }
  if (form.ngayKetThuc <= form.ngayBatDau) {
    return showError(
      "Ngày kết thúc phải sau ngày bắt đầu",
      "Dữ liệu không hợp lệ",
    );
  }
  if (typeof form.trangThai !== "boolean") {
    return showError("Vui lòng chọn trạng thái", "Thiếu thông tin");
  }

  const confirm = await Swal.fire({
    icon: "question",
    title: "Cập nhật phiếu giảm giá này?",
    showCancelButton: true,
    confirmButtonText: "Cập nhật",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#d63348",
  });
  if (!confirm.isConfirmed) return;

  loading.value = true;
  try {
    await axios.put(baseAPI + "admin/phieu-giam-gia/update", {
      ...form,
      giamToiDa: numOrNull(form.giamToiDa),
      hoaDonToiThieu: numOrNull(form.hoaDonToiThieu),
      ngayBatDau: toLocalDateTime(form.ngayBatDau),
      ngayKetThuc: toLocalDateTime(form.ngayKetThuc),
    });

    await Swal.fire({
      icon: "success",
      title: "Cập nhật phiếu giảm giá thành công",
      timer: 1500,
      showConfirmButton: false,
    });
    router.back();
  } catch (e) {
    showError(
      e.response?.data?.message || "Không kết nối được máy chủ",
      "Cập nhật thất bại",
    );
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.voucher-card {
  background: #fff;
  border-radius: 24px;
  padding: 28px 28px 24px;
}
.title-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
}
.title-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: #fde8ea;
  color: #d63348;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.title {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
}
.form-label {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 8px;
}
.required::after {
  content: " *";
  color: #dc3545;
}

.form-control,
.input-group-text {
  background: #f8f9fa;
  border-color: #e9ecef;
  border-radius: 10px;
  min-height: 46px;
}
textarea.form-control {
  min-height: 90px;
  resize: vertical;
}
.input-group .form-control {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}
.input-group-text {
  border-top-left-radius: 0;
  border-bottom-left-radius: 0;
}
.form-control:focus {
  background: #fff;
  border-color: #d63348;
  box-shadow: 0 0 0 0.2rem rgba(214, 51, 72, 0.15);
}

.status-group {
  display: flex;
  align-items: center;
  min-height: 46px;
}
.form-check-input:checked {
  background-color: #d63348;
  border-color: #d63348;
}
.form-check-input:focus {
  border-color: #d63348;
  box-shadow: 0 0 0 0.2rem rgba(214, 51, 72, 0.15);
}

.btn-add {
  background: #d63348;
  border-color: #d63348;
  color: #fff;
  font-weight: 500;
  padding: 10px 26px;
  border-radius: 8px;
}
.btn-add:hover {
  background: #bb2a3d;
  color: #fff;
}
.btn-cancel {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  padding: 10px 26px;
  border-radius: 8px;
}
</style>
