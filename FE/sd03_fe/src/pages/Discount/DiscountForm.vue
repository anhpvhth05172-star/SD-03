<template>
  <div class="card voucher-card border-0 shadow-sm">
    <div class="card-body p-4">
      <div class="d-flex align-items-center gap-3 mb-4">
        <div class="voucher-icon">
          <i class="bi bi-ticket-perforated"></i>
        </div>
        <h5 class="mb-0 fw-semibold">Thông tin phiếu</h5>
      </div>

      <form @submit.prevent="submit()" novalidate>
        <div class="row g-4">
          <div class="col-md-6">
            <label class="form-label">
              Mã phiếu <span class="text-danger">*</span>
            </label>
            <div class="input-group has-validation">
              <input
                v-model.trim="form.code"
                type="text"
                class="form-control"
                :class="{ 'is-invalid': errors.code }"
                maxlength="50"
                placeholder="Nhập mã phiếu"
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

          <div class="col-md-6">
            <label class="form-label">
              Tên phiếu <span class="text-danger">*</span>
            </label>
            <input
              v-model.trim="form.name"
              type="text"
              class="form-control"
              :class="{ 'is-invalid': errors.name }"
              maxlength="200"
              placeholder="VD: ASICS Running giảm 15%"
            />
            <div class="invalid-feedback">{{ errors.name }}</div>
          </div>

          <div class="col-md-6">
            <label class="form-label d-block">
              Loại giảm <span class="text-danger">*</span>
            </label>
            <div class="form-check form-check-inline">
              <input
                id="dt-percent"
                v-model="form.discountType"
                class="form-check-input"
                type="radio"
                value="percent"
              />
              <label class="form-check-label" for="dt-percent"
                >Phần trăm (%)</label
              >
            </div>
            <div class="form-check form-check-inline">
              <input
                id="dt-cash"
                v-model="form.discountType"
                class="form-check-input"
                type="radio"
                value="cash"
              />
              <label class="form-check-label" for="dt-cash"
                >Tiền mặt (VNĐ)</label
              >
            </div>
          </div>

          <div class="col-md-6">
            <label class="form-label">
              Giá trị giảm ({{ isPercent ? "%" : "VNĐ" }})
              <span class="text-danger">*</span>
            </label>
            <div class="input-group has-validation">
              <input
                v-model.number="form.discountValue"
                type="number"
                min="0"
                :max="isPercent ? 100 : undefined"
                class="form-control"
                :class="{ 'is-invalid': errors.discountValue }"
              />
              <span class="input-group-text">{{ isPercent ? "%" : "₫" }}</span>
              <div class="invalid-feedback">{{ errors.discountValue }}</div>
            </div>
          </div>

          <div class="col-md-6">
            <label class="form-label">Giá trị đơn tối thiểu (VNĐ)</label>
            <input
              v-model.number="form.minOrder"
              type="number"
              min="0"
              class="form-control"
            />
          </div>

          <div class="col-md-6">
            <template v-if="isPercent">
              <label class="form-label">Giảm tối đa (VNĐ)</label>
              <input
                v-model.number="form.maxDiscount"
                type="number"
                min="0"
                class="form-control"
              />
              <div
                v-if="!form.maxDiscount"
                class="form-text text-warning-emphasis small mt-2"
              >
                <i class="bi bi-exclamation-triangle"></i>
                Lưu ý: Không giới hạn số tiền giảm tối đa.
              </div>
            </template>
          </div>

          <div class="col-md-6">
            <label class="form-label">
              Số lượng còn lại <span class="text-danger">*</span>
            </label>
            <input
              v-model.number="form.quantity"
              type="number"
              min="1"
              class="form-control"
              :class="{ 'is-invalid': errors.quantity }"
              placeholder="Nhập số lượng"
              :disabled="form.unlimited"
            />
            <div class="invalid-feedback">{{ errors.quantity }}</div>
            <div class="form-check mt-3">
              <input
                id="unlimited"
                v-model="form.unlimited"
                class="form-check-input"
                type="checkbox"
              />
              <label class="form-check-label" for="unlimited"
                >Vô hạn số lượng</label
              >
            </div>
          </div>

          <div class="col-md-6">
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

          <div class="col-md-6">
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

          <div class="col-12">
            <label class="form-label">Mô tả</label>
            <textarea
              v-model.trim="form.description"
              class="form-control"
              rows="2"
              maxlength="500"
              placeholder="Mô tả ngắn về phiếu"
            ></textarea>
          </div>
        </div>

        <hr class="my-4" />

        <div class="d-flex gap-2">
          <button type="submit" class="btn btn-voucher px-4" :disabled="saving">
            <span
              v-if="saving"
              class="spinner-border spinner-border-sm me-2"
            ></span>
            <i v-else class="bi bi-floppy me-2"></i>
            {{ saving ? "Đang lưu..." : "Tạo phiếu giảm giá" }}
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
import { reactive, computed, watch, inject, ref } from "vue";
import axios from "axios";
import Swal from "sweetalert2";
import { useRouter } from "vue-router";

const router = useRouter();
const baseAPI = inject("baseAPI");
const saving = ref(false);

const LOAI_GIAM = { percent: "PERCENT", cash: "AMOUNT" };
const UNLIMITED_QTY = 2147483647;

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
  discountType: "PERCENT",
  discountValue: null,
  minOrder: 0,
  maxDiscount: 0,
  quantity: null,
  unlimited: false,
  startDate: "",
  endDate: "",
  description: "",
});

const errors = reactive({});
const isPercent = computed(() => form.discountType === "percent");

const generateCode = () => {
  form.code = randomCode();
};

watch(
  () => form.discountType,
  () => {
    form.discountValue = null;
    form.maxDiscount = 0;
    delete errors.discountValue;
  },
);

watch(
  () => form.unlimited,
  (value) => {
    if (value) {
      form.quantity = null;
      delete errors.quantity;
    }
  },
);

const validate = () => {
  Object.keys(errors).forEach((key) => delete errors[key]);

  if (!form.code) errors.code = "Vui lòng nhập mã phiếu";
  if (!form.name) errors.name = "Vui lòng nhập tên phiếu";

  if (
    form.discountValue === null ||
    form.discountValue === "" ||
    form.discountValue <= 0
  ) {
    errors.discountValue = "Giá trị giảm phải lớn hơn 0";
  } else if (isPercent.value && form.discountValue > 100) {
    errors.discountValue = "Phần trăm giảm tối đa là 100";
  }

  if (!form.unlimited && (!form.quantity || form.quantity < 1)) {
    errors.quantity = "Vui lòng nhập số lượng hoặc chọn vô hạn";
  }

  if (!form.startDate) errors.startDate = "Vui lòng chọn ngày bắt đầu";

  if (!form.endDate) {
    errors.endDate = "Vui lòng chọn ngày kết thúc";
  } else if (form.startDate && form.endDate <= form.startDate) {
    errors.endDate = "Ngày kết thúc phải sau ngày bắt đầu";
  }

  return Object.keys(errors).length === 0;
};

const toLocalDateTime = (v) => (v && v.length === 16 ? v + ":00" : v);

const submit = async () => {
  if (!validate()) return;

  const data = {
    maPhieuGiamGia: form.code,
    tenPhieuGiamGia: form.name,
    loaiGiamGia: LOAI_GIAM[form.discountType], // giờ đã ra "PHAN_TRAM" / "TIEN_MAT"
    giaTriGiam: form.discountValue,
    giamToiDa:
      isPercent.value && form.maxDiscount > 0 ? form.maxDiscount : null,
    hoaDonToiThieu: form.minOrder || 0,
    ngayBatDau: toLocalDateTime(form.startDate),
    ngayKetThuc: toLocalDateTime(form.endDate),
    soLuong: form.unlimited ? UNLIMITED_QTY : form.quantity,
    trangThai: true, // ✅ THÊM: phiếu mới tạo mặc định là đang bật
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
.voucher-card {
  border-radius: 1.25rem;
}
.voucher-icon {
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
.form-check-input:checked {
  background-color: #e8284a;
  border-color: #e8284a;
}
.form-check-input:focus {
  border-color: #e8284a;
  box-shadow: 0 0 0 0.2rem rgba(232, 40, 74, 0.15);
}
.btn-voucher {
  background-color: #e8284a;
  border-color: #e8284a;
  color: #fff;
  font-weight: 600;
}
.btn-voucher:hover,
.btn-voucher:focus {
  background-color: #cf1f3f;
  border-color: #cf1f3f;
  color: #fff;
}
</style>
