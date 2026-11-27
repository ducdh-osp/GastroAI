import { ArrowLeftOutlined, EyeOutlined, FormOutlined, PictureOutlined, SaveOutlined, SendOutlined } from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Radio,
  Row,
  Select,
  Spin,
  Typography,
  Upload,
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
import { RichTextEditor } from '../../components/editor/RichTextEditor'

const { Title, Text } = Typography

interface FormValues {
  title: string
  slug?: string
  category: string
  summary: string
  content: string
  sourceName: string
  sourceUrl: string
  sourceCheckedAt?: dayjs.Dayjs
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
  const [submitMode, setSubmitMode] = useState<'draft' | 'review'>('review')
  const [error, setError] = useState<string | null>(null)
  const [editorMode, setEditorMode] = useState<'edit' | 'preview'>('edit')
  const watchedContent = Form.useWatch('content', form) || ''

  useEffect(() => {
    listArticleTopics()
      .then((data) => setTopics(data))
      .catch(() => { })
  }, [])

  useEffect(() => {
    if (!id) {
      form.setFieldsValue({
        sourceCheckedAt: dayjs(),
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
          content: article.content,
          sourceName: article.sourceName,
          sourceUrl: article.sourceUrl,
          sourceCheckedAt: article.sourceCheckedAt ? dayjs(article.sourceCheckedAt) : undefined,
          sortOrder: article.sortOrder,
        })
      })
      .catch((err: unknown) => {
        setError(extractArticleError(err, 'Không thể tải thông tin bài viết.'))
      })
      .finally(() => setLoading(false))
  }, [id, form])

  function handleIllustrationUpload(file: File) {
    if (!file.type.startsWith('image/')) {
      message.error('Vui lòng chọn tệp hình ảnh hợp lệ.')
      return false
    }
    if (file.size > 2 * 1024 * 1024) {
      message.error('Ảnh minh họa tối đa 2MB.')
      return false
    }
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = typeof reader.result === 'string' ? reader.result : ''
      if (!dataUrl) return
      const current = form.getFieldValue('content') || ''
      const markdownImage = `![${file.name.replace(/\.[^.]+$/, '')}](${dataUrl})\n\n`
      const next = markdownImage + current
      form.setFieldsValue({ content: next })
      message.success('Đã chèn ảnh minh họa vào đầu bài viết.')
    }
    reader.readAsDataURL(file)
    return false
  }

  function triggerSubmit(mode: 'draft' | 'review') {
    setSubmitMode(mode)
    form.submit()
  }

  async function handleSubmit(values: FormValues) {
    setSaving(true)
    setError(null)
    try {
      // Theo SCR-AD-11: Lưu nháp hoặc Gửi duyệt đều KHÔNG tự động công khai ngay;
      // Biên tập chuyên môn y khoa phải đối chiếu nguồn và bấm Duyệt riêng ở danh sách.
      const payload: CmsArticleInput = {
        title: values.title.trim(),
        slug: values.slug?.trim() || undefined,
        summary: values.summary.trim(),
        category: values.category,
        content: values.content.trim(),
        sourceName: values.sourceName.trim(),
        sourceUrl: values.sourceUrl.trim(),
        sourceCheckedAt: values.sourceCheckedAt ? values.sourceCheckedAt.format('YYYY-MM-DD') : undefined,
        medicalReviewer: undefined,
        published: false,
        status: submitMode === 'review' ? 'PENDING_REVIEW' : 'DRAFT',
        sortOrder: values.sortOrder,
        version: isEdit ? articleVersion : undefined,
      }

      if (isEdit && id) {
        await updateCmsArticle(Number(id), payload)
      } else {
        await createCmsArticle(payload)
      }
      if (submitMode === 'review') {
        message.success('Đã gửi bài viết sang trạng thái Chờ duyệt. Biên tập chuyên môn y khoa cần đối chiếu nguồn và bấm Duyệt trong danh sách.')
      } else {
        message.success('Đã lưu bản nháp bài viết.')
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
      <div className="w-full">
        <div className="mb-4 flex items-center justify-between">
          <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/cms/articles')}>
            Quay lại danh sách bài viết
          </Button>
          <div className="flex gap-2">
            <Button onClick={() => navigate('/cms/articles')} disabled={saving}>Huỷ</Button>
            <Button icon={<SaveOutlined />} loading={saving && submitMode === 'draft'} disabled={saving} onClick={() => triggerSubmit('draft')}>
              Lưu nháp
            </Button>
            <Button type="primary" icon={<SendOutlined />} loading={saving && submitMode === 'review'} disabled={saving} onClick={() => triggerSubmit('review')}>
              Gửi duyệt
            </Button>
          </div>
        </div>

        <div className="mb-6">
          <Text type="secondary">Kho tri thức &gt; Quản lý bài viết</Text>
          <Title level={2} className="mb-1! mt-1!">
            {isEdit ? 'Chỉnh sửa bài viết' : 'Biên soạn bài viết mới'}
          </Title>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-5!" />}

        {loading ? (
          <div className="py-24 text-center">
            <Spin size="large" />
          </div>
        ) : (
          <Form<FormValues> form={form} layout="vertical" onFinish={handleSubmit}>
            <Row gutter={[24, 24]}>
              {/* Cột trái: Thông tin chính & nội dung */}
              <Col xs={24} lg={16}>
                <div className="flex flex-col gap-6">
                  <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!" title="Thông tin cơ bản">
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

                  <Form.Item
                    label="Ảnh minh họa (khuyến khích, không bắt buộc)"
                    extra="Tải ảnh minh họa để chèn trực tiếp vào nội dung bài viết."
                  >
                    <Upload
                      accept="image/*"
                      showUploadList={false}
                      beforeUpload={handleIllustrationUpload}
                    >
                      <Button icon={<PictureOutlined />}>Tải ảnh lên</Button>
                    </Upload>
                  </Form.Item>
                </Card>

                <Card
                  className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!"
                  title="Nội dung chi tiết bài viết"
                  extra={
                    <Radio.Group
                      size="small"
                      value={editorMode}
                      onChange={(e) => setEditorMode(e.target.value)}
                    >
                      <Radio.Button value="edit">
                        <FormOutlined className="mr-1" />
                        Soạn thảo
                      </Radio.Button>
                      <Radio.Button value="preview">
                        <EyeOutlined className="mr-1" />
                        Xem trước
                      </Radio.Button>
                    </Radio.Group>
                  }
                >
                  <div className={editorMode === 'edit' ? '' : 'hidden'}>
                    <Form.Item
                      name="content"
                      rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập nội dung bài viết' }]}
                      className="mb-0"
                    >
                      <RichTextEditor
                        enableHeadings
                        enableImages
                        minHeight={360}
                        placeholder="Nhập nội dung bài viết, chọn tiêu đề hoặc định dạng chữ trực tiếp trên thanh công cụ..."
                      />
                    </Form.Item>
                  </div>
                  {editorMode === 'preview' && (
                    <div className="min-h-[400px] rounded-xl border border-slate-200 bg-white p-6 dark:border-white/10 dark:bg-neutral-900">
                      {watchedContent.trim() ? (
                        <MarkdownContent content={watchedContent} />
                      ) : (
                        <div className="py-20 text-center text-slate-400 dark:text-white">Chưa có nội dung để xem trước</div>
                      )}
                    </div>
                  )}
                </Card>
                </div>
              </Col>

              {/* Cột phải: Quy trình duyệt & nguồn */}
              <Col xs={24} lg={8}>
                <div className="flex flex-col gap-6">
                  <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!" title="Quy trình duyệt & hiển thị">
                    <div className="mb-5">
                      <Alert
                        type="info"
                        showIcon
                        className="text-xs"
                        message="Tách biệt soạn thảo và duyệt đăng"
                        description="Lưu nháp sẽ giữ bài viết ở trạng thái Bản nháp; Gửi duyệt sẽ chuyển sang trạng thái Chờ duyệt để Biên tập chuyên môn y khoa đối chiếu nguồn và bấm Duyệt riêng ở danh sách bài viết."
                      />
                    </div>

                    <Form.Item name="sortOrder" label="Thứ tự hiển thị" extra="Số nhỏ hơn sẽ hiển thị trước." className="mb-0!">
                      <InputNumber min={0} className="w-full" placeholder="0" />
                    </Form.Item>
                  </Card>

                  <Card className="rounded-2xl border-black/5 shadow-sm dark:border-white/10! dark:bg-neutral-900!" title="Nguồn tài liệu y khoa">
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
                </div>
              </Col>
            </Row>
          </Form>
        )}
      </div>
    </CmsAppShell>
  )
}
