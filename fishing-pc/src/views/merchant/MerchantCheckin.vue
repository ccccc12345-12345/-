<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { merchantCheckin, queryMerchantCheckin, type CheckinResult } from '@/api/merchant'

const route = useRoute()
const code = ref('')
const actualFee = ref<number | undefined>(undefined)
const loading = ref(false)
const checkingIn = ref(false)
const detail = ref<CheckinResult | null>(null)
const scannerVisible = ref(false)
const scannerError = ref('')
const scannerReady = ref(false)

let Html5QrcodeCtor: any = null
let html5QrCode: any = null
const scannerId = 'merchant-checkin-qr-reader'

const handleQuery = async () => {
  const c = code.value.trim()
  if (!c || c.length !== 6 || !/^\d{6}$/.test(c)) {
    ElMessage.warning('请输入6位数字核销码')
    return
  }
  loading.value = true
  try {
    const res = await queryMerchantCheckin(c)
    detail.value = res.data
    actualFee.value = res.data.actualFee ?? undefined
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

const handleCheckin = async () => {
  const c = code.value.trim()
  if (!c) return
  try {
    await ElMessageBox.confirm('确认将该预约标记为已到场？', '二次确认', { type: 'warning' })
    checkingIn.value = true
    const res = await merchantCheckin({ checkinCode: c, actualFee: actualFee.value })
    detail.value = res.data
    ElMessage.success('核销成功')
    setTimeout(() => {
      code.value = ''
      detail.value = null
      actualFee.value = undefined
    }, 3000)
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '核销失败')
    }
  } finally {
    checkingIn.value = false
  }
}

const initScannerModule = async () => {
  try {
    const mod = await import('html5-qrcode')
    Html5QrcodeCtor = mod.Html5Qrcode
  } catch (e: any) {
    scannerError.value = '扫码组件加载失败'
  }
}

const startScan = async () => {
  scannerError.value = ''
  scannerVisible.value = true
  if (!Html5QrcodeCtor) {
    await initScannerModule()
  }
  if (!Html5QrcodeCtor) {
    scannerError.value = '扫码组件不可用'
    return
  }
  setTimeout(async () => {
    html5QrCode = new Html5QrcodeCtor(scannerId)
    try {
      await html5QrCode.start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: { width: 250, height: 250 } },
        (decodedText: string) => {
          const c = decodedText.trim()
          if (/^\d{6}$/.test(c)) {
            code.value = c
            ElMessage.success('扫码成功')
            stopScan()
            handleQuery()
          } else {
            ElMessage.warning('二维码内容不是有效的6位核销码')
          }
        },
        () => {}
      )
      scannerReady.value = true
    } catch (e: any) {
      scannerReady.value = false
      scannerError.value = '无法调用摄像头，请检查权限设置'
      ElMessage.error('无法调用摄像头，请检查权限设置')
    }
  }, 100)
}

const stopScan = async () => {
  if (html5QrCode) {
    try {
      await html5QrCode.stop()
      await html5QrCode.clear()
    } catch {}
    html5QrCode = null
  }
  scannerVisible.value = false
  scannerReady.value = false
}

onMounted(() => {
  const queryCode = String(route.query.code || '')
  if (/^\d{6}$/.test(queryCode)) {
    code.value = queryCode
    handleQuery()
  }
})

onUnmounted(() => {
  stopScan()
})
</script>

<template>
  <div class="checkin-page">
    <div class="checkin-card">
      <h2 class="title">扫码核销</h2>

      <div class="input-wrap">
        <label class="input-label">核销码</label>
        <el-input
          v-model="code"
          placeholder="请输入6位数字核销码"
          maxlength="6"
          class="code-input"
          @keyup.enter="handleQuery"
        />
      </div>

      <el-button
        type="primary"
        size="large"
        class="action-btn query-btn"
        :loading="loading"
        @click="handleQuery"
      >
        查询
      </el-button>

      <el-button
        type="success"
        size="large"
        class="action-btn scan-btn"
        @click="startScan"
      >
        扫码核销
      </el-button>

      <div v-if="detail" class="detail-card">
        <div class="detail-row">
          <span class="detail-label">用户昵称</span>
          <span class="detail-value">{{ detail.userNickname || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">鱼塘</span>
          <span class="detail-value">{{ detail.pondName || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">日期/时段</span>
          <span class="detail-value">{{ detail.slotDate || '-' }} {{ detail.slotName || '' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">钓位号</span>
          <span class="detail-value spot">{{ detail.spotCode || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">预约状态</span>
          <span class="detail-value">{{ detail.status || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">实收金额</span>
          <el-input-number
            v-model="actualFee"
            :min="0"
            :precision="2"
            controls-position="right"
            class="fee-input"
            placeholder="请输入金额"
          />
        </div>

        <el-button
          type="danger"
          size="large"
          class="action-btn checkin-btn"
          :loading="checkingIn"
          @click="handleCheckin"
        >
          确认到场
        </el-button>
      </div>
    </div>

    <el-dialog v-model="scannerVisible" title="扫描二维码" width="92%" :before-close="stopScan" align-center>
      <div :id="scannerId" class="qr-reader" />
      <p v-if="scannerError" class="scan-error">{{ scannerError }}</p>
      <p v-else class="scan-tip">将用户展示的二维码放入框内</p>
    </el-dialog>
  </div>
</template>

<style scoped>
.checkin-page {
  min-height: 100%;
  background:
    radial-gradient(circle at 15% 20%, rgba(31, 106, 88, 0.08) 0%, transparent 22%),
    radial-gradient(circle at 85% 80%, rgba(87, 160, 120, 0.06) 0%, transparent 24%),
    linear-gradient(180deg, #e8f2ec 0%, #dfece4 100%);
  padding: 16px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}
.checkin-card {
  width: 100%;
  max-width: 520px;
  background: white;
  border-radius: 16px;
  padding: 28px 20px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}
.title {
  text-align: center;
  color: #1f6a58;
  font-size: 24px;
  margin-bottom: 24px;
}
.input-wrap { margin-bottom: 20px; }
.input-label { display: block; font-size: 16px; color: #606266; margin-bottom: 8px; }
.code-input :deep(.el-input__wrapper) { padding: 8px 12px; height: 64px; }
.code-input :deep(.el-input__inner) { font-size: 26px; text-align: center; letter-spacing: 6px; }
.action-btn { width: 100%; height: 54px; font-size: 18px; border-radius: 10px; margin: 0 0 16px 0; }
.query-btn { background: #0f4c75; }
.scan-btn { background: #67c23a; }
.detail-card { margin-top: 8px; padding: 16px; background: #f0f9ff; border-radius: 12px; }
.detail-row { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; border-bottom: 1px dashed #dbeafe; font-size: 16px; }
.detail-row:last-of-type { border-bottom: none; }
.detail-label { color: #606266; }
.detail-value { color: #303133; font-weight: 600; }
.detail-value.spot { color: #f56c6c; font-size: 22px; }
.fee-input { width: 140px; }
.checkin-btn { margin-top: 16px; background: #f56c6c; }
.qr-reader { width: 100%; min-height: 300px; }
.scan-tip { text-align: center; color: #909399; font-size: 14px; margin-top: 12px; }
.scan-error { text-align: center; color: #f56c6c; font-size: 14px; margin-top: 12px; }
@media (max-width: 768px) {
  .checkin-page { padding: 12px; }
  .checkin-card { padding: 22px 16px; border-radius: 14px; }
  .title { font-size: 22px; }
  .code-input :deep(.el-input__wrapper) { height: 70px; }
  .code-input :deep(.el-input__inner) { font-size: 30px !important; }
  .action-btn { height: 58px; font-size: 20px; margin-bottom: 14px; }
  .detail-row { font-size: 17px; padding: 14px 0; }
  .detail-value.spot { font-size: 24px; }
}
</style>
