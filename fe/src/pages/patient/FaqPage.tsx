import {
  BookOutlined,
  CommentOutlined,
  QuestionCircleOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import { Button, Card, Collapse, Empty, Input, Skeleton, Tag, Typography } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { listFaqs, type FaqItem } from '../../api/faqs'
import { MarkdownContent } from '../../components/chat/MarkdownContent'
import { AppShell } from '../../components/layout/AppShell'

const { Title, Text, Paragraph } = Typography

export default function FaqPage() {
  const [faqs, setFaqs] = useState<FaqItem[]>([])
  const [inputSearch, setInputSearch] = useState('')
  const [activeSearch, setActiveSearch] = useState('')
  const [loading, setLoading] = useState(true)

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const faqData = await listFaqs()
      setFaqs(faqData)
    } catch {
      // ignore
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSearch = () => {
    setActiveSearch(inputSearch.trim())
  }

  const hasQuery = Boolean(activeSearch.trim())

  const MAX_RESULTS = 5

  // Chỉ hiển thị tối đa 5 kết quả khi người dùng ấn Enter tra cứu
  const { filteredFaqs, totalMatches } = useMemo(() => {
    if (!hasQuery) return { filteredFaqs: [], totalMatches: 0 }
    const q = activeSearch.toLowerCase().trim()
    const matches = faqs.filter(
      (faq) =>
        faq.question.toLowerCase().includes(q) ||
        faq.answer.toLowerCase().includes(q) ||
        faq.category.toLowerCase().includes(q)
    )
    return {
      filteredFaqs: matches.slice(0, MAX_RESULTS),
      totalMatches: matches.length,
    }
  }, [faqs, activeSearch, hasQuery])

  const collapseItems = useMemo(() => {
    return filteredFaqs.map((faq) => ({
      key: String(faq.id),
      label: (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 py-1 pr-2">
          <span className="font-semibold text-slate-800 dark:text-white text-base">
            {faq.question}
          </span>
          <Tag color="cyan" className="self-start sm:self-auto shrink-0">
            {faq.category}
          </Tag>
        </div>
      ),
      children: (
        <div className="pt-2 pb-4 text-slate-700 dark:text-white leading-relaxed border-t border-slate-100 dark:text-white dark:border-white/10">
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
              placeholder="Nhập từ khóa và nhấn Enter để tra cứu (VD: trào ngược, dạ dày...)"
              prefix={<SearchOutlined className="text-teal-600 text-lg mr-1 cursor-pointer" onClick={handleSearch} />}
              value={inputSearch}
              onChange={(e) => {
                setInputSearch(e.target.value)
                if (!e.target.value.trim()) {
                  setActiveSearch('')
                }
              }}
              onPressEnter={handleSearch}
              allowClear
              suffix={
                <Button
                  type="primary"
                  size="small"
                  onClick={handleSearch}
                  className="bg-teal-600 hover:bg-teal-700 text-xs font-medium px-3 rounded-md"
                >
                  Tìm
                </Button>
              }
              className="rounded-xl shadow-lg border-0 py-2"
            />
          </div>
        </div>

        {/* Main Content: Accordion */}
        <Card className="rounded-xl shadow-xs border-slate-200 dark:border-slate-800 dark:bg-slate-900/40" styles={{ body: { padding: 16 } }}>
          {loading ? (
            <div className="p-4 space-y-4">
              <Skeleton active paragraph={{ rows: 2 }} />
              <Skeleton active paragraph={{ rows: 2 }} />
              <Skeleton active paragraph={{ rows: 2 }} />
            </div>
          ) : !hasQuery ? (
            <div className="py-12 text-center">
              <div className="inline-flex items-center justify-center w-12 h-12 rounded-full bg-teal-50 dark:bg-teal-900/40 text-teal-600 dark:text-teal-400 mb-3 text-xl">
                <SearchOutlined />
              </div>
              <Title level={5} className="!text-slate-700 dark:!text-white !mb-1">
                Tra cứu câu hỏi thường gặp
              </Title>
              <Paragraph type="secondary" className="text-sm max-w-md mx-auto !mb-0">
                Nhập từ khóa bệnh lý hoặc triệu chứng vào ô tìm kiếm và nhấn Enter để xem các câu hỏi giải đáp.
              </Paragraph>
            </div>
          ) : collapseItems.length > 0 ? (
            <div className="space-y-3">
              <Collapse
                items={collapseItems}
                bordered={false}
                defaultActiveKey={collapseItems.length > 0 ? [collapseItems[0].key] : []}
                className="bg-transparent"
                expandIconPosition="end"
              />
              {totalMatches > MAX_RESULTS && (
                <div className="mt-5 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-sm text-slate-600 dark:text-slate-300 px-4 py-3 sm:py-3.5 bg-slate-50/80 dark:bg-slate-800/60 rounded-xl border border-slate-200/70 dark:border-slate-700/60 shadow-xs">
                  <span>
                    Hiển thị <strong>{filteredFaqs.length}</strong> trên tổng số <strong>{totalMatches}</strong> câu hỏi phù hợp nhất.
                  </span>
                  <span className="text-teal-600 dark:text-teal-400 font-medium text-xs sm:text-sm">
                    Thử thêm từ khóa chi tiết hơn để có kết quả chính xác.
                  </span>
                </div>
              )}
            </div>
          ) : (
            <div className="py-12">
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <div className="space-y-1">
                    <Text className="text-slate-600 dark:text-white font-medium">
                      Không tìm thấy câu hỏi phù hợp với "{activeSearch}"
                    </Text>
                    <p className="text-xs text-slate-400 dark:text-slate-400">
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
          <Card className="rounded-xl border-teal-100 bg-teal-50/50 hover:bg-teal-50 transition-colors dark:border-teal-800 dark:bg-teal-900/30 dark:hover:bg-teal-900/50">
            <div className="flex items-start gap-3">
              <div className="rounded-lg bg-teal-600 p-2.5 text-white">
                <CommentOutlined className="text-lg" />
              </div>
              <div className="space-y-1">
                <h4 className="font-semibold text-teal-950 text-sm dark:text-white">
                  Vẫn còn thắc mắc về triệu chứng?
                </h4>
                <p className="text-xs text-teal-800/80 dark:text-teal-200/80">
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

          <Card className="rounded-xl border-slate-200 bg-white hover:bg-slate-50/50 transition-colors dark:border-white/10 dark:bg-neutral-900 dark:hover:bg-neutral-800/50">
            <div className="flex items-start gap-3">
              <div className="rounded-lg bg-slate-800 p-2.5 text-white">
                <BookOutlined className="text-lg" />
              </div>
              <div className="space-y-1">
                <h4 className="font-semibold text-slate-900 dark:text-white text-sm dark:text-white">
                  Đọc bài viết kiến thức chuyên sâu
                </h4>
                <p className="text-xs text-slate-500 dark:text-white">
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
