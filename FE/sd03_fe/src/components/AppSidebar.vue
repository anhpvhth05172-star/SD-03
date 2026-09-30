<script setup>
defineProps({
  collapsed: { type: Boolean, default: false },
})

const menu = [
  {
    label: 'Tổng Quan',
    path: '/tong-quan',
    icon: ['M4 4h6.5v6.5H4V4zm9.5 0H20v6.5h-6.5V4zM4 13.5h6.5V20H4v-6.5zm9.5 0H20V20h-6.5v-6.5z'],
  },
  {
    label: 'Quản Lý Hóa Đơn',
    path: '/hoa-don',
    evenodd: true,
    icon: [
      'M6 7h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zM9 7V6.5a3 3 0 0 1 6 0V7h-1.5V6.5a1.5 1.5 0 0 0-3 0V7H9z',
    ],
  },
  {
    label: 'Bán Hàng Tại Quầy',
    path: '/ban-hang',
    icon: [
      'M7 18c-1.1 0-1.99.9-1.99 2S5.9 22 7 22s2-.9 2-2-.9-2-2-2zM1 2v2h2l3.6 7.59-1.35 2.45c-.16.28-.25.61-.25.96 0 1.1.9 2 2 2h12v-2H7.42c-.14 0-.25-.11-.25-.25l.03-.12.9-1.63h7.45c.75 0 1.41-.41 1.75-1.03l3.58-6.49c.08-.14.12-.31.12-.49 0-.55-.45-1-1-1H5.21l-.94-2H1zm16 16c-1.1 0-1.99.9-1.99 2s.89 2 1.99 2 2-.9 2-2-.9-2-2-2z',
    ],
  },
  {
    label: 'Quản Lý Sản Phẩm',
    path: '/san-pham',
    expandable: true,
    icon: [
      'M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z',
    ],
  },
  {
    label: 'Quản Lý Tài Khoản',
    path: '/tai-khoan',
    expandable: true,
    icon: [
      'M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z',
    ],
  },
  {
    label: 'Khuyến Mãi',
    path: '/khuyen-mai',
    expandable: true,
    icon: [
      'M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z',
    ],
  },
]
</script>

<template>
  <aside class="sidebar" :class="{ 'is-collapsed': collapsed }">
    <div class="brand">
      <img src="/images/logo_polyshoes_final.png" alt="PolyShoes" />
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
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path
              v-for="(d, index) in item.icon"
              :key="index"
              :d="d"
              :fill-rule="item.evenodd ? 'evenodd' : 'nonzero'"
            />
          </svg>
        </span>
        <span class="menu-label">{{ item.label }}</span>
        <svg
          v-if="item.expandable"
          class="chevron"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M6 14.5l6-6 6 6" />
        </svg>
      </router-link>
    </nav>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 244px;
  flex-shrink: 0;
  background: var(--white);
  border-radius: 12px;
  padding: 16px 14px 22px;
}

.sidebar.is-collapsed {
  display: none;
}

.brand {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 6px 0 26px;
}

.brand img {
  width: 148px;
  height: 108px;
  object-fit: contain;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 11px 12px;
  border: 1px solid transparent;
  border-radius: 9px;
  color: #4b4b53;
  font-size: 14px;
  transition: background 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.menu-item:hover {
  background: #f7f7f8;
}

.menu-item.is-active {
  background: var(--red-soft);
  border-color: #f7ccd0;
  color: var(--red);
  font-weight: 600;
}

.menu-icon {
  width: 21px;
  height: 21px;
  display: inline-flex;
  flex-shrink: 0;
  color: #5a5a62;
}

.menu-item.is-active .menu-icon {
  color: var(--red);
}

.menu-icon svg {
  width: 100%;
  height: 100%;
}

.menu-label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chevron {
  width: 15px;
  height: 15px;
  color: #a8a8b0;
  flex-shrink: 0;
}
</style>
