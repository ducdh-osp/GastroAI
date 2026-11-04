import { SendOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Input, Spin, Typography, message } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  getTriageConversation,
  replyTriageConversation,
  type TriageConversation,
} from '../../api/patientTriageAlerts'
import { AppShell } from '../../components/layout/AppShell'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography
const { TextArea } = Input

export default function TriageConversationPage() {
  const { id } = useParams<{ id: string }>()
  const alertId = Number(id)
  const [conversation, setConversation] = useState<TriageConversation | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const sendGuard = useInFlightGuard<number>()
  const requestId = useRef(0)
  const bottomRef = useRef<HTMLDivElement>(null)

  const load = useCallback((silent: boolean) => {
    if (!Number.isFinite(alertId)) {
      setError('Không tìm thấy cuộc trao đổi.')
      setLoading(false)
      return
    }
    const currentRequestId = ++requestId.current
    if (!silent) setLoading(true)
    getTriageConversation(alertId)
      .then((data) => {
        if (currentRequestId !== requestId.current) return
        setConversation(data)
        setError(null)
        // BE vừa đánh dấu đã xem -> để chuông thông báo cập nhật số chưa đọc.
        window.dispatchEvent(new Event('gastroai:notification-state'))
      })
      .catch((err: unknown) => {
        if (currentRequestId !== requestId.current) return
        setError(err instanceof Error ? err.message : 'Không thể tải cuộc trao đổi.')
      })
      .finally(() => {
        if (currentRequestId === requestId.current) setLoading(false)
      })
  }, [alertId])

  useEffect(() => { load(false) }, [load])

  // Thông báo realtime (AppSidebar phát 'gastroai:notification') -> tải lại tin mới.
  useEffect(() => {
    const refresh = () => load(true)
    window.addEventListener('gastroai:notification', refresh)
    return () => window.removeEventListener('gastroai:notification', refresh)
  }, [load])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' })
  }, [conversation?.messages.length])

  async function handleSend() {
    const content = draft.trim()
    if (!content) return
    await sendGuard.run(alertId, async () => {
      try {
        await replyTriageConversation(alertId, content)
        setDraft('')
        load(true)
      } catch (err) {
        message.error(err instanceof Error ? err.message : 'Không thể gửi tin nhắn.')
      }
    })
  }

  const sending = sendGuard.inFlightId !== null
  const closed = conversation?.status === 'RESOLVED'

  return (
    <AppShell>
      <div className="flex h-[calc(100svh-3rem)] min-h-0 flex-col gap-4 overflow-hidden sm:h-[calc(100svh-4rem)]">
        <div className="shrink-0">
          <Text type="secondary">Cảnh báo khẩn cấp</Text>
          <Title level={2} className="mb-1! mt-1!">Trao đổi với bác sĩ</Title>
          {conversation && (
            <Text type="secondary">{formatDateTime(conversation.occurredAt)}</Text>
          )}
        </div>

        {error && <Alert type="error" showIcon title={error} className="shrink-0" />}
        {closed && <Alert type="info" showIcon title="Cuộc trao đổi đã kết thúc" className="shrink-0" />}

        <Card
          className="min-h-0 flex-1 overflow-hidden rounded-2xl border-black/5 shadow-sm dark:border-white/10"
          styles={{ body: { display: 'flex', height: '100%', minHeight: 0, flexDirection: 'column', gap: 12 } }}
        >
          <Spin spinning={loading} wrapperClassName="min-h-0 flex-1 overflow-y-auto">
            <div className="flex flex-col gap-3 p-1">
              {conversation && conversation.messages.length === 0 && (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có tin nhắn" />
              )}
              {conversation?.messages.map((item) => (
                <div key={item.id} className={`flex ${item.mine ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[80%] rounded-2xl px-4 py-2 ${item.mine
                    ? 'bg-teal-600 text-white'
                    : 'bg-slate-100 text-slate-900 dark:bg-neutral-800 dark:text-slate-100'}`}>
                    {!item.mine && (
                      <div className="mb-1 text-xs font-semibold text-teal-700 dark:text-teal-400">{item.senderLabel}</div>
                    )}
                    <div className="whitespace-pre-wrap break-words">{item.content}</div>
                    <div className={`mt-1 text-xs ${item.mine ? 'text-white/80' : 'text-slate-500 dark:text-slate-400'}`}>
                      {formatDateTime(item.createdAt)}
                    </div>
                  </div>
                </div>
              ))}
              <div ref={bottomRef} />
            </div>
          </Spin>

          {conversation?.canReply && (
            <div className="flex shrink-0 flex-col gap-2">
              <TextArea
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Nhập tin nhắn"
                autoSize={{ minRows: 2, maxRows: 5 }}
                maxLength={2000}
                showCount
                disabled={sending}
              />
              <div className="flex justify-end">
                <Button type="primary" icon={<SendOutlined />} loading={sending}
                  disabled={draft.trim().length === 0} onClick={handleSend}>
                  Gửi
                </Button>
              </div>
            </div>
          )}
        </Card>
      </div>
    </AppShell>
  )
}
