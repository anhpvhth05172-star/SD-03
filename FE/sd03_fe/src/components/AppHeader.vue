<script setup>
defineProps({
  title: { type: String, default: '' },
})

defineEmits(['toggle-sidebar'])
</script>

<template>
  <header class="app-header">
    <div class="header-left">
      <button class="icon-btn menu-btn" type="button" aria-label="Thu gọn menu" @click="$emit('toggle-sidebar')">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
          <path d="M4 6.5h16M4 12h16M4 17.5h16" />
        </svg>
      </button>
      <h1 class="page-title">{{ title }}</h1>
    </div>

    <div class="header-right">
      <button class="icon-btn bell-btn" type="button" aria-label="Thông báo">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 8.5a6 6 0 1 0-12 0c0 6-2.5 7.5-2.5 7.5h17S18 14.5 18 8.5" />
          <path d="M13.7 19.5a2 2 0 0 1-3.4 0" />
        </svg>
        <span class="dot"></span>
      </button>
      <div class="avatar">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7.5" r="4" />
        </svg>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  height: 66px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 26px;
  position: sticky;
  top: 0;
  z-index: 20;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 21px;
  font-weight: 700;
  color: #1f1f1f;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #2f2f2f;
}

.menu-btn svg {
  width: 22px;
  height: 22px;
}

.bell-btn {
  position: relative;
  color: var(--red);
}

.bell-btn svg {
  width: 23px;
  height: 23px;
}

.bell-btn .dot {
  position: absolute;
  top: -2px;
  right: -3px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--red);
  border: 1.5px solid #fff;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--red-soft);
  color: var(--red);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar svg {
  width: 19px;
  height: 19px;
}
</style>
