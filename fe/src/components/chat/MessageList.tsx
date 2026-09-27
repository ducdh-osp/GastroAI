import { MessageOutlined, RobotOutlined } from '@ant-design/icons'
import { useEffect, useRef } from 'react'
import { MessageBubble } from './MessageBubble'
import { QUICK_PROMPTS } from '../../api/chat'
import type { Message } from '../../api/chat'

interface MessageListProps {
  messages: Message[]
  isReplying: boolean
  onQuickPrompt: (prompt: string) => void
  onRetry: (message: Message) => void
}

export function MessageList({ messages, isReplying, onQuickPrompt, onRetry }: MessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [messages, isReplying])

  return (
    <section className="min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6" aria-label="Nội dung hội thoại" aria-live="polite">
      <div className="mx-auto flex max-w-3xl flex-col gap-5">
        {messages.length === 0 && (
          <div className="flex min-h-72 flex-col items-center justify-center text-center">
            <span className="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-teal-50 text-2xl text-teal-700" aria-hidden="true"><MessageOutlined /></span>
            <h2 className="m-0 text-xl font-semibold text-slate-800">Bạn cần tư vấn điều gì?</h2>
            <p className="mb-5 mt-2 max-w-md text-sm leading-6 text-slate-500">Hãy mô tả triệu chứng hoặc chọn một câu hỏi gợi ý để bắt đầu cuộc trò chuyện.</p>
            <div className="flex flex-wrap justify-center gap-2">
              {QUICK_PROMPTS.map((prompt) => (
                <button key={prompt} type="button" onClick={() => onQuickPrompt(prompt)} className="rounded-full border border-teal-200 bg-white px-3.5 py-2 text-sm text-teal-800 transition hover:border-teal-400 hover:bg-teal-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-teal-500">
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
          />
        ))}

        {isReplying && (
          <div className="flex items-start gap-2.5" role="status">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-teal-100 text-teal-700" aria-hidden="true"><RobotOutlined /></span>
            <div className="flex items-center gap-1.5 rounded-2xl rounded-bl-md bg-slate-100 px-4 py-4">
              {[0, 1, 2].map((index) => <span key={index} className="h-2 w-2 animate-pulse rounded-full bg-slate-400" style={{ animationDelay: `${index * 160}ms` }} />)}
              <span className="sr-only">Trợ lý đang trả lời</span>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>
    </section>
  )
}
