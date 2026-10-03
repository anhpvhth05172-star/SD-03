<script setup>
import { provide } from "vue";
// import { provide } from "vue";
// async function testBackend() {
//   try {
//     const response = await axios.get("http://localhost:8080/api/test");
//     console.log("Backend trả về:", response.data);
//     alert(response.data);
//   } catch (error) {
//     console.error("Lỗi kết nối:", error);
//     alert("Không kết nối được Backend");
//   }
// }

let baseAPI = "http://localhost:8080/";
provide("baseAPI", baseAPI);
</script>

<template>
  <router-view />
</template>

<style scoped></style>
