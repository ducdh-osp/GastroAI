import { Alert, Button, Card, Pagination, Spin, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getLoginHistory, type LoginHistoryItem } from '../../api/auth'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text } = Typography

// Map outcome từ BE → hiển thị
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
  const [recentFailureCount, setRecentFailureCount] = useState(0)
  const pageSize = 20

  useEffect(() => {
    setLoading(true)
    setError(null)
    getLoginHistory(page, pageSize)
      .then((res) => {
        setItems(res.items)
        setTotalElements(res.totalElements)
        setRecentFailureCount(res.recentFailureCount)
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
    <AppShell>
      <div className="mb-6">
        <Text type="secondary">Bảo mật tài khoản</Text>
        <Title level={2} className="mb-1! mt-1!">Lịch sử đăng nhập</Title>
        <Text type="secondary">Theo dõi các lần truy cập gần đây để phát hiện hoạt động bất thường.</Text>
      </div>

      {error && (
        <Alert type="error" showIcon message={error} className="mb-6" />
      )}

      {!error && !loading && recentFailureCount > 0 && (
        <Alert
          className="mb-6"
          type="warning"
          showIcon
          message="Có hoạt động cần kiểm tra"
          description={`${recentFailureCount} lần truy cập không thành công hoặc bị chặn trong 7 ngày qua. Nếu không phải bạn, hãy đổi mật khẩu ngay.`}
          action={<Button size="small" type="link"><Link to="/change-password">Đổi mật khẩu</Link></Button>}
        />
      )}

      <Card className="rounded-2xl border-black/5 shadow-sm">
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
    </AppShell>
  )
}
