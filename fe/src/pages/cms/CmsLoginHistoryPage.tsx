import { Alert, Card, Pagination, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { getCmsLoginHistory } from '../../api/cmsAuth'
import type { LoginHistoryItem } from '../../api/auth'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text } = Typography

type LoginStatus = 'SUCCESS' | 'FAILURE' | 'BLOCKED'

const statusLabels: Record<LoginStatus, string> = {
  SUCCESS: 'Thành công',
  FAILURE: 'Thất bại',
  BLOCKED: 'Đã chặn',
}

const statusColors: Record<LoginStatus, string> = {
  SUCCESS: 'green',
  FAILURE: 'orange',
  BLOCKED: 'red',
}

function formatDateTime(iso: string): string {
  try {
    return new Date(iso).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return iso
  }
}

const columns: ColumnsType<LoginHistoryItem> = [
  {
    title: 'Thời gian',
    dataIndex: 'attemptedAt',
    key: 'attemptedAt',
    width: 180,
    render: (val: string) => formatDateTime(val),
  },
  {
    title: 'Thiết bị',
    key: 'device',
    render: (_, record) => (
      <div>
        <Text strong>{record.deviceLabel ?? 'Không rõ thiết bị'}</Text>
        <div><Text type="secondary">{record.userAgent ?? '—'}</Text></div>
      </div>
    ),
  },
  {
    title: 'Địa chỉ IP',
    key: 'ip',
    render: (_, record) => <Text code>{record.ipAddress ?? '—'}</Text>,
  },
  {
    title: 'Lý do thất bại',
    dataIndex: 'failureReason',
    key: 'failureReason',
    render: (val: string | null) => (val ? <Text type="danger">{val}</Text> : <Text type="secondary">—</Text>),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'outcome',
    key: 'outcome',
    width: 130,
    render: (outcome: string) => {
      const status = outcome as LoginStatus
      return <Tag color={statusColors[status] ?? 'default'}>{statusLabels[status] ?? outcome}</Tag>
    },
  },
]

export default function CmsLoginHistoryPage() {
  const [items, setItems] = useState<LoginHistoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const pageSize = 20

  useEffect(() => {
    setLoading(true)
    setError(null)
    getCmsLoginHistory(page, pageSize)
      .then((res) => {
        setItems(res.items)
        setTotalElements(res.totalElements)
      })
      .catch((err: unknown) => {
        const msg =
          (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Không thể tải lịch sử đăng nhập.'
        setError(msg)
      })
      .finally(() => setLoading(false))
  }, [page])

  return (
    <CmsAppShell>
      <div className="mb-6">
        <Text type="secondary">Bảo mật tài khoản</Text>
        <Title level={2} className="mb-1! mt-1!">Lịch sử đăng nhập</Title>
        <Text type="secondary">Các lần đăng nhập gần đây vào tài khoản quản trị của bạn.</Text>
      </div>

      {error && <Alert type="error" showIcon message={error} className="mb-6" />}

      <Card className="rounded-2xl border-black/5 shadow-sm">
        <Spin spinning={loading}>
          <Table<LoginHistoryItem>
            columns={columns}
            dataSource={items}
            rowKey="id"
            pagination={false}
            scroll={{ x: 720 }}
            locale={{ emptyText: error ? 'Lỗi tải dữ liệu' : 'Chưa có lịch sử đăng nhập' }}
          />
        </Spin>

        {totalElements > pageSize && (
          <div className="mt-4 flex justify-end">
            <Pagination
              current={page + 1}
              pageSize={pageSize}
              total={totalElements}
              onChange={(p) => setPage(p - 1)}
              showSizeChanger={false}
            />
          </div>
        )}
      </Card>
    </CmsAppShell>
  )
}
