import { SafetyCertificateOutlined } from '@ant-design/icons'
import { Alert, Card, Select, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { assignCmsRole, listCmsAccounts, type CmsAccount, type CmsPermissionRole } from '../../api/cmsRbac'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography
const ROLE_LABEL: Record<CmsPermissionRole, string> = { ADMIN: 'Admin', DOCTOR: 'Bác sĩ', CONTENT_EDITOR: 'Biên tập nội dung' }

export default function CmsRbacPage() {
  const [data, setData] = useState<CmsAccount[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState<number | null>(null)

  async function reload() {
    setLoading(true)
    try { const result = await listCmsAccounts({ size: 100 }); setData(result.items) } catch (err) { setError(err instanceof Error ? err.message : 'Không thể tải danh sách tài khoản CMS.') } finally { setLoading(false) }
  }
  useEffect(() => { void reload() }, [])

  async function update(account: CmsAccount, role: CmsPermissionRole) {
    setBusy(account.id)
    try { const updated = await assignCmsRole(account, role); setData((items) => items.map((item) => item.accountType === updated.accountType && item.id === updated.id ? updated : item)) } catch (err) { setError(err instanceof Error ? err.message : 'Không thể cập nhật vai trò.') } finally { setBusy(null) }
  }

  const columns: ColumnsType<CmsAccount> = [
    { title: 'Tài khoản', key: 'account', render: (_, a) => <div><div className="font-medium">{a.fullName || 'Chưa cập nhật'}</div><Text type="secondary">{a.email}</Text></div> },
    { title: 'Loại tài khoản', dataIndex: 'accountType', key: 'accountType', render: (v: string) => <Tag>{v === 'ADMIN' ? 'Admin' : 'Tài khoản bác sĩ'}</Tag> },
    { title: 'Vai trò RBAC', key: 'role', render: (_, a) => <Select value={a.role} loading={busy === a.id} disabled={a.accountType === 'ADMIN'} onChange={(role: CmsPermissionRole) => void update(a, role)} options={Object.entries(ROLE_LABEL).map(([value, label]) => ({ value, label, disabled: value === 'ADMIN' }))} style={{ width: 190 }} /> },
    { title: 'Ngày tạo', dataIndex: 'createdAt', key: 'createdAt', render: formatDateTime },
  ]
  return <CmsAppShell><div className="mx-auto max-w-5xl"><div className="mb-6"><Text type="secondary">Người dùng</Text><Title level={2} className="mb-1! mt-1!"><SafetyCertificateOutlined className="mr-2" />Phân quyền RBAC</Title><Text type="secondary">Ba vai trò thực thi: Admin, Bác sĩ và Biên tập nội dung. Thay đổi có hiệu lực ngay ở request kế tiếp.</Text></div>{error && <Alert type="error" message={error} showIcon className="mb-4" closable onClose={() => setError(null)} />}<Card className="rounded-2xl border-black/5 shadow-sm"><Table<CmsAccount> rowKey={(a) => `${a.accountType}-${a.id}`} loading={loading} columns={columns} dataSource={data} pagination={false} /></Card></div></CmsAppShell>
}
