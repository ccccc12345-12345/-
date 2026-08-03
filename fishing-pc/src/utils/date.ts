export const formatDate = (date: Date | string) => {
  if (!date) return '-'
  const d = typeof date === 'string' ? new Date(date) : date
  if (isNaN(d.getTime())) return '-'
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export const formatTime = (time: string) => {
  if (!time) return '-'
  return time.substring(0, 5)
}

export const formatDateTime = (datetime: string | Date) => {
  if (!datetime) return '-'
  const d = typeof datetime === 'string' ? new Date(datetime) : datetime
  if (isNaN(d.getTime())) return '-'
  return `${formatDate(d)} ${formatTime(d.toTimeString())}`
}

export const countdownText = (targetTime: string | Date) => {
  const now = new Date().getTime()
  const target = typeof targetTime === 'string' ? new Date(targetTime).getTime() : targetTime.getTime()
  const diff = target - now
  if (diff <= 0) return '已开始'
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}
