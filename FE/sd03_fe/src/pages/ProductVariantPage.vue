<script setup>
import { ref } from 'vue'

const activeImage = ref(0)

const sizeChips = ['38', '39', '40', '41', '42', '43']
const colorChips = ['Đen', 'Trắng', 'Đỏ']
const materialChips = ['Da', 'Vải lưới', 'Cao su']

const variants = [
  { code: 'SP001-38-EN', size: '38', color: 'Đen', price: '1.250.000₫', stock: 12, status: 'Đang bán' },
  { code: 'SP001-39-EN', size: '39', color: 'Đen', price: '1.250.000₫', stock: 8, status: 'Đang bán' },
  { code: 'SP001-40-TR', size: '40', color: 'Trắng', price: '1.250.000₫', stock: 15, status: 'Đang bán' },
  { code: 'SP001-41-DO', size: '41', color: 'Đỏ', price: '1.300.000₫', stock: 4, status: 'Sắp hết' },
  { code: 'SP001-42-EN', size: '42', color: 'Đen', price: '1.250.000₫', stock: 6, status: 'Đang bán' },
  { code: 'SP001-43-TR', size: '43', color: 'Trắng', price: '1.250.000₫', stock: 0, status: 'Hết hàng' },
]

const statusPill = (value) => {
  if (value === 'Đang bán') return 'pill-green'
  if (value === 'Sắp hết') return 'pill-amber'
  return 'pill-red'
}
</script>

<template>
  <div class="screen">
    <div class="screen-head">
      <div>
        <h1 class="screen-title">Biến thể sản phẩm</h1>
        <p class="screen-sub">Quản lý kích thước, màu sắc, giá và tồn kho theo từng biến thể sản phẩm</p>
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
          Thêm biến thể
        </button>
      </div>
    </div>

    <div class="variant-grid">
      <div class="col-left">
        <section class="panel">
          <div class="panel-head">
            <span class="panel-icon is-red">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
                />
              </svg>
            </span>
            <h3 class="panel-title">Thông tin sản phẩm</h3>
            <div class="panel-right">
              <span class="panel-meta">SP001</span>
            </div>
          </div>

          <div class="f-item f-full">
            <label>Chọn sản phẩm</label>
            <select>
              <option value="SP001">SP001 — Giày Thể Thao Retro 90s</option>
              <option value="SP002">SP002 — Giày Sneaker Bitis Hunter X</option>
              <option value="SP003">SP003 — Giày Da Lãnh Đạo Classic</option>
            </select>
          </div>

          <div class="v-grid">
            <div class="f-item">
              <label>Mã sản phẩm</label>
              <input type="text" value="SP001" readonly />
            </div>
            <div class="f-item">
              <label>Tên sản phẩm</label>
              <input type="text" value="Giày Thể Thao Retro 90s" readonly />
            </div>
            <div class="f-item">
              <label>Danh mục</label>
              <input type="text" value="Giày thể thao" readonly />
            </div>
            <div class="f-item">
              <label>Thương hiệu</label>
              <input type="text" value="Nike" readonly />
            </div>
            <div class="f-item">
              <label>Giá bán</label>
              <input type="text" value="1.250.000₫" readonly />
            </div>
            <div class="f-item">
              <label>Trạng thái</label>
              <input type="text" value="Đang bán" readonly />
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <span class="panel-icon is-red">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M3 4.6h18l-7.1 8.5v5.9l-3.8 2v-7.9L3 4.6z" />
              </svg>
            </span>
            <h3 class="panel-title">Thuộc tính biến thể</h3>
            <span class="count-pill">6 biến thể</span>
            <div class="panel-right">
              <span class="panel-meta">Cập nhật lúc 08:32 25/05/2026</span>
            </div>
          </div>

          <div class="attr-list">
            <div class="attr-row">
              <span class="attr-label">Kích thước</span>
              <span v-for="chip in sizeChips" :key="chip" class="chip">{{ chip }}</span>
              <button class="switch is-on" type="button" aria-label="Bật kích thước" />
            </div>
            <div class="attr-row">
              <span class="attr-label">Màu sắc</span>
              <span v-for="chip in colorChips" :key="chip" class="chip">{{ chip }}</span>
              <button class="switch is-on" type="button" aria-label="Bật màu sắc" />
            </div>
            <div class="attr-row">
              <span class="attr-label">Chất liệu</span>
              <span v-for="chip in materialChips" :key="chip" class="chip">{{ chip }}</span>
              <button class="switch" type="button" aria-label="Bật chất liệu" />
            </div>
          </div>

          <div class="table-wrap sub-table">
            <table class="data-table">
              <thead>
                <tr>
                  <th>Mã biến thể</th>
                  <th>Kích thước</th>
                  <th>Màu sắc</th>
                  <th>Giá bán</th>
                  <th>Tồn kho</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in variants" :key="row.code">
                  <td><span class="cell-strong">{{ row.code }}</span></td>
                  <td>{{ row.size }}</td>
                  <td>{{ row.color }}</td>
                  <td>{{ row.price }}</td>
                  <td>{{ row.stock }}</td>
                  <td><span class="pill" :class="statusPill(row.status)">{{ row.status }}</span></td>
                  <td>
                    <div class="act-group">
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
        </section>
      </div>

      <div class="col-right">
        <section class="panel">
          <div class="panel-head">
            <span class="panel-icon is-red">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z"
                />
              </svg>
            </span>
            <h3 class="panel-title">Ảnh sản phẩm</h3>
            <div class="panel-right">
              <span class="panel-meta">4 ảnh</span>
            </div>
          </div>

          <div class="dropzone">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 16V4M7 9l5-5 5 5" />
              <path d="M4 20h16" />
            </svg>
            <p class="dz-text">Kéo &amp; thả ảnh vào đây</p>
            <p class="dz-hint">PNG, JPG tối đa 5MB</p>
            <button class="dz-btn" type="button">Chọn ảnh</button>
          </div>

          <div class="thumb-grid">
            <button
              v-for="i in 4"
              :key="i"
              class="thumb"
              :class="{ 'is-active': activeImage === i - 1, 'is-empty': i > 2 }"
              type="button"
              @click="activeImage = i - 1"
            >
              <template v-if="i <= 2">
                <span class="thumb-mark" :style="{ background: i === 1 ? '#e0202a' : '#23232a' }">G</span>
              </template>
              <template v-else>+</template>
            </button>
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
            <h3 class="panel-title">Tồn kho biến thể</h3>
          </div>

          <div class="inv-grid">
            <div class="inv-card">
              <span class="inv-label">Tổng tồn kho</span>
              <strong class="inv-value">95</strong>
            </div>
            <div class="inv-card">
              <span class="inv-label">Đang bán</span>
              <strong class="inv-value is-green">05</strong>
            </div>
            <div class="inv-card">
              <span class="inv-label">Hết hàng</span>
              <strong class="inv-value is-red">01</strong>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <span class="panel-icon is-red">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path
                  d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"
                />
              </svg>
            </span>
            <h3 class="panel-title">Thông tin biến thể</h3>
          </div>

          <div class="v-grid">
            <div class="f-item">
              <label>Mã biến thể</label>
              <input type="text" value="SP001-40-TR" readonly />
            </div>
            <div class="f-item">
              <label>Kích thước</label>
              <select>
                <option value="38">38</option>
                <option value="39">39</option>
                <option value="40" selected>40</option>
                <option value="41">41</option>
                <option value="42">42</option>
                <option value="43">43</option>
              </select>
            </div>
            <div class="f-item">
              <label>Màu sắc</label>
              <select>
                <option value="en">Đen</option>
                <option value="tr" selected>Trắng</option>
                <option value="do">Đỏ</option>
              </select>
            </div>
            <div class="f-item">
              <label>Tồn kho</label>
              <input type="text" value="15" />
            </div>
            <div class="f-item f-full">
              <label>Giá bán</label>
              <input type="text" value="1.250.000₫" />
            </div>
          </div>

          <div class="toggle-row">
            <span class="toggle-label">Hiển thị biến thể trên cửa hàng</span>
            <button class="switch is-on" type="button" aria-label="Hiển thị trên cửa hàng" />
          </div>

          <div class="alert alert-green">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path
                d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"
              />
            </svg>
            Ảnh đại diện đã sẵn sàng hiển thị trên trang sản phẩm.
          </div>

          <div class="alert alert-amber">
            <svg viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z" />
            </svg>
            Còn 1 biến thể hết hàng, vui lòng bổ sung tồn kho.
          </div>
        </section>
      </div>
    </div>
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
  background: var(--green);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: filter 0.15s ease;
}

.btn-export:hover {
  filter: brightness(1.06);
}

.btn-export svg {
  width: 15px;
  height: 15px;
}

.variant-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.75fr);
  gap: 16px;
  align-items: start;
}

.col-left,
.col-right {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.f-full {
  margin-bottom: 12px;
}

.v-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 12px;
}

.attr-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding-bottom: 14px;
  margin-bottom: 4px;
  border-bottom: 1px dashed #ececef;
}

.attr-row {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}

.attr-label {
  width: 78px;
  flex-shrink: 0;
  font-size: 11.5px;
  font-weight: 600;
  color: #7a7a83;
}

.chip {
  font-size: 11.5px;
  font-weight: 600;
  color: #4a4a52;
  background: #f4f4f7;
  border: 1px solid #e7e7ec;
  border-radius: 999px;
  padding: 3px 10px;
}

.switch {
  margin-left: auto;
  width: 36px;
  height: 20px;
  border: none;
  border-radius: 999px;
  background: #d5d5dc;
  position: relative;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s ease;
}

.switch::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
  transition: left 0.15s ease;
}

.switch.is-on {
  background: var(--red);
}

.switch.is-on::after {
  left: 18px;
}

.sub-table {
  margin-top: 10px;
}

.dropzone {
  border: 1.6px dashed #f0b7b7;
  background: #fffafa;
  border-radius: 10px;
  padding: 18px 12px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.dropzone svg {
  width: 26px;
  height: 26px;
  color: var(--red);
}

.dz-text {
  margin: 4px 0 0;
  font-size: 12.5px;
  font-weight: 600;
  color: #3a3a42;
}

.dz-hint {
  margin: 0;
  font-size: 11px;
  color: #9a9aa3;
}

.dz-btn {
  margin-top: 8px;
  height: 30px;
  padding: 0 14px;
  border-radius: 7px;
  border: 1px solid var(--red);
  background: #fff;
  color: var(--red);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.dz-btn:hover {
  background: var(--red-soft, #fdecec);
}

.thumb-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.thumb {
  aspect-ratio: 1;
  border-radius: 9px;
  border: 1.5px solid #e7e7ec;
  background: #fafafc;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #b9b9c2;
  padding: 0;
}

.thumb.is-active {
  border-color: var(--red);
}

.thumb.is-empty {
  border-style: dashed;
  background: #fff;
}

.thumb-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.inv-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.inv-card {
  background: #fafafc;
  border: 1px solid #eeeef2;
  border-radius: 9px;
  padding: 10px 8px;
  text-align: center;
}

.inv-label {
  display: block;
  font-size: 11px;
  color: #8a8a93;
}

.inv-value {
  display: block;
  margin-top: 3px;
  font-size: 20px;
  font-weight: 800;
  color: #23232a;
}

.inv-value.is-green {
  color: var(--green);
}

.inv-value.is-red {
  color: var(--red);
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding: 9px 12px;
  background: #fafafc;
  border: 1px solid #eeeef2;
  border-radius: 8px;
}

.toggle-row .switch {
  margin-left: 0;
}

.toggle-label {
  font-size: 12.5px;
  font-weight: 600;
  color: #4a4a52;
}

.alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.45;
}

.alert svg {
  width: 15px;
  height: 15px;
  flex-shrink: 0;
  margin-top: 1px;
}

.alert-green {
  background: #ecfdf3;
  border: 1px solid #b7ebc9;
  color: #14713d;
}

.alert-amber {
  background: #fffbeb;
  border: 1px solid #fde3a3;
  color: #92600b;
}
</style>
