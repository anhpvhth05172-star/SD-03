<script setup>
import { provide } from 'vue'
import ChatWidget from './components/ChatWidget.vue'
import { API_ORIGIN } from './api/http'

const baseAPI = `${API_ORIGIN}/`
provide('baseAPI', baseAPI)
</script>

<template>
  <router-view />
  <ChatWidget />
</template>

<style scoped></style>
