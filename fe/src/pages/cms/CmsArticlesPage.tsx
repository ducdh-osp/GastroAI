import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeInvisibleOutlined,
  PlusOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Empty,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  deleteCmsArticle,
  extractArticleError,
  listCmsArticles,
  publishCmsArticle,
  submitCmsArticleForReview,
  type ArticleStatus,
  type CmsArticleSummary,
} from '../../api/cmsArticles'
import { listArticleTopics, type ArticleTopic } from '../../api/cmsArticleTopics'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { formatDateTime } from '../../lib/format'
import { useCmsAuth } from '../../stores/cmsAuthStore'

const { Title, Text, Paragraph } = Typography

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'PUBLISHED', label: 'Đã đăng' },
  { value: 'PENDING_REVIEW', label: 'Chờ duyệt' },
  { value: 'DRAFT', label: 'Bản nháp' },
  { value: 'HIDDEN', label: 'Đang ẩn' },
]

function resolveArticleStatus(record: CmsArticleSummary): ArticleStatus {
  if (record.status) return record.status
  if (record.published) return 'PUBLISHED'
  return record.medicalReviewedAt ? 'HIDDEN' : 'DRAFT'
}

interface ArticleReviewFormValues {
  medicalReviewer: string
  reviewConfirmed: boolean
}

const ARTICLE_REVIEW_CONFIRM_RULE = {
  validator: (_: unknown, value: boolean) =>
    value
      ? Promise.resolve()
      : Promise.reject(new Error('Vui lòng xác nhận đã đối chiếu bài viết với tài liệu y khoa đã công bố')),
}

export default function CmsArticlesPage() {
  const navigate = useNavigate()
  const { user } = useCmsAuth()
  const canApprove = user?.userType !== 'CONTENT_AUTHOR'

  const [articles, setArticles] = useState<CmsArticleSummary[]>([])
  const [topics, setTopics] = useState<ArticleTopic[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Filters & paging
  const [queryInput, setQueryInput] = useState('')
  const [query, setQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string | undefined>(undefined)
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL')
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(10)
  const [totalElements, setTotalElements] = useState(0)

  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [reviewArticle, setReviewArticle] = useState<CmsArticleSummary | null>(null)
  const [reviewSubmitting, setReviewSubmitting] = useState(false)
  const [reviewForm] = Form.useForm<ArticleReviewFormValues>()
  const requestId = useRef(0)

  useEffect(() => {
    listArticleTopics()
      .then((data) => setTopics(data))
      .catch(() => {
        // Fallback im lặng nếu chưa tải được danh mục
      })
  }, [])

  const load = useCallback(() => {
    const currentRequestId = ++requestId.current
    setLoading(true)
    setError(null)

    const statusParam =
      selectedStatus !== 'ALL' ? (selectedStatus as ArticleStatus) : undefined

    listCmsArticles({
      q: query || undefined,
      category: selectedCategory || undefined,
      status: statusParam,
      page,
      size: pageSize,
    })
      .then((data) => {
        if (currentRequestId === requestId.current) {
          setArticles(data.items)
          setTotalElements(data.totalElements)
        }
      })
      .catch((err: unknown) => {
        if (currentRequestId === requestId.current) {
          setError(extractArticleError(err, 'Không thể tải danh sách bài viết.'))
        }
      })
      .finally(() => {
        if (currentRequestId === requestId.current) setLoading(false)
      })
  }, [query, selectedCategory, selectedStatus, page, pageSize])

  useEffect(() => {
    load()
  }, [load])

  function handleOpenReview(article: CmsArticleSummary) {
    reviewForm.resetFields()
    const defaultReviewer = user?.fullName
      ? `${user.fullName} (${user.email})`
      : user?.email || ''
    reviewForm.setFieldsValue({
      medicalReviewer: defaultReviewer,
      reviewConfirmed: false,
    })
    setReviewArticle(article)
  }

  async function handleReviewSubmit(values: ArticleReviewFormValues) {
    if (!reviewArticle) return
    setReviewSubmitting(true)
    try {
      await publishCmsArticle(reviewArticle.id, {
        published: true,
        medicalReviewer: values.medicalReviewer.trim(),
        version: reviewArticle.version,
      })
      message.success('Đã đối chiếu nguồn và duyệt đăng bài viết.')
      setReviewArticle(null)
      load()
    } catch (err: unknown) {
      message.error(extractArticleError(err, 'Không thể duyệt bài viết.'))
    } finally {
      setReviewSubmitting(false)
    }
  }

  async function handleSubmitForReview(article: CmsArticleSummary) {
    setTogglingId(article.id)
    try {
      await submitCmsArticleForReview(article.id)
      message.success('Đã gửi bài viết sang trạng thái Chờ duyệt.')
      load()
    } catch (err: unknown) {
      message.error(extractArticleError(err, 'Không thể gửi duyệt bài viết.'))
    } finally {
      setTogglingId(null)
    }
  }

  async function handleHide(article: CmsArticleSummary) {
    setTogglingId(article.id)
    try {
      await publishCmsArticle(article.id, {
        published: false,
        version: article.version,
      })
      message.success('Đã ẩn bài viết khỏi thư viện bệnh nhân.')
      load()
    } catch (err: unknown) {
      message.error(extractArticleError(err, 'Không thể ẩn bài viết.'))
    } finally {
      setTogglingId(null)
    }
  }

  async function handleRepublish(article: CmsArticleSummary) {
    setTogglingId(article.id)
    try {
      await publishCmsArticle(article.id, {
        published: true,
        medicalReviewer: article.medicalReviewer ?? undefined,
        version: article.version,
      })
      message.success('Đã hiển thị lại bài viết.')
      load()
    } catch (err: unknown) {
      message.error(extractArticleError(err, 'Không thể hiển thị lại bài viết.'))
    } finally {
      setTogglingId(null)
    }
  }

  async function handleDelete(article: CmsArticleSummary) {
    setDeletingId(article.id)
    try {
      await deleteCmsArticle(article.id)
      message.success('Đã xoá bài viết.')
      load()
    } catch (err: unknown) {
      message.error(extractArticleError(err, 'Không thể xoá bài viết. Vui lòng thử lại.'))
    } finally {
      setDeletingId(null)
    }
  }

  const columns: ColumnsType<CmsArticleSummary> = [
    {
      title: 'Tiêu đề bài viết',
      key: 'title',
      render: (_, record) => (
        <div className="max-w-md">
          <Link to={`/cms/articles/${record.id}/edit`} className="font-medium text-slate-800 dark:text-white hover:text-teal-600">
            {record.title}
          </Link>
          <div className="mt-0.5 text-xs text-slate-400 dark:text-white">/{record.slug}</div>
        </div>
      ),
    },
    {
      title: 'Chủ đề',
      dataIndex: 'category',
      key: 'category',
      width: 150,
      render: (cat: string) => <Tag color="cyan" className="m-0">{cat}</Tag>,
    },
    {
      title: 'Trạng thái',
      key: 'published',
      width: 220,
      render: (_, record) => {
        const status = resolveArticleStatus(record)
        return (
          <div className="flex items-center gap-2 flex-wrap">
            {status === 'PUBLISHED' ? (
              <>
                <Tag color="green" className="m-0">Đã đăng</Tag>
                {canApprove && (
                  <Popconfirm
                    title="Ẩn bài viết này?"
                    description="Bài viết sẽ tạm ẩn khỏi thư viện bệnh nhân mà không bị xóa nội dung gốc."
                    okText="Ẩn"
                    cancelText="Hủy"
                    onConfirm={() => void handleHide(record)}
                  >
                    <Button size="small" icon={<EyeInvisibleOutlined />} loading={togglingId === record.id}>
                      Ẩn
                    </Button>
                  </Popconfirm>
                )}
              </>
            ) : status === 'HIDDEN' ? (
              <>
                <Tag color="default" className="m-0">Đang ẩn</Tag>
                {canApprove && (
                  <Popconfirm
                    title="Hiển thị lại bài viết?"
                    description={`Nội dung đã được ${record.medicalReviewer} đối chiếu nguồn.`}
                    okText="Hiển thị"
                    cancelText="Hủy"
                    onConfirm={() => void handleRepublish(record)}
                  >
                    <Button size="small" type="primary" ghost icon={<CheckCircleOutlined />} loading={togglingId === record.id}>
                      Hiển thị lại
                    </Button>
                  </Popconfirm>
                )}
              </>
            ) : status === 'DRAFT' ? (
              <>
                <Tag color="blue" className="m-0">Bản nháp</Tag>
                <Button
                  size="small"
                  type="primary"
                  ghost
                  loading={togglingId === record.id}
                  onClick={() => void handleSubmitForReview(record)}
                >
                  Gửi duyệt
                </Button>
              </>
            ) : (
              <>
                <Tag color="orange" className="m-0">Chờ duyệt</Tag>
                {canApprove && (
                  <Tooltip title="Biên tập chuyên môn y khoa đối chiếu nguồn y khoa công bố rồi duyệt đăng">
                    <Button size="small" type="primary" ghost icon={<SafetyCertificateOutlined />} onClick={() => handleOpenReview(record)}>
                      Duyệt
                    </Button>
                  </Tooltip>
                )}
              </>
            )}
          </div>
        )
      },
    },
    {
      title: 'Đối chiếu nguồn',
      key: 'medicalReviewer',
      width: 190,
      render: (_, record) =>
        record.medicalReviewer && record.medicalReviewedAt ? (
          <div>
            <div className="text-xs font-medium text-slate-700 dark:text-white">{record.medicalReviewer}</div>
            <div className="text-[11px] text-slate-400 dark:text-white">{formatDateTime(record.medicalReviewedAt)}</div>
          </div>
        ) : (
          <span className="text-xs text-slate-400 dark:text-white">Chưa đối chiếu nguồn</span>
        ),
    },
    {
      title: 'Cập nhật',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 150,
      render: (val: string) => <span className="text-xs text-slate-500 dark:text-white">{formatDateTime(val)}</span>,
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 110,
      render: (_, record) => (
        <div className="flex gap-1">
          <Tooltip title="Chỉnh sửa">
            <Button
              type="text"
              icon={<EditOutlined />}
              onClick={() => navigate(`/cms/articles/${record.id}/edit`)}
              aria-label={`Sửa bài viết ${record.title}`}
            />
          </Tooltip>
          <Popconfirm
            title="Xoá bài viết này?"
            description="Bài viết sẽ bị xoá vĩnh viễn khỏi hệ thống. Thao tác này không thể hoàn tác."
            okText="Xoá"
            cancelText="Huỷ"
            okButtonProps={{ danger: true }}
            onConfirm={() => void handleDelete(record)}
          >
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              loading={deletingId === record.id}
              aria-label={`Xoá bài viết ${record.title}`}
            />
          </Popconfirm>
        </div>
      ),
    },
  ]

  return (
    <CmsAppShell>
      <div className="w-full">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <Text type="secondary">Kho tri thức</Text>
            <Title level={2} className="mb-1! mt-1!">Bài viết kiến thức tiêu hóa</Title>
            <Text type="secondary">
              Soạn thảo và đối chiếu nguồn y khoa đã công bố cho các bài viết giáo dục sức khỏe tiêu hóa hiển thị tới bệnh nhân.
            </Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/cms/articles/new')}>
            Viết bài mới
          </Button>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4!" />}

        <div className="flex flex-col gap-4">
        <Card className="rounded-2xl border-black/5 shadow-sm dark:border-neutral-700! dark:bg-neutral-800!">
          <div className="flex flex-wrap items-center gap-3">
            <Input.Search
              placeholder="Tìm theo tiêu đề bài viết"
              value={queryInput}
              onChange={(e) => {
                setQueryInput(e.target.value)
                if (!e.target.value) {
                  setPage(0)
                  setQuery('')
                }
              }}
              onSearch={(val) => {
                setPage(0)
                setQuery(val.trim())
              }}
              allowClear
              className="max-w-xs"
              enterButton={<SearchOutlined />}
            />
            <Select
              allowClear
              placeholder="Tất cả chủ đề"
              value={selectedCategory || undefined}
              onChange={(val) => {
                setPage(0)
                setSelectedCategory(val || undefined)
              }}
              options={[
                { value: '', label: 'Tất cả chủ đề' },
                ...topics.map((t) => ({ value: t.name, label: t.name })),
              ]}
              className="w-48"
            />
            <Select
              value={selectedStatus}
              onChange={(val) => {
                setPage(0)
                setSelectedStatus(val)
              }}
              options={STATUS_OPTIONS}
              className="w-44"
            />
            <Button
              icon={<CloseCircleOutlined />}
              onClick={() => {
                setQueryInput('')
                setQuery('')
                setSelectedCategory(undefined)
                setSelectedStatus('ALL')
                setPage(0)
              }}
              disabled={!query && !queryInput && !selectedCategory && selectedStatus === 'ALL'}
            >
              Bỏ chọn
            </Button>
          </div>
        </Card>

        <Card className="rounded-2xl border-black/5 shadow-sm dark:border-neutral-700! dark:bg-neutral-800!">
          <Table<CmsArticleSummary>
            columns={columns}
            dataSource={articles}
            rowKey="id"
            loading={loading}
            pagination={{
              current: page + 1,
              pageSize,
              total: totalElements,
              showSizeChanger: true,
              pageSizeOptions: ['10', '20', '50'],
              onChange: (p, ps) => {
                setPage(p - 1)
                setPageSize(ps)
              },
            }}
            locale={{
              emptyText: <Empty description="Chưa có bài viết nào phù hợp" />,
            }}
          />
        </Card>
        </div>

        <Modal
          title={
            <div className="flex items-center gap-2 text-teal-700">
              <SafetyCertificateOutlined />
              <span>Đối chiếu nguồn y khoa & Duyệt bài viết</span>
            </div>
          }
          open={Boolean(reviewArticle)}
          onCancel={() => setReviewArticle(null)}
          footer={null}
          width={560}
          destroyOnHidden
        >
          {reviewArticle && (
            <Form<ArticleReviewFormValues> form={reviewForm} layout="vertical" onFinish={handleReviewSubmit} className="mt-2">
              <Alert
                type="warning"
                showIcon
                className="mb-4!"
                message="Nội dung sẽ hiển thị tới bệnh nhân trong Thư viện kiến thức"
                description="Chỉ duyệt đăng khi Biên tập chuyên môn y khoa đã đối chiếu nội dung bài viết với tài liệu y khoa đã công bố. Đây không phải thẩm định của bác sĩ hành nghề."
              />
              <Paragraph className="!mb-1 font-semibold text-slate-800 dark:text-white">{reviewArticle.title}</Paragraph>
              <div className="mb-4 flex items-center gap-2">
                <Tag color="cyan">{reviewArticle.category}</Tag>
                <Link to={`/cms/articles/${reviewArticle.id}/edit`} className="text-xs text-teal-700 underline">
                  Mở xem/chỉnh sửa toàn văn
                </Link>
              </div>

              <Form.Item
                name="medicalReviewer"
                label="Người đối chiếu (Tài khoản CMS đang đăng nhập)"
                rules={[
                  { required: true, whitespace: true, message: 'Vui lòng nhập tên người đối chiếu nguồn' },
                  { max: 200, message: 'Tối đa 200 ký tự' },
                ]}
              >
                <Input disabled maxLength={200} />
              </Form.Item>

              <Form.Item name="reviewConfirmed" valuePropName="checked" rules={[ARTICLE_REVIEW_CONFIRM_RULE]}>
                <Checkbox>Tôi xác nhận đã đối chiếu nội dung bài viết với tài liệu y khoa đã công bố</Checkbox>
              </Form.Item>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100 dark:border-slate-800">
                <Button onClick={() => setReviewArticle(null)}>Hủy</Button>
                <Button type="primary" htmlType="submit" loading={reviewSubmitting}>
                  Duyệt đăng bài
                </Button>
              </div>
            </Form>
          )}
        </Modal>
      </div>
    </CmsAppShell>
  )
}
