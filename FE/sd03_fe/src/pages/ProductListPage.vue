<script setup>
import { ref } from 'vue'
import StatCard from '../components/StatCard.vue'

const showFilter = ref(true)

const pages = [1, 2, 3, 4, 5]

const iconBox = [
  'M20 2H4c-1 0-2 .9-2 2v3.01c0 .72.43 1.34 1 1.69V20c0 1.1 1.1 2 2 2h14c.9 0 2-.9 2-2V8.7c.57-.35 1-.97 1-1.69V4c0-1.1-1-2-2-2zm-5 12H9v-2h6v2zm5-7H4V4h16v3z',
]
const iconCheck = [
  'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z',
]
const iconAlert = [
  'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z',
]
const iconOff = [
  'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z',
]

const products = [
  {
    name: 'Giày Thể Thao Retro 90s',
    thumb: 'G',
    color: '#cc0000',
    code: 'SP001',
    category: 'Giày thể thao',
    brand: 'Nike',
    price: '1.250.000₫',
    stock: 48,
    status: 'Đang bán',
  },
  {
    name: 'Giày Sneaker Bitis Hunter X',
    thumb: 'G',
    color: '#f59e0b',
    code: 'SP002',
    category: 'Giày thể thao',
    brand: 'Bitis',
    price: '890.000₫',
    stock: 5,
    status: 'Sắp hết',
  },
  {
    name: 'Giày Da Lãnh Đạo Classic',
    thumb: 'G',
    color: '#8b5cf6',
    code: 'SP003',
    category: 'Giày da',
    brand: 'Ananas',
    price: '1.450.000₫',
    stock: 23,
    status: 'Đang bán',
  },
  {
    name: 'Dép Sandal Comfort',
    thumb: 'D',
    color: '#14b8a6',
    code: 'SP004',
    category: 'Dép',
    brand: 'Adidas',
    price: '320.000₫',
    stock: 0,
    status: 'Hết hàng',
  },
  {
    name: 'Giày Chạy Bộ Ultraboost',
    thumb: 'G',
    color: '#3b82f6',
    code: 'SP005',
    category: 'Giày thể thao',
    brand: 'Adidas',
    price: '2.190.000₫',
    stock: 17,
    status: 'Đang bán',
  },
  {
    name: 'Giày Cao Tổ Ong Classic',
    thumb: 'G',
    color: '#ec4899',
    code: 'SP006',
    category: 'Giày da',
    brand: 'Bitis',
    price: '750.000₫',
    stock: 12,
    status: 'Ngừng bán',
  },
]

const statusPill = (value) => {
  if (value === 'Đang bán') return 'pill-green'
  if (value === 'Sắp hết') return 'pill-amber'
  if (value === 'Hết hàng') return 'pill-red'
  return 'pill-gray'
}

const stockClass = (value) => (value === 0 ? 'stock-zero' : value <= 5 ? 'stock-low' : '')
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Quản lý sản phẩm</h1>
        <p class="screen-sub">Quản lý danh mục, giá bán và tồn kho sản phẩm tại Poly Shoe</p>
      </div>
      <div class="head-actions">
        <button class="btn-export" type="button">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 3v12M7 10l5 5 5-5M4 21h16" />
          </svg>
          Xuất File
        </button>
        <button class="btn-add" type="button">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
            <path d="M12 6v12M6 12h12" />
          </svg>
          Thêm sản phẩm
        </button>
      </div>
    </div>

    <div class="stat-row">
      <StatCard
        tone="red"
        :icon="iconBox"
        label="Tổng sản phẩm"
        value="248"
        note="+12 sản phẩm trong tháng"
        note-tone="red"
      />
      <StatCard
        tone="green"
        :icon="iconCheck"
        label="Đang bán"
        value="227"
        note="91% đang hoạt động"
        note-tone="green"
      />
      <StatCard
        tone="amber"
        :icon="iconAlert"
        label="Sắp hết hàng"
        value="19"
        note="Tồn kho ≤ 5 sản phẩm"
        note-tone="amber"
      />
      <StatCard
        tone="blue"
        :icon="iconOff"
        label="Ngừng bán"
        value="08"
        note="Không còn kinh doanh"
        note-tone="blue"
      />
    </div>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
          </svg>
        </span>
        <h3 class="panel-title">Tìm kiếm &amp; bộ lọc</h3>
        <div class="panel-right">
          <span class="result-count">248 kết quả</span>
          <button class="link-red" type="button" @click="showFilter = !showFilter">
            {{ showFilter ? 'Ẩn bớt' : 'Hiện thêm' }}
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path :d="showFilter ? 'M6 14.5l6-6 6 6' : 'M6 9.5l6 6 6-6'" />
            </svg>
          </button>
        </div>
      </div>

      <div v-show="showFilter" class="filter-row">
        <div class="f-item">
          <label>Tìm sản phẩm</label>
          <input type="text" placeholder="Nhập tên hoặc mã sản phẩm" />
        </div>

        <div class="f-item">
          <label>Danh mục</label>
          <select>
            <option value="">Tất cả</option>
            <option value="sneaker">Giày thể thao</option>
            <option value="leather">Giày da</option>
            <option value="sandal">Dép</option>
            <option value="accessory">Phụ kiện</option>
          </select>
        </div>

        <div class="f-item">
          <label>Thương hiệu</label>
          <select>
            <option value="">Tất cả</option>
            <option value="nike">Nike</option>
            <option value="adidas">Adidas</option>
            <option value="bitis">Bitis</option>
            <option value="ananas">Ananas</option>
          </select>
        </div>

        <div class="f-item">
          <label>Trạng thái</label>
          <select>
            <option value="">Tất cả</option>
            <option value="selling">Đang bán</option>
            <option value="low">Sắp hết</option>
            <option value="out">Hết hàng</option>
            <option value="stop">Ngừng bán</option>
          </select>
        </div>

        <div class="filter-actions">
          <button class="btn-reset" type="button" aria-label="Đặt lại">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20 11a8 8 0 1 0-.9 4.5" />
              <path d="M20 4.5V11h-6.5" />
            </svg>
          </button>
          <button class="btn-apply" type="button">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <path d="M4 6h16M7 12h10M10 18h4" />
            </svg>
            Áp dụng
          </button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-head">
        <span class="panel-icon is-red">
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path
              d="M20 2H4c-1 0-2 .9-2 2v3.01c0 .72.43 1.34 1 1.69V20c0 1.1 1.1 2 2 2h14c.9 0 2-.9 2-2V8.7c.57-.35 1-.97 1-1.69V4c0-1.1-1-2-2-2zm-5 12H9v-2h6v2zm5-7H4V4h16v3z"
            />
          </svg>
        </span>
        <h3 class="panel-title">Danh sách sản phẩm</h3>
        <span class="count-pill">248 sản phẩm</span>
        <div class="panel-right">
          <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Sản phẩm</th>
              <th>Danh mục</th>
              <th>Thương hiệu</th>
              <th>Giá bán</th>
              <th>Tồn kho</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in products" :key="row.code">
              <td>
                <div class="entity">
                  <span class="prod-thumb" :style="{ background: row.color }">{{ row.thumb }}</span>
                  <div class="entity-info">
                    <div class="entity-line">
                      <span class="entity-name">{{ row.name }}</span>
                      <span class="entity-code">{{ row.code }}</span>
                    </div>
                    <div class="entity-sub">Cập nhật 20/05/2026</div>
                  </div>
                </div>
              </td>
              <td>{{ row.category }}</td>
              <td>{{ row.brand }}</td>
              <td><span class="cell-strong">{{ row.price }}</span></td>
              <td>
                <span class="stock-num" :class="stockClass(row.stock)">{{ row.stock }}</span>
              </td>
              <td><span class="pill" :class="statusPill(row.status)">{{ row.status }}</span></td>
              <td>
                <div class="act-group">
                  <button class="act-btn" type="button" aria-label="Xem">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  </button>
                  <button class="act-btn is-red" type="button" aria-label="Sửa">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3zM14.5 6.5l3 3" />
                    </svg>
                  </button>
                  <button class="act-btn" type="button" aria-label="Xóa">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M4 7h16M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M6 7l1 13h10l1-13" />
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="panel-foot">
        <span class="foot-text">Hiển thị 1-6 trong 248 sản phẩm</span>
        <div class="pager">
          <button class="page-btn" type="button" aria-label="Trang trước">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14.5 6l-6 6 6 6" />
            </svg>
          </button>
          <button
            v-for="page in pages"
            :key="page"
            class="page-btn"
            :class="{ 'is-active': page === 1 }"
            type="button"
          >
            {{ page }}
          </button>
          <button class="page-btn" type="button" aria-label="Trang sau">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9.5 6l6 6-6 6" />
            </svg>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-export {
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 8px;
  background: var(--red);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.btn-export:hover {
  background: var(--red-dark);
}

.btn-export svg {
  width: 15px;
  height: 15px;
}

.prod-thumb {
  width: 40px;
  height: 40px;
  border-radius: 9px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stock-num {
  font-weight: 700;
  color: #23232a;
}

.stock-num.stock-low {
  color: var(--amber, #f59e0b);
}

.stock-num.stock-zero {
  color: var(--red);
}
</style>
