import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

/** SCR-AD-13: Hệ thống đang tự xử lý → Cần can thiệp → Đã xử lý. */
export type TriageAlertStatus = 'AUTO_HANDLING' | 'NEEDS_INTERVENTION' | 'RESOLVED'
export type TriageAlertOutcome = 'HANDLED' | 'FALSE_ALARM' | 'PATIENT_SAFE'
/** 3 tab SCR-AD-13; OPEN = 2 trạng thái còn mở (dùng cho đếm ở menu). */
export type TriageAlertView = 'INTERVENTION' | 'AUTO' | 'CLOSED' | 'OPEN' | 'ALL'
export type TriageStaffType = 'ADMIN' | 'DOCTOR'
/** Ai làm 1 bước trên timeline: nhân sự, bệnh nhân hoặc hệ thống (bộ hẹn giờ). */
export type TriageActorType = TriageStaffType | 'PATIENT' | 'SYSTEM'
export type TriagePatientResponse = 'SAFE' | 'NEED_HELP'

/** Kết quả liên hệ Admin ghi lại ở bước "Cần can thiệp" - nhãn khớp BE (TriageContactResult). */
export type TriageContactResult =
  | 'RELATIVE_REACHED'
  | 'RELATIVE_NO_ANSWER'
  | 'RELATIVE_CONFIRMED_SAFE'
  | 'PATIENT_REACHED'
  | 'CALLED_EMERGENCY'

export const CONTACT_RESULT_LABEL: Record<TriageContactResult, string> = {
  RELATIVE_REACHED: 'Đã gọi người thân - người thân sẽ kiểm tra',
  RELATIVE_NO_ANSWER: 'Gọi người thân không nghe máy',
  RELATIVE_CONFIRMED_SAFE: 'Người thân xác nhận bệnh nhân an toàn',
  PATIENT_REACHED: 'Đã gọi được cho bệnh nhân',
  CALLED_EMERGENCY: 'Đã gọi 115',
}

export type TriageAlertActionType =
  | 'CREATED'
  | 'CHECK_IN_SENT'
  | 'PATIENT_SAFE'
  | 'PATIENT_NEEDS_HELP'
  | 'ESCALATED'
  | 'FAILSAFE'
  | 'CONTACT_RECORDED'
  | 'REPEATED'
  | 'REPEATED_AFTER_FALSE_ALARM'
  | 'CLAIMED'
  | 'RELEASED'
  | 'MESSAGE_SENT'
  | 'PATIENT_REPLIED'
  | 'PATIENT_READ'
  | 'RESOLVED'
  | 'FALSE_ALARM'

export interface TriageAlert {
  id: number
  patientId: number
  patientFullName: string | null
  patientPhone: string | null
  messageId: number | null
  /** Tin gốc - không bị ghi đè khi bệnh nhân gửi lặp lại. */
  messageContent: string
  matchedGroups: string[]
  status: TriageAlertStatus
  claimedById: number | null
  claimedByType: TriageStaffType | null
  claimedAt: string | null
  resolvedAt: string | null
  resolvedById: number | null
  resolvedByType: TriageActorType | null
  occurredAt: string
  outcome: TriageAlertOutcome | null
  resolutionNote: string | null
  repeatCount: number
  lastMessageContent: string | null
  lastRepeatedAt: string | null
  unreadPatientMessages: number
  staffMessageCount: number
  patientFalseAlarmCount: number
  /** Lúc chuyển "Cần can thiệp" - FE đếm ngược failsafe từ mốc này. */
  needsInterventionAt: string | null
  /** Admin đầu tiên ra tay; có giá trị thì failsafe không chạy. */
  acknowledgedAt: string | null
  /** Failsafe đã chạy (quá hạn mà chưa Admin nào xử lý). */
  failsafeAt: string | null
  patientResponse: TriagePatientResponse | null
  patientRespondedAt: string | null
}

export interface TriageAlertPage {
  items: TriageAlert[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface TriageAlertActionItem {
  id: number
  actionType: TriageAlertActionType
  actorType: TriageActorType | null
  actorId: number | null
  detail: string | null
  createdAt: string
}

export interface TriageAlertMessageItem {
  id: number
  senderType: TriageStaffType | 'PATIENT'
  senderId: number
  content: string
  readAt: string | null
  createdAt: string
}

export interface TriageAlertDetail {
  alert: TriageAlert
  actions: TriageAlertActionItem[]
  messages: TriageAlertMessageItem[]
  emergencyContact: { name: string; phone: string; relationship: string } | null
}

/** Cần can thiệp trước (ca quá hạn lên đầu), rồi đang tự xử lý; trong nhóm theo hoạt động gần nhất. */
export async function listTriageAlerts(view: TriageAlertView, page: number, size: number): Promise<TriageAlertPage> {
  const { data } = await cmsApiClient.get<TriageAlertPage>('/cms/triage-alerts', { params: { view, page, size } })
  return data
}

/** Xem chi tiết - chỉ khi người xem đang tiếp nhận, BE mới đánh dấu đã đọc tin của bệnh nhân. */
export async function getTriageAlertDetail(alertId: number): Promise<TriageAlertDetail> {
  const { data } = await cmsApiClient.get<TriageAlertDetail>(`/cms/triage-alerts/${alertId}`)
  return data
}

/**
 * "Tôi đang liên hệ" - đánh dấu để 2 Admin không cùng gọi 1 người thân. Bấm ở ca đang tự xử lý
 * = can thiệp sớm. 409 nếu đã đóng hoặc Admin khác đang liên hệ.
 */
export async function claimTriageAlert(alertId: number): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/claim`)
  return data
}

/** Bỏ "Tôi đang liên hệ" - đồng hồ failsafe tính lại từ lúc trả. */
export async function releaseTriageAlert(alertId: number): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/release`)
  return data
}

/** SCR-AD-13 bước 3 - ghi kết quả gọi người thân / bệnh nhân / 115. Ghi xong là failsafe dừng. */
export async function recordTriageContact(
  alertId: number,
  result: TriageContactResult,
  note?: string,
): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/contact`, {
    result,
    note: note || null,
  })
  return data
}

/** Đóng ca - BE trả 409 nếu chưa ghi kết quả liên hệ và chưa nhắn tin nào cho bệnh nhân. */
export async function resolveTriageAlert(alertId: number, note?: string): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/resolve`, { note: note || null })
  return data
}

export async function markTriageAlertFalseAlarm(alertId: number, reason: string): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/false-alarm`, { reason })
  return data
}

/** Rút message lỗi thống nhất {message} từ response axios. */
export function extractTriageErrorMessage(error: unknown, fallback: string): string {
  return extractErrorMessage(error, fallback).message
}
