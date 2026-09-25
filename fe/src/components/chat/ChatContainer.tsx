import { ReloadOutlined, SafetyOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Typography } from 'antd'
import { useCallback, useState } from 'react'
import { ChatInput } from './ChatInput'
import { MessageList } from './MessageList'
import { mockChatService } from './mockChatApi'
import type { Attachment, Message, SendMessageRequest } from './chat.types'

const { Text, Title } = Typography

export function ChatContainer() {
  const [messages, setMessages] = useState<Message[]>([])
  const [isReplying, setIsReplying] = useState(false)
  const [networkError, setNetworkError] = useState<string | null>(null)

  const updateMessage = useCallback((id: string, update: Partial<Message>) => {
    setMessages((current) => current.map((message) => message.id === id ? { ...message, ...update } : message))
  }, [])

  async function send(request: SendMessageRequest, existingMessage?: Message) {
    const patientMessage: Message = existingMessage
      ? { ...existingMessage, status: 'sending', createdAt: new Date().toISOString() }
      : { id: crypto.randomUUID(), sender: 'patient', content: request.content, attachments: request.attachments, createdAt: new Date().toISOString(), status: 'sending' }

    setNetworkError(null)
    if (existingMessage) updateMessage(patientMessage.id, { status: 'sending', createdAt: patientMessage.createdAt })
    else setMessages((current) => [...current, patientMessage])
    setIsReplying(true)

    try {
      const response = await mockChatService.sendMessage(request)
      updateMessage(patientMessage.id, { status: 'sent' })
      setMessages((current) => [...current, response])
    } catch (error) {
      updateMessage(patientMessage.id, { status: 'failed' })
      setNetworkError(error instanceof Error ? error.message : 'Không thể kết nối mạng. Vui lòng thử lại.')
    } finally {
      setIsReplying(false)
    }
  }

  function handleSend(content: string, attachments: Attachment[]) {
    void send({ content, attachments })
  }

  function handleRetry(message: Message) {
    void send({ content: message.content, attachments: message.attachments, isRetry: true }, message)
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-5">
      <div>
        <Text type="secondary">Chăm sóc sức khỏe tiêu hóa</Text>
        <Title level={2} className="mb-1! mt-1!">Tư vấn sức khỏe</Title>
        <Text type="secondary">Đặt câu hỏi để nhận thông tin tham khảo từ trợ lý GastroAI.</Text>
      </div>

      <Card className="flex h-[min(760px,calc(100svh-180px))] min-h-[560px] flex-col overflow-hidden rounded-2xl border-black/5 p-0! shadow-sm" styles={{ body: { display: 'flex', minHeight: 0, height: '100%', flexDirection: 'column', padding: 0 } }}>
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-4 sm:px-6">
          <div className="flex items-center gap-3">
            <span className="relative flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-teal-500 to-teal-700 text-lg text-white shadow-sm shadow-teal-700/20" aria-hidden="true">
              <SafetyOutlined />
              <span className="absolute -bottom-0.5 -right-0.5 h-3 w-3 rounded-full border-2 border-white bg-emerald-400" />
            </span>
            <div>
              <h3 className="m-0 text-base font-semibold tracking-tight text-slate-800">Trợ lý GastroAI</h3>
              <span className="mt-0.5 block text-xs font-medium text-emerald-600">Đang hoạt động</span>
            </div>
          </div>
          <span className="hidden rounded-full bg-teal-50 px-3 py-1 text-xs font-medium text-teal-700 sm:inline-flex">Tư vấn trực tuyến</span>
        </header>

        <div className="flex items-start gap-3 border-b border-amber-200 bg-gradient-to-r from-amber-50 to-yellow-50 px-4 py-3.5 text-sm leading-5 text-amber-900 sm:px-6" role="note">
          <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-amber-100 text-xs text-amber-700" aria-hidden="true"><SafetyOutlined /></span>
          <span><strong>Lưu ý y tế:</strong> Nếu bạn đang gặp trường hợp khẩn cấp, vui lòng gọi cấp cứu 115 hoặc đến cơ sở y tế gần nhất.</span>
        </div>

        {networkError && <Alert className="m-3 mb-0" type="error" showIcon message={networkError} action={<Button type="link" size="small" icon={<ReloadOutlined />} onClick={() => setNetworkError(null)}>Đóng</Button>} />}

        <MessageList messages={messages} isReplying={isReplying} onQuickPrompt={(prompt) => handleSend(prompt, [])} onRetry={handleRetry} />
        <ChatInput disabled={isReplying} onSend={handleSend} />
      </Card>
    </div>
  )
}
