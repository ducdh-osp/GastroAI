import { Alert, Button, Card, Pagination, Space, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getLoginHistory, type LoginHistoryItem } from '../../api/auth'

const { Title, Text } = Typography

// Map outcome từ BE → hiển thị
type LoginStatus = 'SUCCESS' | 'FAILED' | 'BLOCKED'

const statusLabels: Record<LoginStatus, string> = {
  SUCCESS: 'Thành công',
  FAILED: 'Thất bại',
  BLOCKED: 'Đã chặn',
}

const statusColors: Record<LoginStatus, string> = {
  SUCCESS: 'green',
  FAILED: 'orange',
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
    render: (_, record) => (
      <div>
        <Text code>{record.ipAddress ?? '—'}</Text>
      </div>
    ),
  },
  {
    title: 'Lý do thất bại',
    dataIndex: 'failureReason',
    key: 'failureReason',
    render: (val: string | null) => val ? <Text type="danger">{val}</Text> : <Text type="secondary">—</Text>,
  },
  {
    title: 'Trạng thái',
    dataIndex: 'outcome',
    key: 'outcome',
    width: 130,
    render: (outcome: string) => {
      const status = outcome as LoginStatus
      return (
        <Tag color={statusColors[status] ?? 'default'}>
          {statusLabels[status] ?? outcome}
        </Tag>
      )
    },
  },
]

export default function LoginHistoryPage() {
  const [items, setItems] = useState<LoginHistoryItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const pageSize = 20

  useEffect(() => {
    setLoading(true)
    setError(null)
    getLoginHistory(page, pageSize)
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

  const failedCount = items.filter((r) => r.outcome !== 'SUCCESS').length

  return (
    <div className="min-h-svh bg-slate-50 px-4 py-8 sm:px-8">
      <main className="mx-auto max-w-6xl">
        <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <Text type="secondary">Bảo mật tài khoản</Text>
            <Title level={2} className="mb-1! mt-1!">Lịch sử đăng nhập</Title>
            <Text type="secondary">Theo dõi các lần truy cập gần đây để phát hiện hoạt động bất thường.</Text>
          </div>
          <Space>
            <Button><Link to="/">Trang chủ</Link></Button>
            <Button type="primary"><Link className="text-white!" to="/login">Đăng nhập lại</Link></Button>
          </Space>
        </div>

        {error && (
          <Alert type="error" showIcon message={error} className="mb-6" />
        )}

        {!error && !loading && failedCount > 0 && (
          <Alert
            className="mb-6"
            type="warning"
            showIcon
            message="Có hoạt động cần kiểm tra"
            description={`${failedCount} lần truy cập gần đây không thành công hoặc đã bị chặn. Nếu không phải bạn, hãy đổi mật khẩu ngay.`}
            action={<Button size="small" type="link">Đổi mật khẩu</Button>}
          />
        )}

        <Card>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <Title level={4} className="mb-1!">Các lần đăng nhập gần đây</Title>
              <Text type="secondary">
                {loading ? 'Đang tải...' : `Tổng cộng ${totalElements} hoạt động`}
              </Text>
            </div>
            {!loading && <Tag color="blue">{totalElements} hoạt động</Tag>}
          </div>

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
      </main>
    </div>
  )
}
