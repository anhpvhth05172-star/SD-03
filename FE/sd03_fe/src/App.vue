<script setup>
import { provide, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ChatWidget from './components/ChatWidget.vue'

const baseAPI = 'http://localhost:8080/'
provide('baseAPI', baseAPI)

const router = useRouter()

const purgeOrphanOverlays = () => {
  if (typeof document === 'undefined') return
  document.querySelectorAll('.modal-backdrop').forEach(el => el.remove())
  document.querySelectorAll('.swal2-container:not(.swal2-shown)').forEach(el => el.remove())
  document.querySelectorAll('vue-devtools-host, #vue-devtools-anchor, [class*="vue-devtools"]').forEach(el => el.remove())

  if (!document.querySelector('.modal.show, .swal2-popup')) {
    document.body.classList.remove('modal-open', 'swal2-shown', 'swal2-height-auto')
    document.documentElement.classList.remove('swal2-shown', 'swal2-height-auto')
  }
}

onMounted(() => {
  purgeOrphanOverlays()
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') purgeOrphanOverlays()
  })
})

router.afterEach(() => {
  setTimeout(purgeOrphanOverlays, 60)
})
</script>

<template>
  <router-view />
  <ChatWidget />
</template>

<style scoped></style>
