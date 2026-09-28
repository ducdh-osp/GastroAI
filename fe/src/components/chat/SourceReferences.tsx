import { FileTextOutlined } from '@ant-design/icons'
import type { SourceRef } from '../../api/chat'

function safeSourceUrl(sourceUrl?: string | null): string | null {
  if (!sourceUrl) return null
  try {
    const url = new URL(sourceUrl)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.toString() : null
  } catch {
    return null
  }
}

export function SourceReferences({ sources }: { sources: SourceRef[] }) {
  if (sources.length === 0) return null

  return (
    <section className="mt-4 border-t border-current/15 pt-3 text-xs">
      <p className="m-0 flex items-center gap-1.5 font-semibold">
        <FileTextOutlined /> Nguồn tham khảo
      </p>
      <ol className="m-0 mt-2 list-none space-y-2 p-0">
        {sources.map((source, index) => {
          const url = safeSourceUrl(source.sourceUrl)
          return (
            <li key={`${source.documentTitle}-${index}`} className="rounded-lg border border-current/10 bg-white/40 p-2.5 leading-5">
              {url ? (
                <a href={url} target="_blank" rel="noopener noreferrer" className="font-semibold underline underline-offset-2">
                  {source.documentTitle}
                </a>
              ) : (
                <span className="font-semibold">{source.documentTitle}</span>
              )}
              <p className="m-0 mt-1 whitespace-pre-wrap">{source.snippet}</p>
            </li>
          )
        })}
      </ol>
    </section>
  )
}
