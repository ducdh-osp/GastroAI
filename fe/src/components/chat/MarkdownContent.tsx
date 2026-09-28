import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

/** Render model and user messages as safe Markdown, with GitHub-flavored lists/tables. */
export function MarkdownContent({ content }: { content: string }) {
  return (
    <div className="break-words [&>*:first-child]:mt-0 [&>*:last-child]:mb-0">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          p: ({ children }) => <p className="my-2 whitespace-pre-wrap">{children}</p>,
          ul: ({ children }) => <ul className="my-2 list-disc space-y-1 pl-5">{children}</ul>,
          ol: ({ children }) => <ol className="my-2 list-decimal space-y-1 pl-5">{children}</ol>,
          li: ({ children }) => <li className="pl-0.5">{children}</li>,
          h1: ({ children }) => <h1 className="mb-2 mt-4 text-lg font-bold">{children}</h1>,
          h2: ({ children }) => <h2 className="mb-2 mt-4 text-base font-bold">{children}</h2>,
          h3: ({ children }) => <h3 className="mb-2 mt-3 text-sm font-bold">{children}</h3>,
          blockquote: ({ children }) => <blockquote className="my-2 border-l-2 border-current/30 pl-3 opacity-90">{children}</blockquote>,
          code: ({ children, className }) => (
            <code className={`${className ?? ''} rounded bg-black/5 px-1 py-0.5 font-mono text-[0.9em]`}>{children}</code>
          ),
          pre: ({ children }) => <pre className="my-2 overflow-x-auto rounded-lg bg-black/5 p-3 text-sm">{children}</pre>,
          a: ({ children, href }) => (
            <a href={href} target="_blank" rel="noopener noreferrer" className="underline underline-offset-2">
              {children}
            </a>
          ),
          table: ({ children }) => <div className="my-2 overflow-x-auto"><table className="w-full border-collapse text-left">{children}</table></div>,
          th: ({ children }) => <th className="border border-current/20 px-2 py-1 font-semibold">{children}</th>,
          td: ({ children }) => <td className="border border-current/20 px-2 py-1">{children}</td>,
          hr: () => <hr className="my-3 border-current/20" />,
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  )
}
