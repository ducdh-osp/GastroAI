import type { ChatService, Message, SendMessageRequest } from './chat.types'

export const QUICK_PROMPTS = [
  'Tư vấn triệu chứng sốt',
  'Hỏi lịch uống thuốc',
  'Đau bụng nên làm gì?',
  'Giải thích kết quả xét nghiệm',
] as const

export const MOCK_MESSAGES: Message[] = []

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
  if (normalizedContent.includes('xét nghiệm')) {
    return 'Bạn có thể đính kèm ảnh hoặc tệp kết quả xét nghiệm. Việc diễn giải chính xác vẫn cần được bác sĩ đối chiếu với triệu chứng và tiền sử của bạn.'
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

export const mockChatService: ChatService = { sendMessage }
