import { CheckOutlined, RollbackOutlined, SendOutlined, StopOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Empty,
  Input,
  Modal,
  Popconfirm,
  Space,
  Spin,
  Tag,
  Timeline,
  Tooltip,
  Typography,
  message,
} from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  claimTriageAlert,
  extractTriageErrorMessage,
  getTriageAlertDetail,
  markTriageAlertFalseAlarm,
  releaseTriageAlert,
  resolveTriageAlert,
  sendTriageAlertMessage,
  type TriageAlert,
  type TriageAlertActionItem,
  type TriageAlertActionType,
  type TriageAlertDetail,
  type TriageAlertStatus,
} from '../../api/cmsTriage'
import { describeMatchedGroups } from '../../api/chat'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { formatDateTime } from '../../lib/format'
import { useCmsAuth } from '../../stores/cmsAuthStore'

const { Text, Paragraph } = Typography
const { TextArea } = Input

export const TRIAGE_STATUS_LABELS: Record<TriageAlertStatus, string> = {
  NEW: 'Mới',
  IN_PROGRESS: 'Đang xử lý',
  RESOLVED: 'Đã đóng',
}

export const TRIAGE_STATUS_COLORS: Record<TriageAlertStatus, string> = {
  NEW: 'red',
  IN_PROGRESS: 'gold',
  RESOLVED: 'green',
}

const ACTION_LABELS: Record<TriageAlertActionType, string> = {
  CREATED: 'Cảnh báo được tạo',
  REPEATED: 'Bệnh nhân gửi lặp lại',
  REPEATED_AFTER_FALSE_ALARM: 'Bệnh nhân gửi lại sau khi bị báo nhầm',
  CLAIMED: 'Tiếp nhận',
  RELEASED: 'Trả lại',
  MESSAGE_SENT: 'Nhắn tin cho bệnh nhân',
  PATIENT_REPLIED: 'Bệnh nhân trả lời',
  PATIENT_READ: 'Bệnh nhân đã xem',
  RESOLVED: 'Xử lý xong',
  FALSE_ALARM: 'Báo nhầm/Spam',
}

const ACTION_COLORS: Partial<Record<TriageAlertActionType, string>> = {
  CREATED: 'red',
  REPEATED: 'red',
  REPEATED_AFTER_FALSE_ALARM: 'red',
  CLAIMED: 'orange',
  RESOLVED: 'green',
  FALSE_ALARM: 'gray',
}

const STAFF_LABELS: Record<string, string> = { ADMIN: 'Admin', DOCTOR: 'Bác sĩ' }

const POLL_INTERVAL_MS = 10000

function describeActor(type: string | null, id: number | null): string | null {
  if (!type) return null
  if (type === 'PATIENT') return 'Bệnh nhân'
  return `${STAFF_LABELS[type] ?? type} #${id}`
}

type DialogKind = 'resolve' | 'falseAlarm'

interface TriageAlertDetailDrawerProps {
  alertId: number | null
  /** Tăng lên khi có realtime của đúng cảnh báo này - drawer tự tải lại. */
  reloadKey: number
  onClose: () => void
  onAlertChanged: (alert: TriageAlert) => void
}

export function TriageAlertDetailDrawer({ alertId, reloadKey, onClose, onAlertChanged }: TriageAlertDetailDrawerProps) {
  const { user } = useCmsAuth()
  const [detail, setDetail] = useState<TriageAlertDetail | null>(null)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [draft, setDraft] = useState('')
  const [dialog, setDialog] = useState<DialogKind | null>(null)
  const [dialogText, setDialogText] = useState('')
  const guard = useInFlightGuard<string>()
  const loadSeq = useRef(0)
  const onAlertChangedRef = useRef(onAlertChanged)
  onAlertChangedRef.current = onAlertChanged

  const load = useCallback((id: number, silent: boolean) => {
    const mySeq = ++loadSeq.current
    if (!silent) setLoading(true)
    getTriageAlertDetail(id)
      .then((data) => {
        if (mySeq !== loadSeq.current) return
        setDetail(data)
        setLoadError(null)
        onAlertChangedRef.current(data.alert)
      })
      .catch((err: unknown) => {
        if (mySeq !== loadSeq.current) return
        setLoadError(extractTriageErrorMessage(err, 'Không thể tải chi tiết cảnh báo.'))
      })
      .finally(() => {
        if (mySeq === loadSeq.current) setLoading(false)
      })
  }, [])

  // Đổi cảnh báo: xoá dữ liệu cũ và tải có spinner.
  useEffect(() => {
    setDetail(null)
    setDraft('')
    setLoadError(null)
    if (alertId !== null) load(alertId, false)
  }, [alertId, load])

  // Realtime của đúng cảnh báo đang mở: tải lại im lặng (chỉ khi reloadKey đổi).
  const handledReloadKey = useRef(reloadKey)
  useEffect(() => {
    if (reloadKey === handledReloadKey.current) return
    handledReloadKey.current = reloadKey
    if (alertId !== null) load(alertId, true)
  }, [reloadKey, alertId, load])

  // Dự phòng khi mất WebSocket: cảnh báo chưa đóng thì tải lại mỗi 10 giây.
  const pollDetail = detail !== null && detail.alert.status !== 'RESOLVED'
  useEffect(() => {
    if (alertId === null || !pollDetail) return
    const intervalId = setInterval(() => load(alertId, true), POLL_INTERVAL_MS)
    return () => clearInterval(intervalId)
  }, [alertId, pollDetail, load])

  // Quay lại tab trình duyệt: tải lại ngay.
  useEffect(() => {
    if (alertId === null) return
    const refresh = () => {
      if (document.visibilityState === 'visible') load(alertId, true)
    }
    window.addEventListener('focus', refresh)
    document.addEventListener('visibilitychange', refresh)
    return () => {
      window.removeEventListener('focus', refresh)
      document.removeEventListener('visibilitychange', refresh)
    }
  }, [alertId, load])

  const alert = detail?.alert ?? null
  const isClaimer = alert !== null && user?.userId === alert.claimedById && user?.userType === alert.claimedByType
  const canHandle = alert?.status === 'IN_PROGRESS' && (isClaimer || user?.userType === 'ADMIN')
  const busy = guard.inFlightId !== null

  async function runAction(key: string, action: () => Promise<unknown>, success: string, fallbackError: string) {
    if (alertId === null) return false
    const ok = await guard.run(key, async () => {
      try {
        await action()
        message.success(success)
        return true
      } catch (err) {
        message.error(extractTriageErrorMessage(err, fallbackError))
        return false
      }
    })
    load(alertId, true)
    return ok === true
  }

  async function handleSend() {
    const content = draft.trim()
    if (!content || alertId === null) return
    const ok = await runAction('send', () => sendTriageAlertMessage(alertId, content),
      'Đã gửi tin nhắn cho bệnh nhân', 'Không thể gửi tin nhắn.')
    if (ok) setDraft('')
  }

  async function handleDialogOk() {
    if (alertId === null || dialog === null) return
    const text = dialogText.trim()
    const ok = dialog === 'resolve'
      ? await runAction('resolve', () => resolveTriageAlert(alertId, text),
        'Đã đánh dấu xử lý xong', 'Không thể đánh dấu xử lý xong.')
      : await runAction('falseAlarm', () => markTriageAlertFalseAlarm(alertId, text),
        'Đã đánh dấu báo nhầm/spam', 'Không thể đánh dấu báo nhầm.')
    if (ok) setDialog(null)
  }

  function openDialog(kind: DialogKind) {
    setDialogText('')
    setDialog(kind)
  }

  const timelineItems = (detail?.actions ?? []).map((action: TriageAlertActionItem) => {
    const actor = describeActor(action.actorType, action.actorId)
    return {
      key: action.id,
      color: ACTION_COLORS[action.actionType] ?? 'blue',
      title: formatDateTime(action.createdAt),
      content: (
        <div className="min-w-0">
          <Text strong>{ACTION_LABELS[action.actionType] ?? action.actionType}</Text>
          {actor && <Text type="secondary"> · {actor}</Text>}
          {action.detail && <div className="whitespace-pre-wrap break-words text-sm">{action.detail}</div>}
        </div>
      ),
    }
  })

  return (
    <Drawer
      title={alert ? `Cảnh báo #${alert.id}` : 'Chi tiết cảnh báo'}
      open={alertId !== null}
      onClose={onClose}
      size="large"
      destroyOnHidden
    >
      <Spin spinning={loading}>
        {loadError && <Alert type="error" showIcon title={loadError} className="mb-4" />}

        {alert && (
          <div className="flex flex-col gap-5">
            <Descriptions column={1} bordered size="small" styles={{ label: { width: 140, whiteSpace: 'nowrap' } }}>
              <Descriptions.Item label="Bệnh nhân">
                <Space wrap size={4}>
                  <Text strong>{alert.patientFullName ?? `Bệnh nhân #${alert.patientId}`}</Text>
                  <Text type="secondary">#{alert.patientId}</Text>
                  {alert.patientFalseAlarmCount > 0 && (
                    <Tag color="orange">{alert.patientFalseAlarmCount} lần báo nhầm trước đây</Tag>
                  )}
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Số điện thoại">{alert.patientPhone ?? '—'}</Descriptions.Item>
              <Descriptions.Item label="Thời điểm">{formatDateTime(alert.occurredAt)}</Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Space wrap size={4}>
                  <Tag color={TRIAGE_STATUS_COLORS[alert.status]}>{TRIAGE_STATUS_LABELS[alert.status]}</Tag>
                  {alert.outcome === 'HANDLED' && <Tag color="green">Xử lý xong</Tag>}
                  {alert.outcome === 'FALSE_ALARM' && <Tag>Báo nhầm/Spam</Tag>}
                  {alert.claimedByType && (
                    <Text type="secondary">
                      {STAFF_LABELS[alert.claimedByType]} #{alert.claimedById} tiếp nhận
                    </Text>
                  )}
                </Space>
              </Descriptions.Item>
              {describeMatchedGroups(alert.matchedGroups) && (
                <Descriptions.Item label="Dấu hiệu">
                  <Text type="danger">{describeMatchedGroups(alert.matchedGroups)}</Text>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="Tin gốc">
                <Paragraph className="mb-0! whitespace-pre-wrap">{alert.messageContent}</Paragraph>
              </Descriptions.Item>
              {alert.repeatCount > 0 && (
                <Descriptions.Item label="Tin mới nhất">
                  <Space orientation="vertical" size={2} className="w-full">
                    <Tag color="red">Lặp lại ×{alert.repeatCount}</Tag>
                    <Paragraph className="mb-0! whitespace-pre-wrap">{alert.lastMessageContent}</Paragraph>
                    {alert.lastRepeatedAt && (
                      <Text type="secondary" className="text-xs">{formatDateTime(alert.lastRepeatedAt)}</Text>
                    )}
                  </Space>
                </Descriptions.Item>
              )}
              {alert.resolutionNote && (
                <Descriptions.Item label={alert.outcome === 'FALSE_ALARM' ? 'Lý do' : 'Ghi chú'}>
                  <Paragraph className="mb-0! whitespace-pre-wrap">{alert.resolutionNote}</Paragraph>
                </Descriptions.Item>
              )}
            </Descriptions>

            <Space wrap>
              {alert.status === 'NEW' && (
                <Button type="primary" loading={guard.inFlightId === 'claim'} disabled={busy}
                  onClick={() => runAction('claim', () => claimTriageAlert(alert.id),
                    'Đã tiếp nhận cảnh báo', 'Không thể tiếp nhận cảnh báo này.')}>
                  Tiếp nhận
                </Button>
              )}
              {canHandle && (
                <>
                  <Popconfirm title="Trả lại cảnh báo này?" okText="Trả lại" cancelText="Huỷ"
                    onConfirm={() => runAction('release', () => releaseTriageAlert(alert.id),
                      'Đã trả lại cảnh báo', 'Không thể trả lại cảnh báo.')}>
                    <Button icon={<RollbackOutlined />} loading={guard.inFlightId === 'release'} disabled={busy}>
                      Trả lại
                    </Button>
                  </Popconfirm>
                  <Tooltip title={alert.staffMessageCount === 0 ? 'Cần nhắn cho bệnh nhân ít nhất 1 tin' : undefined}>
                    <Button type="primary" icon={<CheckOutlined />}
                      disabled={busy || alert.staffMessageCount === 0} onClick={() => openDialog('resolve')}>
                      Xử lý xong
                    </Button>
                  </Tooltip>
                  <Button danger icon={<StopOutlined />} disabled={busy} onClick={() => openDialog('falseAlarm')}>
                    Báo nhầm/Spam
                  </Button>
                </>
              )}
            </Space>

            <section className="flex flex-col gap-3">
              <Text strong>Trao đổi với bệnh nhân</Text>
              <div className="flex max-h-80 flex-col gap-2 overflow-y-auto rounded-xl border border-black/5 p-3 dark:border-white/10">
                {detail && detail.messages.length === 0 && (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có tin nhắn" />
                )}
                {detail?.messages.map((item) => {
                  const fromPatient = item.senderType === 'PATIENT'
                  return (
                    <div key={item.id} className={`flex ${fromPatient ? 'justify-start' : 'justify-end'}`}>
                      <div className={`max-w-[80%] rounded-2xl px-3 py-2 ${fromPatient
                        ? 'bg-slate-100 text-slate-900 dark:bg-neutral-800 dark:text-slate-100'
                        : 'bg-teal-600 text-white'}`}>
                        <div className="whitespace-pre-wrap break-words">{item.content}</div>
                        <div className={`mt-1 text-xs ${fromPatient ? 'text-slate-500 dark:text-slate-400' : 'text-white/80'}`}>
                          {fromPatient
                            ? alert.patientFullName ?? 'Bệnh nhân'
                            : describeActor(item.senderType, item.senderId)}
                          {' · '}{formatDateTime(item.createdAt)}
                          {!fromPatient && item.readAt && ' · Đã xem'}
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
              {canHandle && (
                <div className="flex flex-col gap-2">
                  <TextArea
                    value={draft}
                    onChange={(event) => setDraft(event.target.value)}
                    placeholder="Nhập tin nhắn cho bệnh nhân"
                    autoSize={{ minRows: 2, maxRows: 6 }}
                    maxLength={2000}
                    showCount
                    disabled={busy}
                  />
                  <div className="flex justify-end">
                    <Button type="primary" icon={<SendOutlined />} loading={guard.inFlightId === 'send'}
                      disabled={busy || draft.trim().length === 0} onClick={handleSend}>
                      Gửi
                    </Button>
                  </div>
                </div>
              )}
            </section>

            <section className="flex flex-col gap-3">
              <Text strong>Lịch sử xử lý</Text>
              <Timeline items={timelineItems} />
            </section>
          </div>
        )}
      </Spin>

      <Modal
        title={dialog === 'falseAlarm' ? 'Đánh dấu báo nhầm/spam' : 'Xử lý xong'}
        open={dialog !== null}
        onCancel={() => setDialog(null)}
        onOk={handleDialogOk}
        okText="Xác nhận"
        cancelText="Huỷ"
        okButtonProps={{
          danger: dialog === 'falseAlarm',
          loading: busy,
          disabled: dialog === 'falseAlarm' && dialogText.trim().length < 3,
        }}
        destroyOnHidden
      >
        <TextArea
          value={dialogText}
          onChange={(event) => setDialogText(event.target.value)}
          placeholder={dialog === 'falseAlarm' ? 'Lý do (bắt buộc)' : 'Ghi chú (không bắt buộc)'}
          autoSize={{ minRows: 3, maxRows: 8 }}
          maxLength={1000}
          showCount
        />
      </Modal>
    </Drawer>
  )
}
