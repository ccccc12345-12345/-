import { chromium } from 'playwright'
import fs from 'fs'

const BASE = 'http://localhost:3002'
const OUTDIR = 'e:/工作项目/钓鱼/fishing-pc/screenshots'

if (!fs.existsSync(OUTDIR)) fs.mkdirSync(OUTDIR, { recursive: true })

const browser = await chromium.launch({ headless: true })
const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
const page = await context.newPage()

async function screenshot(name, url, setup) {
  console.log(`screenshot: ${name}`)
  await page.goto(url, { waitUntil: 'networkidle', timeout: 15000 })
  if (setup) await setup(page)
  await page.screenshot({ path: `${OUTDIR}/${name}.png`, fullPage: true })
}

async function fillInput(page, placeholder, value) {
  const input = page.getByPlaceholder(placeholder, { exact: true })
  await input.evaluate((el, v) => {
    el.value = v
    el.dispatchEvent(new Event('input', { bubbles: true }))
    el.dispatchEvent(new Event('change', { bubbles: true }))
  }, value)
}

// 1. 登录页
await screenshot('01-login', `${BASE}/login`)

// 2. 商家登录后渔获回收页（直接设置 token 绕过登录）
await page.goto(`${BASE}/login`)
const merchantUser = { token: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNiIsImlhdCI6MTc4NDcxMjM1NiwiZXhwIjoxNzg0Nzk4NzU2fQ.QRfo2bk418Ydnh_8IEMcdAtqhr7Z4yRV_kqo1DuY00E', userId: '16', username: '18800000001', role: 1 }
await page.evaluate((u) => { localStorage.setItem('user', JSON.stringify(u)) }, merchantUser)
await page.goto(`${BASE}/merchant/catches`)
await page.waitForTimeout(1500)
await screenshot('02-merchant-catches', `${BASE}/merchant/catches`)
await screenshot('03-merchant-shop-orders', `${BASE}/merchant/shop/orders`)
await screenshot('04-merchant-reservations', `${BASE}/merchant/reservations`)

// 3. 用户登录后渔获页和商城
const normalUser = { token: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxNyIsImlhdCI6MTc4NDcxMjMyMiwiZXhwIjoxNzg0Nzk4NzIyfQ.3mTGKICnPC0cuQ-s0Ap7Efj9L4pWMjywXDEn3Hz0vNg', userId: '17', username: '18800000002', role: 0 }
await page.evaluate((u) => { localStorage.setItem('user', JSON.stringify(u)) }, normalUser)
await page.goto(`${BASE}/user/catches`)
await page.waitForTimeout(1500)
await screenshot('05-user-catches', `${BASE}/user/catches`)
await screenshot('06-shop-home', `${BASE}/shop`)
await screenshot('07-shop-orders', `${BASE}/shop/orders`)

await browser.close()
console.log('done, screenshots in', OUTDIR)
