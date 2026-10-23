<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import {
  layHoiThoaiKhach,
  guiTinKhach,
  layTinKhach,
  danhDaDocKhach,
  layHoiThoaiAI,
  hoiAI,
  layTinAI,
} from '@/api/chat'
import { getMaAnDanh, isNhanVienDangNhap } from '@/utils/auth'

const mo = ref(false)
const cheDo = ref('nhan-vien') // 'nhan-vien' | 'ai'
const hoiThoaiNhanVien = ref(null)
const hoiThoaiAI = ref(null)
const tinNhan = ref([])
const noiDung = ref('')
const dangGui = ref(false)
const loi = ref('')
const danhSachRef = ref(null)
let timer = null
let tuId = 0
let tuIdAI = 0

const moChat = async () => {
  mo.value = true
  await nextTick()
  try {
    await taiHoiThoai()
  } catch (e) {
    loi.value = e.message
  }
}

const doiCheDo = async (mode) => {
  if (cheDo.value === mode) return
  cheDo.value = mode
  tinNhan.value = []
  loi.value = ''
  try {
    await taiHoiThoai()
  } catch (e) {
    loi.value = e.message
  }
}

const taiHoiThoai = async () => {
  const ma = getMaAnDanh()
  if (cheDo.value === 'ai') {
    hoiThoaiAI.value = await layHoiThoaiAI()
    tuIdAI = 0
    const ds = await layTinAI(hoiThoaiAI.value.id, tuIdAI)
    tinNhan.value = [...ds]
    if (ds.length) tuIdAI = ds[ds.length - 1].id
  } else {
    hoiThoaiNhanVien.value = await layHoiThoaiKhach()
    tuId = 0
    const ds = await layTinKhach(hoiThoaiNhanVien.value.id, tuId)
    tinNhan.value = [...ds]
    if (ds.length) tuId = ds[ds.length - 1].id
    danhDaDocKhach(hoiThoaiNhanVien.value.id).catch(() => {})
  }
  await nextTick()
  cuonDuoi()
}

const poll = async () => {
  if (!mo.value || dangGui.value) return
  try {
    if (cheDo.value === 'ai') {
      if (!hoiThoaiAI.value) return
      const ds = await layTinAI(hoiThoaiAI.value.id, tuIdAI)
      if (ds.length) {
        tinNhan.value = [...tinNhan.value, ...ds]
        tuIdAI = ds[ds.length - 1].id
        await nextTick()
        cuonDuoi()
      }
      return
    }
    if (!hoiThoaiNhanVien.value) return
    const ds = await layTinKhach(hoiThoaiNhanVien.value.id, tuId)
    if (ds.length) {
      tinNhan.value = [...tinNhan.value, ...ds]
      tuId = ds[ds.length - 1].id
      danhDaDocKhach(hoiThoaiNhanVien.value.id).catch(() => {})
      await nextTick()
      cuonDuoi()
    }
  } catch {
    /* bỏ qua lỗi poll */
  }
}

const cuonDuoi = () => {
  const el = danhSachRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const gui = async () => {
  const text = noiDung.value.trim()
  if (!text || dangGui.value) return
  dangGui.value = true
  loi.value = ''
  try {
    if (cheDo.value === 'ai') {
      const kq = await hoiAI(text)
      tinNhan.value = [
        ...tinNhan.value,
        kq.tinKhach,
        {
          id: `ai-${Date.now()}`,
          loaiNguoiGui: 'AI',
          noiDung: kq.noiDungAI,
          ngayTao: new Date().toISOString(),
        },
      ]
      tuIdAI = typeof kq.tinKhach.id === 'number' ? kq.tinKhach.id : tuIdAI
      noiDung.value = ''
      await nextTick()
      cuonDuoi()
    } else {
      if (!hoiThoaiNhanVien.value) {
        hoiThoaiNhanVien.value = await layHoiThoaiKhach()
      }
      const tin = await guiTinKhach(hoiThoaiNhanVien.value.id, text)
      tinNhan.value = [...tinNhan.value, tin]
      tuId = tin.id
      noiDung.value = ''
      await nextTick()
      cuonDuoi()
    }
  } catch (e) {
    loi.value = e.message
  } finally {
    dangGui.value = false
  }
}

const tenNguoiGui = (t) => {
  if (t.loaiNguoiGui === 'KHACH') return 'Bạn'
  if (t.loaiNguoiGui === 'AI') return 'Trợ lý AI'
  return t.tenNguoiGui ? `Nhân viên ${t.tenNguoiGui}` : 'Nhân viên'
}

onMounted(() => {
  timer = setInterval(poll, 3000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})

watch(mo, (v) => {
  if (!v && timer) {
    /* vẫn poll để badge khi mở lại */
  }
})
</script>

<template>
  <div v-if="!isNhanVienDangNhap()" class="chat-widget-root">
    <button class="chat-fab" type="button" :title="mo ? 'Đóng chat' : 'Chat hỗ trợ'" @click="mo ? (mo = false) : moChat()">
      <svg v-if="!mo" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
        stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
      </svg>
      <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
        stroke-linecap="round" stroke-linejoin="round">
        <line x1="18" y1="6" x2="6" y2="18" />
        <line x1="6" y1="6" x2="18" y2="18" />
      </svg>
    </button>

    <div v-if="mo" class="chat-panel">
      <div class="chat-header">
        <strong>PolyShoes hỗ trợ</strong>
        <div class="chat-tabs">
          <button type="button" class="tab" :class="{ active: cheDo === 'nhan-vien' }" @click="doiCheDo('nhan-vien')">
            Nhân viên
          </button>
          <button type="button" class="tab" :class="{ active: cheDo === 'ai' }" @click="doiCheDo('ai')">
            Trợ lý AI
          </button>
        </div>
      </div>

      <div ref="danhSachRef" class="chat-list">
        <p v-if="loi" class="chat-error">{{ loi }}</p>
        <p v-if="!tinNhan.length && !loi" class="chat-empty">
          {{ cheDo === 'ai' ? 'Hỏi mình bất cứ điều gì về giày dép PolyShoes!' : 'Gửi tin nhắn, nhân viên sẽ phản hồi trong giây lát.' }}
        </p>
        <div v-for="t in tinNhan" :key="t.id" class="chat-bubble" :class="{
          me: t.loaiNguoiGui === 'KHACH',
          ai: t.loaiNguoiGui === 'AI',
          staff: t.loaiNguoiGui === 'NHAN_VIEN',
        }">
          <span class="bubble-from">{{ tenNguoiGui(t) }}</span>
          <p class="bubble-text">{{ t.noiDung }}</p>
        </div>
      </div>

      <form class="chat-input" @submit.prevent="gui">
        <input v-model="noiDung" type="text" placeholder="Nhập tin nhắn..." :disabled="dangGui" maxlength="2000" />
        <button type="submit" :disabled="dangGui || !noiDung.trim()">
          {{ dangGui ? '...' : 'Gửi' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.chat-widget-root {
  position: fixed;
  right: 20px;
  bottom: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.chat-fab {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.chat-panel {
  width: 340px;
  height: 460px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 16px 48px rgba(15, 23, 42, 0.25);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.chat-header {
  background: #0f172a;
  color: #fff;
  padding: 12px 14px 10px;
}

.chat-header strong {
  display: block;
  font-size: 14px;
  margin-bottom: 8px;
}

.chat-tabs {
  display: flex;
  gap: 6px;
}

.chat-tabs .tab {
  flex: 1;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: #cbd5e1;
  padding: 6px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.chat-tabs .tab.active {
  background: #2563eb;
  color: #fff;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #f8fafc;
}

.chat-empty {
  color: #64748b;
  font-size: 13px;
  text-align: center;
  margin-top: 24px;
}

.chat-error {
  color: #dc2626;
  font-size: 12px;
  text-align: center;
}

.chat-bubble {
  max-width: 85%;
  padding: 8px 10px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e2e8f0;
  align-self: flex-start;
}

.chat-bubble.me {
  align-self: flex-end;
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.chat-bubble.ai {
  background: #f0f9ff;
  border-color: #bae6fd;
}

.chat-bubble.me .bubble-from {
  color: #bfdbfe;
}

.bubble-from {
  display: block;
  font-size: 10px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 2px;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.bubble-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.chat-input input {
  flex: 1;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 9px 10px;
  font-size: 13px;
  outline: none;
  box-sizing: border-box;
}

.chat-input button {
  border: none;
  background: #2563eb;
  color: #fff;
  border-radius: 8px;
  padding: 0 14px;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}

.chat-input button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
