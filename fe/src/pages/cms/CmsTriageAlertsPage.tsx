import { CheckOutlined, SafetyOutlined } from '@ant-design/icons'
import { Alert, Badge, Button, Card, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  claimTriageAlert,
  extractTriageErrorMessage,
  listTriageAlerts,
  resolveTriageAlert,
  type TriageAlert,
  type TriageAlertStatus,
} from '../../api/cmsTriage'
import { describeMatchedGroups } from '../../api/chat'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { useCmsAuth } from '../../stores/cmsAuthStore'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import {
  useTriageAlertSocket,
  type TriageAlertSocketEvent,
  type TriageAlertStatusChangedSocketEvent,
} from '../../hooks/useTriageAlertSocket'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

const STATUS_LABELS: Record<TriageAlertStatus, string> = {
  NEW: 'Mới',
  IN_PROGRESS: 'Đang xử lý',
  RESOLVED: 'Đã xử lý',
}

const STATUS_COLORS: Record<TriageAlertStatus, string> = {
  NEW: 'red',
  IN_PROGRESS: 'gold',
  RESOLVED: 'green',
}

const CLAIMER_TYPE_LABELS: Record<string, string> = { ADMIN: 'Admin', DOCTOR: 'Bác sĩ' }


function upsertAlert(alerts: TriageAlert[], event: TriageAlertSocketEvent): TriageAlert[] {
  const incoming: TriageAlert = {
    id: event.id,
    patientId: event.patientId,
    patientFullName: event.patientFullName,
    patientPhone: event.patientPhone,
    sessionId: event.sessionId,
    messageId: event.messageId,
    messageContent: event.messageContent,
    matchedGroups: event.matchedGroups,
    status: event.status,
    
    claimedById: alerts.find((a) => a.id === event.id)?.claimedById ?? null,
    claimedByType: alerts.find((a) => a.id === event.id)?.claimedByType ?? null,
    claimedAt: alerts.find((a) => a.id === event.id)?.claimedAt ?? null,
    resolvedAt: alerts.find((a) => a.id === event.id)?.resolvedAt ?? null,
    occurredAt: event.occurredAt,
  }
  const withoutIncoming = alerts.filter((a) => a.id !== event.id)
  return [incoming, ...withoutIncoming].sort(
    (a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime(),
  )
}


function applyStatusChange(alerts: TriageAlert[], event: TriageAlertStatusChangedSocketEvent): TriageAlert[] {
  return alerts.map((alert) =>
    alert.id === event.id
      ? {
          ...alert,
          status: event.status,
          claimedById: event.claimedById,
          claimedByType: event.claimedByType,
          claimedAt: event.status === 'IN_PROGRESS' ? event.statusChangedAt : alert.claimedAt,
          resolvedAt: event.status === 'RESOLVED' ? event.statusChangedAt : alert.resolvedAt,
        }
      : alert,
  )
}

function mergeAlerts(fromApi: TriageAlert[], current: TriageAlert[], touchedIds: Set<number>): TriageAlert[] {
  const currentById = new Map(current.map((a) => [a.id, a]))
  const merged = fromApi.map((apiAlert) =>
    touchedIds.has(apiAlert.id) ? (currentById.get(apiAlert.id) ?? apiAlert) : apiAlert,
  )
  const apiIds = new Set(fromApi.map((a) => a.id))
  const onlyOnScreen = current.filter((a) => !apiIds.has(a.id))
  return [...merged, ...onlyOnScreen].sort(
    (a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime(),
  )
}

export default function CmsTriageAlertsPage() {
  const { isAuthenticated } = useCmsAuth()
  const [alerts, setAlerts] = useState<TriageAlert[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const actionGuard = useInFlightGuard<number>()
  const touchedDuringLoad = useRef(new Set<number>())

  const loadAlerts = useCallback((silent = false) => {
    if (!silent) setLoading(true)
    touchedDuringLoad.current.clear()
    listTriageAlerts()
      .then((fromApi) => {
        setAlerts((current) => mergeAlerts(fromApi, current, touchedDuringLoad.current))
        setLoadError(null)
      })
      .catch((err: unknown) => {
        // Lan tai im lang that bai thi khong bao loi - cham trang thai "mat ket noi
        // realtime" da bao cho admin biet roi, khong can bat/tat Alert moi 30s.
        if (!silent) {
          setLoadError(extractTriageErrorMessage(err, 'Không thể tải danh sách cảnh báo.'))
        }
      })
      .finally(() => { if (!silent) setLoading(false) })
  }, [])

  useEffect(() => { loadAlerts() }, [loadAlerts])

  const { connectionState } = useTriageAlertSocket({
    enabled: isAuthenticated,
    onAlert: useCallback((event: TriageAlertSocketEvent) => {
      touchedDuringLoad.current.add(event.id)
      setAlerts((prev) => upsertAlert(prev, event))
    }, []),
    onStatusChange: useCallback((event: TriageAlertStatusChangedSocketEvent) => {
      touchedDuringLoad.current.add(event.id)
      setAlerts((prev) => applyStatusChange(prev, event))
    }, []),
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
        setActionError(null)
      } catch (err) {
        setActionError(extractTriageErrorMessage(err, 'Không thể tiếp nhận cảnh báo này.'))
      }
    })
  }

  async function handleResolve(alertId: number) {
    await actionGuard.run(alertId, async () => {
      try {
        const updated = await resolveTriageAlert(alertId)
        setAlerts((prev) => prev.map((a) => (a.id === alertId ? updated : a)))
        setActionError(null)
      } catch (err) {
        setActionError(extractTriageErrorMessage(err, 'Không thể đánh dấu đã xử lý.'))
      }
    })
  }

  const unresolvedCount = alerts.filter((a) => a.status !== 'RESOLVED').length

  const columns: ColumnsType<TriageAlert> = [
    { title: 'Thời gian', dataIndex: 'occurredAt', key: 'occurredAt', width: 150, render: formatDateTime },
    {
      title: 'Bệnh nhân',
      key: 'patient',
      width: 200,
      render: (_, record) => (
        <div className="min-w-0">
          <Text strong>{record.patientFullName ?? `Bệnh nhân #${record.patientId}`}</Text>
          <div><Text type="secondary">{record.patientPhone ?? '—'}</Text></div>
        </div>
      ),
    },
    {
      title: 'Nội dung & dấu hiệu',
      key: 'content',
      render: (_, record) => (
        <div className="min-w-0">
          <Text className="block truncate" title={record.messageContent}>{record.messageContent}</Text>
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
          <Tag color={STATUS_COLORS[record.status]}>{STATUS_LABELS[record.status]}</Tag>
          {record.claimedByType && (
            <div>
              <Text type="secondary" className="text-xs">
                {CLAIMER_TYPE_LABELS[record.claimedByType] ?? record.claimedByType} tiếp nhận
              </Text>
            </div>
          )}
        </div>
      ),
    },
    {
      title: '', key: 'actions', width: 150,
      render: (_, record) => {
        const busy = actionGuard.inFlightId === record.id
        const disabled = actionGuard.inFlightId !== null && actionGuard.inFlightId !== record.id
        if (record.status === 'RESOLVED') return null
        if (record.status === 'NEW') {
          return (
            <Button size="small" type="primary" loading={busy} disabled={disabled} onClick={() => handleClaim(record.id)}>
              Tiếp nhận
            </Button>
          )
        }
        return (
          <Button size="small" icon={<CheckOutlined />} loading={busy} disabled={disabled} onClick={() => handleResolve(record.id)}>
            Đánh dấu xong
          </Button>
        )
      },
    },
  ]

  return (
    <CmsAppShell>
      <div className="flex h-[calc(100svh-3rem)] min-h-0 flex-col gap-4 overflow-hidden sm:h-[calc(100svh-4rem)]">
        <div className="shrink-0">
          <div className="flex items-center justify-between gap-3">
            <div>
              <Text type="secondary">Giám sát</Text>
              <Title level={2} className="mb-1! mt-1!">
                <SafetyOutlined className="mr-2" />
                Cảnh báo khẩn cấp
              </Title>
              <Text type="secondary">Tin nhắn trợ lý AI nhận diện có dấu hiệu khẩn cấp (UC0034/036).</Text>
            </div>
            <Badge
              status={connectionState === 'connected' ? 'success' : connectionState === 'connecting' ? 'processing' : 'error'}
              text={
                connectionState === 'connected'
                  ? 'Đang nhận cảnh báo realtime'
                  : connectionState === 'connecting'
                    ? 'Đang kết nối...'
                    : 'Mất kết nối realtime - đang thử lại'
              }
            />
          </div>
        </div>

        {loadError && <Alert type="error" showIcon message={loadError} className="shrink-0" />}
        {actionError && (
          <Alert
            type="error"
            showIcon
            closable
            onClose={() => setActionError(null)}
            message={actionError}
            className="shrink-0"
          />
        )}

        {!loadError && !loading && unresolvedCount > 0 && (
          <Alert
            className="shrink-0"
            type="warning"
            showIcon
            message={`${unresolvedCount} cảnh báo chưa xử lý xong`}
            description="Vui lòng tiếp nhận và liên hệ bệnh nhân sớm nhất có thể."
          />
        )}

        <Card
          className="min-h-0 flex-1 overflow-hidden rounded-2xl border-black/5 shadow-sm"
          styles={{ body: { display: 'flex', height: '100%', minHeight: 0, flexDirection: 'column', overflow: 'hidden' } }}
        >
          <Spin spinning={loading}>
            <Table<TriageAlert>
              columns={columns}
              dataSource={alerts}
              rowKey="id"
              pagination={false}
              scroll={{ x: 900 }}
              tableLayout="fixed"
              locale={{ emptyText: loadError ? 'Lỗi tải dữ liệu' : 'Chưa có cảnh báo khẩn cấp nào' }}
            />
          </Spin>
        </Card>
      </div>
    </CmsAppShell>
  )
}