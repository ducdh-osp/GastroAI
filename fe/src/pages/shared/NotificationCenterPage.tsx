import {
  BellOutlined,
  CheckOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons'
import { Button, Card, Empty, message, Modal, Pagination, Skeleton, Tag, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  deleteNotification,
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type AppNotification,
  type NotificationAudience,
  type NotificationPage,
} from '../../api/notifications'
import { extractErrorMessage } from '../../lib/errors'

const { Title, Text, Paragraph } = Typography
const PAGE_SIZE = 20

const TYPE_LABEL: Record<AppNotification['type'], string> = {
  JOURNAL_REMINDER: 'Nhắc nhật ký',
  TRIAGE_ALERT: 'Cảnh báo triage',
  SYSTEM: 'Hệ thống',
}

export function NotificationCenterPage({ audience }: { audience: NotificationAudience }) {
  const navigate = useNavigate()
  const [data, setData] = useState<NotificationPage | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [workingId, setWorkingId] = useState<number | null>(null)

  const load = useCallback(async (targetPage = page) => {
    setLoading(true)
    try {
      setData(await getNotifications(audience, targetPage, PAGE_SIZE))
    } catch (error) {
      message.error(extractErrorMessage(error, 'Không thể tải danh sách thông báo').message)
    } finally {
      setLoading(false)
    }
  }, [audience, page])

  useEffect(() => {
    const timeout = window.setTimeout(() => void load(page), 0)
    return () => window.clearTimeout(timeout)
  }, [load, page])
  useEffect(() => {
    const refresh = () => void load(page)
    window.addEventListener('gastroai:notification', refresh)
    return () => window.removeEventListener('gastroai:notification', refresh)
  }, [load, page])

  async function markRead(item: AppNotification, followAction = false) {
    if (!item.readAt) {
      setWorkingId(item.id)
      try {
        await markNotificationRead(audience, item.id)
        setData((current) => current ? {
          ...current,
          unreadCount: Math.max(0, current.unreadCount - 1),
          items: current.items.map((entry) => entry.id === item.id
            ? { ...entry, readAt: new Date().toISOString() }
            : entry),
        } : current)
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

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <Title level={2} className="mb-1!">Trung tâm thông báo</Title>
          <Text type="secondary">
            {data?.unreadCount ? `${data.unreadCount} thông báo chưa đọc` : 'Bạn đã xem tất cả thông báo'}
          </Text>
        </div>
        <div className="flex gap-2">
          <Button icon={<ReloadOutlined />} onClick={() => void load(page)} loading={loading}>Làm mới</Button>
          <Button icon={<CheckOutlined />} disabled={!data?.unreadCount} onClick={() => void markAllRead()}>
            Đọc tất cả
          </Button>
        </div>
      </div>

      {loading && !data ? <Card><Skeleton active /></Card> : data?.items.length ? (
        <div className="space-y-3">
          {data.items.map((item) => (
            <Card
              key={item.id}
              className={`rounded-2xl shadow-sm ${item.readAt ? 'border-black/5' : 'border-teal-200 bg-teal-50/40 dark:border-teal-800 dark:bg-teal-900/30'}`}
              onClick={() => void markRead(item, true)}
              styles={{ body: { cursor: item.actionUrl ? 'pointer' : 'default' } }}
            >
              <div className="flex items-start gap-3">
                <span className={`mt-1 flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${
                  item.type === 'TRIAGE_ALERT' ? 'bg-red-100 text-red-600' : 'bg-teal-100 text-teal-700'
                }`}>
                  {item.type === 'TRIAGE_ALERT' ? <ExclamationCircleOutlined /> : <BellOutlined />}
                </span>
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <Text strong>{item.title}</Text>
                    <Tag color={item.type === 'TRIAGE_ALERT' ? 'red' : 'cyan'}>{TYPE_LABEL[item.type]}</Tag>
                    {!item.readAt && <span className="h-2 w-2 rounded-full bg-teal-500" aria-label="Chưa đọc" />}
                  </div>
                  <Paragraph className="mt-2 mb-1!">{item.message}</Paragraph>
                  <Text type="secondary" className="text-xs">
                    {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(item.createdAt))}
                  </Text>
                </div>
                <Button
                  type="text"
                  danger
                  aria-label="Xóa thông báo"
                  icon={<DeleteOutlined />}
                  loading={workingId === item.id}
                  onClick={(event) => { event.stopPropagation(); confirmDelete(item) }}
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
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Chưa có thông báo nào" />
        </Card>
      )}
    </div>
  )
}
