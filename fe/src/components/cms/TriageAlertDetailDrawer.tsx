import { CheckOutlined, PhoneOutlined, RollbackOutlined, StopOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  Descriptions,
  Drawer,
  Input,
  Modal,
  Popconfirm,
  Select,
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
  CONTACT_RESULT_LABEL,
  extractTriageErrorMessage,
  getTriageAlertDetail,
  markTriageAlertFalseAlarm,
  recordTriageContact,
  releaseTriageAlert,
  resolveTriageAlert,
  type TriageAlert,
  type TriageAlertActionItem,
  type TriageAlertActionType,
  type TriageAlertDetail,
  type TriageAlertOutcome,
  type TriageAlertStatus,
  type TriageContactResult,
} from '../../api/cmsTriage'
import { describeMatchedGroups } from '../../api/chat'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { useNow } from '../../hooks/useNow'
import { formatDateTime } from '../../lib/format'
import { FAILSAFE_AFTER_MS, formatCountdown, INTERVENTION_AFTER_MS, msUntil } from '../../lib/triageTimers'
import { useCmsAuth } from '../../stores/cmsAuthStore'
import { canAccessCmsPath } from './cmsNav'

const { Text, Paragraph } = Typography
const { TextArea } = Input

/** SCR-AD-13: 3 trạng thái rõ ràng - Admin chỉ cần quan tâm "Cần can thiệp". */
export const TRIAGE_STATUS_LABELS: Record<TriageAlertStatus, string> = {
  AUTO_HANDLING: 'Hệ thống đang tự xử lý',
  NEEDS_INTERVENTION: 'Cần can thiệp',
  RESOLVED: 'Đã xử lý',
}

export const TRIAGE_STATUS_COLORS: Record<TriageAlertStatus, string> = {
  AUTO_HANDLING: 'blue',
  NEEDS_INTERVENTION: 'red',
  RESOLVED: 'green',
}

export const TRIAGE_OUTCOME_LABELS: Record<TriageAlertOutcome, string> = {
  HANDLED: 'Admin xử lý xong',
  FALSE_ALARM: 'Báo nhầm/Spam',
  PATIENT_SAFE: 'Bệnh nhân báo an toàn',
}

const ACTION_LABELS: Record<TriageAlertActionType, string> = {
  CREATED: 'Cảnh báo được tạo',
  CHECK_IN_SENT: 'Hỏi bệnh nhân "Bạn có ổn không?"',
  PATIENT_SAFE: 'Bệnh nhân bấm An toàn',
  PATIENT_NEEDS_HELP: 'Bệnh nhân bấm Cần hỗ trợ',
  ESCALATED: 'Chuyển "Cần can thiệp"',
  FAILSAFE: 'Failsafe - quá hạn chưa Admin nào xử lý',
  CONTACT_RECORDED: 'Ghi kết quả liên hệ',
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
  CHECK_IN_SENT: 'gray',
  PATIENT_SAFE: 'green',
  PATIENT_NEEDS_HELP: 'red',
  ESCALATED: 'red',
  FAILSAFE: 'red',
  REPEATED: 'red',
  REPEATED_AFTER_FALSE_ALARM: 'red',
  CLAIMED: 'orange',
  RESOLVED: 'green',
  FALSE_ALARM: 'gray',
}

const STAFF_LABELS: Record<string, string> = { ADMIN: 'Admin' }

const POLL_INTERVAL_MS = 10000

const CONTACT_RESULT_OPTIONS = (Object.keys(CONTACT_RESULT_LABEL) as TriageContactResult[]).map((value) => ({
  value,
  label: CONTACT_RESULT_LABEL[value],
}))

/**
 * Kịch bản gọi người thân - Admin đọc theo, không cần tự nghĩ lời khi đang gấp. Chỉ nói mức cần thiết,
 * không đọc nội dung chat của bệnh nhân (riêng tư).
 */
function callScript(contactName: string, patientName: string, signs: string | null, occurredAt: string): string {
  return `Chào anh/chị ${contactName}, tôi gọi từ GastroAI. ${patientName} vừa báo ${signs ? `dấu hiệu "${signs}"` : 'dấu hiệu sức khoẻ cần chú ý'} lúc ${formatDateTime(occurredAt)} và chưa phản hồi chúng tôi. Anh/chị có thể liên lạc hoặc tới kiểm tra ${patientName} ngay được không? Nếu thấy nguy hiểm, hãy gọi 115.`
}
const MAX_NOTE_LENGTH = 1000

function describeActor(type: string | null, id: number | null): string | null {
  if (!type) return null
  if (type === 'PATIENT') return 'Bệnh nhân'
  if (type === 'SYSTEM') return 'Hệ thống'
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
  const [dialog, setDialog] = useState<DialogKind | null>(null)
  const [dialogText, setDialogText] = useState('')
  const [contactResult, setContactResult] = useState<TriageContactResult | null>(null)
  const [contactNote, setContactNote] = useState('')
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
    setContactResult(null)
    setContactNote('')
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
  const isClaimer = alert !== null && user?.userId === alert.claimedById
  // SCR-AD-13: Admin/Giám sát vận hành bất kỳ xử lý được, không bắt buộc "tiếp nhận" trước - khớp
  // TriageAlertService (cho phép mọi tài khoản loại ADMIN xử lý thay).
  const canOverride = canAccessCmsPath(user?.userType, '/cms/triage-alerts')
  const isOpen = alert !== null && alert.status !== 'RESOLVED'
  const canHandle = isOpen && (alert.claimedById === null || isClaimer || canOverride)
  const canRelease = alert?.status === 'NEEDS_INTERVENTION' && alert.claimedById !== null
    && (isClaimer || canOverride)
  const contactRecorded = (detail?.actions ?? []).some((action) => action.actionType === 'CONTACT_RECORDED')
  const canResolve = contactRecorded
  const busy = guard.inFlightId !== null
  const now = useNow(isOpen)
  const autoRemaining = alert?.status === 'AUTO_HANDLING'
    ? msUntil(alert.occurredAt, INTERVENTION_AFTER_MS, now)
    : null
  const failsafeRemaining = alert?.status === 'NEEDS_INTERVENTION' && !alert.acknowledgedAt && !alert.failsafeAt
    ? msUntil(alert.needsInterventionAt, FAILSAFE_AFTER_MS, now)
    : null

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

  async function handleDialogOk() {
    if (alertId === null || dialog === null) return
    const text = dialogText.trim()
    const ok = dialog === 'resolve'
      ? await runAction('resolve', () => resolveTriageAlert(alertId, text),
        'Đã đóng ca', 'Không thể đóng ca.')
      : await runAction('falseAlarm', () => markTriageAlertFalseAlarm(alertId, text),
        'Đã đánh dấu báo nhầm/spam', 'Không thể đánh dấu báo nhầm.')
    if (ok) setDialog(null)
  }

  async function handleRecordContact() {
    if (alertId === null || contactResult === null) return
    const ok = await runAction('contact', () => recordTriageContact(alertId, contactResult, contactNote.trim()),
      'Đã ghi kết quả liên hệ', 'Không thể ghi kết quả liên hệ.')
    if (ok) {
      setContactResult(null)
      setContactNote('')
    }
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
      title: <span className="whitespace-nowrap">{formatDateTime(action.createdAt)}</span>,
      content: (
        <div className="min-w-0">
          <Text strong>{ACTION_LABELS[action.actionType] ?? action.actionType}</Text>
          {actor && <Text type="secondary"> · {actor}</Text>}
          {action.detail && (
            <div className="whitespace-pre-wrap break-words text-sm [overflow-wrap:anywhere]">{action.detail}</div>
          )}
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
            {alert.status === 'AUTO_HANDLING' && (
              <Alert
                type="info"
                showIcon
                title="Hệ thống đang tự xử lý"
                description={`Đã hỏi bệnh nhân "Bạn có ổn không?". Nếu bệnh nhân im lặng, ca tự chuyển "Cần can thiệp" sau ${formatCountdown(autoRemaining ?? 0)}. Thấy cần gấp thì bấm "Can thiệp ngay".`}
              />
            )}
            {alert.status === 'NEEDS_INTERVENTION' && alert.failsafeAt && !alert.acknowledgedAt && (
              <Alert
                type="error"
                showIcon
                title="QUÁ HẠN - chưa Admin nào xử lý"
                description={`Failsafe đã chạy lúc ${formatDateTime(alert.failsafeAt)}: đã báo động lại toàn bộ Admin và nhắc lại bệnh nhân. Hãy liên hệ ngay.`}
              />
            )}
            {failsafeRemaining !== null && (
              <Alert
                type="error"
                showIcon
                title="Cần can thiệp - chưa Admin nào xử lý"
                description={`Gọi người thân theo số bên dưới rồi ghi kết quả. Nếu không ai ghi gì, failsafe chạy sau ${formatCountdown(failsafeRemaining)}.`}
              />
            )}
            <Descriptions column={1} bordered size="small" styles={{ label: { width: 120, whiteSpace: 'nowrap' }, content: { minWidth: 0 } }}>
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
              <Descriptions.Item label="Bệnh nhân trả lời">
                {alert.patientResponse === 'SAFE' && <Tag color="green">An toàn</Tag>}
                {alert.patientResponse === 'NEED_HELP' && <Tag color="red">Cần hỗ trợ</Tag>}
                {alert.patientResponse === null && <Text type="secondary">Chưa trả lời</Text>}
                {alert.patientRespondedAt && (
                  <Text type="secondary" className="text-xs"> · {formatDateTime(alert.patientRespondedAt)}</Text>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Space wrap size={4}>
                  <Tag color={TRIAGE_STATUS_COLORS[alert.status]}>{TRIAGE_STATUS_LABELS[alert.status]}</Tag>
                  {alert.outcome && (
                    <Tag color={alert.outcome === 'FALSE_ALARM' ? 'default' : 'green'}>
                      {TRIAGE_OUTCOME_LABELS[alert.outcome]}
                    </Tag>
                  )}
                  {alert.claimedByType && alert.status !== 'RESOLVED' && (
                    <Text type="secondary">
                      {STAFF_LABELS[alert.claimedByType]} #{alert.claimedById} đang liên hệ
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
                <Paragraph className="mb-0! whitespace-pre-wrap break-words [overflow-wrap:anywhere]">{alert.messageContent}</Paragraph>
              </Descriptions.Item>
              {alert.repeatCount > 0 && (
                <Descriptions.Item label="Tin mới nhất">
                  <Space orientation="vertical" size={2} className="w-full">
                    <Tag color="red">Lặp lại ×{alert.repeatCount}</Tag>
                    <Paragraph className="mb-0! whitespace-pre-wrap break-words [overflow-wrap:anywhere]">{alert.lastMessageContent}</Paragraph>
                    {alert.lastRepeatedAt && (
                      <Text type="secondary" className="text-xs">{formatDateTime(alert.lastRepeatedAt)}</Text>
                    )}
                  </Space>
                </Descriptions.Item>
              )}
              {alert.resolutionNote && (
                <Descriptions.Item label={alert.outcome === 'FALSE_ALARM' ? 'Lý do' : 'Ghi chú'}>
                  <Paragraph className="mb-0! whitespace-pre-wrap break-words [overflow-wrap:anywhere]">{alert.resolutionNote}</Paragraph>
                </Descriptions.Item>
              )}
            </Descriptions>

            {alert.status === 'NEEDS_INTERVENTION' && (
              <section className="flex flex-col gap-3 rounded-xl border border-red-200 bg-red-50/60 p-4 dark:border-red-900/50 dark:bg-red-950/20">
                <Text strong><PhoneOutlined className="mr-2" />Liên hệ khẩn cấp</Text>
                {detail?.emergencyContact ? (
                  <>
                    <div className="flex flex-wrap items-baseline gap-x-2">
                      <Text strong>{detail.emergencyContact.name}</Text>
                      <Text type="secondary">({detail.emergencyContact.relationship})</Text>
                      <a href={`tel:${detail.emergencyContact.phone}`} className="font-semibold tabular-nums">
                        {detail.emergencyContact.phone}
                      </a>
                    </div>
                    <div>
                      <Text type="secondary" className="text-xs">Kịch bản gọi</Text>
                      <Paragraph className="mb-0! text-sm" copyable>
                        {callScript(detail.emergencyContact.name,
                          alert.patientFullName ?? `Bệnh nhân #${alert.patientId}`,
                          describeMatchedGroups(alert.matchedGroups), alert.occurredAt)}
                      </Paragraph>
                    </div>
                  </>
                ) : (
                  <Text type="secondary">
                    Bệnh nhân chưa khai liên hệ khẩn cấp - gọi thẳng số của bệnh nhân
                    {alert.patientPhone ? ` (${alert.patientPhone})` : ''}, hoặc gọi 115 nếu thấy cần.
                  </Text>
                )}
                {canHandle && (
                  <div className="flex flex-col gap-2">
                    <Select<TriageContactResult>
                      placeholder="Kết quả liên hệ"
                      options={CONTACT_RESULT_OPTIONS}
                      value={contactResult ?? undefined}
                      onChange={setContactResult}
                      disabled={busy}
                    />
                    <TextArea
                      value={contactNote}
                      onChange={(event) => setContactNote(event.target.value)}
                      placeholder="Ghi chú (không bắt buộc)"
                      autoSize={{ minRows: 1, maxRows: 4 }}
                      maxLength={MAX_NOTE_LENGTH}
                      disabled={busy}
                    />
                    <Button type="primary" loading={guard.inFlightId === 'contact'}
                      disabled={busy || contactResult === null} onClick={handleRecordContact}>
                      Ghi kết quả liên hệ
                    </Button>
                  </div>
                )}
              </section>
            )}

            <Space wrap>
              {isOpen && alert.claimedById === null && (
                <Button type={alert.status === 'AUTO_HANDLING' ? 'default' : 'primary'}
                  loading={guard.inFlightId === 'claim'} disabled={busy}
                  onClick={() => runAction('claim', () => claimTriageAlert(alert.id),
                    alert.status === 'AUTO_HANDLING' ? 'Đã chuyển sang Cần can thiệp' : 'Đã đánh dấu bạn đang liên hệ',
                    'Không thể nhận ca này.')}>
                  {alert.status === 'AUTO_HANDLING' ? 'Can thiệp ngay' : 'Tôi đang liên hệ'}
                </Button>
              )}
              {canRelease && (
                <Popconfirm title="Bỏ đánh dấu đang liên hệ?" description="Đồng hồ failsafe sẽ tính lại từ lúc này."
                  okText="Trả lại" cancelText="Huỷ"
                  onConfirm={() => runAction('release', () => releaseTriageAlert(alert.id),
                    'Đã trả lại ca', 'Không thể trả lại ca này.')}>
                  <Button icon={<RollbackOutlined />} loading={guard.inFlightId === 'release'} disabled={busy}>
                    Trả lại
                  </Button>
                </Popconfirm>
              )}
              {canHandle && (
                <>
                  <Tooltip title={canResolve ? undefined : 'Cần ghi kết quả liên hệ trước khi đóng ca'}>
                    <Button type="primary" icon={<CheckOutlined />}
                      disabled={busy || !canResolve} onClick={() => openDialog('resolve')}>
                      Đóng ca
                    </Button>
                  </Tooltip>
                  <Button danger icon={<StopOutlined />} disabled={busy} onClick={() => openDialog('falseAlarm')}>
                    Báo nhầm/Spam
                  </Button>
                </>
              )}
            </Space>

            {/* Ca khẩn cấp do Admin gọi điện, không nhắn tin trong app. Chỉ còn hiện tin nhắn của các ca cũ. */}
            {detail && detail.messages.length > 0 && (
            <section className="flex flex-col gap-3">
              <Text strong>Tin nhắn trước đây (chỉ xem)</Text>
              <div className="flex max-h-80 flex-col gap-2 overflow-y-auto rounded-xl border border-black/5 p-3 dark:border-white/10">
                {detail.messages.map((item) => {
                  const fromPatient = item.senderType === 'PATIENT'
                  return (
                    <div key={item.id} className={`flex ${fromPatient ? 'justify-start' : 'justify-end'}`}>
                      <div className={`min-w-0 max-w-[85%] rounded-2xl px-3 py-2 sm:max-w-[80%] ${fromPatient
                        ? 'bg-slate-100 text-slate-900 dark:bg-neutral-800 dark:text-slate-100'
                        : 'bg-teal-600 text-white'}`}>
                        <div className="whitespace-pre-wrap break-words [overflow-wrap:anywhere]">{item.content}</div>
                        <div className={`mt-1 break-words text-xs ${fromPatient ? 'text-slate-500 dark:text-slate-400' : 'text-white/80'}`}>
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
            </section>
            )}

            <section className="flex flex-col gap-3">
              <Text strong>Lịch sử xử lý</Text>
              <Timeline items={timelineItems} />
            </section>
          </div>
        )}
      </Spin>

      <Modal
        title={dialog === 'falseAlarm' ? 'Đánh dấu báo nhầm/spam' : 'Đóng ca'}
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
        <div className="flex flex-col gap-1">
          <TextArea
            value={dialogText}
            onChange={(event) => setDialogText(event.target.value)}
            placeholder={dialog === 'falseAlarm' ? 'Lý do (bắt buộc)' : 'Ghi chú (không bắt buộc)'}
            autoSize={{ minRows: 3, maxRows: 8 }}
            maxLength={MAX_NOTE_LENGTH}
          />
          <Text type="secondary" className="self-end text-xs tabular-nums">
            {dialogText.length} / {MAX_NOTE_LENGTH}
          </Text>
        </div>
      </Modal>
    </Drawer>
  )
}
