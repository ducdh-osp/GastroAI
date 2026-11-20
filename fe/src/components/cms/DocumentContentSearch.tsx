import { DownOutlined, FileSearchOutlined, FileTextOutlined, RightOutlined, UpOutlined } from '@ant-design/icons'
import { Button, Card, Empty, Input, Tag, Typography, message } from 'antd'
import { useState, type ReactNode } from 'react'
import {
  searchCmsDocumentChunks,
  searchCmsDocumentContent,
  type CmsDocumentSearchResult,
} from '../../api/cmsDocuments'
import { extractErrorMessage } from '../../lib/errors'
import { findExcerpts } from '../../lib/vietnameseText'
import { HighlightedText } from './HighlightedText'

const { Text } = Typography

interface MatchingChunk {
  chunkIndex: number
  content: string
}

interface DocumentContentSearchProps {
  /** Công tắc "Tên / nguồn | Nội dung" của trang, đặt cạnh ô tìm để 2 kiểu tìm nằm cùng 1 chỗ. */
  modeSwitch: ReactNode
  /**
   * Mở ngăn kéo "Xem & xác nhận nội dung" của tài liệu này, kèm từ khoá để ngăn kéo tô vàng;
   * chunkIndex = đoạn người dùng vừa bấm, ngăn kéo cuộn thẳng tới đó.
   */
  onOpenDocument: (documentId: number, query: string, chunkIndex?: number) => void
}

/** Mỗi tài liệu lúc đầu chỉ hiện vài đoạn, mỗi đoạn vài câu trích - đọc kỹ thì mở ngăn kéo. */
const INITIAL_CHUNKS = 3
const EXCERPTS_PER_CHUNK = 2

interface MatchingChunkListProps {
  chunks: MatchingChunk[]
  query: string
  onOpenChunk: (chunkIndex: number) => void
}

/**
 * Các chỗ khớp trong 1 tài liệu, kiểu kết quả tìm của Google: mỗi đoạn 1 dòng gồm vài câu trích ngắn
 * quanh từ khoá. Bấm 1 dòng là mở đúng đoạn đó trong ngăn kéo, cạnh file gốc.
 */
function MatchingChunkList({ chunks, query, onOpenChunk }: MatchingChunkListProps) {
  const [shown, setShown] = useState(INITIAL_CHUNKS)

  return (
    <div className="mt-2 border-l-2 border-teal-200 pl-2 dark:border-teal-700">
      {chunks.slice(0, shown).map((chunk) => {
        const { excerpts } = findExcerpts(chunk.content, query)
        const more = excerpts.length - EXCERPTS_PER_CHUNK
        return (
          <button
            key={chunk.chunkIndex}
            type="button"
            onClick={() => onOpenChunk(chunk.chunkIndex)}
            className="group flex w-full cursor-pointer items-start gap-3 rounded-lg border-0 bg-transparent px-2 py-2 text-left hover:bg-teal-50 dark:hover:bg-white/5"
          >
            <span className="mt-0.5 shrink-0 rounded bg-slate-100 px-1.5 py-0.5 text-xs text-slate-500 dark:bg-white/10 dark:text-white/70">
              Đoạn {chunk.chunkIndex + 1}
            </span>
            <span className="min-w-0 flex-1 space-y-1">
              {excerpts.slice(0, EXCERPTS_PER_CHUNK).map((excerpt, index) => (
                <span key={index} className="block text-sm leading-relaxed text-slate-700 dark:text-white">
                  <HighlightedText text={excerpt} query={query} />
                </span>
              ))}
              {more > 0 && (
                <span className="block text-xs text-slate-400 dark:text-white/50">+{more} chỗ khác trong đoạn này</span>
              )}
            </span>
            <RightOutlined className="mt-1.5 shrink-0 text-xs text-slate-300 group-hover:text-teal-600" />
          </button>
        )
      })}
      {chunks.length > shown && (
        <Button size="small" type="link" onClick={() => setShown(chunks.length)}>
          Xem thêm {chunks.length - shown} đoạn
        </Button>
      )}
    </div>
  )
}

/**
 * UC0055 - tìm trong nội dung các tài liệu chatbot đang dùng. Mỗi tài liệu 1 dòng (khớp nhiều nhất lên
 * đầu), bấm mới tải các đoạn khớp. Giúp Biên tập dò tài liệu nào còn nói khác phác đồ mới (SCR-AD-04 §3.1b).
 */
export function DocumentContentSearch({ modeSwitch, onOpenDocument }: DocumentContentSearchProps) {
  const [query, setQuery] = useState('')
  const [searchedQuery, setSearchedQuery] = useState('')
  const [result, setResult] = useState<CmsDocumentSearchResult | null>(null)
  const [searching, setSearching] = useState(false)
  const [chunksByDocument, setChunksByDocument] = useState<Record<number, MatchingChunk[]>>({})
  const [openDocumentIds, setOpenDocumentIds] = useState<number[]>([])
  const [loadingDocumentId, setLoadingDocumentId] = useState<number | null>(null)

  async function handleSearch(value: string) {
    const q = value.trim()
    if (q.length < 2) {
      message.warning('Nhập ít nhất 2 ký tự để tìm trong nội dung')
      return
    }
    setSearching(true)
    try {
      const data = await searchCmsDocumentContent(q)
      setResult(data)
      setSearchedQuery(q)
      setChunksByDocument({})
      setOpenDocumentIds([])
    } catch (err) {
      message.error(extractErrorMessage(err, 'Tìm kiếm thất bại').message)
    } finally {
      setSearching(false)
    }
  }

  async function toggleChunks(documentId: number) {
    if (openDocumentIds.includes(documentId)) {
      setOpenDocumentIds((ids) => ids.filter((id) => id !== documentId))
      return
    }
    if (!chunksByDocument[documentId]) {
      setLoadingDocumentId(documentId)
      try {
        const chunks = await searchCmsDocumentChunks(documentId, searchedQuery)
        setChunksByDocument((current) => ({ ...current, [documentId]: chunks }))
      } catch (err) {
        message.error(extractErrorMessage(err, 'Không tải được các đoạn khớp').message)
        return
      } finally {
        setLoadingDocumentId(null)
      }
    }
    setOpenDocumentIds((ids) => [...ids, documentId])
  }

  return (
    <div className="space-y-4">
      <Card styles={{ body: { padding: 16 } }}>
        <div className="flex flex-wrap items-center gap-3">
          {modeSwitch}
          <Input.Search
            placeholder="VD: da day, phác đồ 4 thuốc"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onSearch={handleSearch}
            loading={searching}
            enterButton="Tìm"
            allowClear
            maxLength={200}
            style={{ flex: 1, minWidth: 240 }}
          />
        </div>
      </Card>

      <Card styles={{ body: { padding: 0 } }}>
        {result === null ? (
          <Empty
            className="py-10"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description="Gõ từ khoá để xem tài liệu nào đang nói về nội dung đó, ví dụ trước khi xác nhận một phác đồ mới."
          />
        ) : result.items.length === 0 ? (
          <Empty
            className="py-10"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={`Không tài liệu nào có “${searchedQuery}”. Thử từ khác hoặc ngắn hơn.`}
          />
        ) : (
          <>
            <div className="flex flex-wrap items-baseline gap-x-2 border-b border-black/5 px-4 py-3 dark:border-white/10">
              <Text strong>
                {result.totalDocuments} tài liệu có “{searchedQuery}”
              </Text>
              {result.totalDocuments > result.items.length && (
                <Text type="secondary" className="text-xs">
                  Đang hiện {result.items.length} tài liệu khớp nhiều nhất, gõ cụ thể hơn để thu hẹp.
                </Text>
              )}
            </div>

            <div className="divide-y divide-black/5 dark:divide-white/10">
              {result.items.map((hit) => {
                const isOpen = openDocumentIds.includes(hit.documentId)
                const chunks = chunksByDocument[hit.documentId]
                return (
                  <div key={hit.documentId} className="px-4 py-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex min-w-0 items-center gap-2">
                        <FileTextOutlined className="text-teal-600" />
                        <span className="truncate font-semibold text-slate-800 dark:text-white">{hit.title}</span>
                        <Tag className="m-0 shrink-0">{hit.matchCount} đoạn</Tag>
                      </div>
                      <div className="flex shrink-0 items-center gap-1">
                        <Button
                          size="small"
                          icon={isOpen ? <UpOutlined /> : <DownOutlined />}
                          loading={loadingDocumentId === hit.documentId}
                          onClick={() => void toggleChunks(hit.documentId)}
                        >
                          {isOpen ? 'Thu gọn' : 'Xem các đoạn'}
                        </Button>
                        <Button
                          size="small"
                          type="link"
                          icon={<FileSearchOutlined />}
                          onClick={() => onOpenDocument(hit.documentId, searchedQuery)}
                        >
                          Mở tài liệu
                        </Button>
                      </div>
                    </div>

                    {!isOpen && (
                      <p className="mb-0 mt-1 line-clamp-2 text-sm leading-relaxed text-slate-600 dark:text-white/80">
                        <HighlightedText text={hit.snippet} query={searchedQuery} />
                      </p>
                    )}

                    {isOpen && chunks && (
                      <MatchingChunkList
                        chunks={chunks}
                        query={searchedQuery}
                        onOpenChunk={(chunkIndex) => onOpenDocument(hit.documentId, searchedQuery, chunkIndex)}
                      />
                    )}
                  </div>
                )
              })}
            </div>
          </>
        )}
      </Card>
    </div>
  )
}