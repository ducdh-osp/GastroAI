import { LockOutlined, UnlockOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { listCmsPatients, setCmsPatientLock, type CmsPatient } from '../../api/cmsUsers'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { useInFlightGuard } from '../../hooks/useInFlightGuard'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

export default function CmsUsersPage() {
  const [data, setData] = useState<CmsPatient[]>([])
  const [query, setQuery] = useState('')
  const [locked, setLocked] = useState<boolean | undefined>()
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const actionGuard = useInFlightGuard<number>()
  const [lockTarget, setLockTarget] = useState<CmsPatient | null>(null)
  const [lockError, setLockError] = useState<string | null>(null)
  const [lockForm] = Form.useForm<{ reason: string }>()
  const locking = lockTarget !== null && actionGuard.inFlightId === lockTarget.id

  async function reload(nextPage = page) {
    setLoading(true)
    setError(null)
    try {
      const result = await listCmsPatients({ q: query || undefined, locked, page: nextPage - 1, size: 20 })
      setData(result.items)
      setTotal(result.totalElements)
      setPage(result.page + 1)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách bệnh nhân.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void reload(1) }, [locked])

  function openLock(patient: CmsPatient) {
    lockForm.resetFields()
    setLockError(null)
    setLockTarget(patient)
  }

  function closeLock() {
    if (locking) return
    setLockTarget(null)
    setLockError(null)
    lockForm.resetFields()
  }

  async function handleLock(values: { reason: string }) {
    if (!lockTarget) return
    await actionGuard.run(lockTarget.id, async () => {
      setLockError(null)
      try {
        await setCmsPatientLock(lockTarget.id, true, values.reason.trim())
        setLockTarget(null)
        lockForm.resetFields()
        await reload()
      } catch (err) {
        setLockError(err instanceof Error ? err.message : 'Không thể khóa tài khoản.')
      }
    })
  }

  async function handleUnlock(patient: CmsPatient) {
    await actionGuard.run(patient.id, async () => {
      setError(null)
      try {
        await setCmsPatientLock(patient.id, false)
        await reload()
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Không thể mở khóa tài khoản.')
      }
    })
  }

  const columns: ColumnsType<CmsPatient> = [
    { title: 'Bệnh nhân', key: 'name', render: (_, p) => <div><div className="font-medium">{p.fullName || 'Chưa cập nhật'}</div><Text type="secondary">{p.email}</Text></div> },
    { title: 'Điện thoại', dataIndex: 'phone', key: 'phone', render: (value: string | null) => value || '—' },
    { title: 'Trạng thái', key: 'status', render: (_, p) => p.adminLocked ? <Tag color="red">Đã khóa</Tag> : <Tag color="green">Hoạt động</Tag> },
    { title: 'Ngày tạo', dataIndex: 'createdAt', key: 'createdAt', render: formatDateTime },
    {
      title: 'Thao tác', key: 'action', render: (_, patient) => patient.adminLocked ? (
        <Popconfirm
          title="Mở khóa tài khoản này?"
          description="Bệnh nhân có thể đăng nhập lại sau khi mở khóa."
          okText="Mở khóa"
          cancelText="Hủy"
          onConfirm={() => handleUnlock(patient)}
        >
          <Button size="small" icon={<UnlockOutlined />} loading={actionGuard.inFlightId === patient.id} disabled={actionGuard.inFlightId !== null}>
            Mở khóa
          </Button>
        </Popconfirm>
      ) : (
        <Button size="small" danger icon={<LockOutlined />} disabled={actionGuard.inFlightId !== null} onClick={() => openLock(patient)}>
          Khóa
        </Button>
      ),
    },
  ]

  return <CmsAppShell><div className="w-full">
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3"><div><Text type="secondary">Người dùng</Text><Title level={2} className="mb-1! mt-1!"><UserOutlined className="mr-2" />Quản lý bệnh nhân</Title><Text type="secondary">Xem và khóa/mở tài khoản bệnh nhân. Khóa tài khoản sẽ thu hồi các phiên đăng nhập đang hoạt động.</Text></div></div>
    {error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}
    <Card className="rounded-2xl border-black/5 shadow-sm"><Space className="mb-4" wrap><Input.Search placeholder="Tìm theo tên hoặc email" allowClear value={query} onChange={(e) => setQuery(e.target.value)} onSearch={() => void reload(1)} style={{ width: 300 }} /><Select allowClear placeholder="Lọc trạng thái" value={locked} onChange={setLocked} options={[{ value: false, label: 'Đang hoạt động' }, { value: true, label: 'Đã khóa' }]} style={{ width: 160 }} /></Space><Table<CmsPatient> rowKey="id" loading={loading} columns={columns} dataSource={data} pagination={{ current: page, pageSize: 20, total, showSizeChanger: false, onChange: (next) => void reload(next) }} /></Card>
    <Modal
      title="Khóa tài khoản bệnh nhân"
      forceRender
      open={lockTarget !== null}
      onCancel={closeLock}
      onOk={() => lockForm.submit()}
      okText="Xác nhận khóa"
      cancelText="Hủy"
      okButtonProps={{ danger: true }}
      cancelButtonProps={{ disabled: locking }}
      confirmLoading={locking}
      closable={!locking}
      keyboard={!locking}
      mask={{ closable: false }}
    >
      <Typography.Paragraph>
        Khóa tài khoản <Text strong>{lockTarget?.fullName || lockTarget?.email}</Text>
        {lockTarget?.fullName && <> ({lockTarget.email})</>}. Bệnh nhân sẽ không thể đăng nhập và mọi phiên đăng nhập hiện có sẽ bị thu hồi.
      </Typography.Paragraph>
      {lockError && <Alert type="error" title={lockError} showIcon className="mb-4" />}
      <Form form={lockForm} layout="vertical" onFinish={handleLock} disabled={locking}>
        <Form.Item
          name="reason"
          label="Lý do khóa tài khoản"
          extra={<span className="block pr-20">Lý do được ghi vào nhật ký hoạt động để tra cứu.</span>}
          rules={[
            { required: true, whitespace: true, message: 'Nhập lý do khóa tài khoản' },
            { max: 1000, message: 'Lý do khóa tài khoản tối đa 1000 ký tự' },
          ]}
        >
          <Input.TextArea rows={4} maxLength={1000} showCount placeholder="Nhập lý do khóa tài khoản" />
        </Form.Item>
      </Form>
    </Modal>
  </div></CmsAppShell>
}
