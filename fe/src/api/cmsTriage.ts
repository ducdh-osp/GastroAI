import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export type TriageAlertStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED'
export type TriageAlertOutcome = 'HANDLED' | 'FALSE_ALARM'
export type TriageAlertView = 'OPEN' | 'CLOSED' | 'ALL'
export type TriageStaffType = 'ADMIN' | 'DOCTOR'

export type TriageAlertActionType =
  | 'CREATED'
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
  sessionId: number | null
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
  resolvedByType: TriageStaffType | null
  occurredAt: string
  outcome: TriageAlertOutcome | null
  resolutionNote: string | null
  repeatCount: number
  lastMessageContent: string | null
  lastRepeatedAt: string | null
  unreadPatientMessages: number
  staffMessageCount: number
  patientFalseAlarmCount: number
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
  actorType: TriageStaffType | 'PATIENT' | null
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
}

/** Đang mở: NEW trước, IN_PROGRESS sau; trong nhóm theo hoạt động gần nhất. */
export async function listTriageAlerts(view: TriageAlertView, page: number, size: number): Promise<TriageAlertPage> {
  const { data } = await cmsApiClient.get<TriageAlertPage>('/cms/triage-alerts', { params: { view, page, size } })
  return data
}

/** Xem chi tiết - BE đồng thời đánh dấu đã đọc tin của bệnh nhân. */
export async function getTriageAlertDetail(alertId: number): Promise<TriageAlertDetail> {
  const { data } = await cmsApiClient.get<TriageAlertDetail>(`/cms/triage-alerts/${alertId}`)
  return data
}

/** Admin/Bác sĩ bấm "Tiếp nhận" - 409 nếu cảnh báo đã RESOLVED (xem GlobalExceptionHandler). */
export async function claimTriageAlert(alertId: number): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/claim`)
  return data
}

/** Trả cảnh báo về NEW - người tiếp nhận hoặc ADMIN. */
export async function releaseTriageAlert(alertId: number): Promise<TriageAlert> {
  const { data } = await cmsApiClient.post<TriageAlert>(`/cms/triage-alerts/${alertId}/release`)
  return data
}

export async function sendTriageAlertMessage(alertId: number, content: string): Promise<TriageAlertMessageItem> {
  const { data } = await cmsApiClient.post<TriageAlertMessageItem>(
    `/cms/triage-alerts/${alertId}/messages`, { content })
  return data
}

/** Xử lý xong - BE trả 409 nếu chưa có tin nhắn nào gửi bệnh nhân. */
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
