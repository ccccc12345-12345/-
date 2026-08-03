import { chromium } from 'playwright'
import path from 'path'

const BASE = 'http://localhost:4174'
const merchant = { username: '18800000001', password: '123456', role: 1 }

async function apiLogin() {
  const res = await fetch(`${BASE}/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: merchant.username, password: merchant.password, captchaKey: 'demo', captchaCode: 'demo' })
  })
  const json = await res.json()
  return json.data
}

async function main() {
  const data = await apiLogin()
  const browser = await chromium.launch({ headless: true, args: ['--disable-gpu', '--disable-logging', '--log-level=3'] })
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  const page = await context.newPage()
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((d) => {
    localStorage.setItem('fishing_pc_token', d.token)
    localStorage.setItem('fishing_pc_user_id', String(d.userId))
    localStorage.setItem('fishing_pc_username', d.username)
    localStorage.setItem('fishing_pc_role', String(d.role))
    if (d.pondId != null) localStorage.setItem('fishing_pc_pond_id', String(d.pondId))
    if (d.merchantId != null) localStorage.setItem('fishing_pc_merchant_id', String(d.merchantId))
  }, data)
  await page.goto(`${BASE}/merchant/pond-board?pondId=4`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  await page.waitForTimeout(2000)
  const file = path.resolve('screenshots', 'pond-board.png')
  await page.screenshot({ path: file, fullPage: true })
  console.log('截图已保存:', file)

  await page.goto(`${BASE}/merchant/ponds/4/spots`, { waitUntil: 'domcontentloaded', timeout: 20000 })
  await page.waitForTimeout(1500)
  const spotFile = path.resolve('screenshots', 'spot-editor.png')
  await page.screenshot({ path: spotFile, fullPage: true })
  console.log('截图已保存:', spotFile)

  await browser.close()
}

main().catch(err => { console.error(err); process.exit(1) })
