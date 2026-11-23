import {
  FilePdfOutlined,
  FormOutlined,
  HeartOutlined,
  LoadingOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  MessageOutlined,
  PhoneOutlined,
  ReloadOutlined,
  SafetyOutlined,
  SearchOutlined,
  WarningFilled,
} from '@ant-design/icons'
import { Alert, Button, Input, Spin, Tooltip, Typography, message } from 'antd'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { ChatInput } from './ChatInput'
import { HistorySessionItem } from './HistorySessionItem'
import { MessageList } from './MessageList'
import {
  deleteChatSession,
  downloadChatSessionPdf,
  getChatSessionMessages,
  listChatSessions,
  loadAttachmentsForDisplay,
  rateMessage,
  streamMessage,
} from '../../api/chat'
import type { Attachment, ChatSessionSummary, Message, RatingValue, SendMessageRequest } from '../../api/chat'

const { Text } = Typography

function getSessionTimeGroup(isoString: string): 'Hôm nay' | '7 ngày qua' | 'Trước đó' {
  const target = new Date(isoString)
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const diffDays = (startOfToday - new Date(target.getFullYear(), target.getMonth(), target.getDate()).getTime()) / 86400000
  if (diffDays <= 0) return 'Hôm nay'
  if (diffDays < 7) return '7 ngày qua'
  return 'Trước đó'
}

export function ChatContainer() {
  const location = useLocation()
  const navigate = useNavigate()
  const locationState = location.state as { resumeSessionId?: number; sessionTitle?: string; initialQuestion?: string } | null
  const resumeSessionId = locationState?.resumeSessionId ?? null
  const resumeTitle = locationState?.sessionTitle ?? null
  const initialQuestion = locationState?.initialQuestion ?? ''

  // ── Lịch sử phiên chat (thanh bên trái kiểu Gemini) ──
  const [sessions, setSessions] = useState<ChatSessionSummary[]>([])
  const [loadingSessions, setLoadingSessions] = useState(true)
  const [sessionsError, setSessionsError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [deletingSessionId, setDeletingSessionId] = useState<number | null>(null)
  const [exportingSessionId, setExportingSessionId] = useState<number | null>(null)
  const [historyCollapsed, setHistoryCollapsed] = useState(false)

  // ── Phiên chat hiện tại ──
  const [currentSessionId, setCurrentSessionId] = useState<number | null>(resumeSessionId)
  const [activeSessionTitle, setActiveSessionTitle] = useState<string | null>(resumeTitle)
  const [messages, setMessages] = useState<Message[]>([])
  const [isReplying, setIsReplying] = useState(false)
  const [isLoadingHistory, setIsLoadingHistory] = useState(resumeSessionId !== null)
  const [networkError, setNetworkError] = useState<string | null>(null)
  const [exportingPdf, setExportingPdf] = useState(false)

  const abortControllerRef = useRef<AbortController | null>(null)
  const historyRequestSeqRef = useRef(0)
  const hasLoadedSessionsRef = useRef(false)

  useEffect(() => {
    return () => {
      abortControllerRef.current?.abort()
      historyRequestSeqRef.current += 1
    }
  }, [])

  const fetchSessions = useCallback(async (showSpinner = false, query?: string) => {
    if (showSpinner) setLoadingSessions(true)
    setSessionsError(null)
    try {
      const res = await listChatSessions(0, 50, query)
      setSessions(res.sessions)
    } catch {
      setSessionsError('Không thể tải danh sách phiên chat.')
    } finally {
      hasLoadedSessionsRef.current = true
      if (showSpinner) setLoadingSessions(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void fetchSessions(!hasLoadedSessionsRef.current, searchQuery)
    }, searchQuery.trim() ? 250 : 0)
    return () => window.clearTimeout(timer)
  }, [fetchSessions, searchQuery])

  const loadSessionMessages = useCallback(async (sessionId: number) => {
    const seq = ++historyRequestSeqRef.current
    setIsLoadingHistory(true)
    setNetworkError(null)
    try {
      const history = await getChatSessionMessages(sessionId)
      if (seq !== historyRequestSeqRef.current) return
      const mapped = await Promise.all(
        history.map(async (item) => ({
          id: `history-${item.id}`,
          dbMessageId: item.id,
          sender: item.sender,
          content: item.content,
          createdAt: item.createdAt,
          status: 'sent' as const,
          sources: item.sources,
          relatedQuestions: item.relatedQuestions,
          emergency: item.emergency,
          matchedGroups: item.matchedGroups,
          rating: item.rating,
          attachments: await loadAttachmentsForDisplay(item.attachments).catch(() => []),
        })),
      )
      if (seq === historyRequestSeqRef.current) {
        setMessages(mapped)
      }
    } catch (error: unknown) {
      if (seq === historyRequestSeqRef.current) {
        setNetworkError(error instanceof Error ? error.message : 'Không thể tải lịch sử cuộc trò chuyện.')
      }
    } finally {
      if (seq === historyRequestSeqRef.current) {
        setIsLoadingHistory(false)
      }
    }
  }, [])

  // Khi điều hướng từ nơi khác tới kèm resumeSessionId
  useEffect(() => {
    if (resumeSessionId !== null) {
      setCurrentSessionId(resumeSessionId)
      setActiveSessionTitle(resumeTitle)
      setMessages([])
      void loadSessionMessages(resumeSessionId)
    }
  }, [resumeSessionId, resumeTitle, loadSessionMessages])

  const hasEmergencyInSession = messages.some((msg) => msg.emergency)

  const updateMessage = useCallback((id: string, update: Partial<Message>) => {
    setMessages((current) => current.map((item) => (item.id === id ? { ...item, ...update } : item)))
  }, [])

  function handleSelectSession(session: ChatSessionSummary) {
    if (session.id === currentSessionId && messages.length > 0 && !networkError) {
      return
    }
    abortControllerRef.current?.abort()
    setIsReplying(false)
    setCurrentSessionId(session.id)
    setActiveSessionTitle(session.title)
    setMessages([])
    void loadSessionMessages(session.id)
  }

  function handleNewChat() {
    abortControllerRef.current?.abort()
    historyRequestSeqRef.current += 1
    setIsReplying(false)
    setIsLoadingHistory(false)
    setCurrentSessionId(null)
    setActiveSessionTitle(null)
    setMessages([])
    setNetworkError(null)
    if (resumeSessionId !== null || initialQuestion) {
      navigate('/chat', { replace: true, state: null })
    }
  }

  async function handleDeleteSession(session: ChatSessionSummary) {
    if (deletingSessionId !== null) return
    setDeletingSessionId(session.id)
    try {
      await deleteChatSession(session.id)
      setSessions((current) => current.filter((item) => item.id !== session.id))
      if (currentSessionId === session.id) {
        handleNewChat()
      }
      message.success('Đã xóa phiên trò chuyện.')
    } catch {
      message.error('Không thể xóa phiên chat. Vui lòng thử lại.')
    } finally {
      setDeletingSessionId(null)
    }
  }

  async function handleExportSession(sessionId: number) {
    if (exportingSessionId !== null || exportingPdf) return
    setExportingSessionId(sessionId)
    if (sessionId === currentSessionId) setExportingPdf(true)
    try {
      await downloadChatSessionPdf(sessionId)
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Không thể xuất PDF phiên chat.')
    } finally {
      setExportingSessionId(null)
      setExportingPdf(false)
    }
  }

  async function send(request: SendMessageRequest, existingMessage?: Message) {
    const patientMessage: Message = existingMessage
      ? { ...existingMessage, status: 'sending', createdAt: new Date().toISOString() }
      : {
          id: crypto.randomUUID(),
          sender: 'patient',
          content: request.content,
          attachments: request.attachments,
          createdAt: new Date().toISOString(),
          status: 'sending',
        }

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
          onAccepted: () => updateMessage(patientMessage.id, { status: 'sent' }),
          onToken: (text) => {
            setMessages((current) => {
              const exists = current.some((item) => item.id === assistantMessageId)
              if (!exists) {
                return [
                  ...current,
                  {
                    id: assistantMessageId,
                    sender: 'assistant',
                    content: text,
                    createdAt: new Date().toISOString(),
                    status: 'replying',
                  },
                ]
              }
              return current.map((item) =>
                item.id === assistantMessageId ? { ...item, content: item.content + text } : item,
              )
            })
          },
          onDone: (doneData) => {
            setMessages((current) => {
              const exists = current.some((item) => item.id === assistantMessageId)
              const resolvedFields = {
                sources: doneData.sources,
                relatedQuestions: doneData.relatedQuestions,
                emergency: doneData.emergency,
                matchedGroups: doneData.matchedGroups,
                dbMessageId: doneData.assistantMessageId,
                status: 'sent' as const,
              }
              if (!exists) {
                return [
                  ...current,
                  {
                    id: assistantMessageId,
                    sender: 'assistant',
                    content: '',
                    createdAt: new Date().toISOString(),
                    ...resolvedFields,
                  },
                ]
              }
              return current.map((item) => (item.id === assistantMessageId ? { ...item, ...resolvedFields } : item))
            })
            if (doneData.sessionId) {
              setCurrentSessionId(doneData.sessionId)
              if (!activeSessionTitle) {
                setActiveSessionTitle(request.content.trim().slice(0, 80))
              }
            }
            updateMessage(patientMessage.id, { status: 'sent' })
            void fetchSessions(false)
          },
          onError: (msg) => {
            setMessages((current) => current.filter((item) => item.id !== assistantMessageId))
            updateMessage(patientMessage.id, { status: 'failed' })
            setNetworkError(msg)
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

  function handleRetry(targetMsg: Message) {
    void send({ content: targetMsg.content, attachments: targetMsg.attachments, isRetry: true }, targetMsg)
  }

  async function handleRate(targetMsg: Message, rating: RatingValue) {
    if (!targetMsg.dbMessageId) return
    if (targetMsg.rating === rating) {
      message.info('Bạn đã đánh giá câu trả lời này rồi.')
      return
    }
    try {
      await rateMessage(targetMsg.dbMessageId, rating)
      updateMessage(targetMsg.id, { rating })
      if (rating === 'HELPFUL') {
        message.success('Cảm ơn bạn! Đánh giá hữu ích đã được ghi nhận để cải thiện chất lượng AI.')
      } else {
        message.info('Cảm ơn góp ý của bạn! Hệ thống sẽ ghi nhận để nâng cao độ chính xác câu trả lời.')
      }
    } catch {
      message.error('Không thể gửi đánh giá lúc này. Vui lòng thử lại sau.')
    }
  }

  const groupedSessions = useMemo(() => {
    const groups = new Map<string, ChatSessionSummary[]>()
    for (const s of sessions) {
      const label = getSessionTimeGroup(s.updatedAt)
      const list = groups.get(label) ?? []
      list.push(s)
      groups.set(label, list)
    }
    return Array.from(groups.entries())
  }, [sessions])

  function renderHistoryContent(showCollapseButton: boolean) {
    return (
      <div className="flex h-full min-h-0 flex-col bg-white dark:bg-neutral-900">
        <div className="shrink-0 space-y-2.5 border-b border-slate-100 p-3.5 dark:border-white/10">
          <div className="flex items-center gap-2">
            <Button
              type="primary"
              icon={<FormOutlined />}
              onClick={handleNewChat}
              className="flex-1 justify-start rounded-xl font-medium shadow-xs"
            >
              Cuộc trò chuyện mới
            </Button>
            {showCollapseButton && (
              <Tooltip title="Thu gọn thanh lịch sử">
                <Button
                  type="text"
                  icon={<MenuFoldOutlined />}
                  aria-label="Thu gọn thanh lịch sử"
                  onClick={() => setHistoryCollapsed(true)}
                />
              </Tooltip>
            )}
          </div>

          <Input
            allowClear
            size="middle"
            prefix={<SearchOutlined className="text-slate-400" />}
            placeholder="Tìm kiếm cuộc trò chuyện..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="rounded-xl"
          />
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
          {loadingSessions ? (
            <div className="flex h-48 items-center justify-center">
              <Spin indicator={<LoadingOutlined className="text-xl text-teal-600" spin />} />
            </div>
          ) : sessionsError ? (
            <div className="p-3">
              <Alert
                type="error"
                message={sessionsError}
                showIcon
                action={
                  <Button size="small" type="link" onClick={() => void fetchSessions(true)}>
                    Thử lại
                  </Button>
                }
              />
            </div>
          ) : groupedSessions.length === 0 ? (
            <div className="flex flex-col items-center justify-center px-4 py-14 text-center">
              <MessageOutlined className="mb-2.5 text-2xl text-slate-300 dark:text-slate-600" />
              <Text type="secondary" className="text-xs">
                {searchQuery.trim()
                  ? `Không tìm thấy phiên phù hợp với "${searchQuery.trim()}"`
                  : 'Chưa có lịch sử trò chuyện. Hãy đặt câu hỏi đầu tiên!'}
              </Text>
            </div>
          ) : (
            groupedSessions.map(([groupLabel, groupItems]) => (
              <div key={groupLabel}>
                <div className="sticky top-0 z-10 bg-slate-50/95 px-4 py-1.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500 backdrop-blur-xs dark:bg-neutral-800/95 dark:text-slate-300">
                  {groupLabel}
                </div>
                {groupItems.map((session) => (
                  <HistorySessionItem
                    key={session.id}
                    session={session}
                    isSelected={currentSessionId === session.id}
                    onClick={() => handleSelectSession(session)}
                    onDelete={() => void handleDeleteSession(session)}
                    onExport={() => void handleExportSession(session.id)}
                    deleting={deletingSessionId === session.id}
                    exporting={exportingSessionId === session.id}
                  />
                ))}
              </div>
            ))
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-full min-h-0 w-full overflow-hidden">
      {/* Thanh lịch sử phiên chat bên trái (Desktop) */}
      {!historyCollapsed && (
        <aside
          aria-label="Lịch sử phiên trò chuyện"
          className="hidden h-full w-72 shrink-0 flex-col border-r border-slate-200 lg:flex xl:w-80 dark:border-white/10"
        >
          {renderHistoryContent(true)}
        </aside>
      )}

      {/* Vùng tư vấn AI chính bên phải */}
      <div className="flex min-h-0 min-w-0 flex-1 flex-col bg-[#f7faf9] dark:bg-[#0a0a0a]">
        <header className="flex shrink-0 items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-3 sm:px-6 dark:border-white/10 dark:bg-neutral-900">
          <div className="flex min-w-0 items-center gap-3">
            {historyCollapsed && (
              <Tooltip title="Mở thanh lịch sử">
                <Button
                  type="text"
                  icon={<MenuUnfoldOutlined />}
                  aria-label="Mở thanh lịch sử"
                  className="hidden lg:inline-flex"
                  onClick={() => setHistoryCollapsed(false)}
                />
              </Tooltip>
            )}
            <span
              className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-teal-500 to-teal-700 text-base text-white shadow-sm shadow-teal-700/20"
              aria-hidden="true"
            >
              <SafetyOutlined />
              <span className="absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full border-2 border-white bg-emerald-400 dark:border-neutral-900" />
            </span>
            <div className="min-w-0">
              <h1 className="m-0 truncate text-base font-semibold tracking-tight text-slate-800 dark:text-white">
                {activeSessionTitle || 'Trợ lý GastroAI'}
              </h1>
              <span className="block text-xs font-medium text-emerald-600 dark:text-emerald-400">
                {currentSessionId !== null ? 'Tư vấn tiêu hóa · Phiên đã lưu' : 'Sẵn sàng tư vấn'}
              </span>
            </div>
          </div>

          <div className="flex shrink-0 items-center gap-2">
            {currentSessionId !== null && (
              <Button
                size="small"
                icon={<FilePdfOutlined />}
                loading={exportingPdf}
                disabled={isReplying}
                onClick={() => void handleExportSession(currentSessionId)}
              >
                Xuất PDF
              </Button>
            )}
            <Button size="small" icon={<HeartOutlined />} onClick={() => navigate('/symptom-assessment')}>
              Đánh giá triệu chứng
            </Button>
          </div>
        </header>

        {messages.length === 0 && (
          <div
            className="flex shrink-0 items-center justify-center gap-2.5 border-b border-amber-200 bg-gradient-to-r from-amber-50 to-yellow-50 px-4 py-2.5 text-xs leading-5 text-amber-900 sm:px-6 sm:text-sm dark:border-amber-900/40 dark:from-amber-950/50 dark:to-yellow-950/40 dark:text-amber-100"
            role="note"
          >
            <SafetyOutlined className="shrink-0 text-amber-700 dark:text-amber-300" />
            <span>
              <strong>Lưu ý y tế:</strong> Nếu bạn đang gặp trường hợp khẩn cấp, vui lòng gọi cấp cứu{' '}
              <a href="tel:115" className="font-bold underline">
                115
              </a>{' '}
              hoặc đến cơ sở y tế gần nhất.
            </span>
          </div>
        )}

        {networkError && (
          <div className="mx-auto w-full max-w-3xl shrink-0 px-4 pt-3 sm:px-6">
            <Alert
              type="error"
              showIcon
              message={networkError}
              action={
                <Button type="link" size="small" icon={<ReloadOutlined />} onClick={() => setNetworkError(null)}>
                  Đóng
                </Button>
              }
            />
          </div>
        )}

        {hasEmergencyInSession && (
          <div
            className="flex shrink-0 items-center justify-center gap-2 border-b border-red-300 bg-red-50 px-4 py-2.5 text-sm font-medium text-red-900 sm:px-6 dark:border-red-900/50 dark:bg-red-950/50 dark:text-red-100"
            role="alert"
          >
            <WarningFilled className="shrink-0 text-red-600" />
            <span>
              Phiên này có cảnh báo khẩn cấp — nếu đang gặp nguy hiểm, vui lòng gọi ngay <PhoneOutlined />{' '}
              <a href="tel:115" className="font-bold text-red-900 underline dark:text-red-100">
                115
              </a>{' '}
              hoặc đến cơ sở y tế gần nhất.
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

        <ChatInput disabled={isReplying || isLoadingHistory} initialContent={initialQuestion} onSend={handleSend} />
      </div>
    </div>
  )
}
