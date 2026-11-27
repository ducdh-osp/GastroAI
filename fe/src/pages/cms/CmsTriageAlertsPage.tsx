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
  TRIAGE_OUTCOME_LABELS,
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
import { useNow } from '../../hooks/useNow'
import { formatDateTime } from '../../lib/format'
import { FAILSAFE_AFTER_MS, formatCountdown, INTERVENTION_AFTER_MS, msUntil } from '../../lib/triageTimers'

const { Title, Text } = Typography

const PAGE_SIZE = 20

/** SCR-AD-13: 3 tab, "Cần can thiệp" đầu tiên - đó là toàn bộ việc Admin phải làm. */
type TabView = Extract<TriageAlertView, 'INTERVENTION' | 'AUTO' | 'CLOSED'>

const CLAIMER_TYPE_LABELS: Record<string, string> = { ADMIN: 'Admin', DOCTOR: 'Bác sĩ' }

interface OpenCounts {
  intervention: number
  auto: number
}

export default function CmsTriageAlertsPage() {
  const { isAuthenticated } = useCmsAuth()
  const [view, setView] = useState<TabView>('INTERVENTION')
  const [counts, setCounts] = useState<OpenCounts | null>(null)
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

  // Số ca ở 2 tab còn mở - hiện trên tab để Admin thấy ngay có việc hay không.
  const loadCounts = useCallback(() => {
    Promise.all([listTriageAlerts('INTERVENTION', 0, 1), listTriageAlerts('AUTO', 0, 1)])
      .then(([intervention, auto]) => setCounts({
        intervention: intervention.totalElements,
        auto: auto.totalElements,
      }))
      .catch(() => undefined)
  }, [])

  const loadSeq = useRef(0)
  const loadAlerts = useCallback((silent = false) => {
    const mySeq = ++loadSeq.current
    if (!silent) setLoading(true)
    loadCounts()
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
  }, [loadCounts])

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
        message.success(updated.status === 'NEEDS_INTERVENTION' && view === 'AUTO'
          ? 'Đã chuyển sang Cần can thiệp'
          : 'Đã đánh dấu bạn đang liên hệ')
        loadAlerts(true)
      } catch (err) {
        message.error(extractTriageErrorMessage(err, 'Không thể nhận ca này.'))
      }
    })
  }

  // Đồng hồ đếm ngược chỉ chạy khi đang xem ca còn mở.
  const now = useNow(view !== 'CLOSED' && alerts.length > 0)

  const handleAlertChanged = useCallback((updated: TriageAlert) => {
    setAlerts((prev) => prev.map((a) => (a.id === updated.id ? updated : a)))
  }, [])

  const columns: ColumnsType<TriageAlert> = [
    {
      title: <span className="whitespace-nowrap">Hoạt động gần nhất</span>,
      key: 'lastActivity',
      width: 180,
      render: (_, record) => (
        <span className="whitespace-nowrap">{formatDateTime(record.lastRepeatedAt ?? record.occurredAt)}</span>
      ),
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
      width: 230,
      render: (_, record) => <StatusCell alert={record} now={now} />,
    },
    {
      title: <span className="whitespace-nowrap">Tin mới</span>,
      key: 'unread',
      width: 90,
      align: 'center',
      render: (_, record) => (record.unreadPatientMessages > 0
        ? <Badge count={record.unreadPatientMessages} />
        : null),
    },
    {
      title: '', key: 'actions', width: 150,
      render: (_, record) => {
        if (record.status === 'RESOLVED' || record.claimedById !== null) return null
        const busy = actionGuard.inFlightId === record.id
        const disabled = actionGuard.inFlightId !== null && actionGuard.inFlightId !== record.id
        return (
          <Button
            size="small"
            loading={busy}
            disabled={disabled}
            type={record.status === 'AUTO_HANDLING' ? 'default' : 'primary'}
            onClick={(event) => {
              event.stopPropagation()
              void handleClaim(record.id)
            }}
          >
            {record.status === 'AUTO_HANDLING' ? 'Can thiệp ngay' : 'Tôi đang liên hệ'}
          </Button>
        )
      },
    },
  ]

  const overdueCount = view === 'INTERVENTION' ? alerts.filter((alert) => alert.failsafeAt && !alert.acknowledgedAt).length : 0
  const tabOptions: { label: string; value: TabView }[] = [
    { label: `Cần can thiệp${counts ? ` (${counts.intervention})` : ''}`, value: 'INTERVENTION' },
    { label: `Hệ thống đang tự xử lý${counts ? ` (${counts.auto})` : ''}`, value: 'AUTO' },
    { label: 'Đã xử lý', value: 'CLOSED' },
  ]

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
              <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                {connectionState !== 'connected' && (
                  <Badge
                    className="whitespace-nowrap"
                    status={connectionState === 'connecting' ? 'processing' : 'error'}
                    text={connectionState === 'connecting' ? 'Đang kết nối...' : 'Mất kết nối realtime - đang thử lại'}
                  />
                )}
              </div>
            </div>
            <Segmented<TabView>
              options={tabOptions}
              value={view}
              onChange={(value) => {
                setView(value)
                setPage(0)
              }}
            />
          </div>
        </div>

        {loadError && <Alert type="error" showIcon title={loadError} className="shrink-0" />}

        {!loadError && counts !== null && counts.intervention > 0 && (
          <Alert
            className="shrink-0"
            type="error"
            showIcon
            title={`${counts.intervention} ca cần can thiệp${overdueCount > 0 ? ` · ${overdueCount} ca QUÁ HẠN` : ''}`}
          />
        )}

        <Card
          className="min-h-0 flex-1 overflow-hidden rounded-2xl border-black/5 shadow-sm dark:border-neutral-700! dark:bg-neutral-800!"
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
              className: record.failsafeAt && !record.acknowledgedAt && record.status === 'NEEDS_INTERVENTION'
                ? 'cursor-pointer bg-red-50 dark:bg-red-950/30'
                : 'cursor-pointer',
            })}
            scroll={{ x: 990 }}
            tableLayout="fixed"
            locale={{
              emptyText: loadError
                ? 'Lỗi tải dữ liệu'
                : view === 'INTERVENTION' ? 'Không có ca nào cần can thiệp' : 'Chưa có cảnh báo nào',
            }}
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

/**
 * Ô trạng thái: nói rõ ca đang ở bước nào của SCR-AD-13 và còn bao lâu tới bước kế tiếp,
 * để Admin không phải mở từng ca mới biết.
 */
function StatusCell({ alert, now }: { alert: TriageAlert; now: number }) {
  if (alert.status === 'RESOLVED') {
    return (
      <div>
        <Tag color={TRIAGE_STATUS_COLORS.RESOLVED}>{TRIAGE_STATUS_LABELS.RESOLVED}</Tag>
        {alert.outcome && (
          <Tag color={alert.outcome === 'FALSE_ALARM' ? 'default' : 'green'}>{TRIAGE_OUTCOME_LABELS[alert.outcome]}</Tag>
        )}
      </div>
    )
  }

  if (alert.status === 'AUTO_HANDLING') {
    const remaining = msUntil(alert.occurredAt, INTERVENTION_AFTER_MS, now)
    return (
      <div>
        <Tag color={TRIAGE_STATUS_COLORS.AUTO_HANDLING}>{TRIAGE_STATUS_LABELS.AUTO_HANDLING}</Tag>
        <Text type="secondary" className="block text-xs tabular-nums">
          Đang chờ bệnh nhân trả lời · chuyển Admin sau {formatCountdown(remaining ?? 0)}
        </Text>
      </div>
    )
  }

  const overdue = alert.failsafeAt !== null && alert.acknowledgedAt === null
  const failsafeIn = alert.acknowledgedAt === null && alert.failsafeAt === null
    ? msUntil(alert.needsInterventionAt, FAILSAFE_AFTER_MS, now)
    : null
  return (
    <div>
      <Tag color={TRIAGE_STATUS_COLORS.NEEDS_INTERVENTION}>{TRIAGE_STATUS_LABELS.NEEDS_INTERVENTION}</Tag>
      {overdue && <Tag color="red" className="font-semibold">QUÁ HẠN</Tag>}
      {alert.patientResponse === 'NEED_HELP' && <Tag color="volcano">Bệnh nhân bấm Cần hỗ trợ</Tag>}
      <Text type="secondary" className="block text-xs tabular-nums">
        {alert.claimedById !== null
          ? `${CLAIMER_TYPE_LABELS[alert.claimedByType ?? ''] ?? alert.claimedByType} #${alert.claimedById} đang liên hệ`
          : alert.acknowledgedAt
            ? 'Đã có Admin xử lý'
            : failsafeIn !== null
              ? `Chưa ai xử lý · failsafe sau ${formatCountdown(failsafeIn)}`
              : 'Chưa ai xử lý'}
      </Text>
    </div>
  )
}
