import { HeartOutlined, PhoneOutlined, ReloadOutlined, SafetyOutlined, WarningFilled } from '@ant-design/icons'
import { Alert, Button, Card, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { ChatInput } from './ChatInput'
import { MessageList } from './MessageList'
import { SymptomAssessmentModal } from './SymptomAssessmentModal'
import { chatService, getChatSessionMessages, rateMessage } from '../../api/chat'
import type { Attachment, Message, RatingValue, SendMessageRequest } from '../../api/chat'

const { Text, Title } = Typography

export function ChatContainer() {
  const location = useLocation()
  // Khi FE điều hướng từ ChatHistoryPage với state { resumeSessionId, sessionTitle }
  const resumeSessionId: number | null = (location.state as { resumeSessionId?: number } | null)?.resumeSessionId ?? null
  const resumeTitle: string | null = (location.state as { sessionTitle?: string } | null)?.sessionTitle ?? null

  const [messages, setMessages] = useState<Message[]>([])
  const [isReplying, setIsReplying] = useState(false)
  const [isLoadingHistory, setIsLoadingHistory] = useState(resumeSessionId !== null)
  const [networkError, setNetworkError] = useState<string | null>(null)
  const [assessmentOpen, setAssessmentOpen] = useState(false)
  /** ID phiên hiện tại — null = chưa có phiên (câu đầu tiên sẽ tạo phiên mới). */
  const [currentSessionId, setCurrentSessionId] = useState<number | null>(resumeSessionId)

  const hasEmergencyInSession = messages.some((message) => message.emergency)

  useEffect(() => {
    let cancelled = false
    setCurrentSessionId(resumeSessionId)
    setMessages([])
    setNetworkError(null)

    if (resumeSessionId === null) {
      setIsLoadingHistory(false)
      return () => { cancelled = true }
    }

    setIsLoadingHistory(true)
    getChatSessionMessages(resumeSessionId)
      .then((history) => {
        if (cancelled) return
        setMessages(history.map((message) => ({
          id: `history-${message.id}`,
          dbMessageId: message.id,
          sender: message.sender,
          content: message.content,
          createdAt: message.createdAt,
          status: 'sent' as const,
          sources: message.sources,
          relatedQuestions: message.relatedQuestions,
          emergency: message.emergency,
          matchedGroups: message.matchedGroups,
          rating: message.rating,
        })))
      })
      .catch((error: unknown) => {
        if (cancelled) return
        setNetworkError(error instanceof Error ? error.message : 'Không thể tải lịch sử cuộc trò chuyện.')
      })
      .finally(() => {
        if (!cancelled) setIsLoadingHistory(false)
      })

    return () => { cancelled = true }
  }, [resumeSessionId])

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
      const response = await chatService.sendMessage({
        ...request,
        // Tiếp tục phiên hiện tại (hoặc null để tạo phiên mới ở câu đầu tiên)
        sessionId: currentSessionId,
      })
      updateMessage(patientMessage.id, { status: 'sent' })
      // Giữ sessionId từ response cho các lượt sau
      if (response.sessionId) setCurrentSessionId(response.sessionId)
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

  async function handleRate(message: Message, rating: RatingValue) {
    if (!message.dbMessageId) return
    // Clicking the active rating does not remove it: the API only supports HELPFUL/UNHELPFUL.
    if (message.rating === rating) return
    try {
      await rateMessage(message.dbMessageId, rating)
      // Cập nhật ngay UI, không cần reload
      updateMessage(message.id, { rating })
    } catch {
      // Silent fail — đây là tính năng phụ, không block người dùng
    }
  }

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-5">
      <div>
        <Text type="secondary">Chăm sóc sức khỏe tiêu hóa</Text>
        <Title level={2} className="mb-1! mt-1!">
          {resumeTitle ? `Tiếp tục: ${resumeTitle}` : 'Tư vấn sức khỏe'}
        </Title>
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
          <Button size="small" icon={<HeartOutlined />} onClick={() => setAssessmentOpen(true)}>
            Đánh giá triệu chứng
          </Button>
        </header>

        <div className="flex items-start gap-3 border-b border-amber-200 bg-gradient-to-r from-amber-50 to-yellow-50 px-4 py-3.5 text-sm leading-5 text-amber-900 sm:px-6" role="note">
          <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-amber-100 text-xs text-amber-700" aria-hidden="true"><SafetyOutlined /></span>
          <span><strong>Lưu ý y tế:</strong> Nếu bạn đang gặp trường hợp khẩn cấp, vui lòng gọi cấp cứu 115 hoặc đến cơ sở y tế gần nhất.</span>
        </div>

        {networkError && <Alert className="m-3 mb-0" type="error" showIcon message={networkError} action={<Button type="link" size="small" icon={<ReloadOutlined />} onClick={() => setNetworkError(null)}>Đóng</Button>} />}

        {hasEmergencyInSession && (
          <div
            className="flex items-center gap-2 border-b border-red-300 bg-red-50 px-4 py-2.5 text-sm font-medium text-red-900 sm:px-6"
            role="alert"
          >
            <WarningFilled className="shrink-0 text-red-600" />
            <span>
              Phiên này có cảnh báo khẩn cấp — nếu đang gặp nguy hiểm, vui lòng gọi ngay <PhoneOutlined /> <strong>115</strong> hoặc đến cơ sở y tế gần nhất.
            </span>
          </div>
        )}

        <MessageList
          messages={messages}
          isReplying={isReplying}
          isLoadingHistory={isLoadingHistory}
          onQuickPrompt={(prompt) => handleSend(prompt, [])}
          onRetry={handleRetry}
          onRate={handleRate}
        />
        <ChatInput disabled={isReplying || isLoadingHistory} onSend={handleSend} />
      </Card>
      <SymptomAssessmentModal open={assessmentOpen} onClose={() => setAssessmentOpen(false)} />
    </div>
  )
}
