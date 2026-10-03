<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta.title || 'Chức năng')
</script>

<template>
  <section class="card">
    <h2 class="card-title">{{ title }}</h2>
    <p class="empty">Chức năng đang được phát triển.</p>
  </section>
</template>

<style scoped>
.card {
  background: #ffffff;
  border: 1px solid var(--card-line);
  border-radius: 9px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  padding: 16px 18px 22px;
}

.card-title {
  color: var(--red);
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 16px;
}

.empty {
  color: var(--muted);
  font-size: 14px;
}
</style>
