<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const navigateToAdd = () => {
  router.push('/san-pham/them')
}

const stats = [
  {
    title: 'Tổng sản phẩm',
    value: '248',
    badgeText: '+12 trong tháng này',
    badgeType: 'badge-accent',
    iconColor: '#fee2e2',
    iconStroke: '#d92d20'
  },
  {
    title: 'Đang kinh doanh',
    value: '221',
    badgeText: '89.7% danh mục',
    badgeType: 'badge-success',
    iconColor: '#ecfdf5',
    iconStroke: '#059669'
  },
  {
    title: 'Sắp hết hàng',
    value: '18',
    badgeText: 'Cần nhập thêm',
    badgeType: 'badge-warning',
    iconColor: '#fffbeb',
    iconStroke: '#d97706'
  },
  {
    title: 'Ngừng kinh doanh',
    value: '9',
    badgeText: 'Không hiển thị tại quầy',
    badgeType: 'badge-secondary',
    iconColor: '#f1f5f9',
    iconStroke: '#64748b'
  }
]

const products = ref([
  {
    id: 1,
    code: 'SP-NK-0032',
    name: "Nike Air Force 1 '07",
    subtitle: 'Triple White · 8 biến thể',
    brand: 'Nike',
    origin: 'Việt Nam',
    material: 'Da trần',
    style: 'Năng động',
    category: 'Sneaker',
    price: 2929000,
    stock: 84,
    status: 'Đang bán'
  },
  {
    id: 2,
    code: 'SP-NB-0024',
    name: 'New Balance 530',
    subtitle: 'Silver Metallic / Navy · 5 biến thể',
    brand: 'New Balance',
    origin: 'China',
    material: 'Da trần',
    style: 'Năng động',
    category: 'Sneaker',
    price: 2850000,
    stock: 12,
    status: 'Sắp hết'
  },
  {
    id: 3,
    code: 'SP-CV-0011',
    name: 'Converse Chuck 70 Hi',
    subtitle: 'Black / Egret · 7 biến thể',
    brand: 'Converse',
    origin: 'Việt Nam',
    material: 'Da trần',
    style: 'Năng động',
    category: 'Sneaker',
    price: 1900000,
    stock: 53,
    status: 'Đang bán'
  },
  {
    id: 4,
    code: 'SP-VA-0009',
    name: 'Vans Old Skool',
    subtitle: 'Black / White · 4 biến thể',
    brand: 'Vans',
    origin: 'Việt Nam',
    material: 'Vải Canvas',
    style: 'Old skool',
    category: 'Sneaker',
    price: 1650000,
    stock: 0,
    status: 'Ngừng bán'
  },
  {
    id: 5,
    code: 'SP-PM-0015',
    name: 'Puma Palermo',
    subtitle: 'Green / Gum · 6 biến thể',
    brand: 'Puma',
    origin: 'Việt Nam',
    material: 'Da lộn',
    style: 'Năng động',
    category: 'Sneaker',
    price: 2400000,
    stock: 31,
    status: 'Đang bán'
  }
])

const formatCurrency = (val) => {
  return new Intl.NumberFormat('vi-VN').format(val) + ' đ'
}
</script>

<template>
  <div class="product-list-page">
    <div class="page-header-row">
      <div class="page-title-box">
        <h1 class="page-title">Sản phẩm</h1>
        <p class="page-subtitle">Quản lý thông tin, giá bán, biến thể và tồn kho sản phẩm.</p>
      </div>

      <div class="page-actions-box">
        <button class="btn btn-export">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          Xuất Excel
        </button>
        <button class="btn btn-add-product" @click="navigateToAdd">
          <span class="plus-icon">+</span>
          Thêm sản phẩm
        </button>
      </div>
    </div>

    <div class="stats-grid">
      <div v-for="(stat, index) in stats" :key="index" class="stat-card">
        <div class="stat-icon-wrap" :style="{ backgroundColor: stat.iconColor }">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" :stroke="stat.iconStroke" stroke-width="2">
            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-header">
            <span class="stat-title">{{ stat.title }}</span>
            <span class="stat-badge" :class="stat.badgeType">{{ stat.badgeText }}</span>
          </div>
          <div class="stat-value">{{ stat.value }}</div>
        </div>
      </div>
    </div>

    <div class="filter-card">
      <div class="filter-header">
        <div class="filter-header-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#d92d20" stroke-width="2">
            <line x1="4" y1="21" x2="4" y2="14"></line>
            <line x1="4" y1="10" x2="4" y2="3"></line>
            <line x1="12" y1="21" x2="12" y2="12"></line>
            <line x1="12" y1="8" x2="12" y2="3"></line>
            <line x1="20" y1="21" x2="20" y2="16"></line>
            <line x1="20" y1="12" x2="20" y2="3"></line>
            <line x1="1" y1="14" x2="7" y2="14"></line>
            <line x1="9" y1="8" x2="15" y2="8"></line>
            <line x1="17" y1="16" x2="23" y2="16"></line>
          </svg>
          <span>Bộ lọc tìm kiếm</span>
        </div>
        <div class="filter-total-count">Hiển thị 248 sản phẩm</div>
      </div>

      <div class="filter-fields-row">
        <!-- Input Search -->
        <div class="filter-group flex-2">
          <label class="filter-label">Tìm kiếm</label>
          <div class="search-input-wrap">
            <svg class="search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            <input type="text" class="filter-input" placeholder="Tên hoặc mã sản phẩm..." />
          </div>
        </div>

        <div class="filter-group flex-1">
          <label class="filter-label">Danh mục</label>
          <select class="filter-select">
            <option>Tất cả danh mục</option>
            <option>Giày thể thao</option>
            <option>Sneaker</option>
            <option>Giày chạy bộ</option>
          </select>
        </div>

        <div class="filter-group flex-1">
          <label class="filter-label">Trạng thái</label>
          <select class="filter-select">
            <option>Tất cả trạng thái</option>
            <option>Đang bán</option>
            <option>Sắp hết</option>
            <option>Ngừng bán</option>
          </select>
        </div>

        <div class="filter-group flex-1">
          <label class="filter-label">Khoảng giá</label>
          <select class="filter-select">
            <option>Tất cả mức giá</option>
            <option>&lt; 1.000.000 đ</option>
            <option>1.000.000 - 3.000.000 đ</option>
            <option>&gt; 3.000.000 đ</option>
          </select>
        </div>

        <div class="filter-actions-group">
          <button class="btn btn-reset">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 4 23 10 17 10"></polyline>
              <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"></path>
            </svg>
            Đặt lại
          </button>
          <button class="btn btn-search">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
            Tìm kiếm
          </button>
        </div>
      </div>
    </div>

    <div class="table-card">
      <div class="table-header-row">
        <div class="table-title-box">
          <h2 class="table-title">Danh sách sản phẩm</h2>
          <span class="count-pill">248 sản phẩm</span>
        </div>
        <div class="table-sort-box">
          <span class="sort-text">Cập nhật mới nhất</span>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="7 10 12 15 17 10"></polyline>
          </svg>
        </div>
      </div>

      <div class="table-wrapper">
        <table class="products-table">
          <thead>
            <tr>
              <th width="40"><input type="checkbox" /></th>
              <th width="70">Sản phẩm</th>
              <th width="130">Mã sản phẩm</th>
              <th>Tên sản phẩm</th>
              <th>Thương hiệu</th>
              <th>Xuất xứ</th>
              <th>Chất liệu</th>
              <th>Kiểu dáng</th>
              <th>Loại giày</th>
              <th class="text-right">Giá bán</th>
              <th class="text-center">Tồn kho</th>
              <th>Trạng thái</th>
              <th class="text-center" width="90">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in products" :key="item.id">
              <td><input type="checkbox" /></td>
              <td>
                <div class="shoe-thumb">
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="1.8">
                    <path d="M3 14c2-4 5-6 10-6h6a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H4a1 1 0 0 1-1-1v-2z"></path>
                  </svg>
                </div>
              </td>
              <td><span class="product-code">{{ item.code }}</span></td>
              <td>
                <div class="product-name-col">
                  <span class="name-main">{{ item.name }}</span>
                  <span class="name-sub">{{ item.subtitle }}</span>
                </div>
              </td>
              <td>{{ item.brand }}</td>
              <td>{{ item.origin }}</td>
              <td>{{ item.material }}</td>
              <td>{{ item.style }}</td>
              <td>{{ item.category }}</td>
              <td class="text-right"><span class="price-val">{{ formatCurrency(item.price) }}</span></td>
              <td class="text-center font-bold">{{ item.stock }}</td>
              <td>
                <span class="badge-pill" :class="{
                  'badge-green': item.status === 'Đang bán',
                  'badge-yellow': item.status === 'Sắp hết',
                  'badge-gray': item.status === 'Ngừng bán'
                }">
                  <span class="badge-dot"></span>
                  {{ item.status }}
                </span>
              </td>
              <td class="text-center">
                <div class="action-buttons">
                  <button class="btn-icon-edit" title="Chỉnh sửa">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M12 20h9"></path>
                      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                    </svg>
                  </button>
                  <button class="btn-icon-more" title="Tùy chọn khác">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="1"></circle>
                      <circle cx="19" cy="12" r="1"></circle>
                      <circle cx="5" cy="12" r="1"></circle>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-pagination-row">
        <span class="pagination-info">Hiển thị 1-6 trong 248 sản phẩm</span>
        <div class="pagination-controls">
          <button class="page-btn nav-btn">&lt;</button>
          <button class="page-btn active">1</button>
          <button class="page-btn">2</button>
          <button class="page-btn">3</button>
          <button class="page-btn">4</button>
          <button class="page-btn nav-btn">&gt;</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.product-list-page {
  width: 100%;
}

.page-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 13.5px;
  color: #64748b;
  margin-top: 4px;
}

.page-actions-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-export {
  background: #ffffff;
  border: 1px solid #fee2e2;
  color: #d92d20;
  font-size: 13.5px;
  font-weight: 600;
  padding: 9px 18px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-export:hover {
  background-color: #fff1f2;
}

.btn-add-product {
  background-color: var(--primary);
  color: #ffffff;
  border: none;
  font-size: 13.5px;
  font-weight: 700;
  padding: 10px 20px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(217, 45, 32, 0.25);
  transition: all 0.15s;
}

.btn-add-product:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.stat-title {
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
}

.stat-badge {
  font-size: 11px;
  font-weight: 700;
}

.badge-accent {
  color: #d92d20;
}

.badge-success {
  color: #059669;
}

.badge-warning {
  color: #d97706;
}

.badge-secondary {
  color: #64748b;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.filter-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.filter-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.filter-total-count {
  font-size: 12.5px;
  color: #64748b;
  font-weight: 500;
}

.filter-fields-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.flex-2 {
  flex: 2;
}

.flex-1 {
  flex: 1;
}

.filter-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #475569;
}

.search-input-wrap {
  position: relative;
}

.search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #94a3b8;
}

.filter-input {
  width: 100%;
  height: 38px;
  padding: 0 12px 0 36px;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  outline: none;
}

.filter-input:focus {
  border-color: var(--primary);
}

.filter-select {
  height: 38px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  outline: none;
  background: #ffffff;
  cursor: pointer;
}

.filter-actions-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-reset {
  height: 38px;
  padding: 0 14px;
  background: #ffffff;
  border: 1px solid #fee2e2;
  color: var(--primary);
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-search {
  height: 38px;
  padding: 0 18px;
  background: var(--primary);
  color: #ffffff;
  border: none;
  border-radius: var(--radius-md);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.table-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.table-header-row {
  padding: 18px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
}

.table-title-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.count-pill {
  padding: 3px 10px;
  background: #fff1f2;
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  border-radius: 20px;
  border: 1px solid #fee2e2;
}

.table-sort-box {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
  cursor: pointer;
}

.table-wrapper {
  overflow-x: auto;
}

.products-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13.5px;
}

.products-table th {
  padding: 12px 16px;
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.products-table td {
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.shoe-thumb {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12.5px;
  font-weight: 600;
  color: #1e293b;
}

.product-name-col {
  display: flex;
  flex-direction: column;
}

.name-main {
  font-weight: 700;
  color: #0f172a;
}

.name-sub {
  font-size: 12px;
  color: #64748b;
}

.price-val {
  font-weight: 700;
  color: var(--primary);
}

.badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.badge-green {
  background: #ecfdf5;
  color: #059669;
}

.badge-green .badge-dot {
  background: #10b981;
}

.badge-yellow {
  background: #fffbeb;
  color: #d97706;
}

.badge-yellow .badge-dot {
  background: #f59e0b;
}

.badge-gray {
  background: #f1f5f9;
  color: #64748b;
}

.badge-gray .badge-dot {
  background: #94a3b8;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-icon-edit,
.btn-icon-more {
  background: none;
  border: 1px solid #fee2e2;
  border-radius: 6px;
  padding: 5px;
  color: var(--primary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon-more {
  border-color: #e2e8f0;
  color: #64748b;
}

.table-pagination-row {
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #f1f5f9;
}

.pagination-info {
  font-size: 13px;
  color: #64748b;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-btn {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.page-btn.active {
  background: var(--primary);
  color: #ffffff;
  border-color: var(--primary);
}
</style>
