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
    <div class="shell">
      <AppSidebar />

      <div class="main-area">
        <AppHeader :title="pageTitle" />

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
  background: var(--app-bg);
}

.shell {
  flex: 1;
  display: flex;
  align-items: stretch;
  gap: 8px;
  padding: 10px 14px 14px;
}

.main-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--white);
  border-radius: 12px;
  overflow: hidden;
}

.page-body {
  flex: 1;
  padding: 16px 18px 20px;
  background: var(--app-bg);
}
</style>
