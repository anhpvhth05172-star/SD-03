<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const ranks = ['', 'Thường', 'Bạc', 'Vàng', 'Kim cương']
const provinces = ['', 'Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng', 'Hải Phòng', 'Cần Thơ']
const wards = ['', 'Phường 1', 'Phường 2', 'Phường 3', 'Phường 4', 'Phường 5']
</script>

<template>
  <div class="create-wrap">
    <section class="create-card">
      <h2 class="create-title">Tạo Tài Khoản Khách Hàng</h2>

      <div class="form-fields">
        <input type="text" placeholder="Mã khách hàng" />
        <input type="text" placeholder="Họ và tên" />
        <input type="email" placeholder="Email" />
        <input type="tel" placeholder="Số điện thoại" />
        <input type="text" placeholder="Ngày sinh" />

        <select>
          <option v-for="item in ranks" :key="item" :value="item">{{ item || 'Chọn hạng thành viên' }}</option>
        </select>

        <select>
          <option v-for="item in provinces" :key="item" :value="item">{{ item || 'Chọn tỉnh' }}</option>
        </select>

        <select>
          <option v-for="item in wards" :key="item" :value="item">{{ item || 'Chọn phường' }}</option>
        </select>

        <textarea placeholder="Địa chỉ cụ thể" rows="3"></textarea>
      </div>

      <div class="form-actions">
        <button class="btn-primary" type="button">Thêm</button>
        <button class="btn-cancel" type="button" @click="router.push('/khach-hang')">Hủy</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.create-wrap {
  min-height: 100%;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 26px 12px 40px;
}

.create-card {
  width: min(620px, 100%);
  background: var(--white);
  border: 1px solid #ececef;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(20, 20, 22, 0.05);
  padding: 30px 44px 30px;
}

.create-title {
  text-align: center;
  font-size: 15.5px;
  font-weight: 700;
  color: #1d1d23;
  margin-bottom: 22px;
}

.form-fields {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.form-fields input,
.form-fields select,
.form-fields textarea {
  width: 100%;
  height: 38px;
  padding: 0 13px;
  border: 1px solid #e3e3e8;
  border-radius: 7px;
  background: #fff;
  font-size: 13px;
  color: #4a4a52;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.form-fields textarea {
  height: auto;
  min-height: 78px;
  padding: 10px 13px;
  resize: vertical;
  line-height: 1.5;
}

.form-fields input::placeholder,
.form-fields textarea::placeholder {
  color: #9a9aa3;
}

.form-fields select {
  color: #9a9aa3;
}

.form-fields input:focus,
.form-fields select:focus,
.form-fields textarea:focus {
  border-color: var(--blue);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.btn-primary {
  height: 36px;
  min-width: 92px;
  padding: 0 22px;
  border: none;
  border-radius: 7px;
  background: var(--blue);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-primary:hover {
  background: var(--blue-dark);
}

.btn-cancel {
  height: 36px;
  min-width: 76px;
  padding: 0 18px;
  border: none;
  border-radius: 7px;
  background: #6b7280;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-cancel:hover {
  background: #59606b;
}
</style>
