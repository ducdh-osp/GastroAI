import { ArrowLeftOutlined, CloseCircleOutlined, FileSearchOutlined, SearchOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Input, Pagination, Select, Spin, Tag, Typography } from 'antd'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getKnowledgeArticle, listKnowledgeArticleCategories, listKnowledgeArticles } from '../../api/articles'
import type { KnowledgeArticleDetail, KnowledgeArticleSummary } from '../../api/articles'
import { AppShell } from '../../components/layout/AppShell'
import { MarkdownContent } from '../../components/chat/MarkdownContent'

const { Text, Title } = Typography
const PAGE_SIZE = 12

const ARTICLE_ILLUSTRATIONS: Record<string, { src: string; alt: string }> = {
  'trao-nguoc-da-day-thuc-quan': {
    src: '/images/articles/gerd-mechanism.svg',
    alt: 'Cơ chế trào ngược acid dạ dày - thực quản',
  },
  'hoi-chung-ruot-kich-thich': {
    src: '/images/articles/ibs-gut-brain.svg',
    alt: 'Trục Não - Ruột và phân nhóm hội chứng ruột kích thích (IBS)',
  },
  'viem-da-day-va-benh-ly-niem-mac': {
    src: '/images/articles/gastritis-mucosa.svg',
    alt: 'Cấu trúc niêm mạc bảo vệ dạ dày và yếu tố gây viêm',
  },
  'loet-da-day-ta-trang': {
    src: '/images/articles/peptic-ulcer.svg',
    alt: 'Vị trí ổ loét niêm mạc dạ dày và tá tràng',
  },
  'benh-celiac': {
    src: '/images/articles/celiac-villi.svg',
    alt: 'Nhung mao ruột non bình thường so với teo nhung mao ở bệnh Celiac',
  },
  'tieu-chay-va-phong-mat-nuoc': {
    src: '/images/articles/diarrhea-dehydration.svg',
    alt: 'Nguyên tắc bù nước điện giải Oresol và đánh giá mất nước',
  },
  'tao-bon': {
    src: '/images/articles/constipation-habits.svg',
    alt: 'Tư thế ngồi đại tiện sinh lý 35 độ và 3 trụ cột điều trị táo bón',
  },
  'demo-30d-theo-doi-o-nong': {
    src: '/images/articles/heartburn-tracker.svg',
    alt: 'Bảng theo dõi nhật ký thời điểm và mức độ ợ nóng',
  },
  'demo-30d-nhat-ky-bristol': {
    src: '/images/articles/bristol-scale.svg',
    alt: 'Thang phân loại dạng phân Bristol từ Type 1 đến Type 7',
  },
  'demo-30d-chia-nho-bua-an': {
    src: '/images/articles/small-frequent-meals.svg',
    alt: 'Lịch phân bổ các bữa ăn nhỏ trong ngày giảm áp lực dạ dày',
  },
  'demo-30d-nhan-biet-mat-nuoc': {
    src: '/images/articles/dehydration-signs.svg',
    alt: 'Thang đo màu sắc nước tiểu và dấu hiệu mất nước',
  },
  'demo-30d-thoi-quen-sau-an': {
    src: '/images/articles/post-meal-habits.svg',
    alt: 'Thói quen lành mạnh sau bữa ăn hỗ trợ tiêu hóa',
  },
  'demo-30d-theo-doi-thuoc': {
    src: '/images/articles/medication-adherence.svg',
    alt: 'Thời điểm vàng sử dụng các nhóm thuốc đường tiêu hóa',
  },
  'demo-30d-chat-luong-giac-ngu': {
    src: '/images/articles/sleep-posture-gerd.svg',
    alt: 'Tư thế nằm ngủ nghiêng trái nâng cao đầu giường hạn chế trào ngược',
  },
  'demo-30d-khi-nao-di-kham': {
    src: '/images/articles/red-flags-triage.svg',
    alt: 'Các dấu hiệu báo động đỏ bệnh tiêu hóa cần khám ngay',
  },
}

function cleanTitle(title: string): string {
  return title.replace(/^\s*(\[DEMO\]|\(demo\)|\[demo\]|\(DEMO\))\s*/i, '').replace(/\s*\((demo|DEMO)\)\s*/gi, ' ').trim()
}

function KnowledgeArticleCard({ article }: { article: KnowledgeArticleSummary }) {
  const illustration = ARTICLE_ILLUSTRATIONS[article.slug]

  return (
    <Link to={`/articles/${article.slug}`} className="group block h-full no-underline">
      <Card
        hoverable
        className="h-full overflow-hidden rounded-2xl border-black/5 shadow-sm transition-all duration-200 group-hover:-translate-y-1 group-hover:shadow-md"
        cover={
          illustration ? (
            <div className="flex h-44 w-full items-center justify-center overflow-hidden border-b border-slate-100 bg-slate-50 p-3 transition-colors group-hover:bg-teal-50/40 dark:border-slate-700 dark:bg-slate-800">
              <img
                src={illustration.src}
                alt={illustration.alt}
                loading="lazy"
                className="max-h-full max-w-full object-contain transition-transform duration-300 group-hover:scale-105"
              />
            </div>
          ) : undefined
        }
      >
        <Tag color="cyan">{article.category}</Tag>
        <h2 className="mb-2 mt-3 text-lg font-semibold text-slate-800 transition-colors group-hover:text-teal-700 dark:text-white dark:group-hover:text-teal-400">
          {cleanTitle(article.title)}
        </h2>
        <p className="mb-2 text-sm leading-6 text-slate-600 line-clamp-3 dark:text-slate-300">
          {article.summary}
        </p>
      </Card>
    </Link>
  )
}

function ArticleDetail({ slug }: { slug: string }) {
  const navigate = useNavigate()
  const [article, setArticle] = useState<KnowledgeArticleDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const illustration = slug ? ARTICLE_ILLUSTRATIONS[slug] : undefined

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    setArticle(null)
    getKnowledgeArticle(slug)
      .then((result) => { if (!cancelled) setArticle(result) })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setError(reason instanceof Error && reason.message === 'ARTICLE_NOT_FOUND'
            ? 'Không tìm thấy bài viết này.'
            : 'Không thể tải bài viết. Vui lòng thử lại.')
        }
      })
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
          <Title level={2} className="mb-2! mt-3!">{cleanTitle(article.title)}</Title>
          <p className="my-5 text-base leading-7 text-slate-600 dark:text-white">{article.summary}</p>
          {!article.content.includes('/images/articles/') && illustration && (
            <figure className="my-6 block text-center">
              <img
                src={illustration.src}
                alt={illustration.alt}
                loading="lazy"
                className="mx-auto max-h-[420px] w-auto max-w-full rounded-2xl border border-slate-200 bg-white object-contain p-2 shadow-sm dark:border-slate-700 dark:bg-slate-800"
              />
              <figcaption className="mt-2 text-center text-xs font-medium text-slate-500 dark:text-slate-400">
                📷 {illustration.alt}
              </figcaption>
            </figure>
          )}
          <div className="border-t border-slate-100 pt-5 text-slate-700 dark:text-white">
            <MarkdownContent content={article.content} />
          </div>
          <div className="mt-8 rounded-xl bg-slate-50 p-4 text-sm">
            <Text strong>Nguồn tham khảo</Text>
            <p className="mb-1 mt-2"><a href={article.sourceUrl} target="_blank" rel="noreferrer" className="text-teal-700 underline">{article.sourceName}</a></p>
            <Text type="secondary">Đường dẫn nguồn được kiểm tra ngày {new Intl.DateTimeFormat('vi-VN').format(new Date(`${article.sourceCheckedAt}T00:00:00`))}.</Text>
          </div>
          <p className="mb-0 mt-4 text-xs text-slate-500 dark:text-white">Thông tin tham khảo không thay thế chẩn đoán hoặc tư vấn của nhân viên y tế.</p>
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
  const [categories, setCategories] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const handleClearFilters = () => {
    setQueryInput('')
    setQuery('')
    setCategory(undefined)
    setPage(0)
  }

  const hasFilterActive = Boolean(query || queryInput || category)

  useEffect(() => {
    let cancelled = false
    listKnowledgeArticleCategories()
      .then((result) => { if (!cancelled) setCategories(result) })
      .catch(() => { if (!cancelled) setError('Không thể tải chủ đề bài viết.') })
    return () => { cancelled = true }
  }, [])

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
          <div className="mb-6 flex flex-wrap items-center gap-3">
            <Input.Search
              allowClear
              enterButton={<><SearchOutlined /> Tìm</>}
              placeholder="Tìm kiếm theo tiêu đề bài viết..."
              maxLength={100}
              value={queryInput}
              onChange={(event) => {
                const val = event.target.value
                setQueryInput(val)
                if (!val) {
                  setQuery('')
                  setPage(0)
                }
              }}
              onSearch={(value) => { setPage(0); setQuery(value.trim()) }}
              className="max-w-lg"
            />
            <Select
              allowClear
              placeholder="Tất cả chủ đề"
              options={[
                { value: '', label: 'Tất cả chủ đề' },
                ...categories.map((value) => ({ value, label: value })),
              ]}
              value={category || undefined}
              onChange={(value) => {
                setPage(0)
                setCategory(value || undefined)
              }}
              className="w-56"
            />
            <Button
              icon={<CloseCircleOutlined />}
              onClick={handleClearFilters}
              disabled={!hasFilterActive}
              className="shrink-0"
            >
              Bỏ chọn
            </Button>
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
