import {
  DislikeTwoTone,
  FileTextOutlined,
  LikeTwoTone,
  LoadingOutlined,
  MessageOutlined,
  PhoneOutlined,
  RobotOutlined,
  SendOutlined,
  UserOutlined,
  WarningFilled,
} from '@ant-design/icons'
import { Alert, Button, Spin, Tag, Tooltip, Typography } from 'antd'
import type { ChatMessageDetail, ChatSessionSummary } from '../../api/chat'
import { describeMatchedGroups } from '../../api/chat'
import { MarkdownContent } from './MarkdownContent'
import { SourceReferences } from './SourceReferences'

const { Text } = Typography


interface HistoryMessagePanelProps {
  session: ChatSessionSummary | null
  messages: ChatMessageDetail[]
  loading: boolean
  error: string | null
  onContinue?: () => void
}

function formatTime(isoString: string) {
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }).format(
    new Date(isoString),
  )
}

function RatingBadge({ rating }: { rating: string | null }) {
  if (!rating) return null
  return rating === 'HELPFUL' ? (
    <Tag color="success" icon={<LikeTwoTone twoToneColor="#16a34a" />} className="ml-2 text-xs">
      Hữu ích
    </Tag>
  ) : (
    <Tag color="error" icon={<DislikeTwoTone twoToneColor="#dc2626" />} className="ml-2 text-xs">
      Không hữu ích
    </Tag>
  )
}

export function HistoryMessagePanel({
  session,
  messages,
  loading,
  error,
  onContinue,
}: HistoryMessagePanelProps) {
  if (!session) {
    return (
      <div className="flex h-full flex-col items-center justify-center rounded-2xl border border-dashed border-slate-200 bg-white text-center px-8 py-16">
        <MessageOutlined className="mb-4 text-4xl text-slate-300" />
        <p className="m-0 text-base font-medium text-slate-500">Chọn một phiên chat để xem nội dung</p>
        <p className="m-0 mt-2 text-sm text-slate-400">Nhấn vào phiên chat bên trái để xem lại hội thoại</p>
      </div>
    )
  }

  return (
    <div className="flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-100 px-5 py-3.5">
        <div className="min-w-0">
          <p className="m-0 truncate font-semibold text-slate-800">{session.title}</p>
          <p className="m-0 text-xs text-slate-400">
            {new Intl.DateTimeFormat('vi-VN', {
              day: '2-digit',
              month: '2-digit',
              year: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
            }).format(new Date(session.updatedAt))}
          </p>
        </div>
        {onContinue && (
          <Tooltip title="Tiếp tục cuộc trò chuyện này">
            <Button
              type="primary"
              size="small"
              icon={<SendOutlined />}
              onClick={onContinue}
              className="ml-4 shrink-0 bg-teal-600 hover:bg-teal-700! border-teal-600!"
            >
              Tiếp tục
            </Button>
          </Tooltip>
        )}
      </div>

      {/* Nội dung */}
      <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain px-5 py-5 space-y-5">
        {loading && (
          <div className="flex items-center justify-center py-12">
            <Spin indicator={<LoadingOutlined className="text-teal-600 text-xl" spin />} />
          </div>
        )}

        {error && !loading && <Alert type="error" message={error} showIcon />}

        {!loading && !error && messages.length === 0 && (
          <div className="flex flex-col items-center justify-center py-12 text-center">
            <Text type="secondary" className="text-sm">
              Phiên này chưa có tin nhắn nào.
            </Text>
          </div>
        )}

        {!loading &&
          !error &&
          messages.map((msg) => {
            const isPatient = msg.sender === 'patient'
            const matchedGroupsText = describeMatchedGroups(msg.matchedGroups)
            return (
              <article
                key={msg.id}
                className={`flex items-start gap-2.5 ${isPatient ? 'justify-end' : 'justify-start'}`}
              >
                {!isPatient && (
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-teal-100 text-teal-700 text-sm">
                    <RobotOutlined />
                  </span>
                )}

                <div className={`flex max-w-[80%] flex-col ${isPatient ? 'items-end' : 'items-start'}`}>
                  {/* Cảnh báo khẩn cấp */}
                  {!isPatient && msg.emergency && (
                    <div className="mb-2 flex items-start gap-2 rounded-xl border-2 border-red-400 bg-red-50 px-3 py-2.5 text-sm text-red-900">
                      <WarningFilled className="shrink-0 text-red-600 mt-0.5" />
                      <span>
                        <strong className="block">Phát hiện dấu hiệu cần cấp cứu!</strong>
                        Vui lòng gọi <PhoneOutlined /> <strong>115</strong>.
                        {matchedGroupsText && (
                          <span className="mt-1 block text-red-800">Dấu hiệu phát hiện: {matchedGroupsText}</span>
                        )}
                      </span>
                    </div>
                  )}

                  {/* Bubble */}
                  <div
                    className={`rounded-2xl px-4 py-3 text-sm leading-6 shadow-sm ${
                      isPatient
                        ? 'rounded-br-md bg-teal-700 text-white'
                        : 'rounded-bl-md bg-slate-100 text-slate-800'
                    }`}
                  >
                    <MarkdownContent content={msg.content} />

                    {msg.attachments.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-2 border-t border-slate-200 pt-3">
                        {msg.attachments.map((attachment) => attachment.previewUrl ? (
                          <a key={attachment.id} href={attachment.url} target="_blank" rel="noreferrer">
                            <img src={attachment.previewUrl} alt={`Tệp đính kèm ${attachment.name}`} className="h-20 w-20 rounded-lg object-cover" />
                          </a>
                        ) : (
                          <a key={attachment.id} href={attachment.url} target="_blank" rel="noreferrer" className="flex max-w-52 items-center gap-2 rounded-lg bg-white px-2.5 py-2 text-xs text-slate-700 no-underline">
                            <FileTextOutlined className="text-base text-teal-700" />
                            <span className="truncate">{attachment.name}</span>
                          </a>
                        ))}
                      </div>
                    )}

                    {/* Nguồn tham khảo */}
                    {msg.sources && msg.sources.length > 0 && <SourceReferences sources={msg.sources} />}

                    {/* Câu hỏi gợi ý */}
                    {!isPatient && msg.relatedQuestions && msg.relatedQuestions.length > 0 && (
                      <div className="mt-3 border-t border-slate-200 pt-3">
                        <p className="m-0 text-xs text-slate-500 font-medium mb-1">Câu hỏi liên quan:</p>
                        <div className="flex flex-wrap gap-1.5">
                          {msg.relatedQuestions.map((q) => (
                            <span key={q} className="rounded-full border border-teal-200 bg-white px-2 py-0.5 text-xs text-teal-800">
                              {q}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Timestamp + rating badge */}
                  <div className="mt-1 flex items-center gap-1.5 text-xs text-slate-400">
                    <time dateTime={msg.createdAt}>{formatTime(msg.createdAt)}</time>
                    {!isPatient && <RatingBadge rating={msg.rating} />}
                  </div>
                </div>

                {isPatient && (
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-teal-700 text-white text-sm">
                    <UserOutlined />
                  </span>
                )}
              </article>
            )
          })}
      </div>
    </div>
  )
}
