<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '../components/AppSidebar.vue'
import AppHeader from '../components/AppHeader.vue'

const route = useRoute()
const collapsed = ref(false)
const pageTitle = computed(() => route.meta.title || '')
</script>

<template>
  <div class="admin-layout">
    <AppSidebar :collapsed="collapsed" />

    <div class="main-area">
      <AppHeader :title="pageTitle" @toggle-sidebar="collapsed = !collapsed" />

      <div class="page-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.main-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--page-bg);
}

.page-body {
  flex: 1;
  padding: 22px;
  border-top: 3px solid var(--blue);
  border-left: 3px solid var(--blue);
}
</style>
