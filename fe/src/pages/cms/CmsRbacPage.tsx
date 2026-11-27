import { LockOutlined, SafetyCertificateOutlined, UnlockOutlined, UserAddOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Input, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  assignCmsRole,
  lockCmsAccount,
  listCmsAccounts,
  unlockCmsAccount,
  type CmsAccount,
  type CmsPermissionRole,
} from '../../api/cmsRbac'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography
const PAGE_SIZE = 20
const ROLE_LABEL: Record<CmsPermissionRole, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Bác sĩ',
  CONTENT_EDITOR: 'Biên tập chuyên môn y khoa',
  CONTENT_AUTHOR: 'Cộng tác viên nội dung',
  OPERATIONS: 'Giám sát vận hành',
}

type PendingRoleChange = { account: CmsAccount; role: CmsPermissionRole }

function roleOptions(account: CmsAccount) {
  const allowedRoles: CmsPermissionRole[] = account.accountType === 'ADMIN'
    ? ['ADMIN', 'CONTENT_EDITOR', 'CONTENT_AUTHOR', 'OPERATIONS']
    : ['DOCTOR']

  return allowedRoles.map((value) => ({ value, label: ROLE_LABEL[value] }))
}

function accountKey(account: CmsAccount) {
  return `${account.accountType}-${account.id}`
}

export default function CmsRbacPage() {
  const [data, setData] = useState<CmsAccount[]>([])
  const [query, setQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(1)
  const [total, setTotal] = useState(0)
  const [busy, setBusy] = useState<Set<string>>(() => new Set())
  const [pending, setPending] = useState<PendingRoleChange | null>(null)

  async function reload(nextPage = page) {
    setLoading(true)
    setError(null)
    try {
      const result = await listCmsAccounts({ q: query || undefined, page: nextPage - 1, size: PAGE_SIZE })
      setData(result.items)
      setTotal(result.totalElements)
      setPage(result.page + 1)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể tải danh sách tài khoản CMS.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { void reload(1) }, [])

  async function update(account: CmsAccount, role: CmsPermissionRole) {
    const key = accountKey(account)
    setBusy((current) => new Set(current).add(key))
    try {
      const updated = await assignCmsRole(account, role)
      setData((items) => items.map((item) => accountKey(item) === accountKey(updated) ? updated : item))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể cập nhật vai trò.')
    } finally {
      setBusy((current) => {
        const next = new Set(current)
        next.delete(key)
        return next
      })
    }
  }

  async function toggleLock(account: CmsAccount) {
    const key = accountKey(account)
    setBusy((current) => new Set(current).add(key))
    try {
      const updated = account.adminLocked ? await unlockCmsAccount(account.id) : await lockCmsAccount(account.id)
      setData((items) => items.map((item) => accountKey(item) === accountKey(updated) ? updated : item))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Không thể đổi trạng thái khoá.')
    } finally {
      setBusy((current) => {
        const next = new Set(current)
        next.delete(key)
        return next
      })
    }
  }

  const columns: ColumnsType<CmsAccount> = [
    { title: 'Tài khoản', key: 'account', render: (_, a) => <div><div className="font-medium">{a.fullName || 'Chưa cập nhật'}</div><Text type="secondary">{a.email}</Text></div> },
    { title: 'Loại tài khoản', dataIndex: 'accountType', key: 'accountType', render: (v: string) => <Tag>{v === 'ADMIN' ? 'Admin' : 'Tài khoản bác sĩ'}</Tag> },
    {
      title: 'Vai trò RBAC', key: 'role', render: (_, a) => {
        const key = accountKey(a)
        const isPending = pending?.account.accountType === a.accountType && pending.account.id === a.id
        const pendingLabel = isPending && pending ? ROLE_LABEL[pending.role] : ROLE_LABEL[a.role]
        return <Popconfirm
          title={`Xác nhận đổi vai trò thành ${pendingLabel}?`}
          description={`Vai trò hiện tại: ${ROLE_LABEL[a.role]} → vai trò mới: ${pendingLabel}.`}
          okText="Xác nhận"
          cancelText="Hủy"
          okButtonProps={{ danger: true }}
          open={isPending}
          onOpenChange={(open) => { if (!open) setPending(null) }}
          onCancel={() => setPending(null)}
          onConfirm={() => {
            if (!pending) return
            const change = pending
            setPending(null)
            void update(change.account, change.role)
          }}
        >
          <Select
            value={a.role}
            loading={busy.has(key)}
            disabled={a.accountType === 'DOCTOR' || busy.has(key)}
            onChange={(role: CmsPermissionRole) => setPending({ account: a, role })}
            options={roleOptions(a)}
            style={{ width: 220 }}
          />
        </Popconfirm>
      },
    },
    { title: 'Ngày tạo', dataIndex: 'createdAt', key: 'createdAt', render: formatDateTime },
    {
      title: 'Trạng thái', key: 'status', render: (_, a) =>
        a.adminLocked ? <Tag color="red">Đã khoá</Tag> : <Tag color="green">Đang hoạt động</Tag>,
    },
    {
      title: 'Thao tác', key: 'actions', render: (_, a) => {
        if (a.accountType !== 'ADMIN') return null
        const key = accountKey(a)
        return (
          <Popconfirm
            title={a.adminLocked ? 'Mở khoá tài khoản này?' : 'Khoá tài khoản này?'}
            description={a.adminLocked
              ? 'Tài khoản sẽ đăng nhập lại được bình thường.'
              : 'Tài khoản sẽ không đăng nhập được nữa, phiên hiện tại (nếu có) bị huỷ ngay.'}
            okText="Xác nhận"
            cancelText="Huỷ"
            okButtonProps={{ danger: !a.adminLocked }}
            onConfirm={() => void toggleLock(a)}
          >
            <Button
              size="small"
              danger={!a.adminLocked}
              loading={busy.has(key)}
              icon={a.adminLocked ? <UnlockOutlined /> : <LockOutlined />}
            >
              {a.adminLocked ? 'Mở khoá' : 'Khoá'}
            </Button>
          </Popconfirm>
        )
      },
    },
  ]

  return <CmsAppShell><div className="w-full">
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div>
        <Text type="secondary">Người dùng</Text>
        <Title level={2} className="mb-1! mt-1!"><SafetyCertificateOutlined className="mr-2" />Phân quyền RBAC</Title>
        <Text type="secondary">4 vai trò nội bộ: Admin, Biên tập chuyên môn y khoa, Cộng tác viên nội dung, Giám sát vận hành. Tài khoản Bác sĩ (cũ) luôn giữ nguyên vai trò. Thay đổi có hiệu lực ngay ở request kế tiếp.</Text>
      </div>
      <Link to="/cms/rbac/new">
        <Button type="primary" icon={<UserAddOutlined />}>Tạo tài khoản mới</Button>
      </Link>
    </div>
    {error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}
    <Card className="rounded-2xl border-black/5 shadow-sm dark:border-neutral-700! dark:bg-neutral-800!">
      <Space className="mb-4" wrap><Input.Search placeholder="Tìm theo tên hoặc email" allowClear value={query} onChange={(e) => setQuery(e.target.value)} onSearch={() => void reload(1)} style={{ width: 300 }} /></Space>
      <Table<CmsAccount> rowKey={(a) => `${a.accountType}-${a.id}`} loading={loading} columns={columns} dataSource={data} pagination={{ current: page, pageSize: PAGE_SIZE, total, showSizeChanger: false, onChange: (next) => void reload(next) }} />
    </Card>
  </div></CmsAppShell>
}
