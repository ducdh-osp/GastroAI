import { CheckCircleOutlined, EditOutlined, PlusOutlined, StopOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Pagination, Popconfirm, Spin, Switch, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
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
import { usePagedList } from '../../hooks/usePagedList'
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
    timesOfDay: reminder.timesOfDay,
    startDate: reminder.startDate,
    endDate: reminder.endDate,
    instructions: reminder.instructions,
    active: reminder.active,
    version: reminder.version,
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
  const confirmGuard = useInFlightGuard<string>()
  const deleteGuard = useInFlightGuard<number>()

  const confirmationsPageSize = 10
  const fetchConfirmations = useCallback(
    (page: number, size: number) => listMedicationConfirmations(page, size),
    [],
  )
  const confirmationsList = usePagedList({
    fetchPage: fetchConfirmations,
    pageSize: confirmationsPageSize,
    loadErrorMessage: 'Không thể tải lịch sử xác nhận đã uống.',
  })

  function reloadReminders() {
    setLoadingReminders(true)
    listMedicationReminders()
      .then(setReminders)
      .catch((err: unknown) => setError(err instanceof Error ? err.message : 'Không thể tải danh sách lịch nhắc thuốc.'))
      .finally(() => setLoadingReminders(false))
  }

  useEffect(reloadReminders, [])

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

  async function handleDelete(reminder: MedicationReminder) {
    await deleteGuard.run(reminder.id, async () => {
      try {
        await deleteMedicationReminder(reminder.id, reminder.version)
        reloadReminders()
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể xoá lịch nhắc thuốc.')
      }
    })
  }

  async function handleConfirmDose(reminder: MedicationReminder, scheduledTime: string) {
    const guardKey = `${reminder.id}-${scheduledTime}`
    await confirmGuard.run(guardKey, async () => {
      setError(null)
      setSuccess(null)
      try {
        await confirmMedicationDose(reminder.id, scheduledTime)
        setReminders((current) => current.map((item) =>
          item.id === reminder.id
            ? { ...item, confirmedTimesToday: [...item.confirmedTimesToday, scheduledTime] }
            : item,
        ))
        setSuccess(`Đã ghi nhận uống ${reminder.medicineName}.`)
        // Chỉ gọi một trong hai: đổi về trang 0 sẽ để useEffect tải lại lịch sử.
        if (confirmationsList.page === 0) confirmationsList.reload()
        else confirmationsList.setPage(0)
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
    {
      title: 'Giờ nhắc', dataIndex: 'timesOfDay', key: 'timesOfDay', width: 180,
      render: (times: string[]) => <div className="flex flex-wrap gap-1">{times.map((time) => <Tag key={time}>{formatTimeOfDay(time)}</Tag>)}</div>,
    },
    {
      title: 'Liệu trình / hướng dẫn', key: 'regimen',
      render: (_, record) => (
        <div>
          <div>{record.startDate || record.endDate ? `${record.startDate ?? 'Không giới hạn'} → ${record.endDate ?? 'Không giới hạn'}` : 'Dùng liên tục'}</div>
          {record.instructions && <Text type="secondary">{record.instructions}</Text>}
        </div>
      ),
    },
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
      title: 'Xác nhận liều', key: 'actions', width: 260,
      render: (_, record) => (
        <div className="flex flex-wrap gap-1">
          {record.timesOfDay.map((time) => {
            const guardKey = `${record.id}-${time}`
            const confirmed = record.confirmedTimesToday.includes(time)
            return <Button
              key={time}
              size="small"
              icon={<CheckCircleOutlined />}
              loading={confirmGuard.inFlightId === guardKey}
              disabled={confirmed || (confirmGuard.inFlightId !== null && confirmGuard.inFlightId !== guardKey)}
              onClick={() => handleConfirmDose(record, time)}
            >
              {confirmed ? `${formatTimeOfDay(time)} đã uống` : `Uống ${formatTimeOfDay(time)}`}
            </Button>
          })}
          <Button size="small" type="text" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm
            title="Tắt lịch nhắc này?"
            description="Lịch sử xác nhận đã uống trước đây vẫn được giữ lại."
            okText="Tắt nhắc"
            cancelText="Huỷ"
            onConfirm={() => handleDelete(record)}
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
    { title: 'Liều lúc', dataIndex: 'scheduledTime', key: 'scheduledTime', width: 100, render: formatTimeOfDay },
  ]

  return (
    <AppShell>
      <div className="mx-auto max-w-4xl">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <Text type="secondary">Theo dõi sức khỏe</Text>
            <Title level={2} className="mb-1! mt-1!">Nhắc uống thuốc</Title>
            <Text type="secondary">
              Đây là nơi quản lý thuốc có liều cần nhắc: liệu trình, giờ uống, hướng dẫn và xác nhận đã uống. Thêm, sửa hoặc tắt lịch ở đây không thay đổi danh sách thuốc tự khai không cần nhắc trong{' '}
              <Link to="/medical-profile">Hồ sơ bệnh lý</Link>.
            </Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>Thêm lịch nhắc</Button>
        </div>

        {(error || confirmationsList.error) && <Alert type="error" message={error ?? confirmationsList.error} showIcon className="mb-4" closable onClose={() => { setError(null); confirmationsList.setError(null) }} />}
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
          extra={<Tag color="blue">{confirmationsList.totalElements} lần</Tag>}
        >
          <Spin spinning={confirmationsList.loading}>
            <Table<MedicationConfirmationDetail>
              columns={confirmationColumns}
              dataSource={confirmationsList.items}
              rowKey="id"
              size="small"
              pagination={false}
              locale={{ emptyText: 'Chưa có lần xác nhận nào' }}
            />
          </Spin>
          {confirmationsList.totalElements > confirmationsPageSize && (
            <div className="mt-4 flex justify-end">
              <Pagination
                current={confirmationsList.page + 1}
                pageSize={confirmationsPageSize}
                total={confirmationsList.totalElements}
                onChange={(p) => confirmationsList.setPage(p - 1)}
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
