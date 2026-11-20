import {
  BulbFilled,
  BulbOutlined,
  DownOutlined,
  HistoryOutlined,
  HomeOutlined,
  KeyOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import { Badge, Dropdown, notification, Switch } from 'antd'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Logo } from '../brand/Logo'
import { NotificationBell } from '../layout/NotificationBell'
import { filterCmsNavGroups } from './cmsNav'
import { useCmsAuth } from '../../stores/cmsAuthStore'
import { useTheme } from '../../stores/themeStore'
import { getUnreadNotificationCount } from '../../api/notifications'
import { useNotificationSocket } from '../../hooks/useNotificationSocket'
import { listTriageAlerts } from '../../api/cmsTriage'
import { useTriageAlertSocket, type TriageAlertStatusChangedSocketEvent } from '../../hooks/useTriageAlertSocket'

/** Phòng khi mất kết nối socket: vẫn tự cập nhật số ca cần can thiệp sau mỗi khoảng này. */
const TRIAGE_COUNT_POLL_MS = 60_000

// So khớp "đường dẫn hiện tại thuộc mục nav nào" — phải so bằng hoặc có dấu "/" ngay sau, tránh
// startsWith đơn thuần nhận nhầm 2 route có tiền tố giống nhau là cùng 1 mục.
function isActivePath(pathname: string, to: string): boolean {
  return pathname === to || pathname.startsWith(`${to}/`)
}

// Nhãn 1 mục nav, kèm badge số ca "cần can thiệp" nếu > 0 (hiện tại chỉ mục Cảnh báo khẩn cấp dùng).
function navItemLabel(title: string, interventionCount: number) {
  if (interventionCount <= 0) return title
  return (
    <span className="flex min-w-0 flex-1 items-center justify-between gap-2">
      {title}
      <Badge count={interventionCount} overflowCount={99} size="small" title={`${interventionCount} ca cần can thiệp`} />
    </span>
  )
}

const ROLE_LABEL: Record<string, string> = {
  ADMIN: 'Admin',
  DOCTOR: 'Bác sĩ',
  CONTENT_EDITOR: 'Biên tập nội dung',
}

/** Header cố định cho mọi trang CMS sau đăng nhập — thay cho CmsSidebar cũ (xem
 * 00-General/04-QuanTri-AdminCMS/SCR-AD-00-TrangChu.md). Biểu tượng GastroAI chỉ là thương
 * hiệu tĩnh (không phải nút) — điều hướng đi qua Trang chủ + các nhóm dropdown ngay trên
 * header, cùng mẫu với AppHeader bên bệnh nhân. */
export function CmsHeader() {
  const { user, logout } = useCmsAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { mode, toggle: toggleTheme } = useTheme()
  const [unreadCount, setUnreadCount] = useState(0)
  const navGroups = filterCmsNavGroups(user?.userType)

  useEffect(() => {
    if (user?.userType !== 'ADMIN') return
    const refreshUnreadCount = () => {
      void getUnreadNotificationCount('admin').then(setUnreadCount).catch(() => { })
    }
    refreshUnreadCount()
    window.addEventListener('gastroai:notification-state', refreshUnreadCount)
    return () => window.removeEventListener('gastroai:notification-state', refreshUnreadCount)
  }, [user?.userType])

  // Số ca "Cần can thiệp" (SCR-AD-13) hiện cạnh mục "Cảnh báo khẩn cấp" - chỉ đếm, không tải danh sách.
  const canSeeTriage = user?.userType === 'ADMIN'
  const [interventionCount, setInterventionCount] = useState(0)
  const countSeq = useRef(0)
  const refreshInterventionCount = useCallback(() => {
    if (!canSeeTriage) return
    const mySeq = ++countSeq.current
    listTriageAlerts('INTERVENTION', 0, 1)
      .then((result) => {
        // Nhiều sự kiện tới dồn dập -> chỉ lấy kết quả của lần gọi mới nhất.
        if (mySeq === countSeq.current) setInterventionCount(result.totalElements)
      })
      .catch(() => undefined)
  }, [canSeeTriage])

  useEffect(() => {
    if (!canSeeTriage) return
    refreshInterventionCount()
    const timer = window.setInterval(refreshInterventionCount, TRIAGE_COUNT_POLL_MS)
    return () => window.clearInterval(timer)
  }, [canSeeTriage, refreshInterventionCount])

  // Bộ hẹn giờ chuyển ca, Admin nhận/đóng ca, bệnh nhân bấm An toàn... đều phát sự kiện đổi trạng thái.
  useTriageAlertSocket({
    enabled: canSeeTriage,
    onAlert: refreshInterventionCount,
    onStatusChange: useCallback((event: TriageAlertStatusChangedSocketEvent) => {
      refreshInterventionCount()
      if (event.status === 'RESOLVED') {
        // BE đã đánh dấu đã đọc mọi thông báo của ca vừa đóng -> đóng popup đỏ, cập nhật số chưa đọc.
        notification.destroy(`triage-intervention-${event.id}`)
        window.dispatchEvent(new Event('gastroai:notification-state'))
        window.dispatchEvent(new Event('gastroai:notification'))
      }
    }, [refreshInterventionCount]),
    onConnected: refreshInterventionCount,
  })

  useNotificationSocket({
    audience: 'admin',
    enabled: user?.userType === 'ADMIN',
    onNotification: useCallback((item) => {
      setUnreadCount((count) => count + 1)
      if (item.type === 'TRIAGE_INTERVENTION') {
        // SCR-AD-13 bước 3/4: báo mọi Admin đang trực, giữ trên màn tới khi có người bấm vào/đóng.
        // Key theo ca (không theo thông báo): báo động lại cùng ca thì cập nhật popup cũ, đóng ca thì gỡ được.
        const key = `triage-intervention-${item.sourceId ?? item.id}`
        notification.error({
          key,
          message: item.title,
          description: (
            <div className="space-y-2">
              <p className="mb-0 text-sm">{item.message}</p>
              <a
                onClick={() => {
                  notification.destroy(key)
                  navigate('/cms/triage-alerts')
                }}
              >
                Mở danh sách cần can thiệp
              </a>
            </div>
          ),
          placement: 'topRight',
          duration: 0,
        })
      } else {
        notification.warning({ message: item.title, description: item.message, placement: 'topRight' })
      }
      window.dispatchEvent(new Event('gastroai:notification'))
    }, [navigate]),
  })

  return (
    <header className="flex h-16 shrink-0 items-center gap-4 border-b border-white/10 bg-[#0f172a] px-4 text-white sm:px-6">
      <span className="flex shrink-0 items-center">
        <Logo variant="light" />
      </span>

      <nav className="hidden min-w-0 flex-1 items-center gap-1 md:flex">
        <Link
          to="/cms"
          className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm transition-colors ${
            location.pathname === '/cms' ? 'bg-white/10 text-white' : 'text-white/75 hover:bg-white/5 hover:text-white'
          }`}
        >
          <HomeOutlined />
          Trang chủ
        </Link>

        {navGroups.map((group) => {
          const active = group.items.some((item) => isActivePath(location.pathname, item.to))
          const groupInterventionCount = group.items.some((item) => item.to === '/cms/triage-alerts')
            ? interventionCount
            : 0
          return (
            <Dropdown
              key={group.key}
              trigger={['hover', 'click']}
              menu={{
                items: group.items.map((item) => ({
                  key: item.to,
                  icon: <item.icon />,
                  label: navItemLabel(item.title, item.to === '/cms/triage-alerts' ? interventionCount : 0),
                  onClick: () => navigate(item.to),
                })),
              }}
            >
              <button
                type="button"
                className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm transition-colors ${
                  active ? 'bg-white/10 text-white' : 'text-white/75 hover:bg-white/5 hover:text-white'
                }`}
              >
                {group.title}
                {groupInterventionCount > 0 && (
                  <Badge count={groupInterventionCount} overflowCount={99} size="small" />
                )}
                <DownOutlined className="text-[0.6rem]" />
              </button>
            </Dropdown>
          )
        })}
      </nav>

      {/* Menu rút gọn cho màn hình nhỏ — gộp Trang chủ + các nhóm vào 1 dropdown duy nhất. */}
      <div className="flex-1 md:hidden">
        <Dropdown
          trigger={['click']}
          menu={{
            items: [
              { key: '/cms', icon: <HomeOutlined />, label: <Link to="/cms">Trang chủ</Link> },
              ...navGroups.map((group) => ({
                key: group.key,
                label: group.title,
                children: group.items.map((item) => ({
                  key: item.to,
                  icon: <item.icon />,
                  label: navItemLabel(item.title, item.to === '/cms/triage-alerts' ? interventionCount : 0),
                  onClick: () => navigate(item.to),
                })),
              })),
            ],
          }}
        >
          <button type="button" className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm text-white/80 hover:bg-white/10">
            Chức năng
            {interventionCount > 0 && <Badge count={interventionCount} overflowCount={99} size="small" />}
            <DownOutlined className="text-[0.6rem]" />
          </button>
        </Dropdown>
      </div>

      <div className="flex items-center gap-3 sm:gap-4">
        <div className="hidden items-center gap-2 sm:flex">
          {mode === 'dark' ? <BulbFilled className="text-white/70" /> : <BulbOutlined className="text-white/70" />}
          <Switch size="small" checked={mode === 'dark'} onChange={toggleTheme} />
        </div>

        {user?.userType === 'ADMIN' && (
          <NotificationBell
            audience="admin"
            unreadCount={unreadCount}
            onUnreadChange={setUnreadCount}
            allUrl="/cms/notifications"
          />
        )}

        <Dropdown
          menu={{
            items: [
              { key: 'login-history', icon: <HistoryOutlined />, label: 'Lịch sử đăng nhập', onClick: () => navigate('/cms/login-history') },
              { key: 'change-password', icon: <KeyOutlined />, label: 'Đổi mật khẩu', onClick: () => navigate('/cms/change-password') },
              { type: 'divider' },
              { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true, onClick: logout },
            ],
          }}
          trigger={['click']}
        >
          <button className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-left hover:bg-white/10">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-teal-500/20 text-sm font-semibold text-teal-300">
              {(user?.fullName ?? user?.email ?? '?').charAt(0).toUpperCase()}
            </span>
            <span className="hidden text-left sm:block">
              <span className="block max-w-[10rem] truncate text-sm font-medium text-white">
                {user?.fullName ?? user?.email}
              </span>
              <span className="block text-xs text-white/50">{user ? ROLE_LABEL[user.userType] : ''}</span>
            </span>
            <DownOutlined className="text-xs text-white/40" />
          </button>
        </Dropdown>
      </div>
    </header>
  )
}
