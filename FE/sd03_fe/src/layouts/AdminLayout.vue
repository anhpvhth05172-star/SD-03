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
          <router-view :key="$route.fullPath" />
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
  background: #f8fafc;
}

.shell {
  flex: 1;
  display: flex;
  align-items: stretch;
  gap: 0;
  padding: 0;
  min-height: 100vh;
}

.main-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
  border-radius: 0;
}

.page-body {
  flex: 1;
  padding: 20px 24px;
  background: #f8fafc;
}
</style>
