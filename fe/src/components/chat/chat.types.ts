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
