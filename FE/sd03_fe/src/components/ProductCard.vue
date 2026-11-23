<script setup>
import ProductCard from "@/pages/clients/views/ProductCard.vue";
defineProps({
  product: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <ProductCard :product="product" />
</template>
