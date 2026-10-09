<script setup>
import { computed, ref, watch } from 'vue'
import { initialsOf } from '../utils/format'

const props = defineProps({
  ten: { type: String, default: '' },
  phu: { type: String, default: '' },
  anh: { type: String, default: '' },
})

const emit = defineEmits(['change'])

const LOAI_CHUP_NHAN = ['image/png', 'image/jpeg', 'image/webp']
const TOI_DA_KICH_THUOC = 2 * 1024 * 1024

const fileEl = ref(null)
const preview = ref('')
const loi = ref('')
const anhGocThatBai = ref(false)

watch(
  () => props.anh,
  () => {
    anhGocThatBai.value = false
    preview.value = ''
  },
)

const src = computed(() => preview.value || (anhGocThatBai.value ? '' : props.anh))
const initials = computed(() => initialsOf(props.ten))

const moChonAnh = () => fileEl.value?.click()

const chonAnh = async (event) => {
  const file = event.target.files && event.target.files[0]
  event.target.value = ''
  if (!file) return
  if (!LOAI_CHUP_NHAN.includes(file.type)) {
    loi.value = 'Chỉ chấp nhận ảnh định dạng PNG, JPG, JPEG hoặc WebP'
    return
  }
  if (file.size > TOI_DA_KICH_THUOC) {
    loi.value = 'Dung lượng ảnh tối đa 2MB'
    return
  }
  loi.value = ''
  preview.value = await new Promise((resolve) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => resolve('')
    reader.readAsDataURL(file)
  })
  emit('change', { file, dataUrl: preview.value })
}
</script>

<template>
  <aside class="avatar-card">
    <input
      ref="fileEl"
      class="avatar-file"
      type="file"
      accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
      @change="chonAnh"
    />

    <button class="avatar-ring" type="button" title="Bấm vào ảnh để chọn avatar" @click="moChonAnh">
      <img v-if="src" :src="src" alt="Ảnh đại diện" @error="anhGocThatBai = true" />
      <span v-else class="avatar-initials">{{ initials }}</span>
    </button>

    <p class="avatar-name">{{ ten || 'Chưa có tên' }}</p>
    <p v-if="phu" class="avatar-sub">{{ phu }}</p>
    <p class="avatar-hint">(Bấm vào ảnh để chọn avatar)</p>
    <p v-if="loi" class="avatar-loi">{{ loi }}</p>
  </aside>
</template>

<style scoped>
.avatar-card {
  background: var(--white);
  border: 1px solid #ececef;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(20, 20, 22, 0.05);
  padding: 26px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  text-align: center;
}

.avatar-file {
  display: none;
}

.avatar-ring {
  width: 118px;
  height: 118px;
  border-radius: 50%;
  border: 1px dashed #d5d6dd;
  background: #f4f4f7;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.avatar-ring:hover {
  border-color: var(--red);
  box-shadow: 0 0 0 3px rgba(204, 0, 0, 0.08);
}

.avatar-ring img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-initials {
  font-size: 34px;
  font-weight: 700;
  color: #9a9aa3;
  letter-spacing: 0.5px;
}

.avatar-name {
  margin-top: 8px;
  width: 100%;
  font-size: 13.5px;
  font-weight: 700;
  color: #23232a;
  overflow-wrap: anywhere;
}

.avatar-sub {
  width: 100%;
  font-size: 12px;
  color: #8c8c95;
  overflow-wrap: anywhere;
}

.avatar-hint {
  width: 100%;
  font-size: 11.5px;
  color: #a0a0a9;
}

.avatar-loi {
  width: 100%;
  font-size: 11.5px;
  font-weight: 600;
  color: #dc2626;
}
</style>
