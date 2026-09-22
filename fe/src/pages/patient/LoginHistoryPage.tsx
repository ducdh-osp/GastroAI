import { Alert, Button, Card, Space, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { Link } from 'react-router-dom'

const { Title, Text } = Typography

type LoginStatus = 'success' | 'failed' | 'blocked'

interface LoginRecord {
  key: string
  time: string
  device: string
  browser: string
  ip: string
  location: string
  status: LoginStatus
}

const mockLoginHistory: LoginRecord[] = [
  {
    key: '1',
    time: '22/09/2026, 18:42',
    device: 'Windows PC',
    browser: 'Chrome 140',
    ip: '192.168.1.12',
    location: 'Mạng nội bộ',
    status: 'success',
  },
  {
    key: '2',
    time: '22/09/2026, 17:15',
    device: 'iPhone 15',
    browser: 'Safari Mobile',
    ip: '113.161.42.18',
    location: 'Ho Chi Minh City',
    status: 'success',
  },
  {
    key: '3',
    time: '21/09/2026, 23:08',
    device: 'Unknown device',
    browser: 'Firefox 141',
    ip: '45.77.18.203',
    location: 'Unknown location',
    status: 'failed',
  },
  {
    key: '4',
    time: '21/09/2026, 23:07',
    device: 'Unknown device',
    browser: 'Firefox 141',
    ip: '45.77.18.203',
    location: 'Unknown location',
    status: 'blocked',
  },
  {
    key: '5',
    time: '20/09/2026, 09:30',
    device: 'MacBook Pro',
    browser: 'Chrome 140',
    ip: '10.0.0.24',
    location: 'Mạng nội bộ',
    status: 'success',
  },
]

const statusLabels: Record<LoginStatus, string> = {
  success: 'Thành công',
  failed: 'Thất bại',
  blocked: 'Đã chặn',
}

const statusColors: Record<LoginStatus, string> = {
  success: 'green',
  failed: 'orange',
  blocked: 'red',
}

const columns: ColumnsType<LoginRecord> = [
  {
    title: 'Thời gian',
    dataIndex: 'time',
    key: 'time',
    width: 180,
  },
  {
    title: 'Thiết bị',
    key: 'device',
    render: (_, record) => (
      <div>
        <Text strong>{record.device}</Text>
        <div><Text type="secondary">{record.browser}</Text></div>
      </div>
    ),
  },
  {
    title: 'Địa chỉ IP',
    dataIndex: 'ip',
    key: 'ip',
    render: (ip: string, record) => (
      <div>
        <Text code>{ip}</Text>
        <div><Text type="secondary">{record.location}</Text></div>
      </div>
    ),
  },
  {
    title: 'Trạng thái',
    dataIndex: 'status',
    key: 'status',
    width: 130,
    render: (status: LoginStatus) => (
      <Tag color={statusColors[status]}>{statusLabels[status]}</Tag>
    ),
  },
]

export default function LoginHistoryPage() {
  const failedAttempts = mockLoginHistory.filter((record) => record.status !== 'success').length

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

        {failedAttempts > 0 && (
          <Alert
            className="mb-6"
            type="warning"
            showIcon
            title="Có hoạt động cần kiểm tra"
            description={`${failedAttempts} lần truy cập gần đây không thành công hoặc đã bị chặn. Nếu không phải bạn, hãy đổi mật khẩu ngay.`}
            action={<Button size="small" type="link">Đổi mật khẩu</Button>}
          />
        )}

        <Card>
          <div className="mb-4 flex items-center justify-between">
            <div>
              <Title level={4} className="mb-1!">Các lần đăng nhập gần đây</Title>
              <Text type="secondary">Dữ liệu minh họa cho nền FE, sẽ kết nối API BE sau.</Text>
            </div>
            <Tag color="blue">5 hoạt động</Tag>
          </div>
          <Table<LoginRecord>
            columns={columns}
            dataSource={mockLoginHistory}
            pagination={false}
            scroll={{ x: 720 }}
          />
        </Card>
      </main>
    </div>
  )
}
