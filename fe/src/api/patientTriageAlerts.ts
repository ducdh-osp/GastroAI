import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

export interface TriageConversationMessage {
  id: number
  senderLabel: string
  mine: boolean
  content: string
  createdAt: string
}

export interface TriageConversation {
  id: number
  status: 'NEW' | 'IN_PROGRESS' | 'RESOLVED'
  occurredAt: string
  messages: TriageConversationMessage[]
  canReply: boolean
}

export interface TriageConversationSummary {
  id: number
  occurredAt: string
  status: TriageConversation['status']
  lastMessagePreview: string | null
  lastMessageAt: string
  unreadCount: number
}

/** Các cuộc trao đổi mà bác sĩ/nhân viên đã nhắn, tin mới nhất lên đầu. */
export async function listTriageConversations(): Promise<TriageConversationSummary[]> {
  try {
    const { data } = await apiClient.get<TriageConversationSummary[]>('/patient/triage-alerts')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách trao đổi. Vui lòng thử lại.')
  }
}

/** Mở trang trao đổi - BE đồng thời đánh dấu đã xem tin của bác sĩ. */
export async function getTriageConversation(alertId: number): Promise<TriageConversation> {
  try {
    const { data } = await apiClient.get<TriageConversation>(`/patient/triage-alerts/${alertId}`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải cuộc trao đổi. Vui lòng thử lại.')
  }
}

export async function replyTriageConversation(alertId: number, content: string): Promise<TriageConversationMessage> {
  try {
    const { data } = await apiClient.post<TriageConversationMessage>(
      `/patient/triage-alerts/${alertId}/messages`, { content })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể gửi tin nhắn. Vui lòng thử lại.')
  }
}
