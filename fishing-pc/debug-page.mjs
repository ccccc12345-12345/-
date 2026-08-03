import { chromium } from 'playwright'

const logs = []
const pageErrors = []

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({ viewport: { width: 1280, height: 800 } })
const page = await context.newPage()

page.on('console', msg => {
  const text = `[${msg.type()}] ${msg.text()}`
  logs.push(text)
  console.log(text)
})
page.on('pageerror', err => {
  pageErrors.push(err.message)
  console.log('[PAGE ERROR]', err.message)
})
page.on('response', async res => {
  if (res.status() >= 400) {
    const body = await res.text().catch(() => '')
    console.log('[HTTP ERROR]', res.status(), res.url(), body.slice(0, 200))
  }
})

async function visit(url, selector) {
  console.log('\n===== Visiting', url, '=====')
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 10000 })
  try {
    await page.waitForSelector(selector, { timeout: 5000 })
  } catch {
    console.log('Selector not found:', selector)
  }
  await page.waitForTimeout(800)
  const html = await page.content()
  console.log('URL:', page.url())
  console.log('HTML length:', html.length)
  console.log('Body text preview:', (await page.innerText('body')).slice(0, 200).replace(/\s+/g, ' '))
}

// 先访问前端页面建立 origin，再调用登录 API
await page.goto('http://localhost:3002/login', { waitUntil: 'domcontentloaded' })
const loginRes = await page.evaluate(async () => {
  const res = await fetch('/api/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: '18800000001', password: '123456', captchaKey: 'demo', captchaCode: 'demo' })
  })
  return res.json()
})
console.log('Login response:', JSON.stringify(loginRes).slice(0, 300))

if (!loginRes.data?.token) {
  console.log('Login failed')
  await browser.close()
  process.exit(1)
}

const d = loginRes.data
await page.evaluate((data) => {
  localStorage.setItem('fishing_pc_token', data.token)
  localStorage.setItem('fishing_pc_user_id', String(data.userId))
  localStorage.setItem('fishing_pc_username', '18800000001')
  localStorage.setItem('fishing_pc_role', String(data.role))
  if (data.adminType != null) localStorage.setItem('fishing_pc_admin_type', String(data.adminType))
  if (data.pondId != null) localStorage.setItem('fishing_pc_pond_id', String(data.pondId))
  if (data.staffId != null) localStorage.setItem('fishing_pc_staff_id', String(data.staffId))
  if (data.merchantId != null) localStorage.setItem('fishing_pc_merchant_id', String(data.merchantId))
  if (data.staffRole) localStorage.setItem('fishing_pc_staff_role', data.staffRole)
}, d)

await visit('http://localhost:3002/merchant/checkin', '.checkin-card')
await visit('http://localhost:3002/merchant/catches', '.page-header')
await visit('http://localhost:3002/merchant/reservations', '.page-header')
await visit('http://localhost:3002/merchant/shop/orders', '.page-header')

console.log('\n===== SUMMARY =====')
console.log('Page errors:', pageErrors)
console.log('Total console logs:', logs.length)

await browser.close()
