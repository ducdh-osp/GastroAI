import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  DislikeOutlined,
  FileTextOutlined,
  LikeOutlined,
  PhoneOutlined,
  ReloadOutlined,
  RobotOutlined,
  UserOutlined,
  WarningFilled,
} from '@ant-design/icons'
import { Button, Tooltip } from 'antd'
import type { Message, RatingValue } from '../../api/chat'
import { describeMatchedGroups } from '../../api/chat'
import { MarkdownContent } from './MarkdownContent'
import { SourceReferences } from './SourceReferences'

interface MessageBubbleProps {
  message: Message
  onRetry: (message: Message) => void
  onSuggestionClick: (question: string) => void
  suggestionsDisabled: boolean
  onRate: (message: Message, rating: RatingValue) => void
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

export function MessageBubble({ message, onRetry, onSuggestionClick, suggestionsDisabled, onRate }: MessageBubbleProps) {
  const isPatient = message.sender === 'patient'
  const isFailed = message.status === 'failed'
  const matchedGroupsText = describeMatchedGroups(message.matchedGroups)

  return (
    <article className={`flex items-start gap-2.5 ${isPatient ? 'justify-end' : 'justify-start'}`}>
      {!isPatient && (
        <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-teal-100 text-teal-700" aria-hidden="true">
          <RobotOutlined />
        </span>
      )}

      <div className={`flex max-w-[85%] flex-col sm:max-w-[72%] ${isPatient ? 'items-end' : 'items-start'}`}>
        {!isPatient && message.emergency && (
          <div
            className="mb-2 flex items-start gap-2.5 rounded-xl border-2 border-red-400 bg-red-50 px-3.5 py-3 text-sm leading-5 text-red-900 shadow-sm"
            role="alert"
          >
            <WarningFilled className="mt-0.5 shrink-0 text-lg text-red-600" />
            <span>
              <strong className="block">Phát hiện dấu hiệu cần cấp cứu!</strong>
              Vui lòng gọi <PhoneOutlined /> <strong>115</strong> hoặc đến ngay cơ sở y tế gần nhất.
              {matchedGroupsText && (
                <span className="mt-1 block text-red-800">Dấu hiệu phát hiện: {matchedGroupsText}</span>
              )}
            </span>
          </div>
        )}

        {!isPatient && !message.emergency && matchedGroupsText && (
          <div
            className="mb-2 flex items-start gap-2.5 rounded-xl border-2 border-amber-300 bg-amber-50 px-3.5 py-3 text-sm leading-5 text-amber-900 shadow-sm"
            role="status"
          >
            <WarningFilled className="mt-0.5 shrink-0 text-lg text-amber-500" />
            <span>
              <strong className="block">Có dấu hiệu nên lưu ý</strong>
              Chưa phải tình huống cấp cứu, nhưng bạn nên đi khám bác sĩ để kiểm tra sớm.
              <span className="mt-1 block text-amber-800">Dấu hiệu phát hiện: {matchedGroupsText}</span>
            </span>
          </div>
        )}

        <div
          className={`rounded-2xl px-4 py-3 text-[15px] leading-6 shadow-sm ${
            isPatient
              ? isFailed
                ? 'rounded-br-md border border-red-200 bg-red-50 text-red-900'
                : 'rounded-br-md bg-teal-700 text-white'
              : 'rounded-bl-md bg-slate-100 text-slate-800'
          }`}
        >
          <MarkdownContent content={message.content} />

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

          {message.sources && message.sources.length > 0 && <SourceReferences sources={message.sources} />}
        </div>

        {!isPatient && message.relatedQuestions && message.relatedQuestions.length > 0 && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {message.relatedQuestions.map((question) => (
              <button
                key={question}
                type="button"
                disabled={suggestionsDisabled}
                onClick={() => onSuggestionClick(question)}
                className="rounded-full border border-teal-200 bg-white px-2.5 py-1 text-xs text-teal-800 transition hover:border-teal-400 hover:bg-teal-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {question}
              </button>
            ))}
          </div>
        )}

        <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-500">
          <time dateTime={message.createdAt}>{formatTimestamp(message.createdAt)}</time>
          {message.status === 'sending' && <span>· Đang gửi</span>}
          {message.status === 'sent' && isPatient && <><CheckCircleOutlined className="text-teal-600" /><span>Đã gửi</span></>}
          {isFailed && <><CloseCircleOutlined className="text-red-600" /><span className="text-red-600">Gửi lỗi</span></>}

          {/* Rating buttons — chỉ hiện dưới câu trả lời AI đã lưu DB */}
          {!isPatient && message.dbMessageId && (
            <span className="ml-auto flex items-center gap-0.5">
              <Tooltip title={message.rating === 'HELPFUL' ? 'Bạn đã đánh giá hữu ích' : 'Hữu ích'}>
                <button
                  type="button"
                  aria-label="Đánh giá hữu ích"
                  onClick={() => onRate(message, 'HELPFUL')}
                  className={`flex h-6 w-6 items-center justify-center rounded-full transition-colors ${
                    message.rating === 'HELPFUL'
                      ? 'bg-teal-100 text-teal-700'
                      : 'text-slate-400 hover:bg-slate-100 hover:text-teal-600'
                  }`}
                >
                  <LikeOutlined className="text-xs" />
                </button>
              </Tooltip>
              <Tooltip title={message.rating === 'UNHELPFUL' ? 'Bạn đã đánh giá không hữu ích' : 'Không hữu ích'}>
                <button
                  type="button"
                  aria-label="Đánh giá không hữu ích"
                  onClick={() => onRate(message, 'UNHELPFUL')}
                  className={`flex h-6 w-6 items-center justify-center rounded-full transition-colors ${
                    message.rating === 'UNHELPFUL'
                      ? 'bg-red-100 text-red-600'
                      : 'text-slate-400 hover:bg-slate-100 hover:text-red-500'
                  }`}
                >
                  <DislikeOutlined className="text-xs" />
                </button>
              </Tooltip>
            </span>
          )}
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
