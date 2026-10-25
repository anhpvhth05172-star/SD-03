// Kiểm tra contract upload: importEmployeesConfirm PHẢI gửi file lên server
// dạng multipart FormData (không bị axios serialize thành JSON).
// Chạy: npm run verify:import
import assert from 'node:assert/strict'
import { createServer } from 'vite'

const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })

try {
  const { importEmployeesConfirm } = await server.ssrLoadModule('/src/api/employee.js')
  const { default: http } = await server.ssrLoadModule('/src/api/http.js')

  let captured = null
  http.defaults.adapter = async (config) => {
    captured = config
    return { data: {}, status: 200, statusText: 'OK', headers: {}, config }
  }

  const file = new File(['fake-xlsx-bytes'], 'nhan-vien.xlsx', {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  })

  let error = null
  try {
    await importEmployeesConfirm(file)
  } catch (e) {
    error = e
  }

  assert.equal(error, null, `request lỗi trước khi tới adapter: ${error && error.message}`)
  assert.ok(captured, 'adapter không được gọi')

  assert.ok(
    captured.data instanceof FormData,
    'dữ liệu gửi lên phải là FormData, nhận được: ' +
      (typeof captured.data === 'string' ? `string ${captured.data.slice(0, 120)}` : typeof captured.data),
  )

  const part = captured.data.get('file')
  assert.ok(part instanceof File, 'FormData phải chứa part "file" là File')
  assert.equal(part.name, 'nhan-vien.xlsx')

  // Không assert Content-Type ở đây: capture adapter nằm TRƯỚC xhr adapter.
  // Trong browser, xhr adapter (resolveConfig.js) xóa Content-Type của FormData,
  // rồi spec XHR tự gắn multipart/form-data; boundary=... — đúng chuẩn, Spring nhận file.
  // Bug cần chặn: FormData bị serialize thành JSON TRƯỚC khi tới adapter (chuỗi trên).

  captured = null
  await http.post('/api/test-json', { ho: 'ten' })
  assert.ok(captured, 'request JSON không tới adapter')
  assert.equal(typeof captured.data, 'string', 'request JSON phải được stringify')
  assert.deepEqual(JSON.parse(captured.data), { ho: 'ten' })
  const jsonCt = typeof captured.headers?.get === 'function'
    ? captured.headers.get('Content-Type')
    : captured.headers?.['Content-Type']
  assert.ok(String(jsonCt).includes('application/json'), 'request JSON phải giữ Content-Type application/json')

  console.log('PASS: import giữ nguyên FormData (multipart) + JSON vẫn gửi đúng.')
} finally {
  await server.close()
}
