import { ArrowLeftOutlined, ClockCircleOutlined, FileSearchOutlined, SearchOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Input, Pagination, Select, Spin, Tag, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getKnowledgeArticle, listKnowledgeArticles } from '../../api/articles'
import type { KnowledgeArticleDetail, KnowledgeArticleSummary } from '../../api/articles'
import { AppShell } from '../../components/layout/AppShell'
import { MarkdownContent } from '../../components/chat/MarkdownContent'

const { Text, Title } = Typography
const PAGE_SIZE = 12
const CATEGORY_OPTIONS = [
  { value: 'Trào ngược dạ dày', label: 'Trào ngược dạ dày' },
  { value: 'Đại tràng và ruột', label: 'Đại tràng và ruột' },
  { value: 'Dạ dày', label: 'Dạ dày' },
  { value: 'Dinh dưỡng và hấp thu', label: 'Dinh dưỡng và hấp thu' },
]

function KnowledgeArticleCard({ article }: { article: KnowledgeArticleSummary }) {
  return (
    <Link to={`/articles/${article.slug}`} className="block h-full no-underline">
      <Card hoverable className="h-full rounded-2xl border-black/5 shadow-sm">
        <Tag color="cyan">{article.category}</Tag>
        <h2 className="mb-2 mt-3 text-lg font-semibold text-slate-800">{article.title}</h2>
        <p className="mb-4 text-sm leading-6 text-slate-600">{article.summary}</p>
        <Text type="secondary" className="text-xs"><ClockCircleOutlined className="mr-1" />{article.readingMinutes} phút đọc</Text>
      </Card>
    </Link>
  )
}

function ArticleDetail({ slug }: { slug: string }) {
  const navigate = useNavigate()
  const [article, setArticle] = useState<KnowledgeArticleDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    setArticle(null)
    getKnowledgeArticle(slug)
      .then((result) => { if (!cancelled) setArticle(result) })
      .catch(() => { if (!cancelled) setError('Không tìm thấy bài viết hoặc hiện không thể tải bài.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [slug])

  return (
    <div className="mx-auto max-w-4xl">
      <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/articles')} className="mb-4 px-0 text-teal-700">
        Quay lại thư viện
      </Button>
      {loading && <div className="py-20 text-center"><Spin size="large" /></div>}
      {error && <Alert type="error" showIcon message={error} />}
      {article && (
        <Card className="rounded-2xl border-black/5 shadow-sm">
          <Tag color="cyan">{article.category}</Tag>
          <Title level={2} className="mb-2! mt-3!">{article.title}</Title>
          <Text type="secondary"><ClockCircleOutlined className="mr-1" />{article.readingMinutes} phút đọc</Text>
          <p className="my-5 text-base leading-7 text-slate-600">{article.summary}</p>
          <div className="border-t border-slate-100 pt-5 text-slate-700">
            <MarkdownContent content={article.content} />
          </div>
          <div className="mt-8 rounded-xl bg-slate-50 p-4 text-sm">
            <Text strong>Nguồn tham khảo</Text>
            <p className="mb-1 mt-2"><a href={article.sourceUrl} target="_blank" rel="noreferrer" className="text-teal-700 underline">{article.sourceName}</a></p>
            <Text type="secondary">Đường dẫn nguồn được kiểm tra ngày {new Intl.DateTimeFormat('vi-VN').format(new Date(`${article.sourceCheckedAt}T00:00:00`))}.</Text>
          </div>
          <p className="mb-0 mt-4 text-xs text-slate-500">Thông tin tham khảo không thay thế chẩn đoán hoặc tư vấn của nhân viên y tế.</p>
        </Card>
      )}
    </div>
  )
}

export default function KnowledgeLibraryPage() {
  const { slug } = useParams()
  const [articles, setArticles] = useState<KnowledgeArticleSummary[]>([])
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState<string>()
  const [page, setPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (slug) return
    let cancelled = false
    setLoading(true)
    setError(null)
    listKnowledgeArticles({ q: query || undefined, category, page, size: PAGE_SIZE })
      .then((result) => {
        if (cancelled) return
        setArticles(result.articles)
        setTotalElements(result.totalElements)
        setTotalPages(result.totalPages)
      })
      .catch(() => { if (!cancelled) setError('Không thể tải thư viện bài viết. Vui lòng thử lại.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [slug, query, category, page])

  return (
    <AppShell>
      {slug ? <ArticleDetail slug={slug} /> : (
        <div className="mx-auto max-w-6xl">
          <div className="mb-6">
            <Text type="secondary">Góc kiến thức tiêu hoá</Text>
            <Title level={2} className="mb-1! mt-1!">Thư viện bài viết</Title>
            <Text type="secondary">Tìm hiểu các chủ đề tiêu hoá từ nguồn y tế đáng tin cậy trước khi trao đổi với trợ lý GastroAI.</Text>
          </div>
          <div className="mb-6 flex flex-wrap gap-3">
            <Input.Search
              allowClear
              enterButton={<><SearchOutlined /> Tìm</>}
              placeholder="Tìm theo triệu chứng hoặc chủ đề"
              value={queryInput}
              onChange={(event) => setQueryInput(event.target.value)}
              onSearch={(value) => { setPage(0); setQuery(value.trim()) }}
              className="max-w-lg"
            />
            <Select
              allowClear
              placeholder="Tất cả chủ đề"
              options={CATEGORY_OPTIONS}
              value={category}
              onChange={(value) => { setPage(0); setCategory(value) }}
              className="w-56"
            />
          </div>
          {error && <Alert type="error" showIcon message={error} className="mb-4" />}
          {loading ? <div className="py-20 text-center"><Spin size="large" /></div> : articles.length === 0 ? (
            <Empty image={<FileSearchOutlined className="text-4xl text-slate-300" />} description="Không tìm thấy bài viết phù hợp." />
          ) : (
            <>
              <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                {articles.map((article) => <KnowledgeArticleCard key={article.slug} article={article} />)}
              </div>
              {totalPages > 1 && <div className="mt-6 flex justify-center"><Pagination current={page + 1} pageSize={PAGE_SIZE} total={totalElements} onChange={(current) => setPage(current - 1)} showSizeChanger={false} /></div>}
            </>
          )}
        </div>
      )}
    </AppShell>
  )
}
