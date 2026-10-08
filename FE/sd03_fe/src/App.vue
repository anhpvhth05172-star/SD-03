<script setup>
import { provide } from 'vue'

const baseAPI = 'http://localhost:8080/'
provide('baseAPI', baseAPI)
</script>

<template>
  <router-view />
</template>

<style scoped></style>
