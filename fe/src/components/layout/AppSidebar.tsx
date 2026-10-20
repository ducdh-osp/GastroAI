import {
  BarChartOutlined,
  BellOutlined,
  BulbFilled,
  BulbOutlined,
  CoffeeOutlined,
  CommentOutlined,
  DownOutlined,
  ExperimentOutlined,
  FilePdfOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  FireOutlined,
  HistoryOutlined,
  HomeOutlined,
  KeyOutlined,
  LineChartOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  MessageOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons'
import { Badge, Dropdown, notification, Switch } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../stores/authStore'
import { useTheme } from '../../stores/themeStore'
import { Logo } from '../brand/Logo'
import { getUnreadNotificationCount } from '../../api/notifications'
import { useNotificationSocket } from '../../hooks/useNotificationSocket'

const NAV_ITEMS = [
  { key: 'notifications', label: 'Thông báo', icon: <BellOutlined />, to: '/notifications' },
  { key: 'home', label: 'Trang chủ', icon: <HomeOutlined />, to: '/' },
  { key: 'chat', label: 'Tư vấn sức khỏe', icon: <CommentOutlined />, to: '/chat' },
  { key: 'chat-history', label: 'Lịch sử phiên chat', icon: <MessageOutlined />, to: '/chat-history' },
  { key: 'articles', label: 'Bài viết tiêu hoá', icon: <FileSearchOutlined />, to: '/articles' },
  { key: 'faq', label: 'Hỏi đáp thường gặp', icon: <QuestionCircleOutlined />, to: '/faq' },
  { key: 'medical-profile', label: 'Hồ sơ bệnh lý', icon: <FileTextOutlined />, to: '/medical-profile' },
  { key: 'food-diary', label: 'Nhật ký ăn uống', icon: <CoffeeOutlined />, to: '/food-diary' },
  { key: 'food-diary-history', label: 'Lịch sử ăn uống', icon: <BarChartOutlined />, to: '/food-diary-history' },
  { key: 'food-sensitivity', label: 'Nhạy cảm thực phẩm', icon: <BulbOutlined />, to: '/food-sensitivity' },
  { key: 'adherence', label: 'Streak & tuân thủ', icon: <FireOutlined />, to: '/adherence' },
  { key: 'bristol', label: 'Nhật ký Bristol', icon: <ExperimentOutlined />, to: '/bristol' },
  { key: 'bristol-history', label: 'Xu hướng Bristol', icon: <LineChartOutlined />, to: '/bristol-history' },
  { key: 'medications', label: 'Nhắc uống thuốc', icon: <MedicineBoxOutlined />, to: '/medications' },
  { key: 'health-report', label: 'Xuất nhật ký sức khỏe', icon: <FilePdfOutlined />, to: '/health-report' },
  { key: 'login-history', label: 'Lịch sử đăng nhập', icon: <HistoryOutlined />, to: '/login-history' },
  { key: 'change-password', label: 'Đổi mật khẩu', icon: <KeyOutlined />, to: '/change-password' },
]


/** Sidebar cố định cho các trang bệnh nhân sau khi đăng nhập — cùng cấu trúc với CmsSidebar
 * (bên admin) và cùng nền tối, nhưng dùng tông xanh ngọc đậm (#0f2926) thay vì slate để 2
 * cổng vẫn phân biệt được bằng mắt dù cùng kiểu "dashboard tối". */
export function AppSidebar() {
  const { email, fullName, logout, token } = useAuth()
  const { mode, toggle: toggleTheme } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    const refreshUnreadCount = () => {
      void getUnreadNotificationCount('patient').then(setUnreadCount).catch(() => {})
    }
    refreshUnreadCount()
    window.addEventListener('gastroai:notification-state', refreshUnreadCount)
    return () => window.removeEventListener('gastroai:notification-state', refreshUnreadCount)
  }, [])

  useNotificationSocket({
    audience: 'patient',
    enabled: Boolean(token),
    token,
    onNotification: useCallback((item) => {
      setUnreadCount((count) => count + 1)
      notification.info({ message: item.title, description: item.message, placement: 'topRight' })
      window.dispatchEvent(new Event('gastroai:notification'))
    }, []),
  })

  async function handleLogout() {
    await logout()
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
                <span className="flex min-w-0 flex-1 items-center justify-between gap-2">
                  {item.label}
                  {item.key === 'notifications' && unreadCount > 0 && (
                    <Badge count={unreadCount} overflowCount={99} size="small" />
                  )}
                </span>
              </Link>
            )
          })}
        </nav>
      </div>

      <div className="border-t border-white/10 p-3">
        <div className="mb-1 flex items-center justify-between gap-2.5 rounded-lg px-2 py-2 text-sm text-white/80">
          <span className="flex items-center gap-2.5">
            {mode === 'dark' ? <BulbFilled /> : <BulbOutlined />}
            Chế độ tối
          </span>
          <Switch size="small" checked={mode === 'dark'} onChange={toggleTheme} />
        </div>
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
