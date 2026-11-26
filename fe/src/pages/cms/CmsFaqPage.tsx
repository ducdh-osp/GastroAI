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
  AutoComplete,
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
import { CmsAppShell } from '../../components/cms/CmsAppShell'
import { MarkdownContent } from '../../components/chat/MarkdownContent'
import { RichTextEditor } from '../../components/editor/RichTextEditor'
import { useCmsAuth } from '../../stores/cmsAuthStore'

const { Title, Text, Paragraph } = Typography

type FaqFormValues = CmsFaqFormValues

interface ReviewFormValues {
  medicalReviewer: string
  reviewConfirmed: boolean
}

/** Lần đối chiếu nguồn y khoa còn hiệu lực cho nội dung hiện tại (BE xoá medicalReviewedAt khi nội dung đổi). */
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
      : Promise.reject(new Error('Vui lòng xác nhận đã đối chiếu nội dung với tài liệu y khoa đã công bố')),
}

export default function CmsFaqPage() {
  const { user } = useCmsAuth()
  const canApprove = user?.userType !== 'CONTENT_AUTHOR'
  const [items, setItems] = useState<CmsFaqItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const [loading, setLoading] = useState(false)

  // Bộ lọc
  const [search, setSearch] = useState('')
  const [selectedCategory, setSelectedCategory] = useState<string | undefined>(undefined)
  const [selectedPublished, setSelectedPublished] = useState<boolean | undefined>(undefined)

  // Danh sách chủ đề có sẵn (dùng chung từ article_topics và các category đang có trong FAQ)
  const [categoryOptions, setCategoryOptions] = useState<string[]>([])

  // Modal Thêm/Sửa
  const [modalOpen, setModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<CmsFaqItem | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [previewTab, setPreviewTab] = useState<'edit' | 'preview'>('edit')
  const [form] = Form.useForm<FaqFormValues>()
  const answerValue = Form.useWatch('answer', form) ?? editingItem?.answer ?? ''
  const questionValue = Form.useWatch('question', form) ?? editingItem?.question ?? ''

  // Modal Xem chi tiết
  const [detailItem, setDetailItem] = useState<CmsFaqItem | null>(null)

  // Modal Đối chiếu nguồn & Duyệt
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
      // Bắt buộc chuyển trạng thái chờ duyệt, không tự động công khai ngay lúc lưu (SCR-AD-10)
      published: false,
      sortOrder: undefined,
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
      // FAQ mới luôn ở trạng thái Chờ duyệt; khi sửa câu đã duyệt, BE tự chuyển về Chờ duyệt nếu nội dung đổi
      published: editingItem ? editingItem.published : false,
      medicalReviewer: undefined,
    }
    try {
      if (editingItem) {
        const saved = await updateCmsFaq(editingItem.id, { ...payload, version: editingItem.version })
        if (editingItem.published && !saved.published) {
          message.warning(
            'Nội dung câu hỏi đã thay đổi và tự động chuyển về trạng thái Chờ duyệt để đối chiếu nguồn lại trước khi hiển thị.',
          )
        } else {
          message.success('Cập nhật câu hỏi thành công')
        }
      } else {
        await createCmsFaq(payload)
        message.success('Đã lưu câu hỏi sang trạng thái Chờ duyệt')
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
      loadData()
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
      loadData()
    } catch (err) {
      message.error(extractError(err, 'Không thể xuất bản câu hỏi'))
      loadData()
    } finally {
      setTogglingId(null)
    }
  }

  function handleOpenReview(item: CmsFaqItem) {
    reviewForm.resetFields()
    const defaultReviewer = user?.fullName
      ? `${user.fullName} (${user.email})`
      : user?.email || ''
    reviewForm.setFieldsValue({
      medicalReviewer: defaultReviewer,
      reviewConfirmed: false,
    })
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
      message.success('Đã đối chiếu nguồn và duyệt câu hỏi')
      setItems((prev) => prev.map((f) => (f.id === reviewItem.id ? saved : f)))
      setReviewItem(null)
      loadData()
    } catch (err) {
      message.error(extractError(err, 'Không thể duyệt câu hỏi'))
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
    if (!canApprove) {
      return null
    }
    if (record.published) {
      return (
        <Popconfirm
          title="Ẩn câu hỏi này?"
          description="Bệnh nhân sẽ không còn thấy câu hỏi này trong mục FAQ."
          okText="Ẩn"
          cancelText="Hủy"
          onConfirm={() => handleUnpublish(record)}
        >
          <Button size="small" icon={<EyeInvisibleOutlined />} loading={togglingId === record.id} className="whitespace-nowrap shrink-0">
            Ẩn
          </Button>
        </Popconfirm>
      )
    }
    if (hasValidReview(record)) {
      return (
        <Popconfirm
          title="Hiển thị lại câu hỏi này?"
          description={`Nội dung hiện tại đã được ${record.medicalReviewer} đối chiếu nguồn lúc ${dayjs(record.medicalReviewedAt).format('DD/MM/YYYY HH:mm')}.`}
          okText="Hiển thị"
          cancelText="Hủy"
          onConfirm={() => handlePublishWithExistingReview(record)}
        >
          <Button size="small" type="primary" ghost icon={<CheckCircleOutlined />} loading={togglingId === record.id} className="whitespace-nowrap shrink-0">
            Hiển thị lại
          </Button>
        </Popconfirm>
      )
    }
    return (
      <Tooltip title="Biên tập chuyên môn y khoa đối chiếu nguồn y khoa công bố trước khi công khai">
        <Button size="small" type="primary" ghost icon={<SafetyCertificateOutlined />} onClick={() => handleOpenReview(record)} className="whitespace-nowrap shrink-0">
          Duyệt
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
            className="text-left font-medium text-slate-800 dark:text-white hover:text-teal-600 transition-colors cursor-pointer"
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
      render: (answer: string) => (
        <div className="text-slate-600 dark:text-slate-200 text-xs leading-relaxed break-words py-1">
          <MarkdownContent content={answer} />
        </div>
      ),
    },
    {
      title: 'Trạng thái & Duyệt nguồn',
      key: 'published',
      width: 280,
      render: (_, record) => (
        <div className="space-y-1.5 py-0.5">
          <div className="flex items-center gap-2 flex-wrap">
            {record.published ? (
              <Tag color="green" className="m-0 shrink-0">Đã duyệt</Tag>
            ) : hasValidReview(record) ? (
              <Tag color="default" className="m-0 shrink-0">Đang ẩn</Tag>
            ) : (
              <Tag color="orange" className="m-0 shrink-0">Chờ duyệt</Tag>
            )}
            {renderPublishAction(record)}
          </div>
          <div className="text-xs text-slate-400 dark:text-slate-300">
            {hasValidReview(record)
              ? `Đối chiếu: ${record.medicalReviewer}`
              : 'Chờ biên tập chuyên môn đối chiếu nguồn'}
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
        <span className="text-xs text-slate-400 dark:text-white">
          {dayjs(val).format('DD/MM/YYYY HH:mm')}
        </span>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 110,
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
    <CmsAppShell>
      <div className="flex w-full flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <Title level={3} className="!mb-1 flex items-center gap-2">
            <QuestionCircleOutlined className="text-teal-600" />
            Câu hỏi thường gặp (FAQ)
          </Title>
          <Text type="secondary">
            Soạn thảo và đối chiếu nguồn y khoa công bố cho ngân hàng câu hỏi thường gặp hiển thị tới bệnh nhân
          </Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          className="bg-teal-600 hover:bg-teal-700"
          onClick={handleOpenCreate}
        >
          Thêm câu hỏi
        </Button>
      </div>

      {/* Filter bar */}
      <Card styles={{ body: { padding: 16 } }}>
        <div className="flex flex-wrap items-center gap-3">
          <Input
            placeholder="Tìm kiếm theo câu hỏi hoặc câu trả lời..."
            prefix={<SearchOutlined className="text-slate-400 dark:text-white" />}
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
            className="w-44"
            options={[
              { label: 'Tất cả trạng thái', value: undefined as unknown as boolean },
              { label: 'Đã duyệt', value: true },
              { label: 'Chờ duyệt / Đang ẩn', value: false },
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
      <Card styles={{ body: { padding: 0 } }}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={items}
          loading={loading}
          scroll={{ x: 1080 }}
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
        centered
        destroyOnHidden
        styles={{ body: { maxHeight: '74vh', overflowY: 'auto', paddingRight: 4 } }}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleFormSubmit}
          className="mt-4!"
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

          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <Form.Item
              label="Chủ đề / Danh mục"
              name="category"
              className="sm:col-span-3"
              rules={[{ required: true, whitespace: true, message: 'Vui lòng chọn hoặc nhập chủ đề' }]}
            >
              <AutoComplete
                placeholder="Chọn hoặc nhập chủ đề mới"
                options={categoryOptions.map((c) => ({ label: c, value: c }))}
                filterOption={(inputValue, option) =>
                  String(option?.value ?? '')
                    .toLowerCase()
                    .includes(inputValue.trim().toLowerCase())
                }
              />
            </Form.Item>

            <Form.Item
              label="Thứ tự hiển thị"
              name="sortOrder"
              className="sm:col-span-1"
              tooltip="Số nhỏ hơn sẽ hiển thị trước. Để trống hệ thống tự tăng."
            >
              <InputNumber min={0} className="w-full" placeholder="VD: 10" />
            </Form.Item>
          </div>

          <Alert
            type="info"
            showIcon
            className="mb-5!"
            title="Quy trình duyệt nội dung FAQ"
            description={
              editingItem
                ? 'Khi chỉnh sửa nội dung câu hỏi hoặc câu trả lời, câu hỏi sẽ tự động quay về trạng thái Chờ duyệt. Biên tập chuyên môn y khoa cần đối chiếu với tài liệu y khoa đã công bố và bấm Duyệt riêng trước khi hiển thị cho bệnh nhân.'
                : 'FAQ mới sau khi soạn sẽ được lưu ở trạng thái Chờ duyệt, không tự động công khai ngay. Biên tập chuyên môn y khoa cần đối chiếu với tài liệu y khoa đã công bố và bấm Duyệt riêng ở danh sách.'
            }
          />

          <div className="mb-2 flex items-center justify-between">
            <span className="font-semibold text-slate-700 dark:text-white text-sm">
              Câu trả lời y khoa <span className="text-red-500">*</span>
            </span>
            <Radio.Group
              size="small"
              value={previewTab}
              onChange={(e) => setPreviewTab(e.target.value)}
            >
              <Radio.Button value="edit">Soạn thảo</Radio.Button>
              <Radio.Button value="preview">Xem hiển thị</Radio.Button>
            </Radio.Group>
          </div>

          <div className={previewTab === 'edit' ? 'mb-4' : 'hidden'}>
            <Form.Item
              name="answer"
              className="!mb-0"
              rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập câu trả lời' }]}
            >
              <RichTextEditor
                id="faq-answer-editor"
                minHeight={180}
                placeholder="Nhập nội dung câu trả lời y khoa (dùng thanh công cụ phía trên để in đậm, in nghiêng hoặc tạo danh sách)..."
              />
            </Form.Item>
          </div>

          {previewTab === 'preview' && (
            <div className="mb-6 max-h-[340px] min-h-[220px] overflow-y-auto rounded-xl border border-slate-200 bg-slate-50/70 p-4 dark:border-neutral-700 dark:bg-neutral-900/70">
              {questionValue.trim() && (
                <div className="mb-3 border-b border-slate-200 pb-2 text-sm font-semibold text-slate-800 dark:border-neutral-700 dark:text-white">
                  {questionValue}
                </div>
              )}
              {(form.getFieldValue('answer') ?? answerValue)?.trim() ? (
                <div className="text-sm leading-relaxed text-slate-700 dark:text-slate-100">
                  <MarkdownContent content={(form.getFieldValue('answer') ?? answerValue) as string} />
                </div>
              ) : (
                <Text type="secondary">Chưa có nội dung xem trước</Text>
              )}
            </div>
          )}

          <div className="flex justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
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
                  <Tag color="green">Đã duyệt</Tag>
                ) : hasValidReview(detailItem) ? (
                  <Tag color="default">Đang ẩn</Tag>
                ) : (
                  <Tag color="orange">Chờ duyệt</Tag>
                )}
                <Tag color="blue">STT: {detailItem.sortOrder}</Tag>
              </div>
            </div>

            <div>
              <Text type="secondary" className="text-xs uppercase font-medium">
                Đối chiếu nguồn y khoa
              </Text>
              <div className="mt-1 text-sm">
                {hasValidReview(detailItem) ? (
                  <span className="text-slate-700 dark:text-white">
                    <SafetyCertificateOutlined className="mr-1 text-teal-600" />
                    {detailItem.medicalReviewer} — {dayjs(detailItem.medicalReviewedAt).format('DD/MM/YYYY HH:mm')}
                  </span>
                ) : (
                  <span className="text-orange-600">Chờ biên tập chuyên môn y khoa đối chiếu nguồn cho nội dung hiện tại</span>
                )}
              </div>
            </div>

            <div>
              <Text type="secondary" className="text-xs uppercase font-medium">
                Câu hỏi
              </Text>
              <Paragraph className="!mt-1 !mb-0 font-semibold text-slate-800 dark:text-white text-base">
                {detailItem.question}
              </Paragraph>
            </div>

            <div>
              <Text type="secondary" className="text-xs uppercase font-medium">
                Câu trả lời
              </Text>
              <div className="mt-2 rounded-lg border border-slate-200 bg-slate-50 p-4 dark:border-neutral-700 dark:bg-neutral-900/60">
                <MarkdownContent content={detailItem.answer} />
              </div>
            </div>

            <div className="text-xs text-slate-400 dark:text-white pt-2 border-t border-slate-100 flex justify-between">
              <span>Mã câu hỏi: #{detailItem.id}</span>
              <span>Cập nhật: {dayjs(detailItem.updatedAt).format('DD/MM/YYYY HH:mm')}</span>
            </div>
          </div>
        )}
      </Modal>

      {/* Modal Đối chiếu nguồn & Duyệt */}
      <Modal
        title={
          <div className="flex items-center gap-2 text-teal-700">
            <SafetyCertificateOutlined />
            <span>Đối chiếu nguồn y khoa & Duyệt FAQ</span>
          </div>
        }
        open={Boolean(reviewItem)}
        onCancel={() => setReviewItem(null)}
        footer={null}
        width={640}
        destroyOnHidden
      >
        {reviewItem && (
          <Form form={reviewForm} layout="vertical" onFinish={handleReviewSubmit} className="mt-2!">
            <Alert
              type="warning"
              showIcon
              className="mb-4!"
              message="Nội dung này sẽ hiển thị cho bệnh nhân"
              description="Chỉ duyệt khi Biên tập chuyên môn y khoa đã đối chiếu câu trả lời với tài liệu y khoa đã công bố (phác đồ, hướng dẫn điều trị chính thức). Nội dung này không phải thẩm định của bác sĩ hành nghề."
            />
            <Paragraph className="!mb-2 font-semibold text-slate-800 dark:text-white">{reviewItem.question}</Paragraph>
            <div className="mb-4 max-h-[260px] overflow-y-auto rounded-lg border border-slate-200/80 bg-slate-50 p-4 dark:border-slate-800 dark:bg-slate-900/60">
              <MarkdownContent content={reviewItem.answer} />
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

            <Form.Item name="reviewConfirmed" valuePropName="checked" rules={[REVIEW_CONFIRM_RULE]}>
              <Checkbox>Tôi xác nhận đã đối chiếu nội dung này với tài liệu y khoa đã công bố</Checkbox>
            </Form.Item>

            <div className="flex justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
              <Button onClick={() => setReviewItem(null)}>Hủy</Button>
              <Button
                type="primary"
                htmlType="submit"
                loading={reviewSubmitting}
                className="bg-teal-600 hover:bg-teal-700"
              >
                Duyệt
              </Button>
            </div>
          </Form>
        )}
      </Modal>
      </div>
    </CmsAppShell>
  )
}
