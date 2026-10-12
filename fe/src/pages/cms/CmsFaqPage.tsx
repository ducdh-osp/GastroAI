import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlusOutlined,
  QuestionCircleOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import {
  Button,
  Card,
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
  const [form] = Form.useForm<CmsFaqFormValues>()
  const answerValue = Form.useWatch('answer', form)

  // Modal Xem chi tiết
  const [detailItem, setDetailItem] = useState<CmsFaqItem | null>(null)

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
      published: true,
      sortOrder: undefined,
      category: categoryOptions[0] || 'Trào ngược dạ dày thực quản',
    })
    setModalOpen(true)
  }

  function handleOpenEdit(item: CmsFaqItem) {
    setEditingItem(item)
    setPreviewTab('edit')
    form.setFieldsValue({
      question: item.question,
      answer: item.answer,
      category: item.category,
      sortOrder: item.sortOrder,
      published: item.published,
    })
    setModalOpen(true)
  }

  async function handleFormSubmit(values: CmsFaqFormValues) {
    setSubmitting(true)
    try {
      if (editingItem) {
        await updateCmsFaq(editingItem.id, values)
        message.success('Cập nhật câu hỏi thành công')
      } else {
        await createCmsFaq(values)
        message.success('Thêm câu hỏi mới thành công')
      }
      setModalOpen(false)
      loadData()
      loadCategories()
    } catch {
      message.error(editingItem ? 'Cập nhật thất bại' : 'Thêm mới thất bại')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleTogglePublished(item: CmsFaqItem, published: boolean) {
    try {
      await setCmsFaqPublished(item.id, published)
      message.success(published ? 'Đã xuất bản câu hỏi' : 'Đã chuyển thành bản nháp')
      setItems((prev) =>
        prev.map((f) => (f.id === item.id ? { ...f, published } : f)),
      )
    } catch {
      message.error('Không thể đổi trạng thái xuất bản')
    }
  }

  async function handleDelete(item: CmsFaqItem) {
    try {
      await deleteCmsFaq(item.id)
      message.success('Đã xóa câu hỏi')
      loadData()
      loadCategories()
    } catch {
      message.error('Xóa thất bại')
    }
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
      title: 'Xuất bản',
      dataIndex: 'published',
      key: 'published',
      width: 110,
      align: 'center',
      render: (published: boolean, record) => (
        <Switch
          checked={published}
          checkedChildren="Hiện"
          unCheckedChildren="Ẩn"
          onChange={(checked) => handleTogglePublished(record, checked)}
        />
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

          <Form.Item
            label="Trạng thái xuất bản"
            name="published"
            valuePropName="checked"
          >
            <Switch checkedChildren="Xuất bản ngay" unCheckedChildren="Lưu nháp" />
          </Form.Item>

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
    </div>
  )
}
