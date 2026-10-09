<template>
  <div>
    <div class="card voucher-card border-0 shadow-sm">
      <div class="title-wrap">
        <div class="title-icon"><i class="bi bi-ticket-perforated"></i></div>
        <h2 class="title">Thêm đợt giảm giá</h2>
      </div>

      <form @submit.prevent="submit">
        <div class="row g-4">
          <div class="col-6">
            <label class="form-label required">Mã đợt giảm giá</label>
            <input
              v-model.trim="form.maDotGiamGia"
              type="text"
              class="form-control"
              placeholder="VD: DOT009"
            />
          </div>
          <div class="col-6">
            <label class="form-label required">Tên đợt giảm giá</label>
            <input
              v-model.trim="form.tenDotGiamGia"
              type="text"
              class="form-control"
              placeholder="VD: Đợt giảm giá tháng 9"
            />
          </div>

          <div class="col-6">
            <label class="form-label required">Phần trăm giảm</label>
            <div class="input-group">
              <input
                v-model="form.phanTramGiam"
                type="text"
                min="1"
                max="100"
                class="form-control"
                placeholder="VD: 10"
              />
              <span class="input-group-text">%</span>
            </div>
          </div>
          <div class="col-6"></div>

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
              placeholder="Mô tả ngắn về đợt giảm giá (tối đa 500 ký tự)"
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
            Thêm mới
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
import { inject, reactive, ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const baseAPI = inject("baseAPI");

const form = reactive({
  maDotGiamGia: "",
  tenDotGiamGia: "",
  phanTramGiam: 0.0,
  ngayBatDau: "",
  ngayKetThuc: "",
  moTa: "",
});

const loading = ref(false);

const toLocalDateTime = (v) => (v && v.length === 16 ? v + ":00" : v);

const showError = (text, title = "Có lỗi xảy ra") =>
  Swal.fire({
    icon: "error",
    title,
    text,
    confirmButtonText: "Đóng",
    confirmButtonColor: "#d63348",
  });

const submit = async () => {
  if (
    !form.maDotGiamGia ||
    !form.tenDotGiamGia ||
    !form.phanTramGiam ||
    !form.ngayBatDau ||
    !form.ngayKetThuc
  ) {
    return showError("Nhập đủ các trường bắt buộc", "Thiếu thông tin");
  }
  if (form.giaTriGiam < 1 || form.giaTriGiam > 100) {
    return showError(
      "Phần trăm giảm phải từ 1 đến 100",
      "Dữ liệu không hợp lệ",
    );
  }
  if (form.ngayKetThuc < form.ngayBatDau) {
    return showError(
      "Ngày kết thúc phải sau ngày bắt đầu",
      "Dữ liệu không hợp lệ",
    );
  }

  const confirm = await Swal.fire({
    icon: "question",
    title: "Thêm đợt giảm giá này?",
    showCancelButton: true,
    confirmButtonText: "Thêm",
    cancelButtonText: "Hủy",
    confirmButtonColor: "#d63348",
  });
  if (!confirm.isConfirmed) return;

  loading.value = true;
  try {
    await axios.post(baseAPI + "admin/dot-giam-gia/add", {
      ...form,
      ngayBatDau: toLocalDateTime(form.ngayBatDau),
      ngayKetThuc: toLocalDateTime(form.ngayKetThuc),
    });

    await Swal.fire({
      icon: "success",
      title: "Thêm đợt giảm giá thành công",
      timer: 1500,
      showConfirmButton: false,
    });
    router.back();
  } catch (e) {
    showError(
      e.response?.data?.message || "Không kết nối được máy chủ",
      "Thêm thất bại",
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
