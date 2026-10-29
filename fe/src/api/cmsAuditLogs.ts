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
    | 'AUDIT_LOG_EXPORT'

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

export type AuditExportFormat = 'CSV' | 'EXCEL'

async function readBlobErrorMessage(error: unknown, fallback: string): Promise<Error> {
  const data = (error as { response?: { data?: unknown } })?.response?.data
  if (data instanceof Blob) {
    try {
      const body = JSON.parse(await data.text()) as { message?: string }
      if (body.message) return new Error(body.message)
    } catch {
      // Body khong phai JSON -> dung cau mac dinh ben duoi.
    }
  }
  return extractErrorMessage(error, fallback)
}

function fileNameFromHeader(header: string | undefined, fallback: string) {
  const match = header?.match(/filename="?([^";]+)"?/i)
  return match ? match[1] : fallback
}

export async function exportAuditLogs(filter: AuditLogFilter, format: AuditExportFormat) {
  try {
    const response = await cmsApiClient.get<Blob>('/cms/audit-logs/export', {
      params: { ...filter, format },
      responseType: 'blob',
      timeout: 60000,
    })

    const fallbackName = `GastroAI-AuditLog.${format === 'CSV' ? 'csv' : 'xlsx'}`
    const filename = fileNameFromHeader(response.headers['content-disposition'], fallbackName)

    const objectUrl = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
  } catch (error) {
    throw await readBlobErrorMessage(error, 'Không thể xuất nhật ký hoạt động.')
  }
}