import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  InboxOutlined,
  LinkOutlined,
  LoadingOutlined,
  RedoOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tag,
  Typography,
  Upload,
  message,
} from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import dayjs from 'dayjs'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  deleteCmsDocument,
  listCmsDocuments,
  retryCmsDocument,
  uploadCmsDocument,
  type CmsDocumentItem,
  type DocumentProcessingStage,
  type DocumentStatus,
} from '../../api/cmsDocuments'

const { Title, Text } = Typography
const { Dragger } = Upload

export default function CmsDocumentsPage() {
  const [items, setItems] = useState<CmsDocumentItem[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(15)
  const [loading, setLoading] = useState(false)

  // Filters
  const [search, setSearch] = useState('')
  const [selectedStatus, setSelectedStatus] = useState<DocumentStatus | undefined>(undefined)

  // Auto-refresh
  const [autoRefresh, setAutoRefresh] = useState(true)

  // Modal upload
  const [uploadModalOpen, setUploadModalOpen] = useState(false)
  const [uploadFileList, setUploadFileList] = useState<UploadFile[]>([])
  const [sourceUrlInput, setSourceUrlInput] = useState('')
  const [uploading, setUploading] = useState(false)

  // Modal xem lỗi chi tiết
  const [errorModalItem, setErrorModalItem] = useState<CmsDocumentItem | null>(null)

  const loadData = useCallback(async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const res = await listCmsDocuments({
        q: search.trim() || undefined,
        status: selectedStatus,
        page,
        size: pageSize,
      })
      setItems(res.items)
      setTotal(res.total)
    } catch {
      if (!silent) message.error('Không thể tải danh sách tài liệu y khoa')
    } finally {
      if (!silent) setLoading(false)
    }
  }, [search, selectedStatus, page, pageSize])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Polling tự động mỗi 4s nếu có tài liệu đang xử lý
  const hasProcessingDocs = useMemo(() => {
    return items.some((item) => item.status === 'PROCESSING' || item.status === 'PENDING')
  }, [items])

  useEffect(() => {
    if (!autoRefresh && !hasProcessingDocs) return

    const interval = setInterval(() => {
      loadData(true)
    }, 4000)

    return () => clearInterval(interval)
  }, [autoRefresh, hasProcessingDocs, loadData])

  // KPI stats
  const stats = useMemo(() => {
    const done = items.filter((i) => i.status === 'DONE').length
    const processing = items.filter((i) => i.status === 'PROCESSING' || i.status === 'PENDING').length
    const error = items.filter((i) => i.status === 'ERROR').length
    return { done, processing, error, total }
  }, [items, total])

  async function handleUpload() {
    if (uploadFileList.length === 0) {
      message.warning('Vui lòng chọn hoặc kéo thả ít nhất 1 tệp tài liệu')
      return
    }

    const file = uploadFileList[0].originFileObj as File
    setUploading(true)
    try {
      await uploadCmsDocument(file, sourceUrlInput || undefined)
      message.success('Đã tải lên tài liệu thành công. Hệ thống đang tiến hành số hóa tự động.')
      setUploadModalOpen(false)
      setUploadFileList([])
      setSourceUrlInput('')
      loadData()
    } catch (err: unknown) {
      const errMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      message.error(errMsg || 'Tải lên tài liệu thất bại')
    } finally {
      setUploading(false)
    }
  }

  async function handleRetry(item: CmsDocumentItem) {
    try {
      await retryCmsDocument(item.id)
      message.success(`Đã đưa tài liệu #${item.id} vào hàng đợi xử lý lại`)
      loadData()
    } catch {
      message.error('Không thể thực hiện thử lại')
    }
  }

  async function handleDelete(item: CmsDocumentItem) {
    try {
      await deleteCmsDocument(item.id)
      message.success('Đã xóa tài liệu và các đoạn trích dẫn RAG liên quan')
      loadData()
    } catch (err: unknown) {
      const errMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      message.error(errMsg || 'Xóa tài liệu thất bại')
    }
  }

  function renderStatusTag(status: DocumentStatus) {
    switch (status) {
      case 'DONE':
        return (
          <Tag icon={<CheckCircleOutlined />} color="success">
            Sẵn sàng (RAG)
          </Tag>
        )
      case 'PROCESSING':
        return (
          <Tag icon={<LoadingOutlined spin />} color="processing">
            Đang xử lý
          </Tag>
        )
      case 'PENDING':
        return (
          <Tag icon={<ClockCircleOutlined />} color="warning">
            Chờ xử lý
          </Tag>
        )
      case 'ERROR':
        return (
          <Tag icon={<CloseCircleOutlined />} color="error">
            Lỗi xử lý
          </Tag>
        )
      default:
        return <Tag>{status}</Tag>
    }
  }

  function renderStageInfo(stage: DocumentProcessingStage, status: DocumentStatus) {
    if (status === 'ERROR') {
      return <Text type="danger" className="text-xs font-medium">Thất bại</Text>
    }
    switch (stage) {
      case 'PENDING':
        return <Text type="secondary" className="text-xs">Đang xếp hàng</Text>
      case 'EXTRACTION':
        return <Text className="text-xs text-blue-600 font-medium">Trích xuất văn bản</Text>
      case 'CHUNKING':
        return <Text className="text-xs text-purple-600 font-medium">Phân đoạn (Chunking)</Text>
      case 'EMBEDDING':
        return <Text className="text-xs text-amber-600 font-medium">Vector Embedding</Text>
      case 'DONE':
        return <Text type="success" className="text-xs font-medium">Hoàn tất 100%</Text>
      default:
        return <Text className="text-xs">{stage}</Text>
    }
  }

  const columns: ColumnsType<CmsDocumentItem> = [
    {
      title: 'Mã',
      dataIndex: 'id',
      key: 'id',
      width: 65,
      align: 'center',
      render: (id: number) => <span className="text-slate-400 text-xs">#{id}</span>,
    },
    {
      title: 'Tài liệu & Nguồn',
      key: 'title',
      render: (_, record) => (
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <FilePdfOutlined className="text-red-500 text-base" />
            <span className="font-semibold text-slate-800">{record.title}</span>
          </div>
          {record.sourceUrl && (
            <div className="flex items-center gap-1 text-xs text-teal-700">
              <LinkOutlined />
              <a
                href={record.sourceUrl}
                target="_blank"
                rel="noreferrer"
                className="hover:underline truncate max-w-sm"
              >
                {record.sourceUrl}
              </a>
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 140,
      render: (status: DocumentStatus) => renderStatusTag(status),
    },
    {
      title: 'Giai đoạn số hóa',
      key: 'processingStage',
      width: 160,
      render: (_, record) => (
        <div className="space-y-0.5">
          {renderStageInfo(record.processingStage, record.status)}
          {record.status === 'ERROR' && record.errorMessage && (
            <button
              type="button"
              onClick={() => setErrorModalItem(record)}
              className="text-xs text-red-500 hover:underline block text-left cursor-pointer"
            >
              Xem chi tiết lỗi
            </button>
          )}
        </div>
      ),
    },
    {
      title: 'Đoạn trích dẫn',
      dataIndex: 'chunkCount',
      key: 'chunkCount',
      width: 120,
      align: 'center',
      render: (chunks: number, record) =>
        record.status === 'DONE' ? (
          <Tag color="cyan">{chunks} chunks</Tag>
        ) : (
          <span className="text-slate-300 text-xs">-</span>
        ),
    },
    {
      title: 'Thời gian tải lên',
      dataIndex: 'uploadedAt',
      key: 'uploadedAt',
      width: 150,
      render: (val: string) => (
        <span className="text-xs text-slate-500">
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
          {record.status === 'ERROR' && (
            <Button
              type="text"
              size="small"
              icon={<RedoOutlined />}
              title="Thử xử lý lại"
              className="text-amber-600 hover:text-amber-700"
              onClick={() => handleRetry(record)}
            />
          )}
          <Popconfirm
            title="Xóa tài liệu này?"
            description="Tài liệu và toàn bộ vector embeddings liên quan sẽ bị xóa khỏi hệ thống RAG."
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
              title="Xóa tài liệu"
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
            <FileTextOutlined className="text-teal-600" />
            Tài liệu y khoa & Số hóa RAG
          </Title>
          <Text type="secondary">
            Quản lý tài liệu phác đồ y khoa, giám sát tiến trình số hóa & chia chunking dùng cho trợ lý AI (UC0061)
          </Text>
        </div>
        <div className="flex items-center gap-2">
          <Button
            icon={<ReloadOutlined />}
            onClick={() => loadData()}
            loading={loading}
          >
            Làm mới
          </Button>
          <Button
            type="primary"
            icon={<UploadOutlined />}
            className="bg-teal-600 hover:bg-teal-700"
            onClick={() => {
              setUploadFileList([])
              setSourceUrlInput('')
              setUploadModalOpen(true)
            }}
          >
            Tải lên tài liệu mới
          </Button>
        </div>
      </div>

      {/* KPI Stats */}
      <Row gutter={[16, 16]}>
        <Col xs={12} sm={6}>
          <Card className="rounded-xl shadow-xs">
            <Statistic title="Tổng số tài liệu" value={stats.total} />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card className="rounded-xl shadow-xs border-green-100 bg-green-50/20">
            <Statistic
              title="Sẵn sàng cho RAG"
              value={stats.done}
              valueStyle={{ color: '#16a34a' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card className="rounded-xl shadow-xs border-blue-100 bg-blue-50/20">
            <Statistic
              title="Đang trong tiến trình"
              value={stats.processing}
              valueStyle={{ color: '#2563eb' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6}>
          <Card className="rounded-xl shadow-xs border-red-100 bg-red-50/20">
            <Statistic
              title="Lỗi cần xử lý lại"
              value={stats.error}
              valueStyle={{ color: '#dc2626' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Filter bar */}
      <Card bodyStyle={{ padding: 16 }}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <Input
              placeholder="Tìm kiếm tài liệu hoặc URL nguồn..."
              prefix={<SearchOutlined className="text-slate-400" />}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              allowClear
              className="w-72"
            />
            <Select
              placeholder="Trạng thái"
              value={selectedStatus}
              onChange={setSelectedStatus}
              allowClear
              className="w-44"
              options={[
                { label: 'Tất cả trạng thái', value: undefined as unknown as DocumentStatus },
                { label: 'Sẵn sàng (RAG)', value: 'DONE' },
                { label: 'Đang xử lý', value: 'PROCESSING' },
                { label: 'Chờ xử lý', value: 'PENDING' },
                { label: 'Lỗi', value: 'ERROR' },
              ]}
            />
            {(search || selectedStatus !== undefined) && (
              <Button
                type="link"
                onClick={() => {
                  setSearch('')
                  setSelectedStatus(undefined)
                }}
              >
                Đặt lại bộ lọc
              </Button>
            )}
          </div>

          <div className="flex items-center gap-2 text-xs text-slate-500">
            <span
              className={`inline-block w-2 h-2 rounded-full ${
                hasProcessingDocs && autoRefresh ? 'bg-blue-500 animate-pulse' : 'bg-emerald-500'
              }`}
            />
            <span>
              {hasProcessingDocs && autoRefresh
                ? 'Đang tự động đồng bộ tiến trình (mỗi 4s)...'
                : 'Hệ thống ở trạng thái ổn định'}
            </span>
            <Switch
              size="small"
              checked={autoRefresh}
              onChange={setAutoRefresh}
              checkedChildren="Tự động"
              unCheckedChildren="Tắt"
            />
          </div>
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
            pageSizeOptions: ['10', '15', '30'],
            showTotal: (totalCount) => `Tổng cộng ${totalCount} tài liệu`,
            onChange: (p, ps) => {
              setPage(p - 1)
              setPageSize(ps)
            },
          }}
          locale={{
            emptyText: (
              <Empty
                image={Empty.PRESENTED_IMAGE_SIMPLE}
                description="Chưa có tài liệu y khoa nào được tải lên"
              />
            ),
          }}
        />
      </Card>

      {/* Modal Tải lên tài liệu */}
      <Modal
        title="Tải lên tài liệu phác đồ y khoa mới"
        open={uploadModalOpen}
        onCancel={() => !uploading && setUploadModalOpen(false)}
        footer={[
          <Button
            key="cancel"
            onClick={() => setUploadModalOpen(false)}
            disabled={uploading}
          >
            Hủy
          </Button>,
          <Button
            key="submit"
            type="primary"
            className="bg-teal-600 hover:bg-teal-700"
            loading={uploading}
            onClick={handleUpload}
          >
            Bắt đầu tải lên & số hóa
          </Button>,
        ]}
        width={580}
        destroyOnClose
      >
        <div className="space-y-4 py-2">
          <Alert
            type="info"
            showIcon
            message="Định dạng hỗ trợ: PDF, DOCX, TXT"
            description="Sau khi tải lên, hệ thống sẽ tự động trích xuất nội dung văn bản, chia đoạn thông minh (Chunking) và sinh vector embeddings phục vụ RAG tư vấn tiêu hóa."
            className="text-xs"
          />

          <div>
            <Text className="font-semibold text-slate-700 block mb-1">
              Chọn tệp tài liệu:
            </Text>
            <Dragger
              fileList={uploadFileList}
              beforeUpload={(file) => {
                setUploadFileList([file as UploadFile])
                return false
              }}
              onRemove={() => setUploadFileList([])}
              maxCount={1}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined className="text-teal-600 text-3xl" />
              </p>
              <p className="ant-upload-text text-sm font-medium">
                Kéo thả file vào đây hoặc bấm để chọn tệp từ máy tính
              </p>
              <p className="ant-upload-hint text-xs text-slate-400">
                Khuyên dùng các phác đồ điều trị chính thống từ Bệnh viện Nhân Dân 115, Bạch Mai hoặc Bộ Y Tế.
              </p>
            </Dragger>
          </div>

          <div>
            <Text className="font-semibold text-slate-700 block mb-1">
              URL nguồn công khai (Tùy chọn):
            </Text>
            <Input
              placeholder="VD: https://benhvientanphu.vn/phac-do-dieu-tri-ibs.pdf"
              value={sourceUrlInput}
              onChange={(e) => setSourceUrlInput(e.target.value)}
              prefix={<LinkOutlined className="text-slate-400" />}
            />
            <Text type="secondary" className="text-xs mt-1 block">
              URL nguồn sẽ được lưu để AI trích dẫn nguồn minh bạch cho bệnh nhân khi trả lời.
            </Text>
          </div>
        </div>
      </Modal>

      {/* Modal Xem chi tiết lỗi */}
      <Modal
        title={
          <div className="flex items-center gap-2 text-red-600">
            <CloseCircleOutlined />
            <span>Chi tiết lỗi xử lý tài liệu</span>
          </div>
        }
        open={Boolean(errorModalItem)}
        onCancel={() => setErrorModalItem(null)}
        footer={[
          <Button key="close" onClick={() => setErrorModalItem(null)}>
            Đóng
          </Button>,
          errorModalItem && (
            <Button
              key="retry"
              type="primary"
              className="bg-amber-600"
              onClick={() => {
                const current = errorModalItem
                setErrorModalItem(null)
                if (current) handleRetry(current)
              }}
            >
              Thử xử lý lại ngay
            </Button>
          ),
        ]}
      >
        {errorModalItem && (
          <div className="space-y-3 py-2">
            <div>
              <Text className="text-xs uppercase text-slate-400 font-medium">
                Tài liệu
              </Text>
              <div className="font-semibold text-slate-800">
                {errorModalItem.title} (#{errorModalItem.id})
              </div>
            </div>

            <div>
              <Text className="text-xs uppercase text-slate-400 font-medium">
                Giai đoạn phát sinh lỗi
              </Text>
              <div>
                <Tag color="error">{errorModalItem.processingStage}</Tag>
              </div>
            </div>

            <div>
              <Text className="text-xs uppercase text-slate-400 font-medium">
                Thông báo lỗi từ hệ thống
              </Text>
              <div className="mt-1 rounded-lg bg-red-50 border border-red-200 p-3 text-red-800 font-mono text-xs whitespace-pre-wrap">
                {errorModalItem.errorMessage || 'Không có thông tin lỗi chi tiết.'}
              </div>
            </div>
          </div>
        )}
      </Modal>
    </div>
  )
}
