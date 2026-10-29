import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export type AuditAction =
    | 'DOCUMENT_UPLOAD'
    | 'DOCUMENT_NEW_VERSION'
    | 'DOCUMENT_ACTIVATE_VERSION'
    | 'DOCUMENT_UPDATE_INFO'
    | 'DOCUMENT_PUBLISH'
    | 'DOCUMENT_UNPUBLISH'
    | 'DOCUMENT_DELETE_VERSION'
    | 'DOCUMENT_DELETE'
    | 'RBAC_ROLE_CHANGE'
    | 'PATIENT_LOCK'
    | 'PATIENT_UNLOCK'
    | 'PATIENT_DATA_VIEW'
    | 'PASSWORD_CHANGE'
    | 'SYSTEM_RESTORE'
    | 'ANOMALY_DETECTED'
    | 'ANOMALY_RULE_UPDATE'

export type AuditTargetType = 'DOCUMENT' | 'PATIENT' | 'ADMIN' | 'DOCTOR' | 'SYSTEM' | 'RULE'

export interface AuditLog {
    id: number
    occurredAt: string
    actorType: string
    actorId: number | null
    actorEmail: string | null
    action: AuditAction
    targetType: string | null
    targetId: string | null
    detail: string | null
    ipAddress: string
    userAgent: string
}

export interface AuditLogPage {
    items: AuditLog[]
    page: number
    size: number
    totalElements: number
    totalPages: number
}

export interface AuditLogFilter {
    from?: string
    to?: string
    action?: AuditAction
    targetType?: AuditTargetType
    q?: string
}

export async function listAuditLogs(filter: AuditLogFilter & { page?: number; size?: number }) {
    try {
        const { data } = await cmsApiClient.get<AuditLogPage>('/cms/audit-logs', { params: filter })
        return data
    } catch (error) {
        throw extractErrorMessage(error, 'Không thể tải nhật ký hoạt động.')
    }
}

export type AnomalyMetric = 'ACTIONS' | 'DISTINCT_TARGETS' | 'DISTINCT_IPS'

export interface AuditAnomalyRule {
  code: string
  name: string
  actions: AuditAction[]
  metric: AnomalyMetric
  threshold: number
  windowMinutes: number
  enabled: boolean
  updatedAt: string
  updatedBy: string | null
}

export interface UpdateAuditAnomalyRulePayload {
  threshold: number
  windowMinutes: number
  enabled: boolean
}

export async function listAnomalyRules() {
  try {
    const { data } = await cmsApiClient.get<AuditAnomalyRule[]>('/cms/audit-logs/rules')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách quy tắc cảnh báo.')
  }
}

export async function updateAnomalyRule(code: string, payload: UpdateAuditAnomalyRulePayload) {
  try {
    const { data } = await cmsApiClient.put<AuditAnomalyRule>(`/cms/audit-logs/rules/${code}`, payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể lưu quy tắc cảnh báo.')
  }
}