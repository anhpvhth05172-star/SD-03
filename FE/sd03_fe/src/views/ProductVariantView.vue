<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const form = reactive({
  productName: 'Giày thể thao Urban Run 2',
  skuCode: 'SP-UR-2024',
  price: '2.490.000',
  stock: '120',
  note: 'Áp dụng giá chung cho các biến thể chưa có giá riêng. Tồn kho sẽ được điều chỉnh theo từng SKU.',
  hasMultipleVariants: true,
  isSelling: true
})

const variants = ref([
  { id: 1, name: 'Đen / 39', sku: 'SP-UR-2024-BK-39', price: '2.490.000', stock: 10 },
  { id: 2, name: 'Đen / 40', sku: 'SP-UR-2024-BK-40', price: '2.490.000', stock: 30 },
  { id: 3, name: 'Đen / 41', sku: 'SP-UR-2024-BK-41', price: '2.490.000', stock: 26 },
  { id: 4, name: 'Đen / 42', sku: 'SP-UR-2024-BK-42', price: '2.490.000', stock: 14 },
  { id: 5, name: 'Trắng / 39', sku: 'SP-UR-2024-WH-39', price: '2.490.000', stock: 12 },
  { id: 6, name: 'Trắng / 40', sku: 'SP-UR-2024-WH-40', price: '2.490.000', stock: 28 },
  { id: 7, name: 'Trắng / 41', sku: 'SP-UR-2024-WH-41', price: '2.490.000', stock: 22 },
  { id: 8, name: 'Trắng / 42', sku: 'SP-UR-2024-WH-42', price: '2.490.000', stock: 10 },
  { id: 9, name: 'Xanh Navy / 39', sku: 'SP-UR-2024-NV-39', price: '2.490.000', stock: 16 },
  { id: 10, name: 'Xanh Navy / 40', sku: 'SP-UR-2024-NV-40', price: '2.490.000', stock: 30 },
  { id: 11, name: 'Xanh Navy / 41', sku: 'SP-UR-2024-NV-41', price: '2.490.000', stock: 24 },
  { id: 12, name: 'Xanh Navy / 42', sku: 'SP-UR-2024-NV-42', price: '2.490.000', stock: 0 }
])

const currentPage = ref(1)
const pageSize = ref(5)

const totalPages = computed(() => Math.ceil(variants.value.length / pageSize.value))

const paginatedVariants = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return variants.value.slice(start, start + pageSize.value)
})

const getStatus = (stock) => {
  if (stock === 0) return { label: 'Ngừng bán', class: 'badge-gray' }
  if (stock <= 15) return { label: 'Sắp hết', class: 'badge-yellow' }
  return { label: 'Đang bán', class: 'badge-green' }
}

const changePage = (p) => {
  if (p >= 1 && p <= totalPages.value) {
    currentPage.value = p
  }
}

const cancelAction = () => {
  router.push('/san-pham')
}
</script>

<template>
  <div class="variant-page">
    <div class="page-header-row">
      <div class="page-title-box">
        <h1 class="page-title">Biến thể sản phẩm</h1>
      </div>

      <div class="page-actions-box">
        <button class="btn btn-outline-cancel" @click="cancelAction">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="18" y1="6" x2="6" y2="18"></line>
            <line x1="6" y1="6" x2="18" y2="18"></line>
          </svg>
          Hủy
        </button>
        <button class="btn btn-outline-draft">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
            <polyline points="17 21 17 13 7 13 7 21"></polyline>
            <polyline points="7 3 7 8 15 8"></polyline>
          </svg>
          Lưu nháp
        </button>
        <button class="btn btn-save-primary">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"></path>
            <polyline points="17 21 17 13 7 13 7 21"></polyline>
            <polyline points="7 3 7 8 15 8"></polyline>
          </svg>
          Lưu thay đổi
        </button>
      </div>
    </div>

    <div class="variant-grid-layout">
      <div class="grid-left-col">
        <div class="card-box">
          <div class="card-header">
            <div class="card-header-title">
              <span class="header-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path
                    d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
                </svg>
              </span>
              <h2>Thông tin chung</h2>
            </div>
          </div>

          <div class="form-grid-2col">
            <div class="form-group">
              <label class="field-label">Sản phẩm gốc <span class="required-star">*</span></label>
              <input type="text" class="field-input" v-model="form.productName" />
            </div>

            <div class="form-group">
              <label class="field-label">Mã SKU gốc <span class="required-star">*</span></label>
              <input type="text" class="field-input" v-model="form.skuCode" />
            </div>

            <div class="form-group">
              <label class="field-label">Giá bán chung <span class="required-star">*</span></label>
              <div class="input-with-icon">
                <input type="text" class="field-input" v-model="form.price" />
                <span class="input-right-icon">đ</span>
              </div>
            </div>

            <div class="form-group">
              <label class="field-label">Tồn kho ban đầu <span class="required-star">*</span></label>
              <div class="input-with-icon">
                <input type="text" class="field-input" v-model="form.stock" />
                <span class="input-right-icon">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2">
                    <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                    <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                  </svg>
                </span>
              </div>
            </div>
          </div>

          <div class="form-group margin-top-md">
            <label class="field-label">Ghi chú quản lý</label>
            <textarea class="field-textarea" rows="2" v-model="form.note"></textarea>
          </div>
        </div>

        <div class="card-box margin-top-lg">
          <div class="card-header border-bottom-none">
            <div class="card-header-title">
              <span class="header-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path
                    d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-2 10h-4v4h-2v-4H7v-2h4V7h2v4h4v2z" />
                </svg>
              </span>
              <h2>Thuộc tính biến thể</h2>
            </div>
            <button class="btn-text-action">+ Thêm thuộc tính</button>
          </div>

          <div class="toggle-switch-row">
            <span class="toggle-label">Sản phẩm có nhiều biến thể</span>
            <label class="switch">
              <input type="checkbox" v-model="form.hasMultipleVariants" />
              <span class="slider round"></span>
            </label>
          </div>

          <div class="attributes-summary-box">
            <div class="attr-select-group">
              <select class="attr-select">
                <option>Màu sắc: Đen, Trắng, Xanh Navy</option>
              </select>
            </div>
            <div class="attr-select-group">
              <select class="attr-select">
                <option>Kích cỡ: 39, 40, 41, 42</option>
              </select>
            </div>
          </div>
        </div>

        <div class="card-box margin-top-lg">
          <div class="card-header border-bottom-none">
            <div class="card-header-title">
              <span class="header-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z" />
                </svg>
              </span>
              <h2>Danh sách biến thể</h2>
              <span class="count-pill">{{ variants.length }} biến thể</span>
            </div>
            <button class="btn-text-action">+ Thêm biến thể</button>
          </div>

          <div class="table-container">
            <table class="variants-table">
              <thead>
                <tr>
                  <th width="130">BIẾN THỂ</th>
                  <th>SKU</th>
                  <th width="110">GIÁ BÁN</th>
                  <th width="80" class="text-center">TỒN KHO</th>
                  <th width="120">TRẠNG THÁI</th>
                  <th width="90" class="text-center">HÀNH ĐỘNG</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="v in paginatedVariants" :key="v.id">
                  <td class="font-semibold text-main">{{ v.name }}</td>
                  <td class="sku-code">{{ v.sku }}</td>
                  <td class="price-col">{{ v.price }}</td>
                  <td class="text-center stock-col"
                    :class="{ 'text-warning-stock': v.stock <= 15 && v.stock > 0, 'text-danger-stock': v.stock === 0 }">
                    {{ v.stock }}</td>
                  <td>
                    <span class="badge-pill" :class="getStatus(v.stock).class">
                      <span class="badge-dot"></span>
                      {{ getStatus(v.stock).label }}
                    </span>
                  </td>
                  <td class="text-center">
                    <div class="action-btn-group">
                      <button class="btn-icon-sm edit" title="Chỉnh sửa">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                          stroke-width="2">
                          <path d="M12 20h9"></path>
                          <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path>
                        </svg>
                      </button>
                      <button class="btn-icon-sm delete" title="Xóa">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                          stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2">
                          </path>
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="table-pagination-row">
            <span class="pagination-info">Hiển thị {{ (currentPage - 1) * pageSize + 1 }}-{{ Math.min(currentPage *
              pageSize, variants.length) }} trong {{ variants.length }} biến thể</span>
            <div class="pagination-controls">
              <button class="page-btn nav-btn" :disabled="currentPage === 1"
                @click="changePage(currentPage - 1)">&lt;</button>
              <button v-for="p in totalPages" :key="p" class="page-btn" :class="{ active: p === currentPage }"
                @click="changePage(p)">
                {{ p }}
              </button>
              <button class="page-btn nav-btn" :disabled="currentPage === totalPages"
                @click="changePage(currentPage + 1)">&gt;</button>
            </div>
          </div>
        </div>
      </div>

      <div class="grid-right-col">
        <div class="card-box">
          <div class="card-header flex-column-start">
            <div class="card-header-title">
              <span class="header-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <circle cx="12" cy="12" r="3.2" />
                  <path
                    d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z" />
                </svg>
              </span>
              <h2>Hình ảnh biến thể</h2>
            </div>
            <span class="subtext-hint">Tối đa 5 ảnh, mỗi ảnh không quá 5 MB</span>
          </div>

          <div class="upload-dropzone">
            <div class="cloud-icon-circle">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#d92d20" stroke-width="2">
                <path
                  d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM12 11v6m0-6l-3 3m3-3l3 3" />
              </svg>
            </div>
            <p class="drop-main-text">Kéo thả hình ảnh vào đây</p>
            <p class="drop-sub-text">hoặc chọn tệp từ thiết bị của bạn</p>
            <button class="btn btn-select-file">Chọn hình ảnh</button>
          </div>

          <div class="image-slots-grid">
            <div class="image-slot active-slot">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#d92d20" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              <span class="slot-label text-red">Ảnh chính</span>
            </div>
            <div class="image-slot">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              <span class="slot-label">Ảnh phụ</span>
            </div>
            <div class="image-slot">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              <span class="slot-label">Ảnh phụ</span>
            </div>
            <div class="image-slot">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              <span class="slot-label">Ảnh phụ</span>
            </div>
          </div>
        </div>

        <div class="card-box margin-top-lg">
          <div class="card-header border-bottom-none">
            <div class="card-header-title">
              <span class="header-icon-circle">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                  <path
                    d="M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" />
                </svg>
              </span>
              <h2>Thiết lập đăng bán</h2>
            </div>
          </div>

          <div class="toggle-switch-row">
            <span class="toggle-label">Đang kinh doanh</span>
            <label class="switch">
              <input type="checkbox" v-model="form.isSelling" />
              <span class="slider round"></span>
            </label>
          </div>

          <div class="status-alert-box">
            <div class="status-left">
              <span class="status-dot-green"></span>
              <span class="status-text font-bold">Bản sống / Đăng bán</span>
            </div>
            <button class="btn-link-action">Sửa lẹ</button>
          </div>
        </div>

        <div class="card-box margin-top-lg">
          <div class="card-header border-bottom-none flex-column-start">
            <div class="card-header-title full-width justify-between">
              <div class="flex-align-center gap-8">
                <span class="header-icon-circle">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                    <path
                      d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 14l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
                  </svg>
                </span>
                <h2>Hoàn tất biến thể</h2>
              </div>
            </div>
          </div>

          <div class="progress-info-row">
            <span class="progress-title">Mức độ hoàn thiện</span>
            <span class="progress-percent">35%</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" style="width: 35%;"></div>
          </div>

          <div class="completion-checklist">
            <div class="checklist-item checked">
              <span class="check-icon-circle green">✓</span>
              <span class="check-label">Trạng thái đăng bán đã thiết lập</span>
            </div>
            <div class="checklist-item">
              <span class="check-icon-circle gray"></span>
              <span class="check-label">Thêm tên, SKU và phân loại sản phẩm</span>
            </div>
            <div class="checklist-item">
              <span class="check-icon-circle gray"></span>
              <span class="check-label">Thiết lập giá bán và tồn kho</span>
            </div>
            <div class="checklist-item">
              <span class="check-icon-circle gray"></span>
              <span class="check-label">Tải lên ít nhất 1 hình ảnh</span>
            </div>
          </div>

          <div class="yellow-hint-box">
            <span class="bulb-icon">💡</span>
            <p class="hint-text">Bạn có thể lưu nháp để hoàn thiện thông tin sau. Các trường có dấu * là bắt buộc.</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.variant-page {
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

.page-actions-box {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-outline-cancel,
.btn-outline-draft {
  background: #ffffff;
  border: 1px solid #fee2e2;
  color: #d92d20;
  font-size: 13.5px;
  font-weight: 600;
  padding: 9px 18px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-outline-cancel:hover,
.btn-outline-draft:hover {
  background-color: #fff1f2;
  border-color: #fca5a5;
}

.btn-save-primary {
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

.btn-save-primary:hover {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
}

/* 2-Column Grid Layout */
.variant-grid-layout {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 20px;
  align-items: start;
}

.card-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.03);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.border-bottom-none {
  border-bottom: none;
  padding-bottom: 0;
}

.flex-column-start {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-header-title h2 {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.header-icon-circle {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background-color: #fee2e2;
  color: #d92d20;
  display: flex;
  align-items: center;
  justify-content: center;
}

.subtext-hint {
  font-size: 12px;
  color: #64748b;

}

.btn-text-action {
  background: none;
  border: none;
  color: #d92d20;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.btn-text-action:hover {
  text-decoration: underline !important;
}

/* Form Styling */
.form-grid-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 20px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.required-star {
  color: #d92d20;
}

.field-input {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  color: #0f172a;
  outline: none;
  width: 100%;
}

.field-input:focus {
  border-color: #d92d20;
}

.input-with-icon {
  position: relative;
  display: flex;
  align-items: center;
}

.input-right-icon {
  position: absolute;
  right: 14px;
  font-weight: 600;
  color: #64748b;
  font-size: 13.5px;
  display: flex;
  align-items: center;
}

.field-textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: var(--radius-md);
  font-size: 13.5px;
  font-family: inherit;
  color: #334155;
  outline: none;
  resize: vertical;
}

.field-textarea:focus {
  border-color: #d92d20;
}

.margin-top-md {
  margin-top: 16px;
}

.margin-top-lg {
  margin-top: 20px;
}

/* Toggle Switch Row */
.toggle-switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
}

.toggle-label {
  font-size: 13.5px;
  font-weight: 600;
  color: #1e293b;
}

/* Switch UI */
.switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #cbd5e1;
  transition: .2s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: .2s;
}

input:checked+.slider {
  background-color: #d92d20;
}

input:checked+.slider:before {
  transform: translateX(20px);
}

.slider.round {
  border-radius: 34px;
}

.slider.round:before {
  border-radius: 50%;
}

/* Attributes Summary Box */
.attributes-summary-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}

.attr-select {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #334155;
  background: #ffffff;
}

/* Variants Table */
.table-container {
  margin-top: 14px;
  overflow-x: auto;
}

.variants-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.variants-table th {
  padding: 10px 14px;
  background: #f8fafc;
  color: #64748b;
  font-weight: 700;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.variants-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.sku-code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #475569;
}

.price-col {
  font-weight: 700;
  color: #1e293b;
}

.stock-col {
  font-weight: 700;
}

/* Dropzone Upload */
.upload-dropzone {
  background: #fafafa;
  border: 2px dashed #fca5a5;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  margin-top: 12px;
}

.cloud-icon-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fee2e2;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}

.drop-main-text {
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
}

.drop-sub-text {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
  margin-bottom: 14px;
}

.btn-select-file {
  background: #ffffff;
  border: 1px solid #fca5a5;
  color: #d92d20;
  font-weight: 600;
  font-size: 12.5px;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
}

.image-slots-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.image-slot {
  height: 64px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
}

.active-slot {
  border-color: #d92d20;
  background-color: #fff1f2;
}

.slot-label {
  font-size: 10.5px;
  color: #64748b;
  font-weight: 600;
}

.text-red {
  color: #d92d20;
}

/* Status Alert Box */
.status-alert-box {
  background-color: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.status-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot-green {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #10b981;
}

.status-text {
  font-size: 12.5px;
  color: #059669;
}

.btn-link-action {
  background: none;
  border: none;
  color: #059669;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  text-decoration: underline !important;
}

/* Completion Checklist */
.progress-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  font-size: 12.5px;
  font-weight: 600;
}

.progress-percent {
  color: #d92d20;
  font-weight: 800;
}

.progress-track {
  height: 6px;
  width: 100%;
  background: #e2e8f0;
  border-radius: 10px;
  margin-top: 6px;
  margin-bottom: 16px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #d92d20;
  border-radius: 10px;
}

.completion-checklist {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.checklist-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12.5px;
  color: #475569;
}

.check-icon-circle {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 800;
  flex-shrink: 0;
}

.check-icon-circle.green {
  background: #ecfdf5;
  color: #059669;
  border: 1px solid #a7f3d0;
}

.check-icon-circle.gray {
  border: 1.5px solid #cbd5e1;
}

.yellow-hint-box {
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 10px 12px;
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin-top: 18px;
}

.hint-text {
  font-size: 12px;
  color: #b45309;
  line-height: 1.4;
}

/* Status Badges & Stock Highlights */
.text-warning-stock {
  color: #d97706 !important;
  font-weight: 800 !important;
}

.text-danger-stock {
  color: #dc2626 !important;
  font-weight: 800 !important;
}

.badge-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 9px;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 700;
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

.action-btn-group {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-icon-sm {
  background: #ffffff;
  border: 1px solid #fee2e2;
  border-radius: 6px;
  padding: 5px;
  color: #d92d20;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.btn-icon-sm:hover {
  background: #fff1f2;
}

.btn-icon-sm.delete {
  border-color: #fee2e2;
  color: #d92d20;
}

/* Pagination Bar */
.table-pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.pagination-info {
  font-size: 12.5px;
  color: #64748b;
  font-weight: 500;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 6px;
}

.page-btn {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}

.page-btn:hover:not(:disabled) {
  border-color: #d92d20;
  color: #d92d20;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-btn.active {
  background: #d92d20;
  color: #ffffff;
  border-color: #d92d20;
}
</style>
