<script setup>
import { computed } from 'vue'

const props = defineProps({
  slices: { type: Array, default: () => [] },
  size: { type: Number, default: 210 },
  donut: { type: Boolean, default: false },
  showTotal: { type: Boolean, default: true },
})

const R = 86
const C = 100

const clean = computed(() =>
  (props.slices || [])
    .map((s) => ({ ...s, value: Number(s.value) || 0 }))
    .filter((s) => s.value > 0)
)

const total = computed(() => clean.value.reduce((a, s) => a + s.value, 0))

const full = computed(() => clean.value.length === 1 && clean.value[0].value > 0)

const toXY = (angle) => ({
  x: C + R * Math.cos(angle),
  y: C + R * Math.sin(angle),
})

const arcs = computed(() => {
  if (!total.value) return []
  let start = -Math.PI / 2
  return clean.value.map((s) => {
    const sweep = (s.value / total.value) * Math.PI * 2
    const end = start + sweep
    const large = sweep > Math.PI ? 1 : 0
    const p1 = toXY(start)
    const p2 = toXY(end)
    const path =
      sweep >= Math.PI * 2 - 0.0001
        ? `M ${C} ${C - R} A ${R} ${R} 0 1 1 ${C - 0.01} ${C - R} Z`
        : `M ${C} ${C} L ${p1.x} ${p1.y} A ${R} ${R} 0 ${large} 1 ${p2.x} ${p2.y} Z`
    const mid = start + sweep / 2
    start = end
    return { path, color: s.color, label: s.label, value: s.value, mid }
  })
})

const fmt = (v) => new Intl.NumberFormat('vi-VN').format(Number(v) || 0)
</script>

<template>
  <div class="pie-wrap" :style="{ width: size + 'px', height: size + 'px' }">
    <svg v-if="total > 0" :viewBox="`0 0 ${C * 2} ${C * 2}`" class="pie-svg">
      <path
        v-for="(a, i) in arcs"
        :key="i"
        :d="a.path"
        :fill="a.color"
        class="pie-slice"
      >
        <title>{{ a.label }}: {{ fmt(a.value) }}</title>
      </path>
      <circle v-if="donut" :cx="C" :cy="C" r="54" fill="#ffffff" />
      <text v-if="donut && showTotal" :x="C" :y="C - 2" text-anchor="middle" class="pie-center-label">
        Tổng
      </text>
      <text v-if="donut && showTotal" :x="C" :y="C + 16" text-anchor="middle" class="pie-center-value">
        {{ fmt(total) }}
      </text>
    </svg>
    <div v-else class="pie-empty">Không có dữ liệu</div>
  </div>
</template>

<style scoped>
.pie-wrap {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
}

.pie-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.pie-slice {
  transition: filter 0.15s ease;
  cursor: default;
}

.pie-slice:hover {
  filter: brightness(1.07);
}

.pie-center-label {
  font-size: 12px;
  fill: #94a3b8;
  font-weight: 600;
}

.pie-center-value {
  font-size: 15px;
  fill: #1e293b;
  font-weight: 700;
}

.pie-empty {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12.5px;
  color: #94a3b8;
  text-align: center;
}
</style>
