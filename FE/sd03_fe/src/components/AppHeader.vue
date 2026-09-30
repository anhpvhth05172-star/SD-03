<script setup>
defineProps({
  title: { type: String, default: '' },
})
</script>

<template>
  <header class="app-header">
    <nav class="breadcrumb">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3.5 10.5L12 3.5l8.5 7v9a1.5 1.5 0 0 1-1.5 1.5h-4.5v-6h-5v6H5a1.5 1.5 0 0 1-1.5-1.5v-9z" />
      </svg>
      <span class="crumb">Trang chủ</span>
      <span class="crumb-sep">/</span>
      <svg class="crumb-doc" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
        <path d="M6 2.5h8l4 4v15H6v-19zM14 2.5v4h4M9 12h6M9 16h6" />
      </svg>
      <span class="crumb-current">{{ title }}</span>
    </nav>

    <div class="user-box">
      <span class="notif-dot"></span>
      <div class="avatar">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7.5" r="4" />
        </svg>
      </div>
      <div class="user-meta">
        <strong>Phạm Hoàng Anh</strong>
        <span>Giám đốc</span>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: 56px;
  background: var(--white);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 0 22px;
  border-bottom: 1.5px solid var(--pink-line);
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4a4a52;
  font-size: 13px;
}

.breadcrumb svg {
  width: 15px;
  height: 15px;
  color: #2b2b32;
}

.crumb {
  font-weight: 600;
  color: #2b2b32;
}

.crumb-sep {
  color: #b0b0b8;
}

.crumb-doc {
  color: #7a7a83;
}

.crumb-current {
  color: #23232a;
  font-weight: 600;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notif-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--red);
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f1f2f5;
  color: #4b4b53;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar svg {
  width: 17px;
  height: 17px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-meta strong {
  font-size: 11.5px;
  color: #23232a;
  font-weight: 600;
}

.user-meta span {
  font-size: 10.5px;
  color: #9a9aa3;
}
</style>
