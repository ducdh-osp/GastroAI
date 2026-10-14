import {
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
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
  InputNumber,
  Modal,
  Popconfirm,
  Radio,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { useCallback, useEffect, useState } from 'react'
import {
  createCmsFaq,
  deleteCmsFaq,
  listCmsFaqCategories,
  listCmsFaqs,
  setCmsFaqPublished,
  updateCmsFaq,
  type CmsFaqFormValues,
  type CmsFaqItem,
} from '../../api/cmsFaqs'
import { listArticleTopics } from '../../api/cmsArticleTopics'
import { MarkdownContent } from '../../components/chat/MarkdownContent'

const { Title, Text, Paragraph } = Typography

type FaqFormValues = CmsFaqFormValues & { reviewConfirmed?: boolean }

interface ReviewFormValues {
  medicalReviewer: string
  reviewConfirmed: boolean
}

/** Lần duyệt y khoa còn hiệu lực cho nội dung hiện tại (BE xoá medicalReviewedAt khi nội dung đổi). */
function hasValidReview(item: CmsFaqItem | null | undefined): boolean {
  return Boolean(item?.medicalReviewer?.trim() && item?.medicalReviewedAt)
}

function extractError(err: unknown, fallback: string): string {
  const response = (err as { response?: { status?: number; data?: { message?: string } } })?.response
  if (response?.status === 409) {
    return 'Câu hỏi đã được người khác cập nhật. Danh sách đã được tải lại, vui lòng kiểm tra và thử lại.'
  }
  return response?.data?.message || fallback
}

const REVIEW_CONFIRM_RULE = {
  validator: (_: unknown, value: boolean) =>
    value
      ? Promise.resolve()
      : Promise.reject(new Error('Vui lòng xác nhận nội dung đã được bác sĩ kiểm duyệt')),
}

export default function CmsFaqPage() {
  const [items, setItems] = useState<CmsFaqItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [loading, setLoading] = useState(false)

  // Bộ lọc
  const [search, setSearch] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string | undefined>(undefined)
  const [selectedPublished, setSelectedPublished] = useState<boolean | undefined>(undefined)

  // Danh sách chủ đề có sẵn (kết hợp từ article_topics và các category đang có trong FAQ)
  const [categoryOptions, setCategoryOptions] = useState<string[]>([])

  // Modal Thêm/Sửa
  const [modalOpen, setModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<CmsFaqItem | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [previewTab, setPreviewTab] = useState<'edit' | 'preview'>('edit')
  const [form] = Form.useForm<FaqFormValues>()
  const answerValue = Form.useWatch('answer', form)
  const publishedValue = Form.useWatch('published', form)
  const reviewerValue = Form.useWatch('medicalReviewer', form)

  // Modal Xem chi tiết
  const [detailItem, setDetailItem] = useState<CmsFaqItem | null>(null)

  // Modal Kiểm duyệt & xuất bản
  const [reviewItem, setReviewItem] = useState<CmsFaqItem | null>(null)
  const [reviewSubmitting, setReviewSubmitting] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)
  const [reviewForm] = Form.useForm<ReviewFormValues>()

  // Tải danh mục gợi ý
  const loadCategories = useCallback(async () => {
    try {
      const [faqCats, topics] = await Promise.all([
        listCmsFaqCategories().catch(() => [] as string[]),
        listArticleTopics().catch(() => []),
      ])
      const combined = Array.from(
        new Set([...faqCats, ...topics.map((t) => t.name)]),
      ).filter(Boolean)
      setCategoryOptions(combined)
    } catch {
      // ignore
    }
  }, [])

  const loadData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await listCmsFaqs({
        q: search.trim() || undefined,
        category: selectedCategory,
        published: selectedPublished,
        page,
        size: pageSize,
      })
      setItems(res.items)
      setTotal(res.total)
    } catch {
      message.error('Không thể tải danh sách câu hỏi thường gặp')
    } finally {
      setLoading(false)
    }
  }, [search, selectedCategory, selectedPublished, page, pageSize])

  useEffect(() => {
    loadCategories()
  }, [loadCategories])

  useEffect(() => {
    loadData()
  }, [loadData])

  function handleOpenCreate() {
    setEditingItem(null)
    setPreviewTab('edit')
    form.resetFields()
    form.setFieldsValue({
      // Mặc định là bản nháp - chỉ công khai sau khi có bác sĩ kiểm duyệt y khoa
      published: false,
      sortOrder: undefined,
      medicalReviewer: undefined,
      reviewConfirmed: false,
      category: categoryOptions[0] || 'Trào ngược dạ dày thực quản',
    })
    setModalOpen(true)
  }

  function handleOpenEdit(item: CmsFaqItem) {
    setEditingItem(item)
    setPreviewTab('edit')
    form.resetFields()
    form.setFieldsValue({
      question: item.question,
      answer: item.answer,
      category: item.category,
      sortOrder: item.sortOrder,
      published: item.published,
      // Không tự điền reviewer cũ: chỉ nhập khi bác sĩ thực sự duyệt lại nội dung lần này
      medicalReviewer: undefined,
      reviewConfirmed: false,
    })
    setModalOpen(true)
  }

  async function handleFormSubmit(values: FaqFormValues) {
    setSubmitting(true)
    const payload: CmsFaqFormValues = {
      question: values.question,
      answer: values.answer,
      category: Array.isArray(values.category) ? values.category[0] : values.category,
      sortOrder: values.sortOrder,
      published: Boolean(values.published),
      medicalReviewer: values.medicalReviewer?.trim() || undefined,
    }
    try {
      if (editingItem) {
        const saved = await updateCmsFaq(editingItem.id, { ...payload, version: editingItem.version })
        if (payload.published && !saved.published) {
          message.warning(
            'Nội dung y khoa đã thay đổi nhưng chưa được kiểm duyệt lại - câu hỏi đã tự động chuyển về Bản nháp.',
          )
        } else {
          message.success('Cập nhật câu hỏi thành công')
        }
      } else {
        await createCmsFaq(payload)
        message.success(payload.published ? 'Đã thêm và xuất bản câu hỏi' : 'Đã lưu câu hỏi dưới dạng bản nháp')
      }
      setModalOpen(false)
      loadData()
      loadCategories()
    } catch (err) {
      message.error(extractError(err, editingItem ? 'Cập nhật thất bại' : 'Thêm mới thất bại'))
      if ((err as { response?: { status?: number } })?.response?.status === 409) {
        setModalOpen(false)
        loadData()
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleUnpublish(item: CmsFaqItem) {
    setTogglingId(item.id)
    try {
      const saved = await setCmsFaqPublished(item.id, { published: false, version: item.version })
      message.success('Đã ẩn câu hỏi khỏi trang bệnh nhân')
      setItems((prev) => prev.map((f) => (f.id === item.id ? saved : f)))
    } catch (err) {
      message.error(extractError(err, 'Không thể ẩn câu hỏi'))
      loadData()
    } finally {
      setTogglingId(null)
    }
  }

  async function handlePublishWithExistingReview(item: CmsFaqItem) {
    setTogglingId(item.id)
    try {
      const saved = await setCmsFaqPublished(item.id, { published: true, version: item.version })
      message.success('Đã xuất bản câu hỏi')
      setItems((prev) => prev.map((f) => (f.id === item.id ? saved : f)))
    } catch (err) {
      message.error(extractError(err, 'Không thể xuất bản câu hỏi'))
      loadData()
    } finally {
      setTogglingId(null)
    }
  }

  function handleOpenReview(item: CmsFaqItem) {
    reviewForm.resetFields()
    setReviewItem(item)
  }

  async function handleReviewSubmit(values: ReviewFormValues) {
    if (!reviewItem) return
    setReviewSubmitting(true)
    try {
      const saved = await setCmsFaqPublished(reviewItem.id, {
        published: true,
        medicalReviewer: values.medicalReviewer.trim(),
        version: reviewItem.version,
      })
      message.success('Đã kiểm duyệt và xuất bản câu hỏi')
      setItems((prev) => prev.map((f) => (f.id === reviewItem.id ? saved : f)))
      setReviewItem(null)
    } catch (err) {
      message.error(extractError(err, 'Không thể xuất bản câu hỏi'))
      if ((err as { response?: { status?: number } })?.response?.status === 409) {
        setReviewItem(null)
        loadData()
      }
    } finally {
      setReviewSubmitting(false)
    }
  }

  async function handleDelete(item: CmsFaqItem) {
    try {
      await deleteCmsFaq(item.id)
      message.success('Đã xóa câu hỏi')
      loadData()
      loadCategories()
    } catch (err) {
      message.error(extractError(err, 'Xóa thất bại'))
    }
  }

  function renderPublishAction(record: CmsFaqItem) {
    if (record.published) {
      return (
        <Popconfirm
          title="Ẩn câu hỏi này?"
          description="Bệnh nhân sẽ không còn thấy câu hỏi này trong mục FAQ."
          okText="Ẩn"
          cancelText="Hủy"
          onConfirm={() => handleUnpublish(record)}
        >
          <Button size="small" icon={<EyeInvisibleOutlined />} loading={togglingId === record.id}>
            Ẩn
          </Button>
        </Popconfirm>
      )
    }
    if (hasValidReview(record)) {
      return (
        <Popconfirm
          title="Xuất bản câu hỏi này?"
          description={`Nội dung hiện tại đã được ${record.medicalReviewer} kiểm duyệt lúc ${dayjs(record.medicalReviewedAt).format('DD/MM/YYYY HH:mm')}.`}
          okText="Xuất bản"
          cancelText="Hủy"
          onConfirm={() => handlePublishWithExistingReview(record)}
        >
          <Button size="small" type="primary" ghost icon={<CheckCircleOutlined />} loading={togglingId === record.id}>
            Xuất bản
          </Button>
        </Popconfirm>
      )
    }
    return (
      <Tooltip title="Nội dung chưa được bác sĩ kiểm duyệt - cần duyệt trước khi xuất bản">
        <Button size="small" icon={<SafetyCertificateOutlined />} onClick={() => handleOpenReview(record)}>
          Duyệt & xuất bản
        </Button>
      </Tooltip>
    )
  }

  const columns: ColumnsType<CmsFaqItem> = [
    {
      title: 'STT',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 75,
      align: 'center',
      render: (v: number) => <Tag color="blue">{v}</Tag>,
    },
    {
      title: 'Câu hỏi & Chủ đề',
      key: 'question',
      render: (_, record) => (
        <div>
          <button
            type="button"
            onClick={() => setDetailItem(record)}
            className="text-left font-medium text-slate-800 hover:text-teal-600 transition-colors cursor-pointer"
          >
            {record.question}
          </button>
          <div className="mt-1">
            <Tag color="cyan">{record.category}</Tag>
          </div>
        </div>
      ),
    },
    {
      title: 'Câu trả lời tóm lược',
      dataIndex: 'answer',
      key: 'answer',
      ellipsis: true,
      render: (answer: string) => (
        <span className="text-slate-500 text-xs line-clamp-2 max-w-md">
          {answer.replace(/[#*`_]/g, '')}
        </span>
      ),
    },
    {
      title: 'Xuất bản & Kiểm duyệt',
      key: 'published',
      width: 220,
      render: (_, record) => (
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            {record.published ? (
              <Tag color="green">Đang xuất bản</Tag>
            ) : (
              <Tag color="orange">Bản nháp</Tag>
            )}
            {renderPublishAction(record)}
          </div>
          <div className="text-xs text-slate-400">
            {hasValidReview(record)
              ? `Duyệt: ${record.medicalReviewer}`
              : 'Chưa kiểm duyệt y khoa'}
          </div>
        </div>
      ),
    },
    {
      title: 'Cập nhật',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 140,
      render: (val: string) => (
        <span className="text-xs text-slate-400">
          {dayjs(val).format('DD/MM/YYYY HH:mm')}
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 130,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EyeOutlined />}
            title="Xem chi tiết"
            onClick={() => setDetailItem(record)}
          />
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            className="text-blue-600 hover:text-blue-700"
            onClick={() => handleOpenEdit(record)}
          />
          <Popconfirm
            title="Xóa câu hỏi này?"
            description="Câu hỏi sẽ bị xóa vĩnh viễn khỏi hệ thống."
            okText="Xóa"
            cancelText="Hủy"
            okButtonProps={{ danger: true }}
            onConfirm={() => handleDelete(record)}
          >
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
            />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <Title level={3} className="!mb-1 flex items-center gap-2">
            <QuestionCircleOutlined className="text-teal-600" />
            Câu hỏi thường gặp (FAQ)
          </Title>
          <Text type="secondary">
            Quản lý ngân hàng câu hỏi y khoa & giải đáp nhanh cho bệnh nhân tiêu hóa (UC0062)
          </Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          className="bg-teal-600 hover:bg-teal-700"
          onClick={handleOpenCreate}
        >
          Thêm câu hỏi mới
        </Button>
      </div>

      {/* Filter bar */}
      <Card bodyStyle={{ padding: 16 }}>
        <div className="flex flex-wrap items-center gap-3">
          <Input
            placeholder="Tìm kiếm theo câu hỏi hoặc câu trả lời..."
            prefix={<SearchOutlined className="text-slate-400" />}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            allowClear
            className="w-72"
          />
          <Select
            placeholder="Chủ đề / Danh mục"
            value={selectedCategory}
            onChange={setSelectedCategory}
            allowClear
            className="w-56"
            options={[
              { label: 'Tất cả chủ đề', value: undefined as unknown as string },
              ...categoryOptions.map((cat) => ({ label: cat, value: cat })),
            ]}
          />
          <Select
            placeholder="Trạng thái"
            value={selectedPublished}
            onChange={setSelectedPublished}
            allowClear
            className="w-40"
            options={[
              { label: 'Tất cả trạng thái', value: undefined as unknown as boolean },
              { label: 'Đã xuất bản', value: true },
              { label: 'Bản nháp / Ẩn', value: false },
            ]}
          />
          {(search || selectedCategory !== undefined || selectedPublished !== undefined) && (
            <Button
              type="link"
              onClick={() => {
                setSearch('')
                setSelectedCategory(undefined)
                setSelectedPublished(undefined)
              }}
            >
              Đặt lại bộ lọc
            </Button>
          )}
        </div>
      </Card>

      {/* Table */}
      <Card bodyStyle={{ padding: 0 }}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={items}
          loading={loading}
          pagination={{
            current: page + 1,
            pageSize,
            total,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            showTotal: (totalCount) => `Tổng cộng ${totalCount} câu hỏi`,
            onChange: (p, ps) => {
              setPage(p - 1)
              setPageSize(ps)
            },
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Chưa có câu hỏi thường gặp nào phù hợp"
              />
            ),
          }}
        />
      </Card>

      {/* Modal Thêm / Sửa */}
      <Modal
        title={
          editingItem ? 'Chỉnh sửa câu hỏi thường gặp' : 'Thêm câu hỏi thường gặp mới'
        }
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        width={720}
        destroyOnClose
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFormSubmit}
          className="mt-4"
        >
          <Form.Item
            label="Câu hỏi"
            name="question"
            rules={[
              { required: true, message: 'Vui lòng nhập câu hỏi' },
              { max: 500, message: 'Câu hỏi không vượt quá 500 ký tự' },
            ]}
          >
            <Input.TextArea
              rows={2}
              placeholder="VD: Trào ngược dạ dày thực quản có những dấu hiệu điển hình nào?"
              showCount
              maxLength={500}
            />
          </Form.Item>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Form.Item
              label="Chủ đề / Danh mục"
              name="category"
              rules={[{ required: true, message: 'Vui lòng chọn hoặc nhập chủ đề' }]}
            >
              <Select
                placeholder="Chọn hoặc nhập chủ đề mới"
                mode="tags"
                maxCount={1}
                options={categoryOptions.map((c) => ({ label: c, value: c }))}
              />
            </Form.Item>

            <Form.Item
              label="Thứ tự hiển thị"
              name="sortOrder"
              tooltip="Số nhỏ hơn sẽ hiển thị trước. Để trống hệ thống tự tăng."
            >
              <InputNumber min={0} className="w-full" placeholder="VD: 10" />
            </Form.Item>
          </div>

          <Alert
            type="info"
            showIcon
            className="mb-4 text-xs"
            message="Quy định kiểm duyệt y khoa"
            description={
              editingItem
                ? 'FAQ chỉ được công khai khi có bác sĩ kiểm duyệt độc lập. Nếu sửa câu hỏi/câu trả lời của FAQ đang công khai mà không nhập thông tin duyệt mới, FAQ sẽ tự động chuyển về Bản nháp.'
                : 'FAQ mới được lưu dưới dạng Bản nháp. Chỉ xuất bản khi nội dung đã được bác sĩ kiểm duyệt độc lập.'
            }
          />

          {editingItem && (
            <div className="mb-4 text-xs text-slate-500">
              Kiểm duyệt hiện tại:{' '}
              {hasValidReview(editingItem) ? (
                <span className="font-medium text-slate-700">
                  {editingItem.medicalReviewer} ({dayjs(editingItem.medicalReviewedAt).format('DD/MM/YYYY HH:mm')})
                </span>
              ) : (
                <span className="font-medium text-orange-600">Chưa có / đã hết hiệu lực do nội dung thay đổi</span>
              )}
            </div>
          )}

          <Form.Item
            label="Trạng thái xuất bản"
            name="published"
            valuePropName="checked"
          >
            <Switch checkedChildren="Công khai" unCheckedChildren="Bản nháp" />
          </Form.Item>

          {(() => {
            // Bắt buộc nhập reviewer khi công khai mà chưa có lần duyệt còn hiệu lực
            const reviewerRequired = Boolean(publishedValue) && !hasValidReview(editingItem)
            const hasReviewer = Boolean(reviewerValue?.trim())
            return (
              <>
                <Form.Item
                  name="medicalReviewer"
                  label="Bác sĩ kiểm duyệt y khoa"
                  extra={
                    editingItem
                      ? 'Chỉ nhập khi bác sĩ đã duyệt (lại) đúng nội dung lần này. Người duyệt phải độc lập với người biên soạn.'
                      : 'Bắt buộc khi xuất bản. Người duyệt phải độc lập với người biên soạn.'
                  }
                  rules={[
                    {
                      required: reviewerRequired,
                      whitespace: true,
                      message: 'Vui lòng nhập tên bác sĩ kiểm duyệt trước khi công khai FAQ',
                    },
                    { max: 200, message: 'Tối đa 200 ký tự' },
                  ]}
                >
                  <Input placeholder="Ví dụ: BS. CKII Nguyễn Văn A" maxLength={200} />
                </Form.Item>

                {hasReviewer && (
                  <Form.Item name="reviewConfirmed" valuePropName="checked" rules={[REVIEW_CONFIRM_RULE]}>
                    <Checkbox>
                      Tôi xác nhận bác sĩ trên đã kiểm duyệt y khoa đúng nội dung câu hỏi & câu trả lời này
                    </Checkbox>
                  </Form.Item>
                )}
              </>
            )
          })()}

          <div className="mb-2 flex items-center justify-between">
            <span className="font-semibold text-slate-700">
              Câu trả lời y khoa (Hỗ trợ định dạng Markdown):
            </span>
            <Radio.Group
              size="small"
              value={previewTab}
              onChange={(e) => setPreviewTab(e.target.value)}
            >
              <Radio.Button value="edit">Soạn thảo</Radio.Button>
              <Radio.Button value="preview">Xem trước</Radio.Button>
            </Radio.Group>
          </div>

          {previewTab === 'edit' ? (
            <Form.Item
              name="answer"
              rules={[{ required: true, message: 'Vui lòng nhập câu trả lời' }]}
            >
              <Input.TextArea
                rows={8}
                placeholder="Nhập nội dung câu trả lời. Bạn có thể dùng Markdown như gạch đầu dòng (*), in đậm (**chữ đậm**)..."
              />
            </Form.Item>
          ) : (
            <div className="mb-6 min-h-[180px] max-h-[300px] overflow-y-auto rounded-lg border border-slate-200 bg-slate-50/50 p-4">
              {answerValue ? (
                <MarkdownContent content={answerValue} />
              ) : (
                <Text type="secondary">Chưa có nội dung xem trước</Text>
              )}
            </div>
          )}

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
            <Button onClick={() => setModalOpen(false)}>Hủy</Button>
            <Button
              type="primary"
              htmlType="submit"
              loading={submitting}
              className="bg-teal-600 hover:bg-teal-700"
            >
              {editingItem ? 'Lưu thay đổi' : 'Thêm câu hỏi'}
            </Button>
          </div>
        </Form>
      </Modal>

      {/* Modal Xem chi tiết */}
      <Modal
        title={
          <div className="flex items-center gap-2 text-teal-700">
            <QuestionCircleOutlined />
            <span>Chi tiết câu hỏi thường gặp</span>
          </div>
        }
        open={Boolean(detailItem)}
        onCancel={() => setDetailItem(null)}
        footer={[
          <Button key="close" onClick={() => setDetailItem(null)}>
            Đóng
          </Button>,
          <Button
            key="edit"
            type="primary"
            className="bg-teal-600"
            onClick={() => {
              const current = detailItem
              setDetailItem(null)
              if (current) handleOpenEdit(current)
            }}
          >
            Chỉnh sửa
          </Button>,
        ]}
        width={680}
      >
        {detailItem && (
          <div className="space-y-4 py-2">
            <div>
              <Text type="secondary" className="text-xs uppercase font-medium">
                Chủ đề
              </Text>
              <div className="mt-1 flex items-center gap-2">
                <Tag color="cyan">{detailItem.category}</Tag>
                {detailItem.published ? (
                  <Tag color="green">Đang xuất bản</Tag>
                ) : (
                  <Tag color="orange">Bản nháp</Tag>
                )}
                <Tag color="blue">STT: {detailItem.sortOrder}</Tag>
              </div>
            </div>

            <div>
              <Text type="secondary" className="text-xs uppercase font-medium">
                Kiểm duyệt y khoa
              </Text>
              <div className="mt-1 text-sm">
                {hasValidReview(detailItem) ? (
                  <span className="text-slate-700">
                    <SafetyCertificateOutlined className="mr-1 text-teal-600" />
                    {detailItem.medicalReviewer} — {dayjs(detailItem.medicalReviewedAt).format('DD/MM/YYYY HH:mm')}
                  </span>
                ) : (
                  <span className="text-orange-600">Chưa được kiểm duyệt cho nội dung hiện tại</span>
                )}
              </div>
            </div>

            <div>
              <Text type="secondary" className="text-xs uppercase font-medium">
                Câu hỏi
              </Text>
              <Paragraph className="!mt-1 !mb-0 font-semibold text-slate-800 text-base">
                {detailItem.question}
              </Paragraph>
            </div>

            <div>
              <Text type="secondary" className="text-xs uppercase font-medium">
                Câu trả lời
              </Text>
              <div className="mt-2 rounded-lg border border-slate-100 bg-slate-50 p-4">
                <MarkdownContent content={detailItem.answer} />
              </div>
            </div>

            <div className="text-xs text-slate-400 pt-2 border-t border-slate-100 flex justify-between">
              <span>Mã câu hỏi: #{detailItem.id}</span>
              <span>Cập nhật: {dayjs(detailItem.updatedAt).format('DD/MM/YYYY HH:mm')}</span>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal Kiểm duyệt & xuất bản */}
      <Modal
        title={
          <div className="flex items-center gap-2 text-teal-700">
            <SafetyCertificateOutlined />
            <span>Kiểm duyệt y khoa & xuất bản</span>
          </div>
        }
        open={Boolean(reviewItem)}
        onCancel={() => setReviewItem(null)}
        footer={null}
        width={640}
        destroyOnClose
      >
        {reviewItem && (
          <Form form={reviewForm} layout="vertical" onFinish={handleReviewSubmit} className="mt-2">
            <Alert
              type="warning"
              showIcon
              className="mb-4"
              message="Nội dung này sẽ hiển thị cho bệnh nhân"
              description="Chỉ xuất bản khi bác sĩ độc lập đã đọc và xác nhận tính chính xác y khoa của câu trả lời bên dưới."
            />
            <Paragraph className="!mb-2 font-semibold text-slate-800">{reviewItem.question}</Paragraph>
            <div className="mb-4 max-h-[260px] overflow-y-auto rounded-lg border border-slate-100 bg-slate-50 p-4">
              <MarkdownContent content={reviewItem.answer} />
            </div>

            <Form.Item
              name="medicalReviewer"
              label="Bác sĩ kiểm duyệt y khoa"
              rules={[
                { required: true, whitespace: true, message: 'Vui lòng nhập tên bác sĩ kiểm duyệt' },
                { max: 200, message: 'Tối đa 200 ký tự' },
              ]}
            >
              <Input placeholder="Ví dụ: BS. CKII Nguyễn Văn A" maxLength={200} />
            </Form.Item>

            <Form.Item name="reviewConfirmed" valuePropName="checked" rules={[REVIEW_CONFIRM_RULE]}>
              <Checkbox>Tôi xác nhận bác sĩ trên đã kiểm duyệt y khoa đúng nội dung này</Checkbox>
            </Form.Item>

            <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
              <Button onClick={() => setReviewItem(null)}>Hủy</Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={reviewSubmitting}
                className="bg-teal-600 hover:bg-teal-700"
              >
                Duyệt & xuất bản
              </Button>
            </div>
          </Form>
        )}
      </Modal>
    </div>
  )
}
