<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDrawResults, getMissedDrawList, exportDrawResults, type DrawResultVO, type MissedDrawVO, type DrawQuery } from '@/api/draw'
import { getSlots, type TimeSlot } from '@/api/timeslot'
import { formatDate, formatDateTime } from '@/utils/date'

const list = ref<DrawResultVO[]>([])
const slots = ref<TimeSlot[]>([])
const loading = ref(false)
const missedLoading = ref(false)
const missedVisible = ref(false)
const missedList = ref<MissedDrawVO[]>([])
const currentSlotName = ref('')
const total = ref(0)
const query = reactive<DrawQuery>({
  slotId: undefined,
  pageNum: 1,
  pageSize: 20
})
const queryDate = ref('')

const loadSlots = async () => {
  const res = await getSlots({ pageSize: 1000 })
  slots.value = res.data.records
}

const load = async () => {
  loading.value = true
  const params: DrawQuery = {
    slotId: query.slotId,
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
  // 如果选择了日期，则通过 slots 查找对应时段ID集合
  if (queryDate.value) {
    const matched = slots.value.filter(s => s.slotDate === queryDate.value)
    if (matched.length > 0) {
      params.slotId = matched[0].id
    }
  }
  const res = await getDrawResults(params)
  list.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

const openMissed = async (slot?: TimeSlot) => {
  const slotId = slot ? slot.id : query.slotId
  if (!slotId) {
    ElMessage.warning('请先选择一个时段')
    return
  }
  missedVisible.value = true
  missedLoading.value = true
  currentSlotName.value = slot ? `${slot.slotDate} ${slot.slotName}` : '选中时段'
  try {
    const res = await getMissedDrawList(slotId)
    missedList.value = res.data
  } finally {
    missedLoading.value = false
  }
}

const exportExcel = () => {
  const params = new URLSearchParams()
  if (query.slotId) params.append('slotId', String(query.slotId))
  window.open(`/api/admin/draw-results/export?${params.toString()}`)
  ElMessage.success('开始导出')
}

onMounted(() => {
  loadSlots().then(load)
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>抽号记录</h2>
      <div>
        <el-button type="warning" icon="Warning" @click="openMissed()">未参与抽号名单</el-button>
        <el-button type="success" icon="Download" @click="exportExcel">导出 Excel</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="日期">
          <el-date-picker
            v-model="queryDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 160px"
            @change="query.pageNum = 1; load()"
          />
        </el-form-item>
        <el-form-item label="时段">
          <el-select
            v-model="query.slotId"
            placeholder="全部"
            clearable
            style="width: 220px"
            @change="query.pageNum = 1; load()"
          >
            <el-option
              v-for="slot in slots"
              :key="slot.id"
              :label="`${slot.slotDate} ${slot.slotName}`"
              :value="slot.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="query.pageNum = 1; load()">查询</el-button>
          <el-button icon="Refresh" @click="queryDate = ''; query.slotId = undefined; query.pageNum = 1; load()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column label="场次日期" width="120">
          <template #default="{ row }">{{ formatDate(row.slotDate) }}</template>
        </el-table-column>
        <el-table-column label="时段" width="110">
          <template #default="{ row }">{{ row.slotName }}</template>
        </el-table-column>
        <el-table-column label="用户名" width="130">
          <template #default="{ row }">{{ row.userNickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">{{ row.userPhone || '-' }}</template>
        </el-table-column>
        <el-table-column label="钓位号" width="110">
          <template #default="{ row }">
            <span class="spot-code">{{ row.spotCode }}</span>
          </template>
        </el-table-column>
        <el-table-column label="抽号时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.drawTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" @click="openMissed(slots.find(s => s.id === row.slotId))">未参与名单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load"
      />
    </el-card>

    <!-- 未参与抽号名单弹窗 -->
    <el-dialog v-model="missedVisible" :title="`未参与抽号名单 - ${currentSlotName}`" width="700px">
      <el-table v-loading="missedLoading" :data="missedList" stripe>
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column label="用户名" width="140">
          <template #default="{ row }">{{ row.userNickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">{{ row.userPhone || '-' }}</template>
        </el-table-column>
        <el-table-column label="预约ID" min-width="120">
          <template #default="{ row }">{{ row.reservationId }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!missedLoading && missedList.length === 0" description="该时段暂无未参与抽号的用户" />
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding-bottom: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  color: #0f4c75;
}

.filter-card {
  margin-bottom: 16px;
}

.spot-code {
  font-weight: 700;
  color: #f9a825;
}
</style>
