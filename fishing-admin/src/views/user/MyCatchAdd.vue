<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createCatchRecord, getPublicPonds, getPublicSpots, type PublicPond, type PublicSpot } from '@/api/catch'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const ponds = ref<PublicPond[]>([])
const spots = ref<PublicSpot[]>([])
const spotsLoading = ref(false)

const form = reactive({
  pondId: null as number | null,
  spotId: null as number | null,
  reservationId: null as number | null,
  fishType: '',
  weight: null as number | null,
  quantity: null as number | null,
  imageUrl: ''
})

const rules: FormRules = {
  pondId: [{ required: true, message: '请选择鱼塘', trigger: 'change' }],
  fishType: [{ required: true, message: '请输入鱼种', trigger: 'blur' }],
  weight: [
    { required: true, message: '请输入重量', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '重量必须大于0', trigger: 'blur' }
  ],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '数量必须大于0', trigger: 'blur' }
  ]
}

const loadPonds = async () => {
  try {
    const res = await getPublicPonds()
    ponds.value = (res.data || []).filter(p => p.status === 1)
  } catch (e: any) {
    ElMessage.error(e.message || '加载鱼塘失败')
  }
}

const loadSpots = async (pondId: number) => {
  spotsLoading.value = true
  try {
    const res = await getPublicSpots(pondId)
    spots.value = (res.data.records || []).filter(s => s.status === 1)
  } catch (e: any) {
    ElMessage.error(e.message || '加载钓位失败')
  } finally {
    spotsLoading.value = false
  }
}

watch(() => form.pondId, (val) => {
  form.spotId = null
  spots.value = []
  if (val != null) {
    loadSpots(val)
  }
})

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await createCatchRecord({
        pondId: form.pondId!,
        spotId: form.spotId,
        reservationId: form.reservationId,
        fishType: form.fishType.trim(),
        weight: form.weight!,
        quantity: form.quantity!,
        imageUrl: form.imageUrl.trim() || null
      })
      ElMessage.success('记录成功')
      router.push('/my-catches')
    } catch (e: any) {
      ElMessage.error(e.message || '提交失败')
    } finally {
      loading.value = false
    }
  })
}

const cancel = () => {
  router.back()
}

onMounted(loadPonds)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>记录渔获</h2>
    </div>

    <el-card v-loading="loading" style="max-width: 720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="鱼塘" prop="pondId">
          <el-select v-model="form.pondId" placeholder="选择鱼塘" clearable style="width: 100%">
            <el-option
              v-for="pond in ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="钓位" prop="spotId">
          <el-select
            v-model="form.spotId"
            placeholder="请先选择鱼塘"
            clearable
            :disabled="!form.pondId || spotsLoading"
            :loading="spotsLoading"
            style="width: 100%"
          >
            <el-option
              v-for="spot in spots"
              :key="spot.id"
              :label="spot.spotCode"
              :value="spot.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="鱼种" prop="fishType">
          <el-input v-model="form.fishType" placeholder="例如：草鱼" clearable />
        </el-form-item>

        <el-form-item label="重量" prop="weight">
          <el-input-number v-model="form.weight" :min="0.01" :precision="2" placeholder="千克" style="width: 100%" />
        </el-form-item>

        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" :precision="0" placeholder="条" style="width: 100%" />
        </el-form-item>

        <el-form-item label="照片 URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="https://example.com/image.jpg" clearable />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submit">提交</el-button>
          <el-button @click="cancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  padding-bottom: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  color: #0f4c75;
  margin: 0;
}
</style>
