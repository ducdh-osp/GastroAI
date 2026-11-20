import {
  BellOutlined,
  BulbFilled,
  BulbOutlined,
  CoffeeOutlined,
  CommentOutlined,
  DownOutlined,
  ExperimentOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  HomeOutlined,
  CheckCircleOutlined,
  KeyOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
  QuestionCircleOutlined,
} from '@ant-design/icons'
import { Badge, Button, Dropdown, message, notification, Switch } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../stores/authStore'
import { useTheme } from '../../stores/themeStore'
import { Logo } from '../brand/Logo'
import { getUnreadNotificationCount, markNotificationRead } from '../../api/notifications'
import { checkDueMedications, confirmMedicationDose } from '../../api/medication'
import { answerTriageCheckIn, TRIAGE_ANSWERED_EVENT } from '../../api/patientTriageAlerts'
import { useNotificationSocket } from '../../hooks/useNotificationSocket'

const NAV_ITEMS = [
  { key: 'notifications', label: 'Thông báo', icon: <BellOutlined />, to: '/notifications' },
  { key: 'home', label: 'Trang chủ', icon: <HomeOutlined />, to: '/' },
  { key: 'chat', label: 'Tư vấn sức khỏe', icon: <CommentOutlined />, to: '/chat' },
  { key: 'articles', label: 'Bài viết tiêu hoá', icon: <FileSearchOutlined />, to: '/articles' },
  { key: 'faq', label: 'Hỏi đáp thường gặp', icon: <QuestionCircleOutlined />, to: '/faq' },
  { key: 'medical-profile', label: 'Hồ sơ bệnh lý', icon: <FileTextOutlined />, to: '/medical-profile' },
  { key: 'food-diary', label: 'Nhật ký ăn uống', icon: <CoffeeOutlined />, to: '/food-diary' },
  { key: 'bristol', label: 'Nhật ký Bristol', icon: <ExperimentOutlined />, to: '/bristol' },
  { key: 'medications', label: 'Nhắc uống thuốc', icon: <MedicineBoxOutlined />, to: '/medications' },
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

  // Bệnh nhân trả lời "Bạn có ổn không?" ở trang Thông báo -> đóng popup cùng câu hỏi nếu còn mở.
  useEffect(() => {
    const closeAnsweredPopup = (event: Event) => {
      const notificationId = (event as CustomEvent<number>).detail
      notification.destroy(`triage-check-in-${notificationId}`)
    }
    window.addEventListener(TRIAGE_ANSWERED_EVENT, closeAnsweredPopup)
    return () => window.removeEventListener(TRIAGE_ANSWERED_EVENT, closeAnsweredPopup)
  }, [])

  useEffect(() => {
    const refreshUnreadCount = () => {
      void getUnreadNotificationCount('patient').then(setUnreadCount).catch(() => {})
    }
    refreshUnreadCount()

    // UC0068: Khi bệnh nhân đăng nhập/mở ứng dụng, kiểm tra ngay các liều thuốc đến hạn hôm nay
    if (token) {
      checkDueMedications()
        .then((res) => {
          if (res.created > 0) {
            refreshUnreadCount()
          }
        })
        .catch(() => {})
    }

    window.addEventListener('gastroai:notification-state', refreshUnreadCount)
    return () => window.removeEventListener('gastroai:notification-state', refreshUnreadCount)
  }, [token])

  useNotificationSocket({
    audience: 'patient',
    enabled: Boolean(token),
    token,
    onNotification: useCallback((item) => {
      setUnreadCount((count) => count + 1)
      window.dispatchEvent(new Event('gastroai:notification'))

      // UC0068: Nhắc uống thuốc trong app qua WebSocket với thao tác nhanh "Đã uống"
      if (item.type === 'MEDICATION_REMINDER') {
        const notifKey = `med-reminder-${item.id}`
        const scheduledTime = item.scheduledTime || (item.message.match(/lúc\s+(\d{2}:\d{2})/i)?.[1] ?? null)

        notification.open({
          key: notifKey,
          message: <span className="font-semibold text-emerald-800">{item.title}</span>,
          description: (
            <div className="space-y-3 pt-1">
              <p className="text-sm text-slate-700 leading-relaxed">{item.message}</p>
              <div className="flex items-center gap-2 pt-1">
                {item.sourceId && scheduledTime && (
                  <Button
                    type="primary"
                    size="small"
                    icon={<CheckCircleOutlined />}
                    className="bg-emerald-600 hover:bg-emerald-500 text-white font-medium shadow-xs"
                    onClick={async () => {
                      try {
                        await confirmMedicationDose(item.sourceId!, scheduledTime)
                        await markNotificationRead('patient', item.id)
                        notification.destroy(notifKey)
                        message.success('Đã xác nhận uống thuốc thành công!')
                        window.dispatchEvent(new Event('gastroai:notification'))
                        window.dispatchEvent(new Event('gastroai:notification-state'))
                        window.dispatchEvent(new Event('gastroai:medication-confirmed'))
                      } catch (err) {
                        message.error(err instanceof Error ? err.message : 'Không thể xác nhận uống thuốc.')
                      }
                    }}
                  >
                    Đã uống thuốc
                  </Button>
                )}
                <Button
                  size="small"
                  onClick={() => {
                    notification.destroy(notifKey)
                    navigate('/medications')
                  }}
                >
                  Xem đơn thuốc
                </Button>
              </div>
            </div>
          ),
          icon: <MedicineBoxOutlined className="text-emerald-600 text-2xl" />,
          placement: 'topRight',
          duration: 0, // Giữ thông báo nhắc thuốc cho tới khi bệnh nhân xác nhận hoặc đóng
        })
      } else if (item.type === 'TRIAGE_CHECK_IN' && item.sourceId !== null) {
        // SCR-AD-13 bước 1: hỏi "Bạn có ổn không?" ngay trên màn đang dùng (thường là trang chat),
        // trả lời tại chỗ - KHÔNG chuyển trang, để bệnh nhân không mất mạch đang hỏi AI.
        const notifKey = `triage-check-in-${item.id}`
        const alertId = item.sourceId

        // Cùng key -> antd cập nhật đúng popup đang mở (đang gửi / báo lỗi) thay vì mở popup mới.
        const showCheckIn = (sending: boolean, error: string | null) => {
          notification.warning({
            key: notifKey,
            message: <span className="font-semibold">{item.title}</span>,
            description: (
              <div className="space-y-3 pt-1">
                <p className="text-sm leading-relaxed">{item.message}</p>
                <div className="flex flex-wrap gap-2">
                  <Button size="small" disabled={sending} onClick={() => answer('SAFE')}>
                    Tôi an toàn
                  </Button>
                  <Button size="small" type="primary" danger loading={sending} onClick={() => answer('NEED_HELP')}>
                    Cần hỗ trợ
                  </Button>
                </div>
                {error && <p className="mb-0 text-xs text-red-600">{error} Vui lòng bấm lại.</p>}
                <p className="mb-0 text-xs text-slate-500">Nếu đang nguy hiểm, hãy gọi 115 ngay.</p>
              </div>
            ),
            placement: 'topRight',
            duration: 0, // Giữ tới khi bệnh nhân trả lời - đây là kênh duy nhất hệ thống hỏi được.
          })
        }

        const answer = (response: 'SAFE' | 'NEED_HELP') => {
          showCheckIn(true, null)
          answerTriageCheckIn(alertId, response)
            .then((result) => {
              void markNotificationRead('patient', item.id)
                .then(() => window.dispatchEvent(new Event('gastroai:notification-state')))
                .catch(() => undefined)
              if (result.alreadyClosed) {
                notification.destroy(notifKey)
                message.info('Cảnh báo này đã được xử lý xong, bạn không cần trả lời thêm.')
                return
              }
              if (response === 'SAFE') {
                notification.destroy(notifKey)
                message.success('Cảm ơn bạn đã xác nhận. GastroAI sẽ không làm phiền thêm.')
                return
              }
              // Thay popup câu hỏi bằng xác nhận đỏ, giữ tới khi bệnh nhân tự đóng.
              notification.error({
                key: notifKey,
                message: <span className="font-semibold">Đã báo nhân sự GastroAI</span>,
                description: (
                  <p className="mb-0 pt-1 text-sm leading-relaxed">
                    Chúng tôi sẽ gọi điện cho bạn hoặc người thân bạn đã khai trong hồ sơ. Nếu tình trạng nặng hơn,
                    hãy gọi 115 ngay, đừng chờ.
                  </p>
                ),
                placement: 'topRight',
                duration: 0,
              })
            })
            .catch((err: unknown) => {
              // Lỗi thì giữ nguyên câu hỏi để bấm lại - không chuyển trang, không đoán là đã gửi.
              showCheckIn(false, err instanceof Error ? err.message : 'Không gửi được câu trả lời.')
            })
        }

        showCheckIn(false, null)
      } else {
        notification.info({ message: item.title, description: item.message, placement: 'topRight' })
      }
    }, [navigate]),
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
            items: [
              { key: 'change-password', icon: <KeyOutlined />, label: 'Đổi mật khẩu', onClick: () => navigate('/change-password') },
              { type: 'divider' },
              { key: 'logout', icon: <LogoutOutlined />, label: 'Đăng xuất', danger: true, onClick: handleLogout },
            ],
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
