import { CheckCircleOutlined, DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Pagination, Popconfirm, Spin, Switch, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
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
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

function formatTimeOfDay(time: string): string {
  // "HH:mm:ss" hoac "HH:mm" tuy BE tra - chi can 5 ky tu dau la "HH:mm".
  return time.slice(0, 5)
}

export default function MedicationRemindersPage() {
  const [reminders, setReminders] = useState<MedicationReminder[]>([])
  const [loadingReminders, setLoadingReminders] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editingReminder, setEditingReminder] = useState<MedicationReminder | null>(null)
  const [saving, setSaving] = useState(false)

  const [confirmations, setConfirmations] = useState<MedicationConfirmationDetail[]>([])
  const [confirmationsPage, setConfirmationsPage] = useState(0)
  const [confirmationsTotal, setConfirmationsTotal] = useState(0)
  const [loadingConfirmations, setLoadingConfirmations] = useState(true)
  const confirmationsPageSize = 10

  function reloadReminders() {
    setLoadingReminders(true)
    listMedicationReminders()
      .then(setReminders)
      .catch(() => setError('Không thể tải danh sách lịch nhắc thuốc.'))
      .finally(() => setLoadingReminders(false))
  }

  function reloadConfirmations() {
    setLoadingConfirmations(true)
    listMedicationConfirmations(confirmationsPage, confirmationsPageSize)
      .then((res) => {
        setConfirmations(res.items)
        setConfirmationsTotal(res.totalElements)
      })
      .catch(() => setError('Không thể tải lịch sử xác nhận đã uống.'))
      .finally(() => setLoadingConfirmations(false))
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
    try {
      await updateMedicationReminder(reminder.id, {
        medicineName: reminder.medicineName,
        dosage: reminder.dosage,
        timeOfDay: reminder.timeOfDay,
        active,
      })
      reloadReminders()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể cập nhật trạng thái.')
    }
  }

  async function handleDelete(id: number) {
    try {
      await deleteMedicationReminder(id)
      reloadReminders()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể xoá lịch nhắc thuốc.')
    }
  }

  async function handleConfirmDose(reminder: MedicationReminder) {
    setError(null)
    setSuccess(null)
    try {
      await confirmMedicationDose(reminder.id)
      setSuccess(`Đã ghi nhận uống ${reminder.medicineName}.`)
      setConfirmationsPage(0)
      reloadConfirmations()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể ghi nhận đã uống thuốc.')
    }
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
      render: (_, record) => <Switch checked={record.active} onChange={(checked) => handleToggleActive(record, checked)} />,
    },
    {
      title: '', key: 'actions', width: 180,
      render: (_, record) => (
        <div className="flex gap-1">
          <Button size="small" icon={<CheckCircleOutlined />} onClick={() => handleConfirmDose(record)}>Đã uống</Button>
          <Button size="small" type="text" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          <Popconfirm title="Xoá lịch nhắc này?" okText="Xoá" cancelText="Huỷ" onConfirm={() => handleDelete(record.id)}>
            <Button size="small" type="text" danger icon={<DeleteOutlined />} />
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
