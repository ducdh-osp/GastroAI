import { MessageOutlined, RobotOutlined } from '@ant-design/icons'
import { useEffect, useRef } from 'react'
import { MessageBubble } from './MessageBubble'
import { QUICK_PROMPTS } from '../../api/chat'
import type { Message, RatingValue } from '../../api/chat'

interface MessageListProps {
  messages: Message[]
  isReplying: boolean
  isLoadingHistory: boolean
  onQuickPrompt: (prompt: string) => void
  onRetry: (message: Message) => void
  onRate: (message: Message, rating: RatingValue) => void
}

export function MessageList({
  messages,
  isReplying,
  isLoadingHistory,
  onQuickPrompt,
  onRetry,
  onRate,
}: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [messages, isReplying])

  const lastMessage = messages[messages.length - 1]
  const hasStreamingAssistantBubble =
    lastMessage?.sender === 'assistant' && lastMessage?.status === 'replying'

  return (
    <section
      className="min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6"
      aria-label="Nội dung hội thoại"
      aria-live="polite"
    >
      <div className="mx-auto flex max-w-3xl flex-col gap-5">
        {isLoadingHistory && (
          <div className="flex items-center justify-center py-10 text-slate-400">
            <MessageOutlined className="mr-2" />
            Đang tải lịch sử trò chuyện...
          </div>
        )}

        {!isLoadingHistory && messages.length === 0 && (
          <div className="flex flex-col items-center gap-3 py-10">
            <p className="text-sm text-slate-500">Bạn có thể bắt đầu với một trong các câu hỏi gợi ý:</p>
            <div className="flex flex-wrap justify-center gap-2">
              {QUICK_PROMPTS.map((prompt) => (
                <button
                  key={prompt}
                  type="button"
                  onClick={() => onQuickPrompt(prompt)}
                  className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600 transition hover:border-blue-300 hover:text-blue-600"
                >
                  {prompt}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((message) => (
          <MessageBubble
            key={message.id}
            message={message}
            onRetry={onRetry}
            onSuggestionClick={onQuickPrompt}
            suggestionsDisabled={isReplying}
            onRate={onRate}
          />
        ))}

        {isReplying && !hasStreamingAssistantBubble && (
          <div className="flex items-start gap-2.5" role="status">
            <span className="flex h-8 w-8 flex-none items-center justify-center rounded-full bg-blue-50 text-blue-500">
              <RobotOutlined />
            </span>
            <div className="flex items-center gap-1.5 rounded-2xl rounded-bl-md bg-slate-100 px-4 py-4">
              {[0, 1, 2].map((index) => (
                <span
                  key={index}
                  className="h-2 w-2 animate-pulse rounded-full bg-slate-400"
                  style={{ animationDelay: `${index * 160}ms` }}
                />
              ))}
              <span className="sr-only">Trợ lý đang trả lời</span>
            </div>
          </div>
        )}

        <div ref={bottomRef} />
      </div>
    </section>
  )
}