import { apiClient } from '../lib/axios'

export type SenderType = 'patient' | 'assistant' | 'system'

export type MessageStatus = 'sending' | 'sent' | 'failed' | 'replying'

export type RatingValue = 'HELPFUL' | 'UNHELPFUL'

export interface Attachment {
  id: string
  name: string
  type: string
  size: number
  url: string
  previewUrl?: string
}

export interface SourceRef {
  documentTitle: string
  snippet: string
  sourceUrl?: string | null
}

export interface Message {
  id: string
  /** DB-side message id — dùng để gọi API rating. null nếu là tin nhắn tạm thời chưa lưu DB. */
  dbMessageId?: number | null
  sender: SenderType
  content: string
  createdAt: string
  status: MessageStatus
  attachments?: Attachment[]
  sources?: SourceRef[]
  relatedQuestions?: string[]
  emergency?: boolean
  matchedGroups?: string[]
  /** Đánh giá hiện tại của người dùng cho tin nhắn AI này. */
  rating?: RatingValue | null
}

export interface SendMessageRequest {
  content: string
  attachments?: Attachment[]
  isRetry?: boolean
  /** sessionId để tiếp tục phiên hiện tại. null = bắt đầu phiên mới. */
  sessionId?: number | null
}

export interface ChatService {
  sendMessage: (request: SendMessageRequest) => Promise<Message & { sessionId?: number | null }>
}

export interface ChatSessionSummary {
  id: number
  title: string
  createdAt: string
  updatedAt: string
}

export interface ChatSessionListResponse {
  sessions: ChatSessionSummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ChatMessageDetail {
  id: number
  sender: SenderType
  content: string
  createdAt: string
  emergency: boolean
  sources: SourceRef[]
  relatedQuestions: string[]
  matchedGroups: string[]
  rating: RatingValue | null
}

const TRIAGE_GROUP_LABELS: Record<string, string> = {
  XUAT_HUYET_TIEU_HOA: 'nghi xuất huyết tiêu hóa',
  DAU_BUNG_CAP_TINH: 'đau bụng dữ dội, cấp tính',
  TAC_RUOT: 'nghi tắc ruột',
  SOC_MAT_MAU: 'dấu hiệu sốc/mất máu',
  NHIEM_TRUNG_NANG: 'nghi nhiễm trùng nặng/viêm ruột thừa',
  DAU_LAN_CO_QUAN_KHAC: 'đau lan sang vùng khác (lưng/vai/ngực)',
  KEM_HO_HAP_TIM_MACH: 'kèm khó thở/đau ngực',
  TIEU_DUONG_KEM_NON: 'tiểu đường kèm nôn nhiều',
  VANG_DA_KEM_DAU_BUNG: 'vàng da kèm đau bụng',
  MAT_NUOC_NANG: 'mất nước nặng do tiêu chảy kéo dài',
}

/** Chuỗi mô tả các nhóm dấu hiệu khẩn cấp đã khớp (dùng trong banner cảnh báo), null nếu rỗng/không có. */
export function describeMatchedGroups(matchedGroups?: string[]): string | null {
  if (!matchedGroups || matchedGroups.length === 0) return null
  return matchedGroups.map((group) => TRIAGE_GROUP_LABELS[group] ?? group).join(', ')
}

export const QUICK_PROMPTS = [
  'Tư vấn triệu chứng sốt',
  'Hỏi lịch uống thuốc',
  'Đau bụng nên làm gì?',
  'Chế độ ăn cho người đau dạ dày',
] as const

async function sendMessage(request: SendMessageRequest): Promise<Message & { sessionId?: number | null }> {
  try {
    const { data } = await apiClient.post<Message & { sessionId?: number | null }>('/chat/messages', {
      content: request.content,
      sessionId: request.sessionId ?? null,
    })
    return data
  } catch (error) {
    const message =
      (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
      'Không thể kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.'
    throw new Error(message)
  }
}

/** Gọi thật BE (UC0017): BE gọi RagQueryService.answer() (Gemini/RAG thật), không còn là mock. */
export const chatService: ChatService = { sendMessage }

// ─────────────────────────── Chat History API ────────────────────────────────

/** Danh sách phiên chat của bệnh nhân đang đăng nhập, mới nhất trước. */
export async function listChatSessions(page = 0, size = 20): Promise<ChatSessionListResponse> {
  const { data } = await apiClient.get<ChatSessionListResponse>('/chat/sessions', {
    params: { page, size },
  })
  return data
}

/** Toàn bộ tin nhắn trong 1 phiên chat. */
export async function getChatSessionMessages(sessionId: number): Promise<ChatMessageDetail[]> {
  const { data } = await apiClient.get<ChatMessageDetail[]>(`/chat/sessions/${sessionId}`)
  return data
}

/** Xóa một phiên chat cùng toàn bộ tin nhắn và đánh giá liên quan. */
export async function deleteChatSession(sessionId: number): Promise<void> {
  await apiClient.delete(`/chat/sessions/${sessionId}`)
}

/** Đánh giá câu trả lời AI (UPSERT — có thể đổi ý). */
export async function rateMessage(messageId: number, rating: RatingValue): Promise<void> {
  await apiClient.post(`/chat/messages/${messageId}/rating`, { rating })
}
