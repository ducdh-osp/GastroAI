import { ClockCircleOutlined, LoadingOutlined, MessageOutlined, SearchOutlined } from '@ant-design/icons'
import { Alert, Input, Spin, Typography, message } from 'antd'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deleteChatSession, downloadChatSessionPdf, getChatSessionMessages, listChatSessions } from '../../api/chat'
import type { ChatMessageDetail, ChatSessionSummary } from '../../api/chat'
import { AppShell } from '../../components/layout/AppShell'
import { HistorySessionItem } from '../../components/chat/HistorySessionItem'
import { HistoryMessagePanel } from '../../components/chat/HistoryMessagePanel'

const { Title, Text } = Typography

function getSessionTimeGroup(isoString: string): 'Hôm nay' | '7 ngày qua' | 'Trước đó' {
  const target = new Date(isoString)
  const now = new Date()
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const diffDays = (startOfToday - new Date(target.getFullYear(), target.getMonth(), target.getDate()).getTime()) / 86400000
  if (diffDays <= 0) return 'Hôm nay'
  if (diffDays < 7) return '7 ngày qua'
  return 'Trước đó'
}

export default function ChatHistoryPage() {
  const navigate = useNavigate()

  // ── Danh sách phiên ──
  const [sessions, setSessions] = useState<ChatSessionSummary[]>([])
  const [searchQuery, setSearchQuery] = useState('')
  const [loadingSessions, setLoadingSessions] = useState(true)
  const [sessionsError, setSessionsError] = useState<string | null>(null)

  // ── Phiên đang xem ──
  const [selectedSession, setSelectedSession] = useState<ChatSessionSummary | null>(null)
  const [messages, setMessages] = useState<ChatMessageDetail[]>([])
  const messageRequestSequence = useRef(0)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [messagesError, setMessagesError] = useState<string | null>(null)
  const [deletingSessionId, setDeletingSessionId] = useState<number | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const [exportingId, setExportingId] = useState<number | null>(null)
  const hasLoadedRef = useRef(false)
  useEffect(() => () => { messageRequestSequence.current += 1 }, [])
  useEffect(() => {
    let cancelled = false
    const timer = window.setTimeout(() => {
      if (!hasLoadedRef.current) setLoadingSessions(true)
      setSessionsError(null)
      listChatSessions(0, 50, searchQuery)
        .then((res) => {
          if (!cancelled) setSessions(res.sessions)
        })
        .catch(() => {
          if (!cancelled) setSessionsError('Không thể tải lịch sử phiên chat.')
        })
        .finally(() => {
          hasLoadedRef.current = true
          if (!cancelled) setLoadingSessions(false)
        })
    }, searchQuery.trim() ? 250 : 0)
    return () => {
      cancelled = true
      window.clearTimeout(timer)
    }
  }, [searchQuery])

  async function handleSelectSession(session: ChatSessionSummary) {
    const requestSequence = ++messageRequestSequence.current
    setSelectedSession(session)
    setMessages([])
    setMessagesError(null)
    setLoadingMessages(true)
    try {
      const msgs = await getChatSessionMessages(session.id)
      if (requestSequence === messageRequestSequence.current) setMessages(msgs)
    } catch {
      if (requestSequence === messageRequestSequence.current) setMessagesError('Không thể tải nội dung phiên chat.')
    } finally {
      if (requestSequence === messageRequestSequence.current) setLoadingMessages(false)
    }
  }

  function handleContinueChat(session: ChatSessionSummary) {
    // Điều hướng sang trang chat với sessionId để tiếp tục phiên cũ
    navigate('/chat', { state: { resumeSessionId: session.id, sessionTitle: session.title } })
  }

  async function handleDeleteSession(session: ChatSessionSummary) {
    if (deletingSessionId !== null) return

    const sessionId = session.id
    setDeletingSessionId(sessionId)
    setDeleteError(null)
    try {
      await deleteChatSession(sessionId)
      setSessions((current) => current.filter((session) => session.id !== sessionId))
      if (selectedSession?.id === sessionId) {
        messageRequestSequence.current += 1
        setSelectedSession(null)
        setMessages([])
        setLoadingMessages(false)
        setMessagesError(null)
      }
    } catch {
      setDeleteError('Không thể xóa phiên chat. Vui lòng thử lại.')
    } finally {
      setDeletingSessionId(null)
    }
  }
  async function handleExport(session: ChatSessionSummary) {
    if (exportingId !== null) return
    setExportingId(session.id)
    try {
      await downloadChatSessionPdf(session.id)
    } catch (error) {
      message.error(error instanceof Error ? error.message : 'Không thể xuất PDF phiên chat.')
    } finally {
      setExportingId(null)
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

  return (
    <AppShell fixedViewport>
      <div className="flex h-full min-h-0 w-full flex-col">
        <div className="mb-6 shrink-0">
          <Text type="secondary">Lịch sử tư vấn</Text>
          <Title level={2} className="mb-1! mt-1!">
            Phiên chat của tôi
          </Title>
          <Text type="secondary">
            Xem lại các cuộc trò chuyện với trợ lý GastroAI và tiếp tục nơi bạn dừng lại.
          </Text>
        </div>

        {deleteError && <Alert type="error" message={deleteError} showIcon className="mb-4! shrink-0" />}

        <div className="flex min-h-0 flex-1 gap-6">
          {/* ── Cột trái: danh sách phiên ── */}
          <aside className="flex min-h-0 w-80 shrink-0 flex-col">
            <div className="flex h-full min-h-0 flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-white/10 dark:bg-neutral-900">
              <div className="shrink-0 space-y-2.5 border-b border-slate-100 px-4 py-3 dark:border-white/10">
                <div className="flex items-center gap-2">
                  <ClockCircleOutlined className="text-teal-600" />
                  <span className="font-medium text-slate-700 dark:text-white text-sm">Danh sách phiên</span>
                </div>
                <Input
                  size="small"
                  allowClear
                  prefix={<SearchOutlined className="text-slate-400" />}
                  placeholder="Tìm kiếm phiên trò chuyện..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>

              {loadingSessions ? (
                <div className="flex min-h-0 flex-1 items-center justify-center py-16">
                  <Spin indicator={<LoadingOutlined className="text-teal-600 text-xl" spin />} />
                </div>
              ) : sessionsError ? (
                <div className="min-h-0 flex-1 p-4">
                  <Alert type="error" message={sessionsError} showIcon />
                </div>
              ) : groupedSessions.length === 0 ? (
                <div className="flex min-h-0 flex-1 flex-col items-center justify-center px-4 py-16 text-center">
                  <MessageOutlined className="text-3xl text-slate-300 mb-3" />
                  <Text type="secondary" className="text-sm">
                    {searchQuery.trim()
                      ? `Không tìm thấy phiên phù hợp với "${searchQuery.trim()}"`
                      : 'Chưa có phiên chat nào. Hãy bắt đầu tư vấn!'}
                  </Text>
                </div>
              ) : (
                <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
                  {groupedSessions.map(([groupLabel, groupItems]) => (
                    <div key={groupLabel}>
                      <div className="sticky top-0 z-10 bg-slate-50/95 px-4 py-1.5 text-[11px] font-semibold uppercase tracking-wider text-slate-500 backdrop-blur-xs dark:bg-neutral-800/95 dark:text-slate-300">
                        {groupLabel}
                      </div>
                      {groupItems.map((session) => (
                        <HistorySessionItem
                          key={session.id}
                          session={session}
                          isSelected={selectedSession?.id === session.id}
                          onClick={() => void handleSelectSession(session)}
                          onDelete={() => void handleDeleteSession(session)}
                          onExport={() => void handleExport(session)}
                          deleting={deletingSessionId === session.id}
                          exporting={exportingId === session.id}
                        />
                      ))}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </aside>

          {/* ── Cột phải: nội dung phiên được chọn ── */}
          <div className="min-h-0 min-w-0 flex-1">
            <HistoryMessagePanel
              session={selectedSession}
              messages={messages}
              loading={loadingMessages}
              error={messagesError}
              onContinue={selectedSession ? () => handleContinueChat(selectedSession) : undefined}
            />
          </div>
        </div>
      </div>
    </AppShell>
  )
}
