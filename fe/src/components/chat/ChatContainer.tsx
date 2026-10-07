import { FilePdfOutlined, HeartOutlined, PhoneOutlined, ReloadOutlined, SafetyOutlined, WarningFilled } from '@ant-design/icons'
import { Alert, Button, Card, Typography, message } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ChatInput } from './ChatInput'
import { MessageList } from './MessageList'
import { downloadChatSessionPdf, getChatSessionMessages, loadAttachmentsForDisplay, rateMessage, streamMessage } from '../../api/chat'
import type { Attachment, Message, RatingValue, SendMessageRequest } from '../../api/chat'


const { Text, Title } = Typography

export function ChatContainer() {
  const location = useLocation()
  const navigate = useNavigate()
  // Khi FE điều hướng từ ChatHistoryPage với state { resumeSessionId, sessionTitle }
  const resumeSessionId: number | null = (location.state as { resumeSessionId?: number } | null)?.resumeSessionId ?? null
  const resumeTitle: string | null = (location.state as { sessionTitle?: string } | null)?.sessionTitle ?? null

  const [messages, setMessages] = useState<Message[]>([])
  const [isReplying, setIsReplying] = useState(false)
  const [isLoadingHistory, setIsLoadingHistory] = useState(resumeSessionId !== null)
  const [networkError, setNetworkError] = useState<string | null>(null)
  /** ID phiên hiện tại — null = chưa có phiên (câu đầu tiên sẽ tạo phiên mới). */
  const [currentSessionId, setCurrentSessionId] = useState<number | null>(resumeSessionId)
  const [exportingPdf, setExportingPdf] = useState(false)
  // Huy request streaming dang chay (component unmount hoac gui cau hoi moi de chong) - khong
  // cap nhat state cua 1 trang da dong/da chuyen sang luot hoi khac.
  const abortControllerRef = useRef<AbortController | null>(null)
  useEffect(() => () => abortControllerRef.current?.abort(), [])

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
      .then(async (history) => {
        if (cancelled) return
        const mapped = await Promise.all(history.map(async (message) => ({
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
          // Tải lại thành blob URL vì URL gốc cần header Authorization (xem loadAttachmentsForDisplay).
          // Lỗi tải 1 đính kèm không chặn hiển thị toàn bộ tin nhắn - chỉ mất phần preview đó.
          attachments: await loadAttachmentsForDisplay(message.attachments).catch(() => []),
        })))
        if (!cancelled) setMessages(mapped)
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

    const assistantMessageId = crypto.randomUUID()

    setNetworkError(null)
    if (existingMessage) updateMessage(patientMessage.id, { status: 'sending', createdAt: patientMessage.createdAt })
    else setMessages((current) => [...current, patientMessage])
    setIsReplying(true)

    abortControllerRef.current?.abort()
    const abortController = new AbortController()
    abortControllerRef.current = abortController

    try {
      await streamMessage(
        { ...request, sessionId: currentSessionId },
        {
          onToken: (text) => {
            // Them tin AI vao danh sach o lan goi DAU TIEN (chua co bong bong rong tu truoc -
            // vay nen khong con canh 3 cham "dang tra loi" de len bong bong rong). Cac lan
            // sau chi noi chu. Phai dung dang ham setMessages(current => ...) vi token ve
            // lien tuc - neu doc bien messages truc tiep se doc phai ban cu va mat chu.
            setMessages((current) => {
              const exists = current.some((message) => message.id === assistantMessageId)
              if (!exists) {
                return [...current, {
                  id: assistantMessageId,
                  sender: 'assistant',
                  content: text,
                  createdAt: new Date().toISOString(),
                  status: 'replying',
                }]
              }
              return current.map((message) =>
                message.id === assistantMessageId
                  ? { ...message, content: message.content + text }
                  : message,
              )
            })
          },
          onDone: (doneData) => {
            // Tin AI co the chua ton tai (truong hop hiem: cau tra loi rong, done den truoc
            // ca token dau tien) - neu vay them moi thay vi chi update, tranh mat cau tra loi.
            setMessages((current) => {
              const exists = current.some((message) => message.id === assistantMessageId)
              const resolvedFields = {
                sources: doneData.sources,
                relatedQuestions: doneData.relatedQuestions,
                emergency: doneData.emergency,
                matchedGroups: doneData.matchedGroups,
                dbMessageId: doneData.assistantMessageId,
                status: 'sent' as const,
              }
              if (!exists) {
                return [...current, {
                  id: assistantMessageId,
                  sender: 'assistant',
                  content: '',
                  createdAt: new Date().toISOString(),
                  ...resolvedFields,
                }]
              }
              return current.map((message) =>
                message.id === assistantMessageId ? { ...message, ...resolvedFields } : message,
              )
            })
            if (doneData.sessionId) setCurrentSessionId(doneData.sessionId)
            updateMessage(patientMessage.id, { status: 'sent' })
          },
          onError: (message) => {
            setMessages((current) => current.filter((item) => item.id !== assistantMessageId))
            updateMessage(patientMessage.id, { status: 'failed' })
            setNetworkError(message)
          },
        },
        abortController.signal,
      )
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
  async function handleExportPdf() {
    if (currentSessionId === null || exportingPdf) return
    setExportingPdf(true)
    try {
      await downloadChatSessionPdf(currentSessionId)
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Không thể xuất PDF phiên chat.')
    } finally {
      setExportingPdf(false)
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
          <div className="flex items-center gap-2">
            {currentSessionId !== null && (
              <Button
                size="small"
                icon={<FilePdfOutlined />}
                loading={exportingPdf}
                disabled={isReplying}
                onClick={handleExportPdf}
              >
                Xuất PDF
              </Button>
            )}
            <Button size="small" icon={<HeartOutlined />} onClick={() => navigate('/symptom-assessment')}>
              Đánh giá triệu chứng
            </Button>
          </div>
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
    </div>
  )
}