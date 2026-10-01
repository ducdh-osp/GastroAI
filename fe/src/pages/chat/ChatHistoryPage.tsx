import { ClockCircleOutlined, LoadingOutlined, MessageOutlined } from '@ant-design/icons'
import { Alert, Spin, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deleteChatSession, getChatSessionMessages, listChatSessions } from '../../api/chat'
import type { ChatMessageDetail, ChatSessionSummary } from '../../api/chat'
import { AppShell } from '../../components/layout/AppShell'
import { HistorySessionItem } from '../../components/chat/HistorySessionItem'
import { HistoryMessagePanel } from '../../components/chat/HistoryMessagePanel'

const { Title, Text } = Typography

export default function ChatHistoryPage() {
  const navigate = useNavigate()

  // ── Danh sách phiên ──
  const [sessions, setSessions] = useState<ChatSessionSummary[]>([])
  const [loadingSessions, setLoadingSessions] = useState(true)
  const [sessionsError, setSessionsError] = useState<string | null>(null)

  // ── Phiên đang xem ──
  const [selectedSession, setSelectedSession] = useState<ChatSessionSummary | null>(null)
  const [messages, setMessages] = useState<ChatMessageDetail[]>([])
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [messagesError, setMessagesError] = useState<string | null>(null)
  const [deletingSessionId, setDeletingSessionId] = useState<number | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  useEffect(() => {
    setLoadingSessions(true)
    setSessionsError(null)
    listChatSessions(0, 50)
      .then((res) => setSessions(res.sessions))
      .catch(() => setSessionsError('Không thể tải lịch sử phiên chat.'))
      .finally(() => setLoadingSessions(false))
  }, [])

  async function handleSelectSession(session: ChatSessionSummary) {
    setSelectedSession(session)
    setMessagesError(null)
    setLoadingMessages(true)
    try {
      const msgs = await getChatSessionMessages(session.id)
      setMessages(msgs)
    } catch {
      setMessagesError('Không thể tải nội dung phiên chat.')
    } finally {
      setLoadingMessages(false)
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
        setSelectedSession(null)
        setMessages([])
        setMessagesError(null)
      }
    } catch {
      setDeleteError('Không thể xóa phiên chat. Vui lòng thử lại.')
    } finally {
      setDeletingSessionId(null)
    }
  }

  return (
    <AppShell fixedViewport>
      <div className="mx-auto flex h-full min-h-0 max-w-6xl flex-col">
        <div className="mb-6 shrink-0">
          <Text type="secondary">Lịch sử tư vấn</Text>
          <Title level={2} className="mb-1! mt-1!">
            Phiên chat của tôi
          </Title>
          <Text type="secondary">
            Xem lại các cuộc trò chuyện với trợ lý GastroAI và tiếp tục nơi bạn dừng lại.
          </Text>
        </div>

        {deleteError && <Alert type="error" message={deleteError} showIcon className="mb-4 shrink-0" />}

        <div className="flex min-h-0 flex-1 gap-6">
          {/* ── Cột trái: danh sách phiên ── */}
          <aside className="flex min-h-0 w-80 shrink-0 flex-col">
            <div className="flex h-full min-h-0 flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
              <div className="flex shrink-0 items-center gap-2 border-b border-slate-100 px-4 py-3">
                <ClockCircleOutlined className="text-teal-600" />
                <span className="font-medium text-slate-700 text-sm">Danh sách phiên</span>
              </div>

              {loadingSessions ? (
                <div className="flex min-h-0 flex-1 items-center justify-center py-16">
                  <Spin indicator={<LoadingOutlined className="text-teal-600 text-xl" spin />} />
                </div>
              ) : sessionsError ? (
                <div className="min-h-0 flex-1 p-4">
                  <Alert type="error" message={sessionsError} showIcon />
                </div>
              ) : sessions.length === 0 ? (
                <div className="flex min-h-0 flex-1 flex-col items-center justify-center px-4 py-16 text-center">
                  <MessageOutlined className="text-3xl text-slate-300 mb-3" />
                  <Text type="secondary" className="text-sm">
                    Chưa có phiên chat nào. Hãy bắt đầu tư vấn!
                  </Text>
                </div>
              ) : (
                <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
                  {sessions.map((session) => (
                    <HistorySessionItem
                      key={session.id}
                      session={session}
                      isSelected={selectedSession?.id === session.id}
                      onClick={() => void handleSelectSession(session)}
                      onDelete={() => void handleDeleteSession(session)}
                      deleting={deletingSessionId === session.id}
                    />
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
