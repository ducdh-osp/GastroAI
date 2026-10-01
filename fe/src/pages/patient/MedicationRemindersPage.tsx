import { CheckCircleOutlined, EditOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Pagination, Popconfirm, Spin, Switch, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useRef, useState } from 'react'
import {
  confirmMedicationDose,
  createMedicationReminder,
  deleteMedicationReminder,
  listMedicationConfirmations,
  listMedicationReminders,
  updateMedicationReminder,
} from '../../api/medication'
import type {
  MedicationConfirmationDetail,
  MedicationReminder,
  MedicationReminderRequest,
} from '../../api/medication'
import { AppShell } from '../../components/layout/AppShell'
import { MedicationReminderModal } from '../../components/medication/MedicationReminderModal'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

function formatTimeOfDay(time: string): string {
  // "HH:mm:ss" hoac "HH:mm" tuy BE tra - chi can 5 ky tu dau la "HH:mm".
  return time.slice(0, 5)
}

/** Dung chung cho ca handleSubmit (qua modal) lan handleToggleActive (Switch rieng) - tranh
 * 2 noi tu xay dung payload rieng, de sot field khi sau nay them truong moi cho reminder. */
function reminderToRequest(reminder: MedicationReminder): MedicationReminderRequest {
  return {
    medicineName: reminder.medicineName,
    dosage: reminder.dosage,
    timeOfDay: reminder.timeOfDay,
    active: reminder.active,
  }
}

export default function MedicationRemindersPage() {
  const [reminders, setReminders] = useState<MedicationReminder[]>([])
  const [loadingReminders, setLoadingReminders] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingReminder, setEditingReminder] = useState<MedicationReminder | null>(null)
  const [saving, setSaving] = useState(false)
  const toggleGuard = useInFlightGuard<number>()
  const confirmGuard = useInFlightGuard<number>()
  const deleteGuard = useInFlightGuard<number>()

  const [confirmations, setConfirmations] = useState<MedicationConfirmationDetail[]>([])
  const [confirmationsPage, setConfirmationsPage] = useState(0)
  const [confirmationsTotal, setConfirmationsTotal] = useState(0)
  const [loadingConfirmations, setLoadingConfirmations] = useState(true)
  const confirmationsRequestId = useRef(0)
  const confirmationsPageSize = 10

  function reloadReminders() {
    setLoadingReminders(true)
    listMedicationReminders()
      .then(setReminders)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : 'Không thể tải danh sách lịch nhắc thuốc.'))
      .finally(() => setLoadingReminders(false))
  }

  function reloadConfirmations() {
    const currentRequestId = ++confirmationsRequestId.current
    setLoadingConfirmations(true)
    listMedicationConfirmations(confirmationsPage, confirmationsPageSize)
      .then((res) => {
        if (currentRequestId !== confirmationsRequestId.current) return
        setConfirmations(res.items)
        setConfirmationsTotal(res.totalElements)
      })
      .catch((err: unknown) => {
        if (currentRequestId === confirmationsRequestId.current) setError(err instanceof Error ? err.message : 'Không thể tải lịch sử xác nhận đã uống.')
      })
      .finally(() => { if (currentRequestId === confirmationsRequestId.current) setLoadingConfirmations(false) })
  }

  useEffect(reloadReminders, [])
  useEffect(reloadConfirmations, [confirmationsPage])

  function openCreate() {
    setEditingReminder(null)
    setModalOpen(true)
  }

  function openEdit(reminder: MedicationReminder) {
    setEditingReminder(reminder)
    setModalOpen(true)
  }

  async function handleSubmit(payload: MedicationReminderRequest) {
    setSaving(true)
    setError(null)
    try {
      if (editingReminder) {
        await updateMedicationReminder(editingReminder.id, payload)
      } else {
        await createMedicationReminder(payload)
      }
      setModalOpen(false)
      reloadReminders()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể lưu lịch nhắc thuốc.')
    } finally {
      setSaving(false)
    }
  }

  async function handleToggleActive(reminder: MedicationReminder, active: boolean) {
    await toggleGuard.run(reminder.id, async () => {
      try {
        await updateMedicationReminder(reminder.id, { ...reminderToRequest(reminder), active })
        reloadReminders()
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể cập nhật trạng thái.')
      }
    })
  }

  async function handleDelete(id: number) {
    await deleteGuard.run(id, async () => {
      try {
        await deleteMedicationReminder(id)
        reloadReminders()
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể xoá lịch nhắc thuốc.')
      }
    })
  }

  async function handleConfirmDose(reminder: MedicationReminder) {
    await confirmGuard.run(reminder.id, async () => {
      setError(null)
      setSuccess(null)
      try {
        await confirmMedicationDose(reminder.id)
        setReminders((current) => current.map((item) =>
          item.id === reminder.id ? { ...item, confirmedToday: true } : item,
        ))
        setSuccess(`Đã ghi nhận uống ${reminder.medicineName}.`)
        // Chỉ gọi một trong hai: đổi về trang 0 sẽ để useEffect tải lại lịch sử.
        if (confirmationsPage === 0) {
          reloadConfirmations()
        } else {
          setConfirmationsPage(0)
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể ghi nhận đã uống thuốc.')
      }
    })
  }

  const reminderColumns: ColumnsType<MedicationReminder> = [
    { title: 'Tên thuốc', dataIndex: 'medicineName', key: 'medicineName' },
    {
      title: 'Liều lượng', dataIndex: 'dosage', key: 'dosage',
      render: (val: string | null) => val || <Text type="secondary">—</Text>,
    },
    { title: 'Giờ nhắc', dataIndex: 'timeOfDay', key: 'timeOfDay', width: 100, render: formatTimeOfDay },
    {
      title: 'Bật nhắc', key: 'active', width: 100,
      render: (_, record) => (
        <Switch
          checked={record.active}
          loading={toggleGuard.inFlightId === record.id}
          // Chỉ cho phép một thao tác bật/tắt tại một thời điểm. Khoá cả
          // công tắc đang gửi request để tránh click liên tiếp trước khi
          // trạng thái loading của Ant Design kịp cập nhật.
          disabled={saving || toggleGuard.inFlightId !== null}
          onChange={(checked) => handleToggleActive(record, checked)}
        />
      ),
    },
    {
      title: '', key: 'actions', width: 180,
      render: (_, record) => (
        <div className="flex gap-1">
          <Button
            size="small"
            icon={<CheckCircleOutlined />}
            loading={confirmGuard.inFlightId === record.id}
            disabled={record.confirmedToday || (confirmGuard.inFlightId !== null && confirmGuard.inFlightId !== record.id)}
            onClick={() => handleConfirmDose(record)}
          >
            {record.confirmedToday ? 'Đã xác nhận hôm nay' : 'Đã uống'}
          </Button>
          <Button size="small" type="text" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title="Tắt lịch nhắc này?"
            description="Lịch sử xác nhận đã uống trước đây vẫn được giữ lại."
            okText="Tắt nhắc"
            cancelText="Huỷ"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button
              size="small"
              type="text"
              danger
              icon={<StopOutlined />}
              loading={deleteGuard.inFlightId === record.id}
              disabled={deleteGuard.inFlightId !== null && deleteGuard.inFlightId !== record.id}
            />
          </Popconfirm>
        </div>
      ),
    },
  ]

  const confirmationColumns: ColumnsType<MedicationConfirmationDetail> = [
    { title: 'Thời điểm xác nhận', dataIndex: 'confirmedAt', key: 'confirmedAt', width: 180, render: formatDateTime },
    { title: 'Thuốc', dataIndex: 'medicineName', key: 'medicineName' },
  ]

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Nhắc uống thuốc</Title>
            <Text type="secondary">
              Thiết lập giờ nhắc lặp lại hằng ngày và bấm "Đã uống" để ghi nhận (chưa gửi thông báo thật, xác nhận thủ công).
            </Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm lịch nhắc</Button>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}
        {success && <Alert type="success" message={success} showIcon className="mb-4" closable onClose={() => setSuccess(null)} />}

        <Card className="mb-6 rounded-2xl border-black/5 shadow-sm" title="Lịch nhắc thuốc">
          <Spin spinning={loadingReminders}>
            <Table<MedicationReminder>
              columns={reminderColumns}
              dataSource={reminders}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'Chưa có lịch nhắc thuốc nào' }}
            />
          </Spin>
        </Card>

        <Card
          className="rounded-2xl border-black/5 shadow-sm"
          title="Lịch sử xác nhận đã uống"
          extra={<Tag color="blue">{confirmationsTotal} lần</Tag>}
        >
          <Spin spinning={loadingConfirmations}>
            <Table<MedicationConfirmationDetail>
              columns={confirmationColumns}
              dataSource={confirmations}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'Chưa có lần xác nhận nào' }}
            />
          </Spin>
          {confirmationsTotal > confirmationsPageSize && (
            <div className="mt-4 flex justify-end">
              <Pagination
                current={confirmationsPage + 1}
                pageSize={confirmationsPageSize}
                total={confirmationsTotal}
                onChange={(p) => setConfirmationsPage(p - 1)}
                showSizeChanger={false}
              />
            </div>
          )}
        </Card>
      </div>

      <MedicationReminderModal
        open={modalOpen}
        reminder={editingReminder}
        saving={saving}
        onCancel={() => setModalOpen(false)}
        onSubmit={handleSubmit}
      />
    </AppShell>
  )
}
