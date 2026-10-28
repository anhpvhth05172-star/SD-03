<script setup>
import { computed } from 'vue'

const props = defineProps({
  labels: { type: Array, default: () => [] },
  values: { type: Array, default: () => [] },
  color: { type: String, default: '#d92d20' },
  mode: { type: String, default: 'cot' },
  series: { type: Array, default: null },
  height: { type: Number, default: 250 },
  // Mở rộng cho màn so sánh doanh thu (mặc định null → không đổi caller cũ)
  barSeries: { type: Array, default: null }, // [{label, color, values}] — cột nhóm
  lineSeries: { type: Array, default: null }, // [{label, color, values}] — đường thang riêng
  lineFormat: { type: String, default: 'so' }, // 'so' | 'percent'
  showLineLabels: { type: Boolean, default: false },
})

const W = 760
const H = computed(() => props.height)
const PAD = { l: 46, r: 14, t: 14, b: 30 }

const multi = computed(() => Array.isArray(props.series) && props.series.length > 0)
const hasBarSeries = computed(() => Array.isArray(props.barSeries) && props.barSeries.length > 0)
const hasLineSeries = computed(() => Array.isArray(props.lineSeries) && props.lineSeries.length > 0)
const labels = computed(() => props.labels || [])

const activeSeries = computed(() => {
  if (multi.value) return props.series
  return [{ color: props.color, values: props.values || [] }]
})

const yMax = computed(() => {
  let m = 0
  const quet = (vals) => (vals || []).forEach((v) => { if (Number(v) > m) m = Number(v) })
  activeSeries.value.forEach((s) => quet(s.values))
  if (hasBarSeries.value) props.barSeries.forEach((s) => quet(s.values))
  if (m <= 0) return 4
  const pow = Math.pow(10, Math.floor(Math.log10(m)))
  return [1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10].map((x) => x * pow).find((x) => x >= m) || m
})

const ticks = computed(() => [0, 0.25, 0.5, 0.75, 1].map((r) => ({
  y: PAD.t + (H.value - PAD.t - PAD.b) * (1 - r),
  label: Math.round(yMax.value * r),
})))

const innerW = W - PAD.l - PAD.r
const innerH = computed(() => H.value - PAD.t - PAD.b)

const band = computed(() => (labels.value.length ? innerW / labels.value.length : innerW))
const barW = computed(() => Math.max(3, Math.min(26, band.value * 0.55)))

const xCenter = (i) => PAD.l + band.value * i + band.value / 2
const yOf = (v) => PAD.t + innerH.value * (1 - (Number(v) || 0) / yMax.value)

const bars = computed(() => {
  if (multi.value || hasBarSeries.value || props.mode !== 'cot') return []
  const vals = props.values || []
  return vals.map((v, i) => {
    const y = yOf(v)
    return {
      x: PAD.l + band.value * i + (band.value - barW.value) / 2,
      y,
      w: barW.value,
      h: Math.max(0, PAD.t + innerH.value - y),
      v,
      label: labels.value[i] || '',
    }
  })
})

/* ---------------------------------- Cột nhóm (barSeries) ---------------------------------- */
const groupBarW = computed(() => {
  const n = Math.max(1, (props.barSeries || []).length)
  return Math.max(2, Math.min(24, (band.value * 0.8) / n))
})

const groupedBars = computed(() => {
  if (!hasBarSeries.value || props.mode !== 'cot') return []
  const n = props.barSeries.length
  const w = groupBarW.value
  const out = []
  props.barSeries.forEach((s, si) => {
    (s.values || []).forEach((v, i) => {
      const groupX = PAD.l + band.value * i + (band.value - w * n) / 2
      const y = yOf(v)
      out.push({
        x: groupX + w * si,
        y,
        w: Math.max(1, w - 1),
        h: Math.max(0, PAD.t + innerH.value - y),
        v,
        color: s.color || props.color,
        cat: labels.value[i] || '',
        label: s.label || '',
      })
    })
  })
  return out
})

/* ---------------------------------- Đường thang riêng (lineSeries) ---------------------------------- */
const lineScale = computed(() => {
  if (!hasLineSeries.value) return null
  let min = 0
  let max = 0
  props.lineSeries.forEach((s) =>
    (s.values || []).forEach((v) => {
      const n = Number(v) || 0
      if (n < min) min = n
      if (n > max) max = n
    })
  )
  if (min === 0 && max === 0) max = 1
  const range = max - min || 1
  const pad = range * 0.12
  return { lo: min < 0 ? min - pad : 0, hi: max + pad }
})

const yLine = (v) => {
  const s = lineScale.value
  if (!s) return PAD.t + innerH.value
  const ty = ((Number(v) || 0) - s.lo) / (s.hi - s.lo || 1)
  return PAD.t + innerH.value * (1 - ty)
}

const linePointsSeries = (vals) => (vals || []).map((v, i) => `${xCenter(i)},${yLine(v)}`).join(' ')

const fmtLine = (v) =>
  props.lineFormat === 'percent'
    ? `${Number(v) > 0 ? '+' : ''}${fmt(v)}%`
    : fmt(v)

const showLineDots = computed(() => labels.value.length <= 40)

const legendItems = computed(() => {
  if (multi.value) return props.series
  if (hasBarSeries.value || hasLineSeries.value) {
    return [
      ...(hasBarSeries.value ? props.barSeries : []),
      ...(hasLineSeries.value ? props.lineSeries : []),
    ]
  }
  return []
})

const linePoints = (vals) => (vals || []).map((v, i) => `${xCenter(i)},${yOf(v)}`).join(' ')

const areaPath = (vals) => {
  if (!vals || !vals.length) return ''
  const first = xCenter(0)
  const last = xCenter(vals.length - 1)
  const base = PAD.t + innerH.value
  return `M ${first} ${base} ` + vals.map((v, i) => `L ${xCenter(i)} ${yOf(v)}`).join(' ') + ` L ${last} ${base} Z`
}

const showDots = computed(() => (props.values || []).length <= 40 && !multi.value)

const labelStep = computed(() => {
  const n = labels.value.length
  if (n <= 10) return 1
  return Math.ceil(n / 8)
})

const showLine = computed(() => props.mode === 'duong')

const fmt = (v) => new Intl.NumberFormat('vi-VN').format(Number(v) || 0)

const isEmpty = computed(() => {
  if (hasBarSeries.value || hasLineSeries.value) {
    const coGiaTri = (ds) => (ds || []).some((s) => (s.values || []).some((v) => Number(v) !== 0))
    return !coGiaTri(hasBarSeries.value ? props.barSeries : null) && !coGiaTri(props.lineSeries)
  }
  if (multi.value) return !props.series.some((s) => (s.values || []).some((v) => Number(v) > 0))
  return !(props.values || []).some((v) => Number(v) > 0)
})
</script>

<template>
  <div class="blc-wrap">
    <div v-if="legendItems.length" class="blc-legend">
      <span v-for="(s, i) in legendItems" :key="i" class="blc-legend-item">
        <span class="blc-dot" :style="{ background: s.color }"></span>
        {{ s.label }}
      </span>
    </div>

    <svg class="blc-svg" :viewBox="`0 0 ${W} ${H}`" role="img" aria-label="Biểu đồ">
      <g>
        <line
          v-for="t in ticks"
          :key="'g' + t.y"
          :x1="PAD.l"
          :x2="W - PAD.r"
          :y1="t.y"
          :y2="t.y"
          stroke="#eef2f7"
          stroke-width="1"
        />
        <text
          v-for="t in ticks"
          :key="'t' + t.y"
          :x="PAD.l - 8"
          :y="t.y + 4"
          text-anchor="end"
          class="blc-tick"
        >
          {{ t.label }}
        </text>
      </g>

      <g v-if="labels.length">
        <text
          v-for="(lb, i) in labels"
          :key="'x' + i"
          :x="xCenter(i)"
          :y="H - 8"
          text-anchor="middle"
          class="blc-axis-label"
        >
          {{ i % labelStep === 0 ? lb : '' }}
        </text>
      </g>

      <template v-if="!isEmpty">
        <g v-if="mode === 'cot' && hasBarSeries">
          <rect
            v-for="(b, i) in groupedBars"
            :key="'gb' + i"
            :x="b.x"
            :y="b.y"
            :width="b.w"
            :height="b.h"
            rx="3"
            ry="3"
            :fill="b.color"
            class="blc-bar"
          >
            <title>{{ b.cat }} — {{ b.label }}: {{ fmt(b.v) }}</title>
          </rect>
        </g>

        <g v-else-if="mode === 'cot' && !multi">
          <rect
            v-for="(b, i) in bars"
            :key="'b' + i"
            :x="b.x"
            :y="b.y"
            :width="b.w"
            :height="b.h"
            rx="4"
            ry="4"
            :fill="color"
            class="blc-bar"
          >
            <title>{{ b.label }}: {{ fmt(b.v) }}</title>
          </rect>
        </g>

        <g v-else>
          <path
            v-if="!multi && showLine && (values || []).length"
            :d="areaPath(values)"
            :fill="color"
            opacity="0.08"
          />
          <g v-for="(s, si) in activeSeries" :key="'s' + si">
            <polyline
              :points="linePoints(s.values)"
              fill="none"
              :stroke="s.color || color"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <template v-if="showDots && !multi">
              <circle
                v-for="(v, i) in s.values"
                :key="'d' + i"
                :cx="xCenter(i)"
                :cy="yOf(v)"
                r="3.4"
                fill="#ffffff"
                :stroke="s.color || color"
                stroke-width="2"
              >
                <title>{{ labels[i] }}: {{ fmt(v) }}</title>
              </circle>
            </template>
          </g>
        </g>

        <!-- Đường tăng trưởng thang riêng (lineSeries) -->
        <g v-if="hasLineSeries">
          <line
            v-if="lineScale && lineScale.lo < 0 && lineScale.hi > 0"
            :x1="PAD.l"
            :x2="W - PAD.r"
            :y1="yLine(0)"
            :y2="yLine(0)"
            stroke="#cbd5e1"
            stroke-width="1"
            stroke-dasharray="4 4"
          />
          <g v-for="(s, si) in lineSeries" :key="'ls' + si">
            <polyline
              :points="linePointsSeries(s.values)"
              fill="none"
              :stroke="s.color || '#059669'"
              stroke-width="2.2"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <template v-if="showLineDots">
              <g v-for="(v, i) in s.values" :key="'ld' + i">
                <circle
                  :cx="xCenter(i)"
                  :cy="yLine(v)"
                  r="3.4"
                  fill="#ffffff"
                  :stroke="s.color || '#059669'"
                  stroke-width="2"
                >
                  <title>{{ labels[i] }}: {{ fmtLine(v) }}</title>
                </circle>
                <text
                  v-if="showLineLabels"
                  :x="xCenter(i)"
                  :y="yLine(v) - 9"
                  text-anchor="middle"
                  class="blc-line-label"
                >
                  {{ fmtLine(v) }}
                </text>
              </g>
            </template>
          </g>
        </g>
      </template>

      <text
        v-else
        :x="W / 2"
        :y="H / 2"
        text-anchor="middle"
        class="blc-empty"
      >
        Không có dữ liệu trong khoảng này
      </text>
    </svg>
  </div>
</template>

<style scoped>
.blc-wrap {
  width: 100%;
}

.blc-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-bottom: 8px;
}

.blc-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #475569;
  font-weight: 500;
}

.blc-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  display: inline-block;
}

.blc-svg {
  width: 100%;
  height: auto;
  display: block;
}

.blc-tick {
  font-size: 11px;
  fill: #94a3b8;
}

.blc-axis-label {
  font-size: 11px;
  fill: #64748b;
}

.blc-empty {
  font-size: 13px;
  fill: #94a3b8;
}

.blc-bar {
  transition: opacity 0.15s ease;
}

.blc-bar:hover {
  opacity: 0.82;
}

.blc-line-label {
  font-size: 10px;
  font-weight: 600;
  fill: #059669;
}
</style>
