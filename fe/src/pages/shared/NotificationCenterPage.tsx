import {
  BellOutlined,
  CheckOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  MedicineBoxOutlined,
  ReloadOutlined,
  RightOutlined,
  UndoOutlined,
} from '@ant-design/icons'
import { Button, Card, Empty, message, Modal, Pagination, Segmented, Select, Skeleton, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  deleteNotification,
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  markNotificationUnread,
  type AppNotification,
  type NotificationAudience,
  type NotificationPage,
  type NotificationType,
} from '../../api/notifications'
import { extractErrorMessage } from '../../lib/errors'

const { Title, Text, Paragraph } = Typography
const PAGE_SIZE = 20

const TYPE_META: Record<NotificationType, { label: string; color: string }> = {
  JOURNAL_REMINDER: { label: 'Nhắc nhật ký', color: 'cyan' },
  MEDICATION_REMINDER: { label: 'Nhắc uống thuốc', color: 'purple' },
  TRIAGE_ALERT: { label: 'Cảnh báo triage', color: 'red' },
  SYSTEM: { label: 'Hệ thống', color: 'blue' },
}

function notificationIcon(type: NotificationType) {
  if (type === 'TRIAGE_ALERT') return <ExclamationCircleOutlined />
  if (type === 'MEDICATION_REMINDER') return <MedicineBoxOutlined />
  return <BellOutlined />
}

function iconClass(type: NotificationType) {
  if (type === 'TRIAGE_ALERT') return 'bg-red-100 text-red-600'
  if (type === 'MEDICATION_REMINDER') return 'bg-purple-100 text-purple-700'
  return 'bg-teal-100 text-teal-700'
}

export function NotificationCenterPage({ audience }: { audience: NotificationAudience }) {
  const navigate = useNavigate()
  const [data, setData] = useState<NotificationPage | null>(null)
  const [page, setPage] = useState(0)
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [type, setType] = useState<NotificationType | undefined>()
  const [loading, setLoading] = useState(true)
  const [workingId, setWorkingId] = useState<number | null>(null)

  const load = useCallback(async (targetPage = page) => {
    setLoading(true)
    try {
      setData(await getNotifications(audience, targetPage, PAGE_SIZE, { unreadOnly, type }))
    } catch (error) {
      message.error(extractErrorMessage(error, 'Không thể tải danh sách thông báo').message)
    } finally {
      setLoading(false)
    }
  }, [audience, page, type, unreadOnly])

  useEffect(() => {
    const timeout = window.setTimeout(() => void load(page), 0)
    return () => window.clearTimeout(timeout)
  }, [load, page])

  useEffect(() => {
    const refresh = () => void load(page)
    window.addEventListener('gastroai:notification', refresh)
    return () => window.removeEventListener('gastroai:notification', refresh)
  }, [load, page])

  function updateFilter(nextUnreadOnly: boolean, nextType = type) {
    setPage(0)
    setUnreadOnly(nextUnreadOnly)
    setType(nextType)
  }

  async function markRead(item: AppNotification, followAction = false) {
    if (!item.readAt) {
      setWorkingId(item.id)
      try {
        await markNotificationRead(audience, item.id)
        if (unreadOnly) {
          const remaining = (data?.items.length ?? 1) - 1
          const targetPage = remaining === 0 && page > 0 ? page - 1 : page
          if (targetPage !== page) setPage(targetPage)
          else await load(targetPage)
        } else {
          setData((current) => current ? {
            ...current,
            unreadCount: Math.max(0, current.unreadCount - 1),
            items: current.items.map((entry) => entry.id === item.id
              ? { ...entry, readAt: new Date().toISOString() }
              : entry),
          } : current)
        }
        window.dispatchEvent(new Event('gastroai:notification-state'))
      } catch (error) {
        message.error(extractErrorMessage(error, 'Không thể đánh dấu đã đọc').message)
        return
      } finally {
        setWorkingId(null)
      }
    }
    if (followAction && item.actionUrl) navigate(item.actionUrl)
  }

  async function markUnread(item: AppNotification) {
    if (!item.readAt) return
    setWorkingId(item.id)
    try {
      await markNotificationUnread(audience, item.id)
      setData((current) => current ? {
        ...current,
        unreadCount: current.unreadCount + 1,
        items: current.items.map((entry) => entry.id === item.id
          ? { ...entry, readAt: null }
          : entry),
      } : current)
      window.dispatchEvent(new Event('gastroai:notification-state'))
    } catch (error) {
      message.error(extractErrorMessage(error, 'Không thể đánh dấu chưa đọc').message)
    } finally {
      setWorkingId(null)
    }
  }

  async function markAllRead() {
    try {
      await markAllNotificationsRead(audience)
      await load(page)
      window.dispatchEvent(new Event('gastroai:notification-state'))
      message.success('Đã đánh dấu tất cả thông báo là đã đọc')
    } catch (error) {
      message.error(extractErrorMessage(error, 'Không thể cập nhật thông báo').message)
    }
  }

  function confirmDelete(item: AppNotification) {
    Modal.confirm({
      title: 'Xóa thông báo?',
      content: 'Thông báo sẽ được ẩn khỏi trung tâm thông báo của bạn.',
      okText: 'Xóa',
      okButtonProps: { danger: true },
      cancelText: 'Hủy',
      async onOk() {
        setWorkingId(item.id)
        try {
          await deleteNotification(audience, item.id)
          const remaining = (data?.items.length ?? 1) - 1
          const targetPage = remaining === 0 && page > 0 ? page - 1 : page
          if (targetPage !== page) setPage(targetPage)
          else await load(targetPage)
          window.dispatchEvent(new Event('gastroai:notification-state'))
          message.success('Đã xóa thông báo')
        } catch (error) {
          message.error(extractErrorMessage(error, 'Không thể xóa thông báo').message)
        } finally {
          setWorkingId(null)
        }
      },
    })
  }

  const typeOptions = audience === 'patient'
    ? [
        { value: 'JOURNAL_REMINDER', label: 'Nhắc nhật ký' },
        { value: 'MEDICATION_REMINDER', label: 'Nhắc uống thuốc' },
        { value: 'SYSTEM', label: 'Hệ thống' },
      ]
    : [
        { value: 'TRIAGE_ALERT', label: 'Cảnh báo triage' },
        { value: 'SYSTEM', label: 'Hệ thống' },
      ]

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <Title level={2} className="mb-1!">Trung tâm thông báo</Title>
            {!!data?.unreadCount && <Tag color="red">{data.unreadCount} chưa đọc</Tag>}
          </div>
          <Text type="secondary">Theo dõi lời nhắc và các cập nhật quan trọng từ GastroAI.</Text>
        </div>
        <div className="flex gap-2">
          <Button icon={<ReloadOutlined />} onClick={() => void load(page)} loading={loading}>Làm mới</Button>
          <Button icon={<CheckOutlined />} disabled={!data?.unreadCount} onClick={() => void markAllRead()}>
            Đọc tất cả
          </Button>
        </div>
      </div>

      <Card className="mb-4 rounded-2xl border-black/5 shadow-sm" size="small">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Segmented
            value={unreadOnly ? 'unread' : 'all'}
            options={[
              { label: 'Tất cả', value: 'all' },
              { label: `Chưa đọc${data?.unreadCount ? ` (${data.unreadCount})` : ''}`, value: 'unread' },
            ]}
            onChange={(value) => updateFilter(value === 'unread')}
          />
          <Select
            className="min-w-48"
            allowClear
            placeholder="Tất cả loại thông báo"
            value={type}
            options={typeOptions}
            onChange={(value: NotificationType | undefined) => updateFilter(unreadOnly, value)}
          />
        </div>
      </Card>

      {loading && !data ? <Card><Skeleton active /></Card> : data?.items.length ? (
        <div className="space-y-3">
          {data.items.map((item) => (
            <Card
              key={item.id}
              className={`rounded-2xl shadow-sm ${item.readAt ? 'border-black/5' : 'border-teal-200 bg-teal-50/40 dark:border-teal-800 dark:bg-teal-900/30'}`}
            >
              <div className="flex items-start gap-3">
                <span className={`mt-1 flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${iconClass(item.type)}`}>
                  {notificationIcon(item.type)}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <Text strong>{item.title}</Text>
                    <Tag color={TYPE_META[item.type].color}>{TYPE_META[item.type].label}</Tag>
                    {!item.readAt && <span className="h-2 w-2 rounded-full bg-teal-500" aria-label="Chưa đọc" />}
                  </div>
                  <Paragraph className="mt-2 mb-1!">{item.message}</Paragraph>
                  <Text type="secondary" className="text-xs">
                    {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(item.createdAt))}
                  </Text>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {item.actionUrl && (
                      <Button type="primary" ghost size="small" icon={<RightOutlined />} onClick={() => void markRead(item, true)}>
                        Xem chi tiết
                      </Button>
                    )}
                    {!item.readAt ? (
                      <Button size="small" icon={<CheckOutlined />} loading={workingId === item.id} onClick={() => void markRead(item)}>
                        Đánh dấu đã đọc
                      </Button>
                    ) : (
                      <Button size="small" icon={<UndoOutlined />} loading={workingId === item.id} onClick={() => void markUnread(item)}>
                        Đánh dấu chưa đọc
                      </Button>
                    )}
                  </div>
                </div>
                <Button
                  type="text"
                  danger
                  aria-label="Xóa thông báo"
                  icon={<DeleteOutlined />}
                  loading={workingId === item.id}
                  onClick={() => confirmDelete(item)}
                />
              </div>
            </Card>
          ))}
          {data.totalPages > 1 && (
            <div className="flex justify-center pt-3">
              <Pagination
                current={page + 1}
                pageSize={PAGE_SIZE}
                total={data.totalElements}
                showSizeChanger={false}
                onChange={(next) => setPage(next - 1)}
              />
            </div>
          )}
        </div>
      ) : (
        <Card className="rounded-2xl border-black/5 shadow-sm">
          <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={unreadOnly || type ? 'Không có thông báo phù hợp với bộ lọc' : 'Chưa có thông báo nào'}
          />
        </Card>
      )}
    </div>
  )
}
