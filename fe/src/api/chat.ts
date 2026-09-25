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

export interface Message {
  id: string
  sender: SenderType
  content: string
  createdAt: string
  status: MessageStatus
  attachments?: Attachment[]
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

function wait(milliseconds: number) {
  return new Promise<void>((resolve) => window.setTimeout(resolve, milliseconds))
}

function createReply(content: string): string {
  const normalizedContent = content.toLocaleLowerCase('vi-VN')

  if (normalizedContent.includes('sốt')) {
    return 'Bạn nên đo nhiệt độ, uống đủ nước và nghỉ ngơi. Nếu sốt từ 39°C, kéo dài quá 48 giờ hoặc kèm khó thở, lơ mơ, hãy đến cơ sở y tế.'
  }
  if (normalizedContent.includes('thuốc')) {
    return 'Bạn nên dùng thuốc đúng theo đơn và không tự ý tăng hoặc giảm liều. Nếu quên liều, hãy kiểm tra hướng dẫn trên đơn hoặc liên hệ bác sĩ điều trị.'
  }
  if (normalizedContent.includes('đau bụng')) {
    return 'Bạn hãy theo dõi vị trí, mức độ và thời gian đau. Nếu đau dữ dội, bụng cứng, nôn ra máu hoặc đi ngoài phân đen, hãy đến cơ sở y tế ngay.'
  }
  if (normalizedContent.includes('dạ dày') || normalizedContent.includes('chế độ ăn')) {
    return 'Bạn nên ưu tiên bữa ăn nhỏ, đúng giờ; hạn chế rượu bia, cà phê, đồ cay và nhiều dầu mỡ. Nếu đau kéo dài, hãy trao đổi với bác sĩ.'
  }

  return 'Cảm ơn bạn đã chia sẻ. Thông tin từ GastroAI chỉ mang tính tham khảo và không thay thế chẩn đoán của bác sĩ. Nếu triệu chứng kéo dài hoặc tăng nặng, bạn nên đến cơ sở y tế.'
}

async function sendMessage(request: SendMessageRequest): Promise<Message> {
  await wait(1200)

  const shouldSimulateNetworkError = /mất kết nối|mat ket noi|network error/i.test(request.content)
  if (shouldSimulateNetworkError && !request.isRetry) {
    throw new Error('Không thể kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.')
  }

  return {
    id: crypto.randomUUID(),
    sender: 'assistant',
    content: createReply(request.content),
    createdAt: new Date().toISOString(),
    status: 'sent',
  }
}

/** Mock implementation boundary: có thể thay bằng REST/WebSocket mà không đổi UI. */
export const mockChatService: ChatService = { sendMessage }
