import {
  BulbFilled,
  BulbOutlined,
  CheckCircleOutlined,
  DownOutlined,
  HomeOutlined,
  KeyOutlined,
  LogoutOutlined,
  MedicineBoxOutlined,
} from '@ant-design/icons'
import { Button, Dropdown, message, notification, Switch } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../stores/authStore'
import { useTheme } from '../../stores/themeStore'
import { Logo } from '../brand/Logo'
import { NotificationBell } from './NotificationBell'
import { PATIENT_NAV_GROUPS } from './patientNav'
import { getUnreadNotificationCount, markNotificationRead } from '../../api/notifications'
import { checkDueMedications, confirmMedicationDose } from '../../api/medication'
import { answerTriageCheckIn, TRIAGE_ANSWERED_EVENT } from '../../api/patientTriageAlerts'
import { useNotificationSocket } from '../../hooks/useNotificationSocket'

// So khớp "đường dẫn hiện tại thuộc mục nav nào" — phải so bằng hoặc có dấu "/" ngay sau, vì
// startsWith đơn thuần sẽ nhận nhầm "/food-diary-history" thuộc về mục "/food-diary".
function isActivePath(pathname: string, to: string): boolean {
  return pathname === to || pathname.startsWith(`${to}/`)
}

/** Header cố định cho mọi trang bệnh nhân sau đăng nhập — thay cho AppSidebar cũ (xem
 * 00-General/02-NguoiDung-BenhNhan/SCR-PT-00-TrangChu.md). Điều hướng giữa các chức năng giờ
 * đi qua Trang chủ (HomePage), không còn menu liệt kê sẵn ở đây — header chỉ giữ thương hiệu
 * (bấm về Trang chủ), thông báo và tài khoản. */
export function AppHeader() {
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
    <header className="flex h-16 shrink-0 items-center gap-4 border-b border-white/10 bg-[#0f2926] px-4 text-white sm:px-6">
      <span className="flex shrink-0 items-center">
        <Logo variant="light" />
      </span>

      <nav className="hidden min-w-0 flex-1 items-center gap-1 md:flex">
        <Link
          to="/"
          className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm transition-colors ${
            location.pathname === '/' ? 'bg-white/10 text-white' : 'text-white/75 hover:bg-white/5 hover:text-white'
          }`}
        >
          <HomeOutlined />
          Trang chủ
        </Link>

        {PATIENT_NAV_GROUPS.map((group) => {
          const active = group.items.some((item) => isActivePath(location.pathname, item.to))
          return (
            <Dropdown
              key={group.key}
              trigger={['hover', 'click']}
              menu={{
                items: group.items.map((item) => ({
                  key: item.to,
                  icon: <item.icon />,
                  label: item.title,
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
                <DownOutlined className="text-[0.6rem]" />
              </button>
            </Dropdown>
          )
        })}
      </nav>

      {/* Menu rút gọn cho màn hình nhỏ — cùng các mục, gộp vào 1 dropdown duy nhất thay vì ẩn hẳn. */}
      <div className="flex-1 md:hidden">
        <Dropdown
          trigger={['click']}
          menu={{
            items: [
              { key: '/', icon: <HomeOutlined />, label: <Link to="/">Trang chủ</Link> },
              ...PATIENT_NAV_GROUPS.map((group) => ({
                key: group.key,
                label: group.title,
                children: group.items.map((item) => ({
                  key: item.to,
                  icon: <item.icon />,
                  label: item.title,
                  onClick: () => navigate(item.to),
                })),
              })),
            ],
          }}
        >
          <button type="button" className="flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm text-white/80 hover:bg-white/10">
            Chức năng
            <DownOutlined className="text-[0.6rem]" />
          </button>
        </Dropdown>
      </div>

      <div className="flex items-center gap-3 sm:gap-4">
        <div className="hidden items-center gap-2 sm:flex">
          {mode === 'dark' ? <BulbFilled className="text-white/70" /> : <BulbOutlined className="text-white/70" />}
          <Switch size="small" checked={mode === 'dark'} onChange={toggleTheme} />
        </div>

        <NotificationBell
          audience="patient"
          unreadCount={unreadCount}
          onUnreadChange={setUnreadCount}
          allUrl="/notifications"
        />

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
          <button className="flex items-center gap-2 rounded-lg px-2 py-1.5 text-left hover:bg-white/10">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-teal-500/20 text-sm font-semibold text-teal-300">
              {(fullName ?? email ?? '?').charAt(0).toUpperCase()}
            </span>
            <span className="hidden max-w-[10rem] truncate text-sm font-medium text-white sm:block">
              {fullName ?? email}
            </span>
            <DownOutlined className="text-xs text-white/40" />
          </button>
        </Dropdown>
      </div>
    </header>
  )
}
