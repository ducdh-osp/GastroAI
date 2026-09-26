import { apiClient } from '../lib/axios'

export type SenderType = 'patient' | 'assistant' | 'system'

export type MessageStatus = 'sending' | 'sent' | 'failed' | 'replying'

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
}

export interface Message {
  id: string
  sender: SenderType
  content: string
  createdAt: string
  status: MessageStatus
  attachments?: Attachment[]
  sources?: SourceRef[]
  relatedQuestions?: string[]
  emergency?: boolean
}

export interface SendMessageRequest {
  content: string
  attachments?: Attachment[]
  isRetry?: boolean
}

export interface ChatService {
  sendMessage: (request: SendMessageRequest) => Promise<Message>
}

export const QUICK_PROMPTS = [
  'Tư vấn triệu chứng sốt',
  'Hỏi lịch uống thuốc',
  'Đau bụng nên làm gì?',
  'Chế độ ăn cho người đau dạ dày',
] as const

async function sendMessage(request: SendMessageRequest): Promise<Message> {
  try {
    const { data } = await apiClient.post<Message>('/chat/messages', { content: request.content })
    return data
  } catch (error) {
    const message =
      (error as { response?: { data?: { message?: string } } })?.response?.data?.message ??
      'Không thể kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.'
    throw new Error(message)
  }
}

/** Gọi thật BE (UC0017), không còn là mock: BE gọi RagQueryService.answer() (Gemini/RAG thật). */
export const mockChatService: ChatService = { sendMessage }
