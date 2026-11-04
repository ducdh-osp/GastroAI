import { SafetyOutlined } from '@ant-design/icons'
import { Alert, Badge, Button, Card, Segmented, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  claimTriageAlert,
  extractTriageErrorMessage,
  listTriageAlerts,
  type TriageAlert,
  type TriageAlertView,
} from '../../api/cmsTriage'
import { describeMatchedGroups } from '../../api/chat'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import {
  TRIAGE_STATUS_COLORS,
  TRIAGE_STATUS_LABELS,
  TriageAlertDetailDrawer,
} from '../../components/cms/TriageAlertDetailDrawer'
import { useCmsAuth } from '../../stores/cmsAuthStore'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import {
  useTriageAlertSocket,
  type TriageAlertActivitySocketEvent,
  type TriageAlertStatusChangedSocketEvent,
} from '../../hooks/useTriageAlertSocket'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

const PAGE_SIZE = 20

const VIEW_OPTIONS: { label: string; value: TriageAlertView }[] = [
  { label: 'Đang mở', value: 'OPEN' },
  { label: 'Đã đóng', value: 'CLOSED' },
  { label: 'Tất cả', value: 'ALL' },
]

const CLAIMER_TYPE_LABELS: Record<string, string> = { ADMIN: 'Admin', DOCTOR: 'Bác sĩ' }

export default function CmsTriageAlertsPage() {
  const { isAuthenticated } = useCmsAuth()
  const [view, setView] = useState<TriageAlertView>('OPEN')
  const [page, setPage] = useState(0)
  const [alerts, setAlerts] = useState<TriageAlert[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [drawerReloadKey, setDrawerReloadKey] = useState(0)
  const actionGuard = useInFlightGuard<number>()

  // Ref de callback realtime luon doc duoc view/page/cảnh báo đang mở moi nhat.
  const queryRef = useRef({ view, page })
  queryRef.current = { view, page }
  const selectedIdRef = useRef(selectedId)
  selectedIdRef.current = selectedId

  const loadSeq = useRef(0)
  const loadAlerts = useCallback((silent = false) => {
    const mySeq = ++loadSeq.current
    if (!silent) setLoading(true)
    const { view: currentView, page: currentPage } = queryRef.current
    listTriageAlerts(currentView, currentPage, PAGE_SIZE)
      .then((result) => {
        if (mySeq !== loadSeq.current) return
        setAlerts(result.items)
        setTotal(result.totalElements)
        setLoadError(null)
      })
      .catch((err: unknown) => {
        if (mySeq !== loadSeq.current) return
        if (!silent) {
          setLoadError(extractTriageErrorMessage(err, 'Không thể tải danh sách cảnh báo.'))
        }
      })
      .finally(() => {
        if (mySeq === loadSeq.current) setLoading(false)
      })
  }, [])

  useEffect(() => { loadAlerts() }, [view, page, loadAlerts])

  const refreshFor = useCallback((alertId: number) => {
    loadAlerts(true)
    if (selectedIdRef.current === alertId) setDrawerReloadKey((key) => key + 1)
  }, [loadAlerts])

  const { connectionState } = useTriageAlertSocket({
    enabled: isAuthenticated,
    onAlert: useCallback(() => loadAlerts(true), [loadAlerts]),
    onStatusChange: useCallback(
      (event: TriageAlertStatusChangedSocketEvent) => refreshFor(event.id), [refreshFor]),
    onActivity: useCallback(
      (event: TriageAlertActivitySocketEvent) => refreshFor(event.alertId), [refreshFor]),
    onConnected: useCallback(() => loadAlerts(true), [loadAlerts]),
  })
  useEffect(() => {
    if (connectionState === 'connected') return
    const intervalId = setInterval(() => loadAlerts(true), 30000)
    return () => clearInterval(intervalId)
  }, [connectionState, loadAlerts])

  async function handleClaim(alertId: number) {
    await actionGuard.run(alertId, async () => {
      try {
        const updated = await claimTriageAlert(alertId)
        setAlerts((prev) => prev.map((a) => (a.id === alertId ? updated : a)))
        message.success('Đã tiếp nhận cảnh báo')
      } catch (err) {
        message.error(extractTriageErrorMessage(err, 'Không thể tiếp nhận cảnh báo này.'))
      }
    })
  }

  const handleAlertChanged = useCallback((updated: TriageAlert) => {
    setAlerts((prev) => prev.map((a) => (a.id === updated.id ? updated : a)))
  }, [])

  const columns: ColumnsType<TriageAlert> = [
    {
      title: 'Hoạt động gần nhất',
      key: 'lastActivity',
      width: 150,
      render: (_, record) => formatDateTime(record.lastRepeatedAt ?? record.occurredAt),
    },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 210,
      render: (_, record) => (
        <div className="min-w-0">
          <Text strong>{record.patientFullName ?? `Bệnh nhân #${record.patientId}`}</Text>
          <div><Text type="secondary">{record.patientPhone ?? '—'}</Text></div>
          {record.patientFalseAlarmCount > 0 && (
            <Tag color="orange" className="mt-1">{record.patientFalseAlarmCount} lần báo nhầm trước đây</Tag>
          )}
        </div>
      ),
    },
    {
      title: 'Nội dung & dấu hiệu',
      key: 'content',
      render: (_, record) => (
        <div className="min-w-0">
          <Text className="block truncate" title={record.messageContent}>{record.messageContent}</Text>
          {record.repeatCount > 0 && (
            <Text type="secondary" className="block truncate text-xs" title={record.lastMessageContent ?? undefined}>
              <Tag color="red" className="mr-1">Lặp lại ×{record.repeatCount}</Tag>
              {record.lastMessageContent}
            </Text>
          )}
          {describeMatchedGroups(record.matchedGroups) && (
            <Text type="danger" className="block text-xs">{describeMatchedGroups(record.matchedGroups)}</Text>
          )}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 190,
      render: (_, record) => (
        <div>
          <Tag color={TRIAGE_STATUS_COLORS[record.status]}>{TRIAGE_STATUS_LABELS[record.status]}</Tag>
          {record.outcome === 'HANDLED' && <Tag color="green">Xử lý xong</Tag>}
          {record.outcome === 'FALSE_ALARM' && <Tag>Báo nhầm/Spam</Tag>}
          {record.claimedByType && (
            <div>
              <Text type="secondary" className="text-xs">
                {CLAIMER_TYPE_LABELS[record.claimedByType] ?? record.claimedByType} #{record.claimedById} tiếp nhận
              </Text>
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Tin mới',
      key: 'unread',
      width: 90,
      align: 'center',
      render: (_, record) => (record.unreadPatientMessages > 0
        ? <Badge count={record.unreadPatientMessages} />
        : null),
    },
    {
      title: '', key: 'actions', width: 120,
      render: (_, record) => {
        if (record.status !== 'NEW') return null
        const busy = actionGuard.inFlightId === record.id
        const disabled = actionGuard.inFlightId !== null && actionGuard.inFlightId !== record.id
        return (
          <Button
            size="small"
            type="primary"
            loading={busy}
            disabled={disabled}
            onClick={(event) => {
              event.stopPropagation()
              void handleClaim(record.id)
            }}
          >
            Tiếp nhận
          </Button>
        )
      },
    },
  ]

  const openCount = view === 'OPEN' ? total : null

  return (
    <CmsAppShell>
      <div className="flex h-[calc(100svh-3rem)] min-h-0 flex-col gap-4 overflow-hidden sm:h-[calc(100svh-4rem)]">
        <div className="shrink-0">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <Text type="secondary">Giám sát</Text>
              <Title level={2} className="mb-1! mt-1!">
                <SafetyOutlined className="mr-2" />
                Cảnh báo khẩn cấp
              </Title>
              <Text type="secondary">Tin nhắn trợ lý AI nhận diện có dấu hiệu khẩn cấp</Text>
              {connectionState !== 'connected' && (
                <Badge
                  status={connectionState === 'connecting' ? 'processing' : 'error'}
                  text={connectionState === 'connecting' ? 'Đang kết nối...' : 'Mất kết nối realtime - đang thử lại'}
                />
              )}
            </div>
            <Segmented<TriageAlertView>
              options={VIEW_OPTIONS}
              value={view}
              onChange={(value) => {
                setView(value)
                setPage(0)
              }}
            />
          </div>
        </div>

        {loadError && <Alert type="error" showIcon title={loadError} className="shrink-0" />}

        {!loadError && !loading && openCount !== null && openCount > 0 && (
          <Alert className="shrink-0" type="warning" showIcon title={`${openCount} cảnh báo đang mở`} />
        )}

        <Card
          className="min-h-0 flex-1 overflow-hidden rounded-2xl border-black/5 shadow-sm"
          styles={{ body: { display: 'flex', height: '100%', minHeight: 0, flexDirection: 'column', overflow: 'auto' } }}
        >
          <Table<TriageAlert>
            columns={columns}
            dataSource={alerts}
            rowKey="id"
            loading={loading}
            pagination={{
              current: page + 1,
              pageSize: PAGE_SIZE,
              total,
              showSizeChanger: false,
              onChange: (nextPage) => setPage(nextPage - 1),
            }}
            onRow={(record) => ({
              onClick: () => setSelectedId(record.id),
              className: 'cursor-pointer',
            })}
            scroll={{ x: 960 }}
            tableLayout="fixed"
            locale={{ emptyText: loadError ? 'Lỗi tải dữ liệu' : 'Chưa có cảnh báo khẩn cấp nào' }}
          />
        </Card>
      </div>

      <TriageAlertDetailDrawer
        alertId={selectedId}
        reloadKey={drawerReloadKey}
        onClose={() => setSelectedId(null)}
        onAlertChanged={handleAlertChanged}
      />
    </CmsAppShell>
  )
}
