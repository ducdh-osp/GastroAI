import { DownOutlined, HistoryOutlined, KeyOutlined, LogoutOutlined } from '@ant-design/icons'
import { Button, Dropdown } from 'antd'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../stores/authStore'
import { Logo } from '../brand/Logo'

/** Thanh điều hướng trên cùng, dùng cho các trang bệnh nhân sau khi đăng nhập. */
export function AppHeader() {
  const { email, fullName, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <header className="sticky top-0 z-10 border-b border-black/5 bg-white/90 backdrop-blur-sm">
      <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-4 sm:px-6">
        <Link to="/">
          <Logo />
        </Link>

        <Dropdown
          menu={{
            items: [
              {
                key: 'login-history',
                icon: <HistoryOutlined />,
                label: <Link to="/login-history">Lịch sử đăng nhập</Link>,
              },
              {
                key: 'change-password',
                icon: <KeyOutlined />,
                label: <Link to="/change-password">Đổi mật khẩu</Link>,
              },
              { type: 'divider' },
              {
                key: 'logout',
                icon: <LogoutOutlined />,
                label: 'Đăng xuất',
                danger: true,
                onClick: handleLogout,
              },
            ],
          }}
          trigger={['click']}
        >
          <Button type="text" className="h-auto! px-2! py-1!">
            <span className="max-w-40 truncate text-sm font-medium text-[#0f2926]">{fullName ?? email}</span>
            <DownOutlined className="text-xs text-gray-400" />
          </Button>
        </Dropdown>
      </div>
    </header>
  )
}
