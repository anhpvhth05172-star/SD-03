<script setup>
defineProps({
  collapsed: { type: Boolean, default: false },
})

const menu = [
  {
    label: 'Thống kê',
    path: '/thong-ke',
    icon: 'M4 20V10M10 20V4M16 20v-6M3 20h18',
  },
  {
    label: 'Bán hàng tại quầy',
    path: '/ban-hang',
    icon: 'M3 9l1.6-5h14.8L21 9M4.5 9v10.5h15V9M4.5 9h15M9.5 19.5v-6h5v6',
  },
  {
    label: 'Quản lý hóa đơn',
    path: '/hoa-don',
    icon: 'M6 2.5h12v19l-3-1.8-3 1.8-3-1.8-3 1.8v-19zM9.5 7.5h5M9.5 11.5h5M9.5 15.5h3.5',
  },
  {
    label: 'Quản lý sản phẩm',
    path: '/san-pham',
    icon: 'M21 7.8L12 2.8 3 7.8l9 5 9-5zM3 7.8v8.4l9 5 9-5V7.8M12 12.8v8.4',
  },
  {
    label: 'Quản lý khách hàng',
    path: '/khach-hang',
    icon: 'M16 20.5v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 20.5v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75',
  },
  {
    label: 'Quản lý nhân viên',
    path: '/nhan-vien',
    icon: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8',
  },
  {
    label: 'Quản lý giảm giá',
    path: '/giam-gia',
    icon: 'M3 3.5h18v17H3v-17zM8 15l8-8M8.6 8.6h.01M15.4 15.4h.01',
  },
  {
    label: 'Danh mục thuộc tính',
    path: '/thuoc-tinh',
    icon: 'M8.5 6.5H21M8.5 12H21M8.5 17.5H21M3.5 6.5h.01M3.5 12h.01M3.5 17.5h.01',
  },
]
</script>

<template>
  <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
    <div class="brand">
      <svg class="brand-logo" viewBox="0 0 100 100" aria-label="Logo">
        <circle cx="50" cy="50" r="47" fill="#ffffff" stroke="#1f1f1f" stroke-width="4" />
        <circle cx="50" cy="50" r="39" fill="none" stroke="#1f1f1f" stroke-width="1.4" />
        <path
          d="M22 62c7-1 11-6 15-12 3-5 7-8 13-8 5 0 8 3 13 3 7 0 12-5 15-13-1 12-6 21-16 25-8 3-14 1-20 3-7 2-13 2-20 2z"
          fill="#d9232a"
        />
        <path d="M31 66h38" stroke="#1f1f1f" stroke-width="3" stroke-linecap="round" />
        <text
          x="50"
          y="88"
          text-anchor="middle"
          font-size="11"
          font-weight="700"
          fill="#1f1f1f"
          font-family="Arial"
        >
          SHOP
        </text>
      </svg>
    </div>

    <nav class="menu">
      <router-link
        v-for="item in menu"
        :key="item.path"
        :to="item.path"
        class="menu-item"
        active-class="is-active"
      >
        <span class="menu-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
            <path :d="item.icon" />
          </svg>
        </span>
        <span class="menu-label">{{ item.label }}</span>
      </router-link>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 212px;
  flex-shrink: 0;
  background: #ffffff;
  min-height: 100vh;
  position: sticky;
  top: 0;
  align-self: flex-start;
  padding: 18px 14px 24px;
}

.sidebar.is-collapsed {
  display: none;
}

.brand {
  display: flex;
  justify-content: center;
  padding: 6px 0 22px;
}

.brand-logo {
  width: 84px;
  height: 84px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 9px;
  color: #3a3a3a;
  font-size: 14px;
  transition: background 0.15s ease, color 0.15s ease;
}

.menu-item:hover {
  background: #f6f6f6;
}

.menu-item.is-active {
  background: var(--red-soft);
  color: var(--red);
  font-weight: 600;
}

.menu-icon {
  width: 20px;
  height: 20px;
  display: inline-flex;
  flex-shrink: 0;
}

.menu-icon svg {
  width: 100%;
  height: 100%;
}

.menu-label {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
