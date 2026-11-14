<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import {
  layHoiThoaiKhach,
  guiTinKhach,
  layTinKhach,
  danhDaDocKhach,
  layHoiThoaiAI,
  hoiAI,
  layTinAI,
} from '@/api/chat'
import { isNhanVienDangNhap, isKhachHangDangNhap } from '@/utils/auth'

const mo = ref(false)
const cheDo = ref('ai') // 'nhan-vien' | 'ai'
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

const MD_LINK_REGEX = /\[([^\]]+)\]\(([^)]+)\)/g

const parseMessageSegments = (content) => {
  if (!content) return []
  const segments = []
  let lastIndex = 0
  const regex = new RegExp(MD_LINK_REGEX)
  let match

  while ((match = regex.exec(content)) !== null) {
    if (match.index > lastIndex) {
      segments.push({
        isLink: false,
        text: content.slice(lastIndex, match.index),
      })
    }
    const label = match[1]
    let url = match[2].trim()
    if (url.includes('/cua-hang/')) {
      const idx = url.indexOf('/cua-hang/')
      url = url.slice(idx)
    }
    segments.push({
      isLink: true,
      text: label,
      url,
    })
    lastIndex = regex.lastIndex
  }

  if (lastIndex < content.length) {
    segments.push({
      isLink: false,
      text: content.slice(lastIndex),
    })
  }
  return segments
}

const getProductCards = (content) => {
  if (!content) return []
  const cards = []
  const seenUrls = new Set()
  const regex = new RegExp(MD_LINK_REGEX)
  let match

  while ((match = regex.exec(content)) !== null) {
    const rawName = match[1].trim()
    let url = match[2].trim()
    if (url.includes('/cua-hang/')) {
      const idx = url.indexOf('/cua-hang/')
      url = url.slice(idx)
    }
    if (url.startsWith('/cua-hang/') && !seenUrls.has(url)) {
      seenUrls.add(url)
      cards.push({
        name: rawName,
        url,
      })
    }
  }
  return cards
}

const handleLinkClick = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
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
      if (ds && ds.length) {
        const existingIds = new Set(tinNhan.value.map((t) => t.id))
        const newMessages = ds.filter((t) => !existingIds.has(t.id))
        if (newMessages.length) {
          tinNhan.value = [...tinNhan.value, ...newMessages]
          tuIdAI = ds[ds.length - 1].id
          await nextTick()
          cuonDuoi()
        }
      }
      return
    }
    if (!hoiThoaiNhanVien.value) return
    const ds = await layTinKhach(hoiThoaiNhanVien.value.id, tuId)
    if (ds && ds.length) {
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
      noiDung.value = ''
      await hoiAI(text)
      if (hoiThoaiAI.value) {
        const ds = await layTinAI(hoiThoaiAI.value.id, tuIdAI)
        if (ds && ds.length) {
          const existingIds = new Set(tinNhan.value.map((t) => t.id))
          const newMessages = ds.filter((t) => !existingIds.has(t.id))
          if (newMessages.length) {
            tinNhan.value = [...tinNhan.value, ...newMessages]
            tuIdAI = ds[ds.length - 1].id
          }
        }
      }
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
</script>

<template>
  <div v-if="!isNhanVienDangNhap()" class="chat-widget-root">
    <button class="chat-fab" type="button" :title="mo ? 'Đóng chat' : 'Chat hỗ trợ'" @click="mo ? (mo = false) : (mo = true)">
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
        <div v-if="isKhachHangDangNhap()" class="chat-tabs">
          <button type="button" class="tab" :class="{ active: cheDo === 'nhan-vien' }" @click="doiCheDo('nhan-vien')">
            Nhân viên
          </button>
          <button type="button" class="tab" :class="{ active: cheDo === 'ai' }" @click="doiCheDo('ai')">
            Trợ lý AI
          </button>
        </div>
      </div>

      <div v-if="!isKhachHangDangNhap()" class="chat-login-prompt">
        <p>Đăng nhập tài khoản khách hàng để trò chuyện với trợ lý AI hoặc nhân viên.</p>
        <RouterLink to="/khach-hang" class="chat-login-link">Đăng nhập khách hàng</RouterLink>
      </div>
      <template v-else>
      <div ref="danhSachRef" class="chat-list">
        <p v-if="loi" class="chat-error">{{ loi }}</p>
        <p v-if="cheDo === 'nhan-vien' && hoiThoaiNhanVien?.trangThai === 'CHO_TIEP_NHAN'" class="chat-empty">
          Yêu cầu đã gửi. Nhân viên sẽ tiếp nhận trước khi bạn có thể nhắn tin.
        </p>
        <p v-if="!tinNhan.length && !loi" class="chat-empty">
          {{ cheDo === 'ai' ? 'Hỏi mình bất cứ điều gì về giày dép PolyShoes!' : 'Gửi tin nhắn, nhân viên sẽ phản hồi trong giây lát.' }}
        </p>
        <div v-for="t in tinNhan" :key="t.id" class="chat-bubble" :class="{
          me: t.loaiNguoiGui === 'KHACH',
          ai: t.loaiNguoiGui === 'AI',
          staff: t.loaiNguoiGui === 'NHAN_VIEN',
        }">
          <span class="bubble-from">{{ tenNguoiGui(t) }}</span>
          <p class="bubble-text">
            <template v-for="(seg, idx) in parseMessageSegments(t.noiDung)" :key="idx">
              <RouterLink
                v-if="seg.isLink && seg.url.startsWith('/')"
                :to="seg.url"
                class="chat-product-link"
                @click="handleLinkClick"
              >
                {{ seg.text }}
                <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="link-icon">
                  <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                  <polyline points="15 3 21 3 21 9" />
                  <line x1="10" y1="14" x2="21" y2="3" />
                </svg>
              </RouterLink>
              <a
                v-else-if="seg.isLink"
                :href="seg.url"
                target="_blank"
                rel="noopener noreferrer"
                class="chat-product-link"
              >
                {{ seg.text }}
              </a>
              <span v-else>{{ seg.text }}</span>
            </template>
          </p>

          <!-- Thẻ sản phẩm gợi ý để bấm vào xem chi tiết sản phẩm ngay -->
          <div v-if="getProductCards(t.noiDung).length > 0" class="chat-product-cards">
            <RouterLink
              v-for="(card, cIdx) in getProductCards(t.noiDung)"
              :key="cIdx"
              :to="card.url"
              class="chat-product-card-item"
              @click="handleLinkClick"
            >
              <div class="product-card-icon">👟</div>
              <div class="product-card-text">
                <span class="product-card-title">{{ card.name }}</span>
                <span class="product-card-sub">Xem chi tiết sản phẩm ➔</span>
              </div>
            </RouterLink>
          </div>
        </div>
      </div>

      <form v-if="cheDo === 'ai' || hoiThoaiNhanVien?.trangThai === 'DANG_MO'" class="chat-input" @submit.prevent="gui">
        <input v-model="noiDung" type="text" placeholder="Nhập tin nhắn..." :disabled="dangGui" maxlength="2000" />
        <button type="submit" :disabled="dangGui || !noiDung.trim()">
          {{ dangGui ? '...' : 'Gửi' }}
        </button>
      </form>
      </template>
    </div>
  </div>
</template>

<style scoped>
.chat-login-prompt { padding: 24px; text-align: center; color: #475569; }
.chat-login-link { display: inline-block; padding: 8px 16px; border-radius: 8px; background: #2563eb; color: #fff; text-decoration: none; }

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

.chat-product-link {
  color: #0284c7;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 2px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  word-break: break-word;
  transition: color 0.15s ease;
}

.chat-product-link:hover {
  color: #0369a1;
}

.link-icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}

.chat-product-cards {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.chat-product-card-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #ffffff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.chat-product-card-item:hover {
  background: #f0f9ff;
  border-color: #0284c7;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(2, 132, 199, 0.15);
}

.product-card-icon {
  font-size: 16px;
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  background: #e0f2fe;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-card-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.product-card-title {
  font-size: 12px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.product-card-sub {
  font-size: 11px;
  color: #0284c7;
  font-weight: 600;
  margin-top: 1px;
}
</style>
