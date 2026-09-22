import { Button, Space, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { useAuth } from '../../stores/authStore'

const { Title, Paragraph } = Typography

export default function HomePage() {
  const { email, fullName, logout } = useAuth()

  return (
    <div className="min-h-svh flex items-center justify-center">
      <div className="text-center">
        <Title level={2}>Xin chào, {fullName ?? email}</Title>
        <Paragraph type="secondary">Đăng nhập thành công (UC0002).</Paragraph>
        <Space>
          <Button type="primary"><Link className="text-white!" to="/login-history">Lịch sử đăng nhập</Link></Button>
          <Button onClick={logout}>Đăng xuất</Button>
        </Space>
      </div>
    </div>
  )
}
