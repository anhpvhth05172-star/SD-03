<script setup>
defineProps({
  tone: { type: String, default: 'red' },
  icon: { type: Array, required: true },
  evenodd: { type: Boolean, default: false },
  label: { type: String, default: '' },
  value: { type: [String, Number], default: '' },
  note: { type: String, default: '' },
  noteTone: { type: String, default: 'red' },
})
</script>

<template>
  <div class="stat-card">
    <span class="stat-icon" :class="`tone-${tone}`">
      <svg viewBox="0 0 24 24" fill="currentColor">
        <path
          v-for="(d, index) in icon"
          :key="index"
          :d="d"
          :fill-rule="evenodd ? 'evenodd' : 'nonzero'"
        />
      </svg>
    </span>
    <div class="stat-body">
      <span class="stat-label">{{ label }}</span>
      <strong class="stat-value">{{ value }}</strong>
      <span class="stat-note" :class="`note-${noteTone}`">{{ note }}</span>
    </div>
  </div>
</template>
