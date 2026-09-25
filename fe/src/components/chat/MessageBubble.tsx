import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  FileTextOutlined,
  ReloadOutlined,
  RobotOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Button } from 'antd'
import type { Message } from './chat.types'

interface MessageBubbleProps {
  message: Message
  onRetry: (message: Message) => void
}

function formatTimestamp(timestamp: string): string {
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(timestamp))
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export function MessageBubble({ message, onRetry }: MessageBubbleProps) {
  const isPatient = message.sender === 'patient'
  const isFailed = message.status === 'failed'

  return (
    <article className={`flex items-start gap-2.5 ${isPatient ? 'justify-end' : 'justify-start'}`}>
      {!isPatient && (
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-teal-100 text-teal-700" aria-hidden="true">
          <RobotOutlined />
        </span>
      )}

      <div className={`flex max-w-[85%] flex-col sm:max-w-[72%] ${isPatient ? 'items-end' : 'items-start'}`}>
        <div
          className={`rounded-2xl px-4 py-3 text-[15px] leading-6 shadow-sm ${
            isPatient
              ? isFailed
                ? 'rounded-br-md border border-red-200 bg-red-50 text-red-900'
                : 'rounded-br-md bg-teal-700 text-white'
              : 'rounded-bl-md bg-slate-100 text-slate-800'
          }`}
        >
          <p className="m-0 whitespace-pre-wrap break-words">{message.content}</p>

          {message.attachments && message.attachments.length > 0 && (
            <div className={`mt-3 flex flex-wrap gap-2 border-t pt-3 ${isPatient && !isFailed ? 'border-white/20' : 'border-slate-200'}`}>
              {message.attachments.map((attachment) =>
                attachment.previewUrl ? (
                  <a key={attachment.id} href={attachment.url} target="_blank" rel="noreferrer" className="block">
                    <img src={attachment.previewUrl} alt={`Tệp đính kèm ${attachment.name}`} className="h-20 w-20 rounded-lg object-cover" />
                  </a>
                ) : (
                  <a key={attachment.id} href={attachment.url} target="_blank" rel="noreferrer" className="flex max-w-52 items-center gap-2 rounded-lg bg-white/90 px-2.5 py-2 text-xs text-slate-700 no-underline">
                    <FileTextOutlined className="text-base text-teal-700" />
                    <span className="min-w-0"><span className="block truncate">{attachment.name}</span><span className="text-slate-400">{formatFileSize(attachment.size)}</span></span>
                  </a>
                ),
              )}
            </div>
          )}
        </div>

        <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
          <time dateTime={message.createdAt}>{formatTimestamp(message.createdAt)}</time>
          {message.status === 'sending' && <span>· Đang gửi</span>}
          {message.status === 'sent' && isPatient && <><CheckCircleOutlined className="text-teal-600" /><span>Đã gửi</span></>}
          {isFailed && <><CloseCircleOutlined className="text-red-600" /><span className="text-red-600">Gửi lỗi</span></>}
        </div>

        {isFailed && (
          <Button type="link" danger size="small" icon={<ReloadOutlined />} className="h-auto px-0 py-1" onClick={() => onRetry(message)}>
            Gửi lại
          </Button>
        )}
      </div>

      {isPatient && (
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-teal-700 text-white" aria-hidden="true">
          <UserOutlined />
        </span>
      )}
    </article>
  )
}
