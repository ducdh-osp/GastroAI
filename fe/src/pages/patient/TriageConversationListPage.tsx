import { Alert, Badge, Card, Empty, Spin, Tag, Typography } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listTriageConversations, type TriageConversationSummary } from '../../api/patientTriageAlerts'
import { AppShell } from '../../components/layout/AppShell'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

export default function TriageConversationListPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState<TriageConversationSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const requestId = useRef(0)

  const load = useCallback((silent: boolean) => {
    const currentRequestId = ++requestId.current
    if (!silent) setLoading(true)
    listTriageConversations()
      .then((data) => {
        if (currentRequestId !== requestId.current) return
        setItems(data)
        setError(null)
      })
      .catch((err: unknown) => {
        if (currentRequestId !== requestId.current) return
        setError(err instanceof Error ? err.message : 'Không thể tải danh sách trao đổi.')
      })
      .finally(() => {
        if (currentRequestId === requestId.current) setLoading(false)
      })
  }, [])

  useEffect(() => { load(false) }, [load])

  useEffect(() => {
    const refresh = () => load(true)
    window.addEventListener('gastroai:notification', refresh)
    return () => window.removeEventListener('gastroai:notification', refresh)
  }, [load])

  return (
    <AppShell>
      <div className="flex h-[calc(100svh-3rem)] min-h-0 flex-col gap-4 overflow-hidden sm:h-[calc(100svh-4rem)]">
        <div className="shrink-0">
          <Text type="secondary">Cảnh báo khẩn cấp</Text>
          <Title level={2} className="mb-1! mt-1!">Trao đổi với bác sĩ</Title>
        </div>

        {error && <Alert type="error" showIcon title={error} className="shrink-0" />}

        <Spin spinning={loading} wrapperClassName="min-h-0 flex-1 overflow-y-auto">
          {!loading && !error && items.length === 0 && (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có trao đổi nào" className="mt-12" />
          )}
          <div className="flex flex-col gap-3">
            {items.map((item) => {
              const closed = item.status === 'RESOLVED'
              return (
                <Card
                  key={item.id}
                  hoverable
                  onClick={() => navigate(`/triage-alerts/${item.id}`)}
                  className="rounded-2xl border-black/5 shadow-sm dark:border-white/10"
                  styles={{ body: { padding: 16 } }}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <Text strong>{formatDateTime(item.occurredAt)}</Text>
                        <Tag color={closed ? 'default' : 'green'}>{closed ? 'Đã kết thúc' : 'Đang trao đổi'}</Tag>
                      </div>
                      <Text
                        type="secondary"
                        className={`mt-1 block truncate ${item.unreadCount > 0 ? 'font-semibold text-slate-900! dark:text-slate-100!' : ''}`}
                        title={item.lastMessagePreview ?? undefined}
                      >
                        {item.lastMessagePreview ?? '—'}
                      </Text>
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-2">
                      <Text type="secondary" className="text-xs">{formatDateTime(item.lastMessageAt)}</Text>
                      {item.unreadCount > 0 && <Badge count={item.unreadCount} overflowCount={99} />}
                    </div>
                  </div>
                </Card>
              )
            })}
          </div>
        </Spin>
      </div>
    </AppShell>
  )
}
