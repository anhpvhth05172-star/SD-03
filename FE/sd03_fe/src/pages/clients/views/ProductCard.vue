<script setup>
import { money } from "../store";

defineProps({
  product: {
    type: Object,
    required: true,
  },
});
</script>

<template>
  <RouterLink
    :to="`/cua-hang/${product.id}`"
    class="text-decoration-none text-reset"
  >
    <div class="ps-card p-3 h-100 d-flex flex-column gap-2">
      <div class="ps-thumb position-relative">
        {{ product.emoji }}
        <span
          v-if="product.old"
          class="ps-pill position-absolute top-0 start-0 m-2 bg-white"
        >
          -{{ Math.round((1 - product.price / product.old) * 100) }}%
        </span>
      </div>
      <div class="small text-muted-ps">
        {{ product.brand }} · {{ product.type }}
      </div>
      <h6 class="mb-0 fw-bold">{{ product.name }}</h6>
      <div class="mt-auto d-flex align-items-baseline gap-2">
        <span class="price">{{ money(product.price) }}</span>
        <small
          v-if="product.old"
          class="text-muted-ps text-decoration-line-through"
          >{{ money(product.old) }}</small
        >
      </div>
    </div>
  </RouterLink>
</template>
