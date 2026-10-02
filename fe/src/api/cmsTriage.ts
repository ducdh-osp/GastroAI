import { cmsApiClient } from '../lib/cmsAxios'

export type TriageAlertStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED'


export interface TriageAlert {
  id: number
  patientId: number
  patientFullName: string | null
  patientPhone: string | null
  sessionId: number | null
  messageId: number | null
  messageContent: string
  matchedGroups: string[]
  status: TriageAlertStatus
  claimedById: number | null
  claimedByType: 'ADMIN' | 'DOCTOR' | null
  claimedAt: string | null
  resolvedAt: string | null
  resolvedById: number | null
  resolvedByType: 'ADMIN' | 'DOCTOR' | null
  occurredAt: string
}

/** UC0036/067 - 50 cảnh báo gần nhất (NEW/IN_PROGRESS/RESOLVED), mới nhất trước. */
export async function listTriageAlerts(): Promise<TriageAlert[]> {
  const { data } = await cmsApiClient.get<TriageAlert[]>('/cms/triage-alerts')
  return data
}

/** Admin/Bác sĩ bấm "Tiếp nhận" - 409 nếu cảnh báo đã RESOLVED (xem GlobalExceptionHandler). */
export async function claimTriageAlert(alertId: number): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/claim`)
  return data
}

/** Đánh dấu đã xử lý xong - chủ ý không bắt buộc đúng người đã tiếp nhận (ca trực có thể đổi người đóng). */
export async function resolveTriageAlert(alertId: number): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/resolve`)
  return data
}

/** Rút message lỗi thống nhất {message} từ response axios, dùng chung cho 2 action trên. */
export function extractTriageErrorMessage(error: unknown, fallback: string): string {
  return (
    (error as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
  )
}