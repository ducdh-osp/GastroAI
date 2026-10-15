import {
  BookOutlined,
  CommentOutlined,
  QuestionCircleOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import { Button, Card, Collapse, Empty, Input, Skeleton, Tag, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { listFaqCategories, listFaqs, type FaqItem } from '../../api/faqs'
import { MarkdownContent } from '../../components/chat/MarkdownContent'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text, Paragraph } = Typography

export default function FaqPage() {
  const [faqs, setFaqs] = useState<FaqItem[]>([])
  const [categories, setCategories] = useState<string[]>([])
  const [selectedCategory, setSelectedCategory] = useState<string>('all')
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const [faqData, catData] = await Promise.all([
        listFaqs(),
        listFaqCategories(),
      ])
      setFaqs(faqData)
      setCategories(catData)
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Lọc phía client để phản hồi tức thì và mượt mà
  const filteredFaqs = useMemo(() => {
    return faqs.filter((faq) => {
      const matchCat =
        selectedCategory === 'all' || faq.category === selectedCategory
      if (!matchCat) return false

      if (!search.trim()) return true
      const q = search.toLowerCase().trim()
      return (
        faq.question.toLowerCase().includes(q) ||
        faq.answer.toLowerCase().includes(q)
      )
    })
  }, [faqs, selectedCategory, search])

  const collapseItems = useMemo(() => {
    return filteredFaqs.map((faq) => ({
      key: String(faq.id),
      label: (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 py-1 pr-2">
          <span className="font-semibold text-slate-800 text-base">
            {faq.question}
          </span>
          <Tag color="cyan" className="self-start sm:self-auto shrink-0">
            {faq.category}
          </Tag>
        </div>
      ),
      children: (
        <div className="pt-2 pb-4 text-slate-700 leading-relaxed border-t border-slate-100 dark:text-slate-300 dark:border-white/10">
          <MarkdownContent content={faq.answer} />
        </div>
      ),
    }))
  }, [filteredFaqs])

  return (
    <AppShell>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Hero Banner */}
        <div className="rounded-2xl bg-gradient-to-br from-teal-900 via-teal-800 to-emerald-900 text-white p-6 sm:p-8 shadow-sm">
          <div className="inline-flex items-center gap-2 rounded-full bg-teal-500/20 px-3 py-1 text-xs font-medium text-teal-300 mb-3 border border-teal-400/20">
            <QuestionCircleOutlined />
            Cẩm nang hỏi đáp y khoa
          </div>
          <Title level={2} className="!text-white !mb-2 !font-bold">
            Câu hỏi thường gặp về Tiêu hóa
          </Title>
          <Paragraph className="!text-teal-100/90 text-sm sm:text-base max-w-2xl !mb-6">
            Giải đáp nhanh chóng các băn khoăn về triệu chứng, cách xử trí tại nhà, chế độ dinh dưỡng và dấu hiệu cảnh báo từ đội ngũ chuyên khoa tiêu hóa.
          </Paragraph>

          {/* Search bar inside banner */}
          <div className="max-w-xl">
            <Input
              size="large"
              placeholder="Tìm kiếm câu hỏi hoặc triệu chứng (VD: trào ngược, đau dạ dày, vi khuẩn HP, đại tràng...)"
              prefix={<SearchOutlined className="text-teal-600 text-lg mr-1" />}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              allowClear
              className="rounded-xl shadow-lg border-0 py-2.5"
            />
          </div>
        </div>

        {/* Category pills */}
        <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
          <button
            type="button"
            onClick={() => setSelectedCategory('all')}
            className={`cursor-pointer rounded-full px-4 py-1.5 text-xs sm:text-sm font-medium transition-all shrink-0 ${
              selectedCategory === 'all'
                ? 'bg-teal-600 text-white shadow-sm'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50 dark:bg-slate-800 dark:text-slate-300 dark:border-white/10 dark:hover:bg-slate-700'
            }`}
          >
            Tất cả ({faqs.length})
          </button>
          {categories.map((cat) => {
            const count = faqs.filter((f) => f.category === cat).length
            return (
              <button
                key={cat}
                type="button"
                onClick={() => setSelectedCategory(cat)}
                className={`cursor-pointer rounded-full px-4 py-1.5 text-xs sm:text-sm font-medium transition-all shrink-0 ${
                  selectedCategory === cat
                    ? 'bg-teal-600 text-white shadow-sm'
                    : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50 dark:bg-slate-800 dark:text-slate-300 dark:border-white/10 dark:hover:bg-slate-700'
                }`}
              >
                {cat} ({count})
              </button>
            )
          })}
        </div>

        {/* Main Content: Accordion */}
        <Card className="rounded-xl shadow-xs border-slate-200" bodyStyle={{ padding: 16 }}>
          {loading ? (
            <div className="p-4 space-y-4">
              <Skeleton active paragraph={{ rows: 2 }} />
              <Skeleton active paragraph={{ rows: 2 }} />
              <Skeleton active paragraph={{ rows: 2 }} />
            </div>
          ) : collapseItems.length > 0 ? (
            <Collapse
              items={collapseItems}
              bordered={false}
              defaultActiveKey={collapseItems.length > 0 ? [collapseItems[0].key] : []}
              className="bg-transparent"
              expandIconPosition="end"
            />
          ) : (
            <div className="py-12">
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <div className="space-y-1">
                    <Text className="text-slate-600 font-medium dark:text-slate-300">
                      Không tìm thấy câu hỏi phù hợp với "{search}"
                    </Text>
                    <p className="text-xs text-slate-400 dark:text-slate-500">
                      Hãy thử từ khóa khác hoặc đặt câu hỏi trực tiếp cho Trợ lý AI GastroAI bên dưới.
                    </p>
                  </div>
                }
              />
            </div>
          )}
        </Card>

        {/* Call to action: Chat bot & Articles */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          <Card className="rounded-xl border-teal-100 bg-teal-50/50 hover:bg-teal-50 transition-colors">
            <div className="flex items-start gap-3">
              <div className="rounded-lg bg-teal-600 p-2.5 text-white">
                <CommentOutlined className="text-lg" />
              </div>
              <div className="space-y-1">
                <h4 className="font-semibold text-teal-950 text-sm">
                  Vẫn còn thắc mắc về triệu chứng?
                </h4>
                <p className="text-xs text-teal-800/80">
                  Trò chuyện riêng tư với Trợ lý AI GastroAI để được giải đáp và sàng lọc triệu chứng ngay tức thì.
                </p>
                <div className="pt-2">
                  <Link to="/chat">
                    <Button
                      type="primary"
                      size="small"
                      className="bg-teal-600 hover:bg-teal-700"
                    >
                      Bắt đầu chat ngay
                    </Button>
                  </Link>
                </div>
              </div>
            </div>
          </Card>

          <Card className="rounded-xl border-slate-200 bg-white hover:bg-slate-50/50 transition-colors dark:border-white/10 dark:bg-slate-900 dark:hover:bg-slate-800/50">
            <div className="flex items-start gap-3">
              <div className="rounded-lg bg-slate-800 p-2.5 text-white">
                <BookOutlined className="text-lg" />
              </div>
              <div className="space-y-1">
                <h4 className="font-semibold text-slate-900 text-sm dark:text-slate-100">
                  Đọc bài viết kiến thức chuyên sâu
                </h4>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Tìm hiểu các bài viết phân tích y khoa chi tiết về dinh dưỡng, phác đồ điều trị và chăm sóc dạ dày.
                </p>
                <div className="pt-2">
                  <Link to="/articles">
                    <Button size="small">
                      Xem kho bài viết
                    </Button>
                  </Link>
                </div>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </AppShell>
  )
}
