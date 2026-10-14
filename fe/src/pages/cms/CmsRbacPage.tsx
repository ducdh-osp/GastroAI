import { SafetyCertificateOutlined } from '@ant-design/icons'
import { Alert, Card, Input, Popconfirm, Select, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { assignCmsRole, listCmsAccounts, type CmsAccount, type CmsPermissionRole } from '../../api/cmsRbac'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography
const PAGE_SIZE = 20
const ROLE_LABEL: Record<CmsPermissionRole, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Bác sĩ',
  CONTENT_EDITOR: 'Biên tập nội dung',
}

type PendingRoleChange = { account: CmsAccount; role: CmsPermissionRole }

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
            disabled={a.accountType === 'ADMIN' || busy.has(key)}
            onChange={(role: CmsPermissionRole) => setPending({ account: a, role })}
            options={Object.entries(ROLE_LABEL).map(([value, label]) => ({ value, label, disabled: value === 'ADMIN' }))}
            style={{ width: 190 }}
          />
        </Popconfirm>
      },
    },
    { title: 'Ngày tạo', dataIndex: 'createdAt', key: 'createdAt', render: formatDateTime },
  ]

  return <CmsAppShell><div className="mx-auto max-w-6xl">
    <div className="mb-6"><Text type="secondary">Người dùng</Text><Title level={2} className="mb-1! mt-1!"><SafetyCertificateOutlined className="mr-2" />Phân quyền RBAC</Title><Text type="secondary">Ba vai trò thực thi: Admin, Bác sĩ và Biên tập nội dung. Thay đổi có hiệu lực ngay ở request kế tiếp.</Text></div>
    {error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}
    <Card className="rounded-2xl border-black/5 shadow-sm">
      <Space className="mb-4" wrap><Input.Search placeholder="Tìm theo tên hoặc email" allowClear value={query} onChange={(e) => setQuery(e.target.value)} onSearch={() => void reload(1)} style={{ width: 300 }} /></Space>
      <Table<CmsAccount> rowKey={(a) => `${a.accountType}-${a.id}`} loading={loading} columns={columns} dataSource={data} pagination={{ current: page, pageSize: PAGE_SIZE, total, showSizeChanger: false, onChange: (next) => void reload(next) }} />
    </Card>
  </div></CmsAppShell>
}
