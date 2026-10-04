<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const API_BASE = 'http://localhost:8080/api/v1'

const navigateToAdd = () => {
  router.push('/san-pham/them')
}

// Data State
const products = ref([])
const isLoading = ref(false)
const totalElements = ref(0)
const totalPages = ref(1)
const currentPage = ref(0)
const pageSize = ref(5)
const pageSizeOptions = [5, 10, 20, 50]

// Filter Tabs (Status)
const activeTab = ref('ALL') // 'ALL', 'ACTIVE', 'INACTIVE'

// Filters
const filters = reactive({
  keyword: '',
  idThuongHieu: '',
  idLoaiGiay: '',
  idChatLieu: '',
  idKieuDang: '',
  idXuatXu: '',
  doiTuong: ''
})

// Options for dropdowns
const brands = ref([])
const categories = ref([])
const materials = ref([])
const styles = ref([])
const origins = ref([])

let debounceTimer = null
let currentAbortController = null

// Load Dropdown Options
const fetchOptions = async () => {
  try {
    const [resTH, resLG, resCL, resKD, resXX] = await Promise.all([
      fetch(`${API_BASE}/attributes/thuong_hieu`),
      fetch(`${API_BASE}/attributes/loai_giay`),
      fetch(`${API_BASE}/attributes/chat_lieu`),
      fetch(`${API_BASE}/attributes/kieu_dang`),
      fetch(`${API_BASE}/attributes/xuat_xu`)
    ])

    if (resTH.ok) brands.value = await resTH.json()
    if (resLG.ok) categories.value = await resLG.json()
    if (resCL.ok) materials.value = await resCL.json()
    if (resKD.ok) styles.value = await resKD.json()
    if (resXX.ok) origins.value = await resXX.json()
  } catch (err) {
    console.error('Lỗi tải danh mục bộ lọc:', err)
  }
}

// Memory cache for instant page switching
const pageCache = new Map()

// Fetch Products from Backend
const fetchProducts = async (useCache = true) => {
  const cacheKey = JSON.stringify({
    page: currentPage.value,
    size: pageSize.value,
    tab: activeTab.value,
    keyword: filters.keyword.trim(),
    th: filters.idThuongHieu,
    lg: filters.idLoaiGiay,
    cl: filters.idChatLieu,
    kd: filters.idKieuDang,
    xx: filters.idXuatXu,
    dt: filters.doiTuong
  })

  // Instant render from cache if available
  if (useCache && pageCache.has(cacheKey)) {
    const cached = pageCache.get(cacheKey)
    products.value = cached.content
    totalElements.value = cached.totalElements
    totalPages.value = cached.totalPages
  } else {
    isLoading.value = true
  }

  if (currentAbortController) {
    currentAbortController.abort()
  }
  currentAbortController = new AbortController()

  try {
    const params = new URLSearchParams()
    params.append('page', currentPage.value)
    params.append('size', pageSize.value)

    if (filters.keyword && filters.keyword.trim()) params.append('keyword', filters.keyword.trim())
    if (filters.idThuongHieu) params.append('idThuongHieu', filters.idThuongHieu)
    if (filters.idLoaiGiay) params.append('idLoaiGiay', filters.idLoaiGiay)
    if (filters.idChatLieu) params.append('idChatLieu', filters.idChatLieu)
    if (filters.idKieuDang) params.append('idKieuDang', filters.idKieuDang)
    if (filters.idXuatXu) params.append('idXuatXu', filters.idXuatXu)
    if (filters.doiTuong) params.append('doiTuong', filters.doiTuong)

    if (activeTab.value === 'ACTIVE') params.append('trangThai', 'true')
    else if (activeTab.value === 'INACTIVE') params.append('trangThai', 'false')

    const res = await fetch(`${API_BASE}/san-pham?${params.toString()}`, {
      signal: currentAbortController.signal
    })

    if (res.ok) {
      const data = await res.json()
      const content = data.content || []
      const total = data.totalElements !== undefined ? data.totalElements : content.length
      const pages = data.totalPages || 1

      products.value = content
      totalElements.value = total
      totalPages.value = pages

      // Save to cache
      pageCache.set(cacheKey, { content, totalElements: total, totalPages: pages })
    }
  } catch (err) {
    if (err.name !== 'AbortError') {
      console.error('Lỗi khi gọi API sản phẩm:', err)
    }
  } finally {
    isLoading.value = false
  }
}

const selectTab = (tab) => {
  activeTab.value = tab
  currentPage.value = 0
  fetchProducts(false)
}

const onFilterChange = () => {
  currentPage.value = 0
  fetchProducts(false)
}

const onSearchInput = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    currentPage.value = 0
    fetchProducts(false)
  }, 200)
}

const changePage = (p) => {
  if (p >= 0 && p < totalPages.value && p !== currentPage.value) {
    currentPage.value = p
    fetchProducts(true) // Instant cache read
  }
}

const deleteProduct = async (id) => {
  if (!confirm('Bạn có chắc chắn muốn xóa sản phẩm này?')) return
  const originalList = [...products.value]
  const targetProd = products.value.find(p => p.id === id)
  
  // Instant UI update
  products.value = products.value.filter(p => p.id !== id)
  if (totalElements.value > 0) totalElements.value--
  
  try {
    const res = await fetch(`${API_BASE}/san-pham/${id}`, { method: 'DELETE' })
    if (!res.ok) {
      // Revert if error
      products.value = originalList
      if (totalElements.value >= 0) totalElements.value++
      alert('Không thể xóa sản phẩm do có dữ liệu liên quan!')
    } else {
      pageCache.clear() // Clear cache so next page refresh is fresh
    }
  } catch (err) {
    products.value = originalList
    if (totalElements.value >= 0) totalElements.value++
  }
}

onMounted(() => {
  fetchOptions()
  fetchProducts()
})

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (currentAbortController) currentAbortController.abort()
})
</script>

<template>
  <div class="product-management-page">
    <!-- Header -->
    <div class="page-top-row">
      <div class="page-intro">
        <h1 class="page-heading">
          Quản lý Sản phẩm
          <span class="count-badge">{{ totalElements }} sản phẩm</span>
        </h1>
        <p class="page-subheading">
          Quản lý toàn bộ danh mục sản phẩm, bộ lọc thuộc tính đa chiều &amp; chi tiết mặt hàng.
        </p>
      </div>

      <div class="page-actions">
        <button class="btn btn-add-new" @click="navigateToAdd">
          <span class="plus-sign">+</span>
          Thêm sản phẩm mới
        </button>
      </div>
    </div>

    <!-- Filter Card -->
    <div class="filter-box">
      <!-- Status Tabs -->
      <div class="status-tabs-row">
        <button
          class="status-tab"
          :class="{ active: activeTab === 'ALL' }"
          @click="selectTab('ALL')"
        >
          Tất cả ({{ totalElements }})
        </button>
        <button
          class="status-tab"
          :class="{ active: activeTab === 'ACTIVE' }"
          @click="selectTab('ACTIVE')"
        >
          <span class="dot green-dot"></span>
          Đang kinh doanh
        </button>
        <button
          class="status-tab"
          :class="{ active: activeTab === 'INACTIVE' }"
          @click="selectTab('INACTIVE')"
        >
          <span class="dot gray-dot"></span>
          Ngừng kinh doanh
        </button>
      </div>

      <!-- Filters Grid -->
      <div class="filters-grid">
        <!-- Row 1 -->
        <div class="filter-col flex-2">
          <label class="filter-lbl">Tìm kiếm sản phẩm</label>
          <div class="search-wrap">
            <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input
              v-model="filters.keyword"
              type="text"
              class="form-control"
              placeholder="Nhập tên hoặc mã sản phẩm (vd: SP001, Nike...)"
              @input="onSearchInput"
            />
          </div>
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Thương hiệu</label>
          <select v-model="filters.idThuongHieu" class="form-select" @change="onFilterChange">
            <option value="">Tất cả thương hiệu</option>
            <option v-for="b in brands" :key="b.id" :value="b.id">{{ b.ten }}</option>
          </select>
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Loại giày</label>
          <select v-model="filters.idLoaiGiay" class="form-select" @change="onFilterChange">
            <option value="">Tất cả loại giày</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.ten }}</option>
          </select>
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Chất liệu</label>
          <select v-model="filters.idChatLieu" class="form-select" @change="onFilterChange">
            <option value="">Tất cả chất liệu</option>
            <option v-for="m in materials" :key="m.id" :value="m.id">{{ m.ten }}</option>
          </select>
        </div>

        <!-- Row 2 -->
        <div class="filter-col">
          <label class="filter-lbl">Kiểu dáng</label>
          <select v-model="filters.idKieuDang" class="form-select" @change="onFilterChange">
            <option value="">Tất cả kiểu dáng</option>
            <option v-for="s in styles" :key="s.id" :value="s.id">{{ s.ten }}</option>
          </select>
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Xuất xứ</label>
          <select v-model="filters.idXuatXu" class="form-select" @change="onFilterChange">
            <option value="">Tất cả xuất xứ</option>
            <option v-for="o in origins" :key="o.id" :value="o.id">{{ o.ten }}</option>
          </select>
        </div>

        <div class="filter-col">
          <label class="filter-lbl">Đối tượng</label>
          <select v-model="filters.doiTuong" class="form-select" @change="onFilterChange">
            <option value="">Tất cả đối tượng</option>
            <option value="Nam">Nam</option>
            <option value="Nữ">Nữ</option>
            <option value="Unisex">Unisex</option>
          </select>
        </div>
      </div>
    </div>

    <!-- Table Card -->
    <div class="table-container-card">
      <div class="table-card-head">
        <div class="head-left">
          <h2 class="card-title">Danh sách sản phẩm</h2>
          <span class="items-count-pill">{{ products.length }}/{{ totalElements }} mặt hàng</span>
        </div>
        <div class="head-right">
          <span class="page-size-label">Kích thước trang:</span>
          <select v-model="pageSize" class="page-size-select" @change="fetchProducts">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}/trang</option>
          </select>
        </div>
      </div>

      <div class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th width="40"><input type="checkbox" /></th>
              <th width="130">Mã sản phẩm</th>
              <th width="240">Tên sản phẩm</th>
              <th>Thương hiệu</th>
              <th>Loại giày</th>
              <th>Chất liệu</th>
              <th>Kiểu dáng</th>
              <th>Xuất xứ</th>
              <th width="150">Trạng thái</th>
              <th width="100" class="text-center">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in products" :key="item.id">
              <td><input type="checkbox" /></td>
              <td>
                <span class="code-badge">{{ item.maSanPham || `SP0${item.id}` }}</span>
              </td>
              <td>
                <div class="product-info-cell">
                  <span class="prod-title">{{ item.tenSanPham }}</span>
                  <span v-if="item.doiTuong" class="prod-target">Dành cho: {{ item.doiTuong }}</span>
                </div>
              </td>
              <td>{{ item.tenThuongHieu || '---' }}</td>
              <td>
                <span class="type-pill">{{ item.tenLoaiGiay || 'Sneaker' }}</span>
              </td>
              <td>{{ item.tenChatLieu || '---' }}</td>
              <td>{{ item.tenKieuDang || '---' }}</td>
              <td>{{ item.tenXuatXu || '---' }}</td>
              <td>
                <span class="status-pill" :class="{ 'is-active': item.trangThai, 'is-inactive': !item.trangThai }">
                  <span class="status-dot"></span>
                  {{ item.trangThai ? 'Đang kinh doanh' : 'Ngừng kinh doanh' }}
                </span>
              </td>
              <td class="text-center">
                <div class="action-btns">
                  <button class="act-btn view-btn" title="Xem chi tiết" @click="router.push(`/san-pham/bien-the`)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                      <circle cx="12" cy="12" r="3"></circle>
                    </svg>
                  </button>
                  <button class="act-btn del-btn" title="Xóa" @click="deleteProduct(item.id)">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="products.length === 0">
              <td colspan="10" style="text-align: center; padding: 36px; color: #64748b;">
                {{ isLoading ? 'Đang tải dữ liệu từ database...' : 'Không có sản phẩm nào phù hợp.' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="table-card-foot">
        <span class="foot-info">
          Hiển thị {{ products.length ? currentPage * pageSize + 1 : 0 }} - {{ currentPage * pageSize + products.length }} trong tổng số {{ totalElements }} mặt hàng
        </span>
        <div class="foot-pager">
          <button class="pager-btn" :disabled="currentPage === 0" @click="changePage(currentPage - 1)">
            ‹ Trước
          </button>
          <button
            v-for="p in totalPages"
            :key="p"
            class="pager-btn num-btn"
            :class="{ active: p - 1 === currentPage }"
            @click="changePage(p - 1)"
          >
            {{ p }}
          </button>
          <button class="pager-btn" :disabled="currentPage >= totalPages - 1" @click="changePage(currentPage + 1)">
            Sau ›
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-management-page {
  width: 100%;
}

.page-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-heading {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  display: flex;
  align-items: center;
  gap: 12px;
}

.count-badge {
  background: #fee2e2;
  color: #d92d20;
  font-size: 12.5px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
}

.page-subheading {
  font-size: 13.5px;
  color: #64748b;
  margin-top: 4px;
}

.btn-add-new {
  background-color: #cc0000;
  color: #ffffff;
  border: none;
  font-size: 13.5px;
  font-weight: 600;
  padding: 10px 18px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 2px 6px rgba(204, 0, 0, 0.25);
  transition: all 0.15s ease;
}

.btn-add-new:hover {
  background-color: #b30000;
  transform: translateY(-1px);
}

.plus-sign {
  font-size: 16px;
  font-weight: bold;
}

.filter-box {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  padding: 18px 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

.status-tabs-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.status-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid transparent;
  background: none;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s ease;
}

.status-tab:hover {
  background: #f8fafc;
}

.status-tab.active {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #d92d20;
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.green-dot {
  background-color: #10b981;
}

.gray-dot {
  background-color: #94a3b8;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.flex-2 {
  grid-column: span 1;
}

.filter-col {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-lbl {
  font-size: 12.5px;
  font-weight: 600;
  color: #475569;
}

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: #94a3b8;
  pointer-events: none;
}

.form-control {
  width: 100%;
  padding: 8px 12px 8px 36px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  transition: border-color 0.15s;
}

.form-control:focus,
.form-select:focus {
  border-color: #d92d20;
}

.form-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #1e293b;
  outline: none;
  background-color: #ffffff;
  cursor: pointer;
}

.table-container-card {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
}

.table-card-head {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.items-count-pill {
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}

.head-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-size-label {
  font-size: 12.5px;
  color: #64748b;
}

.page-size-select {
  padding: 4px 8px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  font-size: 12.5px;
  color: #1e293b;
  outline: none;
}

.table-responsive {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}

.data-table th {
  background: #fafafa;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  border-bottom: 1px solid #f1f5f9;
}

.data-table td {
  padding: 14px 16px;
  font-size: 13px;
  color: #334155;
  border-bottom: 1px solid #f8fafc;
}

.code-badge {
  background: #f1f5f9;
  color: #334155;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
}

.product-info-cell {
  display: flex;
  flex-direction: column;
}

.prod-title {
  font-weight: 700;
  color: #0f172a;
}

.prod-target {
  font-size: 11.5px;
  color: #64748b;
  margin-top: 2px;
}

.type-pill {
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-pill.is-active {
  background: #ecfdf5;
  color: #059669;
}

.status-pill.is-active .status-dot {
  background: #10b981;
}

.status-pill.is-inactive {
  background: #f1f5f9;
  color: #64748b;
}

.status-pill.is-inactive .status-dot {
  background: #94a3b8;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.act-btn {
  background: none;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 5px;
  color: #64748b;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}

.act-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #0f172a;
}

.table-card-foot {
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f1f5f9;
}

.foot-info {
  font-size: 12.5px;
  color: #64748b;
}

.foot-pager {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pager-btn {
  padding: 5px 10px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.pager-btn.num-btn {
  min-width: 30px;
  padding: 5px 0;
  text-align: center;
}

.pager-btn.active {
  background: #cc0000;
  color: #ffffff;
  border-color: #cc0000;
}
</style>
