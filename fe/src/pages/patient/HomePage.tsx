import { Button, Typography } from 'antd'
import { useAuth } from '../../stores/authStore'

const { Title, Paragraph } = Typography

export default function HomePage() {
  const { email, fullName, logout } = useAuth()

  return (
    <div className="min-h-svh flex items-center justify-center">
      <div className="text-center">
        <Title level={2}>Xin chào, {fullName ?? email}</Title>
        <Paragraph type="secondary">Đăng nhập thành công (UC0002).</Paragraph>
        <Button onClick={logout}>Đăng xuất</Button>
      </div>
    </div>
  )
}
