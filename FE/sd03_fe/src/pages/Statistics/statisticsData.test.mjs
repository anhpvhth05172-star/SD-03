import test from 'node:test'
import assert from 'node:assert/strict'
import { taoSoSanhDoanhThu } from './statisticsData.js'

const daily = (key, doanhThu, brand = {}) => ({ ngay: key, doanhThu, brands: brand })

test('taoSoSanhDoanhThu gom doanh thu theo thang duoc chon', () => {
  const bang = new Map([
    ['2026-01-05', daily('2026-01-05', 100, { Nike: { doanhThu: 40 } })],
    ['2026-01-12', daily('2026-01-12', 50, { Nike: { doanhThu: 10 } })],
    ['2026-02-01', daily('2026-02-01', 75, { Nike: { doanhThu: 30 } })],
  ])
  assert.deepEqual(
    taoSoSanhDoanhThu(bang, ['2026-01', '2026-02'], 'thang'),
    { labels: ['01/2026', '02/2026'], values: [150, 75] },
  )
})

test('taoSoSanhDoanhThu loc theo thuong hieu va gom quy, nam', () => {
  const bang = new Map([
    ['2025-12-01', daily('2025-12-01', 100, { Nike: { doanhThu: 40 } })],
    ['2026-01-01', daily('2026-01-01', 200, { Nike: { doanhThu: 80 } })],
    ['2026-04-01', daily('2026-04-01', 300, { Nike: { doanhThu: 120 } })],
  ])
  assert.deepEqual(
    taoSoSanhDoanhThu(bang, ['2026-Q1', '2026-Q2'], 'quy', 'Nike'),
    { labels: ['Q1/2026', 'Q2/2026'], values: [80, 120] },
  )
  assert.deepEqual(
    taoSoSanhDoanhThu(bang, ['2025', '2026'], 'nam'),
    { labels: ['2025', '2026'], values: [100, 500] },
  )
})
