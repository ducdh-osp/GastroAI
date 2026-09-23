import { HistoryOutlined } from '@ant-design/icons'
import { Button, Card, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { AppShell } from '../../components/layout/AppShell'
import { useAuth } from '../../stores/authStore'

const { Title, Paragraph } = Typography

export default function HomePage() {
  const { email, fullName } = useAuth()

  return (
    <AppShell>
      <Card className="rounded-2xl border-black/5 shadow-sm">
        <Title level={3} className="mb-1!">Xin chào, {fullName ?? email} 👋</Title>
        <Paragraph type="secondary" className="mb-0!">
          Chào mừng quay lại GastroAI — nền tảng theo dõi và chăm sóc sức khỏe tiêu hóa của bạn.
        </Paragraph>
      </Card>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <Card className="rounded-2xl border-black/5 shadow-sm">
          <div className="flex items-center gap-3">
            <span className="flex h-10 w-10 items-center justify-center rounded-full bg-teal-50 text-teal-700">
              <HistoryOutlined />
            </span>
            <div>
              <Title level={5} className="mb-0!">Lịch sử đăng nhập</Title>
              <Paragraph type="secondary" className="mb-0! text-sm">Kiểm tra các lần truy cập gần đây vào tài khoản.</Paragraph>
            </div>
          </div>
          <Link to="/login-history">
            <Button type="primary" className="mt-4" block>Xem lịch sử</Button>
          </Link>
        </Card>
      </div>
    </AppShell>
  )
}
