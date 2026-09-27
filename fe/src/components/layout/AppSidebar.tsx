import { CommentOutlined, DownOutlined, HistoryOutlined, HomeOutlined, KeyOutlined, LogoutOutlined } from '@ant-design/icons'
import { Dropdown } from 'antd'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../stores/authStore'
import { Logo } from '../brand/Logo'

const NAV_ITEMS = [
  { key: 'home', label: 'Trang chủ', icon: <HomeOutlined />, to: '/' },
  { key: 'chat', label: 'Tư vấn sức khỏe', icon: <CommentOutlined />, to: '/chat' },
  { key: 'login-history', label: 'Lịch sử đăng nhập', icon: <HistoryOutlined />, to: '/login-history' },
  { key: 'change-password', label: 'Đổi mật khẩu', icon: <KeyOutlined />, to: '/change-password' },
]

/** Sidebar cố định cho các trang bệnh nhân sau khi đăng nhập — cùng cấu trúc với CmsSidebar
 * (bên admin) và cùng nền tối, nhưng dùng tông xanh ngọc đậm (#0f2926) thay vì slate để 2
 * cổng vẫn phân biệt được bằng mắt dù cùng kiểu "dashboard tối". */
export function AppSidebar() {
  const { email, fullName, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <aside className="flex h-svh w-64 shrink-0 flex-col justify-between border-r border-white/10 bg-[#0f2926] text-white">
      <div>
        <div className="p-5">
          <Logo variant="light" />
        </div>

        <nav className="mt-2 space-y-0.5 px-3">
          {NAV_ITEMS.map((item) => {
            const active = item.to === location.pathname
            return (
              <Link
                key={item.key}
                to={item.to}
                className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm transition-colors ${
                  active ? 'bg-teal-500/15 text-teal-300' : 'text-white/80 hover:bg-white/5'
                }`}
              >
                {item.icon}
                {item.label}
              </Link>
            )
          })}
        </nav>
      </div>

      <div className="border-t border-white/10 p-3">
        <Dropdown
          menu={{
            items: [{ key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true, onClick: handleLogout }],
          }}
          trigger={['click']}
        >
          <button className="flex w-full items-center gap-2.5 rounded-lg px-2 py-2 text-left hover:bg-white/5">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-teal-500/20 text-sm font-semibold text-teal-300">
              {(fullName ?? email ?? '?').charAt(0).toUpperCase()}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-medium text-white">{fullName ?? email}</span>
            </span>
            <DownOutlined className="text-xs text-white/40" />
          </button>
        </Dropdown>
      </div>
    </aside>
  )
}
