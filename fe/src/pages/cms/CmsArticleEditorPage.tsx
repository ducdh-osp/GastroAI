import { ArrowLeftOutlined, EyeOutlined, FormOutlined, SaveOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Row,
  Select,
  Spin,
  Switch,
  Tabs,
  Typography,
  message,
} from 'antd'
import dayjs from 'dayjs'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  createCmsArticle,
  extractArticleError,
  getCmsArticle,
  updateCmsArticle,
  type CmsArticleInput,
} from '../../api/cmsArticles'
import { listArticleTopics, type ArticleTopic } from '../../api/cmsArticleTopics'
import { MarkdownContent } from '../../components/chat/MarkdownContent'
import { CmsAppShell } from '../../components/cms/CmsAppShell'

const { Title, Text } = Typography

interface FormValues {
  title: string
  slug?: string
  category: string
  summary: string
  readingMinutes?: number
  content: string
  sourceName: string
  sourceUrl: string
  sourceCheckedAt?: dayjs.Dayjs
  medicalReviewer?: string
  published: boolean
  sortOrder?: number
}

export default function CmsArticleEditorPage() {
  const { id } = useParams<{ id?: string }>()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  const [form] = Form.useForm<FormValues>()
  const [topics, setTopics] = useState<ArticleTopic[]>([])
  const [articleVersion, setArticleVersion] = useState<number | undefined>(undefined)
  const [loading, setLoading] = useState(isEdit)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [contentWatch, setContentWatch] = useState('')

  useEffect(() => {
    listArticleTopics()
      .then((data) => setTopics(data))
      .catch(() => {
        // Fallback
      })
  }, [])

  useEffect(() => {
    if (!id) {
      // Default values cho bài viết mới
      form.setFieldsValue({
        published: false,
        sourceCheckedAt: dayjs(),
        medicalReviewer: undefined,
        readingMinutes: undefined,
      })
      return
    }

    setLoading(true)
    setError(null)
    getCmsArticle(Number(id))
      .then((article) => {
        setArticleVersion(article.version)
        form.setFieldsValue({
          title: article.title,
          slug: article.slug,
          category: article.category,
          summary: article.summary,
          readingMinutes: article.readingMinutes,
          content: article.content,
          sourceName: article.sourceName,
          sourceUrl: article.sourceUrl,
          sourceCheckedAt: article.sourceCheckedAt ? dayjs(article.sourceCheckedAt) : undefined,
          medicalReviewer: article.medicalReviewer || undefined,
          published: article.published,
          sortOrder: article.sortOrder,
        })
        setContentWatch(article.content || '')
      })
      .catch((err: unknown) => {
        setError(extractArticleError(err, 'Không thể tải thông tin bài viết.'))
      })
      .finally(() => setLoading(false))
  }, [id, form])

  // Tự động tính thời gian đọc khi người dùng nhập nội dung nếu chưa nhập số thủ công
  function handleContentChange(val: string) {
    setContentWatch(val)
    const currentCustomMinutes = form.getFieldValue('readingMinutes')
    if (!currentCustomMinutes && val.trim().length > 0) {
      const words = val.trim().split(/\s+/).length
      const est = Math.max(1, Math.min(30, Math.ceil(words / 200)))
      form.setFieldsValue({ readingMinutes: est })
    }
  }

  async function handleSubmit(values: FormValues) {
    setSaving(true)
    setError(null)
    try {
      const payload: CmsArticleInput = {
        title: values.title.trim(),
        slug: values.slug?.trim() || undefined,
        summary: values.summary.trim(),
        category: values.category,
        readingMinutes: values.readingMinutes,
        content: values.content.trim(),
        sourceName: values.sourceName.trim(),
        sourceUrl: values.sourceUrl.trim(),
        sourceCheckedAt: values.sourceCheckedAt ? values.sourceCheckedAt.format('YYYY-MM-DD') : undefined,
        medicalReviewer: values.medicalReviewer?.trim() || undefined,
        published: values.published,
        sortOrder: values.sortOrder,
        version: isEdit ? articleVersion : undefined,
      }

      if (isEdit && id) {
        await updateCmsArticle(Number(id), payload)
        message.success('Đã cập nhật bài viết thành công.')
      } else {
        await createCmsArticle(payload)
        message.success('Đã tạo bài viết mới thành công.')
      }
      navigate('/cms/articles')
    } catch (err: unknown) {
      const msg = extractArticleError(err, 'Không thể lưu bài viết. Vui lòng thử lại.')
      setError(msg)
      message.error(msg)
    } finally {
      setSaving(false)
    }
  }

  return (
    <CmsAppShell>
      <div className="mx-auto max-w-5xl">
        <div className="mb-4 flex items-center justify-between">
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/cms/articles')}>
            Quay lại danh sách bài viết
          </Button>
          <div className="flex gap-2">
            <Button onClick={() => navigate('/cms/articles')}>Huỷ</Button>
            <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={() => form.submit()}>
              {isEdit ? 'Lưu thay đổi' : 'Tạo bài viết'}
            </Button>
          </div>
        </div>

        <div className="mb-6">
          <Text type="secondary">Kho tri thức &gt; Quản lý bài viết</Text>
          <Title level={2} className="mb-1! mt-1!">
            {isEdit ? 'Chỉnh sửa bài viết' : 'Biên soạn bài viết mới'}
          </Title>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        {loading ? (
          <div className="py-24 text-center">
            <Spin size="large" />
          </div>
        ) : (
          <Form<FormValues> form={form} layout="vertical" onFinish={handleSubmit}>
            <Row gutter={24}>
              {/* Cột trái: Thông tin chính & nội dung */}
              <Col xs={24} lg={16}>
                <Card className="mb-6 rounded-2xl border-black/5 shadow-sm" title="Thông tin cơ bản">
                  <Form.Item
                    name="title"
                    label="Tiêu đề bài viết"
                    rules={[
                      { required: true, whitespace: true, message: 'Vui lòng nhập tiêu đề bài viết' },
                      { max: 200, message: 'Tối đa 200 ký tự' },
                    ]}
                  >
                    <Input placeholder="Ví dụ: Trào ngược dạ dày thực quản: Hiểu đúng về triệu chứng" maxLength={200} />
                  </Form.Item>

                  <Row gutter={16}>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="slug"
                        label="Đường dẫn tĩnh (Slug)"
                        extra="Tự động sinh từ tiêu đề nếu để trống. Ví dụ: trao-nguoc-da-day"
                      >
                        <Input placeholder="tự-động-sinh-nếu-để-trống" maxLength={120} />
                      </Form.Item>
                    </Col>
                    <Col xs={24} sm={12}>
                      <Form.Item
                        name="category"
                        label="Chủ đề bài viết"
                        rules={[{ required: true, message: 'Vui lòng chọn chủ đề bài viết' }]}
                      >
                        <Select
                          placeholder="Chọn chủ đề"
                          options={topics.map((t) => ({ value: t.name, label: t.name }))}
                        />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Form.Item
                    name="summary"
                    label="Tóm tắt ngắn (Summary)"
                    extra="Hiển thị trên thẻ danh mục và kết quả tìm kiếm của bệnh nhân."
                    rules={[
                      { required: true, whitespace: true, message: 'Vui lòng nhập tóm tắt bài viết' },
                      { max: 500, message: 'Tối đa 500 ký tự' },
                    ]}
                  >
                    <Input.TextArea rows={3} maxLength={500} placeholder="Tóm tắt 2-3 câu về nội dung bài viết..." />
                  </Form.Item>
                </Card>

                <Card className="mb-6 rounded-2xl border-black/5 shadow-sm" title="Nội dung chi tiết (Markdown)">
                  <Tabs
                    defaultActiveKey="edit"
                    items={[
                      {
                        key: 'edit',
                        label: (
                          <span>
                            <FormOutlined className="mr-1" />
                            Soạn thảo
                          </span>
                        ),
                        children: (
                          <Form.Item
                            name="content"
                            rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập nội dung bài viết' }]}
                            className="mb-0"
                          >
                            <Input.TextArea
                              rows={18}
                              className="font-mono text-sm leading-6"
                              placeholder="# Tiêu đề chính&#10;&#10;Nội dung đoạn văn...&#10;&#10;## Phụ đề 1&#10;- Ý 1&#10;- Ý 2"
                              onChange={(e) => handleContentChange(e.target.value)}
                            />
                          </Form.Item>
                        ),
                      },
                      {
                        key: 'preview',
                        label: (
                          <span>
                            <EyeOutlined className="mr-1" />
                            Xem trước
                          </span>
                        ),
                        children: (
                          <div className="min-h-[400px] rounded-xl border border-slate-200 bg-white p-6 dark:border-white/10 dark:bg-neutral-900">
                            {contentWatch.trim() ? (
                              <MarkdownContent content={contentWatch} />
                            ) : (
                              <div className="py-20 text-center text-slate-400 dark:text-white">Chưa có nội dung để xem trước</div>
                            )}
                          </div>
                        ),
                      },
                    ]}
                  />
                </Card>
              </Col>

              {/* Cột phải: Cài đặt xuất bản & nguồn */}
              <Col xs={24} lg={8}>
                <Card className="mb-6 rounded-2xl border-black/5 shadow-sm" title="Xuất bản & Kiểm duyệt">
                  {isEdit && (
                    <Alert
                      type="info"
                      showIcon
                      className="mb-4 text-xs"
                      message="Quy định kiểm duyệt y khoa"
                      description="Khi sửa nội dung bài viết đã công khai mà không có thông tin duyệt y khoa mới, bài viết sẽ tự động chuyển về Bản nháp để yêu cầu kiểm duyệt lại."
                    />
                  )}

                  <Form.Item name="published" label="Trạng thái xuất bản" valuePropName="checked">
                    <Switch checkedChildren="Công khai" unCheckedChildren="Bản nháp" />
                  </Form.Item>

                  <Form.Item
                    noStyle
                    shouldUpdate={(prev, curr) => prev.published !== curr.published}
                  >
                    {({ getFieldValue }) => {
                      const isPublished = Boolean(getFieldValue('published'))
                      return (
                        <Form.Item
                          name="medicalReviewer"
                          label="Bác sĩ duyệt y khoa"
                          extra={
                            isPublished
                              ? 'Bắt buộc khi xuất bản công khai. Người duyệt phải độc lập với người biên soạn.'
                              : 'Tên bác sĩ hoặc chuyên gia phụ trách duyệt nội dung bài viết này.'
                          }
                          rules={[
                            {
                              required: isPublished,
                              whitespace: true,
                              message: 'Vui lòng nhập tên bác sĩ duyệt y khoa trước khi công khai bài viết',
                            },
                            { max: 200, message: 'Tối đa 200 ký tự' },
                          ]}
                        >
                          <Input placeholder="Ví dụ: BS. CKII Nguyễn Văn A" maxLength={200} />
                        </Form.Item>
                      )
                    }}
                  </Form.Item>

                  <Form.Item
                    name="readingMinutes"
                    label="Thời lượng đọc (phút)"
                    extra="Ước tính tự động dựa trên số từ nếu để trống."
                  >
                    <InputNumber min={1} max={30} className="w-full" placeholder="1 - 30" />
                  </Form.Item>

                  <Form.Item name="sortOrder" label="Thứ tự hiển thị" extra="Số nhỏ hơn sẽ hiển thị trước.">
                    <InputNumber min={0} className="w-full" placeholder="0" />
                  </Form.Item>
                </Card>

                <Card className="mb-6 rounded-2xl border-black/5 shadow-sm" title="Nguồn tài liệu y khoa">
                  <Form.Item
                    name="sourceName"
                    label="Tên tổ chức / nguồn tài liệu"
                    rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập tên nguồn' }]}
                  >
                    <Input placeholder="Ví dụ: NIDDK, Viện Dinh Dưỡng, Bệnh viện Bạch Mai" maxLength={200} />
                  </Form.Item>

                  <Form.Item
                    name="sourceUrl"
                    label="Đường dẫn nguồn (URL)"
                    rules={[
                      { required: true, whitespace: true, message: 'Vui lòng nhập đường dẫn nguồn' },
                      { type: 'url', message: 'Vui lòng nhập URL hợp lệ' },
                    ]}
                  >
                    <Input placeholder="https://..." maxLength={1000} />
                  </Form.Item>

                  <Form.Item name="sourceCheckedAt" label="Ngày kiểm tra nguồn">
                    <DatePicker
                      className="w-full"
                      format="DD/MM/YYYY"
                      placeholder="Chọn ngày"
                      disabledDate={(current) => Boolean(current && current > dayjs().endOf('day'))}
                    />
                  </Form.Item>
                </Card>
              </Col>
            </Row>
          </Form>
        )}
      </div>
    </CmsAppShell>
  )
}
