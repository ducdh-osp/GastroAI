import { BellOutlined } from '@ant-design/icons'
import { Badge, Empty, Popover, Skeleton, Tag } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getNotifications,
  markNotificationRead,
  type AppNotification,
  type NotificationAudience,
} from '../../api/notifications'

const TYPE_LABEL: Record<string, string> = {
  JOURNAL_REMINDER: 'Nhắc nhật ký',
  MEDICATION_REMINDER: 'Nhắc uống thuốc',
  SECURITY_ALERT: 'An toàn & bảo mật',
  TRIAGE_ALERT: 'Cảnh báo triage',
  SYSTEM: 'Hệ thống',
  TRIAGE_DOCTOR_MESSAGE: 'Tin nhắn',
}

function timeAgo(iso: string): string {
  const minutes = Math.floor((Date.now() - new Date(iso).getTime()) / 60000)
  if (minutes < 1) return 'Vừa xong'
  if (minutes < 60) return `${minutes} phút trước`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} giờ trước`
  return `${Math.floor(hours / 24)} ngày trước`
}

/** Chuông thông báo dùng chung cho AppHeader/CmsHeader — di chuột vào xem trước vài thông báo
 * gần nhất, bấm vào 1 thông báo mới thật sự điều hướng sang trang chi tiết/đánh dấu đã đọc.
 * Bấm thẳng vào icon chuông (không qua panel) vẫn đi tới trang danh sách đầy đủ. */
export function NotificationBell({
  audience,
  unreadCount,
  onUnreadChange,
  allUrl,
}: {
  audience: NotificationAudience
  unreadCount: number
  onUnreadChange: (count: number) => void
  allUrl: string
}) {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const [items, setItems] = useState<AppNotification[] | null>(null)

  function handleOpenChange(next: boolean) {
    setOpen(next)
    if (next) {
      setLoading(true)
      getNotifications(audience, 0, 6)
        .then((res) => setItems(res.items))
        .catch(() => setItems([]))
        .finally(() => setLoading(false))
    }
  }

  async function handleItemClick(item: AppNotification) {
    setOpen(false)
    if (!item.readAt) {
      try {
        await markNotificationRead(audience, item.id)
        onUnreadChange(Math.max(0, unreadCount - 1))
        window.dispatchEvent(new Event('gastroai:notification-state'))
      } catch {
        // best-effort — vẫn điều hướng dù đánh dấu đã đọc thất bại
      }
    }
    navigate(item.actionUrl || allUrl)
  }

  const content = (
    <div className="w-80">
      <div className="flex items-center justify-between border-b border-black/5 px-3 py-2 dark:border-white/10">
        <span className="text-sm font-semibold">Thông báo</span>
        {unreadCount > 0 && <Tag color="red">{unreadCount} chưa đọc</Tag>}
      </div>
      <div className="max-h-96 overflow-y-auto">
        {loading ? (
          <div className="space-y-3 p-3">
            <Skeleton active paragraph={{ rows: 1 }} />
            <Skeleton active paragraph={{ rows: 1 }} />
          </div>
        ) : items && items.length > 0 ? (
          items.map((item) => (
            <button
              key={item.id}
              type="button"
              onClick={() => void handleItemClick(item)}
              className={`block w-full border-b border-black/5 px-3 py-2.5 text-left text-sm hover:bg-gray-50 dark:border-white/10 dark:hover:bg-white/5 ${
                item.readAt ? '' : 'bg-teal-50/60 dark:bg-teal-500/5'
              }`}
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-medium">{item.title}</span>
                {!item.readAt && <span className="h-2 w-2 shrink-0 rounded-full bg-teal-500" />}
              </div>
              <div className="mt-0.5 line-clamp-2 text-xs text-gray-500 dark:text-white/60">{item.message}</div>
              <div className="mt-1 text-[0.7rem] text-gray-400">
                {timeAgo(item.createdAt)} · {TYPE_LABEL[item.type] ?? item.type}
              </div>
            </button>
          ))
        ) : (
          <Empty description="Không có thông báo" image={Empty.PRESENTED_IMAGE_SIMPLE} className="py-6" />
        )}
      </div>
      <button
        type="button"
        onClick={() => {
          setOpen(false)
          navigate(allUrl)
        }}
        className="block w-full border-t border-black/5 px-3 py-2 text-center text-sm text-teal-600 hover:bg-gray-50 dark:border-white/10 dark:hover:bg-white/5"
      >
        Xem tất cả thông báo
      </button>
    </div>
  )

  return (
    <Popover
      trigger="hover"
      open={open}
      onOpenChange={handleOpenChange}
      placement="bottomRight"
      styles={{ content: { padding: 0 } }}
      content={content}
    >
      <button
        type="button"
        aria-label="Thông báo"
        onClick={() => navigate(allUrl)}
        className="flex h-9 w-9 items-center justify-center rounded-full text-white/80 hover:bg-white/10"
      >
        <Badge count={unreadCount} overflowCount={99} size="small" style={{ color: 'inherit' }} className="text-current!">
          <BellOutlined className="text-lg text-current!" />
        </Badge>
      </button>
    </Popover>
  )
}
