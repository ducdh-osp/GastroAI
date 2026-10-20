import { CheckCircleOutlined, CloseCircleOutlined, DeleteOutlined, EditOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Empty, Input, Popconfirm, Select, Switch, Table, Tag, Tooltip, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  deleteCmsArticle,
  extractArticleError,
  listCmsArticles,
  publishCmsArticle,
  type CmsArticleSummary,
} from '../../api/cmsArticles'
import { listArticleTopics, type ArticleTopic } from '../../api/cmsArticleTopics'
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { formatDateTime } from '../../lib/format'

const { Title, Text } = Typography

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'PUBLISHED', label: 'Đã xuất bản' },
  { value: 'DRAFT', label: 'Bản nháp' },
]

export default function CmsArticlesPage() {
  const navigate = useNavigate()

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

    const publishedParam =
      selectedStatus === 'PUBLISHED' ? true : selectedStatus === 'DRAFT' ? false : undefined

    listCmsArticles({
      q: query || undefined,
      category: selectedCategory || undefined,
      published: publishedParam,
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

  async function handleTogglePublish(article: CmsArticleSummary) {
    const nextPublished = !article.published
    if (nextPublished && !article.medicalReviewer?.trim()) {
      message.warning('Bài viết chưa có bác sĩ duyệt y khoa độc lập. Vui lòng mở chỉnh sửa để hoàn tất kiểm duyệt trước khi xuất bản.')
      return
    }
    setTogglingId(article.id)
    try {
      await publishCmsArticle(article.id, {
        published: nextPublished,
        medicalReviewer: nextPublished ? article.medicalReviewer : undefined,
        version: article.version,
      })
      message.success(nextPublished ? 'Đã xuất bản bài viết.' : 'Đã chuyển về bản nháp.')
      load()
    } catch (err: unknown) {
      message.error(extractArticleError(err, 'Không thể cập nhật trạng thái bài viết.'))
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
      render: (cat: string) => <Tag color="cyan">{cat}</Tag>,
    },
    {
      title: 'Trạng thái',
      key: 'published',
      width: 140,
      render: (_, record) => (
        <div className="flex items-center gap-2">
          <Switch
            checked={record.published}
            loading={togglingId === record.id}
            onChange={() => void handleTogglePublish(record)}
            size="small"
          />
          {record.published ? (
            <Tag color="green" icon={<CheckCircleOutlined />}>Đã xuất bản</Tag>
          ) : (
            <Tag color="orange" icon={<CloseCircleOutlined />}>Bản nháp</Tag>
          )}
        </div>
      ),
    },
    {
      title: 'Duyệt y khoa',
      key: 'medicalReviewer',
      width: 180,
      render: (_, record) =>
        record.medicalReviewer ? (
          <div>
            <div className="text-xs font-medium text-slate-700 dark:text-white">{record.medicalReviewer}</div>
            {record.medicalReviewedAt && (
              <div className="text-[11px] text-slate-400 dark:text-white">{formatDateTime(record.medicalReviewedAt)}</div>
            )}
          </div>
        ) : (
          <span className="text-xs text-slate-400 dark:text-white">Chưa ghi nhận</span>
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
      <div className="mx-auto max-w-6xl">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
          <div>
            <Text type="secondary">Kho tri thức</Text>
            <Title level={2} className="mb-1! mt-1!">Bài viết kiến thức tiêu hóa</Title>
            <Text type="secondary">
              Quản lý các bài viết dài giáo dục sức khỏe đường tiêu hóa cho bệnh nhân, do bác sĩ biên soạn và duyệt y khoa.
            </Text>
          </div>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/cms/articles/new')}>
            Thêm bài viết mới
          </Button>
        </div>

        {error && <Alert type="error" message={error} showIcon className="mb-4" />}

        <Card className="mb-4 rounded-2xl border-black/5 shadow-sm">
          <div className="flex flex-wrap items-center gap-3">
            <Input.Search
              placeholder="Tìm theo tiêu đề hoặc nội dung"
              value={queryInput}
              onChange={(e) => setQueryInput(e.target.value)}
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
              value={selectedCategory}
              onChange={(val) => {
                setPage(0)
                setSelectedCategory(val)
              }}
              options={topics.map((t) => ({ value: t.name, label: t.name }))}
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
          </div>
        </Card>

        <Card className="rounded-2xl border-black/5 shadow-sm">
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
    </CmsAppShell>
  )
}
