import request from '@/utils/request'

export interface CheckinResult {
  reservationId: number
  userNickname: string | null
  slotDate: string | null
  slotName: string | null
  spotCode: string | null
  actualFee: number | null
  checkInTime: string | null
  status: string | null
  pondName: string | null
}

export const checkinByCode = (checkinCode: string) => {
  return request.post<any, { data: CheckinResult }>('/api/checkin', { checkinCode })
}

export const queryCheckinByCode = (checkinCode: string) => {
  return request.get<any, { data: CheckinResult }>('/api/checkin', { params: { checkinCode } })
}
