import { DownOutlined, FileTextOutlined, UpOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Collapse, Empty, Input, Tag, Typography, message } from 'antd'
import { useMemo, useState } from 'react'
import { searchCmsDocumentContent, type CmsDocumentSearchHit } from '../../api/cmsDocuments'

const { Text } = Typography

function errorMessageOf(err: unknown, fallback: string): string {
  return (err as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback
}


function escapeRegExp(text: string) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function highlight(text: string, q: string) {
  if (!q) return text
  const parts = text.split(new RegExp(`(${escapeRegExp(q)})`, 'gi'))
  return parts.map((part, index) =>
    part.toLowerCase() === q.toLowerCase() ? (
      <mark key={index} className="rounded bg-amber-100 px-0.5 text-inherit dark:bg-amber-500/30">
        {part}
      </mark>
    ) : (
      part
    ),
  )
}

interface HitGroup {
  documentId: number
  title: string
  hits: CmsDocumentSearchHit[]
}


export function DocumentContentSearch() {
  const [query, setQuery] = useState('')
  const [searchedQuery, setSearchedQuery] = useState('')
  const [hits, setHits] = useState<CmsDocumentSearchHit[] | null>(null)
  const [truncated, setTruncated] = useState(false)
  const [searching, setSearching] = useState(false)
  const [expanded, setExpanded] = useState<Set<string>>(() => new Set())

  function toggleExpanded(key: string) {
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  const groups = useMemo<HitGroup[]>(() => {
    if (!hits) return []
    const map = new Map<number, HitGroup>()
    for (const hit of hits) {
      const group = map.get(hit.documentId) ?? { documentId: hit.documentId, title: hit.title, hits: [] }
      group.hits.push(hit)
      map.set(hit.documentId, group)
    }
    return [...map.values()]
  }, [hits])

  async function handleSearch(value: string) {
    const q = value.trim()
    if (q.length < 2) {
      message.warning('Nhập ít nhất 2 ký tự để tìm trong nội dung')
      return
    }
    setSearching(true)
    try {
      const result = await searchCmsDocumentContent(q)
      setHits(result.items)
      setTruncated(result.truncated)
      setSearchedQuery(q)
      setExpanded(new Set())
    } catch (err) {
      message.error(errorMessageOf(err, 'Tìm kiếm thất bại'))
    } finally {
      setSearching(false)
    }
  }

  return (
    <Card styles={{ body: { padding: 16 } }}>
      <Input.Search
        placeholder="VD: Helicobacter pylori, liều PPI..."
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onSearch={handleSearch}
        loading={searching}
        enterButton="Tìm"
        allowClear
        maxLength={200}
      />
      <Text type="secondary" className="text-xs mt-1 block">
        Chỉ tìm trong phiên bản đang được chatbot dùng. Tìm có phân biệt dấu (gõ "dạ dày", không gõ "da day").
      </Text>

      {hits !== null && (
        <div className="mt-4 space-y-3">
          {truncated && (
            <Alert
              type="warning"
              showIcon
              className="text-xs"
              title="Chỉ hiển thị 50 đoạn đầu tiên. Hãy nhập từ khóa cụ thể hơn để thu hẹp kết quả."
            />
          )}

          {hits.length === 0 ? (
            <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Không tìm thấy đoạn nào chứa cụm từ này" />
          ) : (
            <>
              <Text type="secondary" className="text-xs block">
                Tìm thấy {hits.length}{truncated ? '+' : ''} đoạn trong {groups.length} tài liệu
              </Text>
              <Collapse
                key={searchedQuery}
                defaultActiveKey={groups.map((group) => String(group.documentId))}
                items={groups.map((group) => ({
                  key: String(group.documentId),
                  label: (
                    <span className="font-medium">
                      <FileTextOutlined className="mr-2 text-teal-600" />
                      {group.title}
                    </span>
                  ),
                  extra: <Tag className="m-0">{group.hits.length} đoạn</Tag>,
                  children: (
                    <div className="divide-y divide-slate-100 dark:divide-white/10">
                      {group.hits.map((hit) => {
                        const key = `${hit.documentId}-${hit.chunkIndex}`
                        const isExpanded = expanded.has(key)
                        // Doan trich co "…" o dau/cuoi tuc la BE da cat bot - moi can nut xem ca doan.
                        const isCut = hit.snippet.startsWith('…') || hit.snippet.endsWith('…')
                        return (
                          <div key={key} className="flex gap-3 py-2 first:pt-0 last:pb-0">
                            <Tag className="m-0 h-fit shrink-0">Đoạn {hit.chunkIndex + 1}</Tag>
                            <div className="min-w-0 flex-1">
                              <div className={`text-sm leading-relaxed text-slate-700 dark:text-white ${isExpanded ? 'whitespace-pre-line' : ''}`}>
                                {highlight(isExpanded ? (hit.content ?? hit.snippet) : hit.snippet, searchedQuery)}
                              </div>
                              {isCut && (
                                <div className="mt-1">
                                  <Button
                                    type="link"
                                    size="small"
                                    icon={isExpanded ? <UpOutlined /> : <DownOutlined />}
                                    iconPosition="end"
                                    className="px-0! h-auto! text-xs!"
                                    onClick={() => toggleExpanded(key)}
                                  >
                                    {isExpanded ? 'Thu gọn' : 'Xem cả đoạn'}
                                  </Button>
                                </div>
                              )}
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  ),
                }))}
              />
            </>
          )}
        </div>
      )}
    </Card>
  )
}