<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import {
  dsHoiThoaiNhanVien,
  guiTinNhanVien,
  layTinNhanVien,
  danhDaDocNhanVien,
  dongHoiThoai,
} from '@/api/chat'

const dsHoiThoai = ref([])
const hoiThoaiChon = ref(null)
const tinNhan = ref([])
const noiDung = ref('')
const dangGui = ref(false)
const loi = ref('')
const danhSachRef = ref(null)
let timer = null
let tuId = 0

const taiDanhSach = async () => {
  try {
    dsHoiThoai.value = await dsHoiThoaiNhanVien()
  } catch (e) {
    loi.value = e.message
  }
}

const chonHoiThoai = async (h) => {
  hoiThoaiChon.value = h
  tinNhan.value = []
  tuId = 0
  loi.value = ''
  try {
    const ds = await layTinNhanVien(h.id, tuId)
    tinNhan.value = [...ds]
    if (ds.length) tuId = ds[ds.length - 1].id
    danhDaDocNhanVien(h.id).catch(() => {})
    await nextTick()
    cuonDuoi()
  } catch (e) {
    loi.value = e.message
  }
}

const poll = async () => {
  try {
    await taiDanhSach()
    if (!hoiThoaiChon.value) return
    const ds = await layTinNhanVien(hoiThoaiChon.value.id, tuId)
    if (ds.length) {
      tinNhan.value = [...tinNhan.value, ...ds]
      tuId = ds[ds.length - 1].id
      danhDaDocNhanVien(hoiThoaiChon.value.id).catch(() => {})
      await nextTick()
      cuonDuoi()
    }
  } catch {
    /* bỏ qua poll lỗi */
  }
}

const cuonDuoi = () => {
  const el = danhSachRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const gui = async () => {
  const text = noiDung.value.trim()
  if (!text || dangGui.value || !hoiThoaiChon.value) return
  dangGui.value = true
  loi.value = ''
  try {
    const tin = await guiTinNhanVien(hoiThoaiChon.value.id, text)
    tinNhan.value = [...tinNhan.value, tin]
    tuId = tin.id
    noiDung.value = ''
    await taiDanhSach()
    await nextTick()
    cuonDuoi()
  } catch (e) {
    loi.value = e.message
  } finally {
    dangGui.value = false
  }
}

const dong = async () => {
  if (!hoiThoaiChon.value) return
  try {
    await dongHoiThoai(hoiThoaiChon.value.id)
    hoiThoaiChon.value = null
    tinNhan.value = []
    await taiDanhSach()
  } catch (e) {
    loi.value = e.message
  }
}

const tenNguoiGui = (t) => {
  if (t.loaiNguoiGui === 'KHACH') return 'Khách'
  if (t.loaiNguoiGui === 'AI') return 'AI'
  return 'Bạn'
}

const gio = (s) => {
  if (!s) return ''
  try {
    return new Date(s).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
  } catch {
    return ''
  }
}

onMounted(() => {
  taiDanhSach()
  timer = setInterval(poll, 3000)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="staff-chat">
    <aside class="conv-list">
      <h2>Hội thoại khách hàng</h2>
      <p v-if="!dsHoiThoai.length" class="empty">Chưa có hội thoại nào</p>
      <button v-for="h in dsHoiThoai" :key="h.id" type="button" class="conv-item"
        :class="{ active: hoiThoaiChon?.id === h.id }" @click="chonHoiThoai(h)">
        <div class="conv-top">
          <strong>{{ h.tenKhach || 'Khách ẩn danh' }}</strong>
          <span v-if="h.tinChuaDoc" class="badge">{{ h.tinChuaDoc }}</span>
        </div>
        <div class="conv-meta">
          <span>{{ h.trangThai === 'DANG_MO' ? 'Đang mở' : 'Đã đóng' }}</span>
          <span>{{ h.tongTinKhach + h.tongTinNhanVien }} tin</span>
        </div>
      </button>
    </aside>

    <section class="chat-area">
      <div v-if="!hoiThoaiChon" class="chat-placeholder">
        <p>Chọn một hội thoại bên trái để bắt đầu trả lời khách</p>
      </div>
      <template v-else>
        <header class="chat-top">
          <div>
            <strong>{{ hoiThoaiChon.tenKhach || 'Khách ẩn danh' }}</strong>
            <span class="status" :class="hoiThoaiChon.trangThai">
              {{ hoiThoaiChon.trangThai === 'DANG_MO' ? 'Đang mở' : 'Đã đóng' }}
            </span>
          </div>
          <button v-if="hoiThoaiChon.trangThai === 'DANG_MO'" type="button" class="btn-close" @click="dong">
            Đóng hội thoại
          </button>
        </header>

        <div ref="danhSachRef" class="messages">
          <p v-if="loi" class="error">{{ loi }}</p>
          <p v-if="!tinNhan.length && !loi" class="empty-msg">Chưa có tin nhắn</p>
          <div v-for="t in tinNhan" :key="t.id" class="msg" :class="{
            me: t.loaiNguoiGui === 'NHAN_VIEN',
            guest: t.loaiNguoiGui === 'KHACH',
            ai: t.loaiNguoiGui === 'AI',
          }">
            <div class="msg-head">
              <span>{{ tenNguoiGui(t) }}</span>
              <span class="time">{{ gio(t.ngayTao) }}</span>
            </div>
            <p class="msg-text">{{ t.noiDung }}</p>
          </div>
        </div>

        <form class="composer" @submit.prevent="gui">
          <input v-model="noiDung" type="text" placeholder="Nhập trả lời cho khách..." :disabled="dangGui"
            :readonly="hoiThoaiChon.trangThai !== 'DANG_MO'" maxlength="2000" />
          <button type="submit" :disabled="dangGui || !noiDung.trim() || hoiThoaiChon.trangThai !== 'DANG_MO'">
            {{ dangGui ? '...' : 'Gửi' }}
          </button>
        </form>
      </template>
    </section>
  </div>
</template>

<style scoped>
.staff-chat {
  display: flex;
  height: calc(100vh - 120px);
  min-height: 480px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
}

.conv-list {
  width: 280px;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  background: #f8fafc;
}

.conv-list h2 {
  margin: 0;
  padding: 14px 16px;
  font-size: 15px;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}

.conv-item {
  border: none;
  border-bottom: 1px solid #f1f5f9;
  background: transparent;
  padding: 12px 16px;
  text-align: left;
  cursor: pointer;
  width: 100%;
}

.conv-item:hover {
  background: #f1f5f9;
}

.conv-item.active {
  background: #eff6ff;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.conv-top strong {
  font-size: 13.5px;
  color: #0f172a;
}

.badge {
  background: #dc2626;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 10px;
  padding: 1px 7px;
}

.conv-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #64748b;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.chat-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e2e8f0;
}

.status {
  margin-left: 10px;
  font-size: 12px;
  color: #64748b;
}

.status.DANG_MO {
  color: #16a34a;
  font-weight: 600;
}

.btn-close {
  border: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: #f8fafc;
}

.msg {
  max-width: 70%;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 8px 12px;
  align-self: flex-start;
}

.msg.me {
  align-self: flex-end;
  background: #2563eb;
  border-color: #2563eb;
  color: #fff;
}

.msg.ai {
  background: #f0f9ff;
  border-color: #bae6fd;
}

.msg.me .msg-head {
  color: #bfdbfe;
}

.msg-head {
  display: flex;
  gap: 8px;
  font-size: 11px;
  font-weight: 700;
  color: #64748b;
  margin-bottom: 2px;
  text-transform: uppercase;
}

.time {
  font-weight: 500;
  opacity: 0.8;
}

.msg-text {
  margin: 0;
  font-size: 13.5px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-break: break-word;
}

.empty-msg {
  text-align: center;
  color: #94a3b8;
  margin-top: 32px;
}

.error {
  color: #dc2626;
  text-align: center;
  font-size: 13px;
}

.empty {
  padding: 16px;
  color: #94a3b8;
  font-size: 13px;
}

.composer {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.composer input {
  flex: 1;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13.5px;
  outline: none;
  box-sizing: border-box;
}

.composer button {
  border: none;
  background: #2563eb;
  color: #fff;
  border-radius: 8px;
  padding: 0 18px;
  font-weight: 700;
  font-size: 13.5px;
  cursor: pointer;
}

.composer button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
