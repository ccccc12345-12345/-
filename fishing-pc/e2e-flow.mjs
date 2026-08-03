import { chromium } from 'playwright'
import fs from 'fs'
import path from 'path'
import { exec } from 'child_process'
import { promisify } from 'util'

const execAsync = promisify(exec)

const BASE = 'http://localhost:3002'
const API = 'http://localhost:8080'
const SCREEN_DIR = path.resolve('screenshots', 'flow')

fs.mkdirSync(SCREEN_DIR, { recursive: true })

const logs = []
const pageErrors = []
const networkErrors = []

async function apiLogin(username, password) {
  const res = await fetch(`${API}/api/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password, captchaKey: 'demo', captchaCode: 'demo' })
  })
  const json = await res.json()
  if (json.code !== 200 || !json.data?.token) {
    throw new Error(`登录失败 ${username}: ${JSON.stringify(json)}`)
  }
  return json.data
}

async function apiGet(token, url) {
  const res = await fetch(`${API}${url}`, { headers: { Authorization: `Bearer ${token}` } })
  return res.json()
}

async function apiPost(token, url, body) {
  const res = await fetch(`${API}${url}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: JSON.stringify(body)
  })
  return res.json()
}

async function apiPut(token, url, body) {
  const res = await fetch(`${API}${url}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
    body: body ? JSON.stringify(body) : undefined
  })
  return res.json()
}

async function cleanupTestReservations(userId) {
  const mysqlPath = '"C:\\Program Files\\MySQL\\MySQL Server 9.4\\bin\\mysql.exe"'
  const sql = `DELETE FROM fishing_reservation.reservation WHERE user_id = ${userId};`
  try {
    const { stdout, stderr } = await execAsync(
      `${mysqlPath} -h localhost -u root -proot -e "${sql}"`,
      { timeout: 10000 }
    )
    console.log('清理测试账号历史预约:', stdout?.trim() || '', stderr?.trim() || '')
  } catch (e) {
    console.log('清理测试账号历史预约失败（可忽略）:', e.message)
  }
}

function setupPageListeners(page) {
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
      const info = `[HTTP ERROR] ${res.status()} ${res.url()} ${body.slice(0, 200)}`
      networkErrors.push(info)
      console.log(info)
    }
  })
}

async function screenshot(page, name) {
  const file = path.join(SCREEN_DIR, `${name}.png`)
  await page.screenshot({ path: file, fullPage: true })
  console.log('截图已保存:', file)
}

async function setUserStorage(page, data, username) {
  await page.goto(`${BASE}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((d) => {
    localStorage.setItem('fishing_pc_token', d.token)
    localStorage.setItem('fishing_pc_user_id', String(d.userId))
    localStorage.setItem('fishing_pc_username', d.username)
    localStorage.setItem('fishing_pc_role', String(d.role))
    if (d.adminType != null) localStorage.setItem('fishing_pc_admin_type', String(d.adminType))
    if (d.pondId != null) localStorage.setItem('fishing_pc_pond_id', String(d.pondId))
    if (d.staffId != null) localStorage.setItem('fishing_pc_staff_id', String(d.staffId))
    if (d.merchantId != null) localStorage.setItem('fishing_pc_merchant_id', String(d.merchantId))
    if (d.staffRole) localStorage.setItem('fishing_pc_staff_role', d.staffRole)
  }, { ...data, username })
}

async function openAndWait(page, url, selector, wait = 1200) {
  console.log('\n===== Opening', url, '=====')
  await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 15000 })
  try {
    await page.waitForSelector(selector, { timeout: 8000 })
    console.log('Selector found:', selector)
  } catch {
    console.log('Selector NOT found:', selector)
  }
  await page.waitForTimeout(wait)
}

async function main() {
  console.log('=== 启动端到端流程验证 ===')
  const browser = await chromium.launch({
    headless: true,
    args: [
      '--disable-gpu',
      '--disable-software-rasterizer',
      '--disable-dev-shm-usage',
      '--disable-logging',
      '--log-level=3',
      '--log-file=NUL',
      '--disable-crash-reporter',
      '--disable-breakpad',
      '--disable-crashpad'
    ]
  })
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })

  // 1. 登录拿到两个身份 token
  const merchantData = await apiLogin('18800000001', '123456')
  const userData = await apiLogin('18800000002', '123456')
  console.log('商家登录成功 userId=', merchantData.userId)
  console.log('用户登录成功 userId=', userData.userId)

  // 清理测试账号的历史预约，避免数据库唯一约束 uk_user_slot 导致无法重新预约
  await cleanupTestReservations(userData.userId)

  // 2. 用户查询鱼塘和时段（使用商家拥有的鱼塘）
  const pondsRes = await apiGet(userData.token, '/api/ponds')
  const ponds = (pondsRes.data || []).filter(p => p.merchantId === Number(merchantData.userId))
  if (!ponds.length) throw new Error('没有该商家的鱼塘数据')
  const pond = ponds[0]
  console.log('鱼塘:', pond.id, pond.name)

  const myResRes = await apiGet(userData.token, '/api/reservation/my')
  const myReservations = myResRes.data?.records || []
  const myActiveReservations = myReservations.filter(r => r.status === '待抽号' || r.status === '已抽号')
  const mySlotIds = new Set(myActiveReservations.map(r => r.slotId))

  let slotsRes = await apiGet(userData.token, `/api/time-slots?pondId=${pond.id}`)
  let slots = (slotsRes.data || []).filter(s => s.status === 1 && !mySlotIds.has(s.id))

  // 若该鱼塘可约时段被当前账号占满，先取消本账号的待处理预约以释放时段
  if (!slots.length && myActiveReservations.length > 0) {
    console.log('可约时段不足，取消当前账号已有预约以释放时段')
    for (const r of myActiveReservations) {
      await apiPut(userData.token, `/api/reservation/cancel/${r.id}`, null)
      console.log('已取消预约 id=', r.id)
    }
    const refreshedRes = await apiGet(userData.token, '/api/reservation/my')
    const refreshedSlotIds = new Set((refreshedRes.data?.records || []).map(r => r.slotId))
    slotsRes = await apiGet(userData.token, `/api/time-slots?pondId=${pond.id}`)
    slots = (slotsRes.data || []).filter(s => s.status === 1 && !refreshedSlotIds.has(s.id))
  }

  if (!slots.length) throw new Error('没有可预约时段')
  const slot = slots[0]
  console.log('时段:', slot.id, slot.slotDate, slot.startTime, slot.endTime)

  // 3. 用户创建预约
  const reservationRes = await apiPost(userData.token, '/api/reservation', {
    slotId: slot.id
  })
  console.log('预约结果:', JSON.stringify(reservationRes).slice(0, 200))
  if (reservationRes.code !== 200) {
    console.log('预约可能失败，继续后续步骤')
  }

  // 4. 商家打开预约管理页面验证（使用独立 context 避免 localStorage 被用户覆盖）
  const merchantContext = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  const merchantPage = await merchantContext.newPage()
  setupPageListeners(merchantPage)
  await setUserStorage(merchantPage, merchantData, '18800000001')
  await openAndWait(merchantPage, `${BASE}/merchant/reservations`, '.page-header')
  await screenshot(merchantPage, '01-merchant-reservations')

  // 5. 用户创建渔获并申请回收
  const catchRes = await apiPost(userData.token, '/api/catch', {
    pondId: pond.id,
    fishType: '鲤鱼',
    weight: 2.5,
    quantity: 1,
    imageUrl: ''
  })
  console.log('创建渔获结果:', JSON.stringify(catchRes).slice(0, 200))
  if (catchRes.code !== 200 || !catchRes.data) {
    throw new Error('创建渔获失败')
  }
  const catchId = catchRes.data
  await apiPut(userData.token, `/api/catch/${catchId}/request-recycle`, null)
  console.log('已申请回收 catchId=', catchId)

  // 6. 商家打开渔获回收页面
  await openAndWait(merchantPage, `${BASE}/merchant/catches`, '.page-header')
  await screenshot(merchantPage, '02-merchant-catches-before')

  // 填写回收价格并确认回收
  const priceInput = merchantPage.locator('.el-input-number input').first()
  if (await priceInput.count() > 0) {
    await priceInput.fill('15')
    console.log('已填写回收价格')
  }
  const confirmBtn = merchantPage.locator('button:has-text("确认回收")').first()
  if (await confirmBtn.count() > 0) {
    await confirmBtn.click()
    await merchantPage.waitForTimeout(400)
    const okBtn = merchantPage.locator('.el-message-box__btns button:has-text("确定"), .el-message-box__btns .el-button--primary').first()
    if (await okBtn.count() > 0) {
      await okBtn.click()
      await merchantPage.waitForTimeout(1000)
      console.log('已确认回收')
    } else {
      console.log('未找到确认弹窗')
    }
  } else {
    console.log('未找到确认回收按钮')
  }
  await screenshot(merchantPage, '03-merchant-catches-after')

  // 7. 用户打开渔获回收页面查看状态
  const userContext = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  const userPage = await userContext.newPage()
  setupPageListeners(userPage)
  await setUserStorage(userPage, userData, '18800000002')
  await openAndWait(userPage, `${BASE}/user/catches`, '.page-header, .catch-list, .el-empty')
  await screenshot(userPage, '04-user-catches')

  // 8. 用户进入商城下单
  await openAndWait(userPage, `${BASE}/shop`, '.product-card, .shop-home')
  const productsRes = await apiGet(userData.token, '/api/shop/products')
  const products = (productsRes.data?.records || []).filter(p => p.stock > 0 && p.status === 'on')
  if (products.length === 0) throw new Error('没有可购买商品')
  const product = products[0]
  console.log('购买商品:', product.id, product.name, '库存:', product.stock)

  await openAndWait(userPage, `${BASE}/shop/checkout?productId=${product.id}&quantity=1`, '.summary-panel')
  await screenshot(userPage, '05-shop-checkout')

  // 点击提交订单
  const submitBtn = userPage.locator('.submit-btn')
  if (await submitBtn.count() > 0 && await submitBtn.isEnabled()) {
    await submitBtn.click()
    await userPage.waitForTimeout(1500)
    console.log('已提交订单')
  } else {
    console.log('提交按钮不可用')
  }
  await screenshot(userPage, '06-shop-orders')

  // 9. 商家查看商城订单
  await openAndWait(merchantPage, `${BASE}/merchant/shop/orders`, '.page-header')
  await screenshot(merchantPage, '07-merchant-shop-orders')

  // 10. 商家查看商品管理
  await openAndWait(merchantPage, `${BASE}/merchant/shop/products`, '.page-header')
  await screenshot(merchantPage, '08-merchant-shop-products')

  // 11. 商家首页/工作台
  await openAndWait(merchantPage, `${BASE}/merchant/dashboard`, '.page-header, .dashboard')
  await screenshot(merchantPage, '09-merchant-dashboard')

  console.log('\n===== 流程验证完成 =====')
  console.log('页面错误:', pageErrors)
  console.log('HTTP 错误数:', networkErrors.length)
  console.log('截图目录:', SCREEN_DIR)

  await browser.close()
}

main().catch(err => {
  console.error('流程验证失败:', err)
  process.exit(1)
})
