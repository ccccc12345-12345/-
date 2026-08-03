import type { BoardSpot, StaffRole } from './types'

export const roleLabels: Record<StaffRole, string> = {
  owner: '老板',
  manager: '店长',
  operator: '运营员',
  finance: '财务员',
  checker: '核销员'
}

const toLocalDateInput = (date: Date) => {
  const copy = new Date(date)
  copy.setMinutes(copy.getMinutes() - copy.getTimezoneOffset())
  return copy.toISOString().slice(0, 10)
}

export const todayString = () => toLocalDateInput(new Date())

export const addDaysString = (days: number) => {
  const date = new Date()
  date.setDate(date.getDate() + days)
  return toLocalDateInput(date)
}

export const tomorrowString = () => addDaysString(1)

export const nowDateTimeLocal = () => {
  const date = new Date()
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
  return date.toISOString().slice(0, 16)
}

export const formatDateTime = (value?: string | null) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

export const formatDate = (value?: string | null) => {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

export const formatTime = (value?: string | null) => {
  if (!value) return '-'
  return String(value).slice(0, 5)
}

export const toTimeSeconds = (value?: string | null) => {
  if (!value) return '00:00:00'
  const text = String(value)
  if (text.length === 5) return `${text}:00`
  return text.slice(0, 8)
}

export const toServerDateTime = (value?: string | null) => {
  if (!value) return ''
  const text = String(value).replace('T', ' ')
  if (text.length === 16) return `${text}:00`
  return text.slice(0, 19)
}

export const toDateTimeLocal = (value?: string | null) => {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 16)
}

export const formatCent = (value?: number | string | null) => {
  const num = Number(value || 0)
  return `¥${(num / 100).toFixed(2)}`
}

export const formatYuan = (value?: number | string | null) => {
  const num = Number(value || 0)
  return `¥${num.toFixed(2)}`
}

export const parseMoneyToCent = (value: string | number | undefined | null) => Math.round(Number(value || 0) * 100)

export const statusText = (status?: string | number | null) => {
  const map: Record<string, string> = {
    pending: '待处理',
    cooking: '制作中',
    delivered: '已送达',
    completed: '已完成',
    cancelled: '已取消',
    published: '已发布',
    draft: '草稿',
    closed: '已结束',
    on: '上架',
    off: '下架',
    normal: '正常',
    disabled: '禁用',
    approved: '已通过',
    rejected: '已拒绝',
    待抽号: '待抽号',
    已抽号: '已抽号',
    已核销: '已核销',
    预约取消: '预约取消',
    过期失效: '过期失效'
  }
  return map[String(status ?? '')] || String(status ?? '-')
}

export const pondCategoryText = (category?: string | null) => {
  const map: Record<string, string> = {
    lake: '湖库',
    river: '江河',
    pond: '鱼塘',
    sea: '海区'
  }
  return map[category || ''] || category || '-'
}

export const spotStatusText = (status?: number | null) => {
  if (status === 1) return '可用'
  if (status === 2) return '禁用'
  if (status === 0) return '维修'
  return '未知'
}

export const boardStatusInfo = (spot: Pick<BoardSpot, 'status' | 'spotStatus'>) => {
  const label = `${spot.status || ''}`
  if (spot.spotStatus === 2 || label.includes('禁用')) {
    return { label: '禁用', className: 'bg-slate-400 text-white ring-slate-200' }
  }
  if (spot.spotStatus === 0 || label.includes('维修')) {
    return { label: '维修', className: 'bg-red-500 text-white ring-red-200' }
  }
  if (label.includes('核销') || label.includes('使用') || label.toLowerCase().includes('using')) {
    return { label: '使用中', className: 'bg-blue-500 text-white ring-blue-200' }
  }
  if (label.includes('预约') || label.toLowerCase().includes('reserved')) {
    return { label: '已预约', className: 'bg-amber-400 text-slate-950 ring-amber-200' }
  }
  return { label: '空闲', className: 'bg-emerald-500 text-white ring-emerald-200' }
}

export const imageFromFile = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })

export const exportCsv = (filename: string, rows: Array<Record<string, unknown>>) => {
  const headers = Object.keys(rows[0] || {})
  const lines = [
    headers.join(','),
    ...rows.map((row) =>
      headers
        .map((key) => {
          const value = String(row[key] ?? '').replace(/"/g, '""')
          return `"${value}"`
        })
        .join(',')
    )
  ]
  const blob = new Blob([`\ufeff${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
