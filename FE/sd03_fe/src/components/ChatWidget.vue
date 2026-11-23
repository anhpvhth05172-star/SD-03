<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import {
  layHoiThoaiKhach,
  guiTinKhach,
  layTinKhach,
  danhDaDocKhach,
  layHoiThoaiAI,
  hoiAI,
  layTinAI,
} from '@/api/chat'
import { getSanPhamById, getBienTheBySanPham } from '@/api/product'
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

const productCache = ref({})

const fetchProductInfo = async (id) => {
  if (productCache.value[id] || !id) return
  try {
    const sp = await getSanPhamById(id)
    let img = sp?.hinhAnhDaiDien || sp?.img || null
    const price = sp?.giaSauGiamMin || sp?.giaBanMin || sp?.giaBan || null
    if (!img) {
      try {
        const vars = await getBienTheBySanPham(id)
        const vList = vars?.content || vars || []
        const found = vList.find((v) => v.img)
        if (found) img = found.img
      } catch {}
    }
    productCache.value[id] = {
      name: sp?.tenSanPham,
      image: img,
      priceText: price ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price) : null
    }
  } catch {
    productCache.value[id] = { error: true }
  }
}

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
      const idMatch = url.match(/\/cua-hang\/(\d+)/)
      const prodId = idMatch ? idMatch[1] : null
      if (prodId && !productCache.value[prodId]) {
        fetchProductInfo(prodId)
      }
      const cached = prodId ? productCache.value[prodId] : null
      cards.push({
        id: prodId,
        name: cached?.name || rawName || 'Xem sản phẩm',
        image: cached?.image || null,
        priceText: cached?.priceText || null,
        url,
      })
    }
  }

  // Bắt thêm trường hợp URL thuần /cua-hang/{id}
  const rawUrlRegex = /(?:https?:\/\/[^\s/]+)?(\/cua-hang\/(\d+))/g
  let rawMatch
  while ((rawMatch = rawUrlRegex.exec(content)) !== null) {
    const url = rawMatch[1]
    const prodId = rawMatch[2]
    if (!seenUrls.has(url)) {
      seenUrls.add(url)
      if (prodId && !productCache.value[prodId]) {
        fetchProductInfo(prodId)
      }
      const cached = prodId ? productCache.value[prodId] : null
      cards.push({
        id: prodId,
        name: cached?.name || 'Giày Sneaker PolyShoes',
        image: cached?.image || null,
        priceText: cached?.priceText || null,
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

watch(mo, async (val) => {
  if (val && isKhachHangDangNhap()) {
    try {
      await taiHoiThoai()
    } catch (e) {
      loi.value = e.message
    }
  }
})

const gui = async () => {
  const text = noiDung.value.trim()
  if (!text || dangGui.value) return
  dangGui.value = true
  loi.value = ''
  try {
    if (cheDo.value === 'ai') {
      if (!hoiThoaiAI.value) {
        hoiThoaiAI.value = await layHoiThoaiAI()
      }
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

          <!-- Thẻ sản phẩm gợi ý: Ảnh vuông to ở trên, nút xem chi tiết ở dưới -->
          <div v-if="getProductCards(t.noiDung).length > 0" class="chat-product-cards-grid">
            <RouterLink
              v-for="(card, cIdx) in getProductCards(t.noiDung)"
              :key="cIdx"
              :to="card.url"
              class="chat-product-vertical-card"
              @click="handleLinkClick"
            >
              <!-- 1. Ảnh sản phẩm hình vuông to ở trên -->
              <div class="product-square-img-wrap">
                <img
                  v-if="card.image"
                  :src="card.image"
                  :alt="card.name"
                  class="product-square-img"
                />
                <div v-else class="product-square-fallback">
                  <span class="fallback-icon">👟</span>
                  <span class="fallback-badge">PolyShoes</span>
                </div>
              </div>

              <!-- 2. Tên & Giá sản phẩm ở giữa -->
              <div class="product-card-body">
                <span class="product-card-name" :title="card.name">{{ card.name }}</span>
                <span v-if="card.priceText" class="product-card-price">{{ card.priceText }}</span>
              </div>

              <!-- 3. Nút xem chi tiết ở dưới -->
              <div class="product-card-bottom-btn">
                <span>Xem chi tiết</span>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                  <polyline points="12 5 19 12 12 19"></polyline>
                </svg>
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
.chat-login-link { display: inline-block; padding: 8px 16px; border-radius: 8px; background: #dc2626; color: #fff; text-decoration: none; }

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
  background: #dc2626;
  color: #fff;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(220, 38, 38, 0.4);
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
  background: #dc2626;
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
  background: #dc2626;
  border-color: #dc2626;
  color: #fff;
}

.chat-bubble.ai {
  background: #fff8f8;
  border-color: #fee2e2;
}

.chat-bubble.me .bubble-from {
  color: #fecaca;
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
  background: #dc2626;
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
  color: #dc2626;
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
  color: #b91c1c;
}

.link-icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}

.chat-product-cards-grid {
  margin-top: 10px;
  display: flex;
  flex-direction: row;
  gap: 12px;
  overflow-x: auto;
  padding: 4px 2px 10px;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.chat-product-cards-grid::-webkit-scrollbar {
  height: 6px;
}

.chat-product-cards-grid::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 999px;
}

.chat-product-cards-grid::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 999px;
}

.chat-product-cards-grid::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

.chat-product-vertical-card {
  flex: 0 0 210px;
  max-width: 210px;
  scroll-snap-align: start;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  border: 1.5px solid #fee2e2;
  border-radius: 12px;
  overflow: hidden;
  text-decoration: none;
  color: inherit;
  transition: all 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.chat-product-vertical-card:hover {
  border-color: #dc2626;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(220, 38, 38, 0.16);
}

.product-square-img-wrap {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f8fafc;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #e2e8f0;
}

.product-square-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.chat-product-vertical-card:hover .product-square-img {
  transform: scale(1.04);
}

.product-square-fallback {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #fff5f5 0%, #fee2e2 100%);
}

.fallback-icon {
  font-size: 42px;
}

.fallback-badge {
  font-size: 11px;
  font-weight: 700;
  color: #dc2626;
  background: rgba(255, 255, 255, 0.9);
  padding: 2px 8px;
  border-radius: 10px;
  border: 1px solid #fecaca;
}

.product-card-body {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-card-name {
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-card-price {
  font-size: 13px;
  font-weight: 700;
  color: #dc2626;
}

.product-card-bottom-btn {
  margin: 0 10px 10px;
  padding: 8px 12px;
  background: #dc2626;
  color: #ffffff;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background 0.2s;
}

.chat-product-vertical-card:hover .product-card-bottom-btn {
  background: #b91c1c;
}
</style>
