import { CommentOutlined, DeleteOutlined } from '@ant-design/icons'
import { Button, Popconfirm, Tooltip } from 'antd'
import type { ChatSessionSummary } from '../../api/chat'

interface HistorySessionItemProps {
  session: ChatSessionSummary
  isSelected: boolean
  onClick: () => void
  onDelete: () => void
  deleting?: boolean
}

function formatRelativeTime(isoString: string): string {
  const diff = Date.now() - new Date(isoString).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return 'Vừa xong'
  if (minutes < 60) return `${minutes} phút trước`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} giờ trước`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days} ngày trước`
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(
    new Date(isoString),
  )
}

export function HistorySessionItem({
  session,
  isSelected,
  onClick,
  onDelete,
  deleting = false,
}: HistorySessionItemProps) {
  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onClick}
      onKeyDown={(e) => e.key === 'Enter' && onClick()}
      className={`group relative border-b border-slate-100 px-4 py-3 cursor-pointer transition-colors last:border-b-0 ${
        isSelected ? 'bg-teal-50' : 'hover:bg-slate-50'
      }`}
    >
      <div className="flex items-start gap-2.5">
        <span
          className={`mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-sm ${
            isSelected ? 'bg-teal-100 text-teal-700' : 'bg-slate-100 text-slate-500'
          }`}
        >
          <CommentOutlined />
        </span>
        <div className="min-w-0 flex-1">
          <p
            className={`m-0 line-clamp-2 text-sm leading-5 font-medium ${
              isSelected ? 'text-teal-800' : 'text-slate-700'
            }`}
          >
            {session.title}
          </p>
          <p className="m-0 mt-0.5 text-xs text-slate-400">{formatRelativeTime(session.updatedAt)}</p>
        </div>
      </div>

      <Popconfirm
        title="Xóa phiên chat này?"
        description="Toàn bộ tin nhắn và đánh giá trong phiên sẽ bị xóa vĩnh viễn."
        okText="Xóa phiên"
        cancelText="Hủy"
        okButtonProps={{ danger: true, loading: deleting }}
        onConfirm={(event) => {
          event?.stopPropagation()
          onDelete()
        }}
        onCancel={(event) => event?.stopPropagation()}
      >
        <Tooltip title="Xóa phiên chat khỏi lịch sử">
          <Button
            type="text"
            danger
            size="small"
            icon={<DeleteOutlined />}
            loading={deleting}
            disabled={deleting}
            aria-label={`Xóa phiên chat: ${session.title}`}
            className="absolute right-2 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-50!"
            onClick={(event) => event.stopPropagation()}
            onKeyDown={(event) => event.stopPropagation()}
          />
        </Tooltip>
      </Popconfirm>
    </div>
  )
}
