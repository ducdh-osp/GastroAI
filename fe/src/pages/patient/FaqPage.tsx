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

  const groupedFaqs = useMemo(() => {
    const q = activeSearch.toLowerCase().trim()
    const matched = q
      ? faqs.filter(
          (faq) =>
            faq.question.toLowerCase().includes(q) ||
            faq.answer.toLowerCase().includes(q) ||
            faq.category.toLowerCase().includes(q)
        )
      : faqs

    const groups = new Map<string, FaqItem[]>()
    for (const faq of matched) {
      const cat = faq.category?.trim() || 'Câu hỏi chung'
      const list = groups.get(cat) ?? []
      list.push(faq)
      groups.set(cat, list)
    }
    return Array.from(groups.entries()).map(([category, items]) => ({
      category,
      items: items.map((faq) => ({
        key: String(faq.id),
        label: (
          <span className="font-semibold text-slate-800 dark:text-white text-base py-0.5 block">
            {faq.question}
          </span>
        ),
        children: (
          <div className="pt-2 pb-4 text-slate-700 dark:text-white leading-relaxed border-t border-slate-100 dark:border-white/10">
            <MarkdownContent content={faq.answer} />
          </div>
        ),
      })),
    }))
  }, [faqs, activeSearch])

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
            Giải đáp nhanh về triệu chứng, dinh dưỡng và dấu hiệu cảnh báo. Đây là nội dung tham khảo, không phải thẩm định của bác sĩ hành nghề.
          </Paragraph>

          {/* Search bar inside banner */}
          <div className="max-w-xl">
            <Input
              size="large"
              placeholder="Tìm câu hỏi của bạn (VD: trào ngược, thang Bristol, dạ dày...)"
              prefix={<SearchOutlined className="text-teal-600 text-lg mr-1 cursor-pointer" onClick={handleSearch} />}
              value={inputSearch}
              onChange={(e) => {
                const nextVal = e.target.value
                setInputSearch(nextVal)
                setActiveSearch(nextVal.trim())
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

        {/* Main Content: Grouped Accordions */}
        <Card className="rounded-xl shadow-xs border-slate-200 dark:border-slate-800 dark:bg-slate-900/40" styles={{ body: { padding: 16 } }}>
          {loading ? (
            <div className="p-4 space-y-4">
              <Skeleton active paragraph={{ rows: 2 }} />
              <Skeleton active paragraph={{ rows: 2 }} />
              <Skeleton active paragraph={{ rows: 2 }} />
            </div>
          ) : groupedFaqs.length > 0 ? (
            <div className="space-y-6">
              {groupedFaqs.map((group, groupIdx) => (
                <section key={group.category} className="space-y-2">
                  <div className="flex items-center gap-2 px-1">
                    <Title level={5} className="!mb-0 !text-teal-800 dark:!text-teal-300">
                      {group.category}
                    </Title>
                    <Tag color="cyan" className="m-0 text-xs">
                      {group.items.length}
                    </Tag>
                  </div>
                  <Collapse
                    items={group.items}
                    bordered={false}
                    defaultActiveKey={hasQuery && groupIdx === 0 && group.items.length > 0 ? [group.items[0].key] : []}
                    className="bg-transparent"
                    expandIconPosition="end"
                  />
                </section>
              ))}
            </div>
          ) : (
            <div className="py-12">
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description={
                  <div className="space-y-1">
                    <Text className="text-slate-600 dark:text-white font-medium">
                      {hasQuery
                        ? `Không tìm thấy câu hỏi phù hợp với "${activeSearch}"`
                        : 'Chưa có câu hỏi thường gặp nào được công khai.'}
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
                  Không tìm thấy câu trả lời?
                </h4>
                <p className="text-xs text-teal-800/80 dark:text-teal-200/80">
                  Trò chuyện riêng tư với Trợ lý AI GastroAI để được giải đáp theo tình trạng tiêu hóa của riêng bạn.
                </p>
                <div className="pt-2">
                  <Link to="/chat" state={hasQuery ? { initialQuestion: activeSearch } : undefined}>
                    <Button
                      type="primary"
                      size="small"
                      className="bg-teal-600 hover:bg-teal-700"
                    >
                      Hỏi AI trực tiếp
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
