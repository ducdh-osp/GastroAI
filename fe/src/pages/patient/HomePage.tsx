import { Card, Typography } from 'antd'
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
          Chào mừng quay lại GastroAI — nền tảng theo dõi và chăm sóc sức khỏe tiêu hóa của bạn. Dùng menu bên trái
          để xem lịch sử đăng nhập hoặc đổi mật khẩu.
        </Paragraph>
      </Card>
    </AppShell>
  )
}
