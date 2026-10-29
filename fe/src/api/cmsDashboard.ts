import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export interface DashboardSummary {
  totalPatients: number
  totalSessions: number
  totalMessages: number
  todayMessages: number
  totalAlerts: number
  newAlerts: number
  inProgressAlerts: number
  resolvedAlerts: number
  totalAssessments: number
  emergencyAssessments: number
  helpfulRatings: number
  unhelpfulRatings: number
  satisfactionRate: number
}

export interface DailyChatMetric {
  day: string
  totalMessages: number
  emergencyMessages: number
  patientMessages: number
  botMessages: number
}

export interface SymptomMetric {
  symptomCode: string
  symptomName: string
  totalCount: number
  emergencyCount: number
  percentage: number
}

export interface TriageGroupMetric {
  groupCode: string
  groupName: string
  count: number
  percentage: number
}

export interface TriageStatusMetric {
  status: string
  label: string
  count: number
  percentage: number
}

export interface DashboardStatsResponse {
  summary: DashboardSummary
  dailyChats: DailyChatMetric[]
  topSymptoms: SymptomMetric[]
  triageGroups: TriageGroupMetric[]
  triageStatuses: TriageStatusMetric[]
  days: number
  fromDate: string
  toDate: string
}

export async function getDashboardStats(days = 30): Promise<DashboardStatsResponse> {
  try {
    const { data } = await cmsApiClient.get<DashboardStatsResponse>('/cms/dashboard/stats', {
      params: { days },
    })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải dữ liệu thống kê dashboard.')
  }
}

export async function downloadDashboardReport(format: 'EXCEL' | 'PDF', days = 30): Promise<void> {
  try {
    const ext = format === 'PDF' ? 'pdf' : 'xlsx'
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    const filename = `GastroAI-Dashboard-Report-${days}d-${dateStr}.${ext}`

    const { data: blob } = await cmsApiClient.get<Blob>('/cms/dashboard/export', {
      params: { format, days },
      responseType: 'blob',
    })

    const objectUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể xuất báo cáo dashboard.')
  }
}
