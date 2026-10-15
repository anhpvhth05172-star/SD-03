<script setup>
import { provide } from 'vue'
import ChatWidget from './components/ChatWidget.vue'

const baseAPI = 'http://localhost:8080/'
provide('baseAPI', baseAPI)
</script>

<template>
  <router-view />
  <ChatWidget />
</template>

<style scoped></style>
