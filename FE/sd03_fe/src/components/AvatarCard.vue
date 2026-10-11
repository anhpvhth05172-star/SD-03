<script setup>
import { computed, ref, watch } from 'vue'
import { initialsOf } from '../utils/format'

const props = defineProps({
  ten: { type: String, default: '' },
  phu: { type: String, default: '' },
  anh: { type: String, default: '' },
})

const emit = defineEmits(['change'])

const TOI_DA_KICH_THUOC = 5 * 1024 * 1024 // 5MB

const fileEl = ref(null)
const preview = ref('')
const loi = ref('')
const anhGocThatBai = ref(false)

watch(
  () => props.anh,
  (moi) => {
    anhGocThatBai.value = false
    if (!preview.value && moi) {
      preview.value = moi
    }
  },
  { immediate: true },
)

const src = computed(() => preview.value || (anhGocThatBai.value ? '' : props.anh))
const initials = computed(() => initialsOf(props.ten))

const moChonAnh = () => {
  if (fileEl.value) {
    fileEl.value.click()
  }
}

const chonAnh = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    loi.value = 'Chỉ chấp nhận các tệp định dạng hình ảnh (PNG, JPG, JPEG, WebP, GIF...)'
    return
  }

  if (file.size > TOI_DA_KICH_THUOC) {
    loi.value = 'Dung lượng ảnh tối đa 5MB'
    return
  }

  loi.value = ''
  try {
    const dataUrl = await new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.onload = () => resolve(reader.result)
      reader.onerror = reject
      reader.readAsDataURL(file)
    })
    preview.value = dataUrl
    anhGocThatBai.value = false
    emit('change', { file, dataUrl })
  } catch {
    loi.value = 'Không thể đọc tệp ảnh, vui lòng thử lại'
  }
}

const xoaAnh = (e) => {
  e.stopPropagation()
  preview.value = ''
  anhGocThatBai.value = false
  loi.value = ''
  if (fileEl.value) fileEl.value.value = ''
  emit('change', { file: null, dataUrl: '' })
}
</script>

<template>
  <aside class="avatar-card" @click="moChonAnh">
    <input ref="fileEl" class="avatar-file" type="file" accept="image/*" @click.stop @change="chonAnh" />

    <div class="avatar-wrapper" title="Bấm vào để chọn ảnh đại diện">
      <div class="avatar-ring">
        <img v-if="src" :src="src" alt="Ảnh đại diện" @error="anhGocThatBai = true" />
        <span v-else class="avatar-initials">{{ initials }}</span>
        <div class="avatar-overlay">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
            stroke-linejoin="round">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
            <circle cx="12" cy="13" r="4" />
          </svg>
          <span class="overlay-text">{{ src ? 'Đổi ảnh' : 'Tải ảnh' }}</span>
        </div>
      </div>
      <div class="camera-badge" :title="src ? 'Đổi ảnh đại diện' : 'Thêm ảnh đại diện'">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"
          stroke-linejoin="round">
          <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
          <circle cx="12" cy="13" r="4" />
        </svg>
      </div>
    </div>

    <p class="avatar-name">{{ ten || 'Chưa có tên' }}</p>
    <p v-if="phu" class="avatar-sub">{{ phu }}</p>
    <button type="button" class="btn-avatar-select" @click.stop="moChonAnh">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
        stroke-linejoin="round">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
        <polyline points="17 8 12 3 7 8" />
        <line x1="12" y1="3" x2="12" y2="15" />
      </svg>
      {{ src ? 'Đổi ảnh đại diện' : 'Chọn ảnh đại diện' }}
    </button>

    <div v-if="src" class="btn-remove-wrap" @click.stop="xoaAnh">
      <button type="button" class="btn-remove-avatar">Gỡ ảnh</button>
    </div>

    <p class="avatar-hint" @click.stop="moChonAnh">(Bấm vào ảnh để chọn avatar)</p>
    <p v-if="loi" class="avatar-loi" @click.stop>{{ loi }}</p>
  </aside>
</template>

<style scoped>
.avatar-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(20, 20, 22, 0.05);
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.avatar-card:hover {
  border-color: #cbd5e1;
  box-shadow: 0 6px 20px rgba(20, 20, 22, 0.08);
}

.avatar-file {
  display: none;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  cursor: pointer;
}

.avatar-ring {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 2px dashed #cbd5e1;
  background: #f8fafc;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transition: all 0.2s ease;
}

.avatar-card:hover .avatar-ring,
.avatar-ring:hover {
  border-color: #3b82f6;
  border-style: solid;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.12);
}

.avatar-ring img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-initials {
  font-size: 36px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.5px;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(15, 23, 42, 0.55);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s ease;
  border-radius: 50%;
}

.avatar-overlay svg {
  width: 24px;
  height: 24px;
}

.overlay-text {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.avatar-ring:hover .avatar-overlay,
.avatar-card:hover .avatar-overlay {
  opacity: 1;
}

.camera-badge {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #2563eb;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  border: 2px solid #ffffff;
  transition: transform 0.2s ease, background-color 0.2s ease;
}

.camera-badge svg {
  width: 16px;
  height: 16px;
}

.avatar-card:hover .camera-badge {
  transform: scale(1.1);
  background: #1d4ed8;
}

.avatar-name {
  margin-top: 6px;
  width: 100%;
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  overflow-wrap: anywhere;
}

.avatar-sub {
  width: 100%;
  font-size: 12px;
  color: #64748b;
  overflow-wrap: anywhere;
  margin-top: -4px;
}

.btn-avatar-select {
  margin-top: 4px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-avatar-select svg {
  width: 14px;
  height: 14px;
}

.btn-avatar-select:hover {
  background: #dbeafe;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.btn-remove-wrap {
  margin-top: -2px;
}

.btn-remove-avatar {
  background: none;
  border: none;
  color: #ef4444;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
  padding: 2px 6px;
}

.btn-remove-avatar:hover {
  color: #b91c1c;
}

.avatar-hint {
  width: 100%;
  font-size: 11.5px;
  color: #94a3b8;
  cursor: pointer;
}

.avatar-hint:hover {
  color: #2563eb;
}

.avatar-loi {
  width: 100%;
  font-size: 12px;
  font-weight: 600;
  color: #dc2626;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 6px;
  padding: 4px 8px;
}
</style>
