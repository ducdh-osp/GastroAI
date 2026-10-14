import { LockOutlined, UnlockOutlined, UserOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Input, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { listCmsPatients, setCmsPatientLock, type CmsPatient } from '../../api/cmsUsers'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
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
  const [busyIds, setBusyIds] = useState<Set<number>>(() => new Set())

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

  async function toggle(patient: CmsPatient) {
    setBusyIds((current) => new Set(current).add(patient.id))
    try {
      await setCmsPatientLock(patient.id, !patient.adminLocked)
      await reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể cập nhật tài khoản.')
    } finally {
      setBusyIds((current) => {
        const next = new Set(current)
        next.delete(patient.id)
        return next
      })
    }
  }

  const columns: ColumnsType<CmsPatient> = [
    { title: 'Bệnh nhân', key: 'name', render: (_, p) => <div><div className="font-medium">{p.fullName || 'Chưa cập nhật'}</div><Text type="secondary">{p.email}</Text></div> },
    { title: 'Điện thoại', dataIndex: 'phone', key: 'phone', render: (value: string | null) => value || '—' },
    { title: 'Trạng thái', key: 'status', render: (_, p) => p.adminLocked ? <Tag color="red">Đã khóa</Tag> : <Tag color="green">Hoạt động</Tag> },
    { title: 'Ngày tạo', dataIndex: 'createdAt', key: 'createdAt', render: formatDateTime },
    { title: '', key: 'action', render: (_, p) => <Popconfirm title={p.adminLocked ? 'Mở khóa tài khoản này?' : 'Khóa tài khoản này?'} description={p.adminLocked ? 'Bệnh nhân có thể đăng nhập lại sau khi mở khóa.' : 'Mọi phiên đăng nhập hiện có của bệnh nhân sẽ bị thu hồi.'} okText={p.adminLocked ? 'Mở khóa' : 'Khóa'} cancelText="Hủy" okButtonProps={{ danger: !p.adminLocked }} onConfirm={() => void toggle(p)}><Button size="small" danger={p.adminLocked} type={p.adminLocked ? 'primary' : 'default'} icon={p.adminLocked ? <UnlockOutlined /> : <LockOutlined />} loading={busyIds.has(p.id)} disabled={busyIds.has(p.id)}>{p.adminLocked ? 'Mở khóa' : 'Khóa'}</Button></Popconfirm> },
  ]

  return <CmsAppShell><div className="mx-auto max-w-6xl">
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3"><div><Text type="secondary">Người dùng</Text><Title level={2} className="mb-1! mt-1!"><UserOutlined className="mr-2" />Quản lý bệnh nhân</Title><Text type="secondary">Xem và khóa/mở tài khoản bệnh nhân. Khóa tài khoản sẽ thu hồi các phiên đăng nhập đang hoạt động.</Text></div></div>
    {error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}
    <Card className="rounded-2xl border-black/5 shadow-sm"><Space className="mb-4" wrap><Input.Search placeholder="Tìm theo tên hoặc email" allowClear value={query} onChange={(e) => setQuery(e.target.value)} onSearch={() => void reload(1)} style={{ width: 300 }} /><Select allowClear placeholder="Lọc trạng thái" value={locked} onChange={setLocked} options={[{ value: false, label: 'Đang hoạt động' }, { value: true, label: 'Đã khóa' }]} style={{ width: 160 }} /></Space><Table<CmsPatient> rowKey="id" loading={loading} columns={columns} dataSource={data} pagination={{ current: page, pageSize: 20, total, showSizeChanger: false, onChange: (next) => void reload(next) }} /></Card>
  </div></CmsAppShell>
}
