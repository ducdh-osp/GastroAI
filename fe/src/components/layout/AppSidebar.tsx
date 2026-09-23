import { DownOutlined, HistoryOutlined, HomeOutlined, KeyOutlined, LogoutOutlined } from '@ant-design/icons'
import { Dropdown } from 'antd'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../stores/authStore'
import { Logo } from '../brand/Logo'

const NAV_ITEMS = [
  { key: 'home', label: 'Trang chủ', icon: <HomeOutlined />, to: '/' },
  { key: 'login-history', label: 'Lịch sử đăng nhập', icon: <HistoryOutlined />, to: '/login-history' },
  { key: 'change-password', label: 'Đổi mật khẩu', icon: <KeyOutlined />, to: '/change-password' },
]

/** Sidebar cố định cho các trang bệnh nhân sau khi đăng nhập — cùng cấu trúc với CmsSidebar
 * (bên admin) nhưng tông sáng/xanh ngọc để 2 cổng vẫn phân biệt được bằng mắt. */
export function AppSidebar() {
  const { email, fullName, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <aside className="flex h-svh w-64 shrink-0 flex-col justify-between border-r border-black/5 bg-white">
      <div>
        <div className="p-5">
          <Logo />
        </div>

        <nav className="mt-2 space-y-0.5 px-3">
          {NAV_ITEMS.map((item) => {
            const active = item.to === location.pathname
            return (
              <Link
                key={item.key}
                to={item.to}
                className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors ${
                  active ? 'bg-teal-50 text-teal-700' : 'text-gray-600 hover:bg-gray-50'
                }`}
              >
                {item.icon}
                {item.label}
              </Link>
            )
          })}
        </nav>
      </div>

      <div className="border-t border-black/5 p-3">
        <Dropdown
          menu={{
            items: [{ key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true, onClick: handleLogout }],
          }}
          trigger={['click']}
        >
          <button className="flex w-full items-center gap-2.5 rounded-lg px-2 py-2 text-left hover:bg-gray-50">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-teal-50 text-sm font-semibold text-teal-700">
              {(fullName ?? email ?? '?').charAt(0).toUpperCase()}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium text-[#0f2926]">{fullName ?? email}</span>
            </span>
            <DownOutlined className="text-xs text-gray-400" />
          </button>
        </Dropdown>
      </div>
    </aside>
  )
}
