import { FileTextOutlined } from '@ant-design/icons'
import { useState } from 'react'
import type { SourceRef } from '../../api/chat'

// Chỉ hiện nhãn chung "Tài liệu tham khảo N" - không hiện tên file/đường dẫn gốc, để người
// dùng thấy AI có căn cứ (không bịa) mà không lộ chi tiết nội bộ (tên file, nguồn đã sưu tầm).
function SourceReferenceItem({ source, index }: { source: SourceRef; index: number }) {
  const [expanded, setExpanded] = useState(false)
  const isLong = source.snippet.length > 200

  return (
    <li className="rounded-lg border border-current/10 bg-white/40 p-2.5 leading-5">
      <span className="font-semibold">Tài liệu tham khảo {index + 1}</span>
      <p className={`m-0 mt-1 whitespace-pre-wrap ${expanded ? '' : 'line-clamp-3'}`}>{source.snippet}</p>
      {isLong && (
        <button
          type="button"
          onClick={() => setExpanded((value) => !value)}
          className="mt-1 font-medium text-teal-700 underline underline-offset-2"
        >
          {expanded ? 'Thu gọn' : 'Xem thêm'}
        </button>
      )}
    </li>
  )
}

export function SourceReferences({ sources }: { sources: SourceRef[] }) {
  if (sources.length === 0) return null

  return (
    <section className="mt-4 border-t border-current/15 pt-3 text-xs">
      <p className="m-0 flex items-center gap-1.5 font-semibold">
        <FileTextOutlined /> Nguồn tham khảo
      </p>
      <ol className="m-0 mt-2 list-none space-y-2 p-0">
        {sources.map((source, index) => (
          <SourceReferenceItem key={index} source={source} index={index} />
        ))}
      </ol>
    </section>
  )
}
