<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '../components/AppSidebar.vue'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const pageTitle = computed(() => route.meta.title || '')
</script>

<template>
  <div class="admin-layout">
    <div class="topbar">
      <span class="topbar-title">{{ pageTitle }} 1</span>
      <span class="topbar-line"></span>
    </div>

    <div class="shell">
      <AppSidebar />

      <div class="main-area">
        <AppHeader />

        <div class="page-body">
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--black);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  height: 40px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  background: linear-gradient(90deg, #0a0a0b 0%, #161618 75%, #e0202a 150%);
  overflow: hidden;
}

.topbar-title {
  color: var(--white);
  font-size: 12.5px;
  font-weight: 500;
  letter-spacing: 0.2px;
  opacity: 0.95;
}

.topbar-line {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  background: var(--grad-tri);
}

.shell {
  flex: 1;
  display: flex;
  align-items: stretch;
  gap: 6px;
  padding: 8px 16px 14px;
}

.main-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--panel-bg);
  border: 1px solid var(--pink-line);
  border-radius: 12px;
  overflow: hidden;
}

.page-body {
  flex: 1;
  padding: 14px 16px 18px;
}
</style>
