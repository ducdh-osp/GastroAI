import type { ReactNode } from 'react'
import { findMatches } from '../../lib/vietnameseText'

interface HighlightedTextProps {
  text: string
  /** Từ khoá cần tô; rỗng hoặc null thì hiện nguyên văn. */
  query?: string | null
}

/**
 * Tô vàng mọi chỗ khớp từ khoá, theo đúng luật tìm của BE: gõ "da day" tô cả "dạ dày",
 * gõ "đau" thì không tô "đầu". Dùng chung cho ô tìm nội dung và ngăn kéo xem tài liệu.
 */
export function HighlightedText({ text: rawText, query }: HighlightedTextProps) {
  const text = rawText.normalize('NFC')
  const ranges = query ? findMatches(text, query) : []
  if (ranges.length === 0) return <>{text}</>

  const parts: ReactNode[] = []
  let cursor = 0
  ranges.forEach(([start, end], index) => {
    if (start > cursor) parts.push(text.slice(cursor, start))
    parts.push(
      <mark key={index} className="rounded bg-amber-100 px-0.5 text-inherit dark:bg-amber-500/30">
        {text.slice(start, end)}
      </mark>,
    )
    cursor = end
  })
  if (cursor < text.length) parts.push(text.slice(cursor))
  return <>{parts}</>
}