<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { drawSpot } from '@/api/draw'

const route = useRoute()
const router = useRouter()

const reservationId = Number(route.params.id)
const rollingCode = ref('??')
const finalCode = ref('')
const isRolling = ref(true)
let timer: number | null = null

const codes = ['A01', 'A02', 'A03', 'B01', 'B02', 'B03', 'C01', 'C02', 'C03']

onMounted(async () => {
  timer = window.setInterval(() => {
    rollingCode.value = codes[Math.floor(Math.random() * codes.length)]
  }, 100)

  try {
    const res = await drawSpot(reservationId)
    finalCode.value = res.data
  } catch (e: any) {
    ElMessage.error(e.message || '抽号失败')
    router.replace('/reservations')
    return
  }

  setTimeout(() => {
    if (timer) clearInterval(timer)
    rollingCode.value = finalCode.value
    isRolling.value = false
  }, 1500)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="draw-page">
    <div class="draw-card">
      <h2>正在抽号</h2>
      <div class="spot-display" :class="{ rolling: isRolling }">
        {{ rollingCode }}
      </div>
      <p v-if="isRolling" class="hint">正在分配最佳钓位...</p>
      <div v-else class="result-section">
        <p class="success-hint">恭喜！您已获得钓位</p>
        <p class="tip">请截图保存，作为到场凭证</p>
        <button class="btn-primary back-btn" @click="$router.push('/reservations')">
          查看我的预约
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.draw-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #1a5f7a 0%, #57cc99 100%);
  padding: 20px;
}

.draw-card {
  width: 100%;
  background: white;
  border-radius: 24px;
  padding: 40px 24px;
  text-align: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.draw-card h2 {
  font-size: 24px;
  color: var(--primary);
  margin-bottom: 32px;
}

.spot-display {
  width: 160px;
  height: 160px;
  margin: 0 auto 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), #fda085);
  color: white;
  font-size: 56px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(246, 211, 101, 0.35);
  transition: transform 0.1s;
}

.spot-display.rolling {
  animation: shake 0.3s infinite;
}

@keyframes shake {
  0%, 100% { transform: rotate(-2deg); }
  50% { transform: rotate(2deg); }
}

.hint {
  color: var(--text-secondary);
  font-size: 14px;
}

.success-hint {
  font-size: 18px;
  color: var(--secondary);
  font-weight: 700;
  margin-bottom: 8px;
}

.tip {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.back-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
}
</style>
