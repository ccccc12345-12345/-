export const formatDate = (date: Date | string) => {
  const d = typeof date === 'string' ? new Date(date) : date
  if (isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export const formatTime = (time: string) => {
  if (!time) return ''
  return time.substring(0, 5)
}

export const formatDateTime = (datetime: string) => {
  if (!datetime) return ''
  const d = new Date(datetime)
  if (isNaN(d.getTime())) return ''
  return `${formatDate(d)} ${formatTime(datetime)}`
}

export const countdownText = (targetTime: string) => {
  const now = new Date().getTime()
  const target = new Date(targetTime).getTime()
  const diff = target - now
  if (diff <= 0) return '已开始'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)
  return `${hours}时${minutes}分${seconds}秒`
}
