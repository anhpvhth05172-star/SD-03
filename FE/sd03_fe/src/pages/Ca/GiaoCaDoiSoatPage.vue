<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { notifyWarning } from '@/utils/notify'

const route = useRoute()
const router = useRouter()

const id = route.params.id
const loading = ref(true)
const notFound = ref(false)

onMounted(() => {
  loading.value = false
  notifyWarning('Đang phát triển', 'Trang đối soát ca đang được phát triển. Vui lòng quay lại sau.')
})
</script>

<template>
<div class="doi-soat-placeholder">
  <div class="placeholder-card">
    <div class="placeholder-icon">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="12" cy="12" r="10" />
        <polyline points="12 6 12 12 16 14" />
      </svg>
    </div>
    <h2 class="placeholder-title">Trang đối soát ca</h2>
    <p class="placeholder-desc">Chức năng này đang được phát triển (Task 6).</p>
    <p class="placeholder-id">Mã ca: <strong>{{ id }}</strong></p>
    <router-link to="/ca" class="btn-back">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M19 12H5M12 19l-7-7 7-7" />
      </svg>
      Quay lại lịch sử giao ca
    </router-link>
  </div>
</div>
</template>

<style scoped>
.doi-soat-placeholder {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: #f8fafc;
}

.placeholder-card {
  background: #fff;
  border-radius: 16px;
  border: 1px solid #eef2f6;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  padding: 48px 40px;
  text-align: center;
  max-width: 420px;
  width: 100%;
}

.placeholder-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  color: #b45309;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.placeholder-icon svg {
  width: 32px;
  height: 32px;
}

.placeholder-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

.placeholder-desc {
  margin: 0 0 16px;
  font-size: 15px;
  color: #64748b;
}

.placeholder-id {
  margin: 0 0 24px;
  font-size: 14px;
  color: #475569;
}

.placeholder-id strong {
  color: #1e293b;
  font-family: monospace;
}

.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.15s ease;
}

.btn-back svg {
  width: 18px;
  height: 18px;
}

.btn-back:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #1e293b;
  transform: translateX(-2px);
}
</style>